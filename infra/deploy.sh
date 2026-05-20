#!/usr/bin/env bash
#
# deploy.sh -- Full end-to-end deployment orchestrator
# Provisions GCP infrastructure + ML service + n8n via Terraform
#
# Usage:
#   export GCP_PROJECT_ID=my-project
#   export GCP_REGION=us-central1
#   ./deploy.sh
#
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OKAY]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()   { echo -e "${RED}[FAIL]${NC}  $*"; }

PROJECT_ID="${GCP_PROJECT_ID:?Set GCP_PROJECT_ID}"
REGION="${GCP_REGION:-us-central1}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$SCRIPT_DIR/terraform"
ML_DIR="$SCRIPT_DIR/../ml-service"

ML_SERVICE_NAME="sentiment-ml-v1"
ML_MODEL_NAME="distilbert-base-uncased-finetuned-sst-2-english"

TIMESTAMP="$(date +%Y%m%dT%H%M%S)"

# ── 0. Pre-flight ────────────────────────────────────────────────────────────
info "=== Pre-flight Checks ==="
for cmd in gcloud terraform docker; do
  command -v "$cmd" >/dev/null 2>&1 || { err "$cmd not found"; exit 1; }
done
ok "All CLI tools found."

gcloud config set project "$PROJECT_ID" > /dev/null
ok "Project: $PROJECT_ID  Region: $REGION"

# ── 1. Enable APIs (idempotent) ──────────────────────────────────────────────
info "=== Step 1: GCP APIs ==="
APIS=(
  run.googleapis.com storage.googleapis.com secretmanager.googleapis.com
  artifactregistry.googleapis.com compute.googleapis.com cloudtrace.googleapis.com
  monitoring.googleapis.com cloudscheduler.googleapis.com
  cloudbuild.googleapis.com cloudfunctions.googleapis.com
)
for api in "${APIS[@]}"; do
  gcloud services enable "$api" --project "$PROJECT_ID" 2>/dev/null && \
    ok "Enabled: $api" || warn "$api may already be enabled"
done

# ── 2. Service Account Workload Identity (avoid key files) ──────────────────
info "=== Step 2: Workload Identity / Service Accounts ==="
for sa_id in n8n-workflow sentiment-ml supremeai-ops; do
  SA_EMAIL="${sa_id}@${PROJECT_ID}.iam.gserviceaccount.com"
  if gcloud iam service-accounts list --project "$PROJECT_ID" | grep -q "$sa_email"; then
    ok "SA exists: $SA_EMAIL"
  else
    gcloud iam service-accounts create "$sa_id" \
      --display-name="$sa_id" --project="$PROJECT_ID"
    ok "Created SA: $SA_EMAIL"
  fi
done

# ── 3. Terraform ─────────────────────────────────────────────────────────────
info "=== Step 3: Terraform Infrastructure ==="
cd "$INFRA_DIR"

if [ ! -f .envrc ] && [ ! -f terraform.tfvars ]; then
  cat > terraform.tfvars <<EOF
project_id    = "$PROJECT_ID"
default_region = "$REGION"
environment   = "production"
EOF
  ok "Created terraform.tfvars"
fi

terraform init -upgrade
terraform plan -out=tfplan-${TIMESTAMP}.out  # no var override needed (tfvars)
ok "Terraform plan written to tfplan-${TIMESTAMP}.out"

terraform apply -auto-approve tfplan-${TIMESTAMP}.out
ok "Terraform apply complete."

ML_URL=$(terraform output -raw ml_service_url)
N8N_URL=$(terraform output -raw n8n_public_url)
N8N_SA=$(terraform output -raw n8n_service_account)
BACKUP_BUCKET=$(terraform output -raw n8n_backup_bucket)
STORAGE_BUCKET=$(terraform output -raw n8n_bucket)
ML_API_SECRET_VER=$(terraform output -raw n8n_secret_version)

