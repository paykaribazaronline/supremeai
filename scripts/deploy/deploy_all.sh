#!/bin/bash
# Complete deployment script for Gitingest & GitReverse
# Usage: ./deploy_all.sh [PROJECT_ID]

set -e

PROJECT_ID=${1:-""}
REGION="us-central1"

if [ -z "$PROJECT_ID" ]; then
    echo "Usage: $0 PROJECT_ID"
    echo "Example: $0 my-supremeai-project"
    exit 1
fi

echo "=== SupremeAI Gitingest & GitReverse Deployment ==="
echo "Project: $PROJECT_ID"
echo "Region: $REGION"
echo ""

# Configure gcloud
gcloud config set project $PROJECT_ID

# Authenticate Docker
gcloud auth configure-docker

# Deploy Gitingest
echo ""
echo ">>> Deploying Gitingest..."
cd gitingest

docker build -t gcr.io/$PROJECT_ID/gitingest:latest .
docker push gcr.io/$PROJECT_ID/gitingest:latest

gcloud run deploy gitingest \
  --image gcr.io/$PROJECT_ID/gitingest:latest \
  --region $REGION \
  --allow-unauthenticated \
  --set-env-vars="LOG_FORMAT=json,LOG_LEVEL=INFO,S3_ENABLED=false"

GITINGEST_URL=$(gcloud run services describe gitingest --region=$REGION --format='value(status.url)')
echo "Gitingest deployed to: $GITINGEST_URL"

cd ..

# Deploy GitReverse
echo ""
echo ">>> Deploying GitReverse..."
cd gitreverse

docker build -t gcr.io/$PROJECT_ID/gitreverse:latest .
docker push gcr.io/$PROJECT_ID/gitreverse:latest

gcloud run deploy gitreverse \
  --image gcr.io/$PROJECT_ID/gitreverse:latest \
  --region $REGION \
  --allow-unauthenticated \
  --set-env-vars="NEXT_PUBLIC_APP_URL=https://${PROJECT_ID}.web.app"

GITREVERSE_URL=$(gcloud run services describe gitreverse --region=$REGION --format='value(status.url)')
echo "GitReverse deployed to: $GITREVERSE_URL"

cd ..

echo ""
echo "=== Deployment Complete! ==="
echo "Gitingest: $GITINGEST_URL"
echo "GitReverse: $GITREVERSE_URL"
echo ""
echo "Next steps:"
echo "1. Set environment variables: gcloud run services update [service] --region=$REGION --set-env-vars=..."
echo "2. Optionally deploy to Firebase Hosting: firebase deploy --only hosting"
echo "3. Monitor logs: gcloud logging read 'resource.type=cloud_run_revision'"
