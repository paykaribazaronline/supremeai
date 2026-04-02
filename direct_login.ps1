# Direct login without bootstrap (user already created on app startup)
$ErrorActionPreference = "Stop"

$apiUrl = "http://localhost:8080"
$adminEmail = if ($env:SUPREMEAI_ADMIN_EMAIL) { $env:SUPREMEAI_ADMIN_EMAIL } else { "admin@supremeai.com" }
$adminPassword = $env:SUPREMEAI_ADMIN_PASSWORD
$firebaseApiKey = if ($env:SUPREMEAI_FIREBASE_WEB_API_KEY) { $env:SUPREMEAI_FIREBASE_WEB_API_KEY } else { "AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8" }

if (-not $adminPassword) {
    Write-Host "❌ SUPREMEAI_ADMIN_PASSWORD is not set." -ForegroundColor Red
    Write-Host '   Set it first: $env:SUPREMEAI_ADMIN_PASSWORD = "<your-admin-password>"' -ForegroundColor Yellow
    exit 1
}

Write-Host "SUPREMEAI - DIRECT LOGIN TEST" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

try {
    Write-Host "Step 1: Testing Firebase login + backend token exchange..." -ForegroundColor Yellow
    $firebaseBody = @{
        email = $adminEmail
        password = $adminPassword
        returnSecureToken = $true
    } | ConvertTo-Json

    $firebaseResponse = Invoke-WebRequest -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$firebaseApiKey" `
        -Method Post `
        -Headers @{"Content-Type"="application/json"} `
        -Body $firebaseBody

    $firebaseData = $firebaseResponse.Content | ConvertFrom-Json
    if (-not $firebaseData.idToken) {
        throw "Firebase ID token not returned"
    }

    $exchangeBody = @{ idToken = $firebaseData.idToken } | ConvertTo-Json
    
    $loginResponse = Invoke-WebRequest -Uri "$apiUrl/api/auth/firebase-login" `
        -Method Post `
        -Headers @{"Content-Type"="application/json"} `
        -Body $exchangeBody
    
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
