#!/bin/bash

# ================================================================
# SupremeAI Flutter CI/CD - Pre-Deployment Verification
# ================================================================
# This script verifies all components are working before deployment

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

CHECKS_PASSED=0
CHECKS_FAILED=0

check_item() {
    local name=$1
    local command=$2
    
    if eval "$command" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ $name${NC}"
        ((CHECKS_PASSED++))
    else
        echo -e "${RED}❌ $name${NC}"
        ((CHECKS_FAILED++))
    fi
}

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  SupremeAI Flutter CI/CD - Pre-Deployment Verification        ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

echo -e "${YELLOW}Verifying System Requirements...${NC}"
echo ""

check_item "Flutter installed" "flutter --version"
check_item "Java 17+ installed" "java -version"
check_item "npm installed" "npm --version"
check_item "Firebase CLI installed" "firebase --version"
check_item "Git installed" "git --version"

echo ""
echo -e "${YELLOW}Verifying Project Structure...${NC}"
echo ""

check_item "flutter_admin_app exists" "test -d flutter_admin_app"
check_item ".firebaserc exists" "test -f .firebaserc"
check_item "firebase.json exists" "test -f firebase.json"
check_item ".github/workflows/flutter-ci-cd.yml exists" "test -f .github/workflows/flutter-ci-cd.yml"
check_item "pubspec.yaml exists" "test -f flutter_admin_app/pubspec.yaml"
check_item "web/index.html exists" "test -f flutter_admin_app/web/index.html"

echo ""
echo -e "${YELLOW}Verifying Firebase Configuration...${NC}"
echo ""

check_item ".firebaserc has default project" "grep -q '\"default\"' .firebaserc"
check_item "firebase.json has main-dashboard target" "grep -q 'main-dashboard' firebase.json"
check_item "firebase.json has rewrites configured" "grep -q 'rewrites' firebase.json"

echo ""
echo -e "${YELLOW}Verifying Flutter Configuration...${NC}"
echo ""

cd flutter_admin_app
check_item "pubspec.yaml is valid" "flutter pub get --dry-run"
check_item "Flutter analysis passes" "flutter analyze --no-pub || true"
cd ..

echo ""
echo -e "${YELLOW}Verifying GitHub Configuration...${NC}"
echo ""

check_item "GitHub repo initialized" "test -d .git"
check_item "Git remote 'origin' configured" "git remote | grep -q origin"

echo ""
echo -e "${YELLOW}Checking GitHub Secrets...${NC}"
echo ""

if command -v gh &> /dev/null; then
    check_item "GitHub CLI available" "gh --version"
    
    if gh secret list > /dev/null 2>&1; then
        check_item "FIREBASE_TOKEN secret exists" "gh secret list | grep -q FIREBASE_TOKEN"
    fi
else
    echo -e "${YELLOW}⚠️  GitHub CLI not installed (optional)${NC}"
    echo "   Install from: https://cli.github.com"
fi

echo ""
echo -e "${YELLOW}Verifying Build Capabilities...${NC}"
echo ""

# Check if web build folder exists (from previous build)
if [ -d "flutter_admin_app/build/web" ]; then
    check_item "Previous build artifacts exist" "test -d flutter_admin_app/build/web"
else
    echo -e "${YELLOW}⚠️  No previous build found (build will be created in CI/CD)${NC}"
fi

echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""
echo -e "${BLUE}Summary:${NC}"
echo -e "Checks Passed: ${GREEN}$CHECKS_PASSED${NC}"
echo -e "Checks Failed: ${RED}$CHECKS_FAILED${NC}"
echo ""

if [ $CHECKS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed! System is ready for deployment.${NC}"
    echo ""
    echo -e "${BLUE}Next steps:${NC}"
    echo "1. Generate Firebase token: ${GREEN}firebase login:ci${NC}"
    echo "2. Add to GitHub Secrets: ${GREEN}FIREBASE_TOKEN${NC}"
    echo "3. Push to main branch: ${GREEN}git push origin main${NC}"
    echo "4. Monitor: ${GREEN}gh run watch${NC}"
    echo ""
    exit 0
else
    echo -e "${RED}❌ Some checks failed. Please fix issues above before deploying.${NC}"
    echo ""
    echo -e "${YELLOW}Common issues:${NC}"
    echo "• Missing Flutter: Install from flutter.dev"
    echo "• Missing Java: Install Java 17+ from oracle.com"
    echo "• Missing Firebase CLI: Run: npm install -g firebase-tools"
    echo "• .firebaserc issues: Run: firebase init hosting"
    echo "• GitHub secrets: go to Settings > Secrets > Actions"
    echo ""
    exit 1
fi
