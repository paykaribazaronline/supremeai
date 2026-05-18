#!/usr/bin/env bash
# =============================================================================
#  SupremeAI Studio — One-Click Launcher
#  প্রস্তুত করুন এবং চালান — বাংলা ও ইংরেজি গাইড
# =============================================================================
#  run:  LANG=en ./start-supremeai.sh   (English)
#  run:  LANG=bn ./start-supremeai.sh   (Bengali — default)
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Colors ─────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'

# ── Language (LANG=bn for Bengali, LANG=en for English) ─────────────────────
LANG_MODE="${LANG:-bn}"
HAVE_BN=false; [[ "$LANG_MODE" == "bn" ]] && HAVE_BN=true

bn() { $HAVE_BN && echo "$1" || echo "$2"; }
hl(){ echo -e "\n${CYAN}╔══▶ $1${NC}"; }
ok(){ echo -e "${GREEN}✔ $1${NC}"; }
warn(){ echo -e "${YELLOW}⚠ $1${NC}"; }
err(){ echo -e "${RED}✖ $1${NC}"; exit 1; }

is_cmd() { command -v "$1" &>/dev/null; }

# ── Step 1: Check Java 21 ───────────────────────────────────────────────────
check_java() {
  if ! is_cmd java; then
    err "$(bn 'Java পাওয়া যায়নি! https://adoptium.net/ থেকে JDK 21 ইনস্টল করুন।' 'Java not found! Install JDK 21 from https://adoptium.net/')"
  fi
  local ver
  ver=$(java -version 2>&1 | head -1 | grep -oP '\d+')
  if [[ "$ver" -lt 21 ]]; then
    err "$(bn "Java $ver পাওয়া গেছে, কিন্তু JDK 21 প্রয়োজন। https://adoptium.net/" "Java $ver found — JDK 21 is required. https://adoptium.net/")"
  fi
  ok "$(bn "Java $ver পাওয়া গেছে ✓" "Java $ver found ✓")"
}

# ── Step 2: Check Node.js ───────────────────────────────────────────────────
check_node() {
  if ! is_cmd node; then
    warn "$(bn 'Node.js পাওয়া যায়নি — ড্যাশবোর্ড চালানো যাবে না। https://nodejs.org/' 'Node.js not found — dashboard won`t launch. https://nodejs.org/')"
    DASHBOARD_OK=false; return
  fi
  local ver
  ver=$(node -v | grep -oP '\d+')
  [[ "$ver" -lt 20 ]] && warn "$(bn "Node.js $ver (প্রয়োজন 20+) — ড্যাশবোর্ড সমস্যা হতে পারে" "Node.js $ver (need 20+) — dashboard may have issues")"
  ok "$(bn "Node.js $(node -v) পাওয়া গেছে ✓" "Node.js $(node -v) found ✓")"
  DASHBOARD_OK=true
}

