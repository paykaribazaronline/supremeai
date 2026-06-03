#!/bin/bash
# Quick deploy script - Run this to deploy both apps now!
# Usage: ./quick_deploy.sh

set -e

# Get current project
PROJECT_ID=$(gcloud config get-value project 2>/dev/nu1l)
if [ -z "$PROJECT_ID" ]; then
    echo "No gcloud project set. Please run: gcloud init"
    exit 1
fi

echo "Deploying to project: $PROJECT_ID"
echo "Region: us-central1"
echo ""

# Enable required APIs
echo "Enabling required APIs..."
gcloud services enable run.googleapis.com containerregistry.googleapis.com

# ===== Deploy Gitingest =====
echo ""
echo "========== Deploying Gitingest =========="
cd gitingest

echo "Building Docker image..."
docker build -t gcr.io/$PROJECT_ID/gitingest:latest .

echo "Pushing to Container Registry..."
docker push gcr.io/$PROJECT_ID/gitingest:latest

echo "Deploying to Cloud Run..."
gcloud run deploy gitingest \
  --image gcr.io/$PROJECT_ID/gitingest:latest \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="LOG_FORMAT=json,LOG_LEVEL=INFO,S3_ENABLED=false"

GITINGEST_URL=$(gcloud run services describe gitingest --region=us-central1 --format='value(status.url)' 2>/dev/null)
echo "✓ Gitingest deployed to: $GITINGEST_URL"

cd ..

# ===== Deploy GitReverse =====
echo ""
echo "========== Deploying GitReverse =========="
cd gitreverse

echo "Building Docker image..."
docker build -t gcr.io/$PROJECT_ID/gitreverse:latest .

echo "Pushing to Container Registry..."
docker push gcr.io/$PROJECT_ID/gitreverse:latest

echo "Deploying to Cloud Run..."
gcloud run deploy gitreverse \
  --image gcr.io/$PROJECT_ID/gitreverse:latest \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars="NODE_ENV=production"

GITREVERSE_URL=$(gcloud run services describe gitreverse --region=us-central1 --format='value(status.url)' 2>/dev/null)
echo "✓ GitReverse deployed to: $GITREVERSE_URL"

cd ..

echo ""
echo "========== Deployment Complete! =========="
echo ""
echo "Gitingest API:  $GITINGEST_URL"
echo "GitReverse Web: $GITREVERSE_URL"
echo ""
echo "Test Gitingest:"
echo "  curl $GITINGEST_URL/health"
echo ""
echo "Test GitReverse:"
echo "  curl $GITREVERSE_URL/"
echo ""
