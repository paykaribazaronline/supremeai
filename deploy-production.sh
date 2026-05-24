#!/bin/bash
# SupremeAI Production Deploy Script
# Usage: ./deploy-production.sh

set -e

echo "🚀 SupremeAI Production Deployment"
echo "===================================="

PROJECT_ID="supremeai-a"
REGION="us-central1"

# Check prerequisites
echo "📋 Checking prerequisites..."
command -v firebase >/dev/null 2>&1 || { echo "❌ Firebase CLI required. Run: npm install -g firebase-tools"; exit 1; }
command -v gcloud >/dev/null 2>&1 || { echo "❌ gcloud CLI required."; exit 1; }

# Deploy Firebase Functions
echo ""
echo "📦 Deploying Firebase Functions..."
cd functions
npm install --legacy-peer-deps
firebase deploy --only functions --project "$PROJECT_ID" --region "$REGION"
cd ..

# Deploy Firestore rules
echo ""
echo "🔒 Deploying Firestore Rules..."
firebase deploy --only firestore:rules --project "$PROJECT_ID"

# Deploy Hosting
echo ""
echo "🌐 Deploying Hosting..."
mkdir -p public/admin public/mobile

# Dashboard — prefer local artifacts, fall back to existing public/admin
DASHBOARD_SRC=""
if [ -d "dashboard/dist" ]; then
    DASHBOARD_SRC="dashboard/dist"
elif [ -d "dist" ]; then
    DASHBOARD_SRC="dist"
elif [ -d "public/admin" ] && [ "$(ls -A public/admin 2>/dev/null)" ]; then
    echo "✅ Using existing public/admin build"
    DASHBOARD_SRC=""
else
    echo "⚠️  No dashboard build found, deploying placeholder"
    echo "<html><body><h1>Admin Panel</h1></body></html>" > public/admin/index.html
    DASHBOARD_SRC=""
fi

if [ -n "$DASHBOARD_SRC" ]; then
    cp -r "$DASHBOARD_SRC"/* public/admin/
fi

# Mobile web — prefer local artifacts, fall back to existing public/mobile
MOBILE_SRC=""
if [ -d "supremeai/build/web" ]; then
    MOBILE_SRC="supremeai/build/web"
elif [ -d "build/web" ]; then
    MOBILE_SRC="build/web"
elif [ -d "public/mobile" ] && [ "$(ls -A public/mobile 2>/dev/null)" ]; then
    echo "✅ Using existing public/mobile build"
    MOBILE_SRC=""
else
    echo "⚠️  No mobile build found, creating placeholder"
    echo "<html><body><h1>Mobile</h1></body></html>" > public/mobile/index.html
    MOBILE_SRC=""
fi

if [ -n "$MOBILE_SRC" ]; then
    cp -r "$MOBILE_SRC"/* public/mobile/
fi

firebase deploy --only hosting --project "$PROJECT_ID"

echo ""
echo "✅ Deployment Complete!"
echo ""
echo "🌐 Access your application:"
echo "   Admin Panel: https://$PROJECT_ID.web.app/admin"
echo "   API Health:  https://$PROJECT_ID.web.app/api/health"
echo ""