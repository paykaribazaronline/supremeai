#!/bin/bash

# SupremeAI Multi-Tool Control Script
# Handles building, running, and deploying various components.

# Text Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Environment Variables
export GOOGLE_APPLICATION_CREDENTIALS="$(pwd)/service-account.json"

show_menu() {
    echo -e "${BLUE}====================================================${NC}"
    echo -e "${BLUE}       SupremeAI Control Center (Linux/macOS)       ${NC}"
    echo -e "${BLUE}====================================================${NC}"
    echo "1) Build IntelliJ/Android Studio Plugin"
    echo "2) Build VS Code Extension"
    echo "3) Run Backend (Gradle bootRun)"
    echo "4) Build Backend (skip tests)"
    echo "5) Deploy to Firebase (Functions)"
    echo "6) Deploy to Google Cloud (GCloud Run)"
    echo "7) Build All Plugins (IntelliJ + VS Code)"
    echo "0) Exit"
    echo -e "${BLUE}----------------------------------------------------${NC}"
    echo -n "Select an option [0-7]: "
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
    echo -e "\n${BLUE}Starting SupremeAI Backend...${NC}"
    # Use the service account for local runs
    export GOOGLE_APPLICATION_CREDENTIALS="$(pwd)/service-account.json"
    ./gradlew bootRun
}

build_backend() {
    echo -e "\n${BLUE}Building Backend JAR...${NC}"
    ./gradlew clean build -x test
}

deploy_firebase() {
    echo -e "\n${BLUE}Deploying to Firebase...${NC}"
    cd functions || { echo -e "${RED}functions folder not found!${NC}"; return; }
    npm install && firebase deploy --only functions
    cd ..
}

deploy_gcloud() {
    echo -e "\n${BLUE}Deploying to Google Cloud Run...${NC}"
    # Using the project ID found in cloudbuild.yaml
    gcloud builds submit --config cloudbuild.yaml .
}

while true; do
    show_menu
    read choice
    case $choice in
        1) build_intellij ;;
        2) build_vscode ;;
        3) run_backend ;;
        4) build_backend ;;
        5) deploy_firebase ;;
        6) deploy_gcloud ;;
        7) build_intellij; build_vscode ;;
        0) echo "Goodbye!"; exit 0 ;;
        *) echo -e "${RED}Invalid option!${NC}" ;;
    esac
    echo -e "\nPress Enter to return to menu..."
    read
done
