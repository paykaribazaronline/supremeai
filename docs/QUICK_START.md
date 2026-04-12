# Quick Start: SupremeAI Healing System

**For:** GitHub App + PaykariBazar repo + Cloud Run deployment  
**Time:** ~2 hours total  

---

## What You're Getting

A **production-grade self-healing system** that:

- ✅ Auto-fixes GitHub Actions failures
- ✅ Prevents infinite loops (circuit breaker)
- ✅ Uses GitHub App (10K req/hour)
- ✅ Deploys to Cloud Run
- ✅ Integrated with PagerDuty/Slack

---

## 1️⃣ Prerequisite: GitHub App (10 min)

### Option 1: App Already Exists

If `supremeai-bot` app already exists:

```bash
# Get App ID: https://github.com/settings/apps/supremeai-bot
# Get Installation ID: https://github.com/organizations/paykaribazaronline/settings/apps/supremeai-bot/installations
```

### Option 2: Create New App

```bash
# Go to: https://github.com/settings/developers
# Click: New GitHub App
# Name: supremeai-bot
# Homepage: https://your-domain.com
# Webhook URL: https://your-domain.com/webhook/github
# Permissions: Contents (RW), Workflows (RW), Issues (RW)
# Generate private key → Save .pem file

# Then install on repo:
# https://github.com/apps/supremeai-bot → Install → Select paykaribazaronline/supremeai
```

**Save these values:**

```
App ID: _______________
Installation ID: _______________
Private Key: _______________
```

---

## 2️⃣ Configure Environment (5 min)

```bash
# Copy template
cp .env.example .env

# Edit .env (filling in above values):
GITHUB_APP_ID=your-app-id
GITHUB_APP_PRIVATE_KEY=-----BEGIN RSA PRIVATE KEY-----\nYour\nKey\n-----END RSA PRIVATE KEY-----
GITHUB_APP_INSTALLATION_ID=your-installation-id
GITHUB_WEBHOOK_SECRET=$(openssl rand -hex 32)

# Escalation alerts
PAGERDUTY_API_KEY=u+your-pagerduty-key
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK
ADMIN_EMAIL=admin@company.com

# Repository
TEST_REPO_OWNER=paykaribazaronline
TEST_REPO_NAME=supremeai
```

**⚠️ Important:** Never commit `.env` to git!

---

## 3️⃣ GitHub Webhook Setup (5 min)

Go to: https://github.com/paykaribazaronline/supremeai/settings/hooks

**Add webhook:**

- URL: `https://your-domain.com/webhook/github`
- Secret: Copy from `GITHUB_WEBHOOK_SECRET`
- Events: ✓ Workflow runs
- Active: ✓ Check

---

## 4️⃣ Local Testing (30 min)

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# In another terminal:
curl http://localhost:8080/api/healing/health
# Should return: 200 OK

# Test status
curl http://localhost:8080/api/healing/status
# Should show: watchdog enabled, rateLimiter OK
```

---

## 5️⃣ Docker Build (10 min)

```bash
# Build image
docker build -t supremeai-healing:latest .

# Test with env file
docker run --env-file .env -p 8080:8080 supremeai-healing:latest

# Verify
curl http://localhost:8080/api/healing/health
```

---

## 6️⃣ Deploy to Cloud Run (10 min)

```bash
# Push to GCR
docker tag supremeai-healing:latest \
  gcr.io/supremeai-565236080752/supremeai-healing:latest
docker push gcr.io/supremeai-565236080752/supremeai-healing:latest

# Deploy to Cloud Run
gcloud run deploy supremeai-healing \
  --image gcr.io/supremeai-565236080752/supremeai-healing:latest \
  --region us-central1 \
  --memory 2Gi \
  --cpu 2 \
  --set-env-vars "GITHUB_APP_ID=$(cat .env | grep GITHUB_APP_ID | cut -d= -f2),TEST_REPO_OWNER=paykaribazaronline,TEST_REPO_NAME=supremeai" \
  --set-secrets "GITHUB_APP_PRIVATE_KEY=healing-github-app-private-key,GITHUB_WEBHOOK_SECRET=healing-webhook-secret" \
  --no-allow-unauthenticated

# Note the URL: https://supremeai-healing-xxxxx.run.app
```

**Important:** Create secrets in GCP Secret Manager first:

```bash
gcloud secrets create healing-github-app-private-key \
  --replication-policy="automatic" \
  --data-file=- << EOF
$(cat .env | grep GITHUB_APP_PRIVATE_KEY | cut -d= -f2-)
EOF

gcloud secrets create healing-webhook-secret \
  --replication-policy="automatic" \
  --data-file=- << EOF
$(cat .env | grep GITHUB_WEBHOOK_SECRET | cut -d= -f2)
EOF
```

---

## 7️⃣ Update GitHub Webhook URL (2 min)

Go to: https://github.com/paykaribazaronline/supremeai/settings/hooks

**Edit webhook:**

- Change URL to: `https://supremeai-healing-xxxxx.run.app/webhook/github`
- Test delivery → Should see 200 OK ✓

---

## 8️⃣ Monitoring Setup (5 min)

### Check Status

