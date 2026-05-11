#!/bin/bash
PROJECT_ID="supremeai-a"
REGION="us-central1"
REPO_NAME="supreme-ai-models"

# 1. Create Artifact Registry repository if it doesn't exist
gcloud artifacts repositories create $REPO_NAME --repository-format=docker --location=$REGION --project=$PROJECT_ID || true

# 2. Function to build and deploy
deploy_model() {
    MODEL_ALIAS=$1
    MODEL_FULL_NAME=$2
    IMAGE_TAG="$REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$MODEL_ALIAS:latest"

    echo "----------------------------------------------------"
    echo "Processing Model: $MODEL_ALIAS ($MODEL_FULL_NAME)"
    echo "----------------------------------------------------"

    # Submit Build to Google Cloud Build (Fast & Serverless)
    gcloud builds submit . --config=cloudbuild.yaml --substitutions=_MODEL_NAME="$MODEL_FULL_NAME",_IMAGE_TAG="$IMAGE_TAG" --project=$PROJECT_ID
    
    # Deploy to Cloud Run
    gcloud run deploy "$MODEL_ALIAS" \
        --image="$IMAGE_TAG" \
        --platform=managed \
        --region=$REGION \
        --allow-unauthenticated \
        --memory=4Gi \
        --cpu=2 \
        --min-instances=0 \
        --max-instances=1 \
        --port=8080 \
        --project=$PROJECT_ID
}

# Define models to deploy
# deploy_model "supreme-ai-qwen-coder" "qwen2.5-coder:7b"
# deploy_model "supreme-ai-llama-3-1" "llama3.1:8b"
# deploy_model "supreme-ai-deepseek-pro" "deepseek-coder-v2"
# deploy_model "supreme-ai-phi-3" "phi3"
# deploy_model "supreme-ai-nomic-embed" "nomic-embed-text"

