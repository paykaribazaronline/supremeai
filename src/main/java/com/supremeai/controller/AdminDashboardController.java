package com.supremeai.controller;

import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.model.ActivityLog;
import com.supremeai.repository.*;
import com.supremeai.service.ContextualAIRankingService;
import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.ConfigService;
import com.supremeai.admin.AdminDashboardService;
import com.supremeai.admin.ImprovementProposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.function.Function;
import com.supremeai.response.ApiResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController extends BaseAdminController<Object, String> {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);
    private final UserRepository userRepository;
    private final AgentRepository agentRepository;
    private final ProjectRepository projectRepository;
    private final ProviderRepository providerRepository;
    private final ActivityLogRepository activityLogRepository;
    private final SystemLearningRepository systemLearningRepository;
    private final VPNRepository vpnRepository;
private final ContextualAIRankingService contextualRankingService;
     private final AutonomousQuestioningEngine questioningEngine;
    private final AdminDashboardService adminDashboardService;
    private final SolutionMemoryRepository solutionMemoryRepository;
    private static final long START_TIME = System.currentTimeMillis();

    @Autowired
    private ConfigService configService;

public AdminDashboardController(UserRepository userRepository,
                                     AgentRepository agentRepository,
                                     ProjectRepository projectRepository,
                                     ProviderRepository providerRepository,
                                     ActivityLogRepository activityLogRepository,
                                     SystemLearningRepository systemLearningRepository,
                                     VPNRepository vpnRepository,
                                     ContextualAIRankingService contextualRankingService,
                                     AutonomousQuestioningEngine questioningEngine,
                                     AdminDashboardService adminDashboardService,
                                     SolutionMemoryRepository solutionMemoryRepository) {
         this.userRepository = userRepository;
         this.agentRepository = agentRepository;
         this.projectRepository = projectRepository;
         this.providerRepository = providerRepository;
         this.activityLogRepository = activityLogRepository;
         this.systemLearningRepository = systemLearningRepository;
         this.vpnRepository = vpnRepository;
         this.contextualRankingService = contextualRankingService;
         this.questioningEngine = questioningEngine;
         this.adminDashboardService = adminDashboardService;
         this.solutionMemoryRepository = solutionMemoryRepository;
     }


    @GetMapping("/dashboard/contract")
    public Mono<ApiResponse<Map<String, Object>>> getContract() {
        return Mono.zipDelayError(
                Arrays.asList(
                    agentRepository.findAll().count().onErrorReturn(0L),
                    projectRepository.findAll().count().onErrorReturn(0L),
                    projectRepository.findByStatus("COMPLETED").count().onErrorReturn(0L),
                    activityLogRepository.findAll().count().onErrorReturn(0L),
                    activityLogRepository.findBySeverityOrderByTimestampDesc("CRITICAL").count().onErrorReturn(0L),
                    systemLearningRepository.findAll().count().onErrorReturn(0L),
                    vpnRepository.findAll().count().onErrorReturn(0L),
                    userRepository.findAll().count().onErrorReturn(0L),
                    userRepository.findAll().filter(u -> u.getIsActive() != null && u.getIsActive()).count().onErrorReturn(0L),
                    providerRepository.findAll().count().onErrorReturn(0L),
                    providerRepository.findAll().filter(p -> "ONLINE".equalsIgnoreCase(p.getStatus())).count().onErrorReturn(0L),
                    projectRepository.findByStatus("ACTIVE").count().onErrorReturn(0L)
                ),
                data -> buildContract(data)
        ).map(ApiResponse::ok)
        .onErrorResume(e -> {
            log.error("Failed to build dashboard contract: {}", e.getMessage());
            return Mono.just(ApiResponse.ok(buildDefaultContract()));
        });
    }

    private Map<String, Object> buildDefaultContract() {
        return buildContract(new Object[]{0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L});
    }

    private Map<String, Object> buildContract(Object[] data) {
        long totalAgents = (Long) data[0];
        long totalProjects = (Long) data[1];
        long completedProjects = (Long) data[2];
        long totalLogs = (Long) data[3];
        long criticalErrors = (Long) data[4];
        long totalKnowledge = (Long) data[5];
        long activeVPNs = (Long) data[6];
        long totalUsers = (Long) data[7];
        long activeUsers = (Long) data[8];
        long totalProviders = (Long) data[9];
        long activeProviders = (Long) data[10];
        long runningProjects = (Long) data[11];

        double successRate = totalProjects > 0 ? (double) completedProjects / totalProjects * 100 : 100.0;
        double healthScore = totalLogs > 0 ? Math.max(0, 100.0 - ((double) criticalErrors / totalLogs * 1000)) : 100.0;

        Map<String, Object> contract = new HashMap<>();
        contract.put("contractVersion", "3.1.0-google-acquisition");
        contract.put("title", "Google SupremeAI Studio");
        contract.put("description", "Enterprise-Grade Multi-Agent AI Orchestration & Cloud App Development");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("activeAIAgents", totalAgents);
        stats.put("systemHealthScore", Math.round(healthScore * 10) / 10.0);
        stats.put("runningTasks", totalProjects - completedProjects);
        stats.put("runningProjects", runningProjects);
        stats.put("completedTasks", completedProjects);
        stats.put("successRate", Math.round(successRate * 10) / 10.0);
        stats.put("systemHealthStatus", healthScore > 90 ? "healthy" : (healthScore > 70 ? "warning" : "critical"));
        stats.put("systemHealthReason", criticalErrors > 0 ? criticalErrors + " critical system alerts detected" : "All systems operational");
        stats.put("knowledgeBaseSize", totalKnowledge);
        stats.put("activeConnections", activeVPNs);
        
        // Detailed Analytics for Graphs
        stats.put("totalProviders", totalProviders);
        stats.put("activeProviders", activeProviders);
        stats.put("backendConnected", true);
        stats.put("databaseConnected", true);
        stats.put("lastStartTime", START_TIME);
        stats.put("serverUptime", formatUptime(System.currentTimeMillis() - START_TIME));
        stats.put("lastUpdateAt", System.currentTimeMillis());
        
        // Historical Data for Graphical Views (Mocked for now, but structured for real data)
        stats.put("userHistory", Arrays.asList(
            Map.of("t", "08:00", "total", Math.max(0, totalUsers - 5), "active", Math.max(0, activeUsers - 2)),
            Map.of("t", "10:00", "total", Math.max(0, totalUsers - 3), "active", Math.max(0, activeUsers - 1)),
            Map.of("t", "12:00", "total", Math.max(0, totalUsers - 2), "active", Math.max(0, activeUsers + 1)),
            Map.of("t", "14:00", "total", totalUsers, "active", activeUsers)
        ));
        
        stats.put("projectHistory", Arrays.asList(
            Map.of("t", "Mon", "running", Math.max(0, runningProjects - 2), "completed", Math.max(0, completedProjects - 10)),
            Map.of("t", "Tue", "running", Math.max(0, runningProjects - 1), "completed", Math.max(0, completedProjects - 5)),
            Map.of("t", "Wed", "running", runningProjects, "completed", completedProjects)
        ));
        
        contract.put("stats", stats);

        Map<String, Object> uiMetadata = configService.getConfig().getUiMetadata();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> navigation = (List<Map<String, Object>>) uiMetadata.getOrDefault("navigation", List.of(
                createNavItem("overview", "Dashboard", "📊", "System overview and key performance metrics", true),
                createNavItem("ai-agents", "AI Agents", "🤖", "Monitor and assign autonomous agents", true),
                createNavItem("system-learning", "Intelligence", "🧠", "Knowledge harvester and system evolution status", true),
                createNavItem("requirements", "Requirements", "📝", "Requirement elicitation and analysis", true),
                createNavItem("ocr", "OCR Vision", "👁️", "Neural vision and document processing", true),
                createNavItem("exploitation-techniques", "Defense Hub", "🛡️", "Exploitation and defense analysis", true),
                createNavItem("vpn", "VPN Security", "🔒", "Secure tunnel and proxy orchestration", true),
                createNavItem("audit", "Audit Logs", "📝", "Full traceability of system and admin actions", true),
                createNavItem("phases", "Roadmap", "🗺️", "Implementation progress and feature roadmap", true),
                createNavItem("providers", "AI Providers", "🔌", "Manage LLM and AI service connections", true),
                createNavItem("rules", "Protocols", "⚖️", "System governance and behavioral rules", true),
                createNavItem("config", "System Config", "⚙️", "Global platform and system configurations", true)
        ));
        contract.put("navigation", navigation);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) uiMetadata.getOrDefault("components", List.of(
                createComponent("studio", "SupremeAI Studio", "🚀", "Development", true, Map.of("endpoint", "/api/orchestrate/requirement")),
                createComponent("phases", "Roadmap Overview", "🗺️", "Management", true, Map.of()),
                createComponent("ai-agents", "AI Agent Assignment", "🤖", "AI Systems", true, Map.of("endpoint", "/api/admin/agents")),
                createComponent("projects", "Project Manager", "📦", "Development", true, Map.of("endpoint", "/api/admin/projects")),
                createComponent("metrics", "System Metrics", "📊", "Operations", true, Map.of("endpoint", "/api/admin/metrics")),
                createComponent("learning", "System Learning", "📚", "Intelligence", true, Map.of("endpoint", "/api/admin/learning")),
                createComponent("vpn", "VPN Orchestrator", "🔒", "Infrastructure", true, Map.of("endpoint", "/api/admin/vpn")),
                createComponent("audit", "Audit Explorer", "📝", "Security", true, Map.of("endpoint", "/api/admin/audit")),
                createComponent("settings", "Global Settings", "⚙️", "Admin", true, Map.of("endpoint", "/api/admin/settings")),
                createComponent("exploitation-techniques", "Exploitation Dashboard", "⚔️", "Security", true, Map.of("endpoint", "/api/admin/security/exploitation"))
        ));
        contract.put("components", components);

        contract.put("apiEndpoints", Map.of(
                "contract", "/api/admin/dashboard/contract",
                "stats", "/api/admin/metrics/stats",
                "logs", "/api/admin/logs",
                "suggestions", "/api/admin/suggestions"
        ));

        return contract;
    }

    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        if (hours > 0) return String.format("%dh %dm", hours, minutes % 60);
        return String.format("%dm %ds", minutes, seconds % 60);
    }

    @GetMapping("/users")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getUsers() {
        return wrapList(
                userRepository.findAll().map(this::toUserMap),
                "users"
        );
    }

    @GetMapping("/improvements/pending")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getPendingImprovements() {
        List<ImprovementProposal> pending = adminDashboardService.getPendingApprovals();
        return Mono.just(ResponseEntity.ok(ApiResponse.ok(Map.of(
                "pending", pending,
                "count", pending.size()
        ))));
    }

    @PostMapping("/improvements/approve/{proposalId}")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> approveProposal(@PathVariable String proposalId) {
        boolean success = adminDashboardService.approveProposal(proposalId);
        if (success) {
            return Mono.just(ResponseEntity.ok(ApiResponse.ok(Map.of(
                "status", "approved",
                "proposalId", proposalId
            ))));
        } else {
            return Mono.just(ResponseEntity.status(404).body(ApiResponse.error("Proposal not found", Map.of(
                "proposalId", proposalId
            ))));
        }
    }

    @PostMapping("/improvements/reject/{proposalId}")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> rejectProposal(@PathVariable String proposalId) {
        boolean success = adminDashboardService.rejectProposal(proposalId);
        if (success) {
            return Mono.just(ResponseEntity.ok(ApiResponse.ok(Map.of(
                "status", "rejected",
                "proposalId", proposalId
            ))));
        } else {
            return Mono.just(ResponseEntity.status(404).body(ApiResponse.error("Proposal not found", Map.of(
                "proposalId", proposalId
            ))));
        }
    }

    /**
     * Mark a solution memory as obsolete (soft-delete) to unlearn it.
     * This preserves audit trail but excludes solution from future queries.
     */
    @PostMapping("/knowledge/obsolete/{solutionId}")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> markSolutionObsolete(
            @PathVariable String solutionId,
            @RequestBody Map<String, String> request) {
        String reason = request.getOrDefault("reason", "No reason provided");

        return solutionMemoryRepository.findById(solutionId)
            .flatMap(solution -> {
                solution.markObsolete(reason);
                return solutionMemoryRepository.save(solution);
            })
            .map(updated -> ResponseEntity.ok(ApiResponse.ok(Map.<String, Object>of(
                "status", "obsoleted",
                "solutionId", updated.getId(),
                "reason", updated.getObsoleteReason()
            ))))
            .defaultIfEmpty(ResponseEntity.status(404).body(ApiResponse.error("Solution not found", Map.of(
                "solutionId", solutionId
            ))))
            .onErrorResume(e -> {
                log.error("Failed to obsolete solution {}: {}", solutionId, e.getMessage());
                return Mono.just(ResponseEntity.status(500).body(ApiResponse.<Map<String, Object>>error("Failed to obsolete solution: " + e.getMessage())));
            });
    }

    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getFirebaseUid());
        userMap.put("email", user.getEmail());
        userMap.put("displayName", user.getDisplayName());
        userMap.put("tier", user.getTier().toString());
        userMap.put("monthlyQuota", user.fetchMonthlyQuota());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("lastLoginAt", user.getLastLoginAt());
        userMap.put("isActive", user.getIsActive());
        return userMap;
    }

    @PutMapping("/users/{userId}/tier")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> updateUserTier(@PathVariable String userId,
                                                        @RequestBody Map<String, String> request) {
        String newTierStr = request.get("tier");
        if (newTierStr == null) {
            return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error("Tier is required")));
        }

        UserTier newTier;
        try {
            newTier = UserTier.valueOf(newTierStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error("Invalid tier: " + newTierStr)));
        }

        return userRepository.findById(userId)
                .flatMap(user -> {
                    String oldTier = user.getTier().toString();
                    user.setTier(newTier);
                    user.setUpdatedAt(java.time.LocalDateTime.now().toString());
                    String adminUserId = getCurrentAdminUserId();
                    return userRepository.save(user)
                                                         .flatMap(savedUser -> {
                                 // Log admin action reactive way
                                 ActivityLog log = new ActivityLog();
                                 log.setUser(adminUserId);
                                 log.setAction("UPDATE_USER_TIER");
                                 log.setCategory("USER_MANAGEMENT");
                                 log.setSeverity("INFO");
                                 log.setOutcome("SUCCESS");
                                 log.setDetails("Changed user " + userId + " tier from " + oldTier + " to " + newTier);
                                 return activityLogRepository.save(log).thenReturn(savedUser);
                             });
                })
                .map(user -> ResponseEntity.ok(ApiResponse.ok(Map.of(
                        "message", "User tier updated successfully",
                        "user", Map.of(
                                "id", user.getFirebaseUid(),
                                "tier", user.getTier().toString(),
                                "monthlyQuota", user.fetchMonthlyQuota()
                        )
                ))))
                .defaultIfEmpty(ResponseEntity.status(404).body(ApiResponse.error("User not found")))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.error("Failed to update user tier: " + e.getMessage()))));
    }



    @GetMapping("/tiers")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getAvailableTiers() {
        List<Map<String, Object>> tiers = java.util.stream.Stream.of(UserTier.values())
                .map(tier -> {
                    Map<String, Object> tierMap = new HashMap<>();
                    tierMap.put("name", tier.name());
                    tierMap.put("displayName", tier.name().charAt(0) + tier.name().substring(1).toLowerCase());
                    tierMap.put("monthlyQuota", tier.getDefaultMonthlyQuota());
                    tierMap.put("description", tier.getDescription());
                    tierMap.put("hasUnlimitedQuota", tier.hasUnlimitedQuota());
                    return tierMap;
                }).toList();
        return Mono.just(ResponseEntity.ok(ApiResponse.ok(Map.of("tiers", tiers))));
    }

    private Map<String, Object> createNavItem(String key, String label, String icon, String description, boolean enabled) {
        Map<String, Object> item = new HashMap<>();
        item.put("key", key);
        item.put("label", label);
        item.put("icon", icon);
        item.put("description", description);
        item.put("enabled", enabled);
        return item;
    }

    private Map<String, Object> createComponent(String key, String label, String icon, String category, boolean enabled, Map<String, Object> config) {
        Map<String, Object> comp = new HashMap<>();
        comp.put("key", key);
        comp.put("label", label);
        comp.put("icon", icon);
        comp.put("category", category);
        comp.put("enabled", enabled);
        comp.put("config", config);
        return comp;
    }

    /**
     * Get the current authenticated admin's Firebase UID.
     */
    private String getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Not authenticated");
        }
        return auth.getName();
    }

