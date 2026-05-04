@echo off
SETLOCAL EnableDelayedExpansion

:menu
cls
echo ====================================================
echo        SupremeAI Control Center (Windows)
echo ====================================================
echo 1) Build IntelliJ/Android Studio Plugin
echo 2) Build VS Code Extension
echo 3) Run Backend (Gradle bootRun)
echo 4) Build Backend (skip tests)
echo 5) Deploy to Firebase (Functions)
echo 6) Deploy to Google Cloud (GCloud Run)
echo 7) Build All Plugins (IntelliJ + VS Code)
echo 0) Exit
echo ====================================================
set /p choice="Select an option [0-7]: "

if "%choice%"=="1" goto build_intellij
if "%choice%"=="2" goto build_vscode
if "%choice%"=="3" goto run_backend
if "%choice%"=="4" goto build_backend
if "%choice%"=="5" goto deploy_firebase
if "%choice%"=="6" goto deploy_gcloud
if "%choice%"=="7" goto build_all
if "%choice%"=="0" exit /b 0

echo Invalid choice!
pause
goto menu

:build_intellij
echo Building IntelliJ Plugin...
call gradlew.bat :supremeai-intellij-plugin:buildPlugin
if %ERRORLEVEL% NEQ 0 echo [ERROR] Build failed!
pause
goto menu

:build_vscode
echo Building VS Code Extension...
cd supremeai-vscode-extension
call npm install && call npm run compile
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] VS Code extension compiled.
    call npx vsce --version >nul 2>&1
    if %ERRORLEVEL% EQU 0 call npx vsce package
) else (
    echo [ERROR] Build failed!
)
cd ..
pause
goto menu

:run_backend
echo Starting SupremeAI Backend...
set GOOGLE_APPLICATION_CREDENTIALS=%CD%\service-account.json
call gradlew.bat bootRun
pause
goto menu

:build_backend
echo Building Backend JAR...
call gradlew.bat clean build -x test
pause
goto menu

:deploy_firebase
echo Deploying to Firebase...
cd functions
call npm install && call firebase deploy --only functions
cd ..
pause
goto menu

:deploy_gcloud
echo Deploying to Google Cloud Run...
call gcloud builds submit --config cloudbuild.yaml .
pause
goto menu

:build_all
echo Building All Plugins...
call gradlew.bat :supremeai-intellij-plugin:buildPlugin
cd supremeai-vscode-extension
call npm install && call npm run compile
cd ..
echo Done.
pause
goto menu
