package org.example.controller;

import org.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Phase 7-10: Multi-Agent REST API Controller
 * Exposes all new agents (7 generators, 3 security, 3 cost, 4 evolution)
 * Renamed from AgentOrchestrationController to avoid class name conflict
 * with org.example.agentorchestration.AgentOrchestrationController
 */
@RestController("agentPhasesController")
@RequestMapping("/api/v1/agents")
public class AgentPhasesController {

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
        Map<String, Object> budgetPlan = zetaAgent.forecastFinances();
        firebaseService.saveBudgetPlan(budgetPlan);
        return ResponseEntity.ok(budgetPlan);
    }

    @PostMapping("/phase9/run-scenario")
    public ResponseEntity<Map<String, Object>> runScenario(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(zetaAgent.forecastFinances());
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
        Map<String, Object> report = kappaAgent.orchestrateEvolution();
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

    // ==================== Per-Phase Summary Endpoints ====================

    /** GET /api/v1/agents/phase8/summary */
    @GetMapping("/phase8/summary")
    public ResponseEntity<Map<String, Object>> phase8Summary() {
        Map<String, Object> s = new HashMap<>();
        s.put("phase", "Phase 8 — Security & Compliance");
        s.put("agents", new String[]{"Alpha (Vulnerability Scan)", "Beta (Compliance)", "Gamma (Privacy)"});
        s.put("agentCount", 3);
        s.put("alphaAvailable", alphaAgent != null);
        s.put("betaAvailable", betaAgent != null);
        s.put("gammaAvailable", gammaAgent != null);
        s.put("status", (alphaAgent != null && betaAgent != null && gammaAgent != null) ? "operational" : "partial");
        s.put("capabilities", new String[]{
            "OWASP Top 10 vulnerability scanning",
            "GDPR / CCPA compliance validation",
            "Privacy data flow analysis",
            "Static code security analysis"
        });
        s.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(s);
    }

    /** GET /api/v1/agents/phase9/summary */
    @GetMapping("/phase9/summary")
    public ResponseEntity<Map<String, Object>> phase9Summary() {
        Map<String, Object> s = new HashMap<>();
        s.put("phase", "Phase 9 — Cost Intelligence");
        s.put("agents", new String[]{"Delta (Cost Tracker)", "Epsilon (Optimizer)", "Zeta (Finance)"});
        s.put("agentCount", 3);
        s.put("deltaAvailable", deltaAgent != null);
        s.put("epsilonAvailable", epsilonAgent != null);
        s.put("zetaAvailable", zetaAgent != null);
        s.put("status", (deltaAgent != null && epsilonAgent != null && zetaAgent != null) ? "operational" : "partial");
        s.put("capabilities", new String[]{
            "Real-time cost tracking across cloud providers",
            "AI usage and token cost analysis",
            "Resource utilization optimization",
            "Budget forecasting and scenario planning"
        });
        s.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(s);
    }

    /** GET /api/v1/agents/phase10/summary */
    @GetMapping("/phase10/summary")
    public ResponseEntity<Map<String, Object>> phase10Summary() {
        Map<String, Object> s = new HashMap<>();
        s.put("phase", "Phase 10 — Self-Improvement & Evolution");
        s.put("agents", new String[]{"Eta (Meta-Agent)", "Theta (Learning)", "Iota (Knowledge)", "Kappa (Evolution)"});
        s.put("agentCount", 4);
        s.put("etaAvailable", etaAgent != null);
        s.put("thetaAvailable", thetaAgent != null);
        s.put("iotaAvailable", iotaAgent != null);
        s.put("kappaAvailable", kappaAgent != null);
        s.put("status", (etaAgent != null && thetaAgent != null && iotaAgent != null && kappaAgent != null) ? "operational" : "partial");
        s.put("capabilities", new String[]{
            "Agent performance evolution and self-tuning",
            "Pattern learning from historical decisions",
            "Knowledge base management and consolidation",
            "Consensus mechanism evolution via A/B testing"
        });
        s.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(s);
    }

    /** GET /api/v1/agents/all-phases — Overview of all agent phases */
    @GetMapping("/all-phases")
    public ResponseEntity<Map<String, Object>> allPhasesSummary() {
        Map<String, Object> r = new HashMap<>();
        r.put("totalPhases", 6);
        r.put("timestamp", System.currentTimeMillis());

        List<Map<String, Object>> phases = new ArrayList<>();

        Map<String, Object> p1 = new HashMap<>();
        p1.put("phase", 1); p1.put("name", "Optimization");
        p1.put("description", "LRU Cache, Smart Provider Weighting, Firebase Sync, Error DLQ");
        p1.put("statusUrl", "/api/v1/optimization/health"); p1.put("status", "operational");
        phases.add(p1);

        Map<String, Object> p6 = new HashMap<>();
        p6.put("phase", 6); p6.put("name", "Integration");
        p6.put("description", "Decision Logging, Auto-Fix Loop, A/B Testing, Timeline");
        p6.put("statusUrl", "/api/v1/phase6/health"); p6.put("status", "operational");
        phases.add(p6);

        Map<String, Object> p7 = new HashMap<>();
        p7.put("phase", 7); p7.put("name", "Multi-Platform Generation");
        p7.put("description", "iOS, Web, Desktop app generation + Store publishing");
        p7.put("statusUrl", "/api/phase7/agents/summary"); p7.put("status", "operational");
        phases.add(p7);

        Map<String, Object> p8 = new HashMap<>();
        p8.put("phase", 8); p8.put("name", "Security & Compliance");
        p8.put("description", "OWASP scanning, GDPR compliance, Privacy analysis");
        p8.put("statusUrl", "/api/v1/agents/phase8/summary");
        p8.put("status", (alphaAgent != null && betaAgent != null && gammaAgent != null) ? "operational" : "partial");
        phases.add(p8);

        Map<String, Object> p9 = new HashMap<>();
        p9.put("phase", 9); p9.put("name", "Cost Intelligence");
        p9.put("description", "Cost tracking, resource optimization, budget forecasting");
        p9.put("statusUrl", "/api/v1/agents/phase9/summary");
        p9.put("status", (deltaAgent != null && epsilonAgent != null && zetaAgent != null) ? "operational" : "partial");
        phases.add(p9);

        Map<String, Object> p10 = new HashMap<>();
        p10.put("phase", 10); p10.put("name", "Self-Improvement");
        p10.put("description", "Agent evolution, pattern learning, knowledge management");
        p10.put("statusUrl", "/api/v1/agents/phase10/summary");
        p10.put("status", (etaAgent != null && thetaAgent != null && iotaAgent != null && kappaAgent != null) ? "operational" : "partial");
        phases.add(p10);

        r.put("phases", phases);
        long operational = phases.stream().filter(p -> "operational".equals(p.get("status"))).count();
        r.put("operationalCount", operational);
        r.put("systemStatus", operational == phases.size() ? "fully_operational" : "partially_operational");
        return ResponseEntity.ok(r);
    }
}
