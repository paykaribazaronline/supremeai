# 🔑 GitHub Secrets Setup for Self-Healing CI/CD

**Date:** March 31, 2026  
**Purpose:** Configure required secrets for self-healing and Cloud Run deployment workflows  
**Status:** 🟡 ACTION REQUIRED - Add 2 secrets to GitHub  

---


## Overview

The self-healing CI/CD pipeline requires 2 GitHub secrets to be fully operational:

| Secret Name | Example Value | Where to Get | Purpose |
|------------|---------------|--------------|---------|
| `API_BASE_URL` | `https://supremeai-xxxxx.run.app` | Cloud Run deployment URL | Health checks, auto-repair |
| `ADMIN_TOKEN` | Your JWT admin token | Login to API | Authenticate self-healing endpoints |

**Without these:** Health checks silently skip (appear passing but don't actually run)

---


## Adding Secrets to GitHub


### Step 1: Go to GitHub Settings

1. Open your repository: https://github.com/your-username/supremeai
2. Click **Settings** (top right)

3. Click **Secrets and variables** (left sidebar)

4. Click **Repository secrets**


### Step 2: Add API_BASE_URL

1. Click **New repository secret**
2. **Name:** `API_BASE_URL`

3. **Value:** Your Cloud Run service URL
   ```
   https://supremeai-xxxxx-uc.a.run.app
   ```
   
   **How to find your Cloud Run URL:**
   ```bash
   gcloud run services describe supremeai --region=us-central1 --format='value(status.url)'
   ```

4. Click **Add secret**


### Step 3: Add ADMIN_TOKEN

1. Click **New repository secret**
2. **Name:** `ADMIN_TOKEN`

3. **Value:** Your JWT admin token from login
   
   **How to get your admin token:**
   
   Option A - Using cURL:
   ```bash
   curl -X POST https://supremeai-xxxxx-uc.a.run.app/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "email": "admin@supremeai.com",
       "password": "your-password"
     }'
   
   # Response:
   # {"accessToken": "eyJhbGc...", "refreshToken": "..."}
   # Copy the accessToken value
   ```
   
   Option B - Using local API (if running locally):
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "email": "admin@supremeai.com",
       "password": "your-password"
     }'
   ```

4. Click **Add secret**


### Step 4: Verify Secrets are Set


```bash

# List all secrets (shows names only, not values)

gh secret list


# Expected output:

# API_BASE_URL     Updated Mar 31, 2026

# ADMIN_TOKEN      Updated Mar 31, 2026

```

---


## Using Secrets in Workflows

Secrets are automatically available in GitHub Actions via `secrets.SECRET_NAME`:


```yaml
env:
  API_BASE_URL: ${{ secrets.API_BASE_URL }}
  ADMIN_TOKEN: ${{ secrets.ADMIN_TOKEN }}

jobs:
  health-check:
    steps:
      - name: Check health
        run: |
          curl -X POST "${{ secrets.API_BASE_URL }}/api/v1/self-healing/status" \
            -H "Authorization: Bearer ${{ secrets.ADMIN_TOKEN }}"

```

---


## Workflows That Use These Secrets


### Self-Healing CI/CD (`.github/workflows/self-healing-cicd.yml`)

- **Runs:** Every hour (24 times/day)

- **Uses:** `API_BASE_URL` + `ADMIN_TOKEN`

- **Does:** Health checks, failure prediction, auto-repair

- **Status:** ⏳ BLOCKED without secrets (exits silently)


### Cloud Run Deployment (`.github/workflows/deploy-cloudrun.yml`)

- **Runs:** On push to main branch

- **Uses:** `GCP_SA_KEY`, `GCP_PROJECT_ID` (already set)

- **Does:** Build, push to GCR, deploy to Cloud Run

- **Status:** ✅ Works independently

---


## Workflow Execution Flow


```
GitHub Push → java-ci.yml (build & test)
           ↓
        (if tests pass)
           ↓
      deploy-cloudrun.yml (deploy to Cloud Run)
           ↓
     (gets API_BASE_URL from deployment)
           ↓
   self-healing-cicd.yml (health monitoring scheduled)
           ↓
     (uses API_BASE_URL + ADMIN_TOKEN for checks)

```

---


## Testing Secrets

After adding secrets, trigger the self-healing workflow manually:


```bash
gh workflow run self-healing-cicd.yml --ref main

```

Then check the logs:


```bash

# Watch the workflow run

gh run watch


# View detailed logs

gh run view <run-id> --log

```

**Expected output if secrets are correct:**

```

✅ System Health Pulse
   Health Status: {"status":"healthy",...}
   Metrics: {"mttr": ..., "mttd": ..., ...}

✅ ML Failure Prediction
   Predictions Retrieved: [...]

✅ Compile & Verify
   Artifact built successfully

✅ System is healthy

```

**Expected output if secrets are missing:**

```

⚠️ API_BASE_URL not configured - skipping health check

⚠️ ADMIN_TOKEN not configured - cannot trigger auto-repair

# (workflow succeeds but actually does nothing)

```

---


## Secret Security Best Practices

1. ✅ **Secrets are encrypted** at rest in GitHub

2. ✅ **Never logged** - won't appear in workflow logs

3. ✅ **Only readable in Actions** - not accessible via API

4. ✅ **Rotatable** - update anytime via Settings

5. ✅ **Per-environment** - use branch protection for production


### Token Rotation

If your admin token expires:
1. Log in to get new token
2. Update `ADMIN_TOKEN` secret
3. Next workflow run uses new token

---


## Troubleshooting


### Secret Appears Empty

- **Problem:** `API_BASE_URL` value is blank

- **Fix:** Re-copy the Cloud Run URL and update secret


### "PERMISSION_DENIED" in workflow

- **Problem:** `ADMIN_TOKEN` is invalid or expired

- **Fix:** Get new token via login and update secret


### Health check still skips

- **Problem:** Secrets added but workflow hasn't run yet

- **Fix:** Manually trigger: `gh workflow run self-healing-cicd.yml --ref main`

- Wait 2-3 minutes for GitHub to sync secret changes

---


## Reference


- **GitHub Docs:** https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions

- **Self-Healing Workflow:** `.github/workflows/self-healing-cicd.yml`

- **Deploy Workflow:** `.github/workflows/deploy-cloudrun.yml`

---

**Next Steps:**

1. ✅ Add `API_BASE_URL` secret
2. ✅ Add `ADMIN_TOKEN` secret
3. ✅ Trigger self-healing workflow manually
4. ✅ Verify health checks are running (check workflow logs)
