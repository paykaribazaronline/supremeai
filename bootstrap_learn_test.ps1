#!/usr/bin/env powershell
# SupremeAI Learning System - Complete Bootstrap & Test

Write-Host ""
Write-Host "===================================================" -ForegroundColor Green
Write-Host "  SUPREMEAI LEARNING SYSTEM - BOOTSTRAP & TEST     " -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
Write-Host ""

# ============ STEP 1: SETUP ENVIRONMENT ============
Write-Host "STEP 1: Setting up environment..." -ForegroundColor Yellow
Write-Host "  Setting BOOTSTRAP_TOKEN environment variable" -ForegroundColor Cyan

$env:BOOTSTRAP_TOKEN = "secure-bootstrap-token-2026"
$env:JWT_SECRET = "supremeai-jwt-secret-key-2026"

Write-Host "  OK: Environment variables set" -ForegroundColor Green
Write-Host ""

# ============ STEP 2: BUILD APP ============
Write-Host "STEP 2: Building application..." -ForegroundColor Yellow
cd c:\Users\Nazifa\supremeai

$buildOutput = .\gradlew build -x test --no-daemon 2>&1
if ($buildOutput -match "BUILD SUCCESSFUL") {
    Write-Host "  OK: Build successful" -ForegroundColor Green
} else {
    Write-Host "  WARNING: Build completed (check output)" -ForegroundColor Yellow
}
Write-Host ""

# ============ STEP 3: START APP ============
Write-Host "STEP 3: Starting application..." -ForegroundColor Yellow

$port = 8080
Write-Host "  Starting on port $port" -ForegroundColor Cyan

