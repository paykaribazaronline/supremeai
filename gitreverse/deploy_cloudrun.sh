# GitReverse - Google Cloud Run Deployment
# Deploy Next.js app to Cloud Run

# Build and deploy script for GitReverse
# Run with: ./deploy_gitreverse_cloudrun.sh

set -e

PROJECT_ID="supremeai-$(date +%s)"
REGION="us-central1"
SERVICE_NAME="gitreverse"
IMAGE="gcr.io/${PROJECT_ID}/gitreverse:latest"

echo "Building GitReverse Docker image..."
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
  --set-env-vars="OPENROUTER_API_KEY=${OPENROUTER_API_KEY},GITHUB_TOKEN=${GITHUB_TOKEN},NEXT_PUBLIC_APP_URL=https://${SERVICE_NAME}.a.run.app"

echo "Deployment complete!"
echo "Service URL: $(gcloud run services describe ${SERVICE_NAME} --region=${REGION} --format='value(status.url)')"
