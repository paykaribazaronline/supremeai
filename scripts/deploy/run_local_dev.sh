#!/bin/bash

echo "🚀 Starting SupremeAI Local Development Environment..."

PROJECT_ROOT="$(cd "$(dirname "$0")/../../" && pwd)"

# Function to clean up background processes on exit
cleanup() {
    echo ""
    echo "🛑 Shutting down all local services..."
    # Kill all child processes started by this script
    pkill -P $$ 2>/dev/null || true
    echo "✅ All services stopped safely."
    exit 0
}

# Trap SIGINT (Ctrl+C) and SIGTERM, then call the cleanup function
trap cleanup SIGINT SIGTERM

echo "=========================================="
echo "1. 🔥 Starting Firebase Emulators..."
echo "=========================================="
cd "$PROJECT_ROOT"
if command -v firebase >/dev/null 2>&1; then
  firebase emulators:start &
else
  echo "⚠️ Firebase CLI not found. Skipping emulators."
fi

echo "=========================================="
echo "2. 🛠️ Starting Spring Boot Backend (Local Profile)..."
echo "=========================================="
cd "$PROJECT_ROOT"
export GOOGLE_APPLICATION_CREDENTIALS="$PROJECT_ROOT/src/main/resources/firebase-service-account.json"
chmod +x ./gradlew || true
./gradlew bootRun --args='--spring.profiles.active=local' &

echo "=========================================="
echo "3. 💻 Starting Admin Dashboard (Vite Dev Server)..."
echo "=========================================="
cd "$PROJECT_ROOT/dashboard"
if [ ! -d node_modules ]; then
  npm install
fi
npm run dev &

echo "=========================================="
echo "✅ Local Environment is starting up!"
echo "   Backend:  http://localhost:8080"
echo "   Frontend: http://localhost:5173"
echo "🛑 Press Ctrl+C at any time to stop all services."
echo "=========================================="

# Wait for all background processes (keeps the script running)
wait