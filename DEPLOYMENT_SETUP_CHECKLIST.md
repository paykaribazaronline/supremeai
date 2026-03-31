# Google Cloud Deployment - Setup & Deployment Guide

## ⚠️ Prerequisites Installation

### 1. **Install Docker Desktop** (Required)

**Windows 10/11 Installation:**

1. Download Docker Desktop: https://www.docker.com/products/docker-desktop
2. Run the installer
3. Accept virtualization requirements
4. Restart your computer
5. Verify:
   ```powershell
   docker --version
   docker run hello-world
   ```

**Alternative (Chocolatey):**

```powershell
# Run as Administrator
choco install docker-desktop
```

---

### 2. **Install Google Cloud SDK** (Required)

**Windows Installation:**

1. Download: https://cloud.google.com/sdk/docs/install-sdk
2. Click the Windows 64-bit installer (.exe)
3. Run installer
4. Follow setup wizard
5. Allow PowerShell startup script

**Verify Installation:**

```powershell
gcloud --version
```

**Alternative (Chocolatey):**

```powershell
# Run as Administrator
choco install google-cloud-sdk
```

---

### 3. **Install Java 17+** (Usually Already Installed)

```powershell
java -version
# Should show Java 17 or higher
```

**If not installed:** https://adoptium.net/installation

---

## ✅ Verification Checklist

Before proceeding, verify all are installed:

```powershell
# Check all prerequisites
"=== Checking Installed Tools ===" | Write-Host -ForegroundColor Cyan

$tools = @(
    @{name="Docker"; cmd="docker --version"},
    @{name="Google Cloud SDK"; cmd="gcloud --version"},
    @{name="Java"; cmd="java -version"},
    @{name="Git"; cmd="git --version"},
    @{name="Gradle"; cmd="gradle --version"}
)

foreach ($tool in $tools) {
    try {
        $result = & ([scriptblock]::Create($tool.cmd)) 2>&1
        Write-Host "✓ $($tool.name)" -ForegroundColor Green
    } catch {
        Write-Host "✗ $($tool.name) - NOT INSTALLED" -ForegroundColor Red
    }
}
```

---

## 🔑 Setup Google Cloud Project

Once prerequisites are installed, run:

### Step 1: Login to Google Cloud

```powershell
gcloud auth login

# Browser will open - login with your Google account
# Accept permissions
```

### Step 2: Create Project

```powershell
# Create new project
gcloud projects create supremeai-production --name="SupremeAI Production"

# Set as default
gcloud config set project supremeai-production

# Verify
gcloud config list
```

### Step 3: Enable Required APIs

```powershell
gcloud services enable run.googleapis.com \
  firestore.googleapis.com \
  cloudbuild.googleapis.com \
  containerregistry.googleapis.com \
  secretmanager.googleapis.com \
  --quiet

# Verify APIs are enabled
gcloud services list --enabled
```

### Step 4: Configure Docker Authentication

```powershell
gcloud auth configure-docker

# Verify Docker can push to GCP
docker ps
```

---

## 🚀 Deploy to Google Cloud

### Option A: **Automated Deployment Script** (Recommended)

```powershell
cd c:\Users\Nazifa\supremeai

# Deploy both systems (main + admin)
.\deploy-to-gcp.ps1 -DeployBoth

# Script will:
# 1. Build both projects
# 2. Create Docker images
# 3. Push to Google Container Registry
# 4. Deploy to Cloud Run
# 5. Show service URLs
```

**Estimated Time:** 10-15 minutes

### Option B: **Manual Step-by-Step Deployment**

#### Deploy Main System

```powershell
cd c:\Users\Nazifa\supremeai

# Build with Gradle
Write-Host "Building main system..." -ForegroundColor Yellow
.\gradlew clean build -x test

# Build Docker image
Write-Host "Building Docker image..." -ForegroundColor Yellow
docker build -t gcr.io/supremeai-production/supremeai:1.0.0 .

# Push to Google Container Registry
Write-Host "Pushing to Container Registry..." -ForegroundColor Yellow
docker push gcr.io/supremeai-production/supremeai:1.0.0

# Deploy to Cloud Run
Write-Host "Deploying to Cloud Run..." -ForegroundColor Yellow
gcloud run deploy supremeai `
  --image gcr.io/supremeai-production/supremeai:1.0.0 `
  --platform managed `
  --region us-central1 `
  --port 8080 `
  --memory 1Gi `
  --cpu 1 `
  --timeout 3600 `
  --max-instances 10 `
  --allow-unauthenticated

# Get service URL
$mainUrl = gcloud run services describe supremeai --region us-central1 --format 'value(status.url)'
Write-Host "`n✓ Main System deployed: $mainUrl" -ForegroundColor Green
```

#### Deploy Admin Dashboard

```powershell
cd c:\Users\Nazifa\supremeai-admin

# Build with Gradle
Write-Host "Building admin system..." -ForegroundColor Yellow
.\gradlew clean build -x test

# Build Docker image
Write-Host "Building Docker image..." -ForegroundColor Yellow
docker build -t gcr.io/supremeai-production/supremeai-admin:1.0.0 .

# Push to Google Container Registry
Write-Host "Pushing to Container Registry..." -ForegroundColor Yellow
docker push gcr.io/supremeai-production/supremeai-admin:1.0.0

