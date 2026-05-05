# ========================================
# SupremeAI - Application Startup Script (PowerShell)
# ========================================
# This script sets up the environment and starts the Spring Boot application

Write-Host "Starting SupremeAI Application..." -ForegroundColor Green
Write-Host ""

# Set environment variables
& .\set-env.ps1

# Check if Redis is running (optional but recommended)
Write-Host "Checking Redis connection..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

# Start the Spring Boot application
Write-Host ""
Write-Host "Starting Spring Boot application with profile: $($env:SPRING_PROFILES_ACTIVE)" -ForegroundColor Cyan
Write-Host ""

& .\gradlew.bat bootRun --args="--spring.profiles.active=$($env:SPRING_PROFILES_ACTIVE)"

# If the application exits, pause to see any error messages
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "Application failed to start!" -ForegroundColor Red
    Write-Host "Error code: $LASTEXITCODE" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "1. Redis is not running - Start Redis server"
    Write-Host "2. Missing API keys - Set GROQ_API_KEY, OPENAI_API_KEY, ANTHROPIC_API_KEY"
    Write-Host "3. Java version mismatch - Ensure Java 17 is installed"
    Write-Host "4. Port 8080 is already in use - Change PORT environment variable"
    Write-Host ""
    Read-Host "Press Enter to exit"
}
