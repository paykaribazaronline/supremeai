# Firebase Deployment Guide for Gitingest & GitReverse

## Overview

This guide covers deploying both applications to Firebase and Google Cloud:

- **Gitingest**: Python FastAPI → Cloud Run → Firebase Hosting (rewrites)
- **GitReverse**: Next.js → Cloud Run (standalone) or Firebase Hosting (static export)

---

## Option 1: Full Cloud Run Deployment (Recommended)

### Prerequisites

```bash
# Install gcloud CLI
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init

# Install Firebase CLI
npm install -g firebase-tools
firebase login
```

### Deploy Gitingest to Cloud Run

```bash
cd gitingest

# 1. Build Docker image
docker build -t gcr.io/YOUR_PROJECT_ID/gitingest:latest .

# 2. Configure Docker to use gcloud
gcloud auth configure-docker

# 3. Push image
docker push gcr.io/YOUR_PROJECT_ID/gitingest:latest

# 4. Deploy to Cloud Run
gcloud run deploy gitingest \
  --image gcr.io/YOUR_PROJECT_ID/gitingest:latest \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="GITHUB_TOKEN=${GITHUB_TOKEN},LOG_FORMAT=json,LOG_LEVEL=INFO,S3_ENABLED=false"

# Get the URL
gcloud run services describe gitingest --region=us-central1 --format='value(status.url)'
```

### Deploy GitReverse to Cloud Run

```bash
cd gitreverse

# 1. Build Docker image
docker build -t gcr.io/YOUR_PROJECT_ID/gitreverse:latest .

# 2. Push image
docker push gcr.io/YOUR_PROJECT_ID/gitreverse:latest

# 3. Deploy to Cloud Run
gcloud run deploy gitreverse \
  --image gcr.io/YOUR_PROJECT_ID/gitreverse:latest \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="OPENROUTER_API_KEY=${OPENROUTER_API_KEY},GITHUB_TOKEN=${GITHUB_TOKEN},NEXT_PUBLIC_APP_URL=https://YOUR_PROJECT_ID.web.app"

# Get the URL
gcloud run services describe gitreverse --region=us-central1 --format='value(status.url)'
```

---

## Option 2: Firebase Hosting with Cloud Run Backend

### Configure Firebase Project

```bash
cd /home/nazifarabbu/OneDrive/supremeai

# Initialize Firebase
firebase init hosting

# When prompted:
# - Select "Use an existing project" or create new
# - Public directory: "gitreverse/out" (for static export)
# - Single-page app: Yes
# - Overwrite index.html: No
```

### Update firebase.json for Cloud Run Rewrites

```json
{
  "hosting": {
    "public": "gitreverse/out",
    "ignore": ["firebase.json", "**/.*", "**/node_modules/**"],
    "rewrites": [
      {
        "source": "/api/ingest",
        "run": {
          "serviceId": "gitingest",
          "region": "us-central1"
        }
      },
      {
        "source": "/api/**",
        "run": {
          "serviceId": "gitreverse-api",
          "region": "us-central1"
        }
      },
      {
        "source": "**",
        "destination": "/index.html"
      }
    ]
  }
}
```

### Deploy GitReverse as Static Export (Limited)

**Note**: This will disable API routes. Only use if you don't need server-side features.

```bash
cd gitreverse

# Update next.config.js to enable static export
# Add: output: 'export',

# Build static files
npm run build

# Deploy to Firebase Hosting
firebase deploy --only hosting
```

---

## Option 3: Deploy GitReverse to Firebase Hosting with Functions

### Set up Firebase Functions for API Routes

```bash
cd /home/nazifarabbu/OneDrive/supremeai

# Initialize functions
firebase init functions

# When prompted:
# - Language: TypeScript
# - ESLint: No
# - Install dependencies: Yes
```

### Create Function for GitReverse API

Edit `functions/src/index.ts`:

