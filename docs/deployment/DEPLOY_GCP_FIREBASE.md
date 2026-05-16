# SupremeAI Deployment Guide — Google Cloud & Firebase Hosting

End-to-end deployment instructions for the SupremeAI platform to Google Cloud Run and Firebase Hosting.

## Prerequisites

### 1. Install Required CLIs

```bash
# Google Cloud SDK
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init

# Firebase CLI
npm install -g firebase-tools
firebase login

# Docker
# Download from: https://www.docker.com/products/docker-desktop
```

### 2. Configure Authentication

```bash
# Authenticate gcloud
gcloud auth login
gcloud config set project supremeai-a

# Authenticate Docker to GCR
gcloud auth configure-docker us-central1-docker.pkg.dev

# Authenticate Firebase
firebase use supremeai-a
```

### 3. Set Environment Variables

Create a `.env` file (or export in shell):

```bash
export GCP_PROJECT_ID=supremeai-a
export GCP_REGION=us-central1

# Firebase Config (use existing service account from src/main/resources)
export VITE_FIREBASE_API_KEY=your-firebase-api-key
export VITE_FIREBASE_AUTH_DOMAIN=supremeai-a.firebaseapp.com
export VITE_FIREBASE_PROJECT_ID=supremeai-a
export VITE_FIREBASE_STORAGE_BUCKET=supremeai-a.appspot.com
export VITE_FIREBASE_MESSAGING_SENDER_ID=565236080752
export VITE_FIREBASE_APP_ID=1:565236080752:web:abc123

# Backend API URL (for dashboard to connect to Cloud Run backend)
export VITE_API_URL=https://supremeai-backend-xxxx.a.run.app
```

