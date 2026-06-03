#!/usr/bin/env powershell
# SupremeAI Test App - Triggers Learning System
# This script demonstrates the actual learning happening in real-time

param(
    [string]$ApiUrl = "http://localhost:8080",
    [int]$TestCount = 5
)

Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "🚀 SUPREMEAI LEARNING SYSTEM - LIVE TEST APP" -ForegroundColor Green
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan

Write-Host "`n[INFO] Starting test queries to trigger learning system..." -ForegroundColor Yellow
Write-Host "[INFO] These queries will be sent to the configured AI providers" -ForegroundColor Yellow
Write-Host "[INFO] Results will be stored in Firebase" -ForegroundColor Yellow

# Test queries that will trigger learning
$testQueries = @(
    "What is the best way to optimize database queries?",
    "How should I implement error handling in production code?",
    "What are the best practices for API rate limiting?",
    "How to design a resilient microservices architecture?",
    "What security measures are essential for authentication?"
)

$results = @()
$successCount = 0
$failCount = 0

# Test 1: Check if API is responding
Write-Host "`n[TEST 1/3] Checking API Health..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-WebRequest -Uri "$ApiUrl/actuator/health" -Method Get -ErrorAction SilentlyContinue -TimeoutSec 5
    if ($healthResponse.StatusCode -eq 200) {
        Write-Host "✅ API is responding on $ApiUrl" -ForegroundColor Green
        $successCount++
    }
} catch {
    Write-Host "❌ API not responding: $_" -ForegroundColor Red
    $failCount++
    exit 1
}

# Test 2: Submit consensus queries
Write-Host "`n[TEST 2/3] Submitting $TestCount test queries..." -ForegroundColor Yellow

for ($i = 0; $i -lt $TestCount; $i++) {
    $query = $testQueries[$i % $testQueries.Count]
    
    Write-Host "`n  Query $($i+1)/$TestCount: '$query'" -ForegroundColor Cyan
    
    try {
        $body = @{
            question = $query
            userId = "test-user-$((Get-Date).Ticks)"
        } | ConvertTo-Json
        
        $response = Invoke-WebRequest -Uri "$ApiUrl/api/consensus/ask" `
            -Method Post `
            -ContentType "application/json" `
            -Body $body `
            -ErrorAction SilentlyContinue `
            -TimeoutSec 10
        
        if ($response.StatusCode -eq 200) {
            $data = $response.Content | ConvertFrom-Json
            Write-Host "  ✅ Query submitted successfully" -ForegroundColor Green
            Write-Host "    - Winning Answer: $($data.winningResponse)" -ForegroundColor Cyan
            Write-Host "    - Confidence: $($data.consensusPercentage)%" -ForegroundColor Cyan
            Write-Host "    - Providers Queried: $($data.providerCount)" -ForegroundColor Cyan
            
            $results += $data
            $successCount++
        }
    } catch {
        Write-Host "  ⚠️  Query failed: $($_.Exception.Message)" -ForegroundColor Yellow
        $failCount++
    }
    
    Start-Sleep -Milliseconds 500
}

# Test 3: Check learning stats
Write-Host "`n[TEST 3/3] Checking Learning Statistics..." -ForegroundColor Yellow
try {
    $statsResponse = Invoke-WebRequest -Uri "$ApiUrl/api/learning/stats" -Method Get -ErrorAction SilentlyContinue -TimeoutSec 5
    if ($statsResponse.StatusCode -eq 200) {
        $stats = $statsResponse.Content | ConvertFrom-Json
        Write-Host "✅ Learning stats retrieved:" -ForegroundColor Green
        Write-Host "  - Total Learnings: $($stats.total_learnings)" -ForegroundColor Cyan
        Write-Host "  - Error Learnings: $($stats.error_learnings)" -ForegroundColor Cyan
        Write-Host "  - Pattern Learnings: $($stats.pattern_learnings)" -ForegroundColor Cyan
        Write-Host "  - Average Confidence: $($stats.average_confidence)" -ForegroundColor Cyan
        Write-Host "  - Errors Prevented: $($stats.total_errors_prevented)" -ForegroundColor Cyan
        $successCount++
    }
} catch {
    Write-Host "⚠️  Could not retrieve stats: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Summary
Write-Host "`n════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "📊 TEST RESULTS" -ForegroundColor Green
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Cyan

Write-Host "`n✅ Successful Requests: $successCount" -ForegroundColor Green
Write-Host "❌ Failed Requests: $failCount" -ForegroundColor $(if ($failCount -gt 0) { "Red" } else { "Green" })
Write-Host "`nℹ️  Next Steps:" -ForegroundColor Cyan
Write-Host "  1. Check Firebase Realtime Database" -ForegroundColor White
Write-Host "  2. Navigate to: system/learnings" -ForegroundColor White
Write-Host "  3. Should see new learning records with:" -ForegroundColor White
Write-Host "     - question (what was asked)" -ForegroundColor White
Write-Host "     - solutions (answers from configured AIs)" -ForegroundColor White
Write-Host "     - confidenceScore (0-1 range)" -ForegroundColor White
Write-Host "     - timestamp (when learned)" -ForegroundColor White

Write-Host "`n  4. Check GitHub for auto-commits:" -ForegroundColor White
Write-Host "     git log --oneline -10 (should show SystemLearning updates)" -ForegroundColor White

Write-Host "`n════════════════════════════════════════════════════════`n" -ForegroundColor Cyan

if ($successCount -gt 0) {
    Write-Host "🎉 LEARNING SYSTEM IS WORKING!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "⚠️  No successful API calls - check if server is running" -ForegroundColor Yellow
    exit 1
}
