#!/usr/bin/env powershell
# Enterprise Resilience Endpoint Testing Suite

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "ENTERPRISE RESILIENCE ENDPOINT TESTS" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080/api/v1/resilience"
$testResults = @()

# Helper function
function Test-Endpoint {
    param(
        [string]$method,
        [string]$endpoint,
        [string]$description
    )
    
    Write-Host "`n[$method] $endpoint" -ForegroundColor Yellow
    Write-Host "Description: $description" -ForegroundColor Gray
    
    try {
        $uri = "$baseUrl$endpoint"
        $response = if ($method -eq "GET") {
            Invoke-WebRequest -Uri $uri -Method GET -ErrorAction Stop -TimeoutSec 5
        } else {
            Invoke-WebRequest -Uri $uri -Method POST -ErrorAction Stop -TimeoutSec 5
        }
        
        Write-Host "Response Status: $($response.StatusCode)" -ForegroundColor Green
        if ($response.Content.Length -gt 0) {
            $data = $response.Content | ConvertFrom-Json
            Write-Host "Response: $($data | ConvertTo-Json -Depth 2)" -ForegroundColor Cyan
        }
        
        return @{endpoint=$endpoint; status="PASS"; code=$response.StatusCode}
    } catch {
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Status Code: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
        return @{endpoint=$endpoint; status="FAIL"; error=$_.Exception.Message}
    }
}

# Test resilience health endpoints
Write-Host "`n========== RESILIENCE HEALTH ==========" -ForegroundColor Magenta

$testResults += Test-Endpoint "GET" "/health/status" "Get overall resilience health status"
$testResults += Test-Endpoint "GET" "/health/providers" "Get circuit breaker status for all providers"
$testResults += Test-Endpoint "GET" "/health/cache" "Get cache health status"
$testResults += Test-Endpoint "GET" "/health/database" "Get database connectivity status"

# Test circuit breaker endpoints
Write-Host "`n========== CIRCUIT BREAKERS ==========" -ForegroundColor Magenta

$testResults += Test-Endpoint "GET" "/circuit-breakers" "Get all circuit breakers status"
$testResults += Test-Endpoint "GET" "/circuit-breakers/openai" "Get OpenAI circuit breaker status"
$testResults += Test-Endpoint "GET" "/metrics" "Get circuit breaker metrics"

# Test failover endpoints
Write-Host "`n========== FAILOVER MANAGEMENT ==========" -ForegroundColor Magenta

$testResults += Test-Endpoint "GET" "/failover/providers" "Get provider failover chain"
$testResults += Test-Endpoint "GET" "/failover/status" "Get current failover status"

# Test resilience testing endpoints
Write-Host "`n========== TESTING ENDPOINTS ==========" -ForegroundColor Magenta

$testResults += Test-Endpoint "POST" "/test/failover/provider" "Trigger provider failover test"
$testResults += Test-Endpoint "POST" "/test/circuit-breaker" "Test circuit breaker behavior"
$testResults += Test-Endpoint "POST" "/test/cache-failback" "Test cache fallback behavior"

# Summary
Write-Host "`n==========================================" -ForegroundColor Cyan
Write-Host "TEST SUMMARY" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan

$passCount = ($testResults | Where-Object {$_.status -eq "PASS"}).Count
$failCount = ($testResults | Where-Object {$_.status -eq "FAIL"}).Count

Write-Host "Passed: $passCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor Red

if ($failCount -eq 0) {
    Write-Host "`nAll resilience endpoints operational!" -ForegroundColor Green
} else {
    Write-Host "`nSome endpoints failed. Check log above." -ForegroundColor Yellow
}
