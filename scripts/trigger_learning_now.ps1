#!/usr/bin/env powershell
# SupremeAI Learning Trigger Script
# This ACTUALLY submits queries to trigger learning on Firebase

param(
    [int]$QueryCount = 3,
    [int]$WaitSeconds = 2
)

Write-Host "`n" -ForegroundColor Green
Write-Host "🚀 SUPREMEAI LEARNING TRIGGER" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host "`n"

$ApiUrl = "http://localhost:8080"
$questions = @(
    "What is the best way to optimize database queries?",
    "How should I implement error handling in production?",
    "What are the best practices for API rate limiting?",
    "How to design a resilient microservices architecture?",
    "What security measures are essential for authentication?"
)

# Test 1: Check API
Write-Host "[Step 1] Checking if API is running..." -ForegroundColor Yellow
try {
    $health = Invoke-WebRequest -Uri "$ApiUrl/actuator/health" -Method Get -TimeoutSec 3 -ErrorAction Stop
    if ($health.StatusCode -eq 200) {
        Write-Host "✅ API IS RUNNING on $ApiUrl`n" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ API NOT RUNNING. App may have crashed." -ForegroundColor Red
    Write-Host "Please run: .\gradlew bootRun" -ForegroundColor Yellow
    exit 1
}

# Test 2: Submit queries
Write-Host "[Step 2] Submitting $QueryCount test queries...`n" -ForegroundColor Yellow

$results = @()

for ($i = 1; $i -le $QueryCount; $i++) {
    $question = $questions[($i - 1) % $questions.Count]
    
    Write-Host "Query $i/$QueryCount: `"$question`"" -ForegroundColor Cyan
    
    try {
        $body = @{
            question = $question
            userId = "tester-$(Get-Random)"
        } | ConvertTo-Json
        
        $response = Invoke-WebRequest `
            -Uri "$ApiUrl/api/consensus/ask" `
            -Method Post `
            -ContentType "application/json" `
            -Body $body `
            -TimeoutSec 30 `
            -ErrorAction Stop
        
        if ($response.StatusCode -eq 200) {
            $data = $response.Content | ConvertFrom-Json
            Write-Host "  ✅ SUCCESS" -ForegroundColor Green
            Write-Host "     Answer: `"$($data.winningResponse)`"" -ForegroundColor Green
            Write-Host "     Confidence: $($data.consensusPercentage)%" -ForegroundColor Green
            Write-Host "     Providers: $($data.providerCount)" -ForegroundColor Green
            
            $results += $data
        }
    } catch {
        Write-Host "  ❌ FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    if ($i -lt $QueryCount) {
        Start-Sleep -Seconds $WaitSeconds
    }
}

# Test 3: Get learning stats
Write-Host "`n[Step 3] Checking learning statistics..." -ForegroundColor Yellow

try {
    $stats = Invoke-WebRequest `
        -Uri "$ApiUrl/api/learning/stats" `
        -Method Get `
        -TimeoutSec 10 `
        -ErrorAction Stop
    
    if ($stats.StatusCode -eq 200) {
        $statsData = $stats.Content | ConvertFrom-Json
        Write-Host "`n✅ LEARNING STATS:" -ForegroundColor Green
        Write-Host "   Total Learnings: $($statsData.total_learnings)" -ForegroundColor Green
        Write-Host "   Error Learnings: $($statsData.error_learnings)" -ForegroundColor Green
        Write-Host "   Pattern Learnings: $($statsData.pattern_learnings)" -ForegroundColor Green
        Write-Host "   Average Confidence: $($statsData.average_confidence)" -ForegroundColor Green
        Write-Host "   Errors Prevented: $($statsData.total_errors_prevented)" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️  Could not get stats: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Summary
Write-Host "`n" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Cyan
Write-Host "📊 SUMMARY" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host "`nQueries submitted: $($results.Count)" -ForegroundColor Cyan

if ($results.Count -gt 0) {
    Write-Host "`n✅ LEARNING TRIGGERED!" -ForegroundColor Green
    Write-Host "`nNow check Firebase to see the learnings:" -ForegroundColor Yellow
    Write-Host "  1. Go to: https://console.firebase.google.com" -ForegroundColor White
    Write-Host "  2. Select: supremeai project" -ForegroundColor White
    Write-Host "  3. Navigate to: Realtime Database" -ForegroundColor White
    Write-Host "  4. Expand: system/learnings/" -ForegroundColor White
    Write-Host "  5. Should see NEW entries with each AI perspective!" -ForegroundColor White
    Write-Host "`n⏱️  Wait 10-15 seconds for Firebase to sync..." -ForegroundColor Yellow
} else {
    Write-Host "`n❌ No queries succeeded.`n" -ForegroundColor Red
}

Write-Host "`n================================`n" -ForegroundColor Cyan
