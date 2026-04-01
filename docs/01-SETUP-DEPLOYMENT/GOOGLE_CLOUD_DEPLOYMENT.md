# Google Cloud Deployment Guide - SupremeAI System

## Complete Setup for Deploying to Google Cloud

### Prerequisites Checklist

- [ ] Google Cloud account with billing enabled
- [ ] Firebase project configured
- [ ] Docker installed locally
- [ ] Google Cloud SDK (gcloud CLI) to be installed
- [ ] Both SupremeAI and Admin projects ready

---

## Step 1: Install Google Cloud SDK

### Install on Windows 10/11

**Option A: Using Installer (Recommended)**

1. Download: https://cloud.google.com/sdk/docs/install-sdk
2. Select Windows 64-bit installer
3. Run the downloaded `.exe` file
4. Follow installation wizard
5. Accept default installation path: `C:\Program Files (x86)\Google\Cloud SDK`

**Option B: Using Chocolatey**

```powershell
# Run as Administrator
choco install google-cloud-sdk
```

**Option C: Using PowerShell (Skip step 1 if using above)**

```powershell
# Download Cloud SDK
$url = "https://dl.google.com/dl/cloudsdk/channels/rapid/GoogleCloudSDKInstaller.exe"
$out = "$env:TEMP\GoogleCloudSDKInstaller.exe"
Invoke-WebRequest -Uri $url -OutFile $out

# Run installer
& $out
```

### Verify Installation

```powershell
gcloud --version

# Expected output:
# Google Cloud SDK [VERSION]
# app-engine-python [VERSION]
# bq [VERSION]
# core [VERSION]
# gsutil [VERSION]
```

---

## Step 2: Initialize Google Cloud SDK

```powershell
# Initialize gcloud
gcloud init

# You will be prompted:
# 1. Login to Google account
# 2. Create new project or select existing
# 3. Set default region
```

### Quick Setup

```powershell
# Login
gcloud auth login

# Set default project
gcloud config set project supremeai-production

# Verify configuration
gcloud config list
```

---

## Step 3: Create Google Cloud Project

### Via Console (Recommended)

1. Go to: https://console.cloud.google.com/
2. Click "Select a Project" → "New Project"
3. Name: `supremeai-production`
4. Organization: (if applicable)
5. Click "Create"

### Via CLI

```powershell
gcloud projects create supremeai-production \
  --name="SupremeAI Production" \
  --set-as-default
```

### Enable Required APIs

```powershell
# Enable Cloud Run API
gcloud services enable run.googleapis.com

# Enable Firestore API
gcloud services enable firestore.googleapis.com

# Enable Cloud Build API
gcloud services enable cloudbuild.googleapis.com

# Enable Container Registry
gcloud services enable containerregistry.googleapis.com

# Enable Service Usage API
gcloud services enable serviceusage.googleapis.com
```

---

## Step 4: Configure Firebase Project

```powershell
# Install Firebase CLI
npm install -g firebase-tools

# Verify installation
firebase --version

# Login to Firebase
firebase login

# Initialize Firebase in your project
cd c:\Users\Nazifa\supremeai
firebase init

# Select:
# - Firestore
# - Cloud Functions
# - Hosting
```

---

## Step 5: Create Docker Images

### For Main SupremeAI System

**Create Dockerfile:**

```dockerfile
FROM openjdk:17-slim

WORKDIR /app

# Copy gradlew and gradle
COPY gradlew .
COPY gradle gradle

# Copy build configs
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy source code
COPY src src

# Build application
RUN ./gradlew build -x test

# Extract JAR
RUN cp build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Set environment
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build Docker image:**

```powershell
cd c:\Users\Nazifa\supremeai

docker build -t gcr.io/supremeai-production/supremeai:1.0.0 .

# Verify image
docker images | grep supremeai
```

### For Admin Dashboard

**Create Dockerfile for Admin:**

```dockerfile
FROM openjdk:17-slim

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

RUN ./gradlew build -x test

RUN cp build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build Docker image:**

```powershell
cd c:\Users\Nazifa\supremeai-admin

docker build -t gcr.io/supremeai-production/supremeai-admin:1.0.0 .

# Verify image
docker images | grep admin
```

---

## Step 6: Push Docker Images to Google Container Registry

### Setup Authentication

