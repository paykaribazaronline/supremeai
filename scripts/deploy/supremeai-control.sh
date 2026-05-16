#!/bin/bash

# SupremeAI Multi-Tool Control Script
# Handles building, running, and deploying various components.

# Text Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Environment Variables
SA_FILE="$(pwd)/service-account.json"
if [ -f "$SA_FILE" ]; then
    export GOOGLE_APPLICATION_CREDENTIALS="$SA_FILE"
else
    echo -e "${YELLOW}[WARNING] service-account.json not found. Some cloud operations may fail.${NC}"
fi

show_menu() {
    echo -e "${BLUE}====================================================${NC}"
    echo -e "${BLUE}       SupremeAI Control Center (Linux/macOS)       ${NC}"
    echo -e "${BLUE}====================================================${NC}"
    echo "1) Build IntelliJ/Android Studio Plugin"
    echo "2) Build VS Code Extension"
    echo "3) Run Backend (Local Gradle bootRun)"
    echo "4) Build Backend JAR (skip tests)"
    echo "5) Deploy to Firebase (Functions)"
    echo "6) Deploy to Google Cloud (Build & Cloud Run)"
    echo "7) Check Cloud Run Service Status"
    echo "8) Build All Plugins (IntelliJ + VS Code)"
    echo "0) Exit"
    echo -e "${BLUE}----------------------------------------------------${NC}"
    echo -n "Select an option [0-8]: "
}

build_intellij() {
    echo -e "\n${BLUE}Building IntelliJ Plugin...${NC}"
    ./gradlew :supremeai-intellij-plugin:buildPlugin
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}[SUCCESS] Plugin built at: supremeai-intellij-plugin/build/distributions/${NC}"
    else
        echo -e "${RED}[ERROR] IntelliJ build failed.${NC}"
    fi
}

build_vscode() {
    echo -e "\n${BLUE}Building VS Code Extension...${NC}"
    cd supremeai-vscode-extension || { echo -e "${RED}Folder not found!${NC}"; return; }
    npm install && npm run compile
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}[SUCCESS] VS Code extension compiled.${NC}"
        if npx vsce --version &> /dev/null; then
            npx vsce package
            echo -e "${GREEN}[SUCCESS] Extension packaged (.vsix) in folder.${NC}"
        fi
    else
        echo -e "${RED}[ERROR] VS Code build failed.${NC}"
    fi
    cd ..
}

run_backend() {
    echo -e "\n${BLUE}Starting SupremeAI Backend Locally...${NC}"
    # Kill existing process on 8080 if any
    fuser -k 8080/tcp 2>/dev/null
    ./gradlew bootRun
}

build_backend() {
    echo -e "\n${BLUE}Building Backend JAR...${NC}"
    ./gradlew clean bootJar -x test
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}[SUCCESS] JAR built at: build/libs/app.jar${NC}"
    else
        echo -e "${RED}[ERROR] Backend build failed.${NC}"
    fi
}

deploy_firebase() {
    echo -e "\n${BLUE}Deploying to Firebase...${NC}"
    cd functions || { echo -e "${RED}functions folder not found!${NC}"; return; }
    npm install && firebase deploy --only functions
    cd ..
}

deploy_gcloud() {
    echo -e "\n${BLUE}Deploying to Google Cloud (Build & Run)...${NC}"
    if [ ! -f "cloudbuild.yaml" ]; then
        echo -e "${RED}[ERROR] cloudbuild.yaml not found!${NC}"
        return
    fi
    
    # Check for project ID
    PROJECT_ID=$(gcloud config get-value project 2>/dev/null)
    if [ -z "$PROJECT_ID" ]; then
        echo -e "${YELLOW}[WARNING] No default GCP project set. Please enter Project ID:${NC}"
        read -r PROJECT_ID
    fi
    
    echo -e "${BLUE}Submitting build for project: ${YELLOW}$PROJECT_ID${NC}"
    gcloud builds submit --config cloudbuild.yaml --project "$PROJECT_ID" .
}

check_cloud_status() {
    echo -e "\n${BLUE}Checking Cloud Run Service Status...${NC}"
    gcloud run services list --region us-central1
}

while true; do
    show_menu
    read -r choice
    case $choice in
        1) build_intellij ;;
        2) build_vscode ;;
        3) run_backend ;;
        4) build_backend ;;
        5) deploy_firebase ;;
        6) deploy_gcloud ;;
        7) check_cloud_status ;;
        8) build_intellij; build_vscode ;;
        0) echo "Goodbye!"; exit 0 ;;
        *) echo -e "${RED}Invalid option!${NC}" ;;
    esac
    echo -e "\nPress Enter to return to menu..."
    read -r
done
