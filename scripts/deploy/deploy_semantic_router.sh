#!/bin/bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../../" && pwd)"
SEMANTIC_DIR="$PROJECT_ROOT/microservices/semantic_router"

echo "🚀 Deploying Semantic Router to Google Cloud Run..."

if [ ! -d "$SEMANTIC_DIR" ]; then
  echo "⚠️  Semantic Router directory not found at $SEMANTIC_DIR — skipping"
  exit 0
fi

cd "$SEMANTIC_DIR"

if ! command -v gcloud >/dev/null 2>&1; then
  echo "⚠️  gcloud CLI not found — skipping Cloud Run deploy"
  exit 0
fi

gcloud run deploy supremeai-semantic-router \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --max-instances 10

echo "✅ Deployment Triggered successfully!"