```powershell
# Configure Docker authentication
gcloud auth configure-docker

# Verify authentication
docker ps
```

### Push Images

```powershell
# Push main system image
docker push gcr.io/supremeai-production/supremeai:1.0.0

# Push admin image
docker push gcr.io/supremeai-production/supremeai-admin:1.0.0

# Verify pushed images
gcloud container images list
```

---

## Step 7: Deploy to Cloud Run

### Deploy Main SupremeAI System

```powershell
gcloud run deploy supremeai \
  --image gcr.io/supremeai-production/supremeai:1.0.0 \
  --platform managed \
  --region us-central1 \
  --port 8080 \
  --memory 1Gi \
  --cpu 1 \
  --timeout 3600 \
  --max-instances 10 \
  --no-allow-unauthenticated \
  --set-env-vars FIREBASE_CONFIG_PATH=/secrets/firebase-service-account.json,JWT_SECRET=your-secret-key,DATABASE_URL=your-firestore-url
```

### Deploy Admin Dashboard

```powershell
gcloud run deploy supremeai-admin \
  --image gcr.io/supremeai-production/supremeai-admin:1.0.0 \
  --platform managed \
  --region us-central1 \
  --port 8080 \
  --memory 512Mi \
  --cpu 1 \
  --timeout 3600 \
  --max-instances 5 \
  --no-allow-unauthenticated \
  --set-env-vars FIREBASE_CONFIG_PATH=/secrets/firebase-service-account.json,JWT_SECRET=your-secret-key,MAIN_SYSTEM_URL=https://supremeai-xxxxxxx.run.app
```

### Get Service URLs

```powershell
# Get supremeai URL
gcloud run services describe supremeai --region us-central1 --format='value(status.url)'

# Get admin URL
gcloud run services describe supremeai-admin --region us-central1 --format='value(status.url)'
```

---

## Step 8: Configure Environment Secrets

### Using Secret Manager

```powershell
# Create secrets
gcloud secrets create firebase-service-account \
  --replication-policy="automatic" \
  --data-file="path/to/service-account.json"

gcloud secrets create jwt-secret \
  --replication-policy="automatic"

# Grant Cloud Run access
gcloud projects add-iam-policy-binding supremeai-production \
  --member=serviceAccount:supremeai@supremeai-production.iam.gserviceaccount.com \
  --role=roles/secretmanager.secretAccessor
```

### Mount Secrets in Cloud Run

```powershell
gcloud run deploy supremeai \
  --image gcr.io/supremeai-production/supremeai:1.0.0 \
  --region us-central1 \
  --update-secrets=FIREBASE_CONFIG_PATH=firebase-service-account:latest \
  --update-secrets=JWT_SECRET=jwt-secret:latest
```

---

## Step 9: Setup Custom Domain & SSL

### Add Custom Domain

```powershell
gcloud run domain-mappings create \
  --service=supremeai \
  --domain=api.supremeai.dev \
  --region=us-central1

gcloud run domain-mappings create \
  --service=supremeai-admin \
  --domain=admin.supremeai.dev \
  --region=us-central1
```

### Configure DNS

1. Go to Cloud Console
2. Cloud Run → Manage Custom Domains
3. Note the CNAME values
4. Update DNS provider (GoDaddy, etc.) with CNAME records
5. Wait for DNS propagation (5-48 hours)

---

## Step 10: Setup Firestore Database

```powershell
# Create Firestore database
gcloud firestore databases create \
  --region=us-central1 \
  --type=firestore-native

# Collections will auto-create when first written to:
# - admin_users
# - ai_providers
# - ai_agents
# - projects
# - audit_logs
# - quotas
```

---

## Step 11: Setup Cloud Build for CI/CD

### Create `cloudbuild.yaml` for Main System

```yaml
steps:
  # Build image
  - name: 'gcr.io/cloud-builders/docker'
    args:
      - 'build'
      - '-t'
      - 'gcr.io/$PROJECT_ID/supremeai:$SHORT_SHA'
      - '-f'
      - 'Dockerfile'
      - '.'

  # Push to registry
  - name: 'gcr.io/cloud-builders/docker'
    args:
      - 'push'
      - 'gcr.io/$PROJECT_ID/supremeai:$SHORT_SHA'

  # Deploy to Cloud Run
  - name: 'gcr.io/cloud-builders/gke-deploy'
    args:
      - run
      - '--filename=k8s/'
      - '--image=gcr.io/$PROJECT_ID/supremeai:$SHORT_SHA'
      - '--location=us-central1'

images:
  - 'gcr.io/$PROJECT_ID/supremeai:$SHORT_SHA'
```

