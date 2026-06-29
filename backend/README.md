# SupremeAI Backend
FastAPI backend for SupremeAI.

## Cloud Build ignore
This backend uses `backend/.gcloudignore` to keep `gcloud builds submit` small and focused on real source code.
Do not include local artifacts in the build context:
- virtual envs: `.venv/`, `venv/`
- caches: `__pycache__/`, `.pytest_cache/`, `.mypy_cache/`, `.ruff_cache/`
- local DB files: `*.db`, `*.sqlite3`, `*.sqlite`
- secrets: `service-account.json`, `.env*`
- docs and non-runtime artifacts: `*.md`, `LICENSE`, `tests/`, `logs/`

This is important because the backend deployment should connect to Supabase via environment variables, not by uploading local database files.

## Supabase configuration
The backend connects to Supabase through environment variables.
Required variables for Supabase client usage include:
- `SUPABASE_URL`
- `SUPABASE_KEY`

Optional values used in this repo include:
- `SUPABASE_PUBLISHABLE_KEY`
- `SUPABASE_SECRET_KEY`
- `SUPABASE_JWKS_URL`
- `SUPABASE_DATABASE_URL`
- `SUPABASE_DATABASE_URL_POOLER`
- `DATABASE_URL`

These values should be configured in your deployment environment or CI secrets. Do not commit them to source control.

## Deploying with Supabase
Supabase in this repo is a hosted database/storage service, not the backend host.
The Python backend is deployed to Google Cloud Run and connects to Supabase at runtime.

Basic deploy flow:
1. Create a Supabase project and get `SUPABASE_URL` and `SUPABASE_KEY` from the dashboard.
2. Configure those values in Cloud Run or your CI/CD environment.
3. Build and deploy the backend from `backend/`:
   ```bash
   cd backend
   gcloud builds submit --tag gcr.io/supremeai-a/supremeai-api .
   gcloud run deploy supremeai-api \
     --image gcr.io/supremeai-a/supremeai-api \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated \
     --set-env-vars="SUPABASE_URL=<your-url>,SUPABASE_KEY=<your-key>,ENV=production,GCP_PROJECT_ID=supremeai-a"
   ```
4. If you need Supabase database schema setup, use the Supabase dashboard SQL editor or migration scripts; do not upload local SQLite/DB files as part of the build.

<!-- Trigger CI/CD backend test run -->
 
