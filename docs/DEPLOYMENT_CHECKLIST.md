# Healing System Deployment Checklist

**Project:** SupremeAI Self-Healing GitHub Actions  
**Test Repository:** https://github.com/paykaribazaronline/supremeai  
**Date:** April 12, 2026  

---

## Phase 1: Local Development Setup

> Estimated time: 30 minutes

### GitHub App Setup

- [ ] Create GitHub App `supremeai-bot` (or use existing)
- [ ] Generate private key and save `.pem` file
- [ ] Note App ID: `_______________`
- [ ] Install app on test repository: `paykaribazaronline/supremeai`
- [ ] Note Installation ID: `_______________`
- [ ] Generate webhook secret: `openssl rand -hex 32`
- [ ] Note Webhook Secret: `_______________`

### Environment Configuration

- [ ] Copy template: `cp .env.example .env`
- [ ] Fill in `.env` with GitHub App credentials:

  ```
  GITHUB_APP_ID=...
  GITHUB_APP_PRIVATE_KEY=...
  GITHUB_APP_INSTALLATION_ID=...
  GITHUB_WEBHOOK_SECRET=...
  ```

- [ ] Fill in Escalation credentials:

  ```
  PAGERDUTY_API_KEY=...
  SLACK_WEBHOOK_URL=...
  ADMIN_EMAIL=...
  ```

- [ ] Note: `.env` is git-ignored ✓

### Repository Settings

- [ ] Configure webhook at: Settings → Webhooks
  - [ ] Payload URL: `http://localhost:8080/webhook/github`
  - [ ] Content type: `application/json`
  - [ ] Secret: Set from `GITHUB_WEBHOOK_SECRET`
  - [ ] Events: Workflow runs ✓
  - [ ] Active: ✓

### Build & Run Locally

- [ ] Build: `mvn clean package`
- [ ] Run: `mvn spring-boot:run`
- [ ] Verify startup:

  ```bash
  curl http://localhost:8080/api/healing/health
  # Should return: 200 OK
  ```

- [ ] Check logs: `✓ GitHub App configuration verified`

---

## Phase 2: Configuration Validation

> Estimated time: 15 minutes

### Verify Environment Variables

- [ ] Check all required vars are set:

  ```bash
  echo $GITHUB_APP_ID
  echo $GITHUB_APP_INSTALLATION_ID
  echo $GITHUB_WEBHOOK_SECRET
  # Should output non-empty values
  ```

### Test GitHub API Connection

- [ ] Call test endpoint:

  ```bash
  curl http://localhost:8080/api/healing/status
  # Should return JSON with healingEnabled: true
  ```

### Check Firestore Connection

- [ ] Verify Firebase credentials are set
- [ ] Check Firestore collections exist:
  - `healing_attempts`
  - `error_patterns`

### Validate Rate Limiter

- [ ] Check rate limit status:

  ```bash
  curl http://localhost:8080/api/healing/rate-limit
  # Should show: max 4500 requests/hour
  ```

### Verify Watchdog

- [ ] Check watchdog status:

  ```bash
  curl http://localhost:8080/api/healing/watchdog
  # Should show: enabled true, status HEALTHY
  ```

---

## Phase 3: Manual Testing (Local)

> Estimated time: 45 minutes

### Test Webhook Delivery

- [ ] Push a commit to test repo
- [ ] Check GitHub webhook deliveries:
  - Settings → Webhooks → supremeai-bot → Recent Deliveries
  - Should see successful delivery (200 status)

### Test Manual Workflow Failure

- [ ] Go to: https://github.com/paykaribazaronline/supremeai/actions
- [ ] Select a workflow
- [ ] Click "Run workflow"
- [ ] (Optional) Edit workflow to ensure it fails
- [ ] Monitor healing system:

  ```bash
  curl http://localhost:8080/api/healing/attempts/{workflowId}
  ```

- [ ] Check logs for healing attempt startup

### Test Circuit Breaker

