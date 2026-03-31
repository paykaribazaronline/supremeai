package org.example.controller;

import org.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Phase 7-10: Multi-Agent REST API Controller
 * Exposes all new agents (7 generators, 3 security, 3 cost, 4 evolution)
 */
@RestController
@RequestMapping("/api/v1/agents")
public class AgentOrchestrationController {

    @Autowired
    private iOSGeneratorAgent iosAgent;
    @Autowired
    private WebGeneratorAgent webAgent;
    @Autowired
    private DesktopGeneratorAgent desktopAgent;
    @Autowired
    private PlayStorePublisherAgent playStoreAgent;
    @Autowired
    private AppStorePublisherAgent appStoreAgent;

    @Autowired
    private AlphaSecurityAgent alphaAgent;
    @Autowired
    private BetaComplianceAgent betaAgent;
    @Autowired
    private GammaPrivacyAgent gammaAgent;

    @Autowired
    private DeltaCostAgent deltaAgent;
    @Autowired
    private EpsilonOptimizerAgent epsilonAgent;
    @Autowired
    private ZetaFinanceAgent zetaAgent;

    @Autowired
    private EtaMetaAgent etaAgent;
    @Autowired
    private ThetaLearningAgent thetaAgent;
    @Autowired
    private IotaKnowledgeAgent iotaAgent;
    @Autowired
    private KappaEvolutionAgent kappaAgent;

    @Autowired
    private FirebaseService firebaseService;

    // ==================== PHASE 7: Generators & Publishers ====================

    @PostMapping("/phase7/generate-ios")
    public ResponseEntity<Map<String, Object>> generateiOS(@RequestBody Map<String, Object> request) {
        String projectId = (String) request.get("projectId");
        return ResponseEntity.ok(iosAgent.generateiOSApp(projectId, request));
    }

    @PostMapping("/phase7/generate-web")
    public ResponseEntity<Map<String, Object>> generateWeb(@RequestBody Map<String, Object> request) {
        String projectId = (String) request.get("projectId");
        String framework = (String) request.getOrDefault("framework", "react");
        return ResponseEntity.ok(webAgent.generateWebApp(projectId, framework));
    }

    @PostMapping("/phase7/generate-desktop")
    public ResponseEntity<Map<String, Object>> generateDesktop(@RequestBody Map<String, Object> request) {
        String projectId = (String) request.get("projectId");
        String framework = (String) request.getOrDefault("framework", "tauri");
        return ResponseEntity.ok(desktopAgent.generateDesktopApp(projectId, framework));
    }

    @PostMapping("/phase7/publish-playstore")
    public ResponseEntity<Map<String, Object>> publishPlayStore(@RequestBody Map<String, Object> request) {
        String appId = (String) request.get("appId");
        String buildPath = (String) request.get("buildPath");
        return ResponseEntity.ok(playStoreAgent.publishToPlayStore(appId, buildPath));
    }

    @PostMapping("/phase7/publish-appstore")
    public ResponseEntity<Map<String, Object>> publishAppStore(@RequestBody Map<String, Object> request) {
        String appId = (String) request.get("appId");
        String buildPath = (String) request.get("buildPath");
        return ResponseEntity.ok(appStoreAgent.publishToAppStore(appId, buildPath));
    }

    // ==================== PHASE 8: Security & Compliance ====================

    @PostMapping("/phase8/scan-security")
    public ResponseEntity<Map<String, Object>> scanSecurity(@RequestBody Map<String, Object> request) {
        String projectId = (String) request.get("projectId");
        return ResponseEntity.ok(alphaAgent.scanForVulnerabilities(projectId));
    }

    @PostMapping("/phase8/validate-compliance")
    public ResponseEntity<Map<String, Object>> validateCompliance(@RequestBody Map<String, Object> request) {
        String projectId = (String) request.get("projectId");
        return ResponseEntity.ok(betaAgent.validateCompliance(projectId));
    }

    @PostMapping("/phase8/analyze-privacy")
    public ResponseEntity<Map<String, Object>> analyzePrivacy(@RequestBody Map<String, Object> request) {
        String projectId = (String) request.get("projectId");
        return ResponseEntity.ok(gammaAgent.analyzePrivacy(projectId));
    }

