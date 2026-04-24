package com.supremeai.service;

import com.supremeai.cost.CloudCostCollector;
import org.springframework.stereotype.Service;

@Service
public class BudgetManager {

    private final double MONTHLY_BUDGET = 1000.0;






        private final CloudCostCollector costCollector;
    // Map to track cost per user
    private final java.util.Map<String, Double> userCosts = new java.util.concurrent.ConcurrentHashMap<>();

    public BudgetManager(CloudCostCollector costCollector) {
        this.costCollector = costCollector;
    }

    public void recordUserCost(String userId, double cost) {
        userCosts.merge(userId, cost, Double::sum);
    }

    public double getUserCost(String userId) {
        return userCosts.getOrDefault(userId, 0.0);
    }
    
    public java.util.Map<String, Double> getAllUserCosts() {
        return new java.util.HashMap<>(userCosts);
    }

    public boolean isWithinBudget() {
        return costCollector.calculateTotalCost() <= MONTHLY_BUDGET;
    }

    public String generateAlerts() {
        double currentCost = costCollector.calculateTotalCost();
        double usagePercentage = (currentCost / MONTHLY_BUDGET) * 100;

        if (usagePercentage > 100) {
            return "CRITICAL: Budget exceeded! Current usage: " + String.format("%.2f", usagePercentage) + "%";
        } else if (usagePercentage > 90) {
            return "WARNING: Approaching budget limit. Current usage: " + String.format("%.2f", usagePercentage) + "%";
        }
        return "INFO: Budget is healthy. Current usage: " + String.format("%.2f", usagePercentage) + "%";
    }
}