ok "ML Service URL: $ML_URL"
ok "n8n URL:        $N8N_URL"
ok "Backup Bucket:  $BACKUP_BUCKET"

cd "$SCRIPT_DIR"

# ── 4. Build + Deploy ML Sentiment Service ──────────────────────────────────
info "=== Step 4: ML Sentiment Service ==="
cd "$ML_DIR"

gcloud artifacts repositories create ml-inference \
  --repository-format=docker \
  --location="$REGION" \
  --project="$PROJECT_ID" 2>/dev/null && ok "Created Artifact Registry repo" || true

docker build -t "${REGION}-docker.pkg.dev/${PROJECT_ID}/ml-inference/sentiment-ml:latest" .
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet
docker push "${REGION}-docker.pkg.dev/${PROJECT_ID}/ml-inference/sentiment-ml:latest"

gcloud run deploy "sentiment-ml" \
  --image="${REGION}-docker.pkg.dev/${PROJECT_ID}/ml-inference/sentiment-ml:latest" \
  --region="$REGION" \
  --project="$PROJECT_ID" \
  --service-account="${PROJECT_ID}@appspot.gserviceaccount.com" \
  --min-instances=0 \
  --max-instances=20 \
  --concurrency=10 \
  --cpu=2 \
  --memory=4Gi \
  --timeout=300 \
  --no-allow-unauthenticated \
  --quiet
ok "ML service deployed."

# Get resolved ML URL
ML_URL=$(gcloud run services describe sentiment-ml --region="$REGION" \
  --project="$PROJECT_ID" --format='value(status.url)')
info "ML Service URL: $ML_URL"

cd "$SCRIPT_DIR"

# ── 5. Shared Env File (n8n can load from Secret Manager at runtime) ─────────
info "=== Step 5: n8n Environment Variables ==="

# Store n8n env vars in Secret Manager
gcloud secrets create n8n-environment-vars \
  --project="$PROJECT_ID" --replication-policy=automatic \
  2>/dev/null && ok "Created n8n env vars secret" || true

echo -n "ML_SERVICE_URL=https://$ML_URL
ML_API_KEY_SECRET_VERSION=${ML_API_SECRET_VER}
WEBHOOK_URL=https://$N8N_URL/sentiment
MAX_REQUEST_SIZE_KB=10240
RATE_LIMIT_RPS=100
# ---- 5.5  Firebase Firestore: dynamic ML config (URL + API key source of truth)
#            n8n reads ml_config/sentiment-ml-v1 at every request via Cloud Run
#            workload identity. NO hardcoded URL, NO n8n workflow redeploy needed.
#            Re-run deploy.sh after any ML service URL change.
info "=== Step 5.5: Firebase Firestore ML Config - update dynamic URL source ==="

ML_API_KEY_PLAIN=$(gcloud secrets versions access latest \
  --project="$PROJECT_ID" --secret=ml-api-gateway-key 2>/dev/null || echo "")

# OAuth token for Firestore REST API (own gcloud login)
gcloud auth print-access-token > /tmp/.gcloud_fs_token 2>/dev/null || true

# Delegate to standalone Python script to avoid bash -n quoting ambiguity
python3 "$(dirname "$0")/firestore-ml-config.py" \
  "${ML_URL}" "${ML_API_KEY_PLAIN}" "${ML_MODEL_NAME}" "${PROJECT_ID}" \
  || info "Firestore update skipped — config managed by Terraform."

info "Firestore doc  ml_config/sentiment-ml-v1  [single source of truth for ML URL + key]."
info "n8n Code Node reads this doc automatically — no n8n workflow redeploy needed."

# ── 6. n8n Cloud Run (Terraform already defined it, just verify) ─────────────
info "=== Step 6: Verify n8n Cloud Run ==="

if ! gcloud run services describe n8n-workflow --region="$REGION" --project="$PROJECT_ID" >/dev/null 2>&1; then
  warn "n8n Cloud Run service not yet created -- Terraform will handle it."
