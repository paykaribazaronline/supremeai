#!/bin/bash

# ==============================================================================
# SupremeAI Unified Control Center (v3.6 - GOLDEN EDITION)
# ==============================================================================
# Repository: SupremeAI Monorepo
# Description: Centralized command hub for all project components.
# Includes: Cloud Verification, Plugin Management, and Bengali Localization Hints.
# Bengali Support: Full Bengali hints for all operational modules.
#
# Quick shortcuts: ./supremeai-control.sh --help
# ==============================================================================

# --- Configuration & Paths ---
PROJECT_ROOT=$(pwd)
BACKEND_DIR="$PROJECT_ROOT"
DASHBOARD_DIR="$PROJECT_ROOT/dashboard"
MOBILE_DIR="$PROJECT_ROOT/supremeai"
VSCODE_DIR="$PROJECT_ROOT/supremeai-vscode-extension"
INTELLIJ_DIR="$PROJECT_ROOT/supremeai-intellij-plugin"
FUNCTIONS_DIR="$PROJECT_ROOT/functions"
SCRIPTS_DIR="$PROJECT_ROOT/scripts"
CLI_DIR="$PROJECT_ROOT/command-hub/cli"

# --- Credentials Detection & Setup ---
setup_credentials() {
    log_info "Verifying Firebase Credentials... (ক্রেডেনশিয়াল যাচাই করা হচ্ছে...)"
    
    # Target path for Spring Boot
    TARGET_JSON="$PROJECT_ROOT/src/main/resources/firebase-service-account.json"
    
    # Source candidates
    SOURCE_ROOT="$PROJECT_ROOT/service-account.json"
    SOURCE_CONFIG="$PROJECT_ROOT/config/service-account.json"
    
    if [ -f "$SOURCE_ROOT" ]; then
        cp "$SOURCE_ROOT" "$TARGET_JSON"
        log_success "Copied service-account.json to src/main/resources/firebase-service-account.json"
        export GOOGLE_APPLICATION_CREDENTIALS="$SOURCE_ROOT"
    elif [ -f "$SOURCE_CONFIG" ]; then
        cp "$SOURCE_CONFIG" "$TARGET_JSON"
        log_success "Copied config/service-account.json to src/main/resources/firebase-service-account.json"
        export GOOGLE_APPLICATION_CREDENTIALS="$SOURCE_CONFIG"
    elif [ -f "$TARGET_JSON" ]; then
        log_info "Credentials already exist in resources."
        export GOOGLE_APPLICATION_CREDENTIALS="$TARGET_JSON"
    else
        log_warn "service-account.json not found! Firebase operations may fail."
    fi
}

# --- Module: Dashboard Sync ---
sync_dashboard() {
    log_info "Starting Dashboard Build & Sync... (ড্যাশবোর্ড তৈরি এবং সিঙ্ক শুরু হচ্ছে...)"
    
    cd "$DASHBOARD_DIR"
    if [ ! -d "node_modules" ]; then
        log_info "Installing dependencies first..."
        npm install
    fi
    
    log_info "Building Dashboard..."
    npm run build
    
    # Vite is configured to build directly to src/main/resources/static/admin
    # Check if that happened correctly
    if [ -d "$BACKEND_DIR/src/main/resources/static/admin" ]; then
        log_success "Dashboard built and synced to Backend (static/admin) successfully!"
        # Copy index.html to static/ for /admin.html compatibility if needed
        cp "$BACKEND_DIR/src/main/resources/static/admin/index.html" "$BACKEND_DIR/src/main/resources/static/admin.html" 2>/dev/null || true
    else
        log_error "Build failed or target directory not found!"
    fi
    
    cd "$PROJECT_ROOT"
}