- [ ] Simulate 3 failures for same workflow:
  - [ ] First failure → Healing attempts #1
  - [ ] Second failure → Healing attempts #2
  - [ ] Third failure → Healing attempts #3
  - [ ] VERIFY: Circuit breaker opens, no more attempts

  ```bash
  curl http://localhost:8080/api/healing/circuit-breaker/{workflowId}
  # Should show: status OPEN
  ```

### Test Emergency Disable

- [ ] Disable healing:

  ```bash
  curl -X POST http://localhost:8080/api/healing/disable
  ```

- [ ] Verify disabled:

  ```bash
  curl http://localhost:8080/api/healing/watchdog
  # Should show: enabled false
  ```

- [ ] Re-enable:

  ```bash
  curl -X POST http://localhost:8080/api/healing/enable
  ```

---

## Phase 4: Docker Build & Push

> Estimated time: 20 minutes

### Build Container Image

- [ ] Build image:

  ```bash
  docker build -t supremeai-healing:latest .
  ```

- [ ] Verify image:

  ```bash
  docker images | grep supremeai-healing
  ```

### Tag & Push to Registry

- [ ] Tag with registry:

  ```bash
  docker tag supremeai-healing:latest gcr.io/supremeai-565236080752/supremeai-healing:latest
  ```

- [ ] Push to GCR:

  ```bash
  docker push gcr.io/supremeai-565236080752/supremeai-healing:latest
  ```

- [ ] Verify in GCP Console → Container Registry

### Test Container Locally

- [ ] Run container with env file:

  ```bash
  docker run --env-file .env -p 8080:8080 supremeai-healing:latest
  ```

- [ ] Test health:

  ```bash
  curl http://localhost:8080/api/healing/health
  ```

---

## Phase 5: Cloud Deployment

> Estimated time: 30 minutes

### Deploy to Cloud Run

- [ ] Deploy image:

  ```bash
  gcloud run deploy supremeai-healing \
    --image gcr.io/supremeai-565236080752/supremeai-healing:latest \
    --region us-central1 \
    --memory 2Gi \
    --cpu 2 \
    --set-env-vars GITHUB_APP_ID=...,GITHUB_APP_INSTALLATION_ID=... \
    --set-secrets GITHUB_APP_PRIVATE_KEY=... \
    --no-allow-unauthenticated
  ```

- [ ] Note the Cloud Run URL: `_______________`

### Configure GitHub Webhook for Cloud Run

- [ ] Go to GitHub repo settings → Webhooks
- [ ] Edit the webhook:
  - [ ] Change payload URL to Cloud Run URL
  - [ ] Test delivery
  - [ ] Should see 200 OK

### Configure Service Account Access

- [ ] Ensure service account has permissions:
  - [ ] Firebase (read/write healing_attempts collection)
  - [ ] Logging (write logs)
  - [ ] PagerDuty API (if internal connection needed)

---

## Phase 6: Production Validation

> Estimated time: 30 minutes

### Health Checks

- [ ] Cloud Run health:

  ```bash
  CLOUD_URL=https://your-cloud-run-url
  curl $CLOUD_URL/api/healing/health
  # Should return 200 OK
  ```

### Test Workflow Failure Detection

- [ ] Push a deliberate failure to repo
- [ ] Verify webhook is received:

  ```bash
  # Check Cloud Run logs
  gcloud run logs read supremeai-healing
  # Should see: "🚨 Workflow failure detected"
  ```

### Monitor Firestore

- [ ] Check Firestore console:
  - Collection: `healing_attempts`
  - Should see new documents for healing attempts
  - Status should be: ATTEMPTED or SUCCESS or FAILED

### Scaling Preparation

- [ ] Configure auto-scaling in Cloud Run:
  - Minimum instances: 1
  - Maximum instances: 10
  - CPU concurrency: 100

---

## Phase 7: Monitoring & Alerting Setup

> Estimated time: 45 minutes

### Cloud Logging & Metrics

- [ ] Create log sink for healing system:
  - [ ] Filter: `resource.type="cloud_run_revision" labels.service_name="supremeai-healing"`
  - [ ] Destination: Cloud Logging

### PagerDuty Integration

- [ ] Test escalation:

  ```bash
  curl -X POST $CLOUD_URL/api/healing/escalate \
    -H "Content-Type: application/json" \
    -d '{"workflowId": "test", "reason": "Test escalation"}'
  ```