```bash
CLOUD_URL=https://supremeai-healing-xxxxx.run.app

# System health
curl $CLOUD_URL/api/healing/status

# Watchdog
curl $CLOUD_URL/api/healing/watchdog

# Rate limit
curl $CLOUD_URL/api/healing/rate-limit
```

### Set Up Alerts (Optional)

```bash
# Cloud Run logs
gcloud run logs read supremeai-healing --limit 20

# Create alert in Cloud Console:
# Monitoring → Uptime checks → Create check
# URL: $CLOUD_URL/api/healing/health
```

---

## 9️⃣ Test End-to-End (15 min)

### Option 1: Manual Failure

```bash
# Go to: https://github.com/paykaribazaronline/supremeai/actions
# Select a workflow → Run workflow → Let it fail
```

### Option 2: Create Test Failure

```bash
# Edit a workflow to add:
- run: exit 1  # Force failure

# Push to trigger workflow
git add . && git commit -m "test" && git push
```

### Monitor Healing

```bash
# Check logs
gcloud run logs read supremeai-healing --follow

# Check status
curl $CLOUD_URL/api/healing/status

# Check Firestore: healing_attempts collection
# Should see new documents appearing
```

---

## 🆘 Emergency Stop

If something breaks:

```bash
# Disable healing
curl -X POST $CLOUD_URL/api/healing/disable

# Check what happened
curl $CLOUD_URL/api/healing/diagnostics

# View logs
gcloud run logs read supremeai-healing --limit 50

# Re-enable after fixing
curl -X POST $CLOUD_URL/api/healing/enable
```

---

## 📚 Full Guides

For more details, see:

1. **[GITHUB_APP_SETUP.md](GITHUB_APP_SETUP.md)** — Detailed GitHub App instructions
2. **[DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)** — 9-phase deployment
3. **[SAFE_HEALING_LOOP_GUIDE.md](SAFE_HEALING_LOOP_GUIDE.md)** — Complete technical guide
4. **[HEALING_QUICK_REFERENCE.md](HEALING_QUICK_REFERENCE.md)** — Developer reference

---

## REST API Endpoints

```bash
# Status monitoring
GET /api/healing/status               # Overview
GET /api/healing/watchdog             # Watchdog health
GET /api/healing/rate-limit           # GitHub API limits
GET /api/healing/diagnostics          # Full diagnostics
GET /api/healing/attempts/{id}        # Attempt history

# Admin control
POST /api/healing/disable             # STOP healing
POST /api/healing/enable              # Start healing
POST /api/healing/retry/{id}          # Manual retry
```

---

## Success Metrics

**First day:**

- ✅ Webhook deliveries working (check GitHub)
- ✅ No auth errors in logs
- ✅ Watchdog shows HEALTHY

**First week:**

- ✅ >70% success rate
- ✅ <5 escalations/day
- ✅ 0 rate limit hits
- ✅ Circuit breaker triggered appropriately

**First month:**

- ✅ >85% success rate
- ✅ CI/CD time reduced 30%
- ✅ Team debugging time reduced 50%

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Webhook not received | Check GitHub webhook delivery logs |
| "GitHub App config error" | Verify .env has all 4 GitHub fields |
| "Rate limit exceeded" | Check /api/healing/rate-limit |
| Healing never triggers | Ensure webhook events include "Workflow runs" |
| Can't access Cloud Run | Check --no-allow-unauthenticated flag, IAM permissions |

---

## Support

- **GitHub App docs:** See GITHUB_APP_SETUP.md
- **Deployment issues:** See DEPLOYMENT_CHECKLIST.md
- **Technical deep-dive:** See SAFE_HEALING_LOOP_GUIDE.md
- **Quick reference:** See HEALING_QUICK_REFERENCE.md

---

## What's Running

**12 Java services:**

1. HealingCircuitBreaker - Retry limiter
2. FixValidationPipeline - 4-stage validation
3. GitHubRateLimiter - Rate limiting
4. SafeInfiniteHealingLoop - Main orchestrator
5. HealingStateManager - Persistence
6. SupremeAIHealingWatchdog - External monitor
7. AutoCodeRepairAgent - Fix generation
8. AdminEscalationService - Alerts
9. HealingSystemController - REST API
10. GitHubAppAuthService - Authentication
11. WebhookListener - Event ingestion (updated)
12. - domain models + supporting services

**Security features:**

- Circuit breaker (max 3 retries)
- Validation pipeline (≥85% confidence)
- Rate limiter (respects GitHub limits)
- External watchdog (kills broken healing)
- PagerDuty escalation
- Firestore audit trail

---

## Timeline

- **Setup:** 10 min (GitHub App)
- **Config:** 5 min (.env)
- **Webhook:** 5 min (GitHub settings)
- **Testing:** 30 min (local)
- **Build:** 10 min (Docker)
- **Deploy:** 10 min (Cloud Run)
- **Monitoring:** 5 min (status check)
- **End-to-end:** 15 min (test workflow)

**Total:** ~2 hours

---

**Ready to go! 🚀**

Next step: Follow [GITHUB_APP_SETUP.md](GITHUB_APP_SETUP.md) to get your app ID and private key.
