# SupremeAI Production Deployment Guide

**Goal:** Deploy both the Spring Boot backend and Flutter admin dashboard to production with proper SSL certificates.

---

## 🚀 STEP 1: Prepare Google Cloud Project

### 1.1 Set Up GCP Project

```bash
# Set your GCP project ID (replace with your actual project)
export GCP_PROJECT_ID="supremeai-production"  # or your actual project ID

# Initialize gcloud CLI
gcloud init
gcloud config set project $GCP_PROJECT_ID

# Enable required APIs
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  containerregistry.googleapis.com \
  firestore.googleapis.com \
  firebase.googleapis.com
```

### 1.2 Verify Firebase Project

```bash
# List available Firebase projects
firebase projects:list

# Use your Firebase project (should be supremeai-a based on .firebaserc)
firebase use supremeai-a
```

---

## 🐳 STEP 2: Build and Deploy Backend Service

### 2.1 Build Docker Image Locally (Optional - for testing)

```bash
cd c:\Users\Nazifa\supremeai

# Build Docker image
docker build -t supremeai:latest .

# Test locally
docker run -p 8080:8080 \
  -e FIREBASE_CONFIG_PATH=/secrets/firebase-service-account.json \
  supremeai:latest
```

### 2.2 Deploy via Google Cloud Build (RECOMMENDED)

**Option A: Using GitHub Integration (Auto-deploy on push)**

1. Go to Google Cloud Console → Cloud Build → Connected repositories
2. Connect your GitHub repo: `supremeai/supremeai`
3. Create build trigger:
   - Name: `supremeai-main-build`
   - Branch: `main`
   - Configuration file: `cloudbuild.yaml`
   - Substitution variables:
     - `_GCP_PROJECT_ID`: your-gcp-project-id

**Option B: Manual Deployment (Using script)**

```bash
cd c:\Users\Nazifa\supremeai

# Authenticate with Google Cloud
gcloud auth login
gcloud auth configure-docker

# Run deployment script
.\deploy-to-gcp.ps1 -ProjectId supremeai-production -DeployBoth

# Or manual gcloud command:
gcloud builds submit --config=cloudbuild.yaml \
  --project=supremeai-production \
  --substitutions="SHORT_SHA=latest"
```

### 2.3 Check Deployment Status

```bash
# View Cloud Run services
gcloud run services list --project=supremeai-production

# View deployment logs
gcloud run services describe supremeai --project=supremeai-production --region=us-central1

# Expected output shows the service URL: https://supremeai-<PROJECT_ID>.us-central1.run.app
```

---

## 📱 STEP 3: Update Flutter Admin App Configuration

### 3.1 Get the Correct Backend URL

After Cloud Run deployment completes, you'll see a URL like:

```
https://supremeai-supremeai-production.us-central1.run.app
```

**Extract your actual service URL:**

```bash
gcloud run services describe supremeai \
  --project=supremeai-production \
  --region=us-central1 \
  --format='value(status.url)'
```

### 3.2 Update Flutter Environment Configuration

File: `flutter_admin_app/lib/config/environment.dart`

```dart
// Environment configuration for SupremeAI Admin App

class Environment {
  // UPDATE THIS WITH YOUR ACTUAL CLOUD RUN URL
  // Get it from: gcloud run services describe supremeai --format='value(status.url)'
  static const String baseUrl = 'https://supremeai-YOURPROJECT.us-central1.run.app';
  
  // For local development (keep commented)
  // static const String baseUrl = 'http://localhost:8080';
  
  // ... rest of endpoints
}
```

Replace `YOURPROJECT` with your GCP project ID (e.g., `supremeai-production`).

---

## 🔧 STEP 4: Rebuild and Deploy Frontend

### 4.1 Rebuild Flutter Web Admin App

```bash
cd flutter_admin_app

# Install dependencies
flutter pub get

# Build web with admin prefix
flutter build web --base-href "/admin/" --release

# This creates: flutter_admin_app/build/web/
```

### 4.2 Rebuild HTML Dashboard

```bash
# The main dashboard is already in combined_deploy/
# If you have a build step, run it here
# Example: npm run build (if using React/Node)
```

### 4.3 Prepare Deployment Files

```bash
# Combine both into deployment folder (already done in combined_deploy/)
cd c:\Users\Nazifa\supremeai

# Verify structure
ls combined_deploy/
ls combined_deploy/admin/

# Expected:
# combined_deploy/
# ├── index.html (main dashboard)
# ├── admin/ (Flutter web app)
# └── ... other assets
```

### 4.4 Deploy to Firebase Hosting

```bash
# Make sure you're authenticated
firebase login

# Deploy to Firebase
firebase deploy --only hosting:supremeai-a

# View deployment logs
firebase hosting:channel:list --project=supremeai-a
```

