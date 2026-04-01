# 🔐 GCP IAM Permissions Fix - GitHub Actions CI/CD

**Date:** March 31, 2026  
**Issue:** Cloud Run deployment failing due to insufficient service account permissions  
**Status:** 🔴 ACTIONABLE - Fix required in GCP Console

---

## Problem Summary

The GitHub Actions service account (`github-action-1192200658@supremeai-a.iam.gserviceaccount.com`) is missing two critical IAM roles:

| Role | Purpose | Current | Needed |
|------|---------|---------|--------|
| `roles/run.admin` | Deploy to Cloud Run | ❌ run.viewer | ✅ admin |
| `roles/secretmanager.admin` | Manage Firebase secrets | ❌ Missing | ✅ admin |
| `roles/storage.admin` | Upload container images | ✅ Already granted | ✅ ✓ |
| `roles/iam.serviceAccountUser` | Impersonate service accounts | ✅ Already granted | ✅ ✓ |

**Error Messages:**
```
PERMISSION_DENIED: roles/secretmanager.admin
PERMISSION_DENIED: roles/run.admin (currently has run.viewer)
```

---

## Solution: Add Missing Roles

### Option 1: GCP Console (Recommended for Users)

**Step 1:** Open [GCP IAM Console](https://console.cloud.google.com/iam-admin/iam)

**Step 2:** Find the service account
- Click "Grant Access" 
- Search for: `github-action-1192200658@supremeai-a.iam.gserviceaccount.com`

**Step 3:** Add roles
- Click "Add another role"
- Search for: `Cloud Run Admin` → Select `roles/run.admin`
- Click "Add another role"
- Search for: `Secret Manager Admin` → Select `roles/secretmanager.admin`

**Step 4:** Save
- Click "Save"

### Option 2: Command Line (Fast)

```bash
# Set your GCP project
export GCP_PROJECT="supremeai-a"
export SA_EMAIL="github-action-1192200658@supremeai-a.iam.gserviceaccount.com"

# Add Cloud Run Admin role
gcloud projects add-iam-policy-binding $GCP_PROJECT \
  --member="serviceAccount:$SA_EMAIL" \
  --role="roles/run.admin"

# Add Secret Manager Admin role
gcloud projects add-iam-policy-binding $GCP_PROJECT \
  --member="serviceAccount:$SA_EMAIL" \
  --role="roles/secretmanager.admin"

# Verify both roles are granted
gcloud projects get-iam-policy $GCP_PROJECT \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:$SA_EMAIL"
```

### Option 3: Terraform/IaC

```hcl
resource "google_project_iam_member" "gh_run_admin" {
  project = "supremeai-a"
  role    = "roles/run.admin"
  member  = "serviceAccount:github-action-1192200658@supremeai-a.iam.gserviceaccount.com"
}

resource "google_project_iam_member" "gh_secret_admin" {
  project = "supremeai-a"
  role    = "roles/secretmanager.admin"
  member  = "serviceAccount:github-action-1192200658@supremeai-a.iam.gserviceaccount.com"
}
```

---

## Verification

After adding roles, verify with:

```bash
# Check that service account has both roles
gcloud projects get-iam-policy supremeai-a \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:github-action-1192200658@supremeai-a.iam.gserviceaccount.com"

# Expected output:
# ROLE                            MEMBERS
# roles/iam.serviceAccountUser    serviceAccount:github-action-1192200658@supremeai-a.iam.gserviceaccount.com
# roles/run.admin                 serviceAccount:github-action-1192200658@supremeai-a.iam.gserviceaccount.com
# roles/secretmanager.admin       serviceAccount:github-action-1192200658@supremeai-a.iam.gserviceaccount.com
# roles/storage.admin             serviceAccount:github-action-1192200658@supremeai-a.iam.gserviceaccount.com
```

---

## What Each Role Does

### `roles/run.admin`
- Deploy services to Cloud Run
- Update Cloud Run services
- View and manage Cloud Run resources

**Replaces:** `roles/run.viewer` (read-only, insufficient for deployments)

### `roles/secretmanager.admin`
- Create/read/update/delete secrets in Google Secret Manager
- Manage secret versions
- Grant access to secrets

**Why needed:** Firebase service account stored in Secret Manager for Cloud Run to access

### `roles/storage.admin` ✅
- Upload/download container images to GCS
- Manage build artifacts

### `roles/iam.serviceAccountUser` ✅
- Impersonate this service account
- Allow Cloud Run to use this account's permissions

---

## Deployment Workflow

Once roles are added:

1. **Push to main branch** or manually trigger workflow
2. **GitHub Actions runs:**
   - ✅ Build Docker image
   - ✅ Push to Container Registry
   - ✅ Sync Firebase secrets to Secret Manager
   - ✅ Deploy to Cloud Run
3. **Application is live** at `https://supremeai-xxxxx.run.app`

---

## Troubleshooting

### Issue: "PERMISSION_DENIED: roles/secretmanager.admin"
**Fix:** Run the command above to add `roles/secretmanager.admin`

### Issue: "PERMISSION_DENIED: roles/run.admin"  
**Fix:** Run the command above to add `roles/run.admin`

### Issue: Changes not taking effect
**Solution:**
1. Wait 2-3 minutes for IAM propagation
2. Trigger workflow manually: GitHub → Actions → Deploy to Google Cloud Run → Run workflow

### Issue: "Service account has roles but still getting denied"
**Solution:** 
1. Verify service account email matches in deploy workflow
2. Check `secrets.GCP_SA_KEY` contains correct JSON
3. Verify JSON has `client_email` matching the service account

---

## Related Files

- `.github/workflows/deploy-cloudrun.yml` - Deployment workflow
- Need to add to GitHub Secrets: `API_BASE_URL` + `ADMIN_TOKEN` for self-healing workflows

---

**Status:** Ready to apply the fix  
**Time to fix:** 5-10 minutes  
**Blocks:** Cloud Run deployment  
**Action:** Add 2 roles via GCP Console or CLI commands above