```typescript
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

export const api = functions.https.onRequest(async (req, res) => {
  // Proxy to Cloud Run services
  const service = req.path.includes("ingest") ? "gitingest" : "gitreverse";
  const url = `https://${service}-uc.a.run.app${req.path}`;
  
  // Forward request
  const response = await fetch(url, {
    method: req.method,
    headers: req.headers as any,
    body: req.method !== "GET" ? JSON.stringify(req.body) : undefined,
  });
  
  const data = await response.json();
  res.status(response.status).json(data);
});
```

### Deploy Functions

```bash
cd functions
npm run build
cd ..
firebase deploy --only functions
```

---

## Environment Variables Setup

### Create .env files for each service

**Gitingest** (Cloud Run):
```bash
gcloud run services update gitingest \
  --region=us-central1 \
  --set-env-vars="GITHUB_TOKEN=ghp_...,LOG_FORMAT=json,LOG_LEVEL=INFO"
```

**GitReverse** (Cloud Run):
```bash
gcloud run services update gitreverse \
  --region=us-central1 \
  --set-env-vars="OPENROUTER_API_KEY=sk-or-...,GITHUB_TOKEN=ghp_..."
```

---

## Quick Deploy Script

I've created deploy scripts for easy deployment:

### For Gitingest:
```bash
cd gitingest
chmod +x deploy_cloudrun.sh
./deploy_cloudrun.sh
```

### For GitReverse:
```bash
cd gitreverse
chmod +x deploy_cloudrun.sh
./deploy_cloudrun.sh
```

---

## Post-Deployment Verification

### Test Gitingest:
```bash
# Get service URL
GITINGEST_URL=$(gcloud run services describe gitingest --region=us-central1 --format='value(status.url)')

# Test health endpoint
curl ${GITINGEST_URL}/health

# Test ingest endpoint
curl -X POST ${GITINGEST_URL}/api/ingest \
  -H "Content-Type: application/json" \
  -d '{"url": "https://github.com/fastapi/fastapi"}'
```

### Test GitReverse:
```bash
# Get service URL
GITREVERSE_URL=$(gcloud run services describe gitreverse --region=us-central1 --format='value(status.url)')

# Test the home page
curl ${GITREVERSE_URL}/

# Test reverse API
curl -X POST ${GITREVERSE_URL}/api/reverse \
  -H "Content-Type: application/json" \
  -d '{"url": "https://github.com/fastapi/fastapi"}'
```

---

## Custom Domains (Optional)

### Firebase Hosting:
```bash
firebase hosting:channel:deploy production
firebase open hosting:channel
# Then add custom domain in Firebase Console
```

### Cloud Run:
```bash
# Map custom domain
gcloud run domain-mappings create \
  --service gitingest \
  --domain api.yourdomain.com \
  --region us-central1
```

---

## Monitoring & Logs

### View Cloud Run Logs:
```bash
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=gitingest" --limit 50
```

### View Firebase Logs:
```bash
firebase functions:log
```

---

## Cost Estimation

### Cloud Run Pricing (approximate):
- **Gitingest**: ~$5-10/month (if moderate traffic)
- **GitReverse**: ~$5-15/month (includes API calls)

### Firebase Hosting:
- Free tier: 10GB storage, 360 MB/day transfer
- Blaze plan: Pay as you go

---

## Troubleshooting

### Common Issues:

1. **Docker build fails**: Ensure Docker is running
2. **Permission denied**: Run `gcloud auth login` and `gcloud config set project YOUR_PROJECT_ID`
3. **Service unavailable**: Check logs with `gcloud run services describe`
4. **Environment variables not set**: Verify with `gcloud run services describe --format='value(spec.template.spec.template.spec.containers[0].env[0].value)'`

---

## Security Considerations

1. **Use Secret Manager** for API keys:
```bash
gcloud secrets create openrouter-api-key --data-file=- <<< "$OPENROUTER_API_KEY"
```

2. **Enable IAM** for service-to-service auth:
```bash
gcloud run services add-iam-policy-binding gitingest \
  --region=us-central1 \
  --member=serviceAccount:YOUR_SA@YOUR_PROJECT.iam.gserviceaccount.com \
  --role=roles/run.invoker
```

---

## Next Steps

1. Choose deployment option (Cloud Run recommended)
2. Set up gcloud and Firebase CLI
3. Run deployment scripts
4. Configure custom domains (optional)
5. Monitor logs and set up alerts
