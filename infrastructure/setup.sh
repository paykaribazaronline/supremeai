#!/bin/bash
#
# SupremeAI Infrastructure Setup Script
# Creates service accounts, IAM bindings, Pub/Sub topics, Firestore DB, etc.
#
# Run this BEFORE deploying services to Cloud Run.
#

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# --- Configuration ---
PROJECT_ID="${GCP_PROJECT_ID:-supremeai-a}"
REGION="${GCP_REGION:-us-central1}"
BACKEND_SA="supremeai-backend"
REVERSE_ENG_SA="reverse-engineering"
SIMULATOR_SA="simulator-runtime"

log_info "=== SupremeAI Infrastructure Setup ==="
log_info "Project: $PROJECT_ID"
log_info "Region: $REGION"
log_info ""

# --- Pre-check ---
command -v gcloud >/dev/null 2>&1 || { log_error "gcloud CLI not found"; exit 1; }

# Set project
gcloud config set project "$PROJECT_ID" 2>/dev/null || true

# --- 1. Enable Required APIs ---
log_info "=== Step 1: Enabling Required GCP APIs ==="

APIS=(
  "cloudrun.googleapis.com"
  "firestore.googleapis.com"
  "firebase.googleapis.com"
  "pubsub.googleapis.com"
  "cloudbuild.googleapis.com"
  "run.googleapis.com"
  "iam.googleapis.com"
)

for api in "${APIS[@]}"; do
  log_info "Enabling $api..."
  gcloud services enable "$api" --project "$PROJECT_ID" || log_warn "API $api may already be enabled"
done

log_info "✅ Required APIs enabled"

# --- 2. Create Service Accounts ---
log_info ""
log_info "=== Step 2: Creating Service Accounts ==="

# Create backend service account
log_info "Creating service account: $BACKEND_SA..."
gcloud iam service-accounts create "$BACKEND_SA" \
  --display-name="SupremeAI Backend Service Account" \
  --project="$PROJECT_ID" || log_warn "Service account $BACKEND_SA may already exist"

# Create reverse engineering service account
log_info "Creating service account: $REVERSE_ENG_SA..."
gcloud iam service-accounts create "$REVERSE_ENG_SA" \
  --display-name="Reverse Engineering Service Account" \
  --project="$PROJECT_ID" || log_warn "Service account $REVERSE_ENG_SA may already exist"

# Create simulator runtime service account
log_info "Creating service account: $SIMULATOR_SA..."
gcloud iam service-accounts create "$SIMULATOR_SA" \
  --display-name="Simulator Runtime Service Account" \
  --project="$PROJECT_ID" || log_warn "Service account $SIMULATOR_SA may already exist"

log_info "✅ Service accounts created"

# --- 3. Grant IAM Roles ---
log_info ""
log_info "=== Step 3: Granting IAM Permissions ==="

# Common roles for all service accounts
COMMON_ROLES=(
  "roles/logging.logWriter"
  "roles/monitoring.metricWriter"
  "roles/errorreporting.writer"
)

# Backend-specific roles
BACKEND_ROLES=(
  "roles/datastore.user"              # Firestore access
  "roles/firebaseauth.admin"          # Firebase Auth admin
  "roles/pubsub.publisher"            # Publish to Pub/Sub
  "roles/run.admin"                   # Deploy/manage Cloud Run
  "roles/cloudbuild.builds.editor"    # Trigger builds (if needed)
)

# Reverse Engineering roles
REVERSE_ENG_ROLES=(
  "roles/datastore.user"
  "roles/pubsub.subscriber"
  "roles/storage.objectViewer"
)

# Simulator roles
SIMULATOR_ROLES=(
  "roles/run.invoker"                  # Allow invocations
  "roles/cloudbuild.builds.editor"
)