---

## ✅ STEP 5: Verify End-to-End Deployment

### 5.1 Test Backend Health

```bash
# Replace with your actual URL
curl -X GET https://supremeai-supremeai-production.us-central1.run.app/actuator/health

# Expected response:
# {"status":"UP"}
```

### 5.2 Test Login Flow

1. Go to: https://supremeai-a.web.app/admin/#/login
2. Enter credentials:
   - Email: `supremeai@admin.com`

- Password: value configured in `SUPREMEAI_ADMIN_PASSWORD`

3. Check browser DevTools (F12):
   - Network tab: Should see successful response from backend API
   - Application tab: localStorage should have `supremeai_token`
4. Should redirect to admin dashboard

### 5.3 Verify "Remember Me" Works

1. Check "Remember me" before login
2. Close browser completely
3. Reopen admin login page
4. Email should auto-fill
5. Check localStorage for 30-day expiry timestamp

---

## 🔒 STEP 6: Security Checklist

### Before Going Live

- [ ] Backend has valid SSL certificate (Cloud Run auto-manages this)
- [ ] Firebase project is set to production security rules
- [ ] Admin user created in Firebase Authentication
- [ ] Environment variables set in Cloud Run deployment
- [ ] FIREBASE_TOKEN secret created in GitHub Actions
- [ ] FIREBASE_SERVICE_ACCOUNT secret set for Cloud Build
- [ ] Backend is not publicly accessible without authentication
- [ ] JWT tokens are properly validated
- [ ] HTTPS only (no HTTP fallback)

### Security Variables to Check

```bash
# In Cloud Run, verify these are set:
gcloud run services describe supremeai --project=supremeai-production --region=us-central1

# Should show environment variables:
# FIREBASE_CONFIG_PATH=/secrets/firebase-service-account.json
# BUILD_TIMESTAMP=...
```

---

## 🚨 Troubleshooting

### Backend Deployment Fails

```bash
# Check Cloud Build logs
gcloud builds log --limit=50

# Check Cloud Run service logs
gcloud run services describe supremeai --project=supremeai-production --region=us-central1
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=supremeai" --limit=50 --format=json
```

### "Dangerous" SSL Warning Still Shows

- [ ] Wait 5-10 minutes for SSL certificate to propagate
- [ ] Hard refresh browser (Ctrl+Shift+R)
- [ ] Clear service worker cache
- [ ] Check backend URL in Flutter app matches Cloud Run URL exactly
- [ ] Verify no typos in `flutter_admin_app/lib/config/environment.dart`

### Admin Can't Login

- [ ] Check admin user exists in Firebase
- [ ] Verify backend can reach Firebase APIs
- [ ] Check network tab in DevTools for API response
- [ ] Verify JWT token is being returned from `/api/auth/login`
- [ ] Check backend logs for auth errors

### Firebase Hosting Deployment Fails

- [ ] Verify `.firebaserc` has correct project ID (`supremeai-a`)
- [ ] Run: `firebase use supremeai-a`
- [ ] Verify `combined_deploy/` directory has files
- [ ] Check `firebase.json` points to correct `public` directory
- [ ] Run: `firebase deploy --only hosting --debug`

---

## 📊 Architecture After Deployment

```
User Browser
    ↓
https://supremeai-a.web.app (Firebase Hosting)
    ├─→ /admin/ → Flutter Web App
    │      ↓
    │   HTTP → Backend
    │
    └─→ / → Main Dashboard
       
Backend (Google Cloud Run)
https://supremeai-PROJECT_ID.us-central1.run.app
    ├─→ /api/auth/login
    ├─→ /api/auth/setup
    ├─→ /api/projects/*
    └─→ ... other APIs
            ↓
        Firebase
    (Authentication, Firestore, Storage)
```

---

## 📝 Quick Reference

| Component | URL | Status |
|-----------|-----|--------|
| Main Dashboard | `https://supremeai-a.web.app/` | Firebase Hosting |
| Admin Panel | `https://supremeai-a.web.app/admin/#/login` | Flutter Web |
| Backend API | `https://supremeai-[PROJECT_ID].us-central1.run.app` | Cloud Run |
| Firebase Console | `https://console.firebase.google.com` | supremeai-a |
| GCP Console | `https://console.cloud.google.com` | supremeai-production |

---

## 🎯 Next Steps

1. ✅ Determine your GCP Project ID
2. ✅ Run deployment script or manual gcloud commands
3. ✅ Get Cloud Run URL
4. ✅ Update Flutter app `environment.dart`
5. ✅ Rebuild and deploy everything
6. ✅ Test end-to-end login flow
