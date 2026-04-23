package com.supremeai.controller;

import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.model.ActivityLog;
import com.supremeai.repository.*;
import com.supremeai.service.AIRankingService;
import com.supremeai.service.AutonomousQuestioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple7;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController extends BaseAdminController<Object, String> {

    private final UserRepository userRepository;
    private final AgentRepository agentRepository;
    private final ProjectRepository projectRepository;
    private final ProviderRepository providerRepository;
    private final ActivityLogRepository activityLogRepository;
    private final SystemLearningRepository systemLearningRepository;
    private final VPNRepository vpnRepository;
    private final AIRankingService aiRankingService;
    private final AutonomousQuestioningService questioningService;

    public AdminDashboardController(UserRepository userRepository,
                                     AgentRepository agentRepository,
                                     ProjectRepository projectRepository,
                                     ProviderRepository providerRepository,
                                     ActivityLogRepository activityLogRepository,
                                     SystemLearningRepository systemLearningRepository,
                                     VPNRepository vpnRepository,
                                     AIRankingService aiRankingService,
                                     AutonomousQuestioningService questioningService) {
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
        this.projectRepository = projectRepository;
        this.providerRepository = providerRepository;
        this.activityLogRepository = activityLogRepository;
        this.systemLearningRepository = systemLearningRepository;
        this.vpnRepository = vpnRepository;
        this.aiRankingService = aiRankingService;
        this.questioningService = questioningService;
    }

    @GetMapping("/dashboard/contract")
    public Mono<Map<String, Object>> getContract() {
        return Mono.zip(
                agentRepository.findAll().count(),
                projectRepository.findAll().count(),
                projectRepository.findByStatus("COMPLETED").count(),
                activityLogRepository.findAll().count(),
                activityLogRepository.findBySeverityOrderByTimestampDesc("CRITICAL").count(),
                systemLearningRepository.findAll().count(),
                vpnRepository.findAll().count()
        ).map(tuple -> buildContract(tuple));
    }

    private Map<String, Object> buildContract(Tuple7<Long, Long, Long, Long, Long, Long, Long> tuple) {
        long totalAgents = tuple.getT1();
        long totalProjects = tuple.getT2();
        long completedProjects = tuple.getT3();
        long totalLogs = tuple.getT4();
        long criticalErrors = tuple.getT5();
        long totalKnowledge = tuple.getT6();
        long activeVPNs = tuple.getT7();

        double successRate = totalProjects > 0 ? (double) completedProjects / totalProjects * 100 : 100.0;
        double healthScore = totalLogs > 0 ? Math.max(0, 100.0 - ((double) criticalErrors / totalLogs * 1000)) : 100.0;

        Map<String, Object> contract = new HashMap<>();
        contract.put("contractVersion", "2026-04-10-live");
        contract.put("title", "SupremeAI Admin Dashboard");

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeAIAgents", totalAgents);
        stats.put("systemHealthScore", Math.round(healthScore * 10) / 10.0);
        stats.put("runningProjects", totalProjects - completedProjects);
        stats.put("completedProjects", completedProjects);
        stats.put("successRate", Math.round(successRate * 10) / 10.0);
        stats.put("systemHealthStatus", healthScore > 90 ? "HEALTHY" : (healthScore > 70 ? "STABLE" : "CRITICAL"));
        stats.put("knowledgeBaseSize", totalKnowledge);
        stats.put("activeConnections", activeVPNs);
        contract.put("stats", stats);

        List<Map<String, Object>> navigation = List.of(
                createNavItem("overview", "Dashboard", "📊", true),
                createNavItem("projects", "Projects & Git", "📂", true),
                createNavItem("ai-systems", "AI Systems", "🤖", true),
                createNavItem("metrics", "Metrics & Analytics", "📈", true),
                createNavItem("knowledge", "Knowledge & Learning", "🧠", true),
                createNavItem("integrations", "Integrations", "🔌", true),
                createNavItem("resilience", "System Resilience", "🛡️", true),
                createNavItem("administration", "Administration", "⚙️", true)
        );
        contract.put("navigation", navigation);

        return contract;
    }

    @GetMapping("/users")
    public Mono<ResponseEntity<Object>> getUsers() {
        return wrapList(
                userRepository.findAll().map(this::toUserMap),
                "users"
        );
    }

    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getFirebaseUid());
        userMap.put("email", user.getEmail());
        userMap.put("displayName", user.getDisplayName());
        userMap.put("tier", user.getTier().toString());
        userMap.put("monthlyQuota", user.getMonthlyQuota());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("lastLoginAt", user.getLastLoginAt());
        userMap.put("isActive", user.getIsActive());
        return userMap;
    }

    @PutMapping("/users/{userId}/tier")
    public Mono<ResponseEntity<Object>> updateUserTier(@PathVariable String userId,
                                                        @RequestBody Map<String, String> request) {
        String newTierStr = request.get("tier");
        if (newTierStr == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "Tier is required")));
        }

        UserTier newTier;
        try {
            newTier = UserTier.valueOf(newTierStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "Invalid tier: " + newTierStr)));
        }

        return userRepository.findById(userId)
                .flatMap(user -> {
                    String oldTier = user.getTier().toString();
                    user.setTier(newTier);
                    user.setUpdatedAt(java.time.LocalDateTime.now());
                    String adminUserId = getCurrentAdminUserId();
                    return userRepository.save(user)
                                                        .doOnSuccess(savedUser -> {
                                // Log admin action
                                ActivityLog log = new ActivityLog();
                                log.setUser(adminUserId);
                                log.setAction("UPDATE_USER_TIER");
                                log.setCategory("USER_MANAGEMENT");
                                log.setSeverity("INFO");
                                log.setOutcome("SUCCESS");
                                log.setDetails("Changed user " + userId + " tier from " + oldTier + " to " + newTier);
                                activityLogRepository.save(log).block();
                            });
                })
                                .map(userObject -> {
                    User user = (User) userObject;
                    return (ResponseEntity<Object>) ResponseEntity.ok((Object) Map.of(
                        "message", "User tier updated successfully",
                        "user", Map.of(
                                "id", user.getFirebaseUid(),
                                "tier", user.getTier().toString(),
                                "monthlyQuota", user.getMonthlyQuota()
                        )
                ));})
                .defaultIfEmpty(ResponseEntity.status(404).body(Map.of("error", "User not found")))
                .onErrorResume(e -> handleError("Failed to update user tier for user: " + userId, e));
    }



    @GetMapping("/tiers")
    public Mono<ResponseEntity<Object>> getAvailableTiers() {
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
        return Mono.just(ResponseEntity.ok((Object) Map.of("tiers", tiers)));
    }

    private Map<String, Object> createNavItem(String key, String label, String icon, boolean enabled) {
        Map<String, Object> item = new HashMap<>();
        item.put("key", key);
        item.put("label", label);
        item.put("icon", icon);
        item.put("enabled", enabled);
        return item;
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
    public Mono<ResponseEntity<Object>> getProviderRankings() {
        return Mono.just(ResponseEntity.ok((Object) Map.of(
            "rankings", aiRankingService.getRankings(),
            "timestamp", System.currentTimeMillis()
        )));
    }

    /**
     * Record a successful request to a provider (called by other services).
     */
    public void recordProviderSuccess(String provider) {
        aiRankingService.recordSuccess(provider);
    }

    /**
     * Record a failed request to a provider (called by other services).
     */
    public void recordProviderFailure(String provider) {
        aiRankingService.recordFailure(provider);
    }

    /**
     * POST /api/admin/prompt/analyze - Analyze a prompt and get clarifying questions.
     * Helps reduce ambiguity before code generation.
     */
    @PostMapping("/prompt/analyze")
    public ResponseEntity<Map<String, Object>> analyzePrompt(@RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt is required"));
        }

        Map<String, Object> analysis = questioningService.getPromptAnalysis(prompt);
        return ResponseEntity.ok(analysis);
    }

    /**
     * GET /api/admin/prompt/suggestions - Get common clarifying questions for code generation.
     */
    @GetMapping("/prompt/suggestions")
    public ResponseEntity<Map<String, Object>> getPromptSuggestions() {
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

        return ResponseEntity.ok(Map.of(
            "suggestions", suggestions,
            "timestamp", System.currentTimeMillis()
        ));
    }
}