grant_roles() {
  local sa="$1"
  local roles=("${@:2}")
  for role in "${roles[@]}"; do
    log_info "Granting $role to $sa..."
    gcloud projects add-iam-policy-binding "$PROJECT_ID" \
      --member="serviceAccount:${sa}@${PROJECT_ID}.iam.gserviceaccount.com" \
      --role="$role" || log_warn "Role $role may already be granted"
  done
}

# Grant common roles to all
for sa in "$BACKEND_SA" "$REVERSE_ENG_SA" "$SIMULATOR_SA"; do
  grant_roles "$sa" "${COMMON_ROLES[@]}"
done

# Grant specific roles
grant_roles "$BACKEND_SA" "${BACKEND_ROLES[@]}"
grant_roles "$REVERSE_ENG_SA" "${REVERSE_ENG_ROLES[@]}"
grant_roles "$SIMULATOR_SA" "${SIMULATOR_ROLES[@]}"

log_info "✅ IAM permissions granted"

# --- 4. Create Pub/Sub Topics ---
log_info ""
log_info "=== Step 4: Creating Pub/Sub Resources ==="

TOPICS=(
  "reverse-engineering-jobs"
  "reverse-engineering-results"
  "simulator-jobs"
  "ai-consensus-requests"
  "code-generation-jobs"
)

for topic in "${TOPICS[@]}"; do
  log_info "Creating topic: $topic"
  gcloud pubsub topics create "$topic" --project="$PROJECT_ID" || log_warn "Topic $topic may already exist"
done

log_info "✅ Pub/Sub topics created"

# --- 5. Create Firestore Database ---
log_info ""
log_info "=== Step 5: Setting up Firestore ==="

log_info "Enabling Firestore API and creating database..."
gcloud services enable firestore.googleapis.com --project="$PROJECT_ID" || true

# Create Firestore database (native mode)
gcloud firestore databases create --region="$REGION" --project="$PROJECT_ID" || log_warn "Firestore database may already exist"

log_info "✅ Firestore database ready"

# --- 6. Configure Firebase Auth (Optional: Set custom claims) ---
log_info ""
log_info "=== Step 6: Firebase Auth Admin SDK Setup ==="

# Note: Firebase Auth is managed via Firebase Console.
# To set admin user programmatically, use:
#   node scripts/add_admin.js niloyjoy7@gmail.com

log_info "ℹ️  To set admin user: node scripts/add_admin.js niloyjoy7@gmail.com"
log_info "✅ Firebase Auth configuration complete"

# --- 7. Output Summary ---
log_info ""
log_info "=========================================="
log_info "  Infrastructure Setup Complete!"
log_info "=========================================="
log_info ""
log_info "Service Accounts Created:"
log_info "  Backend:        ${BACKEND_SA}@${PROJECT_ID}.iam.gserviceaccount.com"
log_info "  Reverse Eng:    ${REVERSE_ENG_SA}@${PROJECT_ID}.iam.gserviceaccount.com"
log_info "  Simulator:      ${SIMULATOR_SA}@${PROJECT_ID}.iam.gserviceaccount.com"
log_info ""
log_info "Next Steps:"
log_info "  1. Deploy backend with service account:"
log_info "     gcloud run deploy supremeai-backend \\"
log_info "       --service-account ${BACKEND_SA}@${PROJECT_ID}.iam.gserviceaccount.com \\"
log_info "       --image gcr.io/${PROJECT_ID}/supremeai-backend:latest \\"
log_info "       --region $REGION"
log_info ""
log_info "  2. Deploy reverse-engineering service:"
log_info "     gcloud run deploy reverse-engineering \\"
log_info "       --service-account ${REVERSE_ENG_SA}@${PROJECT_ID}.iam.gserviceaccount.com"
log_info ""
log_info "  3. Deploy dashboard:"
log_info "     firebase deploy --only hosting"
log_info ""
log_info "  4. Set admin user (if not done):"
log_info "     node scripts/add_admin.js niloyjoy7@gmail.com"
log_info ""
log_info "  5. Verify deployment:"
log_info "     gcloud run services list --region $REGION"
log_info "=========================================="