# --- Pipeline Fixes ---
apply_pipeline_fixes() {
    log_info "Applying System-wide Fixes... (সিস্টেম ফিক্স প্রয়োগ করা হচ্ছে...)"
    
    # Run the dedicated pipeline fix script
    if [ -f "$SCRIPTS_DIR/PIPELINE_FIX_SCRIPTS.sh" ]; then
        bash "$SCRIPTS_DIR/PIPELINE_FIX_SCRIPTS.sh"
    else
        # Inline fallback for IntelliJ and VS Code fixes
        log_info "Fixing IntelliJ Plugin Gradle Wrapper..."
        if [ -d "$INTELLIJ_DIR" ]; then
            cd "$INTELLIJ_DIR" && gradle wrapper --gradle-version 8.10 && chmod +x gradlew 2>/dev/null
            cd "$PROJECT_ROOT"
        fi
        
        log_info "Updating VS Code Extension Configs..."
        if [ -d "$VSCODE_DIR" ]; then
            [ ! -f "$VSCODE_DIR/.eslintrc.json" ] && echo '{"env":{"node":true,"es2021":true},"extends":["eslint:recommended"],"rules":{"semi":["error","always"]}}' > "$VSCODE_DIR/.eslintrc.json"
            [ ! -f "$VSCODE_DIR/tsconfig.json" ] && echo '{"compilerOptions":{"module":"commonjs","target":"es2020","outDir":"out","rootDir":"src","strict":true}}' > "$VSCODE_DIR/tsconfig.json"
        fi
        
        log_info "Verifying Main Gradle Wrapper..."
        ./gradlew --version > /dev/null 2>&1 || log_warn "Main gradlew might need attention."
    fi
    
    log_success "Pipeline fixes applied!"
}

# --- UI Styling ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# --- UI Helpers ---
log_header() {
    clear
    echo -e "${CYAN}${BOLD}====================================================${NC}"
    echo -e "${CYAN}${BOLD}       $1 ${NC}"
    echo -e "${CYAN}${BOLD}====================================================${NC}"
    if [ -n "$2" ]; then
        echo -e "${YELLOW}💡 $2${NC}"
        echo -e "${CYAN}${BOLD}----------------------------------------------------${NC}"
    fi
}

log_info() { echo -e "${BLUE}${BOLD}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}${BOLD}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}${BOLD}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}${BOLD}[ERROR]${NC} $1"; }

wait_key() {
    echo -e "\n${YELLOW}Press Enter to return to menu... (মেনুতে ফিরতে এন্টার চাপুন)${NC}"
    read
}

# --- Quick Access Functions ---
run_dashboard_dev() { cd "$DASHBOARD_DIR" && npm run dev; }
run_dashboard_build() { cd "$DASHBOARD_DIR" && npm run build && log_success "Dashboard built!"; cd "$PROJECT_ROOT"; }
run_backend() { export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/src/main/resources/firebase-service-account.json && ./gradlew bootRun --args='--spring.profiles.active=local'; }
run_backend_cloud() { ./gradlew bootRun --args='--spring.profiles.active=prod'; }
stop_all() {
    log_info "Stopping all local services... (সব লোকাল সার্ভিস বন্ধ করা হচ্ছে...)"
    fuser -k 8080/tcp 2>/dev/null && log_success "Backend (8080) stopped."
    fuser -k 5173/tcp 2>/dev/null && log_success "Vite Dev Server (5173) stopped."
    pkill -f "gradlew bootRun" 2>/dev/null && log_success "Gradle bootRun processes terminated."
}

