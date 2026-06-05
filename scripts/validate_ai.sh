#!/bin/bash
# AI Validation Script - real model integration, latency, quality, and security checks
set -euo pipefail

PROVIDERS_FILE="${AI_PROVIDERS_FILE:-config/skills-local.json}"
# Accept either AI_VALIDATION_BASE_URL (preferred) or legacy POCKETLAB_URL
BASE_URL="${AI_VALIDATION_BASE_URL:-${POCKETLAB_URL:-http://localhost:8080}}"
VALIDATION_ENDPOINT="${AI_VALIDATION_ENDPOINT:-/api/chat/send}"
PROMPT="Return only the word OK"
TIMEOUT_SECONDS="${AI_VALIDATION_TIMEOUT:-15}"
MAX_LATENCY_MS="${AI_VALIDATION_MAX_LATENCY:-8000}"
REPORT="ai_validation_report.json"
PASS=0
FAIL=0
ERRORS=()

check_command() {
  command -v "$1" >/dev/null 2>&1
}

if ! check_command curl && ! check_command wget; then
  echo "ERROR: curl or wget required"
  exit 2
fi

echo "Running real AI validation checks..."
echo "Base URL: ${BASE_URL}"
echo "Validation endpoint: ${VALIDATION_ENDPOINT}"
echo "Providers file: ${PROVIDERS_FILE}"
echo "Timeout: ${TIMEOUT_SECONDS}s"
echo "Max latency: ${MAX_LATENCY_MS}ms"

http_get() {
  local url="$1"
  local out
  if check_command curl; then
    out=$(curl -sS -o /dev/null -w "%{http_code} %{time_total}" --max-time "${TIMEOUT_SECONDS}" "$url" 2>/dev/null || true)
  else
    out=$(wget -S -q -O /dev/null --timeout="${TIMEOUT_SECONDS}" "$url" 2>&1 | awk '/HTTP\//{code=$2} /^.*seconds/{t=$4} END{print code, t}' || true)
  fi
  echo "$out"
}

http_post() {
  local url="$1"
  local body="$2"
  local tmp
  tmp=$(mktemp)
  local code latency
  if check_command curl; then
    code=$(curl -sS -o "$tmp" -w "%{http_code}" --max-time "${TIMEOUT_SECONDS}" -H "Content-Type: application/json" -H "X-Guest-Access: true" -d "$body" "$url" 2>/dev/null || true)
    latency=$(curl -sS -o /dev/null -w "%{time_total}" --max-time "${TIMEOUT_SECONDS}" -H "Content-Type: application/json" -H "X-Guest-Access: true" -d "$body" "$url" 2>/dev/null || true)
  else
    echo "000 0"
    rm -f "$tmp"
    return
  fi
  echo "$code $latency"
  RESPONSE_BODY="$({
    cat "$tmp"
  })"
  rm -f "$tmp"
}

start_time=$(date +%s)
declare -A results
results=( ["provider_health_check"]="000" ["pass"]="000" ["latency_ms"]="0" ["provider"]="default" ["model"]="default" ["error"]="" ["response_valid"]="false" )
RESPONSE_BODY=""

check_health() {
  local code latency
  read -r code latency <<< "$(http_get "${BASE_URL}/api/health")" || true
  results[provider_health_check]="${code:-000}"
  results[latency_ms]=$(awk "BEGIN{printf \"%d\", ${latency:-0} * 1000}")
  if [[ "${results[provider_health_check]}" == "200" ]]; then
    echo "PASS Health endpoint reachable (${results[provider_health_check]}, ${results[latency_ms]}ms)"
    ((PASS++)) || true
  else
    echo "FAIL Health endpoint returned ${results[provider_health_check]}"
    ERRORS+=("Health endpoint failed: ${results[provider_health_check]}")
    ((FAIL++)) || true
  fi
}

