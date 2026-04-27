 @echo off
REM ========================================
REM SupremeAI - Application Startup Script
REM ========================================
REM This script sets up the environment and starts the Spring Boot application

echo Starting SupremeAI Application...
echo.

REM Set environment variables
call set-env.bat

REM Check if Redis is running (optional but recommended)
echo Checking Redis connection...
timeout /t 2 /nobreak >nul

REM Start the Spring Boot application
echo.
echo Starting Spring Boot application with profile: %SPRING_PROFILES_ACTIVE%
echo.
gradlew.bat bootRun --args='--spring.profiles.active=%SPRING_PROFILES_ACTIVE%'

REM If the application exits, pause to see any error messages
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo Application failed to start!
    echo Error code: %ERRORLEVEL%
    echo ========================================
    echo.
    echo Common issues:
    echo 1. Redis is not running - Start Redis server
    echo 2. Missing API keys - Set GROQ_API_KEY, OPENAI_API_KEY, ANTHROPIC_API_KEY
    echo 3. Java version mismatch - Ensure Java 17 is installed
    echo 4. Port 8080 is already in use - Change PORT environment variable
    echo.
    pause
)
