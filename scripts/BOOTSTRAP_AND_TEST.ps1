#!/usr/bin/env powershell
# SupremeAI Learning System - Complete Bootstrap & Test

Write-Host "`n" -ForegroundColor Green
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "   SUPREMEAI LEARNING SYSTEM - BOOTSTRAP & TEST             " -ForegroundColor Green
Write-Host "═══════════════════════════════════════════════════════════`n" -ForegroundColor Green

# ============ STEP 1: SETUP ENVIRONMENT ============
Write-Host "STEP 1: Setting up environment..." -ForegroundColor Yellow
Write-Host "  Setting BOOTSTRAP_TOKEN environment variable" -ForegroundColor Cyan

if (-not $env:BOOTSTRAP_TOKEN) {
    Write-Host "  ⚠️ BOOTSTRAP_TOKEN not set; using temporary value for this run" -ForegroundColor Yellow
    $env:BOOTSTRAP_TOKEN = "dev-bootstrap-token-$(Get-Date -Format 'yyyyMMddHHmmss')"
}
if (-not $env:JWT_SECRET) {
    Write-Host "  ⚠️ JWT_SECRET not set; generating temporary value for this run" -ForegroundColor Yellow
    $env:JWT_SECRET = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes((New-Guid).Guid + (New-Guid).Guid))
}

$adminEmail = if ($env:SUPREMEAI_ADMIN_EMAIL) { $env:SUPREMEAI_ADMIN_EMAIL } else { "admin@supremeai.com" }
$bootstrapUsername = (($adminEmail -replace '@.*$','') -replace '[^a-zA-Z0-9._-]','_')
$adminPassword = $env:SUPREMEAI_ADMIN_PASSWORD
$firebaseApiKey = if ($env:SUPREMEAI_FIREBASE_WEB_API_KEY) { $env:SUPREMEAI_FIREBASE_WEB_API_KEY } else { "AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8" }

if (-not $adminPassword) {
    Write-Host "  ❌ SUPREMEAI_ADMIN_PASSWORD is required" -ForegroundColor Red
    Write-Host '  Set it before running: $env:SUPREMEAI_ADMIN_PASSWORD="<your-admin-password>"' -ForegroundColor Yellow
    exit 1
}

Write-Host "  ✅ Environment variables set`n" -ForegroundColor Green

# ============ STEP 2: BUILD APP ============
Write-Host "STEP 2: Building application..." -ForegroundColor Yellow
cd c:\Users\Nazifa\supremeai

$buildOutput = .\gradlew build -x test --no-daemon 2>&1
if ($buildOutput -match "BUILD SUCCESSFUL") {
    Write-Host "  ✅ Build successful`n" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  Build completed (check output)`n" -ForegroundColor Yellow
}

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
    Write-Host "  ✅ App started successfully`n" -ForegroundColor Green
} catch {
    Write-Host "  ❌ App failed to start" -ForegroundColor Red
    Write-Host "  Check app_startup.log for details`n" -ForegroundColor Red
    exit 1
}

# ============ STEP 4: BOOTSTRAP FIRST ADMIN ============
Write-Host "STEP 4: Bootstrapping first admin user..." -ForegroundColor Yellow

$bootstrapPayload = @{
    username = $bootstrapUsername
    email = $adminEmail
    password = $adminPassword
} | ConvertTo-Json

try {
    $bootstrapResponse = Invoke-WebRequest `
        -Uri "http://localhost:$port/api/auth/bootstrap" `
        -Method POST `
        -ContentType "application/json" `
        -Body $bootstrapPayload `
        -Headers @{ "X-Bootstrap-Token" = $env:BOOTSTRAP_TOKEN } `
        -ErrorAction Stop

    Write-Host "  ✅ Admin user created for email: $adminEmail`n" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 400 -or $_.Exception.Response.StatusCode -eq 409) {
        Write-Host "  ⚠️  Admin might already exist, continuing...`n" -ForegroundColor Yellow
    } else {
        Write-Host "  ❌ Bootstrap failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# ============ STEP 5: FIREBASE LOGIN ============
Write-Host "STEP 5: Authenticating..." -ForegroundColor Yellow

$firebaseLoginPayload = @{
    email = $adminEmail
    password = $adminPassword
    returnSecureToken = $true
} | ConvertTo-Json

try {
    $firebaseResponse = Invoke-WebRequest `
        -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$firebaseApiKey" `
        -Method POST `
        -ContentType "application/json" `
        -Body $firebaseLoginPayload `
        -ErrorAction Stop

    $firebaseData = $firebaseResponse.Content | ConvertFrom-Json
    if (-not $firebaseData.idToken) {
        throw "Firebase ID token not returned"
    }

    $exchangePayload = @{ idToken = $firebaseData.idToken } | ConvertTo-Json

    $loginResponse = Invoke-WebRequest `
        -Uri "http://localhost:$port/api/auth/firebase-login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $exchangePayload `
        -ErrorAction Stop

    $loginData = $loginResponse.Content | ConvertFrom-Json
    $token = $loginData.token
    
    Write-Host "  ✅ Authentication successful" -ForegroundColor Green
    Write-Host "  Token: $($token.Substring(0, 30))...`n" -ForegroundColor Green
    
} catch {
    Write-Host "  ❌ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

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
    Write-Host "`n  Query: '$($query.Substring(0, 50))...'" -ForegroundColor Cyan
    
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
        Write-Host "    ✅ Submitted" -ForegroundColor Green
        Write-Host "    Winner: $($result.winningResponse.Substring(0, 40))..." -ForegroundColor Cyan
        Write-Host "    Confidence: $($result.consensusPercentage)%" -ForegroundColor Cyan
        
        $successCount++
        
    } catch {
        Write-Host "    ❌ Failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Start-Sleep -Milliseconds 800
}

Write-Host "`n  ✅ Submitted $successCount queries`n" -ForegroundColor Green

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
    Write-Host "  ✅ Learning Statistics:" -ForegroundColor Green
    Write-Host "     Total Learnings: $($stats.total_learnings)" -ForegroundColor Cyan
    Write-Host "     Pattern Learnings: $($stats.pattern_learnings)" -ForegroundColor Cyan
    Write-Host "     Average Confidence: $($stats.average_confidence)" -ForegroundColor Cyan
    Write-Host "     Errors Prevented: $($stats.total_errors_prevented)`n" -ForegroundColor Cyan
    
} catch {
    Write-Host "  Could not fetch stats: $($_.Exception.Message)`n" -ForegroundColor Yellow
}

# ============ SUMMARY ============
Write-Host "===========================================================" -ForegroundColor Green
Write-Host "LEARNING SYSTEM TEST COMPLETE" -ForegroundColor Green
Write-Host "===========================================================`n" -ForegroundColor Green

Write-Host "LEARNING TRIGGERED. Check Firebase for results:" -ForegroundColor Green
Write-Host "  1. Go to: https://console.firebase.google.com" -ForegroundColor White
Write-Host "  2. Project: SupremeAI" -ForegroundColor White
Write-Host "  3. Realtime Database -> system/learnings/" -ForegroundColor White
Write-Host "  4. Look for new entries with question, solutions, confidenceScore, and timestamp`n" -ForegroundColor White

Write-Host "Check GitHub for changes:" -ForegroundColor Cyan
Write-Host "  git status --short" -ForegroundColor White
Write-Host "  git log --oneline -10`n" -ForegroundColor White

Write-Host "App is running on: http://localhost:$port" -ForegroundColor Green
Write-Host "Admin email used: $adminEmail" -ForegroundColor Green
Write-Host "System is learning from configured AI perspectives" -ForegroundColor Green
Write-Host ""