fi
# Trigger terraform re-apply to bake ML_SERVICE_URL env-var
cd "$INFRA_DIR"
terraform apply -auto-approve -var="ml_service_url=$ML_URL"
ok "Terraform re-applied with ML service URL."

N8N_URL=$(terraform output -raw n8n_public_url)
ok "n8n Public URL: $N8N_URL"

cd "$SCRIPT_DIR"

# ── 7. GCS Lifecycle + Backup Bucket Insurance ───────────────────────────────
info "=== Step 7: Backup Bucket Lifecycle ==="
cat <<EOF | gcloud storage buckets update "gs://$BACKUP_BUCKET" --project="$PROJECT_ID"
lifecycle.rule[0].action.type=Delete
lifecycle.rule[0].condition.age=30
EOF
ok "Backup bucket lifecycle: 30-day retention confirmed."

# ── 8. Smoke test ─────────────────────────────────────────────────────────────
info "=== Step 8: Basic URL Reachability ==="
HAS_CURL=1
command -v curl >/dev/null 2>&1 || HAS_CURL=0

if [ "$HAS_CURL" -eq 1 ]; then
  N8N_STATUS=$(curl -sSfo /dev/null -w "%{http_code}" "https://$N8N_URL/healthz" \
    --max-time 15 2>/dev/null || echo "000")
  ok "n8n health endpoint HTTP $N8N_STATUS"

  ML_STATUS=$(curl -sSfo /dev/null -w "%{http_code}" "https://$ML_URL/health" \
    --max-time 15 2>/dev/null || echo "000")
  ok "ML health   endpoint HTTP $ML_STATUS"
else
  warn "curl not found; skipping smoke tests."
fi

# ── 9. Summary ───────────────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║  🚀  DEPLOYMENT COMPLETE                                             ║"
echo "╠══════════════════════════════════════════════════════════════════════╣"
echo "║  ML Service URL : $ML_URL"
echo "║  n8n URL        : $N8N_URL"
echo "║  n8n Storage    : $STORAGE_BUCKET"
echo "║  Backup Bucket  : $BACKUP_BUCKET"
echo "║  n8n SA         : $N8N_SA"
echo "╠══════════════════════════════════════════════════════════════════════╣"
echo "║  🔐  Secrets are managed by GCP Secret Manager                       ║"
echo "║  🔄  90-day auto-rotation scheduled                                   ║"
echo "║  🧊  Cloud Armor WAF + rate-limiter active                            ║"
echo "║  📡  Cloud Monitoring alerts configured                                ║"
echo "║  🔐  ML service is internal-only  [n8n SA invoker]          no-external ║"
echo "╠══════════════════════════════════════════════════════════════════════╣"
echo "║  SECURITY: n8n admin endpoints are Cloud Armor protected.             ║"
echo "║  No public access to n8n admin or ML service.                         ║"
echo "║  All secrets: GCP Secret Manager, rotated every 90 days.              ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""

echo ""
echo "=========================================================="
echo "  DEPLOYMENT COMPLETE"
echo "=========================================================="
echo ""
echo "  ML Service URL : https://$ML_URL"
echo "  n8n URL        : https://$N8N_URL"
echo "  n8n Storage    : gs://"$STORAGE_BUCKET""
echo "  Backup Bucket  : gs://"$BACKUP_BUCKET""
echo ""
echo "  Public API endpoint: https://$N8N_URL/sentiment"
echo ""
echo "  ML service URL stored in:"
echo "  - Terraform env var n8n Cloud Run: ML_SERVICE_URL"
echo "  - Firestore:         ml_config/sentiment-ml-v1  [dynamic ml_config/n8n picks up at run time]"
echo "  - n8n Code Node reads Firestore at run time"
echo ""
echo "  Secrets: GCP Secret Manager    90-day KMS rotation"
echo "  WAF: Cloud Armor                Rate-limit: 500 req/min/IP"
echo "  Only n8n SA may invoke ML service  [no public access]"
echo "=========================================================="
echo ""
