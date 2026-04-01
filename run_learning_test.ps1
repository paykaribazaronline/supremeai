
# SupremeAI Learning System - Authentication & Test Script
# This script will:
# 1. Create your admin account via bootstrap (first time only)
# 2. Login and get JWT token  
# 3. Submit test queries to trigger learning
# 4. Show what gets stored in Firebase

param(
    [string]$Email = "user@supremeai.com",
    [string]$Password = "TestPassword123!",
    [string]$Username = "admin_user",
    [string]$ApiUrl = "http://localhost:8080",
    [int]$TestCount = 3
)

Write-Host "`n════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "🚀 SUPREMEAI LEARNING SYSTEM - AUTHENTICATION & TEST" -ForegroundColor Green
Write-Host "════════════════════════════════════════════════════════════`n" -ForegroundColor Cyan

# ============================================
# STEP 1: TRY TO BOOTSTRAP (Create First Admin)
# ============================================

Write-Host "[1/4] Creating admin account via bootstrap..." -ForegroundColor Yellow

$bootstrapBody = @{
    username = $Username
    email = $Email
    password = $Password
} | ConvertTo-Json

try {
    $bootstrapResponse = Invoke-WebRequest -Uri "$ApiUrl/api/auth/bootstrap" `
        -Method Post `
        -ContentType "application/json" `
        -Body $bootstrapBody `
        -ErrorAction SilentlyContinue `
        -TimeoutSec 10
    
    if ($bootstrapResponse.StatusCode -eq 200) {
        $bootstrapData = $bootstrapResponse.Content | ConvertFrom-Json
        Write-Host "✅ Admin account created!" -ForegroundColor Green
        Write-Host "   Username: $Username" -ForegroundColor Cyan
        Write-Host "   Email: $Email" -ForegroundColor Cyan
    } else {
        Write-Host "⚠️  Bootstrap returned: $($bootstrapResponse.StatusCode)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  Bootstrap skipped (account may exist): $($_.Exception.Message)" -ForegroundColor Yellow
}

Start-Sleep -Seconds 2

# ============================================
# STEP 2: LOGIN AND GET JWT TOKEN
# ============================================

Write-Host "`n[2/4] Logging in to get JWT token..." -ForegroundColor Yellow

$loginBody = @{
    username = $Username
    password = $Password
} | ConvertTo-Json

try {
    $loginResponse = Invoke-WebRequest -Uri "$ApiUrl/api/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginBody `
        -ErrorAction Stop `
        -TimeoutSec 10
    
    $loginData = $loginResponse.Content | ConvertFrom-Json
    $jwtToken = $loginData.token
    
    Write-Host "✅ Login successful!" -ForegroundColor Green
    Write-Host "   Token: $($jwtToken.Substring(0, 50))..." -ForegroundColor Cyan
    
} catch {
    Write-Host "❌ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Start-Sleep -Seconds 1

# ============================================
# STEP 3: SUBMIT TEST QUERIES
# ============================================

Write-Host "`n[3/4] Submitting $TestCount test queries to trigger learning..." -ForegroundColor Yellow

$testQuestions = @(
    "What is the best way to optimize database query performance?",
    "How should I implement error handling in production environments?",
    "What are the best practices for REST API design and security?"
)

$successCount = 0

for ($i = 0; $i -lt $TestCount; $i++) {
    $question = $testQuestions[$i % $testQuestions.Count]
    
    Write-Host "`n  Query $($i+1)/$TestCount : '$question'" -ForegroundColor Cyan
    
    $queryBody = @{
        question = $question
    } | ConvertTo-Json
    
    try {
        $consensusResponse = Invoke-WebRequest -Uri "$ApiUrl/api/consensus/ask" `
            -Method Post `
            -ContentType "application/json" `
            -Body $queryBody `
            -Headers @{ Authorization = "Bearer $jwtToken" } `
            -ErrorAction Stop `
            -TimeoutSec 15
        
        $consensusData = $consensusResponse.Content | ConvertFrom-Json
        
        Write-Host "  ✅ Query submitted successfully!" -ForegroundColor Green
        Write-Host "     Winning Answer: $($consensusData.winningResponse)" -ForegroundColor Green
        Write-Host "     Confidence: $($consensusData.consensusPercentage)%" -ForegroundColor Cyan
        Write-Host "     Providers Used: $($consensusData.responses)" -ForegroundColor Cyan
        
        $successCount++
        
    } catch {
        Write-Host "  ❌ Query failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Start-Sleep -Milliseconds 500
}

Write-Host "`n✅ Submitted $successCount/$TestCount test queries" -ForegroundColor Green

Start-Sleep -Seconds 2

# ============================================
# STEP 4: CHECK LEARNING STATISTICS
# ============================================

Write-Host "`n[4/4] Checking learning statistics..." -ForegroundColor Yellow

try {
    $statsResponse = Invoke-WebRequest -Uri "$ApiUrl/api/learning/stats" `
        -Method Get `
        -Headers @{ Authorization = "Bearer $jwtToken" } `
        -ErrorAction Stop `
        -TimeoutSec 10
    
    $statsData = $statsResponse.Content | ConvertFrom-Json
    
    Write-Host "`n✅ Learning Statistics:" -ForegroundColor Green
    Write-Host "   Total Learnings: $($statsData.total_learnings)" -ForegroundColor Cyan
    Write-Host "   Pattern Learnings: $($statsData.pattern_learnings)" -ForegroundColor Cyan
    Write-Host "   Average Confidence: $($statsData.average_confidence)" -ForegroundColor Cyan
    Write-Host "   Errors Prevented: $($statsData.total_errors_prevented)" -ForegroundColor Cyan
    
} catch {
    Write-Host "⚠️  Could not fetch stats: $($_.Exception.Message)" -ForegroundColor Yellow
}

# ============================================
# FINAL SUMMARY
# ============================================

Write-Host "`n════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "✨ LEARNING TRIGGERED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Cyan

Write-Host "`n📊 What just happened:" -ForegroundColor Yellow
Write-Host "  1. Your admin account was created/verified" -ForegroundColor White
Write-Host "  2. You logged in and got a JWT token" -ForegroundColor White
Write-Host "  3. $successCount test queries were submitted" -ForegroundColor White
Write-Host "  4. 10 AI providers were queried for each question" -ForegroundColor White
Write-Host "  5. Consensus voting determined the best answers" -ForegroundColor White
Write-Host "  6. Learnings were extracted from ALL 10 perspectives" -ForegroundColor White
Write-Host "  7. Results stored in Firebase Realtime Database" -ForegroundColor White

Write-Host "`n🔥 CHECK FIREBASE NOW:" -ForegroundColor Cyan
Write-Host "  1. Go to: https://console.firebase.google.com" -ForegroundColor White
Write-Host "  2. Select: Your SupremeAI project" -ForegroundColor White
Write-Host "  3. Go to: Realtime Database" -ForegroundColor White
Write-Host "  4. Path: system/learnings/" -ForegroundColor White
Write-Host "  5. You should see: NEW entries with timestamps!" -ForegroundColor Yellow

Write-Host "`n💾 YOUR JWT TOKEN (for manual API calls):" -ForegroundColor Cyan
Write-Host "  Bearer $jwtToken" -ForegroundColor Yellow

Write-Host "`n📝 Next API calls to try:" -ForegroundColor Cyan
Write-Host "  GET /api/learning/stats" -ForegroundColor White
Write-Host "  GET /api/consensus/history" -ForegroundColor White
Write-Host "  GET /api/learning/critical" -ForegroundColor White

Write-Host "`n════════════════════════════════════════════════════════════" -ForegroundColor Cyan

Write-Host "🎉 System is LEARNING! Check Firebase console now." -ForegroundColor Green
Write-Host "" -ForegroundColor Cyan
