# Direct login without bootstrap (user already created on app startup)
$ErrorActionPreference = "Stop"

$apiUrl = "http://localhost:8080"

Write-Host "SUPREMEAI - DIRECT LOGIN TEST" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

try {
    # Try to login with credentials from automatic bootstrap
    Write-Host "Step 1: Testing direct login as 'supremeai'..." -ForegroundColor Yellow
    $loginBody = @{
        username = "supremeai"
        password = "Admin@123456!"
    } | ConvertTo-Json
    
    $loginResponse = Invoke-WebRequest -Uri "$apiUrl/api/auth/login" `
        -Method Post `
        -Headers @{"Content-Type"="application/json"} `
        -Body $loginBody
    
    $loginData = $loginResponse.Content | ConvertFrom-Json
    Write-Host "✅ Login successful!" -ForegroundColor Green
    Write-Host "Status: $($loginData.status)"
    Write-Host "Token: $($loginData.token.Substring(0, 50))..." -ForegroundColor Green
    $token = $loginData.token
    
    # Now test a consensus query with this token
    Write-Host ""
    Write-Host "Step 2: Using token to ask consensus question..." -ForegroundColor Yellow
    
    $questionsToAsk = @(
        "What is the best pattern for distributed systems?"
        "How should I implement rate limiting?"
        "What are microservice best practices?"
    )
    
    foreach ($question in $questionsToAsk) {
        Write-Host "   Q: $question" -ForegroundColor Cyan
        
        $queryBody = @{
            question = $question
        } | ConvertTo-Json
        
        $consensusResponse = Invoke-WebRequest -Uri "$apiUrl/api/consensus/ask" `
            -Method Post `
            -Headers @{
                "Authorization" = "Bearer $token"
                "Content-Type" = "application/json"
            } `
            -Body $queryBody
        
        $consensusData = $consensusResponse.Content | ConvertFrom-Json
        Write-Host "   ✅ Consensus: $($consensusData.consensusPercentage)% agreement" -ForegroundColor Green
        Write-Host "   🏆 Winner: $($consensusData.winningResponse.Substring(0, 80))..." -ForegroundColor Green
        Write-Host ""
    }
    
    # Check learning stats
    Write-Host "Step 3: Checking learning stats..." -ForegroundColor Yellow
    $statsResponse = Invoke-WebRequest -Uri "$apiUrl/api/learning/stats" `
        -Method Get `
        -Headers @{
            "Authorization" = "Bearer $token"
            "Content-Type" = "application/json"
        }
    
    $statsData = $statsResponse.Content | ConvertFrom-Json
    Write-Host "✅ Learning Stats Retrieved" -ForegroundColor Green
    Write-Host "Total Learnings: $($statsData.totalLearnings)"
    Write-Host "Critical Requirements: $($statsData.criticalRequirements.Count)"
    Write-Host ""
    Write-Host "Learning has been triggered! Check Firebase at: system/learnings/" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Error occurred!" -ForegroundColor Red
    Write-Host "Status: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    Write-Host "Message: $($_.Exception.Message)" -ForegroundColor Red
    
    # Try to get response body
    try {
        $errorResponse = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorResponse)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    } catch {
        Write-Host "Could not read error response" -ForegroundColor Red
    }
}
