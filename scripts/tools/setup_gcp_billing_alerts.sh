#!/bin/bash
# ==============================================================================
# GCP Billing Alerts Configuration Script (S-04 Compliance)
# Automatically configures $10, $50, and $100 budget alert thresholds on GCP.
# ==============================================================================

# Default Project ID
PROJECT_ID="supremeai-a"

echo "🛡️ Starting GCP Billing Alerts configuration (S-04 Audit)..."

# 1. Verify gcloud CLI is authenticated and active
if ! command -v gcloud &> /dev/null; then
    echo "❌ Error: gcloud CLI is not installed. Please install it first."
    exit 1
fi

# 2. Get active GCP Project ID
CURRENT_PROJECT=$(gcloud config get-value project 2>/dev/null)
if [ -n "$CURRENT_PROJECT" ]; then
    PROJECT_ID=$CURRENT_PROJECT
fi
echo "📍 Using GCP Project ID: $PROJECT_ID"

# 3. Retrieve Billing Account associated with the project
echo "🔍 Retrieving billing account linked to project '$PROJECT_ID'..."
BILLING_ACCOUNT=$(gcloud billing projects describe "$PROJECT_ID" --format="value(billingAccountName)" 2>/dev/null)

if [ -z "$BILLING_ACCOUNT" ] || [ "$BILLING_ACCOUNT" == "null" ]; then
    echo "⚠️ Warning: Could not auto-detect a linked billing account. Please make sure billing is enabled."
    echo "Please select a billing account from the list below:"
    gcloud billing accounts list
    echo "Enter your Billing Account ID (e.g., 0X0X0X-0X0X0X-0X0X0X):"
    read -r BILLING_ACCOUNT
fi

if [ -z "$BILLING_ACCOUNT" ]; then
    echo "❌ Error: No billing account specified. Aborting budget alert setup."
    exit 1
fi

echo "✅ Linked Billing Account found: $BILLING_ACCOUNT"

# 4. Check if the budget already exists
BUDGET_NAME="SupremeAI-Core-Budget"
echo "🔍 Checking for existing budget configuration: $BUDGET_NAME..."
EXISTING_BUDGET=$(gcloud billing budgets list --billing-account="$BILLING_ACCOUNT" --filter="displayName=$BUDGET_NAME" --format="value(name)" 2>/dev/null)

if [ -n "$EXISTING_BUDGET" ]; then
    echo "🔄 Existing budget alert found ($BUDGET_NAME). Re-creating to update thresholds to $10, $50, $100..."
    gcloud billing budgets delete "$EXISTING_BUDGET" --quiet 2>/dev/null
fi

# 5. Create GCP Billing Budget Alert with $10, $50, and $100 thresholds
# Set budget target amount to $100 USD
# Threshold Rules:
# - 10% ($10 threshold) -> triggers alert
# - 50% ($50 threshold) -> triggers alert
# - 100% ($100 threshold) -> triggers alert
echo "➕ Creating budget alert ($BUDGET_NAME) with $10, $50, and $100 triggers..."
gcloud billing budgets create \
    --billing-account="$BILLING_ACCOUNT" \
    --display-name="$BUDGET_NAME" \
    --budget-amount="100USD" \
    --threshold-rule=percent=0.1,basis=current-spend \
    --threshold-rule=percent=0.5,basis=current-spend \
    --threshold-rule=percent=1.0,basis=current-spend \
    --all-updates-rule-pubsub-topic="" # optional: pubsub integration can be wired here

if [ $? -eq 0 ]; then
    echo "✅ Success! GCP Billing Alerts configured successfully:"
    echo "   - Budget Target: $100 USD"
    echo "   - Alert 1: $10 (10% of budget)"
    echo "   - Alert 2: $50 (50% of budget)"
    echo "   - Alert 3: $100 (100% of budget)"
else
    echo "❌ Failed to create budget. Please verify your billing administrative permissions in GCP IAM."
    exit 1
fi
