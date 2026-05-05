#!/bin/bash

# SupremeAI AI Provider Health Check Script
# Validates that all required AI providers are accessible
# Used by CI/CD pipelines to ensure system integrity before deployment

set -e

echo "🚀 Starting SupremeAI AI Provider Health Check"
echo "================================================"

GEMINI_API_KEY="${GEMINI_API_KEY:-}"
GROK_API_KEY="${GROK_API_KEY:-}"
OPENAI_API_KEY="${OPENAI_API_KEY:-}"
OLLAMA_HOST="${OLLAMA_HOST:-http://localhost:11434}"

PROVIDERS_CHECKED=0
PROVIDERS_HEALTHY=0
PROVIDERS_WARNING=0

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_provider() {
    local provider=$1
    local api_endpoint=$2
    local api_key=$3
    
    PROVIDERS_CHECKED=$((PROVIDERS_CHECKED + 1))
    
    if [ -z "$api_key" ] && [ "$provider" != "Ollama" ]; then
        echo -e "${YELLOW}⚠️  $provider${NC} - API Key not configured (optional)"
        PROVIDERS_WARNING=$((PROVIDERS_WARNING + 1))
        return 0
    fi
    
    if [ "$provider" = "Ollama" ]; then
        if curl -sf -o /dev/null -w "" "$OLLAMA_HOST/api/tags" 2>/dev/null; then
            echo -e "${GREEN}✅ Ollama${NC} - Healthy (local models available)"
            PROVIDERS_HEALTHY=$((PROVIDERS_HEALTHY + 1))
        else
            echo -e "${YELLOW}⚠️  Ollama${NC} - Not available (acceptable for cloud-first setup)"
            PROVIDERS_WARNING=$((PROVIDERS_WARNING + 1))
        fi
    elif [ "$provider" = "Gemini" ]; then
        if curl -sf -o /dev/null -w "" "https://generativelanguage.googleapis.com/v1beta/models?key=${api_key}" 2>/dev/null; then
            echo -e "${GREEN}✅ Gemini${NC} - Healthy"
            PROVIDERS_HEALTHY=$((PROVIDERS_HEALTHY + 1))
        else
            echo -e "${RED}❌ Gemini${NC} - Unreachable or invalid key"
            return 1
        fi
    elif [ "$provider" = "OpenAI" ]; then
        if curl -sf -H "Authorization: Bearer ${api_key}" \
            "https://api.openai.com/v1/models" 2>/dev/null | grep -q "gpt"; then
            echo -e "${GREEN}✅ OpenAI${NC} - Healthy"
            PROVIDERS_HEALTHY=$((PROVIDERS_HEALTHY + 1))
        else
            echo -e "${RED}❌ OpenAI${NC} - Unreachable or invalid key"
            return 1
        fi
    fi
}

echo ""
echo "📋 Checking AI Providers..."
echo ""

# Check all providers
check_provider "Gemini" "https://generativelanguage.googleapis.com" "$GEMINI_API_KEY" || true
check_provider "OpenAI" "https://api.openai.com" "$OPENAI_API_KEY" || true
check_provider "Ollama" "$OLLAMA_HOST" "" || true

echo ""
echo "================================================"
echo "📊 Health Check Results"
echo "================================================"
echo "Total providers checked: $PROVIDERS_CHECKED"
echo -e "${GREEN}Healthy: $PROVIDERS_HEALTHY${NC}"
echo -e "${YELLOW}Warnings: $PROVIDERS_WARNING${NC}"
echo ""

# Determine exit code
if [ $PROVIDERS_HEALTHY -ge 1 ]; then
    echo -e "${GREEN}✅ AI Provider check PASSED${NC} - At least one provider available"
    exit 0
else
    echo -e "${RED}❌ AI Provider check FAILED${NC} - No providers available"
    exit 1
fi
