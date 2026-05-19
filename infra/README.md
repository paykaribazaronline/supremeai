# Sentiment Analysis API — GCP + n8n + BERT ML

> **Production-ready, cost-optimised, fully IaC sentiment analysis API**  
> n8n (Cloud Run) + BERT model (Cloud Run) + Terraform — end-to-end deployed on Google Cloud Platform.

---

## Architecture Overview

```
Internet
   │
   │  HTTPS (TLS 1.3)
   ▼
Cloud Armor WAF   ── rate-limit 500 req/min/IP, IP allowlist, DDoS, XSS, SQLi
   │
   ▼
┌─────────────────────────────────────────────────────────────────┐
│  n8n Cloud Run  (sentiment-ml-workflow)  ●●●●●●●●●●●●●●●●●●●   │
│  • Webhook /sentiment (POST)                                    │
│  • JSON schema validation (text, max 5000 chars)              │
│  • POST to ML service  ───────────────┐                        │
│  • Post-process results                │ internal               │
│  • Slack alert on negative>0.9        │ (no public IP)         │
│  • Structured JSON response            │                       │
└───────────────────┬───────────────────┘                       │
                    │ invoker token (SA only)                     │
                    ▼                                             │
┌─────────────────────────────────────────────────────────────────┐│
│  BERT ML Cloud Run  (sentiment-ml-service)  ──────────────────  ││
│  distilbert-base-uncased-finetuned-sst-2-english               ││
│  • /predict  →  {sentiment, confidence, processed_at}         ││
│  • /health /ready /metrics                                      │
└─────────────────────────────────────────────────────────────────┘│
     │                              │                               │
     ▼                              ▼                               │
Secret Manager             GCS (workflow + backup buckets)         │
(n8n key, ML key)          KMS 90-day key rotation                  │
                            Cloud Trace  ──────────────────────────┘
                            Cloud Monitoring alerts ─────────────────
                            Cloud Scheduler ───────────────  daily backups
```

---

## What Gets Deployed

| Resource | Type | Purpose |
|---|---|---|
| `n8n-workflow` | Cloud Run | n8n orchestration + public webhook |
| `sentiment-ml` | Cloud Run | BERT inference service |
| `supremeai-n8n-storage-prod` | GCS | n8n workflow persistent storage |
| `supremeai-n8n-backups-prod` | GCS | Daily backup, 30-day retention |
| `n8n-encryption-key` / `n8n-basic-auth-credentials` / `ml-api-gateway-key` | Secret Manager | API keys + encryption |
| `n8n-vpc-connector` | VPC Access Connector | Serverless VPC control plane |
| `n8n-armor` | Cloud Armor | WAF, IP allowlist, DDoS, rate-limit |
| `n8n-keyring / n8n-storage-key / n8n-secrets-key` | Cloud KMS | Customer-managed encryption |90-day rotation |
| No auth policies | Cloud Monitoring | n8n failures, ML latency, error rate |
| `rotate-n8n-encryption-key` / `daily-n8n-backup` | Cloud Scheduler | 90-day secret rotation + daily backups |

---

## Quick Start

### 1. Prerequisites

```bash
# Install Terraform   https://developer.hashicorp.com/terraform/install
# Install gcloud CLI  https://cloud.google.com/sdk/install
# Install Docker       https://docs.docker.com/get-docker/
# Install Python 3    # for the ML service
# Install Make (optional)

gcloud auth login
gcloud config set project YOUR_PROJECT_ID
terraform version  # >= 1.5
docker --version
```

### 2. Clone / extract

```bash
cd infra
```

### 3. One-command deploy

```bash
export GCP_PROJECT_ID=your-gcp-project
export GCP_REGION=us-central1   # default: us-central1
./deploy.sh
```

Terraform provisions every GCP resource in ~8–12 min. The script then builds and
pushes the ML Docker image, deploys both services, stores environment variables in
Secret Manager, and runs smoke tests.

### 4. Get the public URL

```bash
terraform output n8n_public_url
# → https://n8n-workflow-xxxxxx-uc.a.run.app/sentiment
```

That `/sentiment` path is the production API endpoint.

---

## Environment Variables Required at Deploy Time

Pass to `deploy.sh` or to the ML service:

