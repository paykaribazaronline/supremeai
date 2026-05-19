# Step-by-Step Deployment Instructions

## Prerequisites

- GCP Project with billing enabled (`gcloud init && gcloud projects create`)
- Terraform >= 1.5
- Docker >= 20
- Python 3.11
- `gcloud` CLI authenticated (`gcloud auth login`)

---

## Phase 1 — Provision GCP Infrastructure (Terraform)

```bash
cd infra
export GCP_PROJECT_ID=<your-gcp-project>
export GCP_REGION=us-central1

# Review the plan (read-only — no changes made)
make tf-plan

# Apply ~ all GCP resources
make tf-apply  # ~8–15 minutes
```

What Terraform creates:
- Cloud Run `n8n-workflow` + `sentiment-ml`
- Storage, Secret Manager, Cloud Armor, VPC Connector, Cloud KMS, Alert Policies, Scheduler jobs

---

## Phase 2 — Build and Deploy ML Sentiment Service

```bash
# Build Docker image
make ml-build

# Push to Artifact Registry
make ml-push

# Deploy to Cloud Run
make ml-deploy

# Verify health
curl -sS https://$(cd terraform && terraform output -raw ml_service_url)/health
```

---

## Phase 3 — Load n8n Workflow

```bash
# Get the public URL
N8N_URL=$(cd terraform && terraform output -raw n8n_public_url)

# Open n8n in browser (authenticated via Basic Auth / IAP)
# Username/password: retrieve from Secret Manager
gcloud secrets versions access latest \
  --project=$GCP_PROJECT_ID \
  --secret=n8n-basic-auth-credentials
# → base64-encoded "username:password", decode and log in

# Import workflow:  Menu → Workflows → Import → upload
#   infra/n8n-workflow/sentiment-analysis-workflow.json

# IMPORTANT: Fill in missing credentials after import
#   Node → Slack Alert → "Resource" → add real Slack connection
#   Node → Incident Record  → comment out or replace with BigQuery tool
```

---

## Phase 4 — Validate End-to-End

```bash
export GCP_PROJECT_ID=<your-project>
export N8N_URL=https://$(cd terraform && terraform output -raw n8n_public_url)
export ML_URL=https://$(cd terraform && terraform output -raw ml_service_url)
export API_KEY=$(gcloud secrets versions access \
  latest --project=$GCP_PROJECT_ID --secret=ml-api-gateway-key)

make smoke
make test

# Test the public endpoint directly
curl -sS -X POST "$N8N_URL/sentiment" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{"text":"I absolutely love this product!"}' | python3 -m json.tool
```

Expected output:
```json
{
  "confidence": 0.98765,
  "model": "distilbert-base-uncased-finetuned-sst-2-english",
  "negative_score": 0.01235,
  "neutral_score": 0.00617,
  "positive_score": 0.98765,
  "processed_at": "2025-...",
  "request_ip": "...",
  "requested_at": "...",
  "sentiment": "positive",
  "tracer_id": "supremeai-..."
}
```

---

## Phase 5 — Verify Security Posture

```bash
# ML service must NOT be publicly invocable
gcloud run services get-iam-policy sentiment-ml \
  --region=us-central1 --project=$GCP_PROJECT_ID \
  | grep "allUsers" && echo "ERROR: ML service is public!" \
  || echo "OK: ML service is not public"

# n8n Cloud Armor policy
gcloud compute security-policies describe n8n-workflow-armor \
  --project=$GCP_PROJECT_ID

# TLS check
curl -svo /dev/null "$N8N_URL/sentiment" 2>&1 | grep "SSL"
# Must contain TLS 1.2 or TLS 1.3

# Secret Manager: no service account key files committed
cd /your-repo
git diff --cached | grep "private_key\|-----BEGIN\|service_account.json" \
  && echo "ERROR: Commit contains credentials!" || echo "OK: No secrets in git"
```

---

## Ongoing Operations

### View n8n logs
```bash
gcloud logging read \
  "resource.labels.service_name=n8n-workflow AND severity>=ERROR" \
  --project=$GCP_PROJECT_ID --limit 50
```

### View ML service logs
```bash
gcloud logging read \
  "resource.labels.service_name=sentiment-ml AND severity>=ERROR" \
  --project=$GCP_PROJECT_ID --limit 50
```

### Manually rotate ML API key
```bash
NEW_KEY=$(openssl rand -hex 32)
echo -n "$NEW_KEY" | gcloud secrets versions add ml-api-gateway-key \
  --project=$GCP_PROJECT_ID --data-file=-
# Trigger a rolling restart of ML service to pick up new key
gcloud run services update sentiment-ml --region=us-central1 --update-env-vars=__DUMMY__=x
```

### Manually trigger n8n backup
```bash
gcloud jobs execute \
  projects/$GCP_PROJECT_ID/locations/us-central1/jobs/daily-n8n-backup
```

### Rotate n8n encryption key immediately (bypasses 90-days schedule)
```bash
# Load n8n-ops SA credentials into env or workload identity then:
gcloud secrets versions add n8n-encryption-key \
  --project=$GCP_PROJECT_ID \
  --data-file=<(openssl rand -hex 64)
# Rotate via GKE or init container as needed — n8n reads latest on restart
```

---

## Tear Down

```bash
cd infra
terraform destroy -auto-approve
```

This removes all GCP resources. Backups in `supremeai-n8n-backups-prod` persist
for 30 days unless manually deleted.

---

## Cost Estimate — 100 000 Monthly Requests

| Service | Approx. monthly |
|---|---|
| sentiment-ml Cloud Run | $4–6 |
| n8n workflow Cloud Run | $4–6 |
| GCS storage + Nearline backup | $0.15 |
| Secret Manager + KMS | $0.05 |
| Monitoring / Tracing | < $1 |
| **Total** | **≈ $5–10** |

Min-instances=0 on both Cloud Run services eliminates idle cost.
