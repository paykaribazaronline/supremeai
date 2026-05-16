# Deploy SupremeAI to Google Cloud & Firebase — Quick Start

> **One-command deployment after initial setup**

## Prerequisites (One-time Setup)

```bash
# Install CLIs
curl https://sdk.cloud.google.com | bash && exec -l $SHELL
npm install -g firebase-tools

# Authenticate
gcloud auth login
gcloud config set project supremeai-a
firebase login
firebase use supremeai-a
```

## Deploy Now

### Option A: All-in-One (Recommended)

```bash
# 1. Setup infrastructure (service accounts, APIs, Pub/Sub)
./infrastructure/setup.sh

# 2. Deploy everything (backend + dashboard)
./deploy_gcp_firebase.sh
```

**Total time:** ~10-15 minutes

### Option B: Manual Step-by-Step

```bash
# 1. Build backend
./gradlew clean build -x test

# 2. Build dashboard
cd dashboard && npm ci && npm run build && cd ..
rm -rf public/admin/* && cp -r dashboard/dist/* public/admin/

# 3. Deploy backend to Cloud Run
gcloud run deploy supremeai-backend \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,FIREBASE_PROJECT_ID=supremeai-a" \
  --cpu 2 --memory 2Gi

# 4. Deploy dashboard to Firebase Hosting
firebase deploy --only hosting

# 5. (Optional) Deploy Cloud Functions
cd functions && npm ci && firebase deploy --only functions && cd ..
```

## Verify Deployment

```bash
# Check backend health
BACKEND_URL=$(gcloud run services describe supremeai-backend --region us-central1 --format='value(status.url)')
curl "$BACKEND_URL/actuator/health"

# Expected: {"status":"UP"}

# Access dashboard
echo "Dashboard: https://supremeai-a.web.app/admin/"
echo "Backend API: $BACKEND_URL"
```

## Admin Login

Use the admin email configured in Firebase Auth:
- **Email:** `niloyjoy7@gmail.com`
- **Password:** Set via Firebase Console → Authentication → Users → Reset password

Or set admin claim programmatically:

```bash
node scripts/add_admin.js niloyjoy7@gmail.com
```

## Rollback

```bash
# Cloud Run revert
gcloud run services revert supremeai-backend --region us-central1

# Firebase Hosting rollback (re-deploy previous build)
# Check releases: firebase hosting:channel:list
```

## Troubleshooting

### "FirebaseApp doesn't exist" error
Fixed in `FirebaseConfig.java`. Ensure you're running latest commit:
```bash
git pull origin master
./gradlew clean build
```

### Dashboard not loading
Check `firebase.json` rewrites. Dashboard should be at `/admin/` path:
```
https://supremeai-a.web.app/admin/
```

### Backend returns 401
Ensure Firebase Auth is enabled and service account has `roles/firebaseauth.admin`:
```bash
gcloud projects add-iam-policy-binding supremeai-a \
  --member="serviceAccount:supremeai-backend@supremeai-a.iam.gserviceaccount.com" \
  --role="roles/firebaseauth.admin"
```

### Docker build fails
Build locally first to debug:
```bash
./gradlew clean build -x test
```

### Firebase deploy fails: "Auth error"
```bash
firebase logout && firebase login
firebase use supremeai-a
```

## Cost Monitoring

```bash
# View Cloud Run costs
gcloud billing accounts list
gcloud beta billing budgets create --billing-account=ACCOUNT_ID --display-name="SupremeAI Budget" --budget-amount=50

# View daily spend
gcloud beta billing budgets list --billing-account=ACCOUNT_ID
```

## Stop / Tear Down

```bash
# Delete Cloud Run services
gcloud run services delete supremeai-backend --region us-central1 --platform managed --quiet
gcloud run services delete reverse-engineering --region us-central1 --quiet
gcloud run services delete simulator-runtime --region us-central1 --quiet

# Disable APIs (optional)
gcloud services disable cloudrun.googleapis.com --project supremeai-a

# Delete Firebase Hosting
firebase hosting:channel:delete live

# Note: Firestore and Firebase Auth data persists unless manually deleted.
```

## Files Overview

| File | Purpose |
|------|---------|
| `deploy_gcp_firebase.sh` | Main deployment script (recommended) |
| `infrastructure/setup.sh` | Creates service accounts, APIs, Pub/Sub |
| `Dockerfile` | Backend container definition |
| `firebase.json` | Hosting rewrites + Firestore rules |
| `cloudbuild.yaml` | CI/CD pipeline (GitHub Actions) |
| `DEPLOY_GCP_FIREBASE.md` | Comprehensive deployment guide |

## Support

- **Deployment issues:** Check `gcloud run services logs read supremeai-backend --region us-central1`
- **Firebase Auth issues:** Check Firebase Console → Authentication → Users
- **Build errors:** Ensure Java 21 and Gradle wrapper are working: `./gradlew --version`

---

Ready? Start deployment:

```bash
./infrastructure/setup.sh
./deploy_gcp_firebase.sh
```
