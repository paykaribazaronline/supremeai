# SupremeAI Deployment Guide

Complete setup for autonomous business partner features: Simulator Runtime + Reverse Engineering.

## Prerequisites

- GCP project with billing enabled
- `gcloud` CLI installed and authenticated (`gcloud auth login`)
- Docker installed (for building images)
- Java 21, Gradle
- Python 3.11+ (for local testing of Python services)
- Firebase CLI (`npm install -g firebase-tools`) optional

## Architecture Overview

```
[User] → React Dashboard (React Admin) → Spring Boot Backend (Cloud Run)
           │                                    │
           │                                    ├→ Pub/Sub → Reverse Eng Service (FastAPI, Cloud Run)
           │                                    │              ↓
           │                                    │           Firestore (jobs)
           │                                    │
           │                                    └→ SimulatorDeploymentService (gcloud CLI)
           │                                         ↓
           │                                    Cloud Run (simulator-runtime image)
           │                                         ↓
           └────────────────────────────────────────┘
                      Preview iframe (/api/simulator/preview/{appId})
```

## Step-by-Step Deployment

### 1. Configure Environment

Set environment variables for the Spring Boot backend:

```bash
export GCP_PROJECT_ID=supremeai-459910  # or your project
export SPRING_CLOUD_GCP_PROJECT_ID=$GCP_PROJECT_ID
export DATABASE_URL=jdbc:postgresql://<host>:5432/supremeai
# ... other env vars from application.yml
```

### 2. Create GCP Infrastructure

Run the setup script:

```bash
cd infrastructure
./setup.sh
```

Or manually:

```bash
# Enable APIs
gcloud services enable \
  run.googleapis.com \
  pubsub.googleapis.com \
  firestore.googleapis.com \
  cloudbuild.googleapis.com

# Create Pub/Sub topic
gcloud pubsub topics create reverse-engineering-jobs

# Create Firestore database (native mode)
gcloud firestore databases create --region=us-central1

# Create service account for reverse-engineering service
gcloud iam service-accounts create reverse-engineering \
  --display-name="Reverse Engineering Service"

# Grant roles
gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:reverse-engineering@${GCP_PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/datastore.user"
gcloud projects add-iam-policy-binding $GCP_PROJECT_ID \
  --member="serviceAccount:reverse-engineering@${GCP_PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/pubsub.subscriber"
```

### 3. Build & Deploy Python Services

#### Reverse Engineering Service

```bash
cd reverse-engineering

# Build Docker image
docker build -t gcr.io/$GCP_PROJECT_ID/reverse-engineering:latest .

# Push to Google Container Registry
docker push gcr.io/$GCP_PROJECT_ID/reverse-engineering:latest

# Deploy to Cloud Run
gcloud run deploy reverse-engineering \
  --image gcr.io/$GCP_PROJECT_ID/reverse-engineering:latest \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars GOOGLE_CLOUD_PROJECT=$GCP_PROJECT_ID \
  --service-account reverse-engineering@${GCP_PROJECT_ID}.iam.gserviceaccount.com
```

Note the URL output: `https://reverse-engineering-xxxxxx-uc.a.run.app`

#### Simulator Runtime Service

```bash
cd simulator-runtime

docker build -t gcr.io/$GCP_PROJECT_ID/simulator-runtime:latest .
docker push gcr.io/$GCP_PROJECT_ID/simulator-runtime:latest

gcloud run deploy simulator-runtime \
  --image gcr.io/$GCP_PROJECT_ID/simulator-runtime:latest \
  --region us-central1 \
  --allow-unauthenticated \
  --set-min-instances=0 \
  --set-max-instances=10
```

### 4. Create Pub/Sub Push Subscription

Point Pub/Sub to the deployed reverse-engineering service:

```bash
gcloud pubsub subscriptions create reverse-engineering-jobs-push \
  --topic=reverse-engineering-jobs \
  --push-endpoint=https://reverse-engineering-xxxxxx-uc.a.run.app/pubsub/push \
  --push-auth-service-account=reverse-engineering@${GCP_PROJECT_ID}.iam.gserviceaccount.com
```

### 5. Deploy Spring Boot Backend

Build and deploy backend:

