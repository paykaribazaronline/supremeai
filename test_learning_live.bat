@echo off
REM SupremeAI Learning System - Live API Test
REM This script submits test queries and shows what gets learned

setlocal enabledelayedexpansion

echo ════════════════════════════════════════════════════════════
echo SUPREMEAI LEARNING SYSTEM - API TEST
echo ════════════════════════════════════════════════════════════
echo.

set ApiUrl=http://localhost:8080
set QueryCount=3

REM Test 1: API Health Check
echo [1/4] Checking API health...
curl -s -o nul -w "HTTP Response: %%{http_code}\n" %ApiUrl%/actuator/health

if !errorlevel! neq 0 (
    echo ERROR: Cannot connect to API on %ApiUrl%
    echo Make sure the app is running: gradlew bootRun
    pause
    exit /b 1
)

echo SUCCESS: API is responding
echo.

REM Test 2: Submit test query 1
echo [2/4] Submitting test query 1...
set query1="How should I optimize database query performance?"

powershell -Command "
\$body = @{ question = 'How should I optimize database query performance?' } | ConvertTo-Json
\$response = Invoke-WebRequest -Uri '%ApiUrl%/api/consensus/ask' -Method Post -ContentType 'application/json' -Body \$body -ErrorAction SilentlyContinue
if (\$response.StatusCode -eq 200) {
    \$data = \$response.Content | ConvertFrom-Json
    Write-Host 'Query 1 Results:' -ForegroundColor Green
    Write-Host '  Winning Answer: '$data.winningResponse
    Write-Host '  Confidence: '$data.consensusPercentage'%%'
}
"

timeout /t 2 /nobreak > nul

REM Test 3: Submit test query 2
echo.
echo [3/4] Submitting test query 2...
set query2="What is the best error handling pattern?"

powershell -Command "
\$body = @{ question = 'What is the best error handling pattern?' } | ConvertTo-Json
\$response = Invoke-WebRequest -Uri '%ApiUrl%/api/consensus/ask' -Method Post -ContentType 'application/json' -Body \$body -ErrorAction SilentlyContinue
if (\$response.StatusCode -eq 200) {
    \$data = \$response.Content | ConvertFrom-Json
    Write-Host 'Query 2 Results:' -ForegroundColor Green
    Write-Host '  Winning Answer: '$data.winningResponse
    Write-Host '  Confidence: '$data.consensusPercentage'%%'
}
"

timeout /t 2 /nobreak > nul

REM Test 4: Check learning stats
echo.
echo [4/4] Checking learning statistics...

powershell -Command "
\$response = Invoke-WebRequest -Uri '%ApiUrl%/api/learning/stats' -Method Get -ErrorAction SilentlyContinue
if (\$response.StatusCode -eq 200) {
    \$stats = \$response.Content | ConvertFrom-Json
    Write-Host 'Learning Stats:' -ForegroundColor Green
    Write-Host '  Total Learnings: '$stats.total_learnings
    Write-Host '  Error Learnings: '$stats.error_learnings
    Write-Host '  Pattern Learnings: '$stats.pattern_learnings
    Write-Host '  Average Confidence: '$stats.average_confidence
}
"

echo.
echo ════════════════════════════════════════════════════════════
echo NEXT STEPS:
echo ════════════════════════════════════════════════════════════
echo.
echo 1. Check Firebase Console for new learnings:
echo    Navigate to: https://console.firebase.google.com
echo    Path: system/learnings/
echo.
echo 2. Check GitHub for changes:
echo    git status --short
echo    git log --oneline -10
echo.
echo 3. Check in-memory cache:
echo    curl %ApiUrl%/api/learning/stats
echo.
echo ════════════════════════════════════════════════════════════
pause
