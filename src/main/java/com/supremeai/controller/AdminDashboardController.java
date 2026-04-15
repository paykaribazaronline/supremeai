package com.supremeai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @GetMapping("/contract")
    public Map<String, Object> getContract() {
        Map<String, Object> contract = new HashMap<>();
        contract.put("contractVersion", "2026-04-09-unified");
        contract.put("title", "SupremeAI Admin Dashboard");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeAIAgents", 12);
        stats.put("systemHealthScore", 98.5);
        stats.put("runningProjects", 5);
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
        contract.put("navigation", navigation);

        return contract;
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
