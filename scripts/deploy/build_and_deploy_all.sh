#!/bin/bash
set -euo pipefail

echo "🚀 Starting Master Build & Deploy Pipeline..."

PROJECT_ROOT="$(cd "$(dirname "$0")/../../" && pwd)"

check_command() {
  command -v "$1" >/dev/null 2>&1
}

preflight() {
  local missing=()
  check_command node || missing+=("node")
  check_command npm || missing+=("npm")
  check_command ./gradlew || missing+=("gradlew (at PROJECT_ROOT)")
  if [[ ${#missing[@]} -gt 0 ]]; then
    echo "❌ Missing required tools: ${missing[*]}"
    exit 1
  fi
  echo "✅ Preflight passed"
}

preflight

echo "=========================================="
echo "1. 🏗️ Building Spring Boot Backend..."
echo "=========================================="
cd "$PROJECT_ROOT"
chmod +x ./gradlew || true
./gradlew clean build -x test

echo "=========================================="
echo "2. 🏗️ Building Admin Dashboard (React/Vite)..."
echo "=========================================="
cd "$PROJECT_ROOT/dashboard"
if [ ! -d node_modules ]; then
  npm install
fi
npm run build

echo "=========================================="
echo "3. 🧠 Deploying Semantic Router (Cloud Run)..."
echo "=========================================="
SEMANTIC_DIR="$PROJECT_ROOT/microservices/semantic_router"
if [ -d "$SEMANTIC_DIR" ]; then
  bash "$PROJECT_ROOT/scripts/deploy/deploy_semantic_router.sh"
else
  echo "⚠️  Semantic Router directory not found at $SEMANTIC_DIR — skipping"
fi

echo "=========================================="
echo "4. 🔥 Building Firebase Functions..."
echo "=========================================="
FUNCTIONS_DIR="$PROJECT_ROOT/functions"
if [ -d "$FUNCTIONS_DIR" ]; then
  cd "$FUNCTIONS_DIR"
  if [ ! -d node_modules ]; then
    npm install
  fi
  npm run build 2>/dev/null || true
  cd "$PROJECT_ROOT"
else
  echo "⚠️  Functions directory not found — skipping functions build"
fi

echo "=========================================="
echo "5. 🚀 Deploying to Firebase (Hosting & Functions)..."
echo "=========================================="
if check_command firebase; then
  cd "$PROJECT_ROOT"
  read -p "❓ Do you want to proceed with Firebase deployment? (y/N): " confirm
  if [[ "$confirm" =~ ^[Yy]$ ]]; then
    firebase deploy --only hosting,functions
  else
    echo "⏭️  Skipping Firebase deployment."
  fi
else
  echo "⚠️  Firebase CLI not found — skipping firebase deploy"
fi

echo "=========================================="
echo "✅ All builds and deployments completed successfully!"
echo "=========================================="
