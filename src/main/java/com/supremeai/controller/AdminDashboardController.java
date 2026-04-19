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

    @GetMapping("/dashboard/contract")
    public Mono<Map<String, Object>> getContract() {
        return Mono.zip(
            agentRepository.findAll().count(),
            projectRepository.findAll().count()
        ).map(tuple -> {
            Map<String, Object> contract = new HashMap<>();
            contract.put("contractVersion", "2026-04-09-unified");
            contract.put("title", "SupremeAI Admin Dashboard");

            Map<String, Object> stats = new HashMap<>();
            stats.put("activeAIAgents", tuple.getT1());
            stats.put("systemHealthScore", 98.5);
            stats.put("runningProjects", tuple.getT2());
            stats.put("completedProjects", 142);
            stats.put("successRate", 99.2);
            stats.put("systemHealthStatus", "HEALTHY");
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
    public Mono<ResponseEntity<?>> getUsers() {
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
                    return ResponseEntity.ok(Map.of("users", userList));
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to fetch users: " + e.getMessage()))));
    }

    @PutMapping("/users/{userId}/tier")
    public Mono<ResponseEntity<?>> updateUserTier(@PathVariable String userId,
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
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500)
                        .body(Map.of("error", "Failed to update user tier: " + e.getMessage()))));
    }

    @GetMapping("/tiers")
    public ResponseEntity<?> getAvailableTiers() {
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
        return ResponseEntity.ok(Map.of("tiers", tiers));
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
