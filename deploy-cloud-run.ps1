#!/usr/bin/env pwsh
# SupremeAI - Cloud Run Deployment (Windows)
# Usage: .\deploy-cloud-run.ps1

param(
    [string]$ProjectId = "supremeai-565236080752",
    [string]$ServiceName = "supremeai",
    [string]$Region = "us-central1"
)

$ImageName = "gcr.io/$ProjectId/$ServiceName"

Write-Host "🚀 Deploying SupremeAI to Cloud Run" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Green
Write-Host ""

try {
    # Step 1: Authenticate
    Write-Host "1️⃣ Authenticating with Google Cloud..." -ForegroundColor Cyan
    gcloud auth login
    gcloud config set project $ProjectId
    Write-Host "✅ Authenticated" -ForegroundColor Green
    Write-Host ""
    
    # Step 2: Build JAR
    Write-Host "2️⃣ Building JAR file..." -ForegroundColor Cyan
    .\gradlew clean build -x test
    if ($LASTEXITCODE -ne 0) { throw "Build failed" }
    Write-Host "✅ JAR built successfully" -ForegroundColor Green
    Write-Host ""
    
    # Step 3: Build Docker image
    Write-Host "3️⃣ Building Docker image..." -ForegroundColor Cyan
    $timestamp = Get-Date -UFormat %s
    docker build -t "$ImageName`:latest" -t "$ImageName`:$timestamp" .
    if ($LASTEXITCODE -ne 0) { throw "Docker build failed" }
    Write-Host "✅ Docker image built" -ForegroundColor Green
    Write-Host ""
    
    # Step 4: Push to Registry
    Write-Host "4️⃣ Pushing to Google Container Registry..." -ForegroundColor Cyan
    docker push "$ImageName`:latest"
    if ($LASTEXITCODE -ne 0) { throw "Docker push failed" }
    Write-Host "✅ Image pushed" -ForegroundColor Green
    Write-Host ""
    
    # Step 5: Deploy to Cloud Run
    Write-Host "5️⃣ Deploying to Cloud Run..." -ForegroundColor Cyan
    gcloud run deploy $ServiceName `
        --image "$ImageName`:latest" `
        --platform managed `
        --region $Region `
        --port 8080 `
        --memory 512Mi `
        --cpu 1 `
        --timeout 3600 `
        --max-instances 100 `
        --allow-unauthenticated `
        --set-env-vars "PORT=8080,FIREBASE_PROJECT_ID=supremeai-a"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Deployment complete!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Service URL:" -ForegroundColor Cyan
        gcloud run services describe $ServiceName --region $Region --format "value(status.url)"
    }
}
catch {
    Write-Host "❌ Error: $_" -ForegroundColor Red
    exit 1
}