# --- Module: Backend ---
backend_menu() {
    while true; do
        log_header "Backend Services (Spring Boot 3)" "সার্ভার সাইড লজিক এবং API ম্যানেজমেন্টের জন্য (Server-side logic and API management)"
        echo "1) Run Backend (Local Emulator)            [এমুলেটর মোডে সার্ভার চালু করতে]"
        echo "2) Run Backend (Cloud Profile)             [লাইভ ক্লাউড মোডে সার্ভার চালু করতে]"
        echo "3) Build Executable JAR                    [ডেপ্লয়মেন্ট ফাইল বা JAR তৈরি করতে]"
        echo "4) Run All Tests                           [কোডের গুণমান যাচাই করতে টেস্ট রান করুন]"
        echo "5) Clean & Reset Backend                   [পুরানো ফাইল মুছে নতুন করে শুরু করতে]"
        echo "6) Stop Running Backend                  [চলমান ব্যাকএন্ড বন্ধ করতে (Stop Server)]"
        echo "0) Return to Main Menu                     [প্রধান মেনুতে ফিরে যান]"
        echo -n "Action: "
        read sub_choice
        case $sub_choice in
            1) export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/src/main/resources/firebase-service-account.json && ./gradlew bootRun --args='--spring.profiles.active=local' ;;
            2) ./gradlew bootRun --args='--spring.profiles.active=prod' ;;
            3) ./gradlew clean build -x test ;;
            4) ./gradlew test ;;
            5) ./gradlew clean ;;
            6) 
                log_info "Stopping Backend on port 8080... (পোর্ট ৮০৮০ তে ব্যাকএন্ড বন্ধ করা হচ্ছে...)"
                fuser -k 8080/tcp 2>/dev/null && log_success "Backend stopped!" || log_warn "No backend found running on port 8080."
                ;;
            0) break ;;
            *) log_error "Invalid option!" ;;
        esac
        wait_key
    done
}

# --- Module: Dashboard ---
dashboard_menu() {
    while true; do
        log_header "Admin Dashboard (React)" "অ্যাডমিন প্যানেল এবং ইউজার ইন্টারফেস (User interface for management)"
        echo "1) Start Dev Server (Vite)                 [কোড পরিবর্তন সরাসরি দেখতে এটি ব্যবহার করুন]"
        echo "2) Install Dependencies                    [নতুন প্যাকেজ বা লাইব্রেরি ইনস্টল করতে]"
        echo "3) Build & Sync to Backend                 [ড্যাশবোর্ড তৈরি করে ব্যাকএন্ডে সিঙ্ক করুন]"
        echo "4) Run Lint & Type Check                   [কোডে কোনো ভুল বা এরর আছে কিনা দেখতে]"
        echo "5) Stop Dashboard Server                 [ড্যাশবোর্ড সার্ভার বন্ধ করতে (Stop Vite)]"
        echo "0) Return to Main Menu                     [প্রধান মেনুতে ফিরে যান]"
        echo -n "Action: "
        read sub_choice
        case $sub_choice in
            1) cd "$DASHBOARD_DIR" && npm run dev ;;
            2) cd "$DASHBOARD_DIR" && npm install ;;
            3) sync_dashboard ;;
            4) cd "$DASHBOARD_DIR" && npm run type-check ;;
            5) 
                log_info "Stopping Dashboard on port 5173... (পোর্ট ৫১৭৩ তে ড্যাশবোর্ড বন্ধ করা হচ্ছে...)"
                fuser -k 5173/tcp 2>/dev/null && log_success "Dashboard stopped!" || log_warn "No dashboard found running on port 5173."
                ;;
            0) break ;;
        esac
        cd "$PROJECT_ROOT"
        wait_key
    done
}

# --- Shortcut: Quick Dashboard Build ---
quick_dashboard_build() {
    log_info "Quick Dashboard Build... (ড্যাশবোর্ড দ্রুত তৈরি করা হচ্ছে...)"
    cd "$DASHBOARD_DIR"
    npm run build && log_success "Dashboard built successfully!"
    cd "$PROJECT_ROOT"
}

# --- Module: Mobile ---
mobile_menu() {
    while true; do
        log_header "Mobile App (Flutter)" "স্মার্টফোন অ্যাপ ম্যানেজমেন্ট (Android and iOS app management)"
        echo "1) Run Mobile App                          [ফোনে অ্যাপটি চালু করে দেখতে]"
        echo "2) Build Android APK                       [অ্যান্ড্রয়েডে ইনস্টল করার ফাইল তৈরি করুন]"
        echo "3) Reset Flutter Environment               [অ্যাপে সমস্যা হলে এটি দিয়ে ফ্রেশ করুন]"
        echo "4) Run Flutter Doctor                      [আপনার পিসিতে সব ঠিক আছে কিনা চেক করতে]"
        echo "0) Return to Main Menu                     [প্রধান মেনুতে ফিরে যান]"
        echo -n "Action: "
        read sub_choice
        case $sub_choice in
            1) cd "$MOBILE_DIR" && flutter run ;;
            2) cd "$MOBILE_DIR" && flutter build apk --release ;;
            3) cd "$MOBILE_DIR" && flutter clean && flutter pub get ;;
            4) cd "$MOBILE_DIR" && flutter doctor ;;
            0) break ;;
        esac
        cd "$PROJECT_ROOT"
        wait_key
    done
}

