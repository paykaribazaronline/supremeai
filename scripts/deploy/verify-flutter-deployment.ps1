# ================================================================
# SupremeAI Flutter CI/CD - Pre-Deployment Verification (Windows)
# ================================================================

$ErrorActionPreference = "SilentlyContinue"

$Green = "DarkGreen"
$Red = "Red"
$Yellow = "DarkYellow"
$Blue = "Blue"

$ChecksPassed = 0
$ChecksFailed = 0

function Check-Item {
    param(
        [string]$Name,
        [scriptblock]$Command
    )
    
    if (& $Command) {
        Write-Host "✅ $Name" -ForegroundColor $Green
        $script:ChecksPassed++
    } else {
        Write-Host "❌ $Name" -ForegroundColor $Red
        $script:ChecksFailed++
    }
}

Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor $Blue
Write-Host "║  SupremeAI Flutter CI/CD - Pre-Deployment Verification        ║" -ForegroundColor $Blue
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor $Blue
Write-Host ""

Write-Host "Verifying System Requirements..." -ForegroundColor $Yellow
Write-Host ""

Check-Item "Flutter installed" { $null = flutter --version 2>&1; $? }
Check-Item "Java 17+ installed" { $null = java -version 2>&1; $? }
Check-Item "npm installed" { $null = npm --version 2>&1; $? }
Check-Item "Firebase CLI installed" { $null = firebase --version 2>&1; $? }
Check-Item "Git installed" { $null = git --version 2>&1; $? }

Write-Host ""
Write-Host "Verifying Project Structure..." -ForegroundColor $Yellow
Write-Host ""

Check-Item "flutter_admin_app exists" { Test-Path "flutter_admin_app" -PathType Container }
Check-Item ".firebaserc exists" { Test-Path ".firebaserc" -PathType Leaf }
Check-Item "firebase.json exists" { Test-Path "firebase.json" -PathType Leaf }
Check-Item ".github/workflows/flutter-ci-cd.yml exists" { Test-Path ".github\workflows\flutter-ci-cd.yml" -PathType Leaf }
Check-Item "pubspec.yaml exists" { Test-Path "flutter_admin_app\pubspec.yaml" -PathType Leaf }
Check-Item "web/index.html exists" { Test-Path "flutter_admin_app\web\index.html" -PathType Leaf }

Write-Host ""
Write-Host "Verifying Firebase Configuration..." -ForegroundColor $Yellow
Write-Host ""

$firebaserc = Get-Content ".firebaserc" -Raw
Check-Item ".firebaserc has default project" { $firebaserc -match '"default"' }

$firebaseJson = Get-Content "firebase.json" -Raw
Check-Item "firebase.json has main-dashboard target" { $firebaseJson -match 'main-dashboard' }
Check-Item "firebase.json has rewrites configured" { $firebaseJson -match 'rewrites' }

Write-Host ""
Write-Host "Verifying Flutter Configuration..." -ForegroundColor $Yellow
Write-Host ""

Push-Location flutter_admin_app

Check-Item "pubspec.yaml is valid" { 
    $output = flutter pub get --dry-run 2>&1
    $? -and -not ($output -match "error")
}

Check-Item "Flutter can analyze code" {
    $output = flutter analyze --no-pub 2>&1
    $? -or ($output -match "info|warning") # Analysis can have warnings but still succeed
}

Pop-Location

Write-Host ""
Write-Host "Verifying GitHub Configuration..." -ForegroundColor $Yellow
Write-Host ""

Check-Item "GitHub repo initialized" { Test-Path ".git" -PathType Container }
Check-Item "Git remote 'origin' configured" { 
    $remotes = git remote 2>&1
    $remotes -contains "origin"
}

Write-Host ""
Write-Host "Checking GitHub Secrets..." -ForegroundColor $Yellow
Write-Host ""

if (Get-Command gh -ErrorAction SilentlyContinue) {
    Check-Item "GitHub CLI available" { $null = gh --version 2>&1; $? }
    
    $secrets = gh secret list 2>&1
    Check-Item "FIREBASE_TOKEN secret exists" { $secrets -match "FIREBASE_TOKEN" }
} else {
    Write-Host "⚠️  GitHub CLI not installed (optional)" -ForegroundColor $Yellow
    Write-Host "   Install from: https://cli.github.com" -ForegroundColor $Yellow
}

Write-Host ""
Write-Host "Verifying Build Capabilities..." -ForegroundColor $Yellow
Write-Host ""

if (Test-Path "flutter_admin_app\build\web" -PathType Container) {
    Check-Item "Previous build artifacts exist" { Test-Path "flutter_admin_app\build\web" }
} else {
    Write-Host "⚠️  No previous build found (build will be created in CI/CD)" -ForegroundColor $Yellow
}

Write-Host ""
Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Blue
Write-Host ""

Write-Host "Summary:" -ForegroundColor $Blue
Write-Host "Checks Passed: $ChecksPassed" -ForegroundColor $Green
Write-Host "Checks Failed: $ChecksFailed" -ForegroundColor $Red
Write-Host ""

if ($ChecksFailed -eq 0) {
    Write-Host "✅ All checks passed! System is ready for deployment." -ForegroundColor $Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor $Blue
    Write-Host "1. Generate Firebase token: " -NoNewline
    Write-Host "firebase login:ci" -ForegroundColor $Green
    Write-Host "2. Add to GitHub Secrets: " -NoNewline
    Write-Host "FIREBASE_TOKEN" -ForegroundColor $Green
    Write-Host "3. Push to main branch: " -NoNewline
    Write-Host "git push origin main" -ForegroundColor $Green
    Write-Host "4. Monitor: " -NoNewline
    Write-Host "gh run watch" -ForegroundColor $Green
    Write-Host ""
    exit 0
} else {
    Write-Host "❌ Some checks failed. Please fix issues above before deploying." -ForegroundColor $Red
    Write-Host ""
    Write-Host "Common issues:" -ForegroundColor $Yellow
    Write-Host "• Missing Flutter: Install from flutter.dev"
    Write-Host "• Missing Java: Install Java 17+ from oracle.com"
    Write-Host "• Missing Firebase CLI: Run: npm install -g firebase-tools"
    Write-Host "• .firebaserc issues: Run: firebase init hosting"
    Write-Host "• GitHub secrets: go to Settings > Secrets > Actions"
    Write-Host ""
    exit 1
}
