# CI/CD Firebase Hosting Deployment Setup

## Updated cloudbuild.yaml Features

✅ **Automatic deployment pipeline includes:**
1. Backend build & deploy to Cloud Run (existing)
2. Dashboard build (React + Vite) 
3. Flutter admin app build (web)
4. Firebase Hosting deployment (NEW)

---

## Setup Instructions

### Step 1: Generate Firebase Token

```bash
# Login to Firebase CLI
firebase login:ci

# This will output a token like:
# 1//0gJ-zT_y5vW...
```

### Step 2: Add Token to Cloud Build

#### Option A: Using Google Cloud Console

1. Go to [Cloud Build](https://console.cloud.google.com/cloud-build/triggers)
2. Click your build trigger for `supremeai-a` repo
3. Edit → Advanced
4. Scroll to **Substitution variables**
5. Click **+ Add variable**
6. Name: `_FIREBASE_TOKEN`
7. Value: `[Your Firebase token from Step 1]`
8. **Save**

#### Option B: Using gcloud CLI

```bash
gcloud builds update --substitutions="_FIREBASE_TOKEN=YOUR_TOKEN_HERE" supremeai-a
```

### Step 3: Test the Pipeline

```bash
# Make a commit and push to main
git add .
git commit -m "Enable Firebase Hosting auto-deploy"
git push origin main

# Cloud Build will automatically:
# 1. Build backend Docker image
# 2. Deploy to Cloud Run
# 3. Build React dashboard
# 4. Build Flutter admin app
# 5. Deploy both to Firebase Hosting
```

---

## Monitoring Deployments

### View Build Logs
```bash
gcloud builds log --stream
```

### View Build History
```bash
gcloud builds list
```

### View Specific Build Details
```bash
gcloud builds log BUILD_ID --stream
```

---

## What Deploys Where

| Component | Host | Trigger |
|-----------|------|---------|
| Spring Boot Backend | Cloud Run | Docker build + deploy |
| Dashboard (React) | Firebase Hosting | npm build → combined_deploy/ |
| Admin App (Flutter) | Firebase Hosting /admin/ | flutter build web → combined_deploy/admin/ |

---

## File Flow

```
Git push origin main
    ↓
Cloud Build triggered
    ↓
├─ Backend: Build Docker → Push → Deploy to Cloud Run
│  (API available at `supremeai-a.run.app`)
│
└─ Frontend:
   ├─ Build dashboard (React + Vite)
   ├─ Build Flutter admin app
   ├─ Copy to combined_deploy/
   └─ Deploy to Firebase Hosting
      (Main: `supremeai-a.web.app`)
      (Admin: `supremeai-a.web.app/admin/`)
```

---

## Troubleshooting

### Build Fails: "Firebase token invalid"
- Regenerate token: `firebase login:ci`
- Update in Cloud Build substitutions
- Retry build

### Build Fails: "Flutter not found"
- Flutter container image is slow to pull (~5min first time)
- Subsequent builds cache it
- Check build logs for actual errors

### Dashboard not updating
- Verify `dashboard/dist/` has `index.html`
- Check `combined_deploy/` has copied files
- Clear Firebase cache: `firebase hosting:disable && firebase hosting:enable`

### Admin app shows blank
- Verify Flutter web build completed
- Check `combined_deploy/admin/flutter_bootstrap.js` exists
- Check browser console for errors

---

## Next Steps

1. ✅ Generate Firebase token (`firebase login:ci`)
2. ✅ Add `_FIREBASE_TOKEN` to Cloud Build substitutions
3. ✅ Make a test commit to trigger the pipeline
4. ✅ Both sites will auto-deploy on every `git push origin main`

After setup, you'll have:
- **Manual deployment**: Just `git push`
- **Automatic deployment**: Everything deploys to both Cloud Run + Firebase Hosting
- **No manual Firebase commands needed** (except for emergency rollbacks)
