#!/bin/bash

# ================================================================
# SupremeAI Flutter Admin App - Local CI/CD Setup & Test Script
# ================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  SupremeAI Flutter Admin App - CI/CD Setup                      ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check prerequisites
echo -e "${YELLOW}📋 Checking Prerequisites...${NC}"
echo ""

# Check Flutter
if ! command -v flutter &> /dev/null; then
    echo -e "${RED}❌ Flutter not found. Please install Flutter first.${NC}"
    echo "   Visit: https://flutter.dev/docs/get-started/install"
    exit 1
fi
echo -e "${GREEN}✅ Flutter installed: $(flutter --version | head -1)${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java not found. Please install Java 17+${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java installed: $(java -version 2>&1 | head -1)${NC}"

# Check Firebase CLI
if ! command -v firebase &> /dev/null; then
    echo -e "${YELLOW}⚠️  Firebase CLI not found. Installing...${NC}"
    npm install -g firebase-tools || {
        echo -e "${RED}❌ Failed to install Firebase CLI${NC}"
        exit 1
    }
fi
echo -e "${GREEN}✅ Firebase CLI installed: $(firebase --version)${NC}"

# Check npm
if ! command -v npm &> /dev/null; then
    echo -e "${RED}❌ npm not found. Please install Node.js${NC}"
    exit 1
fi
echo -e "${GREEN}✅ npm installed${NC}"

echo ""
echo -e "${BLUE}📦 Step 1: Building Flutter Web App...${NC}"
echo ""

cd flutter_admin_app

# Get dependencies
echo "Getting Flutter dependencies..."
flutter pub get

# Build web app
echo ""
echo "Building web app (this may take 2-3 minutes)..."
flutter build web --release --no-pub --base-href /

echo ""
echo -e "${GREEN}✅ Web app built successfully!${NC}"
echo ""

# Show build size
echo "Build artifacts:"
du -sh build/web/
echo ""

cd ..

# Check Firebase configuration
echo -e "${BLUE}🔥 Step 2: Verifying Firebase Configuration...${NC}"
echo ""

if [ ! -f ".firebaserc" ]; then
    echo -e "${RED}❌ .firebaserc not found${NC}"
    exit 1
fi

if [ ! -f "firebase.json" ]; then
    echo -e "${RED}❌ firebase.json not found${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Firebase configuration files found${NC}"
echo ""

# Check if logged in to Firebase
echo -e "${BLUE}🔐 Step 3: Firebase Authentication...${NC}"
echo ""

if [ -z "$FIREBASE_TOKEN" ]; then
    echo -e "${YELLOW}⚠️  FIREBASE_TOKEN env variable not set${NC}"
    echo ""
    echo "For GitHub Actions deployment:"
    echo "1. Generate a Firebase token:"
    echo "   firebase login:ci"
    echo ""
    echo "2. Add to GitHub Secrets:"
    echo "   FIREBASE_TOKEN=<your-token>"
    echo ""
    echo "Alternatively, for local testing:"
    echo "   firebase login"
else
    echo -e "${GREEN}✅ FIREBASE_TOKEN is set${NC}"
fi

echo ""
echo -e "${BLUE}📋 Step 4: Configuration Summary${NC}"
echo ""

echo "Firebase Project: $(grep -o '"default"[^}]*' .firebaserc | head -1)"
echo "Hosting Targets:"
grep -o '"flutter-admin"' firebase.json && echo "  ✅ flutter-admin"
grep -o '"main-dashboard"' firebase.json && echo "  ✅ main-dashboard"

echo ""
echo -e "${YELLOW}🚀 Optional: Deploy to Firebase${NC}"
echo ""
echo "To deploy the web app to Firebase Hosting:"
echo ""
echo "  # Option 1: If logged in locally"
echo "  firebase deploy --only hosting:flutter-admin"
echo ""
echo "  # Option 2: Using token"
echo "  firebase deploy --only hosting:flutter-admin --token \$FIREBASE_TOKEN"
echo ""

echo -e "${BLUE}✅ Setup Complete!${NC}"
echo ""
echo "Next steps:"
echo "1. Run GitHub Actions workflow by pushing to main branch"
echo "2. Or manually trigger: ${BLUE}gh workflow run flutter-ci-cd.yml${NC}"
echo "3. Monitor deployment: ${BLUE}gh run watch${NC}"
echo ""
echo -e "${GREEN}Your Flutter app will be live at:${NC}"
echo "https://supremeai-a.web.app/admin/"
echo ""