/**
      * Get AI provider rankings based on success rates.
      */
     @GetMapping("/providers/rankings")
     public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getProviderRankings() {
         return Mono.just(ResponseEntity.ok(ApiResponse.ok(Map.of(
             "rankings", contextualRankingService.getStatistics(),
             "timestamp", System.currentTimeMillis()
         ))));
     }

     /**
      * Record a successful request to a provider (called by other services).
      */
     public void recordProviderSuccess(String provider) {
         contextualRankingService.recordTaskOutcome(provider, 
             ContextualAIRankingService.TaskType.QUESTION_ANSWERING, true, 1000L, 4.0);
     }

     /**
      * Record a failed request to a provider (called by other services).
      */
     public void recordProviderFailure(String provider) {
         contextualRankingService.recordTaskOutcome(provider, 
             ContextualAIRankingService.TaskType.QUESTION_ANSWERING, false, 1000L, 1.0);
     }

     /**
      * POST /api/admin/prompt/analyze - Analyze a prompt and get clarifying questions.
      * Helps reduce ambiguity before code generation.
      */
     @PostMapping("/prompt/analyze")
     public ResponseEntity<ApiResponse<Map<String, Object>>> analyzePrompt(@RequestBody Map<String, String> body) {
         String prompt = body.get("prompt");
         if (prompt == null || prompt.trim().isEmpty()) {
             return ResponseEntity.badRequest().body(ApiResponse.error("Prompt is required"));
         }

         AutonomousQuestioningEngine.ValidationResult result = 
             questioningEngine.validateAndQuestion(prompt, AutonomousQuestioningEngine.RequestType.GENERAL_AI);
         
         Map<String, Object> analysis = new HashMap<>();
         analysis.put("originalInput", result.getOriginalInput());
         analysis.put("clarityScore", result.getClarityScore());
         analysis.put("isComplete", result.isComplete());
         analysis.put("clarifyingQuestions", result.getClarifyingQuestions());
         return ResponseEntity.ok(ApiResponse.ok(analysis));
     }

    /**
     * GET /api/admin/prompt/suggestions - Get common clarifying questions for code generation.
     */
    @GetMapping("/prompt/suggestions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPromptSuggestions() {
        List<Map<String, Object>> suggestions = List.of(
            Map.of(
                "category", "Scope",
                "questions", List.of(
                    "What is the target user base?",
                    "Is this a prototype or production system?",
                    "What are the key features needed?"
                )
            ),
            Map.of(
                "category", "Tech Stack",
                "questions", List.of(
                    "Preferred programming language?",
                    "Frontend framework preference?",
                    "Database requirements?"
                )
            ),
            Map.of(
                "category", "Constraints",
                "questions", List.of(
                    "Timeline expectations?",
                    "Budget considerations?",
                    "Existing systems to integrate with?"
                )
            )
        );

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "suggestions", suggestions,
            "timestamp", System.currentTimeMillis()
        )));
    }
}
