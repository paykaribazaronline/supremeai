package com.supremeai.cost;

import org.springframework.stereotype.Service;

@Service
public class CloudCostCollector {

    public double collectGCPCost() {
        // Mock GCP integration for collecting cost
        return 150.0;
    }

    public double collectAWSCost() {
        // Mock AWS integration
        return 200.0;
    }

    public double calculateTotalCost() {
        return collectGCPCost() + collectAWSCost();
    }
}
