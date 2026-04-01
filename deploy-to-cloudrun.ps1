#!/usr/bin/env pwsh
# SupremeAI - Google Cloud Run Deployment Script
# Purpose: Automated build and deploy to Cloud Run with all fixes applied
# Usage: .\deploy-to-cloudrun.ps1

param(
    [string]$ProjectId = "supremeai-565236080752",
    [string]$ServiceName = "supremeai",
    [string]$Region = "us-central1",
    [string]$Memory = "512Mi",
    [int]$Port = 8080,
    [switch]$SkipTest,
    [switch]$ViewLogs
)

$ErrorActionPreference = "Stop"

Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "🚀 SupremeAI - Google Cloud Run Deployment" -ForegroundColor Green
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""

# Step 1: Verify tools
Write-Host "1️⃣  Checking prerequisites..." -ForegroundColor Cyan
$tools = @("gcloud", "gradle")
foreach ($tool in $tools) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        Write-Host "❌ $tool not found. Please install it first." -ForegroundColor Red
        exit 1
    }
}
Write-Host "✅ All tools found" -ForegroundColor Green
Write-Host ""

# Step 2: Clean build
Write-Host "2️⃣  Building JAR file..." -ForegroundColor Cyan
.\gradlew clean build $(if ($SkipTest) { "-x test" } else { "" })
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Build complete" -ForegroundColor Green
Write-Host ""

# Step 3: Set GCP project
Write-Host "3️⃣  Setting GCP project..." -ForegroundColor Cyan
gcloud config set project $ProjectId
Write-Host "✅ Project set to: $ProjectId" -ForegroundColor Green
Write-Host ""

# Step 4: Build and push to Cloud Build
Write-Host "4️⃣  Building Docker image on Google Cloud Build..." -ForegroundColor Cyan
gcloud builds submit --tag "gcr.io/$ProjectId/$ServiceName`:latest"
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Cloud Build failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Docker image built and pushed" -ForegroundColor Green
Write-Host ""

# Step 5: Deploy to Cloud Run
Write-Host "5️⃣  Deploying to Cloud Run..." -ForegroundColor Cyan
gcloud run deploy $ServiceName `
    --image "gcr.io/$ProjectId/$ServiceName`:latest" `
    --platform managed `
    --region $Region `
    --port $Port `
    --memory $Memory `
    --cpu 1 `
    --timeout 3600 `
    --set-env-vars PORT=$Port `
    --no-allow-unauthenticated `
    --min-instances 1 `
    --max-instances 10 `
    --health-check-path /actuator/health

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Deployment failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Deployment complete" -ForegroundColor Green
Write-Host ""

# Step 6: Get service details
Write-Host "6️⃣  Getting service details..." -ForegroundColor Cyan
$serviceInfo = gcloud run services describe $ServiceName --region $Region --format="value(status.url)"
$serviceUrl = $serviceInfo.Trim()
Write-Host "✅ Service URL: $serviceUrl" -ForegroundColor Green
Write-Host ""

# Step 7: Test endpoints
Write-Host "7️⃣  Testing endpoints..." -ForegroundColor Cyan
$maxRetries = 10
$retry = 0
$healthCheck = $false

while ($retry -lt $maxRetries -and -not $healthCheck) {
    try {
        $response = Invoke-WebRequest -Uri "$serviceUrl/actuator/health" -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "✅ Health check: PASSED" -ForegroundColor Green
            $healthCheck = $true
        }
    } catch {
        $retry++
        Write-Host "⏳ Waiting for service to be ready... ($retry/$maxRetries)"
        Start-Sleep -Seconds 5
    }
}

if (-not $healthCheck) {
    Write-Host "❌ Health check timeout - service might be starting" -ForegroundColor Yellow
} else {
    try {
        $homeResponse = Invoke-WebRequest -Uri "$serviceUrl/" -ErrorAction SilentlyContinue
        Write-Host "✅ Home endpoint: $(($homeResponse.Content -split '\n')[0])" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  Could not reach home endpoint" -ForegroundColor Yellow
    }
}
Write-Host ""

# Step 8: Display results
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "✅ DEPLOYMENT SUCCESSFUL!" -ForegroundColor Green
Write-Host "════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""
Write-Host "Service Details:" -ForegroundColor Cyan
Write-Host "  Name: $ServiceName" -ForegroundColor White
Write-Host "  Project: $ProjectId" -ForegroundColor White
Write-Host "  Region: $Region" -ForegroundColor White
Write-Host "  Memory: $Memory" -ForegroundColor White
Write-Host "  URL: $serviceUrl" -ForegroundColor Green
Write-Host ""

# Step 9: View logs if requested
if ($ViewLogs) {
    Write-Host "📋 Viewing logs..." -ForegroundColor Cyan
    gcloud run services logs read $ServiceName --region $Region --limit 50 --follow
}

Write-Host "🎉 Done! Your SupremeAI service is now live!" -ForegroundColor Green
Write-Host "📚 Documentation: https://cloud.google.com/run/docs" -ForegroundColor Cyan
