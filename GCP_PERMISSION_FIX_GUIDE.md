# 🔧 GCP Cloud Run Deployment - Permission Fix Guide

**Error:** `denied: Permission 'artifactregistry.repositories.uploadArtifacts' denied`  
**Date:** March 31, 2026  
**Status:** Fixing GCP IAM permissions for Docker push

---

## 🎯 The Problem

Your GitHub Actions workflow is failing when trying to push Docker images to GCP's Container Registry (Artifact Registry).

```

docker push "gcr.io/supremeai-a/supremeai:324db18"
Error: denied: Permission 'artifactregistry.repositories.uploadArtifacts' denied

```

### Root Cause

The service account running GitHub Actions doesn't have permission to push to Artifact Registry.

---

## ✅ Solution: Fix GCP IAM Permissions

### Step 1: Verify GCP Project

1. Go to https://console.cloud.google.com
2. Select project: **supremeai-a**
3. Note the project ID for later

### Step 2: Create/Find Service Account

1. Go to: **IAM & Admin** → **Service Accounts**

2. Look for: `github-actions` or `cloud-build` service account
3. If it doesn't exist:
   - Click **Create Service Account**
   - Name: `github-actions`
   - Description: "GitHub Actions deployment service account"
   - Click **Create and Continue**

### Step 3: Grant Required IAM Roles

Select your service account, click **Manage roles** (or **Grant Access** tab):

**Add these roles:**

| Role | Permission | Why |

|------|-----------|-----|
| **Artifact Registry Writer** | `artifactregistry.repositories.uploadArtifacts` | CRITICAL - to push Docker images |

| **Cloud Run Admin** | Deploy to Cloud Run | To deploy services |

| **Service Account User** | Use the service account | Required for Cloud Run |

**Or use Editor role (for testing):**

- Easier: Just assign **Editor** role for now

- Later: Restrict to specific roles above

### Step 4: Create & Download Service Account Key

1. In **Service Accounts**, click your service account
2. Go to **Keys** tab

3. Click **Add Key** → **Create new key**

4. Select **JSON** format

5. Click **Create** (downloads JSON automatically)

⚠️ **Keep this file secure!** Don't commit to GitHub.

### Step 5: Add to GitHub Secrets

1. Go to GitHub repo → **Settings** → **Secrets and variables** → **Actions**

2. Click **New repository secret**
3. Name: `GCP_SA_KEY`

4. Value: Paste the entire JSON content from the downloaded file
5. Click **Add secret**

### Step 6: Update GitHub Workflow (if needed)

Check `.github/workflows/deploy-cloudrun.yml` to ensure it uses the secret:

```yaml

- name: Authenticate to Google Cloud
  uses: google-github-actions/auth@v2
  with:
    credentials_json: ${{ secrets.GCP_SA_KEY }}

```

---

## 🚀 Verify the Fix

After completing steps above:

1. **Commit and push** your changes:
   ```bash
   git add -A
   git commit -m "fix: Sync GitHub changes and prepare for GCP deployment"
   git push origin main
   ```

2. **Check GitHub Actions**:
   - Go to repo → **Actions** tab
   - Click latest workflow run
   - Watch deployment logs
   - Should see successful Docker push: ✅

3. **Verify Cloud Run deployment**:
   ```bash
   gcloud run list --region us-central1
   ```

   Should show: `supremeai` service with ✅ ACTIVE status

4. **Test the deployed API**:
   ```bash
   curl -s https://supremeai-[hash].run.app/api/health
   ```

   Should return JSON with `"status":"UP"`

---

## 🆘 Troubleshooting

### Still Getting Permission Denied?

1. **Wait 30 seconds** for IAM changes to propagate

2. **Check service account has correct roles:**
   ```bash
   gcloud projects get-iam-policy supremeai-a \
     --flatten="bindings[].members" \
     --filter "bindings.members:serviceAccount:github-actions@*"
   ```

   Should show: `roles/artifactregistry.writer`

3. **Verify secret is set correctly:**
   - GitHub → Settings → Secrets → GCP_SA_KEY
   - Click "**Update**" and paste key again (if old)

### Docker Login Failing?

```bash

# Authenticate locally first (for debugging)

gcloud auth configure-docker gcr.io
docker push gcr.io/supremeai-a/supremeai:latest

```

### Artifact Registry Not Found?

Enable API:

```bash
gcloud services enable artifactregistry.googleapis.com
gcloud services enable cloudrun.googleapis.com
gcloud services enable cloudbuild.googleapis.com

```

---

## 📋 Checklist

- [ ] Service account created in GCP

- [ ] IAM roles granted (Artifact Registry Writer + Cloud Run Admin)

- [ ] Service account key generated and downloaded

- [ ] `GCP_SA_KEY` secret added to GitHub

- [ ] Latest code pushed to main branch

- [ ] GitHub Actions workflow runs successfully

- [ ] Docker image pushed to `gcr.io/supremeai-a/supremeai`

- [ ] Cloud Run deployment shows ACTIVE

- [ ] Health endpoint `/api/health` responds with status UP

---

## ⏱️ Time to Fix

- Estimated total time: **10-15 minutes**

- GitHub Actions re-run: **5-10 minutes**

- GCP Cloud Run deploy: **2-3 minutes**

---

## 📊 After Fix - Full CI/CD Flow

```

Your Code Push
        ↓
GitHub Actions Workflow Triggers
        ↓
✅ Java Build & Tests (62 passing)
        ↓
✅ Docker Image Built & Pushed to GCR
        ↓
✅ Cloud Run Service Deployed
        ↓
✅ Self-Healing Monitor Activated (every 5 min)
        ↓
💚 System Health Monitored in Real-Time

```

---

## 🎯 Next Steps After Fix

1. **Get deployment URL:**
   ```bash
   gcloud run services describe supremeai --region us-central1 --format 'value(status.url)'
   ```

2. **Set GitHub secret for self-healing:**
   - Settings → Secrets → **DEPLOYMENT_URL**
   - Value: `https://supremeai-[hash].run.app`

3. **Self-healing will automatically:**
   - Check health every 5 minutes
   - Monitor metrics (MTTR, MTTD, availability)
   - Trigger auto-repairs if needed

---

## 📞 Still Having Issues?

Common solutions:

- **Permission denied:** Wait 30 sec for IAM propagation, or regenerate service account key

- **Build fails:** Check Java compilation errors in GitHub Actions logs

- **Cloud Run deploy fails:** Check logs with `gcloud run deployments describe supremeai`

- **Health endpoint unreachable:** Verify port 8080 is set in Dockerfile/application.properties

Run this to check deployment status:

```bash
gcloud run describe supremeai --region us-central1

```
