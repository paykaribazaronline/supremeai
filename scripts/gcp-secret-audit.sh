#!/bin/bash
# SupremeAI GCP Secret Manager Audit
# Checks environment variable fallback configuration

echo "🔍 Auditing Secret Manager configuration..."

# Check if secrets are configured in application.yml
echo "Checking application.yml for env var patterns..."
if grep -q '\${' /home/nazifarabbu/supremeai/src/main/resources/application.yml; then
    echo "✅ Environment variable fallbacks found in application.yml"
    grep -E '\$\{[A-Z_]+[:\?]' /home/nazifarabbu/supremeai/src/main/resources/application.yml | head -20
else
    echo "⚠️ No environment variable patterns found"
fi

# Check for hardcoded secrets (should not exist)
echo ""
echo "Checking for hardcoded secrets..."
if grep -iE '(password|secret|key).*[:=]"[^"$]' /home/nazifarabbu/supremeai/src/main/resources/application.yml | grep -v '\$\{' | grep -v '#'; then
    echo "⚠️ Potential hardcoded secrets detected!"
else
    echo "✅ No hardcoded secrets found"
fi

echo ""
echo "Secret Manager audit complete"