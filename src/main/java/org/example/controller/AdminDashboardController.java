package org.example.controller;

import org.example.model.TaskAssignment;
import org.example.service.AssignmentService;
import org.example.service.GeneratedProjectRegistryService;
import org.example.service.ProviderRegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Admin Dashboard API Controller - UNIFIED CONTRACT FOR ALL CLIENTS
 * 
 * ⭐ SINGLE SOURCE OF TRUTH
 * - React, Flutter Mobile, Flutter Web all consume this contract
 * - One place to change = everywhere updates
 * - All labels, icons, text from this endpoint
 */
@RestController("adminDashboardController")
@RequestMapping("/api/admin/dashboard")
@CrossOrigin(origins = "*")
public class AdminDashboardController {

    @Autowired(required = false)
    private AssignmentService assignmentService;

    @Autowired(required = false)
    private GeneratedProjectRegistryService generatedProjectRegistryService;

    @Autowired(required = false)
    private ProviderRegistryService providerRegistryService;

    @GetMapping("/contract")
    public ResponseEntity<?> getDashboardContract() {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            
            // Metadata
            response.put("contractVersion", "2026-04-09-unified");
            response.put("title", "SupremeAI Admin Control Panel");
            response.put("description", "Unified admin dashboard for all clients");
            response.put("entryPath", "/admin.html");
            response.put("language", "en");
            
            // Statistics
            response.put("stats", buildDashboardStats());
            
            // Navigation menu - single source of truth
            response.put("navigation", buildUnifiedNavigation());
            
            // Component definitions - all 19 components configured here
            response.put("components", buildComponentDefinitions());
            
            // API endpoints for all features
            response.put("apiEndpoints", buildApiEndpoints());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Dashboard stats - same data everywhere
     */
    private Map<String, Object> buildDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long lastSync = System.currentTimeMillis();
        int runningTasks = 0;
        int completedTasks = 0;
        int activeAIAgents = 0;
        int runningProjects = 0;
        int completedProjects = 0;

        if (assignmentService != null) {
            try {
                List<TaskAssignment> assignments = assignmentService.getAllAssignments();
                Set<String> activeAgentIds = new HashSet<>();
                for (TaskAssignment assignment : assignments) {
                    String status = String.valueOf(assignment.getStatus()).toLowerCase(Locale.ROOT);
                    boolean isCompleted = status.equals("completed") || status.equals("done");
                    boolean isRunning = status.equals("in-progress") || status.equals("running") || status.equals("pending");

                    if (isCompleted) {
                        completedTasks++;
                    }
                    if (isRunning) {
                        runningTasks++;
                    }

                    String agentId = assignment.getAgentId();
                    if (agentId != null && !agentId.isBlank() && !isCompleted) {
                        activeAgentIds.add(agentId);
                    }
                }
                activeAIAgents = activeAgentIds.size();
            } catch (Exception ignored) {
            }
        }

        if (generatedProjectRegistryService != null) {
            try {
                for (Map<String, Object> project : generatedProjectRegistryService.listProjects()) {
                    String status = String.valueOf(project.getOrDefault("status", "")).toUpperCase(Locale.ROOT);
                    if (status.equals("GENERATING") || status.equals("TEMPLATE_INITIALIZED") || status.equals("RUNNING")) {
                        runningProjects++;
                    }
                    if (status.equals("COMPLETED") || status.equals("PUSHED_TO_REPO") || status.equals("PUSH_FAILED") || status.equals("FAILED")) {
                        completedProjects++;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (activeAIAgents == 0 && providerRegistryService != null) {
            try {
                activeAIAgents = providerRegistryService.getActiveProviderCount();
            } catch (Exception ignored) {
            }
        }

        int totalTasks = runningTasks + completedTasks;
        double successRate = totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0.0;

        double systemHealthScore = 70.0;
        systemHealthScore += Math.min(15.0, activeAIAgents * 3.0);
        systemHealthScore += Math.min(10.0, runningProjects * 2.0);
        systemHealthScore += Math.min(5.0, successRate / 20.0);
        systemHealthScore = Math.min(99.9, Math.max(35.0, systemHealthScore));

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        stats.put("activeAIAgents", activeAIAgents);
        stats.put("runningTasks", runningTasks);
        stats.put("completedTasks", completedTasks);
        stats.put("runningProjects", runningProjects);
        stats.put("completedProjects", completedProjects);
        stats.put("successRate", Math.round(successRate * 10.0) / 10.0);
        stats.put("systemHealth", systemHealthScore);
        stats.put("systemHealthScore", systemHealthScore);
        stats.put("systemHealthStatus", resolveHealthStatus(systemHealthScore));
        stats.put("systemHealthReason", buildHealthStatusReason(systemHealthScore, activeAIAgents, runningTasks, completedTasks, successRate));
        stats.put("uptime", formatUptime(uptimeMs));
        stats.put("lastSync", lastSync);
        stats.put("lastSyncTime", new Date(lastSync).toString());
        
        return stats;
    }

    /**
     * UNIFIED Navigation menu
     * Changed here = changes in React, Flutter Mobile, Flutter Web
     */
    private List<Map<String, Object>> buildUnifiedNavigation() {
        return List.of(
            menuItem("overview", "📊 Dashboard", "dashboard", true, "Overview"),
            menuItem("techniques", "🎯 Techniques", "techniques", true, "Operational Techniques"),
            menuItem("provider-coverage", "🌐 Provider Coverage", "provider", true, "AI Provider Status"),
            menuItem("api-keys", "🔑 API Keys", "api-keys", true, "API Key Management"),
            menuItem("ai-agents", "🤖 AI Agents", "ai-agents", true, "AI Assignment & Control"),
            menuItem("projects", "📁 Projects", "projects", true, "Project Management"),
            menuItem("logs", "📋 Audit Logs", "logs", true, "System Audit Trail"),
            menuItem("timeline", "⏱️ Timeline", "timeline", true, "Decision Timeline"),
            menuItem("system-control", "⚙️ System Control", "system", true, "System Mode & Status"),
            menuItem("chat", "💬 Chat AI", "chat", true, "Chat Interface"),
            menuItem("voting", "🗳️ Voting", "voting", true, "Decision Voting"),
            menuItem("learning", "🧠 Learning", "learning", true, "System Learning Stats"),
            menuItem("settings", "⚙️ Settings", "settings", true, "System Settings")
        );
    }

    /**
     * Component definitions - configure all 19 components
     * Each component has its settings centralized here
     */
    private List<Map<String, Object>> buildComponentDefinitions() {
        List<Map<String, Object>> components = new ArrayList<>();
        
        // 1. Dashboard Overview
        components.add(component(
            "overview",
            "Dashboard Overview",
            "📊",
            "main",
            true,
            Map.of(
                "title", "System Overview",
                "description", "Real-time dashboard statistics",
                "refreshInterval", 30000,
                "showStats", true,
                "showHealth", true
            )
        ));
        
        // 2. API Management
        components.add(component(
            "api-management",
            "API Management",
            "🔌",
            "management",
            true,
            Map.of(
                "title", "API Management",
                "description", "Manage API keys and endpoints",
                "endpoint", "/api/admin/api-management",
                "requireAuth", true
            )
        ));
        
        // 3. AI Model Search
        components.add(component(
            "ai-models",
            "AI Model Search",
            "🔍",
            "tools",
            true,
            Map.of(
                "title", "AI Model Catalog",
                "description", "Search and discover AI models",
                "endpoint", "/api/admin/ai-models",
                "searchable", true
            )
        ));
        
        // 4. VPN Management
        components.add(component(
            "vpn",
            "VPN Management",
            "🔐",
            "security",
            true,
            Map.of(
                "title", "VPN Configuration",
                "description", "Manage VPN connections",
                "endpoint", "/api/admin/vpn",
                "requireAuth", true
            )
        ));
        
        // 5. Chat with AI
        components.add(component(
            "chat",
            "Chat with AI",
            "💬",
            "communication",
            true,
            Map.of(
                "title", "AI Chat Interface",
                "description", "Chat with AI agents",
                "endpoint", "/api/chat",
                "realtime", true
            )
        ));
        
        // 6. AI Assignment
        components.add(component(
            "ai-assignment",
            "AI Assignment",
            "👥",
            "management",
            true,
            Map.of(
                "title", "Assign AI Agents",
                "description", "Assign tasks to AI agents",
                "endpoint", "/api/admin/ai-assignment",
                "requireAdmin", true
            )
        ));
        
        // 7. Decision Voting
        components.add(component(
            "voting",
            "Decision Voting",
            "🗳️",
            "decision",
            true,
            Map.of(
                "title", "Multi-AI Consensus",
                "description", "Vote on system decisions",
                "endpoint", "/api/consensus",
                "requireAuth", true
            )
        ));
        
        // 8. King Mode Panel
        components.add(component(
            "king-mode",
            "King Mode Panel",
            "👑",
            "control",
            true,
            Map.of(
                "title", "King Mode Control",
                "description", "Override system decisions",
                "endpoint", "/api/admin/king-mode",
                "requireAdmin", true
            )
        ));
        
        // 9. Progress Monitor
        components.add(component(
            "progress",
            "Progress Monitor",
            "📈",
            "monitoring",
            true,
            Map.of(
                "title", "Task Progress",
                "description", "Monitor running tasks",
                "endpoint", "/api/admin/progress",
                "refresh", true
            )
        ));
        
        // 10. Improvement Tracking
        components.add(component(
            "improvements",
            "Improvement Tracking",
            "🎯",
            "analytics",
            true,
            Map.of(
                "title", "System Improvements",
                "description", "Track system improvements",
                "endpoint", "/api/learning/improvements",
                "requireAuth", true
            )
        ));
        
        // 11. AI Work History
        components.add(component(
            "work-history",
            "AI Work History",
            "📜",
            "history",
            true,
            Map.of(
                "title", "Work History",
                "description", "Historical work records",
                "endpoint", "/api/admin/work-history",
                "paginated", true
            )
        ));
        
        // 12. Audit Logs
        components.add(component(
            "audit-logs",
            "Audit Logs",
            "📋",
            "security",
            true,
            Map.of(
                "title", "Audit Trail",
                "description", "System audit logs",
                "endpoint", "/api/admin/audit",
                "searchable", true
            )
        ));
        
        // 13. System Metrics
        components.add(component(
            "metrics",
            "System Metrics",
            "📊",
            "monitoring",
            true,
            Map.of(
                "title", "Performance Metrics",
                "description", "System performance data",
                "endpoint", "/api/admin/metrics",
                "realtime", true
            )
        ));
        
        // 14. API Keys Manager
        components.add(component(
            "api-keys",
            "API Keys Manager",
            "🔑",
            "security",
            true,
            Map.of(
                "title", "API Key Management",
                "description", "Manage all API keys",
                "endpoint", "/api/admin/api-keys",
                "requireAdmin", true
            )
        ));
        
        // 15. GitHub Dashboard
        components.add(component(
            "github",
            "GitHub Dashboard",
            "🐙",
            "integration",
            true,
            Map.of(
                "title", "GitHub Integration",
                "description", "GitHub repository status",
                "endpoint", "/api/github",
                "external", true
            )
        ));
        
        // 16. Headless Browser
        components.add(component(
            "browser",
            "Headless Browser",
            "🌐",
            "tools",
            true,
            Map.of(
                "title", "Browser Automation",
                "description", "Headless browser control",
                "endpoint", "/api/browser",
                "requireAuth", true
            )
        ));
        
        // 17. Chat History
        components.add(component(
            "chat-history",
            "Chat History",
            "💭",
            "history",
            true,
            Map.of(
                "title", "Chat History",
                "description", "Previous conversations",
                "endpoint", "/api/chat/history",
                "paginated", true
            )
        ));
        
        // 18. System Learning
        components.add(component(
            "learning",
            "System Learning",
            "🧠",
            "analytics",
            true,
            Map.of(
                "title", "Learning Statistics",
                "description", "AI learning metrics",
                "endpoint", "/api/learning/stats",
                "requireAuth", true
            )
        ));
        
        // 19. Admin Tips
        components.add(component(
            "admin-tips",
            "Admin Tips",
            "💡",
            "help",
            true,
            Map.of(
                "title", "Tips & Help",
                "description", "Admin tips and tutorials",
                "endpoint", "/api/admin/tips",
                "external", false
            )
        ));
        
        // 20. Settings (Flutter)
        components.add(component(
            "settings",
            "Settings",
            "⚙️",
            "configuration",
            true,
            Map.of(
                "title", "System Settings",
                "description", "Configure system preferences",
                "endpoint", "/api/admin/settings",
                "requireAdmin", true,
                "categories", new String[]{"language", "theme", "notifications", "security"}
            )
        ));
        
        // 21. Quota Management (Flutter)
        components.add(component(
            "quota",
            "Quota Management",
            "📊",
            "management",
            true,
            Map.of(
                "title", "Quota & Limits",
                "description", "Monitor quota usage and limits",
                "endpoint", "/api/admin/quota",
                "requireAuth", true,
                "metrics", new String[]{"requests", "storage", "bandwidth", "computeTime"}
            )
        ));
        
        // 22. System Resilience (Flutter)
        components.add(component(
            "resilience",
            "System Resilience",
            "🛡️",
            "security",
            true,
            Map.of(
                "title", "System Healing & Resilience",
                "description", "Monitor and manage system recovery",
                "endpoint", "/api/admin/resilience",
                "requireAuth", true,
                "features", new String[]{"auto-healing", "failover", "recovery", "health-check"}
            )
        ));
        
        // 23. ML Intelligence (Flutter)
        components.add(component(
            "ml-intelligence",
            "ML Intelligence",
            "🤖",
            "analytics",
            true,
            Map.of(
                "title", "ML Anomaly Detection",
                "description", "Anomaly detection and ML insights",
                "endpoint", "/api/admin/ml-intelligence",
                "requireAuth", true,
                "types", new String[]{"anomalies", "predictions", "patterns", "alerts"}
            )
        ));
        
        return components;
    }

    /**
     * All API endpoints - centralized configuration
     */
    private Map<String, Object> buildApiEndpoints() {
        Map<String, Object> endpoints = new LinkedHashMap<>();
        
        endpoints.put("dashboard", Map.of(
            "stats", "/api/admin/dashboard/stats",
            "health", "/api/admin/dashboard/health",
            "contract", "/api/admin/dashboard/contract"
        ));
        
        endpoints.put("control", Map.of(
            "status", "/api/admin/control",
            "mode", "/api/admin/control/mode",
            "stop", "/api/admin/control/stop",
            "resume", "/api/admin/control/resume",
            "pending", "/api/admin/control/pending",
            "history", "/api/admin/control/history"
        ));
        
        endpoints.put("features", Map.of(
            "api", "/api/admin/api-management",
            "models", "/api/admin/ai-models",
            "vpn", "/api/admin/vpn",
            "chat", "/api/chat",
            "assignment", "/api/admin/ai-assignment",
            "voting", "/api/consensus",
            "progress", "/api/admin/progress",
            "learning", "/api/learning"
        ));
        
        return endpoints;
    }

    // ========== HELPER METHODS ==========
    
    private Map<String, Object> menuItem(String key, String label, String icon, boolean enabled, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("label", label);
        item.put("icon", icon);
        item.put("enabled", enabled);
        item.put("description", description);
        return item;
    }

    private Map<String, Object> component(String key, String label, String icon, String category, 
                                          boolean enabled, Map<String, Object> config) {
        Map<String, Object> comp = new LinkedHashMap<>();
        comp.put("key", key);
        comp.put("label", label);
        comp.put("icon", icon);
        comp.put("category", category);
        comp.put("enabled", enabled);
        comp.put("config", config);
        return comp;
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

    private String buildHealthStatusReason(double systemHealthScore, int activeAIAgents, int runningTasks, int completedTasks, double successRate) {
        StringBuilder reason = new StringBuilder();
        
        if (systemHealthScore >= 90) {
            reason.append("All systems operating normally");
        } else if (systemHealthScore >= 70) {
            reason.append("⚠️ System running below optimal performance: ");
            List<String> issues = new ArrayList<>();
            
            if (activeAIAgents < 3) {
                issues.add("only " + activeAIAgents + " AI agent(s) active");
            }
            if (runningTasks == 0 && completedTasks > 0) {
                issues.add("no active tasks");
            }
            if (successRate < 80) {
                issues.add("success rate at " + Math.round(successRate) + "%");
            }
            if (systemHealthScore < 75) {
                issues.add("health score degrading");
            }
            
            reason.append(String.join(", ", issues));
        } else {
            reason.append("🔴 CRITICAL: System health critical: ");
            List<String> issues = new ArrayList<>();
            
            if (activeAIAgents == 0) {
                issues.add("no AI agents available");
            }
            if (runningTasks == 0 && completedTasks == 0) {
                issues.add("no tasks executing");
            }
            if (successRate < 50) {
                issues.add("low success rate (" + Math.round(successRate) + "%)");
            }
            if (systemHealthScore < 50) {
                issues.add("severe health degradation");
            }
            
            reason.append(String.join(", ", issues));
        }
        
        return reason.toString();
    }

    private String formatUptime(long uptimeMs) {
        long totalSeconds = Math.max(0, uptimeMs / 1000);
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return days + "d " + hours + "h " + minutes + "m";
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
}
