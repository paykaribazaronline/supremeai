package com.supremeai.cost;

import org.springframework.stereotype.Service;

@Service
public class BudgetManager {

    private final double MONTHLY_BUDGET = 1000.0;
    private final CloudCostCollector costCollector;

    public BudgetManager(CloudCostCollector costCollector) {
        this.costCollector = costCollector;
    }

    public boolean isWithinBudget() {
        return costCollector.calculateTotalCost() <= MONTHLY_BUDGET;
    }

    public String generateAlerts() {
        double currentCost = costCollector.calculateTotalCost();
        double usagePercentage = (currentCost / MONTHLY_BUDGET) * 100;

        if (usagePercentage > 100) {
            return "CRITICAL: Budget exceeded! Current usage: " + usagePercentage + "%";
        } else if (usagePercentage > 90) {
            return "WARNING: Approaching budget limit. Current usage: " + usagePercentage + "%";
        }
        return "INFO: Budget is healthy. Current usage: " + usagePercentage + "%";
    }
}