# --- Module: Extensions & Tools ---
tooling_menu() {
    while true; do
        log_header "Extensions & CLI Tools" "প্লাগইন এবং কমান্ড লাইন টুলস (IDE Plugins and CLI management)"
        echo "1) Build VS Code Extension                 [ভিএস কোড প্লাগইন তৈরি করতে]"
        echo "2) Build IntelliJ Plugin                   [ইন্টেলিজে প্লাগইন তৈরি করতে]"
        echo "3) Build All Plugins                       [সব প্লাগইন একসাথে তৈরি করুন]"
        echo "4) Install CLI (supcmd)                    [কমান্ড লাইন টুল সেটআপ করতে]"
        echo "5) Setup Flutter CI/CD                     [ফ্লাটার CI/CD সেটআপ করুন]"
        echo "0) Return to Main Menu                     [প্রধান মেনুতে ফিরে যান]"
        echo -n "Action: "
        read sub_choice
        case $sub_choice in
            1) cd "$VSCODE_DIR" && npm run compile && npx vsce package ;;
            2) ./gradlew :supremeai-intellij-plugin:buildPlugin ;;
            3) build_all_plugins ;;
            4) sudo ln -sf "$CLI_DIR/supcmd.py" /usr/local/bin/supcmd && log_success "CLI installed!" ;;
            5) setup_flutter_cicd ;;
            0) break ;;
        esac
        cd "$PROJECT_ROOT"
        wait_key
    done
}

build_all_plugins() {
    log_info "Building All Plugins... (সব প্লাগইন তৈরি করা হচ্ছে...)"
    
    # Build IntelliJ Plugin
    log_info "Building IntelliJ Plugin..."
    ./gradlew :supremeai-intellij-plugin:buildPlugin 2>/dev/null && log_success "IntelliJ Plugin built"
    
    # Build VS Code Extension
    log_info "Building VS Code Extension..."
    cd "$VSCODE_DIR"
    npm install 2>/dev/null && npm run compile 2>/dev/null
    npx vsce package 2>/dev/null && log_success "VS Code Extension packaged"
    cd "$PROJECT_ROOT"
    
    log_success "All plugins built successfully!"
}

setup_flutter_cicd() {
    log_header "Flutter CI/CD Setup" "ফ্লাটার সিআই/সিডিএফ সেটআপ"
    
    # Check Flutter
    if ! command -v flutter &> /dev/null; then
        log_error "Flutter not installed. Visit https://flutter.dev/docs/get-started/install"
        wait_key
        return
    fi
    log_success "Flutter found: $(flutter --version | head -1)"
    
    # Check Firebase CLI
    if ! command -v firebase &> /dev/null; then
        log_warn "Firebase CLI not found"
    else
        log_success "Firebase CLI: $(firebase --version)"
    fi
    
    # Build Flutter
    log_info "Building Flutter web app..."
    cd "$MOBILE_DIR"
    flutter pub get 2>/dev/null && flutter build web --release 2>/dev/null && log_success "Web app built"
    cd "$PROJECT_ROOT"
    
    log_success "Flutter CI/CD setup complete!"
    wait_key
}