# ── Step 3: Firestore Credential Handling ───────────────────────────────────
handle_firestore_creds() {
  hl "$(bn 'Firestore ক্রেডেনশিয়াল চেক হচ্ছে...' 'Checking Firestore credentials...')"

  # Priority 1: GCP_CREDENTIALS_LOCATION env var (file: prefix)
  if [[ -n "${GCP_CREDENTIALS_LOCATION:-}" ]] && [[ "${GCP_CREDENTIALS_LOCATION}" == file:* ]]; then
    local f="${GCP_CREDENTIALS_LOCATION#file:}"
    [[ -f "$f" ]] && cp -f "$f" "$SCRIPT_DIR/service-account.json" 2>/dev/null && \
      ok "$(bn 'এনভায়রনমেন্ট ভেরিয়েবল থেকে ক্রেডেনশিয়াল কপি ✓' 'Credentials copied from env var ✓')" && return
  fi

  # Priority 2: Raw JSON in env var
  if [[ -n "${GOOGLE_CREDENTIALS_JSON:-}" ]]; then
    echo "$GOOGLE_CREDENTIALS_JSON" > "$SCRIPT_DIR/service-account.json"
    ok "$(bn 'GOOGLE_CREDENTIALS_JSON থেকে কপি ✓' 'Credentials copied from GOOGLE_CREDENTIALS_JSON ✓')"
    return
  fi

  # Priority 3: Already present in repo root
  if [[ -f "$SCRIPT_DIR/service-account.json" ]]; then
    ok "$(bn 'ক্রেডেনশিয়াল ইতিমধ্যে আছে ✓' 'Credentials already in place ✓')"
    return
  fi

  # ── GUI Setup Wizard ───────────────────────────────────────────────
  warn "$(bn 'Firestore ক্রেডেনশিয়াল পাওয়া যায়নি! সেটআপ উইজার্ড চালু করা হচ্ছে...' 'Firestore credentials not found! Launching setup wizard...')"
  
  # Start the node wizard in the background momentarily to let the browser open it
  node "$SCRIPT_DIR/setup-wizard.js" &
  WIZARD_PID=$!
  
  sleep 2
  if is_cmd xdg-open; then
    xdg-open http://localhost:8080 2>/dev/null || true
  elif is_cmd open; then
    open http://localhost:8080 2>/dev/null || true
  fi
  
  hl "$(bn 'ব্রাউজারে সেটআপ উইজার্ড খুলুন এবং service-account.json আপলোড করুন...' 'Please open the setup wizard in your browser and upload service-account.json...')"
  
  # Wait for the wizard to finish (it exits 0 on successful upload)
  wait $WIZARD_PID
  
  if [[ -f "$SCRIPT_DIR/service-account.json" ]]; then
    ok "$(bn 'ক্রেডেনশিয়াল সফলভাবে সেট আপ করা হয়েছে ✓' 'Credentials configured successfully ✓')"
  else
    err "$(bn 'সেটআপ অসম্পূর্ণ। ক্রেডেনশিয়াল পাওয়া যায়নি।' 'Setup incomplete. Credentials not found.')"
  fi
}

# ── Main ────────────────────────────────────────────────────────────────────
printf "${CYAN}"
cat << 'BANNER'
╔══════════════════════════════════════════════════════════════════╗
║                                                              ║
║   ███████╗██╗   ██╗███╗   ██╗ ██████╗██████╗ ██╗   ██╗███████╗  ║
║   ██╔════╝██║   ██║████╗  ██║██╔════╝██╔══██╗██║   ██║██╔════╝  ║
║   ███████╗██║   ██║██╔██╗ ██║██║     ██████╔╝██║   ██║█████╗   ║
║   ╚════██║██║   ██║██║╚██╗██║██║     ██╔═══╝ ██║   ██║██╔══╝   ║
║   ███████║╚██████╔╝██║ ╚████║╚██████╗██║     ╚██████╔╝███████╗ ║
║   ╚══════╝ ╚═════╝ ╚═╝  ╚═══╝ ╚═════╝╚═╝      ╚═════╝ ╚══════╝ ║
║                                                              ║
║      SupremeAI Studio — AI App Generator & Code Analyzer    ║
╚══════════════════════════════════════════════════════════════════╝
BANNER
printf "${NC}\n"

# 1. Java
hl "$(bn 'প্রয়োজনীয় সফটওয়্যার চেক...' 'Checking prerequisites...') / Java"
check_java

# 2. Node.js
hl "$(bn 'প্রয়োজনীয় সফটওয়্যার চেক...' 'Checking prerequisites...') / Node.js"
check_node
DASHBOARD_OK=false

# 3. Firestore
handle_firestore_creds

# 4. Build
if [[ -f "$SCRIPT_DIR/build/libs/app.jar" ]]; then
  warn "$(bn 'ইতিমধ্যে বিল্ড করা আছে। পুনরায় বিল্ড? (y/N)' 'Already built. Rebuild? (y/N)')"
  read -rp " " _rebuild
  [[ "$_rebuild" =~ ^[Yy]$ ]] && REBUILD=true || REBUILD=false
else
  REBUILD=true
fi

if [[ "$REBUILD" == "true" ]]; then
  hl "$(bn 'প্রজেক্ট বিল্ড হচ্ছে...' 'Building project...') (সময় pasa: ১-৩ মিনিট)"
  if ./gradlew clean bootJar -x test; then
    ok "$(bn 'বিল্ড সফল ✓' 'Build successful ✓')"
  else
    err "$(bn 'বিল্ড ব্যর্থ — gradle লগ চেক করুন।' 'Build failed — check gradle logs.')"
  fi
else
  ok "$(bn 'পুরানো বিল্ড ব্যবহার করা হচ্ছে' 'Using existing build')"
fi