You can find Firebase config values in the [Firebase Console](https://console.firebase.google.com) → Project Settings.

## Quick Deploy (Recommended)

Run the all-in-one deployment script:

```bash
./deploy_gcp_firebase.sh
```

This script will:
1. ✅ Build Spring Boot JAR
2. ✅ Build React dashboard
3. ✅ Build & push Docker image to GCR
4. ✅ Deploy backend to Cloud Run
5. ✅ Deploy dashboard to Firebase Hosting
6. ✅ (Optional) Deploy Cloud Functions

## Manual Deployment Steps

### Step 1: Build Backend

```bash
./gradlew clean build -x test
```

### Step 2: Build Dashboard

```bash
cd dashboard
npm ci
npm run build
cd ..

# Copy build to Firebase public directory
rm -rf public/admin/*
cp -r dashboard/dist/* public/admin/
```

### Step 3: Deploy Backend to Cloud Run

**Option A: Using deploy.sh (original script)**

```bash
./deploy.sh
```

**Option B: Direct gcloud deployment**

```bash
# Build and push Docker image
docker build -t gcr.io/supremeai-a/supremeai-backend:latest .
gcloud builds submit --tag gcr.io/supremeai-a/supremeai-backend:latest .

# Deploy to Cloud Run
gcloud run deploy supremeai-backend \
  --image gcr.io/supremeai-a/supremeai-backend:latest \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,FIREBASE_PROJECT_ID=supremeai-a" \
  --cpu 2 --memory 2Gi \
  --min-instances 1 --max-instances 10 \
  --timeout 300
```

### Step 4: Deploy Dashboard to Firebase Hosting

```bash
firebase deploy --only hosting
```

Your dashboard will be available at: `https://supremeai-a.web.app/admin/`

### Step 5: (Optional) Deploy Cloud Functions

```bash
cd functions
npm ci
npm run build  # if needed
firebase deploy --only functions
cd ..
```

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Internet / Users                         │
└─────────────┬───────────────────────────┬───────────────────┘
              │                           │
     https://supremeai-a.web.app     https://supremeai-backend-xxxx.a.run.app
              │                           │
    ┌─────────▼──────────┐    ┌──────────▼───────────┐
    │ Firebase Hosting   │    │  Google Cloud Run    │
    │ (Static Dashboard) │    │  (Spring Boot API)   │
    └─────────┬──────────┘    └──────────┬───────────┘
              │                           │
              └───────────┬───────────────┘
                          │
              ┌──────────▼────────────┐
              │   Firebase Services   │
              │  • Auth               │
              │  • Firestore          │
              │  • Realtime DB       │
              │  • Cloud Functions   │
              └───────────────────────┘
```

### URL Structure After Deployment

| Resource | URL Pattern |
|-----------|------------|
| Dashboard | `https://supremeai-a.web.app/admin/` |
| API | `https://supremeai-backend-xxxx.a.run.app/api/**` |
| Auth | Firebase Authentication (managed) |
| Database | Firestore & Realtime Database |

**Note:** `firebase.json` rewrites `/api/**` requests to Cloud Run automatically.

## Configuration Files Reference

| File | Purpose |
|------|---------|
| `Dockerfile` | Multi-stage build for backend container |
| `firebase.json` | Firebase Hosting rewrites & Firestore rules |
| `.firebaserc` | Firebase project alias mapping |
| `cloudbuild.yaml` | Google Cloud Build pipeline |
| `deploy.sh` | Legacy deployment script |
| `deploy_gcp_firebase.sh` | **Recommended** unified deployment script |
| `build.gradle.kts` | Gradle build (Spring Boot 3, Java 21) |
| `dashboard/package.json` | React dashboard dependencies |

## Post-Deployment Verification

### 1. Test Backend Health

```bash
# Get Cloud Run URL
BACKEND_URL=$(gcloud run services describe supremeai-backend --region us-central1 --format='value(status.url)')
curl "$BACKEND_URL/actuator/health"
```

Expected response: `{"status":"UP"}`

### 2. Test Firebase Auth

```bash
curl -X POST "$BACKEND_URL/api/auth/validate-token" \
  -H "Content-Type: application/json" \
  -d '{"idToken":"valid-token-here"}'
```

### 3. Access Dashboard

Open browser: `https://supremeai-a.web.app/admin/`

Login with admin email: `niloyjoy7@gmail.com`

### 4. Check Logs

```bash
# Cloud Run logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=supremeai-backend" \
  --limit 50 --format "json" | less

# Firebase Hosting logs
firebase hosting:channel:list

# Real-time log streaming
gcloud logging tail "resource.type=cloud_run_revision AND resource.labels.service_name=supremeai-backend"
```

## Rollback Procedure

### Cloud Run Rollback

```bash
# List revisions
gcloud run services describe supremeai-backend --region us-central1 --format="value(status.traffic)"

# Rollback to previous revision
gcloud run services revert supremeai-backend --region us-central1
```

### Firebase Hosting Rollback

```bash
# List releases
firebase hosting:channel:list

# Rollback to previous version (manual: re-deploy previous build)
```

## Common Issues & Solutions

### Issue 1: "FirebaseApp with name [DEFAULT] doesn't exist"

**Cause:** Firebase not initialized before use (lazy init issue).

**Fix:** Already applied in commit `e463fa30` — `@Lazy(false)` added to `FirebaseConfig.firebaseApp()`. Verify:

```java
// In FirebaseConfig.java
@Bean
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
@Lazy(false)  // ← Present?
public FirebaseApp firebaseApp(...) { ... }
```

### Issue 2: Authentication failed on dashboard

**Cause:** API URL misconfigured in dashboard build.

**Fix:** Ensure `VITE_API_URL` is set correctly **before** building dashboard:

```bash
export VITE_API_URL=https://supremeai-backend-xxxx.a.run.app
cd dashboard && npm run build && cd ..
firebase deploy --only hosting
```

Verify `firebase.json` rewrites are routing `/api/**` → Cloud Run:

```json
"rewrites": [
  {
    "source": "/api/**",
    "run": {
      "serviceId": "supremeai-backend",
      "region": "us-central1"
    }
  }
]
```

### Issue 3: Cloud Run build fails (Gradle)

**Fix:** Ensure `build.gradle.kts` compiles locally first:

```bash
./gradlew clean build -x test
```

If tests are flaky in CI, skip them:

```bash
./gradlew build -x test
```

**Note:** Cloud Build uses `Dockerfile` which runs `gradle bootJar` — ensure no local uncommitted changes affect build.

### Issue 4: Docker push permission denied

**Fix:** Authenticate Docker to GCR:

```bash
gcloud auth configure-docker us-central1-docker.pkg.dev
```

Or for older GCR:
```bash
gcloud auth configure-docker gcr.io
```

### Issue 5: Firestore permission errors

**Cause:** Service account missing Firestore roles.

**Fix:** Grant IAM roles to Cloud Run service account:

```bash
gcloud projects add-iam-policy-binding supremeai-a \
  --member="serviceAccount:supremeai-backend@supremeai-a.iam.gserviceaccount.com" \
  --role="roles/datastore.user"

gcloud projects add-iam-policy-binding supremeai-a \
  --member="serviceAccount:supremeai-backend@supremeai-a.iam.gserviceaccount.com" \
  --role="roles/firebaseauth.admin"
```

### Issue 6: Dashboard shows stale assets

**Fix:** Clear Firebase Hosting cache:

```bash
# Clear CDN cache
firebase hosting:channel:deploy live --only hosting

# Or use cache-busting query (development)
firebase deploy --only hosting --only "hosting"
```

## Cost Optimization

### Cloud Run Cost Savings

Edit Cloud Run service to scale to zero when idle (dev/staging only):

```bash
gcloud run services update supremeai-backend \
  --region us-central1 \
  --min-instances 0 \
  --max-instances 5 \
  --cpu 1 \
  --memory 1Gi
```

**⚠️ Production:** Keep `min-instances=1` to avoid cold starts.

### Firebase Hosting

Free tier includes:
- 10 GB storage
- 10 GB/month bandwidth
- 125k redirects/month

## Monitoring & Logs

### Cloud Monitoring Dashboard

Create a dashboard in GCP Console → Monitoring with these metrics:

```
- Cloud Run: request_count, response_latency, instance_count
- Firestore: document_read_count, write_count
- Firebase Auth: sign_in_count
```

### Alert Policies (Optional)

```bash
# High error rate alert
gcloud alpha monitoring policies create \
  --policy-from-file="alert-policy.yaml"
```

## CI/CD (GitHub Actions)

The repository includes a GitHub Actions workflow at `.github/workflows/supreme_unified.yml` that automates:

1. Code analysis (CodeQL)
2. Backend build & test (Gradle)
3. Docker image build & push
4. Cloud Run deployment (on merge to main)
5. Post-deployment validation

To enable:
- Push to `main` branch → auto-deploys
- Or trigger manually via GitHub Actions UI

## Security Checklist

- ✅ Service account keys NOT committed (use Firebase default credentials in Cloud Run)
- ✅ JWT_SECRET set via environment variable
- ✅ Firebase Auth used for authentication
- ✅ Firestore security rules enforced
- ✅ Cloud Run with minimal IAM roles
- ✅ HTTPS enforced by Firebase Hosting & Cloud Run

## Support

For issues:
1. Check logs: `gcloud logging read ...`
2. Verify Firebase config: `firebase apps:list`
3. Test locally: `./gradlew bootRun` + `cd dashboard && npm run dev`
4. Open GitHub issue with logs attached

---

**Last updated:** 2026-05-13
**Deployment script:** `./deploy_gcp_firebase.sh`
