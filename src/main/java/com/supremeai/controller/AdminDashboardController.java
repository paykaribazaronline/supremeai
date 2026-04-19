package com.supremeai.controller;

import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private SystemLearningRepository systemLearningRepository;

    @Autowired
    private VPNRepository vpnRepository;

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
        ).map(tuple -> {
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

            List<Map<String, Object>> navigation = new ArrayList<>();
            navigation.add(createNavItem("overview", "Dashboard", "📊", true));
            navigation.add(createNavItem("projects", "Projects", "📂", true));
            navigation.add(createNavItem("providers", "AI Providers", "🤖", true));
            navigation.add(createNavItem("metrics", "Metrics", "📈", true));
            navigation.add(createNavItem("settings", "Settings", "⚙️", true));
            navigation.add(createNavItem("api-keys", "API Keys", "🔑", true));
            navigation.add(createNavItem("learning", "Learning", "🧠", true));
            navigation.add(createNavItem("vpn", "VPN", "🔒", true));
            navigation.add(createNavItem("quota", "Quota", "📊", true));
            navigation.add(createNavItem("resilience", "Resilience", "🛡️", true));
            navigation.add(createNavItem("ml-intelligence", "ML Intelligence", "🤖", true));
            navigation.add(createNavItem("notifications", "Notifications", "🔔", true));
            navigation.add(createNavItem("analytics", "Analytics", "📊", true));
            navigation.add(createNavItem("consensus", "Consensus", "🤝", true));
            navigation.add(createNavItem("git-ops", "Git Ops", "🔀", true));
            navigation.add(createNavItem("headless-browser", "Headless Browser", "🌐", true));
            navigation.add(createNavItem("chat-history", "Chat History", "💬", true));
            navigation.add(createNavItem("system-learning", "System Learning", "📚", true));
            navigation.add(createNavItem("autofix", "Auto Fix", "🔧", true));
            navigation.add(createNavItem("deployment", "Deployment", "🚀", true));
            navigation.add(createNavItem("self-healing", "Self Healing", "💊", true));
            navigation.add(createNavItem("ai-agents", "AI Agents", "🤖", true));
            navigation.add(createNavItem("exploitation-techniques", "Exploitation Techniques", "🧨", true));
            navigation.add(createNavItem("user-management", "User Management", "👥", true));
            contract.put("navigation", navigation);

            return contract;
        });
    }

    @GetMapping("/users")
    public Mono<ResponseEntity<Object>> getUsers() {
        return userRepository.findAll()
                .collectList()
                .map(users -> {
                    List<Map<String, Object>> userList = users.stream().map(user -> {
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
                    }).toList();
                    Map<String, Object> responseBody = Map.of("users", userList);
                    return (ResponseEntity<Object>) ResponseEntity.ok((Object) responseBody);
                })
                .onErrorResume(e -> {
                    Map<String, Object> errorBody = Map.of("error", "Failed to fetch users: " + e.getMessage());
                    return Mono.just((ResponseEntity<Object>) ResponseEntity.status(500).body((Object) errorBody));
                });
    }

    @PutMapping("/users/{userId}/tier")
    public Mono<ResponseEntity<Object>> updateUserTier(@PathVariable String userId,
                                                 @RequestBody Map<String, String> request) {
        String newTierStr = request.get("tier");
        if (newTierStr == null) {
            Map<String, Object> errorBody = Map.of("error", "Tier is required");
            return Mono.just((ResponseEntity<Object>) ResponseEntity.badRequest().body((Object) errorBody));
        }

        UserTier newTier;
        try {
            newTier = UserTier.valueOf(newTierStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorBody = Map.of("error", "Invalid tier: " + newTierStr);
            return Mono.just((ResponseEntity<Object>) ResponseEntity.badRequest().body((Object) errorBody));
        }

        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setTier(newTier);
                    user.setUpdatedAt(java.time.LocalDateTime.now());
                    return userRepository.save(user);
                })
                .map(user -> {
                    Map<String, Object> responseBody = Map.of(
                        "message", "User tier updated successfully",
                        "user", Map.of(
                                "id", user.getFirebaseUid(),
                                "tier", user.getTier().toString(),
                                "monthlyQuota", user.getMonthlyQuota()
                        )
                    );
                    return (ResponseEntity<Object>) ResponseEntity.ok((Object) responseBody);
                })
                .defaultIfEmpty((ResponseEntity<Object>) ResponseEntity.status(404).body((Object) Map.of("error", "User not found")))
                .onErrorResume(e -> {
                    Map<String, Object> errorBody = Map.of("error", "Failed to update user tier: " + e.getMessage());
                    return Mono.just((ResponseEntity<Object>) ResponseEntity.status(500).body((Object) errorBody));
                });
    }

    @GetMapping("/tiers")
    public Mono<ResponseEntity<Object>> getAvailableTiers() {
        List<Map<String, Object>> tiers = new ArrayList<>();
        for (UserTier tier : UserTier.values()) {
            Map<String, Object> tierMap = new HashMap<>();
            tierMap.put("name", tier.name());
            tierMap.put("displayName", tier.name().charAt(0) + tier.name().substring(1).toLowerCase());
            tierMap.put("monthlyQuota", tier.getDefaultMonthlyQuota());
            tierMap.put("description", tier.getDescription());
            tierMap.put("hasUnlimitedQuota", tier.hasUnlimitedQuota());
            tiers.add(tierMap);
        }
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
