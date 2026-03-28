# SupremeAI Google Cloud Deployment Script
# Automates the entire deployment process to Google Cloud Run

param(
    [string]$ProjectId = "supremeai-production",
    [string]$Region = "us-central1",
    [string]$ImageTag = "1.0.0",
    [switch]$DeployMain,
    [switch]$DeployAdmin,
    [switch]$DeployBoth,
    [switch]$InstallSDK,
    [switch]$Help
)

$ErrorActionPreference = "Stop"

function Show-Help {
    Write-Host @"
SupremeAI Google Cloud Deployment Script

Usage: .\deploy-to-gcp.ps1 [Options]

Options:
  -ProjectId <string>      GCP Project ID (default: supremeai-production)
  -Region <string>         GCP Region (default: us-central1)
  -ImageTag <string>       Docker image tag (default: 1.0.0)
  -DeployMain             Deploy only main SupremeAI system
  -DeployAdmin            Deploy only admin dashboard
  -DeployBoth             Deploy both (default if neither specified)
  -InstallSDK             Install Google Cloud SDK first
  -Help                   Show this help message

Examples:
  .\deploy-to-gcp.ps1 -DeployBoth
  .\deploy-to-gcp.ps1 -DeployMain -Region us-east1
  .\deploy-to-gcp.ps1 -InstallSDK -DeployBoth
"@
}

function Write-Status {
    param([string]$Message)
    Write-Host "`n✓ $Message" -ForegroundColor Green
}

function Write-Section {
    param([string]$Title)
    Write-Host "`n════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host "════════════════════════════════════════" -ForegroundColor Cyan
}

function Check-Prerequisites {
    Write-Section "Checking Prerequisites"
    
    # Check Docker
    try {
        docker --version | Out-Null
        Write-Status "Docker is installed"
    } catch {
        Write-Host "✗ Docker not found. Please install Docker Desktop." -ForegroundColor Red
        exit 1
    }
    
    # Check gcloud
    try {
        gcloud --version | Out-Null
        Write-Status "Google Cloud SDK is installed"
    } catch {
        if ($InstallSDK) {
            Write-Host "Installing Google Cloud SDK..." -ForegroundColor Yellow
            # Auto-download and install (simplified)
            Write-Host "Please download from: https://cloud.google.com/sdk/docs/install-sdk" -ForegroundColor Yellow
            exit 1
        } else {
            Write-Host "✗ Google Cloud SDK not found. Use -InstallSDK flag." -ForegroundColor Red
            exit 1
        }
    }
    
    # Check git
    try {
        git --version | Out-Null
        Write-Status "Git is installed"
    } catch {
        Write-Host "✗ Git not found. Please install Git." -ForegroundColor Red
        exit 1
    }
    
    # Check Java
    try {
        java -version 2>&1 | Out-Null
        Write-Status "Java is installed"
    } catch {
        Write-Host "✗ Java not found. Please install Java 17+." -ForegroundColor Red
        exit 1
    }
}

function Setup-GCP {
    Write-Section "Setting Up Google Cloud SDK"
    
    # Check authentication
    Write-Host "Checking GCP authentication..." -ForegroundColor Yellow
    try {
        gcloud config list | Out-Null
    } catch {
        Write-Host "Authenticating with Google Cloud..." -ForegroundColor Yellow
        gcloud auth login
    }
    
    # Set project
    Write-Host "Setting project to $ProjectId..." -ForegroundColor Yellow
    gcloud config set project $ProjectId
    Write-Status "GCP project configured: $ProjectId"
    
    # Enable required APIs
    Write-Host "Enabling required APIs..." -ForegroundColor Yellow
    $apis = @(
        "run.googleapis.com",
        "firestore.googleapis.com",
        "cloudbuild.googleapis.com",
        "containerregistry.googleapis.com",
        "serviceusage.googleapis.com",
        "cloudscheduler.googleapis.com",
        "secretmanager.googleapis.com"
    )
    
    foreach ($api in $apis) {
        gcloud services enable $api --quiet
    }
    Write-Status "APIs enabled"
    
    # Configure Docker authentication
    Write-Host "Configuring Docker authentication..." -ForegroundColor Yellow
    gcloud auth configure-docker --quiet
    Write-Status "Docker authentication configured"
}

function Build-MainSystem {
    Write-Section "Building Main SupremeAI System"
    
    Push-Location c:\Users\Nazifa\supremeai
    
    # Gradle build
    Write-Host "Building with Gradle..." -ForegroundColor Yellow
    .\gradlew clean build -x test
    
    # Docker build
    Write-Host "Building Docker image..." -ForegroundColor Yellow
    $imageName = "gcr.io/$ProjectId/supremeai:$ImageTag"
    docker build -t $imageName .
    
    # Docker push
    Write-Host "Pushing image to Google Container Registry..." -ForegroundColor Yellow
    docker push $imageName
    Write-Status "Main system Docker image pushed: $imageName"
    
    Pop-Location
}

