# Render Deployment Failure Logs

วันที่: 2026-08-08
Repo: paykaribazaronline/supremeai

## 📊 CI Status
- সর্বশেষ GitHub Actions run: `31241267899`
- Backend (Test): ✅ success
- Build & Push Backend Image: ✅ success
- Deploy Combined Backend (Render): 🔄 in_progress → update_failed

## 🔍 Failed Deploys

### User Backend
- Service ID: `srv-d9d3n58js32c738n79k0`
- Deploy ID: `dep-d9rbpa2fngtc73d13n1g`
- Status: `update_failed`
- Trigger: `api`
- Created: `2026-08-08T05:20:40.473865Z`
- Finished: `2026-08-08T05:21:54.421288Z`
- Image: `ghcr.io/paykaribazaronline/supremeai/supremeai-backend:latest`
- Image SHA: `sha256:4b4103c505f7312b13e2ff2c74cb5702a5b357262500f3a95114a3a4870f7331`

### Admin Backend
- Service ID: `srv-d9fg48bh523c73f63bb0`
- Deploy ID: `dep-d9rbpaajobas73d2br70`
- Status: `update_failed`
- Trigger: `deploy_hook`
- Created: `2026-08-08T05:20:41.204763Z`
- Finished: `2026-08-08T05:21:58.016435Z`
- Image: `ghcr.io/paykaribazaronline/supremeai/supremeai-backend:latest`
- Image SHA: `sha256:4b4103c505f7312b13e2ff2c74cb5702a5b357262500f3a95114a3a4870f7331`

## 🔎 Preliminary Analysis

### What succeeded
1. GitHub Actions CI সব job success — Backend Test, Build Image, Pre-Merge Gate সব গ্রিন।
2. Image `ghcr.io/paykaribazaronline/supremeai/supremeai-backend:latest` successfully built and pushed।
3. Render API ডিপ্লয় trigger success (201/202) — service আপডেট শুরু হয়েছে।

### What failed
উভয় service-এ `update_failed` — meaning:
- Image pull হয়েছে ✅
- Container create হয়েছে ✅
- কিন্তু container startup/crash导致 with exit code != 0 ❌

### Possible causes ( prioritized )
1. **Application startup crash** —最快 and most likely
   - Port binding issue: `PORT=10000` Render passes, app binds to `settings.port` (default 8080) if `os.getenv("PORT")` missing
   - Critical env var missing (SUPREMEAI_ADMIN_PASSWORD_HASH, JWT_SECRET, etc.)
   - Database connection failure (Supabase/Postgres)

2. **Health check failure** — Render-এ `/api/v1/health` expected, but app crashed before registering routes

3. **Image layer/permission issue** — Non-root user `appuser` might not have permission to write to `/app` or bind to port

## 📝 Evidence

### Port Configuration
- Dockerfile: `EXPOSE 8080`, `CMD ["sh", "-c", "exec python main.py"]`
- main.py: `port = int(os.getenv("PORT", str(settings.port)))` — respects Render's PORT env var
- Render service: `openPorts: null` — uses default port from image

### Env Configuration
- Render service env: `image` type — all env vars must be set in Render dashboard or image
- Dockerfile: `RUN rm -rf /app/.git /app/.env* /app/.env /app/secrets.sh || true` — .env removed in image
- CI workflow sets env vars during test, but Render deploys use pre-built image with secrets from Render dashboard

### CI vs Render Difference
CI成功 but Render failed:
- CI uses test env vars (mock values) set in workflow
- Render uses production secrets from Render dashboard / Infisical vault
- If any critical secret is missing in Render dashboard, app crashes on boot

## 🛠️ Recommended Next Steps

1. **Check Render Logs** — Render dashboard > Service > Logs tab for actual error message
2. **Verify Render Environment Variables** — Ensure all critical secrets are configured:
   - `SUPREMEAI_ADMIN_PASSWORD_HASH`
   - `SUPREMEAI_JWT_SECRET`
   - `SUPREMEAI_ENCRYPTION_KEY`
   - `SUPABASE_DATABASE_URL_POOLER`
   - `REDIS_URL`
   - All LLM API keys
3. **Check Render SSH** — `srv-d9d3n58js32c738n79k0@ssh.singapore.render.com` for container inspection
4. **Test locally with production env** — `ENV=production python main.py` to reproduce crash

## 🔗 Related Files
- `backend/Dockerfile` — Multi-stage build, non-root user
- `backend/main.py` — Entry point, port binding logic
- `backend/core/config.py` — Settings, secret loading
- `.github/workflows/supreme-core-ci.yml` — CI workflow