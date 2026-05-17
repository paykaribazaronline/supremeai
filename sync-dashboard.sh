#!/bin/bash

# Script to sync dashboard build artifacts with Spring Boot static resources
# Author: Antigravity

set -e

PROJECT_ROOT="/home/nazifarabbu/supremeai"
DASHBOARD_DIR="$PROJECT_ROOT/dashboard"
STATIC_DIR="$PROJECT_ROOT/src/main/resources/static"

echo "🚀 Starting Dashboard Sync..."

# 1. Build the dashboard
echo "📦 Building Dashboard (React/Vite)..."
cd "$DASHBOARD_DIR"
npm install
npm run build

# 2. Clear old static files
echo "🗑️  Clearing old static files in $STATIC_DIR..."
# Keep .gitkeep if it exists, but remove other files
find "$STATIC_DIR" -mindepth 1 ! -name ".gitkeep" -delete

# 3. Copy new build artifacts
echo "📁 Copying new build artifacts to $STATIC_DIR..."
cp -r "$DASHBOARD_DIR/dist/"* "$STATIC_DIR/"

echo "✅ Dashboard Sync Complete!"
echo "✨ You can now run './gradlew bootRun' to see the updated design on localhost:8080"