# Deploy to Cloud Run
Write-Host "Deploying to Cloud Run..." -ForegroundColor Yellow
gcloud run deploy supremeai-admin `
  --image gcr.io/supremeai-production/supremeai-admin:1.0.0 `
  --platform managed `
  --region us-central1 `
  --port 8080 `
  --memory 512Mi `
  --cpu 1 `
  --timeout 3600 `
  --max-instances 5 `
  --allow-unauthenticated

# Get service URL
$adminUrl = gcloud run services describe supremeai-admin --region us-central1 --format 'value(status.url)'
Write-Host "`n✓ Admin Dashboard deployed: $adminUrl" -ForegroundColor Green
```

---

## ✅ Verify Deployment

```powershell
# List deployed services
gcloud run services list

# Test main system
$mainUrl = gcloud run services describe supremeai --region us-central1 --format 'value(status.url)'
curl "$mainUrl/api/v1/system/health"

# Test admin dashboard
$adminUrl = gcloud run services describe supremeai-admin --region us-central1 --format 'value(status.url)'
curl "$adminUrl/api/admin/dashboard/health"

# View real-time logs
gcloud logging read "resource.type=cloud_run_revision" --limit 50 --format json
```

---

## 📊 Deployment Status Dashboard

```powershell
# Check all services
Write-Host "`n=========== Cloud Run Services ===========" -ForegroundColor Cyan
gcloud run services list --format "table(NAME, STATUS, REGION, URL)"

# Check recent deployments
Write-Host "`n========== Recent Cloud Build Logs ==========" -ForegroundColor Cyan
gcloud builds list --limit 10 --format "table(ID, STATUS, SOURCE, CREATE_TIME)"

# Check resource usage
Write-Host "`n========== Firestore Status ==========" -ForegroundColor Cyan
gcloud firestore databases list
```

---

## 🐛 Troubleshooting

### Issue: "Docker image not found"

**Solution:**

```powershell
# Verify image was built
docker images | grep supremeai

# If missing, rebuild
docker build -t gcr.io/supremeai-production/supremeai:1.0.0 .
```

### Issue: "Authentication required for push"

**Solution:**

```powershell
# Re-configure Docker authentication
gcloud auth configure-docker --quiet
```

### Issue: "Cloud Run deployment timeout"

**Solution:**

```powershell
# Check service status
gcloud run services describe supremeai --region us-central1

# View build logs
gcloud builds log [BUILD_ID]

# Check application logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=supremeai" --limit 50
```

### Issue: "Permission denied"

**Solution:**

```powershell
# Ensure project is set correctly
gcloud config set project supremeai-production

# Check IAM permissions
gcloud projects get-iam-policy supremeai-production
```

---

## 📝 Post-Deployment Configuration

### 1. Configure Custom Domain (Optional)

```powershell
# Map custom domain to main service
gcloud run domain-mappings create \
  --service=supremeai \
  --domain=api.supremeai.dev \
  --region=us-central1

# Verify DNS (update in your domain registrar)
gcloud run domain-mappings describe api.supremeai.dev
```

### 2. Setup Environment Secrets (Optional)

```powershell
# Create secrets
gcloud secrets create firebase-service-account \
  --replication-policy="automatic" \
  --data-file="path/to/service-account.json"

gcloud secrets create jwt-secret \
  --replication-policy="automatic"

# Grant Cloud Run access
gcloud run services update supremeai \
  --update-secrets=FIREBASE_CONFIG_PATH=firebase-service-account:latest
```

### 3. Enable Cloud Monitoring (Optional)

```powershell
# View metrics in Cloud Console
# https://console.cloud.google.com/monitoring

# Create custom dashboard
gcloud monitoring dashboards create --config-from-file=dashboard.json
```

---

## 🎯 Success Criteria

- ✅ Both services deployed to Cloud Run
- ✅ Service URLs returned and accessible
- ✅ `/api/v1/system/health` returns 200 OK
- ✅ `/api/admin/dashboard/health` returns 200 OK
- ✅ Firestore database connected
- ✅ Logs appearing in Cloud Logging
- ✅ Services auto-scaling based on load

---

## 📚 Useful Commands Reference

```powershell
# View service details
gcloud run services describe supremeai --region us-central1

# Update service environment
gcloud run services update supremeai --region us-central1 --update-env-vars KEY=VALUE

# View running revisions
gcloud run revisions list --service=supremeai --region=us-central1

# Scale service
gcloud run services update supremeai --region us-central1 --min-instances=1 --max-instances=10

# Delete service
gcloud run services delete supremeai --region us-central1

# View build history
gcloud builds list --limit 20

# Follow service logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=supremeai" \
  --limit 50 --format json | ConvertFrom-Json | Select-Object -ExpandProperty jsonPayload
```

---

## 🎉 Next Steps After Deployment

1. ✅ Test deployed services (curl health endpoints)
2. ✅ Configure custom domain (optional)
3. ✅ Setup monitoring alerts (optional)
4. ✅ Enable Cloud SQL if needed (optional)
5. ✅ Configure Cloud Armor for DDoS (optional)
6. ✅ Set up VPC and Cloud NAT (optional)
7. ✅ Enable Cloud CDN for caching (optional)

---

## 💰 Cost Monitoring

```powershell
# Estimate monthly costs
Write-Host "Cloud Run Pricing:" -ForegroundColor Cyan
Write-Host "- vCPU: \$0.00002400 per vCPU-second"
Write-Host "- Memory: \$0.00000250 per GiB-second"
Write-Host "- Requests: \$0.40 per million requests"
Write-Host ""
Write-Host "Example: 10M requests/month with main system running continuously:"
Write-Host "- Compute: ~\$20-40/month"
Write-Host "- Requests: ~\$4/month"
Write-Host "- Total: ~\$25-45/month"

# View GCP billing
# https://console.cloud.google.com/billing
```

---

**Status:** 🟢 Ready to Deploy  
**Estimated Time:** 10-15 minutes  
**Created:** March 28, 2026