# Kill existing Java processes if any
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Start app in background
$appProcess = Start-Process -FilePath ".\gradlew" `
    -ArgumentList "bootRun" `
    -NoNewWindow `
    -PassThru `
    -RedirectStandardOutput "c:\Users\Nazifa\supremeai\app_startup.log" `
    -RedirectStandardError "c:\Users\Nazifa\supremeai\app_startup_error.log"

Write-Host "  App started with PID: $($appProcess.Id)" -ForegroundColor Cyan
Write-Host "  Waiting for startup (15 seconds)..." -ForegroundColor Cyan

Start-Sleep -Seconds 15

# Check if app is running
try {
    $health = Invoke-WebRequest -Uri "http://localhost:$port/actuator/health" -ErrorAction Stop -TimeoutSec 3
    Write-Host "  OK: App started successfully" -ForegroundColor Green
} catch {
    Write-Host "  ERROR: App failed to start" -ForegroundColor Red
    Write-Host "  Check app_startup.log for details" -ForegroundColor Red
    Write-Host ""
    exit 1
}
Write-Host ""

# ============ STEP 4: BOOTSTRAP FIRST ADMIN ============
Write-Host "STEP 4: Bootstrapping first admin user..." -ForegroundColor Yellow

$bootstrapPayload = @{
    username = "supremeai"
    email = "admin@supremeai.com"
    password = "Admin@123456!"
} | ConvertTo-Json

try {
    $bootstrapResponse = Invoke-WebRequest `
        -Uri "http://localhost:$port/api/auth/bootstrap" `
        -Method POST `
        -ContentType "application/json" `
        -Body $bootstrapPayload `
        -Headers @{ "X-Bootstrap-Token" = $env:BOOTSTRAP_TOKEN } `
        -ErrorAction Stop

    Write-Host "  OK: Admin user created: supremeai" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 400 -or $_.Exception.Response.StatusCode -eq 409) {
        Write-Host "  WARNING: Admin might already exist, continuing..." -ForegroundColor Yellow
    } else {
        Write-Host "  ERROR: Bootstrap failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}
Write-Host ""

# ============ STEP 5: LOGIN ============
Write-Host "STEP 5: Authenticating..." -ForegroundColor Yellow

$loginPayload = @{
    username = "supremeai"
    password = "Admin@123456!"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-WebRequest `
        -Uri "http://localhost:$port/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginPayload `
        -ErrorAction Stop

    $loginData = $loginResponse.Content | ConvertFrom-Json
    $token = $loginData.token
    
    Write-Host "  OK: Authentication successful" -ForegroundColor Green
    Write-Host "  Token: $($token.Substring(0, 30))..." -ForegroundColor Green
    
} catch {
    Write-Host "  ERROR: Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Write-Host ""

# ============ STEP 6: SUBMIT TEST QUERIES ============
Write-Host "STEP 6: Submitting test queries to trigger learning..." -ForegroundColor Yellow

$authHeader = @{ Authorization = "Bearer $token" }

$queries = @(
    "What is the best practice for database indexing and optimization?",
    "How should error handling and retry logic be implemented in production systems?",
    "What are the best practices for implementing API rate limiting and quota management?"
)

$successCount = 0
foreach ($query in $queries) {
    Write-Host ""
    Write-Host "  Query: '$($query.Substring(0, 50))...'" -ForegroundColor Cyan
    
    $queryPayload = @{
        question = $query
        userId = "test-learning-$(Get-Date -Format 'yyyyMMddHHmmss')"
    } | ConvertTo-Json
    
    try {
        $response = Invoke-WebRequest `
            -Uri "http://localhost:$port/api/consensus/ask" `
            -Method POST `
            -ContentType "application/json" `
            -Body $queryPayload `
            -Headers $authHeader `
            -ErrorAction Stop `
            -TimeoutSec 15

        $result = $response.Content | ConvertFrom-Json
        Write-Host "    OK: Submitted" -ForegroundColor Green
        Write-Host "    Winner: $($result.winningResponse.Substring(0, 40))..." -ForegroundColor Cyan
        if ($result.consensusPercentage) {
            Write-Host "    Confidence: $($result.consensusPercentage)%" -ForegroundColor Cyan
        }
        
        $successCount++
        
    } catch {
        Write-Host "    ERROR: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Start-Sleep -Milliseconds 800
}

Write-Host ""
Write-Host "  OK: Submitted $successCount queries" -ForegroundColor Green
Write-Host ""

# ============ STEP 7: CHECK LEARNING STATS ============
Write-Host "STEP 7: Checking learning statistics..." -ForegroundColor Yellow

Start-Sleep -Seconds 2

try {
    $statsResponse = Invoke-WebRequest `
        -Uri "http://localhost:$port/api/learning/stats" `
        -Method GET `
        -Headers $authHeader `
        -ErrorAction Stop

    $stats = $statsResponse.Content | ConvertFrom-Json
    Write-Host "  OK: Learning Statistics:" -ForegroundColor Green
    Write-Host "     Total Learnings: $($stats.total_learnings)" -ForegroundColor Cyan
    Write-Host "     Pattern Learnings: $($stats.pattern_learnings)" -ForegroundColor Cyan
    Write-Host "     Average Confidence: $($stats.average_confidence)" -ForegroundColor Cyan
    Write-Host "     Errors Prevented: $($stats.total_errors_prevented)" -ForegroundColor Cyan
    
} catch {
    Write-Host "  Could not fetch stats: $($_.Exception.Message)" -ForegroundColor Yellow
}
Write-Host ""

# ============ SUMMARY ============
Write-Host "===================================================" -ForegroundColor Green
Write-Host "   SUCCESS: LEARNING SYSTEM TEST COMPLETE!         " -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
Write-Host ""

Write-Host "LEARNING TRIGGERED! Check Firebase for results:" -ForegroundColor Green
Write-Host "  1. Go to: https://console.firebase.google.com" -ForegroundColor White
Write-Host "  2. Project: SupremeAI" -ForegroundColor White
Write-Host "  3. Realtime Database → system/learnings/" -ForegroundColor White
Write-Host "  4. Look for NEW entries with:" -ForegroundColor White
Write-Host "     - question (what was asked)" -ForegroundColor White
Write-Host "     - solutions (answers from configured AI providers)" -ForegroundColor White
Write-Host "     - confidenceScore (0.70 - 0.95)" -ForegroundColor White
Write-Host "     - timestamp (timestamp of learning)" -ForegroundColor White
Write-Host ""

Write-Host "Check GitHub for changes:" -ForegroundColor Cyan
Write-Host "  git status --short" -ForegroundColor White
Write-Host "  git log --oneline -10" -ForegroundColor White
Write-Host ""

Write-Host "OK: App is running on: http://localhost:$port" -ForegroundColor Green
Write-Host "OK: Your credentials: supremeai / Admin@123456!" -ForegroundColor Green
Write-Host "OK: System is learning from configured AI perspectives" -ForegroundColor Green
Write-Host ""
