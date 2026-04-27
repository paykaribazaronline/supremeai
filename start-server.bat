@echo off
REM SupremeAI Local Server Start Script
REM This script automatically starts the local server with configuration

echo.
echo ========================================
echo   Starting SupremeAI Local Server
echo ========================================
echo.

cd smart_chat_system

echo Checking Python dependencies...
pip show psutil >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Installing psutil for system monitoring...
    pip install psutil
)

echo.
echo Starting Flask server...
echo.
echo Server will be available at:
echo   - Health Check: http://localhost:5000/health
echo   - API Status: http://localhost:5000/api/status
echo   - Main App: http://localhost:5000/
echo.
echo Press Ctrl+C to stop the server
echo.

REM Start Python server in background
start /B python run.py

REM Wait a moment for Python server to start
timeout /t 3 /nobreak >nul

REM Start Spring Boot server
echo Starting Spring Boot server...
call gradlew bootRun

cd ..