infer() {
  local provider="${1:-}"
  local model="${2:-}"
  local payload
  if [[ -n "$provider" && -n "$model" ]]; then
    payload=$(cat <<EOF
{"message":"${PROMPT}","provider":"${provider}","model":"${model}","messages":[{"role":"user","content":"${PROMPT}"}],"max_tokens":8,"temperature":0.0}
EOF
)
  else
    payload=$(cat <<EOF
{"message":"${PROMPT}","messages":[{"role":"user","content":"${PROMPT}"}],"max_tokens":8,"temperature":0.0}
EOF
)
  fi
  local out
  RESPONSE_BODY=""
  out=$(http_post "${BASE_URL}${VALIDATION_ENDPOINT}" "$payload") || true
  read -r code latency <<< "$out" || true
  results[provider]="${provider:-default}"
  results[model]="${model:-default}"
  results[pass]="${code}"
  results[latency_ms]=$(awk "BEGIN{printf \"%d\", ${latency:-0} * 1000}")

  local response_ok="false"
  if [[ "$code" == "200" ]]; then
    echo "PASS Inference (${provider:-default}/${model:-default}) ${results[latency_ms]}ms — checking response content..."
    if echo "$RESPONSE_BODY" | grep -qi "OK"; then
      echo "PASS Response content contains expected output 'OK'"
      response_ok="true"
      ((PASS++)) || true
    else
      echo "FAIL Response body does not contain expected output 'OK'"
      ERRORS+=("Inference ${provider:-default}/${model:-default}: response body missing expected content 'OK'")
      ((FAIL++)) || true
    fi
  else
    if [[ "$code" == "500" && "${VALIDATION_ENDPOINT}" == "/api/chat/send" ]]; then
      echo "WARN Inference endpoint returned HTTP 500; trying fallback /api/chat/message"
      out=$(http_post "${BASE_URL}/api/chat/message" "$payload") || true
      read -r code latency <<< "$out" || true
      results[provider]="${provider:-default}"
      results[model]="${model:-default}"
      results[pass]="${code}"
      results[latency_ms]=$(awk "BEGIN{printf \"%d\", ${latency:-0} * 1000}")
      if [[ "$code" == "200" ]]; then
        echo "PASS Fallback inference (/api/chat/message) ${results[latency_ms]}ms — checking response content..."
        if echo "$RESPONSE_BODY" | grep -qi "OK"; then
          echo "PASS Response content contains expected output 'OK'"
          response_ok="true"
          ((PASS++)) || true
        else
          echo "FAIL Response body does not contain expected output 'OK'"
          ERRORS+=("Fallback inference ${provider:-default}/${model:-default}: response body missing expected content 'OK'")
          ((FAIL++)) || true
        fi
      else
        echo "FAIL Fallback inference (/api/chat/message) HTTP ${code}"
        ERRORS+=("Fallback inference ${provider:-default}/${model:-default}: HTTP ${code}")
        ((FAIL++)) || true
      fi
    else
      echo "FAIL Inference (${provider:-default}/${model:-default}) HTTP ${code}"
      ERRORS+=("Inference ${provider:-default}/${model:-default}: HTTP ${code}")
      ((FAIL++)) || true
    fi
  fi
  results[response_valid]="$response_ok"

  if [[ "${results[latency_ms]}" -gt "${MAX_LATENCY_MS}" ]]; then
    echo "WARN Latency ${results[latency_ms]}ms exceeds threshold ${MAX_LATENCY_MS}ms"
  fi
}

discover_and_infer() {
  if [[ -f "${PROVIDERS_FILE}" ]]; then
    echo "Discovering providers from ${PROVIDERS_FILE}..."
    local count
    count=$(python -c "import json; d=json.load(open('${PROVIDERS_FILE}')); print(len(d.get('skills', d.get('providers', d.get('ai_providers', []))) if isinstance(d, dict) else []))" 2>/dev/null || echo 0)
    if [[ "${count}" != "0" ]]; then
      echo "Found ${count} providers in configuration"
      local first_skill
      first_skill=$(python -c "import json; d=json.load(open('${PROVIDERS_FILE}')); s=d.get('skills', d.get('providers', {})); print(list(s.keys())[0] if s else '')" 2>/dev/null || echo "")
      infer "${first_skill}" "default"
    else
      echo "No parsable providers in ${PROVIDERS_FILE}; running default inference"
      infer "" ""
    fi
  else
    echo "Providers file not found; running default inference against ${BASE_URL}${VALIDATION_ENDPOINT}"
    infer "" ""
  fi
}

check_secrets() {
  echo "Checking secrets hygiene..."
  local track_rc
  if check_command git; then
    if git ls-files --error-unmatch config/.env >/dev/null 2>&1; then
      echo "WARN config/.env is tracked in git — consider removing and rotating secrets"
      ((FAIL++)) || true
      ERRORS+=("config/.env tracked in git")
    else
      echo "PASS Secrets hygiene (config/.env not tracked)"
      ((PASS++)) || true
    fi
    if git ls-files --error-unmatch src/main/resources/firebase-service-account.json >/dev/null 2>&1; then
      echo "WARN firebase-service-account.json tracked in git — consider removing and using Secret Manager"
      ((FAIL++)) || true
      ERRORS+=("firebase-service-account.json tracked in git")
    else
      echo "PASS Secrets hygiene (service account not tracked)"
      ((PASS++)) || true
    fi
  else
    echo "SKIP Secrets hygiene check (git not installed)"
  fi
}

check_health
discover_and_infer
check_secrets

end_time=$(date +%s)
elapsed=$((end_time - start_time))
overall="PASS"
if [[ "$FAIL" -gt 0 ]]; then
  overall="FAIL"
fi

cat > "${REPORT}" <<EOF
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "overall": "${overall}",
  "pass": ${PASS},
  "fail": ${FAIL},
  "errors": $(printf '%s\n' "${ERRORS[@]}" | python3 -c 'import sys,json; print(json.dumps([l.strip() for l in sys.stdin if l.strip()]))'),
  "duration_seconds": ${elapsed},
  "results": {
    "provider_health_check": "${results[provider_health_check]}",
    "inference": "${results[pass]}",
    "latency_ms": ${results[latency_ms]},
    "provider": "${results[provider]}",
    "model": "${results[model]}",
    "response_content_valid": "${results[response_valid]}"
  }
}
EOF

echo ""
echo "========================================"
echo "AI Validation Report: ${REPORT}"
cat "${REPORT}"
echo "========================================"
if [[ "${overall}" != "PASS" ]]; then
  exit 1
fi
