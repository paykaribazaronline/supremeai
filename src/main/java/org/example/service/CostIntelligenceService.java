package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * Orchestrator for Phase 9 Cost Intelligence.
 * Periodically triggers cost tracking and optimization analysis.
 */
@Service
public class CostIntelligenceService {
    private static final Logger logger = LoggerFactory.getLogger(CostIntelligenceService.class);

    @Autowired
    private DeltaCostAgent deltaAgent;
    @Autowired
    private EpsilonOptimizerAgent epsilonAgent;
    @Autowired
    private ZetaFinanceAgent zetaAgent;
    @Autowired
    private FirebaseService firebaseService;

    /**
     * Automatically track costs every 6 hours
     */
    @Scheduled(fixedRate = 21600000) 
    public void performAutomatedAudit() {
        logger.info("🕒 Scheduled Cost Audit starting...");
        
        // 1. Track current costs
        Map<String, Object> costs = deltaAgent.trackCosts();
        firebaseService.saveCostReport(costs);
        
        // 2. Check for budget alerts
        double currentSpend = (double) costs.getOrDefault("total_monthly_spend", 0.0);
        if (firebaseService.isBudgetExceeded(currentSpend)) {
            logger.warn("🚨 BUDGET EXCEEDED: ${}", currentSpend);
            firebaseService.sendNotification("admin", "Budget Alert", 
                "Current spend $" + currentSpend + " exceeds allocated budget!", "CRITICAL");
        }

        // 3. Generate optimizations if spend is high
        if (currentSpend > 1000) {
            Map<String, Object> recommendations = epsilonAgent.optimizeResources();
            firebaseService.saveOptimizationRecommendations(recommendations);
            logger.info("✅ Optimization recommendations generated and saved.");
        }
        
        logger.info("✓ Scheduled Cost Audit complete.");
    }

    public Map<String, Object> getFullCostIntelligencePackage() {
        Map<String, Object> fullReport = new LinkedHashMap<>();
        fullReport.put("current_costs", deltaAgent.trackCosts());
        fullReport.put("optimizations", epsilonAgent.optimizeResources());
        fullReport.put("budget_plan", zetaAgent.planBudget());
        fullReport.put("generated_at", System.currentTimeMillis());
        return fullReport;
    }
}