- [ ] Verify incident created in PagerDuty ✓

### Slack Integration

- [ ] Test notification:
  - [ ] Manually trigger healing disable
  - [ ] Verify Slack message received ✓

### Dashboard Setup (Recommended)

- [ ] Create Grafana/Datadog dashboard:
  - Healing success rate (%)
  - Average fail-to-fix time
  - Circuit breaker openings
  - Rate limiter utilization
  - Watchdog health

---

## Phase 8: Documentation & Handoff

> Estimated time: 20 minutes

### Documentation Review

- [ ] README.md updated with:
  - [ ] Healing system overview
  - [ ] Quick start instructions
  - [ ] GitHub App setup link
- [ ] Team has access to:
  - [ ] SAFE_HEALING_LOOP_GUIDE.md
  - [ ] HEALING_QUICK_REFERENCE.md
  - [ ] GITHUB_APP_SETUP.md

### Knowledge Transfer

- [ ] Walkthrough for on-call team:
  - [ ] How to check healing status
  - [ ] Emergency disable procedure
  - [ ] How to read Firestore audit trail
  - [ ] Escalation procedures

### Runbook Creation

- [ ] Create ops runbook:
  - [ ] Normal operations
  - [ ] Emergency stop
  - [ ] Recovery procedures
  - [ ] Troubleshooting

---

## Phase 9: Post-Deployment Monitoring

> Ongoing

### First 24 Hours

- [ ] Monitor success rate: Target >70%
- [ ] Check error patterns in Firestore
- [ ] Verify no false positives (bad fixes deployed)
- [ ] Watch Cloud Run logs for issues
- [ ] Monitor GitHub API rate limit usage

### First Week

- [ ] Target success rate: >80%
- [ ] Aggregate learnings from error patterns
- [ ] Fine-tune validation pipeline thresholds
- [ ] Review PagerDuty/Slack escalations
- [ ] Check watchdog never triggered

### Optimization

- [ ] Analyze most common error types
- [ ] Improve fix templates for common issues
- [ ] Tune circuit breaker thresholds if needed
- [ ] Document patterns learned

---

## Emergency Procedures

### If Healing System Malfunctions

1. **IMMEDIATE: Disable Healing**

   ```bash
   CLOUD_URL=https://your-cloud-run-url
   curl -X POST $CLOUD_URL/api/healing/disable
   ```

2. **Check Status**

   ```bash
   curl $CLOUD_URL/api/healing/diagnostics
   ```

3. **Investigate**
   - Cloud Run logs: `gcloud run logs read supremeai-healing --limit 50`
   - Firestore: Check recent `healing_attempts`
   - GitHub webhook: Check delivery logs

4. **Fix & Restart**
   - Deploy fixed version: `gcloud run deploy supremeai-healing ...`
   - Test locally first!

5. **Re-enable**

   ```bash
   curl -X POST $CLOUD_URL/api/healing/enable
   ```

---

## Rollback Procedure

If something goes wrong in production:

```bash
# Redeploy previous version
gcloud run deploy supremeai-healing \
  --image gcr.io/supremeai-565236080752/supremeai-healing:previous-tag \
  --region us-central1

# Disable until ready
curl -X POST $CLOUD_URL/api/healing/disable

# Investigate
gcloud run logs read supremeai-healing --limit 100

# Fix & redeploy
# (repeat from build step)
```

---

## Sign-Off

- **Deployed By:** `_______________`
- **Deployment Date:** `_______________`
- **Cloud Run URL:** `_______________`
- **PagerDuty Integration:** ✓ Verified
- **Slack Integration:** ✓ Verified
- **Firestore Schema:** ✓ Ready
- **GitHub App:** ✓ Installed
- **Initial Testing:** ✓ Passed

**Status:** ✅ READY FOR PRODUCTION

---

## Support Contacts

- **GitHub App Issues:** Check https://github.com/apps/supremeai-bot
- **Cloud Run Issues:** Check GCP Console
- **Firestore Issues:** Check Firebase Console
- **On-Call:** [Your team's on-call rotation]
