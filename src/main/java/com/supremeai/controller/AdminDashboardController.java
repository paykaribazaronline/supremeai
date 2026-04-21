package com.supremeai.controller;

import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController extends BaseAdminController<User, String> {

    private final UserRepository userRepository;
    private final AgentRepository agentRepository;
    private final ProjectRepository projectRepository;
    private final ProviderRepository providerRepository;
    private final ActivityLogRepository activityLogRepository;
    private final SystemLearningRepository systemLearningRepository;
    private final VPNRepository vpnRepository;

    public AdminDashboardController(UserRepository userRepository,
                                     AgentRepository agentRepository,
                                     ProjectRepository projectRepository,
                                     ProviderRepository providerRepository,
                                     ActivityLogRepository activityLogRepository,
                                     SystemLearningRepository systemLearningRepository,
                                     VPNRepository vpnRepository) {
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
        this.projectRepository = projectRepository;
        this.providerRepository = providerRepository;
        this.activityLogRepository = activityLogRepository;
        this.systemLearningRepository = systemLearningRepository;
        this.vpnRepository = vpnRepository;
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
                createNavItem("projects", "Projects", "📂", true),
                createNavItem("providers", "AI Providers", "🤖", true),
                createNavItem("metrics", "Metrics", "📈", true),
                createNavItem("settings", "Settings", "⚙️", true),
                createNavItem("api-keys", "API Keys", "🔑", true),
                createNavItem("learning", "Learning", "🧠", true),
                createNavItem("vpn", "VPN", "🔒", true),
                createNavItem("quota", "Quota", "📊", true),
                createNavItem("resilience", "Resilience", "🛡️", true),
                createNavItem("ml-intelligence", "ML Intelligence", "🤖", true),
                createNavItem("notifications", "Notifications", "🔔", true),
                createNavItem("analytics", "Analytics", "📊", true),
                createNavItem("consensus", "Consensus", "🤝", true),
                createNavItem("git-ops", "Git Ops", "🔀", true),
                createNavItem("headless-browser", "Headless Browser", "🌐", true),
                createNavItem("chat-history", "Chat History", "💬", true),
                createNavItem("system-learning", "System Learning", "📚", true),
                createNavItem("autofix", "Auto Fix", "🔧", true),
                createNavItem("deployment", "Deployment", "🚀", true),
                createNavItem("self-healing", "Self Healing", "💊", true),
                createNavItem("ai-agents", "AI Agents", "🤖", true),
                createNavItem("exploitation-techniques", "Exploitation Techniques", "🧨", true),
                createNavItem("user-management", "User Management", "👥", true)
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
                    user.setTier(newTier);
                    user.setUpdatedAt(java.time.LocalDateTime.now());
                    return userRepository.save(user);
                })
                .map(user -> ResponseEntity.ok(Map.of(
                        "message", "User tier updated successfully",
                        "user", Map.of(
                                "id", user.getFirebaseUid(),
                                "tier", user.getTier().toString(),
                                "monthlyQuota", user.getMonthlyQuota()
                        )
                )))
                .defaultIfEmpty(ResponseEntity.status(404).body(Map.of("error", "User not found")))
                .onErrorResume(e -> Mono.just(handleError("Failed to update user tier for user: " + userId, e)));
    }

    private ResponseEntity<Object> handleError(String context, Throwable e) {
        logger.error(context, e);
        Map<String, Object> errorBody = Map.of("error", context + ": " + e.getMessage());
        return ResponseEntity.status(500).body((Object) errorBody);
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
}
