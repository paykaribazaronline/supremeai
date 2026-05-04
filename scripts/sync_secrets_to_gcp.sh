#!/bin/bash

# Project ID
PROJECT_ID="supremeai-a"

# Function to create or update secret
sync_secret() {
    local secret_name=$1
    local secret_value=$2

    if [ -z "$secret_value" ] || [ "$secret_value" == "sk-your-openai-key-here" ] || [ "$secret_value" == "your-ant-your-key-here" ]; then
        echo "⚠️ Skipping $secret_name (placeholder or empty value)"
        return
    fi

    # Check if secret exists
    if gcloud secrets describe "$secret_name" --project="$PROJECT_ID" &>/dev/null; then
        echo "🔄 Updating secret: $secret_name"
        echo -n "$secret_value" | gcloud secrets versions add "$secret_name" --data-file=- --project="$PROJECT_ID"
    else
        echo "➕ Creating secret: $secret_name"
        echo -n "$secret_value" | gcloud secrets create "$secret_name" --replication-policy="automatic" --data-file=- --project="$PROJECT_ID"
    fi
}

# Extract values from .env
# Note: This is a simple parser, might need adjustment if .env has complex values
get_env_val() {
    grep "^$1=" .env | cut -d'=' -f2- | tr -d '\r'
}

sync_secret "openai-api-key" "$(get_env_val OPENAI_API_KEY)"
sync_secret "anthropic-api-key" "$(get_env_val ANTHROPIC_API_KEY)"
sync_secret "gemini-api-key" "$(get_env_val GEMINI_API_KEY)"
sync_secret "deepseek-api-key" "$(get_env_val DEEPSEEK_API_KEY)"
sync_secret "mistral-api-key" "$(get_env_val MISTRAL_API_KEY)"
sync_secret "huggingface-api-key" "$(get_env_val HUGGINGFACE_API_KEY)"
sync_secret "kimi-api-key" "$(get_env_val KIMI_API_KEY)"
sync_secret "maps-api-key" "$(get_env_val MAPS_API_KEY)"
sync_secret "stepfun-api-key" "$(get_env_val STEPFUN_API_KEY)"
sync_secret "codegeex-api-key" "$(get_env_val CODEGEEX_API_KEY)"
sync_secret "continue-dev-api-key" "$(get_env_val CONTINUE_DEV_API_KEY)"
sync_secret "kilo-claw-api-key" "$(get_env_val KILO_CLAW_API_KEY)"
sync_secret "api-encryption-key" "$(get_env_val API_ENCRYPTION_KEY)"

echo "✅ Secret synchronization complete!"