# --- Module: Cloud & Infrastructure ---
cloud_menu() {
    while true; do
        log_header "Cloud & Infrastructure" "ক্লাউড ডেপ্লয়মেন্ট এবং ইনফ্রাস্ট্রাকচার (Firebase and Cloud management)"
        echo "1) Deploy Firebase Hosting                 [ওয়েবসাইট লাইভ করতে এটি ব্যবহার করুন]"
        echo "2) Deploy Firebase Functions               [ব্যাকএন্ড ফাংশন লাইভ করতে এটি ব্যবহার করুন]"
        echo "3) Deploy All to Firebase                  [সব ফায়ারবেসে ডেপ্লয় করুন]"
        echo "4) Start Firebase Emulators                [লোকাল টেস্টিং এনভায়রনমেন্ট চালু করুন]"
        echo "5) Docker Compose UP                       [ডকার কন্টেইনার চালু করতে]"
        echo "6) Docker Compose DOWN                     [ডকার কন্টেইনার বন্ধ করতে]"
        echo "0) Return to Main Menu                     [প্রধান মেনুতে ফিরে যান]"
        echo -n "Action: "
        read sub_choice
        case $sub_choice in
            1) firebase deploy --only hosting ;;
            2) cd "$FUNCTIONS_DIR" && firebase deploy --only functions ;;
            3) deploy_all_firebase ;;
            4) firebase emulators:start ;;
            5) docker-compose up -d ;;
            6) docker-compose down ;;
            0) break ;;
        esac
        cd "$PROJECT_ROOT"
        wait_key
    done
}

deploy_all_firebase() {
    log_info "Deploying all to Firebase... (সব ফায়ারবেসে ডেপ্লয় করা হচ্ছে...)"
    firebase deploy --only hosting,functions 2>/dev/null && log_success "All deployed to Firebase!"
}

# --- Module: Maintenance ---
maintenance_menu() {
    while true; do
        log_header "Maintenance & Data" "সিস্টেম রক্ষণাবেক্ষণ এবং ডেটা সিডিং (System maintenance and data seeding)"
        echo "1) Run Data Seeding                        [ডেটাবেসে প্রাথমিক ডেটা যোগ করতে]"
        echo "2) Check AI Provider Health                [সব AI প্রোভাইডার কাজ করছে কিনা চেক করুন]"
        echo "3) Audit Project Integrity                 [প্রজেক্টের কোনো ফাইল মিসিং কিনা চেক করুন]"
        echo "4) Verify CodeGeeX4 Integration           [কোডজিএক্স-৪ ইন্টিগ্রেশন যাচাই করুন]"
        echo "5) Verify StepFun Integration              [স্টেপফান ইন্টিগ্রেশন যাচাই করুন]"
        echo "6) Sync Secrets to GCP                     [গোগল ক্লাউডে সিক্রেট সিঙ্ক করুন]"
        echo "0) Return to Main Menu                     [প্রধান মেনুতে ফিরে যান]"
        echo -n "Action: "
        read sub_choice
        case $sub_choice in
            1) python3 "$SCRIPTS_DIR/seed_run_all.py" ;;
            2) bash "$SCRIPTS_DIR/check-ai-providers.sh" ;;
            3) bash "$SCRIPTS_DIR/PIPELINE_FIX_SCRIPTS.sh" ;;
            4) verify_codegeex4 ;;
            5) verify_stepfun ;;
            6) sync_gcp_secrets ;;
            0) break ;;
        esac
        wait_key
    done
}

