package org.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Admin Dashboard API Controller
 * Provides endpoints for dashboard statistics and system overview
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    @GetMapping("/contract")
    public ResponseEntity<?> getDashboardContract() {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("contractVersion", "2026-04-03");
            response.put("title", "SupremeAI Admin Control Panel");
            response.put("entryPath", "/admin.html");
            response.put("stats", buildDashboardStats());
            response.put("navigation", buildNavigation());
            response.put("orchestration", buildOrchestrationEndpoints());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            return ResponseEntity.ok(buildDashboardStats());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "healthy");
            health.put("cpuUsage", 45.2);
            health.put("memoryUsage", 62.1);
            health.put("apiLatency", 125);
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> buildDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long lastSync = System.currentTimeMillis();
        double systemHealthScore = 98.5;
        stats.put("activeAIAgents", 5);
        stats.put("runningTasks", 12);
        stats.put("completedTasks", 156);
        stats.put("successRate", 94.2);
        stats.put("systemHealth", systemHealthScore);
        stats.put("systemHealthScore", systemHealthScore);
        stats.put("systemHealthStatus", resolveHealthStatus(systemHealthScore));
        stats.put("uptime", "45d 14h 23m");
        stats.put("lastSync", lastSync);
        stats.put("lastSyncTime", new Date(lastSync).toString());
        return stats;
    }

    private List<Map<String, Object>> buildNavigation() {
        return List.of(
            nav("overview", "Dashboard", true),
            nav("techniques", "Techniques", true),
            nav("provider-coverage", "Provider Coverage", true),
            nav("api-keys", "API Key Manager", true),
            nav("ai-agents", "AI Agent Assignment", true),
            nav("projects", "Projects", true),
            nav("logs", "Audit Logs", true),
            nav("timeline", "Decision Timeline", true),
            nav("settings", "Settings", true)
        );
    }

    private Map<String, Object> buildOrchestrationEndpoints() {
        Map<String, Object> endpoints = new LinkedHashMap<>();
        endpoints.put("status", "/api/agent-orchestration/status");
        endpoints.put("leaderboard", "/api/agent-orchestration/leaderboard");
        endpoints.put("history", "/api/agent-orchestration/history");
        endpoints.put("submit", "/api/agent-orchestration/submit");
        return endpoints;
    }

    private Map<String, Object> nav(String key, String label, boolean enabled) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("label", label);
        entry.put("enabled", enabled);
        return entry;
    }

    private String resolveHealthStatus(double systemHealthScore) {
        if (systemHealthScore >= 90) {
            return "healthy";
        }
        if (systemHealthScore >= 70) {
            return "warning";
        }
        return "critical";
    }
}