| Variable | Description | Default |
|---|---|---|
| `GCP_PROJECT_ID` | **Required.** Your GCP project | — |
| `GCP_REGION` *(optional)* | GCP region for all resources | `us-central1` |
| `MODEL_NAME` *(ML only)* | HuggingFace model ID | `distilbert-base-uncased-finetuned-sst-2-english` |
| `MAX_TEXT_LENGTH` *(ML only)* | Max allowed text chars | `5000` |
| `RATE_LIMIT_RPS` *(ML only)* | Rate limit per IP | `100` |

n8n environment variables are injected from **GCP Secret Manager** at runtime
by Terraform (no `.env` files committed to the repo).

---

## Make Targets

```bash
make help          # Show all targets
make tf-plan       # Terraform plan only (read-only)
make tf-apply      # Apply Terraform plan (provisions GCP)
make deploy        # Full end-to-end deploy (requires env vars)
make smoke         # URL reachability smoke test
make test          # Run e2e test suite
make show-urls     # Print all Terraform outputs
make logs          # Tail n8n Cloud Run logs
make ml-logs       # Tail ML service logs
```

---

## E2E Test Suite

```bash
export GCP_PROJECT_ID=...
export N8N_URL=$(cd terraform && terraform output -raw n8n_public_url)
export ML_URL=$(cd terraform && terraform output -raw ml_service_url)
export API_KEY=$(gcloud secrets versions access \
    latest --project=$GCP_PROJECT_ID --secret=ml-api-gateway-key)

cd infra && make test
```

| # | Test | Expected |
|---|------|----------|
| 1 | Short positive text | `200`, `sentiment=positive` |
| 2 | Short negative text | `200`, `sentiment=negative` |
| 3 | Exactly 5000 chars | `200` |
| 4 | 5001 chars | `400`, error message |
| 5 | Missing/null/empty text | `400` |
| 6 | 10 MB payload | `400` |
| 7 | Non-JSON body | `400` |
| 8 | No API key | `401` |
| 9 | Wrong API key | `401` |
| 10 | Invalid Content-Type | `400` |
| 11 | JSON schema — all fields present | pass |
| 12 | e2e latency < 5 s | pass |

---

## Public API Contract

**Endpoint**: `POST {n8n_public_url}/sentiment`

### Request

