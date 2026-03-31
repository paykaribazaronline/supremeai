# ================================================================
# SupremeAI Flutter Admin App - Local CI/CD Setup & Test Script
# Windows PowerShell Version
# ================================================================

$ErrorActionPreference = "Stop"

# Colors
$Green = "DarkGreen"
$Red = "Red"
$Yellow = "DarkYellow"
$Blue = "Blue"

Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor $Blue
Write-Host "║  SupremeAI Flutter Admin App - CI/CD Setup (Windows)           ║" -ForegroundColor $Blue
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor $Blue
Write-Host ""

# Check prerequisites
Write-Host "📋 Checking Prerequisites..." -ForegroundColor $Yellow
Write-Host ""

# Check Flutter
try {
    $flutterVersion = flutter --version 2>&1 | Select-Object -First 1
    Write-Host "✅ Flutter installed: $flutterVersion" -ForegroundColor $Green
} catch {
    Write-Host "❌ Flutter not found. Please install Flutter first." -ForegroundColor $Red
    Write-Host "   Visit: https://flutter.dev/docs/get-started/install" -ForegroundColor $Red
    exit 1
}

# Check Java
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✅ Java installed: $javaVersion" -ForegroundColor $Green
} catch {
    Write-Host "❌ Java not found. Please install Java 17+" -ForegroundColor $Red
    exit 1
}

# Check Firebase CLI
try {
    $firebaseVersion = firebase --version 2>&1
    Write-Host "✅ Firebase CLI installed: $firebaseVersion" -ForegroundColor $Green
} catch {
    Write-Host "⚠️  Firebase CLI not found. Installing..." -ForegroundColor $Yellow
    npm install -g firebase-tools
}

# Check npm
try {
    $npmVersion = npm --version 2>&1
    Write-Host "✅ npm installed" -ForegroundColor $Green
} catch {
    Write-Host "❌ npm not found. Please install Node.js" -ForegroundColor $Red
    exit 1
}

Write-Host ""
Write-Host "📦 Step 1: Building Flutter Web App..." -ForegroundColor $Blue
Write-Host ""

# Navigate to flutter app
Set-Location flutter_admin_app

# Get dependencies
Write-Host "Getting Flutter dependencies..." -ForegroundColor $Yellow
flutter pub get

# Build web app
Write-Host ""
Write-Host "Building web app (this may take 2-3 minutes)..." -ForegroundColor $Yellow
flutter build web --release --no-pub --base-href /

Write-Host ""
Write-Host "✅ Web app built successfully!" -ForegroundColor $Green
Write-Host ""

# Show build stats
Write-Host "Build artifacts:" -ForegroundColor $Yellow
$webBuildSize = (Get-Item -Path "build\web" -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host "Total size: $([Math]::Round($webBuildSize, 2)) MB" -ForegroundColor $Green

Get-ChildItem "build\web" | Select-Object -First 10 | Format-Table Name, @{Name="Size(KB)"; Expression={[Math]::Round($_.Length/1KB, 2)}}

Write-Host ""

# Go back to root
Set-Location ..

# Check Firebase configuration
Write-Host "🔥 Step 2: Verifying Firebase Configuration..." -ForegroundColor $Blue
Write-Host ""

if (-not (Test-Path ".firebaserc")) {
    Write-Host "❌ .firebaserc not found" -ForegroundColor $Red
    exit 1
}

if (-not (Test-Path "firebase.json")) {
    Write-Host "❌ firebase.json not found" -ForegroundColor $Red
    exit 1
}

Write-Host "✅ Firebase configuration files found" -ForegroundColor $Green
Write-Host ""

# Check if logged in to Firebase
Write-Host "🔐 Step 3: Firebase Authentication..." -ForegroundColor $Blue
Write-Host ""

$firebaseToken = [Environment]::GetEnvironmentVariable("FIREBASE_TOKEN", "User")
if (-not $firebaseToken) {
    Write-Host "⚠️  FIREBASE_TOKEN env variable not set" -ForegroundColor $Yellow
    Write-Host ""
    Write-Host "For GitHub Actions deployment:" -ForegroundColor $Yellow
    Write-Host "1. Generate a Firebase token:" -ForegroundColor $Yellow
    Write-Host "   firebase login:ci" -ForegroundColor $Green
    Write-Host ""
    Write-Host "2. Add to GitHub Secrets:" -ForegroundColor $Yellow
    Write-Host "   Settings > Secrets and variables > Actions" -ForegroundColor $Green
    Write-Host "   Secret name: FIREBASE_TOKEN" -ForegroundColor $Green
    Write-Host ""
    Write-Host "Alternatively, for local testing:" -ForegroundColor $Yellow
    Write-Host "   firebase login" -ForegroundColor $Green
} else {
    Write-Host "✅ FIREBASE_TOKEN is set" -ForegroundColor $Green
}

Write-Host ""
Write-Host "📋 Step 4: Configuration Summary" -ForegroundColor $Blue
Write-Host ""

$firebaseConfig = Get-Content .firebaserc | ConvertFrom-Json
Write-Host "Default Project: $($firebaseConfig.projects.default)" -ForegroundColor $Yellow

Write-Host ""
Write-Host "Hosting Targets:" -ForegroundColor $Yellow
$hostingConfig = Get-Content firebase.json | ConvertFrom-Json
foreach ($host in $hostingConfig.hosting) {
    Write-Host "  ✅ $($host.target)" -ForegroundColor $Green
}

Write-Host ""
Write-Host "🚀 Optional: Deploy to Firebase" -ForegroundColor $Yellow
Write-Host ""
Write-Host "To deploy the web app to Firebase Hosting:" -ForegroundColor $Yellow
Write-Host ""
Write-Host "  # Option 1: If logged in locally" -ForegroundColor $Yellow
Write-Host "  firebase deploy --only hosting:flutter-admin" -ForegroundColor $Green
Write-Host ""
Write-Host "  # Option 2: Using token" -ForegroundColor $Yellow
Write-Host "  firebase deploy --only hosting:flutter-admin --token `$FIREBASE_TOKEN" -ForegroundColor $Green
Write-Host ""

Write-Host "✅ Setup Complete!" -ForegroundColor $Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor $Yellow
Write-Host "1. Push code to main branch to trigger GitHub Actions"
Write-Host "2. Or manually trigger: gh workflow run flutter-ci-cd.yml"
Write-Host "3. Monitor deployment: gh run watch"
Write-Host ""
Write-Host "Your Flutter app will be live at:" -ForegroundColor $Green
Write-Host "https://supremeai-565236080752.web.app/admin/" -ForegroundColor $Green
Write-Host ""

Write-Host "📚 Documentation:" -ForegroundColor $Blue
Write-Host "• CI/CD Automation: flutter_admin_app/CI_CD_AUTOMATION.md" -ForegroundColor $Green
Write-Host "• GitHub Secrets: GITHUB_SECRETS_SETUP.md" -ForegroundColor $Green
Write-Host ""