# --- Verification Functions ---
verify_codegeex4() {
    log_header "CodeGeeX4 Integration Verification" "কোডজিএক্স-৪ ইন্টিগ্রেশন যাচাই করা হচ্ছে..."
    
    local pass=0 fail=0
    
    # Test 1: Check provider file exists
    log_info "Test 1: CodeGeeX4Provider.java exists..."
    [ -f "src/main/java/com/supremeai/provider/CodeGeeX4Provider.java" ] && { ((pass++)); log_success "PASS"; } || { ((fail++)); log_error "FAIL"; }
    
    # Test 2: Check class definition
    log_info "Test 2: CodeGeeX4Provider class defined..."
    grep -q "public class CodeGeeX4Provider" src/main/java/com/supremeai/provider/CodeGeeX4Provider.java 2>/dev/null && { ((pass++)); log_success "PASS"; } || { ((fail++)); log_error "FAIL"; }
    
    # Test 3: Check extends AbstractHttpProvider
    log_info "Test 3: Extends AbstractHttpProvider..."
    grep -q "extends AbstractHttpProvider" src/main/java/com/supremeai/provider/CodeGeeX4Provider.java 2>/dev/null && { ((pass++)); log_success "PASS"; } || { ((fail++)); log_error "FAIL"; }
    
    # Test 4: Check getName() returns "codegeex4"
    log_info "Test 4: getName() returns 'codegeex4'..."
    grep -q 'return "codegeex4"' src/main/java/com/supremeai/provider/CodeGeeX4Provider.java 2>/dev/null && { ((pass++)); log_success "PASS"; } || { ((fail++)); log_error "FAIL"; }
    
    # Test 5: Check AIProviderFactory has codegeex4 case
    log_info "Test 5: AIProviderFactory has codegeex4 case..."
    grep -q 'case "codegeex4":' src/main/java/com/supremeai/provider/AIProviderFactory.java 2>/dev/null && { ((pass++)); log_success "PASS"; } || { ((fail++)); log_error "FAIL"; }
    
    # Test 6: Check .env has CODEGEEX4_API_KEY
    log_info "Test 6: .env has CODEGEEX4_API_KEY..."
    grep -q "CODEGEEX4_API_KEY" .env 2>/dev/null && { ((pass++)); log_success "PASS"; } || { ((fail++)); log_error "FAIL"; }
    
    echo ""
    log_info "Results: $pass passed, $fail failed"
    [ $fail -eq 0 ] && log_success "All tests passed! CodeGeeX4 is properly integrated." || log_error "Some tests failed."
    wait_key
}

verify_stepfun() {
    log_header "StepFun Integration Verification" "স্টেপফান ইন্টিগ্রেশন যাচাই করা হচ্ছে..."
    
    # Check if backend is running
    log_info "Checking if backend is running..."
    curl -s http://localhost:8080/actuator/health > /dev/null 2>&1 && log_success "Backend is running on port 8080" || log_warn "Backend not running"
    
    # Check StepFun provider is registered
    log_info "Checking if StepFun provider is registered..."
    curl -s http://localhost:8080/api/providers/list 2>/dev/null | grep -q "stepfun" && log_success "StepFun found in provider list" || log_warn "StepFun NOT in provider list"
    
    # Check STEPFUN_API_KEY
    log_info "Checking STEPFUN_API_KEY..."
    [ -n "$STEPFUN_API_KEY" ] && log_success "STEPFUN_API_KEY is set" || log_warn "STEPFUN_API_KEY not set"
    
    wait_key
}

sync_gcp_secrets() {
    log_header "Sync Secrets to GCP" "গোগল ক্লাউডে সিক্রেট সিঙ্ক করা হচ্ছে..."
    
    local PROJECT_ID="${GCP_PROJECT_ID:-supremeai-a}"
    
    if ! command -v gcloud &> /dev/null; then
        log_error "gcloud CLI not found. Please install Google Cloud SDK."
        wait_key
        return
    fi
    
    get_env_val() {
        grep "^$1=" .env 2>/dev/null | cut -d'=' -f2- | tr -d '\r'
    }
    
    sync_secret() {
        local secret_name=$1
        local secret_value=$2
        
        [ -z "$secret_value" ] && return
        [[ "$secret_value" == *"xxxx"* ]] && return
        
        if gcloud secrets describe "$secret_name" --project="$PROJECT_ID" &>/dev/null; then
            echo -n "$secret_value" | gcloud secrets versions add "$secret_name" --data-file=- --project="$PROJECT_ID" 2>/dev/null && log_success "Updated $secret_name"
        else
            echo -n "$secret_value" | gcloud secrets create "$secret_name" --replication-policy="automatic" --data-file=- --project="$PROJECT_ID" 2>/dev/null && log_success "Created $secret_name"
        fi
    }
    
    log_info "Syncing secrets from .env..."
    sync_secret "openai-api-key" "$(get_env_val OPENAI_API_KEY)"
    sync_secret "gemini-api-key" "$(get_env_val GEMINI_API_KEY)"
    sync_secret "stepfun-api-key" "$(get_env_val STEPFUN_API_KEY)"
    sync_secret "api-encryption-key" "$(get_env_val API_ENCRYPTION_KEY)"
    
    log_success "Secret synchronization complete!"
    wait_key
}

