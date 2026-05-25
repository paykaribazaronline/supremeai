#!/bin/bash
# GCP Billing Alerts Setup
# Creates budget alerts at $10, $50, $100 thresholds

PROJECT_ID="supremeai-a"
BUDGET_THRESHOLDS=("10" "50" "100")

echo "Setting up GCP billing alerts for project: $PROJECT_ID"

for threshold in "${BUDGET_THRESHOLDS[@]}"; do
    echo "Creating alert for \$${threshold} budget..."
    
    gcloud alpha billing budgets create \
        --billing-account=BILLING_ACCOUNT_ID \
        --project=$PROJECT_ID \
        --budget-amount=${threshold} \
        --threshold-rule=type=COMPUTD spend-threshold=${threshold} \
        --display-name="Budget Alert \$${threshold}" \
        --budget-filter="services=services/6F02-500-00" \
        --notification-email=admin@supremeai.app \
        --notification-channel=email \
        --enable-logging
    
    echo "Alert created for \$${threshold}"
done

echo "All billing alerts configured successfully!"
echo "Thresholds: $10, $50, $100"
