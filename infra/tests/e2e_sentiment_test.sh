#!/usr/bin/env bash
#
# tests/e2e_sentiment_test.sh
#
# End-to-end validation suite for the BERT Sentiment Analysis public API.
#
# Requirements
#   GCP_PROJECT_ID   – your GCP project
#   N8N_URL          – public Cloud Run URL of n8n workflow service
#   ML_URL           – public Cloud Run URL of ML sentiment service
#   API_KEY          – ML API key (from GCP Secret Manager)
#
# Usage:
#   export GCP_PROJECT_ID=my-project
#   export N8N_URL=https://n8n-workflow-abc123.run.app
#   export ML_URL=https://sentiment-ml-abc123.run.app
#   export API_KEY=$(gcloud secrets versions access latest \
#     --project=$GCP_PROJECT_ID --secret=ml-api-gateway-key)
#   cd infra && ./tests/e2e_sentiment_test.sh
#
set -euo pipefail

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; NC='\033[0m'

PASS=0; FAIL=0; TOTAL=0

ok()   { echo -e "  ${GREEN}✔ PASS${NC}  $*"; ((++PASS)); }
fail() { echo -e "  ${RED}✘ FAIL${NC}  $*"; ((++FAIL)); ((++TOTAL)); }
info() { echo -e "${BLUE}[TEST]${NC} $*"; }

: "${GCP_PROJECT_ID:?export GCP_PROJECT_ID first}"
: "${N8N_URL:?export N8N_URL first}"
: "${ML_URL:?export ML_URL first}"
: "${API_KEY:?export API_KEY first}"

PUBLIC="${N8N_URL}/sentiment"
ML_HEALTH="${ML_URL}/health"
ML_READY="${ML_URL}/ready"

# ── Helper: HTTP POST ─────────────────────────────────────────────────────
http_post() {
  local payload="$1" expected="$2" label="$3"
  ((++TOTAL))
  raw="$(curl -sS -X POST "$PUBLIC" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: ${API_KEY}" \
    -d "$payload" \
    -w "\n%{http_code}" -m 20 2>/dev/null || true)"

  body="$(echo "$raw" | sed '$d')"
  code="$(echo "$raw" | tail -1)"
  if [ "$code" = "$expected" ]; then
    ok "$label — HTTP $code"
  else
    fail "$label — expected $expected got $code  Body: $(echo "$body" | head -c 120)"
  fi
  echo "$body"
}

json_extract() { echo "$1" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('$2','__MISSING__'))"; }

LONG5K="$(python3 -c "print('x'*5000)")"
LONG5KP1="$(python3 -c "print('x'*5001)")"

# ── 0. Health / Ready ─────────────────────────────────────────────────────
info "── 0 · Sanity (service health) ─────────────────────────────────"
((++TOTAL))
code="$(curl -so /dev/null -w "%{http_code}" "$ML_HEALTH" -m 10 2>/dev/null || echo "000")"
if [ "$code" = 200 ]; then ok "ML /health => 200"; else
  ML_HEALTH="$ML_URL/ SentimentServiceStatus is"

  fail "ML /health returned $code"
fi

((++TOTAL))
code="$(curl -so /dev/null -w "%{http_code}" "$ML_READY" -m 10 2>/dev/null || echo "000")"
if [ "$code" = 200 ]; then ok "ML /ready => 200"; else fail "ML /ready returned $code"; fi

code="$(curl -so /dev/null -w "%{http_code}" "$PUBLIC" -m 10 2>/dev/null || echo "000")"
if [ "$code" = 401 ]; then ok "Public endpoint rejects without auth => 401"; else fail "Expected 401 (no auth), got $code"; fi

# ── 1. Happy paths ────────────────────────────────────────────────────────
info "── 1 · Happy-path requests ───────────────────────────────────────"

resp="$(http_post '{"text":"I love this product, it is absolutely wonderful!"}' 200 \
  "Short positive text")"
[ "$FAIL" -gt 0 ] || for field in sentiment confidence processed_at; do
  val="$(json_extract "$resp" "$field")"
  if [ "$val" = "__MISSING__" ]; then fail "Missing field: $field"; else ok "Field '$field' = $(echo "$val" | head -c 60)"; fi
done

resp="$(http_post "{\"text\":\"$LONG5K\"}" 200 \
  "Exactly 5000 chars")"

resp="$(http_post '{"text":"This is the single worst item I have ever purchased in my entire life."}' 200 \
  "Negative text")"

# ── 2. Boundary / error cases ──────────────────────────────────────────────
info "── 2 · Boundary & validation ──────────────────────────────────────"

http_post "{\"text\":\"$LONG5KP1\"}" 400 "Over limit (5001 chars)"
http_post '{"message":"hello"}'       400 "Missing text field"
http_post '{"text":null}'             400 "Null text field"
http_post '{"text":""}'               400 "Empty text"
http_post '{"text":"   "}'            400 "Whitespace-only"
http_post 'not json'                  400 "Non-JSON body"
http_post '{}'                        400 "Empty JSON"

# ─────────────────────────────────────────────────────────────────────────────
info "── 3 · Latency ───────────────────────────────────────────────────"
((++TOTAL))
START_NS="$(date +%s%N)"
http_post '{"text":"Quick latency test."}' 200 "Latency probe"
END_NS="$(date +%s%N)"
ELAPSED_MS="$(( (END_NS - START_NS) / 1000000 ))"
info "  e2e latency: ${ELAPSED_MS} ms"
if [ "$ELAPSED_MS" -lt 5000 ]; then ok "e2e < 5 s (model warm)"; else fail "e2e > 5 s — model cold"; fi

# ─────────────────────────────────────────────────────────────────────────────
info "── 4 · Auth ──────────────────────────────────────────────────────"
((++TOTAL))
code="$(curl -so /dev/null -w "%{http_code}" -X POST "$PUBLIC" \
  -H "Content-Type: application/json" \
  -d '{"text":"test"}' -m 10 2>/dev/null || echo "000")"
if [ "$code" = 401 ]; then ok "No API-key → 401"; else fail "Expected 401 got $code"; fi

((++TOTAL))
code="$(curl -so /dev/null -w "%{http_code}" -X POST "$PUBLIC" \
  -H "Content-Type: application/json" -H "X-API-Key: wrong-key" \
  -d '{"text":"test"}' -m 10 2>/dev/null || echo "000")"
if [ "$code" = 401 ]; then ok "Bad API-key → 401"; else fail "Expected 401 got $code"; fi

# ─────────────────────────────────────────────────────────────────────────────
info "── 5 · n8n alert Webhook health ─────────────────────────────────"
N8N_HEALTH="${N8N_URL%/}/healthz"
((++TOTAL))
code="$(curl -so /dev/null -w "%{http_code}" "$N8N_HEALTH" -m 10 2>/dev/null || echo "000")"
if [ "$code" = 200 ]; then ok "n8n healthz => $code"; else fail "n8n healthz => $code"; fi

# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "══════════════════════════════════════════════════════════════════════"
if [ "$FAIL" -eq 0 ]; then
  echo -e "  ${GREEN}All $TOTAL tests passed ✔${NC}"
else
  echo -e "  ${RED}$FAIL / $TOTAL tests failed ✘${NC}"
fi
echo "══════════════════════════════════════════════════════════════════════"
exit "$FAIL"
