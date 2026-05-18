#!/usr/bin/env bash
# =============================================================================
#  launch-dashboard.sh — Serve the pre-built public/admin/ dashboard instantly
#  No build, no GCP, no Docker — just open http://localhost:3000/admin
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ADMIN_DIR="$SCRIPT_DIR/public/admin"
PORT="${PORT:-3000}"

if [[ ! -d "$ADMIN_DIR" ]]; then
  echo "✖ public/admin/ folder not found. Are you in the SupremeAI repo root?"
  exit 1
fi

echo ""
echo -e "\033[0;36m╔══════════════════════════════════════════════════════════╗\033[0m"
echo -e "\033[0;36m║  📊  SupremeAI Admin Dashboard — Quick Launch            ║\033[0m"
echo -e "\033[0;36m║                                                          ║\033[0m"
echo -e "\033[0;36m║  📍 http://localhost:${PORT}/admin                          ║\033[0m"
echo -e "\033[0;36m╚══════════════════════════════════════════════════════════╝\033[0m"
echo ""

# Try python3 first (no npm required)
if command -v python3 &>/dev/null; then
  cd "$ADMIN_DIR"
  echo "📡 Serving public/admin/ on port $PORT ... (Ctrl+C to stop)"
  echo "   Open → http://localhost:${PORT}/admin"
  python3 -m http.server "$PORT" --bind 0.0.0.0
# Fall back to npx http-server
elif command -v npx &>/dev/null; then
  cd "$ADMIN_DIR"
  npx -y http-server -p "$PORT" -c-1 --cors
else
  echo "✖ Neither python3 nor npx found. Install python3 to use this."
  exit 1
fi