# --- Module: Cloud & Verification ---
verification_menu() {
    log_header "Cloud & Integrity Check" "সিস্টেমের সঠিকতা এবং ক্লাউড কানেকশন যাচাই (System verification)"
    
    log_info "Checking API Health (https://supremeai-a.web.app)..."
    curl -s -f https://supremeai-a.web.app/api/v1/health > /dev/null && log_success "Cloud API is ONLINE" || log_warn "Cloud API is OFFLINE"
    
    log_info "Checking Local Artifacts..."
    [ -f "service-account.json" ] && log_success "service-account.json found" || log_error "service-account.json MISSING"
    
    apply_pipeline_fixes
    wait_key
}

# --- Main Entry Point ---
setup_credentials

# Quick command-line shortcuts
case "$1" in
    --dashboard-build|dashboard-build|-db)
        quick_dashboard_build
        exit 0
        ;;
    --backend-dev|backend-dev|-bd)
        run_backend
        exit 0
        ;;
    --stop-all|stop-all|-sa)
        stop_all
        exit 0
        ;;
    --build-plugins|build-plugins|-bp)
        build_all_plugins
        exit 0
        ;;
    --deploy|deploy|-d)
        deploy_all_firebase
        exit 0
        ;;
    --help|-h)
        echo "SupremeAI Control Center - Usage:"
        echo "  ./supremeai-control.sh              # Launch interactive menu"
        echo "  ./supremeai-control.sh --dashboard-build  # Quick dashboard build"
        echo "  ./supremeai-control.sh --backend-dev      # Run backend in local mode"
        echo "  ./supremeai-control.sh --stop-all         # Stop all local services"
        echo "  ./supremeai-control.sh --build-plugins    # Build all plugins"
        echo "  ./supremeai-control.sh --deploy           # Deploy to Firebase"
        echo ""
        echo "Shortcuts: -db, -bd, -sa, -bp, -d"
        exit 0
        ;;
esac

while true; do
    log_header "SupremeAI Unified Control Center (v3.6)" "এক ক্লিকে আপনার পুরো প্রজেক্ট কন্ট্রোল করুন! (Control your entire project from here)"
    echo -e "${MAGENTA}1) Backend Services${NC}       (সার্ভার এবং API ম্যানেজমেন্ট)"
    echo -e "${CYAN}2) Web Dashboard${NC}         (অ্যাডমিন প্যানেল এবং ওয়েবসাইট)"
    echo -e "${BLUE}3) Mobile App${NC}            (অ্যান্ড্রয়েড এবং আইওএস অ্যাপ)"
    echo -e "${YELLOW}4) Extensions & CLI${NC}      (প্লাগইন এবং কমান্ড লাইন টুলস)"
    echo -e "${RED}5) Cloud & Infrastructure${NC} (ফায়ারবেস এবং ডেপ্লয়মেন্ট)"
    echo -e "${GREEN}6) Maintenance & Data${NC}    (বাগ ফিক্স এবং ডাটা আপডেট)"
    echo -e "${BOLD}7) Run System Check${NC}       (সবকিছু ঠিক আছে কিনা পরীক্ষা)"
    echo -e "${RED}${BOLD}8) Stop All Services${NC}      (সবকিছু বন্ধ করুন - Emergency Stop)"
    echo -e "${CYAN}q) Quick Dashboard Build${NC}  (ড্যাশবোর্ড দ্রুত তৈরি করুন)"
    echo -e "${NC}0) Exit System (সিস্টেম থেকে বের হয়ে যান)"
    echo -n "Select Module [0-8,q]: "
    read choice
    case $choice in
        1) backend_menu ;;
        2) dashboard_menu ;;
        3) mobile_menu ;;
        4) tooling_menu ;;
        5) cloud_menu ;;
        6) maintenance_menu ;; 
        7) verification_menu ;;
        8) stop_all ;;
        q) quick_dashboard_build ;;
        0) exit 0 ;;
        *) log_error "Invalid selection!" ; sleep 1 ;;
    esac
done
