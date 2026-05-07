#!/bin/bash

echo "===================================================="
echo "SupremeAI Plugin Build Script (Linux/macOS)"
echo "===================================================="

# 1. Build IntelliJ / Android Studio Plugin
echo -e "\n[1/2] Building IntelliJ Plugin..."
echo "----------------------------------------------------"
./gradlew :supremeai-intellij-plugin:buildPlugin

if [ $? -eq 0 ]; then
    echo -e "\033[0;32m[SUCCESS] IntelliJ Plugin built: supremeai-intellij-plugin/build/distributions/\033[0m"
else
    echo -e "\033[0;31m[ERROR] IntelliJ Plugin build failed.\033[0m"
    exit 1
fi

# 2. Build VS Code Extension
echo -e "\n[2/2] Building VS Code Extension..."
echo "----------------------------------------------------"
cd supremeai-vscode-extension

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo "[ERROR] npm is not installed. Skipping VS Code build."
    cd ..
    exit 1
fi

npm install
if [ $? -ne 0 ]; then
    echo "[ERROR] VS Code npm install failed."
    cd ..
    exit 1
fi

npm run compile
if [ $? -ne 0 ]; then
    echo "[ERROR] VS Code compilation failed."
    cd ..
    exit 1
fi

# Optional: Package VS Code extension
if npx vsce --version &> /dev/null; then
    echo "Packaging VS Code extension..."
    npx vsce package
    echo -e "\033[0;32m[SUCCESS] VS Code Extension packaged in supremeai-vscode-extension/\033[0m"
else
    echo "[INFO] vsce not found, skipping packaging. Compiled files are in supremeai-vscode-extension/out/"
fi

cd ..

echo -e "\n===================================================="
echo -e "\033[0;32mALL BUILDS COMPLETED SUCCESSFULLY\033[0m"
echo "===================================================="
