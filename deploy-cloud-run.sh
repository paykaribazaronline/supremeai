#!/bin/bash
# SupremeAI - Cloud Run Deployment Script (Simplified)
# Usage: bash deploy-cloud-run.sh

set -e

PROJECT_ID="supremeai-a"
SERVICE_NAME="supremeai"
REGION="us-central1"
IMAGE_NAME="gcr.io/$PROJECT_ID/$SERVICE_NAME"

echo "🚀 Deploying SupremeAI to Cloud Run"
echo "===================================="
echo ""

# Step 1: Authenticate with GCP
echo "1️⃣ Authenticating with Google Cloud..."
gcloud auth login
gcloud config set project $PROJECT_ID
echo "✅ Authenticated"
echo ""

# Step 2: Build JAR
echo "2️⃣ Building JAR file..."
./gradlew clean build -x test
echo "✅ JAR built successfully"
echo ""

# Step 3: Build Docker image
echo "3️⃣ Building Docker image..."
docker build -t $IMAGE_NAME:latest -t $IMAGE_NAME:$(date +%s) .
echo "✅ Docker image built"
echo ""

# Step 4: Push to Container Registry
echo "4️⃣ Pushing to Google Container Registry..."
docker push $IMAGE_NAME:latest
echo "✅ Image pushed"
echo ""

# Step 5: Deploy to Cloud Run
echo "5️⃣ Deploying to Cloud Run..."
gcloud run deploy $SERVICE_NAME \
  --image $IMAGE_NAME:latest \
  --platform managed \
  --region $REGION \
  --port 8080 \
  --memory 512Mi \
  --cpu 1 \
  --timeout 3600 \
  --max-instances 100 \
  --allow-unauthenticated \
  --set-env-vars "PORT=8080,FIREBASE_PROJECT_ID=supremeai-a,app.jwtSecret=supremeai-secret-key-for-jwt-token-generation-2026"

echo ""
echo "✅ Deployment complete!"
echo ""
echo "Service URL:"
gcloud run services describe $SERVICE_NAME --region $REGION --format 'value(status.url)'
