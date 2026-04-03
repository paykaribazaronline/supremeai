#!/usr/bin/env pwsh
param(
    [string]$ProjectId = "supremeai-a",
    [string]$ServiceName = "supremeai",
    [string]$Region = "us-central1",
    [string]$FirebaseDatabaseUrl = "https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/",
    [string]$FirebaseServiceAccountFile = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

function Ensure-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

function Run-Checked {
    param([string]$Command)
    Write-Host "> $Command" -ForegroundColor Cyan
    Invoke-Expression $Command
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed: $Command"
    }
}

Write-Host "=== SupremeAI Cloud Run Auth Fix Deploy ===" -ForegroundColor Green

Ensure-Command "gcloud"
Ensure-Command "git"

$sha = (git rev-parse --short HEAD).Trim()
if ([string]::IsNullOrWhiteSpace($sha)) {
    throw "Could not resolve git short SHA"
}

$imageSha = "gcr.io/$ProjectId/$ServiceName`:$sha"
$imageLatest = "gcr.io/$ProjectId/$ServiceName`:latest"

Run-Checked "gcloud config set project $ProjectId"
Run-Checked "gcloud auth configure-docker gcr.io --quiet"

if (-not $SkipBuild) {
    $useLocalDocker = $false
    if (Get-Command "docker" -ErrorAction SilentlyContinue) {
        try {
            docker version | Out-Null
            if ($LASTEXITCODE -eq 0) {
                $useLocalDocker = $true
            }
        } catch {
            $useLocalDocker = $false
        }
    }

    if ($useLocalDocker) {
        Run-Checked "docker build -t $imageSha -t $imageLatest ."
        Run-Checked "docker push $imageSha"
        Run-Checked "docker push $imageLatest"
    } else {
        Write-Host "Docker daemon unavailable. Using Cloud Build fallback..." -ForegroundColor Yellow
        Run-Checked "gcloud builds submit --tag $imageSha --project $ProjectId"
        Run-Checked "gcloud container images add-tag $imageSha $imageLatest --quiet"
    }
}

$secretName = "firebase-service-account-json"
$secretExists = $false

try {
    gcloud secrets describe $secretName --project $ProjectId --format="value(name)" | Out-Null
    if ($LASTEXITCODE -eq 0) {
        $secretExists = $true
    }
} catch {
    $secretExists = $false
}

if (-not [string]::IsNullOrWhiteSpace($FirebaseServiceAccountFile)) {
    if (-not (Test-Path $FirebaseServiceAccountFile)) {
        throw "Firebase service account file not found: $FirebaseServiceAccountFile"
    }

    if ($secretExists) {
        Run-Checked "gcloud secrets versions add $secretName --data-file=`"$FirebaseServiceAccountFile`" --project $ProjectId"
    } else {
        Run-Checked "gcloud secrets create $secretName --replication-policy=automatic --data-file=`"$FirebaseServiceAccountFile`" --project $ProjectId"
        $secretExists = $true
    }

    $projectNumber = (gcloud projects describe $ProjectId --format="value(projectNumber)").Trim()
    if (-not [string]::IsNullOrWhiteSpace($projectNumber)) {
        $computeSa = "$projectNumber-compute@developer.gserviceaccount.com"
        Run-Checked "gcloud secrets add-iam-policy-binding $secretName --member=serviceAccount:$computeSa --role=roles/secretmanager.secretAccessor --project $ProjectId"
    }
}

$deployCommand = @(
    "gcloud run deploy $ServiceName",
    "--image $imageSha",
    "--region $Region",
    "--platform managed",
    "--port 8080",
    "--memory 1Gi",
    "--cpu 1",
    "--max-instances 10",
    "--timeout 3600",
    "--allow-unauthenticated",
    "--update-env-vars FIREBASE_PROJECT_ID=$ProjectId,GOOGLE_CLOUD_PROJECT=$ProjectId,FIREBASE_DATABASE_URL=$FirebaseDatabaseUrl"
)

if ($secretExists) {
    $deployCommand += "--update-secrets FIREBASE_SERVICE_ACCOUNT_JSON=$secretName`:latest"
}

$deployCommand += "--project $ProjectId"
$deployCommand += "--quiet"

Run-Checked ($deployCommand -join " ")

$serviceUrl = (gcloud run services describe $ServiceName --region $Region --project $ProjectId --format="value(status.url)").Trim()
if ([string]::IsNullOrWhiteSpace($serviceUrl)) {
    throw "Could not resolve Cloud Run service URL"
}

Write-Host "Service URL: $serviceUrl" -ForegroundColor Green

try {
    $health = Invoke-WebRequest -Uri "$serviceUrl/actuator/health" -UseBasicParsing -TimeoutSec 30
    Write-Host "Health status: $($health.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "Health check failed: $($_.Exception.Message)" -ForegroundColor Yellow
}

$firebaseEndpointOk = $false
try {
    $resp = Invoke-WebRequest -Uri "$serviceUrl/api/auth/firebase-login" -Method POST -ContentType "application/json" -Body "{}" -UseBasicParsing -TimeoutSec 30
    if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 500) {
        $firebaseEndpointOk = $true
    }
} catch {
    if ($_.Exception.Response) {
        $code = [int]$_.Exception.Response.StatusCode.value__
        if ($code -ne 404) {
            $firebaseEndpointOk = $true
        }
    }
}

if (-not $firebaseEndpointOk) {
    throw "Deployed service still returns 404 for /api/auth/firebase-login"
}

Write-Host "Firebase login endpoint check: PASS" -ForegroundColor Green
Write-Host "Done." -ForegroundColor Green