```json
{
  "text": "I love this product, it is amazing!"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `text` | string | ✔ | Text to analyse; must be a string. Trimmed; no leading/trailing whitespace |
| `max_length` | 5000 chars | — | Hard upper bound enforced by n8n and ML service |

### Response  — `200 OK`

```json
{
  "sentiment":     "positive",
  "confidence":    0.98765,
  "processed_at":  "2024-05-19T14:23:01.123Z",
  "requested_at":  "2024-05-19T14:23:01.100Z",
  "request_ip":    "203.0.113.42",
  "model":         "distilbert-base-uncased-finetuned-sst-2-english",
  "positive_score":   0.98765,
  "negative_score":   0.01235,
  "neutral_score":    0.00618
}
```

| Field | Type | Description |
|---|---|---|
| `sentiment` | `"positive"\|"negative"\|"neutral"` | Classified label |
| `confidence` | `float [0, 1]` | Model confidence |
| `processed_at` | ISO-8601 timestamp | Inference timestamp |
| `requested_at` | ISO-8601 timestamp | Received timestamp |
| `request_ip` | string or null | Client IP |
| `model` | string | Model identifier |

### Error Responses

| Status | Body |
|---|---|
| `400` | `{ "error": "Field 'text' is required …" }` |
| `400` | `{ "error": "'text' exceeds maximum length …" }` |
| `401` | `{ "error": "Unauthorized" }` |
| `429` | `{ "error": "Rate limit exceeded …", "limit_per_min": 100 }` |
| `500` | `{ "error": "Inference failed.", "detail": "…" }` |

---

## Security

| Feature | Detail |
|---|---|
| **TLS 1.3+** | Enforced by GCP Load Balancer / Cloud Run |
| **Admin allowlist** | Cloud Armor IP allowlist for n8n admin |
| **Auth** | Apollo/IAM-based workload identity (no key files) |
| **Secrets** | GCP Secret Manager — KMS 90-day rotation |
| **Encryption in transit** | TLS 1.3 on all endpoints |
| **Encryption at rest** | Cloud Storage → CMEK (n8n-secrets-key) |
| **Secrets in Secret Manager** | n8n-encryption-key, n8n-basic-auth, ml-api-gateway-key |
| **ML service – secret key** | X-API-Key per-request, secret injected from Secret Manager |
| **Rate-limit** | 500 req/min/IP globally |
| **No hardcoded credentials** | All secrets are env-var references |
| **RBAC** | n8n, ML, Ops service accounts — least-privilege |

---

## Cost Estimate — 100 000 Monthly Inference Requests

| Resource | Monthly Cost |
|---|---|
| ML Service Cloud Run (cold start, ~250 ms avg inference, 200 MiB/mem, 1 CPU) ≈ 1.5 vCPU-seconds/req × 100 k = 150 k vCPU-s + 300 GiB-s | ~$4 |
| n8n Cloud Run (same usage pattern) | ~$4 |
| GCS workflow storage (2 GB) | ~$0.01 |
| GCS backup storage (Nearline 30-day, 5 GB/month) | ~$0.12 |
| Secret Manager (3 secrets, 2 KMS keys) + rotator | ~$0.05 |
| Monitoring / Tracing | < $0.50 |
| **Total** | **≈ <$10/month** |

The $0.05 estimate is achievable given n8n min-instances=0, Cloud Run pay-per-use,
and Nearline storage with lifecycle deletion. Actual cost with min-instances=0 is
dominantly per-request: ~$0.08 per 1000 requests for CPU+memory.

---

## Key Files

```
infra/
├── terraform/
│   ├── providers.tf            # TerraformProviders Registry + state
│   ├── variables.tf            # All configureable inputs
│   ├── outputs.tf              # Public URL, bucket names, SA emails
│   └── main.tf                 # All GCP infra resources
├── ml-service/
│   ├── Dockerfile              # Production Docker image
│   ├── requirements.txt        # Python dependencies
│   └── app.py                  # BERT inference + rate-limit + auth
├── n8n-workflow/
│   └── sentiment-analysis-workflow.json   # Exported n8n workflow
├── tests/
│   └── e2e_sentiment_test.sh   # Full e2e test suite
├── deploy.sh                   # One-command deploy orchestrator
└── Makefile                    # Make targets for every operation
```

---

## IAM / RBAC Summary

| Role | Who | Permissions |
|---|---|---|
| `n8n-sa` | n8n Cloud Run service | Storage RW, Secret LR, Cloud Run Invoker (n8n→ML), Pub/Sub Publisher, Cloud Trace Metric Writer, Cloud Scheduler runner |
| `ml-sa` | ML Cloud Run service | Storage RO, Secret LR, Cloud Trace Agent, Metric Writer |
| `ops-sa` | Operations / Scheduled jobs | Storage Admin, Secret Secret Accessor (read-only ops) |

No service account key files are ever written to disk. All access is via workload
identity / service account references.

---

## n8n Workflow Pipeline

```
POST /sentiment
   │
   ▼
Validate Input  ── checks JSON has non-empty "text"
   │
   ▼
Validate Length  ── rejects > 5 000 chars
   │
   ▼
Call ML Inference  ── POST {ML_URL}/predict  (X-API-Key from Secret Manager)
   │
   ▼
Post-Process  ── standardise sentiment/confidence/timestamp
   │
   ├─ sentiment == "negative" AND confidence > 0.9 ──► Slack Alert
   │
   ▼
Build Final Response  ── JSON + security headers
   │
   ▼
200 JSON  ← returned to caller
```

---

## Troubleshooting

```bash
# View n8n logs
cd terraform && make logs

# View ML service logs
make ml-logs

# Debug Terraform state
cd terraform && terraform state list

# Force-reload ML model (Cloud Run restart)
gcloud run services replace-pending-revisions sentiment-ml --region=us-central1

# Manual e2e test with curl
curl -sS -X POST "$(terraform -chdir=terraform output -raw n8n_public_url)/sentiment" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $(gcloud secrets versions access latest --project=$GCP_PROJECT_ID --secret=ml-api-gateway-key)" \
  -d '{"text":"I love this!"}' | python3 -m json.tool
```

---

## License

Internal use — SupremeAI. All secrets via GCP Managed Identity. No hardcoded credentials.
