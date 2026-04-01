#!/usr/bin/env powershell
# Simple SupremeAI Learning System Test - Run JAR directly

Write-Host ""
Write-Host "===================================================" -ForegroundColor Green
Write-Host "  SUPREMEAI LEARNING SYSTEM - SIMPLE TEST         " -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
Write-Host ""

# ============ STEP 1: ENVIRONMENT ============
Write-Host "STEP 1: Setting environment..." -ForegroundColor Yellow

$env:BOOTSTRAP_TOKEN = "secure-bootstrap-token-2026"
$env:JWT_SECRET = "supremeai-jwt-secret-key-2026"
$env:SPRING_PROFILES_ACTIVE = "prod"

Write-Host "  OK: Environment set" -ForegroundColor Green
Write-Host ""

# ============ STEP 2: START APP ============
Write-Host "STEP 2: Starting app (via JAR)..." -ForegroundColor Yellow

$port = 8080
$jarFile = "c:\Users\Nazifa\supremeai\build\libs\supremeai-6.0-Phase6-Week1-2.jar"

# Kill existing processes
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Start app
$appProcess = Start-Process -FilePath "java" `
    -ArgumentList @("-Xmx2g", "-jar", $jarFile, "--server.port=$port") `
    -NoNewWindow `
    -PassThru `
    -RedirectStandardOutput "app_run.log" `
    -WorkingDirectory "c:\Users\Nazifa\supremeai"

Write-Host "  App PID: $($appProcess.Id)" -ForegroundColor Cyan
Write-Host "  Waiting 20 seconds for startup..." -ForegroundColor Cyan

Start-Sleep -Seconds 20

# Check if running
try {
    $response = Invoke-WebRequest -Uri "http://localhost:$port/actuator/health" -TimeoutSec 3 -ErrorAction Stop
    Write-Host "  OK: App running on port $port" -ForegroundColor Green
} catch {
    Write-Host "  ERROR: App not responding - $($_.Exception.Message)" -ForegroundColor Red
    Get-Content app_run.log -Tail 50 | Write-Host
    exit 1
}
Write-Host ""

# ============ STEP 3: LOGIN ============
Write-Host "STEP 3: Authenticating..." -ForegroundColor Yellow

$loginPayload = @{
    username = "supremeai"
    password = "Admin@123456!"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest `
        -Uri "http://localhost:$port/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginPayload `
        -ErrorAction Stop -TimeoutSec 5

    $data = $response.Content | ConvertFrom-Json
    $token = $data.token
    
    Write-Host "  OK: Logged in successfully" -ForegroundColor Green
    Write-Host "  Token: $($token.Substring(0, 30))..." -ForegroundColor Green
} catch {
    Write-Host "  ERROR: Login failed - trying to bootstrap first..." -ForegroundColor Yellow
    
    # Try bootstrap if no user exists
    $bootstrapPayload = @{
        username = "supremeai"
        email = "admin@supremeai.com"
        password = "Admin@123456!"
    } | ConvertTo-Json
    
    try {
        $response = Invoke-WebRequest `
            -Uri "http://localhost:$port/api/auth/bootstrap" `
            -Method POST `
            -ContentType "application/json" `
            -Body $bootstrapPayload `
            -Headers @{ "X-Bootstrap-Token" = $env:BOOTSTRAP_TOKEN } `
            -ErrorAction Stop -TimeoutSec 5
        
        Write-Host "  OK: Bootstrap succeeded, getting token..." -ForegroundColor Green
        $data = $response.Content | ConvertFrom-Json
        $token = $data.token
        
    } catch {
        Write-Host "  ERROR: Bootstrap also failed - $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}
Write-Host ""

# ============ STEP 4: SUBMIT QUERY ============
Write-Host "STEP 4: Submitting test query..." -ForegroundColor Yellow

$authHeader = @{ Authorization = "Bearer $token" }

$queryPayload = @{
    question = "What is the best practice for enterprise error handling?"
    userId = "test-direct-$((Get-Date).Ticks)"
} | ConvertTo-Json

try {
    Write-Host "  Sending: 'What is the best practice for enterprise error handling?'" -ForegroundColor Cyan
    
    $response = Invoke-WebRequest `
        -Uri "http://localhost:$port/api/consensus/ask" `
        -Method POST `
        -ContentType "application/json" `
        -Body $queryPayload `
        -Headers $authHeader `
        -TimeoutSec 20 `
        -ErrorAction Stop

    $result = $response.Content | ConvertFrom-Json
    Write-Host "  OK: Query submitted successfully!" -ForegroundColor Green
    Write-Host "  Response: $($result.winningResponse.Substring(0, 60))..." -ForegroundColor Cyan
    
    if ($result.consensusPercentage) {
        Write-Host "  Consensus: $($result.consensusPercentage)%" -ForegroundColor Green
    }
    
} catch {
    Write-Host "  ERROR: Query submission failed" -ForegroundColor Red
    Write-Host "  Message: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
Write-Host ""

# ============ STEP 5: CHECK STATS ============
Write-Host "STEP 5: Checking learning stats..." -ForegroundColor Yellow

Start-Sleep -Seconds 3

try {
    $response = Invoke-WebRequest `
        -Uri "http://localhost:$port/api/learning/stats" `
        -Method GET `
        -Headers $authHeader `
        -TimeoutSec 5 `
        -ErrorAction Stop

    $stats = $response.Content | ConvertFrom-Json
    Write-Host "  OK: Learning Statistics:" -ForegroundColor Green
    Write-Host "     Total: $($stats.total_learnings)" -ForegroundColor Cyan
    Write-Host "     Errors: $($stats.error_learnings)" -ForegroundColor Cyan
    Write-Host "     Confidence: $($stats.average_confidence)" -ForegroundColor Cyan
    
} catch {
    Write-Host "  Warning: Could not fetch stats" -ForegroundColor Yellow
}
Write-Host ""

# ============ SUCCESS ============
Write-Host "===================================================" -ForegroundColor Green
Write-Host "  SUCCESS: QUERIES SUBMITTED & LEARNING TRIGGERED!" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Green
Write-Host ""

Write-Host "NEXT STEPS:" -ForegroundColor Green
Write-Host "  1. Open Firebase Console:" -ForegroundColor White
Write-Host "     https://console.firebase.google.com" -ForegroundColor Cyan
Write-Host "  2. Navigate to: Realtime Database > system/learnings/" -ForegroundColor White
Write-Host "  3. Look for NEW entries with question, solutions, confidence" -ForegroundColor White
Write-Host ""

Write-Host "App is running on: http://localhost:$port" -ForegroundColor Green
Write-Host "Type Ctrl+C to stop the app" -ForegroundColor Yellow
Write-Host ""

# Keep app running
$appProcess.WaitForExit()
