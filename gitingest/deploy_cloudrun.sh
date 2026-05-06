# Gitingest - Google Cloud Run Deployment
# Deploy Python FastAPI app to Cloud Run

# Build and deploy script for Gitingest
# Run with: ./deploy_gitingest_cloudrun.sh

set -e

PROJECT_ID="supremeai-$(date +%s)"
REGION="us-central1"
SERVICE_NAME="gitingest"
IMAGE="gcr.io/${PROJECT_ID}/gitingest:latest"

echo "Building Gitingest Docker image..."
docker build -t ${IMAGE} .

echo "Configuring Docker to use gcloud..."
gcloud auth configure-docker

echo "Pushing image to Container Registry..."
docker push ${IMAGE}

echo "Deploying to Cloud Run..."
gcloud run deploy ${SERVICE_NAME} \
  --image ${IMAGE} \
  --region ${REGION} \
  --platform managed \
  --allow-unauthenticated \
  --set-env-vars="GITHUB_TOKEN=${GITHUB_TOKEN},LOG_FORMAT=json,LOG_LEVEL=INFO,S3_ENABLED=false"

echo "Deployment complete!"
echo "Service URL: $(gcloud run services describe ${SERVICE_NAME} --region=${REGION} --format='value(status.url)')"