# 5. Launch Backend
hl "$(bn 'ব্যাকএন্ড চালানো হচ্ছে...' 'Starting backend...')"
PROFILE="local"
[[ -f "$SCRIPT_DIR/service-account.json" ]] && CRT="-Dspring.cloud.gcp.credentials.location=file:${SCRIPT_DIR}/service-account.json" || CRT=""
if [[ -f "$SCRIPT_DIR/service-account.json" ]] && grep -q "supremeai-sandbox-mock" "$SCRIPT_DIR/service-account.json"; then
  PROFILE="sandbox"
fi
nohup java -Dspring.profiles.active=$PROFILE -Dserver.port=8080 $CRT \
  -jar "$SCRIPT_DIR/build/libs/app.jar" \
  > "$SCRIPT_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
ok "$(bn "ব্যাকএন্ড চালু হয়েছে (PID: $BACKEND_PID) ✓" "Backend started (PID: $BACKEND_PID) ✓")"
warn "$(bn 'লগ দেখতে: tail -f backend.log' 'Check logs: tail -f backend.log')"

# 6. Wait for ready
hl "$(bn 'ব্যাকএন্ড রেডি হওয়ার অপেক্ষা...' 'Waiting for backend to be ready...')"
dots=""
for i in {1..30}; do
  if curl -sf http://localhost:8080/api/generate/health &>/dev/null; then
    ok "$(bn "ব্যাকএন্ড রেডি ✓ (${i} সেকেন্ডে)" "Backend ready in ${i}s ✓")"
    break
  fi
  dots="$dots."
  echo -n "$dots"
  sleep 1
done

# 7. Dashboard
if [[ "$DASHBOARD_OK" == "true" ]]; then
  echo ""
  read -rp "$(bn '
ড্যাশবোর্ড চালান?
   Y = হ্যাঁ, N = নাহিং
   Your choice (Y/n): ' '
Launch dashboard? (Y/n): ')" launch_dash

  if [[ ! "$launch_dash" =~ ^[Nn]$ ]]; then
    hl "$(bn 'ড্যাশবোর্ড চালানো হচ্ছে...' 'Starting dashboard...')"
    cd "$SCRIPT_DIR/dashboard" || err "$(bn 'ড্যাশবোর্ড ফোল্ডার পাওয়া যায়নি!' 'Dashboard folder not found!')"
    if npm install --silent 2>&1 | tail -5; then
      npm run dev -- --host 0.0.0.0 --port 5173 > "$SCRIPT_DIR/dashboard.log" 2>&1 &
      DASH_PID=$!
      sleep 4
      cd "$SCRIPT_DIR"
      if is_cmd xdg-open; then
        xdg-open http://localhost:5173 2>/dev/null || true
      elif is_cmd open; then
        open http://localhost:5173 2>/dev/null || true
      fi
      ok "$(bn 'ড্যাশবোর্ড চালু ✓ → http://localhost:5173' 'Dashboard running ✓ → http://localhost:5173')"
    else
      warn "$(bn 'npm install ব্যর্থ — ম্যানুয়ালি চালান: cd dashboard && npm install && npm run dev' 'npm install failed — manually: cd dashboard && npm install && npm run dev')"
    fi
  else
    echo ""
    ok "$(bn 'শুধুমাত্র ব্যাকএন্ড চালু আছে → http://localhost:8080' 'Backend only → http://localhost:8080')"
  fi
else
  echo ""
  warn "$(bn 'Node.js নেই → শুধু ব্যাকএন্ড http://localhost:8080' 'Node.js missing → backend only at http://localhost:8080')"
fi

# ── Final Banner ────────────────────────────────────────────────────────────
echo ""
printf "${CYAN}"
echo '╔══════════════════════════════════════════════════════════════════╗'
bn \
'║  ✅ SupremeAI Studio চালু হয়েছে!                               ║' \
'║  ✅ SupremeAI Studio is running!                                ║'
bn \
'║                                                              ║' \
'║                                                              ║'
bn \
'║  📍 ব্যাকএন্ড:      http://localhost:8080                    ║' \
'║  📍 Backend API:    http://localhost:8080                    ║'
bn \
'║  📏 ড্যাশবোর্ড:    http://localhost:5173                    ║' \
'║  📏 Dashboard:      http://localhost:5173                    ║'
bn \
'║  📊 লগ:           tail -f backend.log                      ║' \
'║  📊 Logs:          tail -f backend.log                      ║'
echo '╚══════════════════════════════════════════════════════════════════╝'
printf "${NC}\n"
