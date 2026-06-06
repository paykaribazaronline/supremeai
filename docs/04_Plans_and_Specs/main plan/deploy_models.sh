#!/bin/bash

# SupremeAI Hybrid Core Deployment Script
# Automates the deployment of supporting infrastructure for Godmode 3 and Tiny AI connectors.

PROJECT_ID=$(gcloud config get-value project)
REGION="us-central1"

echo "🚀 Starting SupremeAI Hybrid Core Infrastructure Deployment for Project: $PROJECT_ID"

# Function to deploy a model
deploy_model() {
    local name=$1
    local model_tag=$2
    local cpu=$3
    local memory=$4
    local min_instances=$5

    echo "----------------------------------------------------------"
    echo "📦 Deploying $name ($model_tag)..."
    
    gcloud run deploy "$name" \
      --image=ollama/ollama \
      --platform=managed \
      --region="$REGION" \
      --memory="$memory" \
      --cpu="$cpu" \
      --timeout=3600 \
      --no-allow-unauthenticated \
      --set-env-vars=OLLAMA_MODEL="$model_tag" \
      --min-instances="$min_instances" \
      --max-instances=5 \
      --quiet
}

# 1. Godmode 3 Browser Engine (Stateful Playwright)
# This service powers browser-based information retrieval and automation
deploy_model "engine-godmode-3" "supremeai/playwright-service:latest" "2" "4Gi" 0

# 2. Tiny AI Management Service
# While Tiny AI runs on-device (SuperFly), this service handles weight synchronization
deploy_model "service-tiny-sync" "supremeai/tiny-sync:latest" "1" "2Gi" 0

echo "----------------------------------------------------------"
echo "✅ Deployment Complete!"
echo "Hybrid Core infrastructure is ready. Claude Code CLI should be configured locally."
echo "On-device Tiny AI (SuperFly) will sync with the tiny-sync service automatically."