    // ==================== PHASE 9: Cost Intelligence ====================

    @GetMapping("/phase9/track-costs")
    public ResponseEntity<Map<String, Object>> trackCosts() {
        Map<String, Object> report = deltaAgent.trackCosts();
        firebaseService.saveCostReport(report);
        
        // Check for budget alerts
        double currentSpend = (double) report.getOrDefault("total_monthly_spend", 0.0);
        if (firebaseService.isBudgetExceeded(currentSpend)) {
            report.put("ALERT", "BUDGET_EXCEEDED");
            firebaseService.sendNotification("admin", "Budget Alert", "Monthly spend has exceeded limits!", "URGENT");
        }
        
        return ResponseEntity.ok(report);
    }

    @PostMapping("/phase9/optimize-resources")
    public ResponseEntity<Map<String, Object>> optimizeResources(@RequestBody Map<String, Object> request) {
        Map<String, Object> recommendations = epsilonAgent.optimizeResources();
        firebaseService.saveOptimizationRecommendations(recommendations);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/phase9/plan-budget")
    public ResponseEntity<Map<String, Object>> planBudget(@RequestBody Map<String, Object> request) {
        Map<String, Object> budgetPlan = zetaAgent.planBudget();
        firebaseService.saveBudgetPlan(budgetPlan);
        return ResponseEntity.ok(budgetPlan);
    }

    @PostMapping("/phase9/run-scenario")
    public ResponseEntity<Map<String, Object>> runScenario(@RequestBody Map<String, Object> request) {
        String name = (String) request.getOrDefault("scenarioName", "Default Growth");
        double scale = ((Number) request.getOrDefault("scaleFactor", 1.2)).doubleValue();
        return ResponseEntity.ok(zetaAgent.runScenario(name, scale));
    }

    // ==================== PHASE 10: Self-Improvement ====================

    @PostMapping("/phase10/evolve-agents")
    public ResponseEntity<Map<String, Object>> evolveAgents(@RequestBody Map<String, Object> request) {
        Map<String, Object> report = etaAgent.evolveAgents();
        firebaseService.saveEvolutionReport(report);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/phase10/learn-patterns")
    public ResponseEntity<Map<String, Object>> learnPatterns(@RequestBody Map<String, Object> request) {
        Map<String, Object> report = thetaAgent.learnPatterns();
        if (report.containsKey("top_patterns")) {
            List<Map<String, Object>> patterns = (List<Map<String, Object>>) report.get("top_patterns");
            patterns.forEach(firebaseService::saveLearnedPattern);
        }
        return ResponseEntity.ok(report);
    }

    @GetMapping("/phase10/knowledge-base")
    public ResponseEntity<Map<String, Object>> manageKnowledge() {
        return ResponseEntity.ok(iotaAgent.manageKnowledge());
    }

    @PostMapping("/phase10/evolve-consensus")
    public ResponseEntity<Map<String, Object>> evolveConsensus(@RequestBody Map<String, Object> request) {
        Map<String, Object> report = kappaAgent.evolveConsensus();
        // If a variant is ready for promotion, we could automatically update the config
        if (report.containsKey("promotion_status")) {
             Map<String, Object> promotion = (Map<String, Object>) report.get("promotion_status");
             if ("READY_FOR_PROMOTION".equals(promotion.get("status"))) {
                 // Logic to update main_config via firebaseService
                 Map<String, Object> newConfig = new HashMap<>();
                 newConfig.put("active_variant", promotion.get("pending_promotion"));
                 firebaseService.updateActiveSystemConfig(newConfig);
             }
        }
        return ResponseEntity.ok(report);
    }

    // ==================== Status & Health ====================

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("phase7_agents", 6);
        status.put("phase8_agents", 3);
        status.put("phase9_agents", 3);
        status.put("phase10_agents", 4);
        status.put("total_agents", 20);
        status.put("status", "operational");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("endpoints", 20);
        health.put("version", "10.0-Self-Improvement-Complete");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
