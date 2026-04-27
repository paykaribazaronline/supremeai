@echo off
REM SupremeAI Auto Deploy Script for Windows
REM This script automates deployment process for Firebase Functions and Local Server

echo.
echo ========================================
echo   SupremeAI Auto Deployment Script
echo ========================================
echo.

REM Check if Firebase CLI is installed
where firebase >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Firebase CLI is not installed.
    echo Please install it first: npm install -g firebase-tools
    pause
    exit /b 1
)

REM Check if Python is installed
where python >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Python is not installed.
    echo Please install Python first.
    pause
    exit /b 1
)

REM Deploy Firebase Functions
echo.
echo [Step 1/2] Deploying Firebase Functions...
echo.
cd functions
echo Installing dependencies...
call npm install
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to install dependencies
    cd ..
    pause
    exit /b 1
)

echo.
echo Deploying functions...
call firebase deploy --only functions
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to deploy functions
    cd ..
    pause
    exit /b 1
)
cd ..

REM Install Python dependencies for local server
echo.
echo [Step 2/2] Setting up Local Server...
echo.
cd smart_chat_system
echo Installing Python dependencies...
call pip install -r requirements.txt
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to install Python dependencies
    cd ..
    pause
    exit /b 1
)
cd ..

echo.
echo ========================================
echo   Deployment Complete!
echo ========================================
echo.
echo Next steps:
echo   1. Start local server: cd smart_chat_system ^&^& python app.py
echo   2. Access admin dashboard at: https://your-project.firebaseapp.com/admin-dashboard.html
echo.
pause
