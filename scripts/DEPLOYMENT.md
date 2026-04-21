# Deployment Guide - SupremeAI Backend

> **Single Source of Truth:** Production deployments are fully automated via GitHub Actions. Manual deployment scripts are provided only for emergencies/recovery.

---

## Primary: GitHub Actions CI/CD Pipeline (Recommended)

The **official** deployment path is the GitHub Actions workflow: `.github/workflows/supreme_pipeline.yml`

### How it works

1. **Push to `main`** triggers the pipeline automatically
2. **Detect Changes** - Determines which components changed (backend, frontend, plugin, vscode)
3. **Java Build & Test** - Runs `./gradlew clean build -x test`, produces `app.jar`
4. **Upload Artifact** - JAR stored as GitHub Actions artifact
5. **Deploy Backend** - Downloads JAR, builds Docker image via Cloud Build, deploys to Cloud Run
6. **Deploy Frontend** - Flutter web build → Firebase Hosting
7. **Health Checks** - Verifies both services respond

### Artifact Registry

Image is pushed to:

```
us-central1-docker.pkg.dev/supremeai-a/supremeai-repo/supremeai
```

Cloud Run service: `supremeai` in `us-central1`

### Configuration

All settings are defined in the workflow file:

- Memory: 4Gi
- CPU: 2
- Concurrency: 80
- Timeout: 300s
- Env vars: `SPRING_PROFILES_ACTIVE=cloud`, `FIREBASE_PROJECT_ID=supremeai-a`, `spring.threads.virtual.enabled=true`
- Secrets: `firebase-service-account-json` from Google Secret Manager

---

## Secondary: Local Manual Deploy (Emergency Only)

If you need to deploy manually from local machine, use:

```powershell
pwsh scripts/deploy-cloudrun-auth-fix.ps1 -ProjectId supremeai-a
```

**What it does:**

- Builds Docker image (uses local Docker if available, falls back to Cloud Build)
- Pushes to Artifact Registry
- Deploys to Cloud Run with production-grade settings
- Runs health checks

**Prerequisites:**

- `gcloud` installed and authenticated (`gcloud auth login`)
- Docker installed (optional - falls back to Cloud Build if missing)
- Git repository with clean working tree

**Parameters:**

| Parameter | Default | Description |
|-----------|---------|-------------|
| `-ProjectId` | `supremeai-a` | GCP project ID |
| `-ServiceName` | `supremeai` | Cloud Run service name |
| `-Region` | `us-central1` | Deployment region |
| `-FirebaseServiceAccountFile` | (none) | Path to service account JSON for secret update |
| `-SkipBuild` | false | Skip Docker build/push, deploy existing image |

---

## Deprecated: Old Scripts (Do Not Use)

The following scripts are **obsolete** and may reference outdated registries (`gcr.io`), incorrect resources, or insecure configurations. They have been moved to `scripts/deprecated-deploys/` for historical reference only:

- `deploy-cloud-run.sh` / `deploy-cloud-run.ps1` (old naming, gcr.io path)
- `deploy-to-cloudrun.ps1` (incomplete)
- `deploy-to-gcp.ps1` (outdated env vars)
- `deploy-supremeai-a.sh` (gcr.io path)
- `scripts/_archive` (old deployment helper)

Do not use these. They will be removed in a future cleanup.

---

## Cloud Build Config

`cloudbuild.yaml` is still used by the GitHub Actions pipeline (via `gcloud builds submit`). It:

1. Receives source tarball from GitHub Actions (includes downloaded `app.jar` at repo root)
2. Runs **only** Docker build (Gradle build already completed in GitHub Actions)
3. Pushes to Artifact Registry

> **Note:** The Dockerfile expects `app.jar` in the repo root. The GitHub Actions workflow copies it from `build/libs/app.jar` → `./app.jar` before triggering Cloud Build.

---

## Important: Registry Migration (Completed)

**Old:** `gcr.io/supremeai-a/gcr.io/supremeai` (Google Container Registry - mixed up path)  
**New:** `us-central1-docker.pkg.dev/supremeai-a/supremeai-repo/supremeai` (Artifact Registry)

All deployment paths have been updated. Old scripts that reference `gcr.io` should be considered deprecated.

---

## Troubleshooting

### Cloud Build fails: "COPY failed: file not found"

The JAR artifact wasn't included in the build context. Verify:

- `.gcloudignore` whitelists `build/libs/app.jar` (already configured)
- GitHub Actions `Java_Build_And_Test` job succeeded and uploaded artifact
- `Deploy_Backend` job downloaded artifact to `build/libs/` and copied to `./app.jar`

### Cloud Run deployment fails: permission errors

Ensure the service account `github-action-...@supremeai-a.iam.gserviceaccount.com` has:

- `roles/run.admin`
- `roles/artifactregistry.writer`
- `roles/cloudbuild.builds.editor`

### Docker push fails: "Repository not found"

Use the correct Artifact Registry path: `us-central1-docker.pkg.dev/supremeai-a/supremeai-repo/supremeai`. The old `gcr.io/supremeai-a/gcr.io/supremeai` path is invalid.

---

## Production Endpoints

| Service | URL |
|---------|-----|
| **Backend API** | `https://supremeai-lhlwyikwlq-uc.a.run.app` |
| **Admin Dashboard** | `https://supremeai-a.web.app/admin.html` |
| **Health Check** | `https://supremeai-lhlwyikwlq-uc.a.run.app/actuator/health` |

> **Note:** The Cloud Run URL hash (`lhlwyikwlq`) is stable for the lifetime of the service. If the service is deleted and recreated, the URL will change. Update references accordingly.

---

## Quick Reference

| Task | Command |
|------|---------|
| Trigger production deploy | `git push origin main` (watch GitHub Actions) |
| Manual deploy (emergency) | `pwsh scripts/deploy-cloudrun-auth-fix.ps1` |
| View Cloud Run logs | `gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=supremeai" --limit 50` |
| Roll back to previous revision | `gcloud run services update-traffic supremeai --to-revisions=LATEST_REVISION=100` |
| Check Artifact Registry images | `gcloud artifacts docker images list us-central1-docker.pkg.dev/supremeai-a/supremeai-repo/supremeai` |

---

*Last updated: 2026-04-21*
