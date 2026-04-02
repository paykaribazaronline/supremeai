# SupremeAI Learning Trigger Script
param(
    [string]$Email = "admin@supremeai.com",
    [string]$Password = $env:SUPREMEAI_ADMIN_PASSWORD,
    [string]$ApiUrl = "http://localhost:8080",
    [int]$TestCount = 3
)

if (-not $Password) {
    Write-Host "❌ Admin password not provided." -ForegroundColor Red
    Write-Host "   Set SUPREMEAI_ADMIN_PASSWORD or pass -Password explicitly." -ForegroundColor Yellow
    exit 1
}

$firebaseApiKey = if ($env:SUPREMEAI_FIREBASE_WEB_API_KEY) { $env:SUPREMEAI_FIREBASE_WEB_API_KEY } else { "AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8" }
$bootstrapUsername = (($Email -replace '@.*$','') -replace '[^a-zA-Z0-9._-]','_')

Write-Host "SUPREMEAI LEARNING SYSTEM - AUTHENTICATION TEST" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan

# Step 1: Bootstrap
Write-Host "`nStep 1: Creating admin account..." -ForegroundColor Yellow
$bootstrapBody = @{
    username = $bootstrapUsername
    email = $Email
    password = $Password
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$ApiUrl/api/auth/bootstrap" -Method Post -ContentType "application/json" -Body $bootstrapBody -ErrorAction SilentlyContinue -TimeoutSec 10 -UseBasicParsing
    Write-Host "Admin account ready!" -ForegroundColor Green
} catch {
    Write-Host "Account exists (or other issue)" -ForegroundColor Yellow
}

Start-Sleep -Seconds 2

# Step 2: Firebase Login
Write-Host "`nStep 2: Logging in..." -ForegroundColor Yellow

$firebaseLoginBody = @{
    email = $Email
    password = $Password
    returnSecureToken = $true
} | ConvertTo-Json

try {
    $firebaseResponse = Invoke-WebRequest -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$firebaseApiKey" -Method Post -ContentType "application/json" -Body $firebaseLoginBody -ErrorAction Stop -TimeoutSec 10 -UseBasicParsing
    $firebaseData = $firebaseResponse.Content | ConvertFrom-Json
    if (-not $firebaseData.idToken) {
        throw "Firebase ID token not returned"
    }

    $exchangeBody = @{ idToken = $firebaseData.idToken } | ConvertTo-Json

    $loginResponse = Invoke-WebRequest -Uri "$ApiUrl/api/auth/firebase-login" -Method Post -ContentType "application/json" -Body $exchangeBody -ErrorAction Stop -TimeoutSec 10 -UseBasicParsing
    $loginData = $loginResponse.Content | ConvertFrom-Json
    $jwtToken = $loginData.token
    Write-Host "Login successful! Token obtained." -ForegroundColor Green
} catch {
    Write-Host "Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Submit queries
Write-Host "`nStep 3: Submitting $TestCount test queries..." -ForegroundColor Yellow
$questions = @(
    "What is the best way to optimize database queries?",
    "How should I handle errors in production?",
    "What are REST API best practices?"
)

for ($i = 0; $i -lt $TestCount; $i++) {
    $q = $questions[$i % $questions.Count]
    Write-Host "`n  Query $($i+1): $q" -ForegroundColor Cyan
    
    $queryBody = @{ question = $q } | ConvertTo-Json
    
    try {
        $queryResponse = Invoke-WebRequest -Uri "$ApiUrl/api/consensus/ask" -Method Post -ContentType "application/json" -Body $queryBody -Headers @{ Authorization = "Bearer $jwtToken" } -ErrorAction Stop -TimeoutSec 15 -UseBasicParsing
        $queryData = $queryResponse.Content | ConvertFrom-Json
        Write-Host "  Answer: $($queryData.winningResponse)" -ForegroundColor Green
        Write-Host "  Confidence: $($queryData.consensusPercentage)%" -ForegroundColor Cyan
    } catch {
        Write-Host "  Failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds 1
}

# Step 4: Check stats
Write-Host "`nStep 4: Checking learning statistics..." -ForegroundColor Yellow
try {
    $statsResponse = Invoke-WebRequest -Uri "$ApiUrl/api/learning/stats" -Method Get -Headers @{ Authorization = "Bearer $jwtToken" } -ErrorAction Stop -TimeoutSec 10 -UseBasicParsing
    $statsData = $statsResponse.Content | ConvertFrom-Json
    Write-Host "  Total Learnings: $($statsData.total_learnings)" -ForegroundColor Green
    Write-Host "  Confidence: $($statsData.average_confidence)" -ForegroundColor Cyan
} catch {
    Write-Host "  Could not fetch stats" -ForegroundColor Yellow
}

Write-Host "`nDONE! Check Firebase console at:" -ForegroundColor Green
Write-Host "https://console.firebase.google.com" -ForegroundColor Yellow
Write-Host "Path: system/learnings/" -ForegroundColor Yellow
Write-Host "`nYour JWT Token:" -ForegroundColor Cyan
Write-Host $jwtToken -ForegroundColor Yellow
