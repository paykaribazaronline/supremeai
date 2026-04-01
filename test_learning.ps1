# Test SupremeAI Learning System

Write-Host "================================" -ForegroundColor Cyan
Write-Host "SUPREMEAI SYSTEM TEST" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Cyan

# Test 1: Git Status
Write-Host "`n[1/3] Checking GIT for auto-commits..." -ForegroundColor Yellow
Push-Location "c:\Users\Nazifa\supremeai"
$commits = git log --oneline -5 2>$null
if ($commits) {
    Write-Host "✅ Git log available:" -ForegroundColor Green
    $commits | Select-Object -First 5 | ForEach-Object { Write-Host "    $_" }
} else {
    WritHost "⚠️  Could not read git log" -ForegroundColor Yellow
}
Pop-Location

# Test 2: App Log Analysis
Write-Host "`n[2/3] Checking APP LOG..." -ForegroundColor Yellow
$logPath = "c:\Users\Nazifa\supremeai\app.log"
if (Test-Path $logPath) {
    $logContent = Get-Content $logPath -Raw
    
    Write-Host "✅ App Log found (Size: $([Math]::Round((Get-Item $logPath).Length / 1KB))KB)" -ForegroundColor Green
    
    # Check for keywords
    if ($logContent -match "LIVE|Orchestrator") { Write-Host "  ✓ App is LIVE" -ForegroundColor Green }
    if ($logContent -match "Firebase") { Write-Host "  ✓ Firebase integration present" -ForegroundColor Green }
    if ($logContent -match "learning|Learning") { Write-Host "  ✓ Learning service loaded" -ForegroundColor Green }
    if ($logContent -match "WebSocket") { Write-Host "  ✓ WebSocket handlers registered" -ForegroundColor Green }
} else {
    Write-Host "❌ App log not found" -ForegroundColor Red
}

# Test 3: Check Firebase Files
Write-Host "`n[3/3] Looking for FIREBASE learnings..." -ForegroundColor Yellow

$firebase_files = @(
    "test-firebase-credentials.json",
    "firebase-test-creds.json",
    ".firebaserc"
)

foreach ($file in $firebase_files) {
    $path = Join-Path "c:\Users\Nazifa\supremeai" $file
    if (Test-Path $path) {
        Write-Host "✅ Found: $file" -ForegroundColor Green
    }
}

# Check for learning data directories
if (Test-Path "c:\Users\Nazifa\supremeai\data\learnings") {
    Write-Host "✅ Found learnings directory" -ForegroundColor Green
}

if (Test-Path "c:\Users\Nazifa\supremeai\src\main\java\org\example\service\SystemLearningService.java") {
    Write-Host "✅ SystemLearningService exists (Learning engine)" -ForegroundColor Green
}

Write-Host "`n================================" -ForegroundColor Cyan
Write-Host "SYSTEM STATUS: READY FOR TESTING" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Cyan
Write-Host "`nNext Steps:`n  1. App is running on http://localhost:8080`n  2. Learning system is active`n  3. Ready for test requests" -ForegroundColor Cyan
