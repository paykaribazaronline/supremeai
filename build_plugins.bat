@echo off
SETLOCAL EnableDelayedExpansion

echo ====================================================
echo SupremeAI Plugin Build Script
echo ====================================================

:: 1. Build IntelliJ / Android Studio Plugin
echo.
echo [1/2] Building IntelliJ Plugin...
echo ----------------------------------------------------
call gradlew.bat :supremeai-intellij-plugin:buildPlugin
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] IntelliJ Plugin build failed.
    goto :error
)
echo [SUCCESS] IntelliJ Plugin built: supremeai-intellij-plugin/build/distributions/

:: 2. Build VS Code Extension
echo.
echo [2/2] Building VS Code Extension...
echo ----------------------------------------------------
cd supremeai-vscode-extension
call npm install
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] VS Code npm install failed.
    cd ..
    goto :error
)
call npm run compile
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] VS Code compilation failed.
    cd ..
    goto :error
)

:: Optional: Package VS Code extension if vsce is available
echo Checking for vsce to package extension...
call npx vsce --version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Packaging VS Code extension...
    call npx vsce package
    echo [SUCCESS] VS Code Extension packaged: supremeai-vscode-extension/*.vsix
) else (
    echo [INFO] vsce not found, skipping packaging. Compiled files are in supremeai-vscode-extension/out/
)

cd ..

echo.
echo ====================================================
echo ALL BUILDS COMPLETED SUCCESSFULLY
echo ====================================================
echo IntelliJ Plugin: supremeai-intellij-plugin/build/distributions/
echo VS Code Extension: supremeai-vscode-extension/
echo ====================================================
pause
exit /b 0

:error
echo.
echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
echo BUILD FAILED
echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
pause
exit /b 1