function Build-AdminSystem {
    Write-Section "Building Admin Dashboard"
    
    Push-Location c:\Users\Nazifa\supremeai-admin
    
    # Gradle build
    Write-Host "Building with Gradle..." -ForegroundColor Yellow
    .\gradlew clean build -x test
    
    # Docker build
    Write-Host "Building Docker image..." -ForegroundColor Yellow
    $imageName = "gcr.io/$ProjectId/supremeai-admin:$ImageTag"
    docker build -t $imageName .
    
    # Docker push
    Write-Host "Pushing image to Google Container Registry..." -ForegroundColor Yellow
    docker push $imageName
    Write-Status "Admin Docker image pushed: $imageName"
    
    Pop-Location
}

function Deploy-MainSystem {
    Write-Section "Deploying Main System to Cloud Run"
    
    $imageName = "gcr.io/$ProjectId/supremeai:$ImageTag"
    
    Write-Host "Deploying supremeai service..." -ForegroundColor Yellow
    gcloud run deploy supremeai `
        --image $imageName `
        --platform managed `
        --region $Region `
        --port 8080 `
        --memory 1Gi `
        --cpu 1 `
        --timeout 3600 `
        --max-instances 10 `
        --allow-unauthenticated `
        --set-env-vars FIREBASE_CONFIG_PATH=/secrets/firebase-service-account.json `
        --quiet
    
    # Get service URL
    $serviceUrl = gcloud run services describe supremeai --region $Region --format 'value(status.url)'
    Write-Status "Main system deployed: $serviceUrl"
    
    return $serviceUrl
}

function Deploy-AdminSystem {
    Write-Section "Deploying Admin Dashboard to Cloud Run"
    
    $imageName = "gcr.io/$ProjectId/supremeai-admin:$ImageTag"
    
    Write-Host "Deploying supremeai-admin service..." -ForegroundColor Yellow
    gcloud run deploy supremeai-admin `
        --image $imageName `
        --platform managed `
        --region $Region `
        --port 8080 `
        --memory 512Mi `
        --cpu 1 `
        --timeout 3600 `
        --max-instances 5 `
        --allow-unauthenticated `
        --set-env-vars FIREBASE_CONFIG_PATH=/secrets/firebase-service-account.json `
        --quiet
    
    # Get service URL
    $serviceUrl = gcloud run services describe supremeai-admin --region $Region --format 'value(status.url)'
    Write-Status "Admin dashboard deployed: $serviceUrl"
    
    return $serviceUrl
}

function Show-Summary {
    param(
        [string]$MainUrl,
        [string]$AdminUrl
    )
    
    Write-Section "Deployment Complete!"
    
    if ($MainUrl) {
        Write-Host "Main System: $MainUrl" -ForegroundColor Green
    }
    if ($AdminUrl) {
        Write-Host "Admin Dashboard: $AdminUrl" -ForegroundColor Green
    }
    
    Write-Host @"

Next Steps:
1. Verify services are running:
   - Test main system health: $($MainUrl)/api/v1/system/health
   - Test admin health: $($AdminUrl)/api/admin/dashboard/health

2. Configure custom domains (optional):
   - gcloud run domain-mappings create --service=supremeai --domain=api.supremeai.dev
   - gcloud run domain-mappings create --service=supremeai-admin --domain=admin.supremeai.dev

3. Setup monitoring:
   - View logs: gcloud logging read "resource.type=cloud_run_revision" --limit 100
   - Monitor: https://console.cloud.google.com/monitoring

4. Configure secrets:
   - gcloud secrets create firebase-service-account --data-file=service-account.json
   - gcloud secrets create jwt-secret --data-file=jwt-secret.txt

Documentation: https://cloud.google.com/run/docs
"@
}

# Main execution
if ($Help) {
    Show-Help
    exit 0
}

# Determine deployment mode
if (-not $DeployMain -and -not $DeployAdmin) {
    $DeployBoth = $true
}

try {
    Check-Prerequisites
    Setup-GCP
    
    $mainUrl = $null
    $adminUrl = $null
    
    if ($DeployMain -or $DeployBoth) {
        Build-MainSystem
        $mainUrl = Deploy-MainSystem
    }
    
    if ($DeployAdmin -or $DeployBoth) {
        Build-AdminSystem
        $adminUrl = Deploy-AdminSystem
    }
    
    Show-Summary -MainUrl $mainUrl -AdminUrl $adminUrl
    
} catch {
    Write-Host "`n✗ Error: $_" -ForegroundColor Red
    exit 1
}
