# Google Cloud Deployment - Quick Start

## 5-Minute Quick Start

### Prerequisites
- Google Cloud account (https://cloud.google.com)
- Docker installed
- Google Cloud SDK (gcloud CLI)

### Step 1: Install Google Cloud SDK (if not already installed)

**Windows PowerShell (as Administrator):**
```powershell
# Using Chocolatey
choco install google-cloud-sdk

# Or download: https://cloud.google.com/sdk/docs/install-sdk
```

**Verify:**
```powershell
gcloud --version
```

### Step 2: Authenticate & Setup Project

```powershell
# Login to Google Cloud
gcloud auth login

# Create project (if new)
gcloud projects create supremeai-production --name="SupremeAI Production"

# Set as default
gcloud config set project supremeai-production

# Enable required APIs
gcloud services enable run.googleapis.com firestore.googleapis.com cloudbuild.googleapis.com containerregistry.googleapis.com --quiet
```

### Step 3: Configure Docker

```powershell
# Authenticate Docker with GCP
gcloud auth configure-docker
```

### Step 4: Deploy Using Automated Script

**Option A: Deploy Both Systems**
```powershell
cd c:\Users\Nazifa\supremeai

# Run deployment script
.\deploy-to-gcp.ps1 -DeployBoth

# Output will show URLs like:
# Main System: https://supremeai-xxxxx.run.app
# Admin Dashboard: https://supremeai-admin-xxxxx.run.app
```

**Option B: Deploy Only Main System**
```powershell
.\deploy-to-gcp.ps1 -DeployMain
```

**Option C: Deploy Only Admin**
```powershell
cd c:\Users\Nazifa\supremeai-admin
..\supremeai\deploy-to-gcp.ps1 -DeployAdmin
```

### Step 5: Verify Deployment

```powershell
# List deployed services
gcloud run services list

# Get service URLs
gcloud run services describe supremeai --region us-central1 --format 'value(status.url)'
gcloud run services describe supremeai-admin --region us-central1 --format 'value(status.url)'

# Check logs
gcloud logging read "resource.type=cloud_run_revision" --limit 20 --format json
```

### Step 6: Access Your Deployed Services

- **Main System:** Visit the supremeai service URL
- **Admin Dashboard:** Visit the supremeai-admin service URL
- **Test Health:** `https://[URL]/api/v1/system/health` (main) or `https://[URL]/api/admin/dashboard/health` (admin)

---

## Manual Step-by-Step (If Script Fails)

### Build & Push Main System

```powershell
cd c:\Users\Nazifa\supremeai

# Build with Gradle
.\gradlew clean build -x test

# Build Docker image
docker build -t gcr.io/supremeai-production/supremeai:1.0.0 .

# Push to Google Container Registry
docker push gcr.io/supremeai-production/supremeai:1.0.0
```

### Deploy Main System

```powershell
gcloud run deploy supremeai \
  --image gcr.io/supremeai-production/supremeai:1.0.0 \
  --platform managed \
  --region us-central1 \
  --port 8080 \
  --memory 1Gi \
  --cpu 1 \
  --allow-unauthenticated
```

### Build & Push Admin

```powershell
cd c:\Users\Nazifa\supremeai-admin

# Build with Gradle
.\gradlew clean build -x test

# Build Docker image
docker build -t gcr.io/supremeai-production/supremeai-admin:1.0.0 .

# Push to Google Container Registry
docker push gcr.io/supremeai-production/supremeai-admin:1.0.0
```

### Deploy Admin

```powershell
gcloud run deploy supremeai-admin \
  --image gcr.io/supremeai-production/supremeai-admin:1.0.0 \
  --platform managed \
  --region us-central1 \
  --port 8080 \
  --memory 512Mi \
  --allow-unauthenticated
```

---

## Common Issues & Solutions

### Docker build fails: "gradle command not found"
- Solution: Run from project root where `gradlew` exists
- The Dockerfile uses `./gradlew` to build

### Push fails: "authentication required"
```powershell
gcloud auth configure-docker --quiet
```

### Service deployment timeout
- Increase timeout in CLI or wait for image to build
- Check `gcloud run services describe [SERVICE] --region us-central1`

### Out of quota
- Check Google Cloud Console
- May need to upgrade billing account

---

## Configuration

### Set Environment Variables

```powershell
# Create .env file for local reference
@"
# Firebase
FIREBASE_CONFIG_PATH=./config/firebase-service-account.json

# JWT
JWT_SECRET=your-production-secret-key-here

# Main System URL (for admin)
MAIN_SYSTEM_URL=https://supremeai-xxxxx.run.app

# Regions
GCP_REGION=us-central1
"@ | Out-File -FilePath .\gcp-env.txt
```

### Update Cloud Run Environment

```powershell
# Update main system env vars
gcloud run services update supremeai \
  --region us-central1 \
  --update-env-vars FIREBASE_CONFIG_PATH=/secrets/firebase-service-account.json,JWT_SECRET=new-secret-key

# Update admin env vars
gcloud run services update supremeai-admin \
  --region us-central1 \
  --update-env-vars FIREBASE_CONFIG_PATH=/secrets/firebase-service-account.json,MAIN_SYSTEM_URL=https://supremeai-xxxxx.run.app
```

---

## Monitoring After Deployment

### View Real-time Logs

```powershell
# Stream logs from main system
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=supremeai" \
  --limit 50 --format 'value(jsonPayload)' | Select-Object -First 20

# Stream logs from admin
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=supremeai-admin" \
  --limit 50 --format 'value(jsonPayload)' | Select-Object -First 20
```

### Monitor Resources

```powershell
# Get service details
gcloud run services describe supremeai --region us-central1

# Check revision history
gcloud run revisions list --service supremeai --region us-central1
```

---

## Cost Estimation

| Component | Free Tier | Monthly (Production) |
|-----------|-----------|-------------------|
| Cloud Run | 2M requests | $10-40 |
| Firestore | 50k ops/day | $10-30 |
| Cloud Storage | 5GB free | $0-5 |
| **Total** | Generous | **~$25-75** |

---

## Next Steps

1. ✅ Deploy to Cloud Run
2. Test endpoints (HTTP/HTTPS)
3. Configure custom domains
4. Set up monitoring alerts
5. Enable HTTPS/SSL (automatic on .run.app)
6. Configure database backups
7. Setup CI/CD for auto-deployment
8. Production hardening

---

## Useful Commands Reference

```powershell
# List all services
gcloud run services list

# Get service URL
gcloud run services describe supremeai --format='value(status.url)' --region us-central1

# Delete service
gcloud run services delete supremeai --region us-central1

# Update service
gcloud run services update supremeai --region us-central1 --image gcr.io/supremeai-production/supremeai:2.0.0

# View build history
gcloud builds list

# View container images
gcloud container images list

# Tail logs
gcloud logging tails "resource.type=cloud_run_revision" --lines 50

# SSH into running container (for debugging)
gcloud beta run exec --container [CONTAINER_NAME] --service supremeai --region us-central1
```

---

## Documentation
- Cloud Run: https://cloud.google.com/run/docs
- Firestore: https://firebase.google.com/docs/firestore
- Cloud Build: https://cloud.google.com/build/docs
- Google Cloud SDK: https://cloud.google.com/sdk/docs

**Status:** 🟢 Ready for Deployment  
**Last Updated:** March 28, 2026
