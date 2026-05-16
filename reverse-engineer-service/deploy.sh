#!/bin/bash
set -e

PROJECT_ID="${PROJECT_ID:-supremeai-project}"
REGION="${REGION:-us-central1}"
SERVICE_NAME="reverse-engineer-service"
IMAGE="gcr.io/${PROJECT_ID}/${SERVICE_NAME}:latest"

# Build Docker image
echo "Building Docker image..."
docker build -t "${IMAGE}" .

# Push to GCR
echo "Pushing to Google Container Registry..."
docker push "${IMAGE}"

# Deploy to Cloud Run
echo "Deploying to Cloud Run..."
gcloud run deploy "${SERVICE_NAME}" \
  --image "${IMAGE}" \
  --region "${REGION}" \
  --platform managed \
  --allow-unauthenticated \
  --set-env-vars "GOOGLE_APPLICATION_CREDENTIALS=/var/run/secrets/cloud.json" \
  --memory "2Gi" \
  --cpu "1"

echo "Deployment complete."