### Enable Cloud Build Trigger

```powershell
# Connect GitHub repository
gcloud builds connect --repository-name=supremeai \
  --repository-owner=paykaribazaronline \
  --region=us-central1

# Create trigger
gcloud builds triggers create github \
  --repo-name=supremeai \
  --repo-owner=paykaribazaronline \
  --name=supremeai-build \
  --branch-pattern=^main$ \
  --build-config=cloudbuild.yaml
```

---

## Step 12: Monitoring & Logging

### View Logs

```powershell
# View supremeai logs
gcloud logging read \
  "resource.type=cloud_run_revision AND resource.labels.service_name=supremeai" \
  --limit 50 \
  --format json

# View admin logs
gcloud logging read \
  "resource.type=cloud_run_revision AND resource.labels.service_name=supremeai-admin" \
  --limit 50 \
  --format json
```

### Setup Monitoring

```powershell
# Create uptime check
gcloud monitoring uptime-checks create \
  --display-name="SupremeAI API" \
  --resource-type=uptime-url \
  --monitored-resource-labels=host=api.supremeai.dev \
  --http-check-path=/api/v1/system/health \
  --period=60
```

---

## Step 13: Database Backup & Recovery

```powershell
# Enable automatic backups
gcloud firestore backups create \
  --collection-ids=admin_users,ai_providers,ai_agents,projects \
  --region=us-central1

# Restore from backup
gcloud firestore restore \
  --backup=projects/supremeai-production/locations/us-central1/backups/[BACKUP_ID] \
  --collection-ids=admin_users,ai_providers
```

---

## Deployment Checklist

- [ ] Google Cloud SDK installed
- [ ] Google Cloud project created
- [ ] Firebase project initialized
- [ ] APIs enabled (Cloud Run, Firestore, etc.)
- [ ] Docker images built
- [ ] Docker images pushed to GCR
- [ ] Services deployed to Cloud Run
- [ ] Environment variables/secrets configured
- [ ] Custom domains configured
- [ ] DNS records updated
- [ ] Firestore database created
- [ ] Cloud Build configured
- [ ] Monitoring/logging setup
- [ ] Backups enabled
- [ ] SSL certificates working
- [ ] Load testing completed

---

## Estimated Costs

| Service | Usage | Monthly Cost |
|---------|-------|--------------|
| Cloud Run | 2 services × 1GB × 1CPU | ~$20-40 |
| Firestore | 1M reads + 1M writes | ~$10-30 |
| Cloud Storage | 10GB | ~$0.20 |
| Cloud Build | 50 builds × 15min | ~$2 |
| Secret Manager | 1 secret | ~$0.06 |
| **Total Estimated** | Production workload | **~$35-75/month** |

---

## Troubleshooting

### Service Unreachable

```powershell
# Check service status
gcloud run services describe supremeai --region us-central1

# View recent revisions
gcloud run revisions list --service=supremeai --region=us-central1

# Check logs for errors
gcloud logging read "resource.type=cloud_run_revision" --limit 100
```

### Database Connection Issues

```powershell
# Verify Firestore database exists
gcloud firestore databases list

# Check IAM permissions
gcloud projects get-iam-policy supremeai-production
```

### Image Push Failures

```powershell
# Re-authenticate Docker
gcloud auth configure-docker

# Verify image exists
docker images | grep supremeai

# Retry push
docker push gcr.io/supremeai-production/supremeai:1.0.0
```

---

## Next Steps After Deployment

1. ✅ Test deployed services
2. ✅ Configure custom domain
3. ✅ Set up monitoring & alerting
4. ✅ Enable CloudSQL (if needed)
5. ✅ Configure Cloud Armor (DDoS protection)
6. ✅ Setup VPC & Cloud NAT
7. ✅ Configure Cloud CDN for caching
8. ✅ Setup disaster recovery

---

**Documentation:** https://cloud.google.com/run/docs  
**Firebase Docs:** https://firebase.google.com/docs  
**Status:** Ready for Deployment  
**Created:** March 28, 2026
