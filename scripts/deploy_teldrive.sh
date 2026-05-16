#!/bin/bash

# Teldrive Deployment Script for SupremeAI
# This script deploys Teldrive to Google Cloud Run using the provided credentials.

PROJECT_ID="supremeai-a"
REGION="us-central1"
SERVICE_NAME="teldrive"
IMAGE="gcr.io/supremeai-a/teldrive:latest"

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
    echo "Usage: $0 <API_ID> <API_HASH> <BOT_TOKEN>"
    exit 1
fi

API_ID=$1
API_HASH=$2
BOT_TOKEN=$3
JWT_SECRET="903924ce71426c6fc666e3d5c3736cebec27ec07fe511f9da5fab0532581ba01"
# Cloud Run unix socket path for Cloud SQL
# Supabase Connection String
DB_URL="postgres://postgres:njel.com.bd123@db.knqoqjnwpgmrsezgqgpg.supabase.co:5432/postgres"

set -e


echo "🏗️ Building Teldrive custom image..."
# Temporarily rename Dockerfile.teldrive to Dockerfile for gcloud builds submit
mv scripts/Dockerfile.teldrive scripts/Dockerfile
(cd scripts && gcloud builds submit . --tag $IMAGE --project $PROJECT_ID)
mv scripts/Dockerfile scripts/Dockerfile.teldrive

echo "🚀 Deploying Teldrive to Cloud Run..."




gcloud run deploy $SERVICE_NAME \
  --image $IMAGE \
  --project $PROJECT_ID \
  --region $REGION \
  --set-env-vars "DB_DATA_SOURCE=$DB_URL,JWT_SECRET=$JWT_SECRET,TG_APP_ID=$API_ID,TG_APP_HASH=$API_HASH,TG_BOT_TOKEN=$BOT_TOKEN" \
  --allow-unauthenticated \
  --port 8080 \
  --memory 512Mi \
  --timeout 300s \
  --quiet


if [ $? -eq 0 ]; then
    URL=$(gcloud run services describe $SERVICE_NAME --project $PROJECT_ID --region $REGION --format='value(status.url)')
    echo "✅ Teldrive deployed successfully at: $URL"
    echo "URL=$URL"
else
    echo "❌ Deployment failed."
    exit 1
fi