```bash
./gradlew clean build -x test

# Build Docker image
docker build -t gcr.io/$GCP_PROJECT_ID/supremeai-backend:latest .

docker push gcr.io/$GCP_PROJECT_ID/supremeai-backend:latest

gcloud run deploy supremeai-backend \
  --image gcr.io/$GCP_PROJECT_ID/supremeai-backend:latest \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="GCP_PROJECT_ID=$GCP_PROJECT_ID" \
  --set-env-vars="JWT_SECRET=<your-jwt-secret>" \
  --set-env-vars="OPENAI_API_KEY=<key>" \
  --set-env-vars="GEMINI_API_KEY=<key>" \
  --service-account <backend-service-account> \
  --cpu 2 --memory 2Gi
```

Ensure the backend service account has:
- `roles/datastore.user` (Firestore)
- `roles/pubsub.publisher` (to publish to reverse-engineering-jobs)
- `roles/run.admin` (to deploy simulator Cloud Run services via gcloud)

### 6. Deploy React Dashboard

```bash
cd dashboard
npm install
npm run build

# Deploy to Firebase Hosting or Cloud Run
firebase deploy --only hosting
```

Or serve statically.

### 7. Verify Firestore Indexes

Some queries may require composite indexes. The first time a query fails, Firestore provides a link to create the required index. Click it to auto-create.

Required indexes (pre-create):
- `ReverseEngineeringJob` collection: `createdAt DESC` (for `findAllByOrderByCreatedAtDesc` — not directly used; we sort in-memory)
- `GeneratedApp` collection: `userId ASC, platform ASC`

### 8. Test End-to-End

1. Open dashboard at `https://<your-dashboard>.web.app`
2. Login as admin
3. Navigate to **Simulator** tab
4. Generate an app via **Projects** or **AI Chat**
5. In Admin → Simulator, select the app and click **Live Preview**
   - Backend deploys simulator Cloud Run service (using gcloud)
   - Frontend loads iframe pointing to `/api/simulator/preview/{appId}`
   - Simulator runtime serves the app with device emulation
6. Navigate to **Reverse Engineer** tab
   - Submit a URL (e.g., `https://example.com`)
   - Job appears in history with status PENDING
   - Python FastAPI worker processes (via Pub/Sub push)
   - Status changes to COMPLETED with discovered endpoints

## Operations

### Viewing Logs

```bash
# Backend
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=supremeai-backend" --limit 50

# Reverse Engineering
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=reverse-engineering"

# Simulator instances (per-app)
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=sim-*"
```

### Cleaning Up

```bash
# Delete all simulator deployments (in-memory registry lost on restart; use Firestore for persistence if needed)
# Or use SimulatorController's undeploy endpoint if added.

# Remove Pub/Sub topic
gcloud pubsub topics delete reverse-engineering-jobs

# Remove Cloud Run services
gcloud run services delete reverse-engineering --region us-central1
gcloud run services delete simulator-runtime --region us-central1
```

## Troubleshooting

### Simulator deployment fails (`gcloud` command not found)
- Ensure Cloud Run Admin API is enabled
- Ensure the backend service account has `roles/run.admin`
- Ensure `gcloud` is installed on the backend container (if using Cloud Run, use a custom image with gcloud installed, or switch to Cloud Run Admin API client library)

### Reverse engineering jobs stuck in PENDING
- Check Pub/Sub push subscription exists and points to correct URL
- Verify reverse-engineering service has Firestore access
- Check service logs for `/pubsub/push` endpoint errors

### Preview iframe 404
- Confirm `SimulatorRuntimeController` is mapped to `/api/simulator/preview/{appId}`
- Check `GeneratedApp` exists in Firestore `generated_apps` collection
- Verify device emulation context has correct device profile

### Firestore permission denied
- Ensure service accounts have `datastore.user` role
- For local dev, set `GOOGLE_APPLICATION_CREDENTIALS` path to service account key

## Security Notes

- All admin endpoints secured with `@PreAuthorize("hasRole('ADMIN')")`
- Public endpoints (preview) are unauthenticated — Cloud Run IAM or API Gateway recommended for production
- Store secrets in Secret Manager, not environment variables
- Rotate JWT secret regularly
