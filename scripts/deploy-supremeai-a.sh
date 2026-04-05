#!/bin/bash
# SupremeAI Quick Deployment Script for GCP Project: supremeai-a
# Run this script to deploy the backend to Google Cloud Run

set -e

PROJECT_ID="supremeai-a"
REGION="us-central1"
SERVICE_NAME="supremeai"

echo "=================================================="
echo "SupremeAI Backend Deployment to Cloud Run"
echo "=================================================="
echo "Project ID: $PROJECT_ID"
echo "Region: $REGION"
echo "Service: $SERVICE_NAME"
echo ""

# Step 1: Authenticate with Google Cloud
echo "[1/5] Authenticating with Google Cloud..."
gcloud auth login
gcloud config set project $PROJECT_ID

# Step 2: Enable required APIs
echo "[2/5] Enabling Google Cloud APIs..."
gcloud services enable run.googleapis.com cloudbuild.googleapis.com containerregistry.googleapis.com

# Step 3: Build and push Docker image
echo "[3/5] Building Docker image and pushing to Google Container Registry..."
gcloud builds submit --config=cloudbuild.yaml \
  --project=$PROJECT_ID \
  --substitutions="SHORT_SHA=latest"

# Step 4: Check deployment status
echo "[4/5] Checking Cloud Run service status..."
sleep 30  # Wait for deployment to complete

SERVICE_URL=$(gcloud run services describe $SERVICE_NAME \
  --project=$PROJECT_ID \
  --region=$REGION \
  --format='value(status.url)' 2>/dev/null || echo "")

if [ -z "$SERVICE_URL" ]; then
  echo "⚠️  Service may still be deploying. Checking again in 30 seconds..."
  sleep 30
  SERVICE_URL=$(gcloud run services describe $SERVICE_NAME \
    --project=$PROJECT_ID \
    --region=$REGION \
    --format='value(status.url)')
fi

# Step 5: Display results
echo "[5/5] Deployment complete!"
echo ""
echo "=================================================="
echo "✅ Backend Service Deployed Successfully!"
echo "=================================================="
echo ""
echo "Your Cloud Run Service URL:"
echo "$SERVICE_URL"
echo ""
echo "Next Steps:"
echo "1. Copy the URL above"
echo "2. Update flutter_admin_app/lib/config/environment.dart:"
echo "   static const String baseUrl = '$SERVICE_URL';"
echo "3. Rebuild Flutter web:"
echo "   cd flutter_admin_app"
echo "   flutter build web --base-href \"/admin/\" --release"
echo "4. Deploy to Firebase Hosting:"
echo "   firebase deploy --only hosting"
echo ""
echo "Test the admin login:"
echo "URL: https://supremeai-a.web.app/admin/#/login"
echo "Email: \${SUPREMEAI_ADMIN_EMAIL:-admin@supremeai.com}"
echo "Password: <set SUPREMEAI_ADMIN_PASSWORD in your shell>"
echo ""

# Health check
echo "Testing backend health..."
HEALTH=$(curl -s $SERVICE_URL/actuator/health | grep -o '"status":"UP"' || echo "")
if [ -n "$HEALTH" ]; then
  echo "✅ Backend is healthy and responding to requests!"
else
  echo "⚠️  Backend may still be starting. Try the health check in 30 seconds:"
  echo "curl $SERVICE_URL/actuator/health"
fi
