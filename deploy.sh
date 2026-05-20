#!/bin/bash
# SupremeAI Unified Deployment Script
# Deploys Backend (GCP Cloud Run), Microservices, and Frontend (Firebase Hosting)

set -e

# Colors
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
BACKEND_SERVICE="supremeai-backend"
REVERSE_ENG_SERVICE="reverse-engineering"
SIMULATOR_SERVICE="simulator-runtime"

BACKEND_IMAGE="gcr.io/${PROJECT_ID}/${BACKEND_SERVICE}:latest"
REVERSE_ENG_IMAGE="gcr.io/${PROJECT_ID}/${REVERSE_ENG_SERVICE}:latest"
SIMULATOR_IMAGE="gcr.io/${PROJECT_ID}/${SIMULATOR_SERVICE}:latest"

# Load environment variables from root .env if it exists
if [ -f ".env" ]; then
    log_info "Loading global environment variables from .env"
    set -a
    source .env
    set +a
fi

log_info "=== SupremeAI Deployment Started ==="
log_info "Project: $PROJECT_ID, Region: $REGION"

# --- Pre-flight Checks ---
command -v gcloud >/dev/null 2>&1 || { log_error "gcloud CLI not found."; exit 1; }
command -v firebase >/dev/null 2>&1 || { log_error "Firebase CLI not found."; exit 1; }
command -v node >/dev/null 2>&1 || { log_error "Node.js not found."; exit 1; }

# Step 1: Build Dashboard
log_info "--- Step 1: Building Admin Dashboard ---"
if [ -d "dashboard" ]; then
    cd dashboard
    [ ! -d "node_modules" ] && npm ci
    npm run build
    
    log_info "Staging dashboard build to public/admin/..."
    rm -rf ../public/admin/*
    mkdir -p ../public/admin
    cp -r dist/* ../public/admin/
    
    log_info "Syncing dashboard build to Spring Boot static resources..."
    rm -rf ../src/main/resources/static/*
    mkdir -p ../src/main/resources/static
    cp -r dist/* ../src/main/resources/static/
    
    cd ..
    log_info "✅ Dashboard built and staged to public/admin/ & src/main/resources/static/"
else
    log_warn "Dashboard directory not found, skipping..."
fi

# Step 2: Build Backend JAR
log_info "--- Step 2: Building Spring Boot Backend ---"
./gradlew clean build -x test
cp build/libs/*.jar ./app.jar
log_info "✅ Backend JAR ready"

# Step 3: Build Docker Images via Cloud Build (Optimized)
log_info "--- Step 3: Building Images via Cloud Build ---"

# Build Backend using a minimal staging directory to avoid large uploads
log_info "Building Backend (Staged)..."
STAGING_DIR="temp_build_staging"
mkdir -p "$STAGING_DIR"
cp app.jar "$STAGING_DIR/"
cp Dockerfile "$STAGING_DIR/"
gcloud builds submit "$STAGING_DIR" --tag "$BACKEND_IMAGE" --project "$PROJECT_ID" --suppress-logs || log_warn "Backend build failed"
rm -rf "$STAGING_DIR"

if [ -d "reverse-engineering" ]; then
    log_info "Building Reverse Engineering..."
    cd reverse-engineering
    gcloud builds submit . --tag "$REVERSE_ENG_IMAGE" --project "$PROJECT_ID" --suppress-logs || log_warn "Rev-Eng build failed"
    cd ..
fi

if [ -d "simulator-runtime" ]; then
    log_info "Building Simulator..."
    cd simulator-runtime
    gcloud builds submit . --tag "$SIMULATOR_IMAGE" --project "$PROJECT_ID" --suppress-logs || log_warn "Simulator build failed"
    cd ..
fi
log_info "✅ All images built via Cloud Build"

# Step 4: Deploy to Cloud Run
log_info "--- Step 4: Deploying to Cloud Run ---"

# Backend
gcloud run deploy "$BACKEND_SERVICE" \
  --image "$BACKEND_IMAGE" \
  --region "$REGION" \
  --allow-unauthenticated \
  --set-env-vars="SPRING_PROFILES_ACTIVE=cloud,FIREBASE_PROJECT_ID=$PROJECT_ID,REDIS_MOCK_ONLINE=true" \
  --cpu 2 --memory 2Gi \
  --min-instances 0 \
  --project "$PROJECT_ID"

# Reverse Engineering
gcloud run deploy "$REVERSE_ENG_SERVICE" \
  --image "$REVERSE_ENG_IMAGE" \
  --region "$REGION" \
  --allow-unauthenticated \
  --set-env-vars="GOOGLE_CLOUD_PROJECT=$PROJECT_ID" \
  --min-instances 0 \
  --project "$PROJECT_ID" || log_warn "Rev-Eng deploy failed"

# Simulator
gcloud run deploy "$SIMULATOR_SERVICE" \
  --image "$SIMULATOR_IMAGE" \
  --region "$REGION" \
  --allow-unauthenticated \
  --min-instances=0 \
  --project "$PROJECT_ID" || log_warn "Simulator deploy failed"

# Step 5: Deploy Firebase Resources
log_info "--- Step 5: Deploying Firebase (Hosting, Functions, Rules) ---"
firebase deploy --project "$PROJECT_ID" --only hosting,functions,firestore,dataconnect

# Step 6: Pub/Sub Configuration
log_info "--- Step 6: Configuring Pub/Sub Subscriptions ---"
BACKEND_URL=$(gcloud run services describe "$BACKEND_SERVICE" --region "$REGION" --format='value(status.url)' --project "$PROJECT_ID")
REVERSE_ENG_URL=$(gcloud run services describe "$REVERSE_ENG_SERVICE" --region "$REGION" --format='value(status.url)' --project "$PROJECT_ID")

if [ -n "$REVERSE_ENG_URL" ]; then
    gcloud pubsub subscriptions create reverse-engineering-jobs-push \
      --topic=reverse-engineering-jobs \
      --push-endpoint="${REVERSE_ENG_URL}/pubsub/push" \
      --project "$PROJECT_ID" || log_warn "Job subscription exists"
fi

if [ -n "$BACKEND_URL" ]; then
    gcloud pubsub subscriptions create reverse-engineering-results-push \
      --topic=reverse-engineering-results \
      --push-endpoint="${BACKEND_URL}/api/pubsub/push" \
      --project "$PROJECT_ID" || log_warn "Result subscription exists"
fi

log_info "=== Deployment Complete! ==="
log_info "Backend URL: $BACKEND_URL"
log_info "Hosting URL: https://$PROJECT_ID.web.app"
