# Try using test tokens from test configuration
$ErrorActionPreference = "Stop"

$apiUrl = "http://localhost:8080"

Write-Host "SUPREMEAI - TEST TOKEN ATTEMPT" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

# Test tokens from application-test.properties
$testTokens = @("admin-token", "test-token", "valid-token", "dev-token")

foreach ($token in $testTokens) {
    Write-Host "Testing token: $token" -ForegroundColor Yellow
    
    try {
        $queryBody = @{
            question = "What are the best practices for API design?"
        } | ConvertTo-Json
        
        $response = Invoke-WebRequest -Uri "$apiUrl/api/consensus/ask" `
            -Method Post `
            -Headers @{
                "Authorization" = "Bearer $token"
                "Content-Type" = "application/json"
            } `
            -Body $queryBody
        
        Write-Host "✅ Success with token: $token" -ForegroundColor Green
        Write-Host "Response: $($response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 2)" -ForegroundColor Green
        break
        
    } catch {
        Write-Host "❌ Failed: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Alternative: Try without Authorization header..."
try {
    $queryBody = @{
        question = "What is distributed system design?"
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "$apiUrl/api/consensus/ask" `
        -Method Post `
        -Headers @{"Content-Type"="application/json"} `
        -Body $queryBody
    
    Write-Host "✅ Public endpoint available!" -ForegroundColor Green
    $data = $response.Content | ConvertFrom-Json
    Write-Host "Consensus: $($data.consensusPercentage)%" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Public access denied" -ForegroundColor Red
}
