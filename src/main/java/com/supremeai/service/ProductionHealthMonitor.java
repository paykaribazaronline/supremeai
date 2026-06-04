package com.supremeai.service;

import com.supremeai.mcp.MCPClientManager;
import com.supremeai.model.MonitoringLog;
import com.supremeai.repository.MonitoringLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Production Health Monitoring Service - Phase 4
 * Tracks system health, API provider status, and performance metrics.
 * Provides real-time monitoring data for dashboard and alerting.
 */
@Service
public class ProductionHealthMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ProductionHealthMonitor.class);

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);

    private final Map<String, AtomicLong> providerErrors = new ConcurrentHashMap<>();
    private final Map<String, Long> providerLastCheck = new ConcurrentHashMap<>();
    private final Map<String, String> providerStatus = new ConcurrentHashMap<>();

    private LocalDateTime lastHealthCheck;
    private LocalDateTime lastDeploymentCheck;
    private double cpuUsage;
    private double memoryUsage;
    private int activeSessions;
    private long uptimeSeconds;
    private String overallStatus = "HEALTHY";

    @Autowired(required = false)
    private MCPClientManager mcpClientManager;

    @Autowired
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MonitoringLogRepository monitoringLogRepository;

    public ProductionHealthMonitor(io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.uptimeSeconds = 0;
        this.lastHealthCheck = LocalDateTime.now();
        this.lastDeploymentCheck = LocalDateTime.now();
        
        // Initialize Micrometer gauges
        meterRegistry.gauge("supremeai.health.status", this, monitor -> 
            "HEALTHY".equals(monitor.overallStatus) ? 1.0 : 0.0);
        meterRegistry.gauge("supremeai.system.cpu", this, monitor -> monitor.cpuUsage);
        meterRegistry.gauge("supremeai.system.memory", this, monitor -> monitor.memoryUsage);
    }

    /**
     * Record a successful request
     */
    public void recordSuccess(String provider, long responseTimeMs) {
        successfulRequests.incrementAndGet();
        totalRequests.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
        providerLastCheck.put(provider, System.currentTimeMillis());
        providerStatus.put(provider, "HEALTHY");
        
        // Micrometer metrics
        meterRegistry.counter("supremeai.requests.total", "status", "success", "provider", provider).increment();
        meterRegistry.timer("supremeai.requests.latency", "provider", provider)
            .record(responseTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Record a failed request
     */
    public void recordFailure(String provider, String error) {
        failedRequests.incrementAndGet();
        totalRequests.incrementAndGet();
        providerErrors.computeIfAbsent(provider, k -> new AtomicLong(0)).incrementAndGet();
        providerStatus.put(provider, "ERROR: " + error);
        logger.warn("Provider {} failed: {}", provider, error);
        
        // Log to monitoring dashboard
        logEvent("ERROR", "PROVIDER", "Provider " + provider + " failure: " + error);
        
        // Micrometer metrics
        meterRegistry.counter("supremeai.requests.total", "status", "failure", "provider", provider, "error", error).increment();
    }

    /**
     * Get current health status
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", LocalDateTime.now().toString());
        status.put("overallStatus", overallStatus);
        status.put("uptimeSeconds", uptimeSeconds);

        // Request metrics
        long total = totalRequests.get();
        long success = successfulRequests.get();
        long failed = failedRequests.get();
        double successRate = total > 0 ? (double) success / total * 100 : 100.0;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalRequests", total);
        metrics.put("successfulRequests", success);
        metrics.put("failedRequests", failed);
        metrics.put("successRate", Math.round(successRate * 100) / 100.0);
        metrics.put("avgResponseTimeMs", total > 0 ? totalResponseTime.get() / total : 0);
        metrics.put("cpuUsage", cpuUsage);
        metrics.put("memoryUsage", memoryUsage);
        metrics.put("activeSessions", activeSessions);

        status.put("metrics", metrics);

        // Provider health
        Map<String, Object> providers = new HashMap<>();
        for (Map.Entry<String, String> entry : providerStatus.entrySet()) {
            Map<String, Object> providerInfo = new HashMap<>();
            providerInfo.put("status", entry.getValue());
            providerInfo.put("lastCheck", providerLastCheck.get(entry.getKey()));
            Long errors = providerErrors.getOrDefault(entry.getKey(), new AtomicLong(0)).longValue();
            providerInfo.put("errorCount", errors);
            providers.put(entry.getKey(), providerInfo);
        }
        status.put("providerHealth", providers);

        // Alerts
        status.put("alerts", computeAlerts());

        return status;
    }

    /**
     * Compute alerts based on thresholds
     */
    private java.util.List<Map<String, Object>> computeAlerts() {
        java.util.List<Map<String, Object>> alerts = new java.util.ArrayList<>();

        // Check success rate
        long total = totalRequests.get();
        long success = successfulRequests.get();
        if (total > 100) {
            double rate = (double) success / total * 100;
            if (rate < 95.0) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("severity", "HIGH");
                alert.put("message", "Success rate dropped below 95%: " + Math.round(rate * 100) / 100.0 + "%");
                alert.put("type", "success_rate");
                alerts.add(alert);
            }
        }

        // Check for provider errors
        for (Map.Entry<String, AtomicLong> entry : providerErrors.entrySet()) {
            long errors = entry.getValue().get();
            if (errors > 10) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("severity", "MEDIUM");
                alert.put("message", "Provider " + entry.getKey() + " has " + errors + " consecutive errors");
                alert.put("type", "provider_errors");
                alerts.add(alert);
            }
        }

        // Check response time
        long totalResp = totalResponseTime.get();
        if (total > 0) {
            long avgResponse = totalResp / total;
            if (avgResponse > 500) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("severity", "MEDIUM");
                alert.put("message", "Average response time exceeds 500ms: " + avgResponse + "ms");
                alert.put("type", "response_time");
                alerts.add(alert);
            }
        }

        return alerts;
    }

    /**
     * Update system resource metrics
     */
    public void updateSystemMetrics(double cpu, double memory, int sessions) {
        this.cpuUsage = cpu;
        this.memoryUsage = memory;
        this.activeSessions = sessions;
    }

    /**
     * Update uptime
     */
    @Scheduled(fixedRate = 1000)
    public void updateUptime() {
        uptimeSeconds++;
        // Update overall status
        if (failedRequests.get() > 0 && totalRequests.get() > 0) {
            double rate = (double) successfulRequests.get() / totalRequests.get() * 100;
            String oldStatus = overallStatus;
            if (rate < 90.0) {
                overallStatus = "DEGRADED";
            } else if (rate < 99.0) {
                overallStatus = "WARNING";
            } else {
                overallStatus = "HEALTHY";
            }
            
            if (!oldStatus.equals(overallStatus)) {
                logEvent(overallStatus.equals("HEALTHY") ? "SUCCESS" : "ALERT", "SYSTEM", "Global health status changed to " + overallStatus);
            }
        }
    }

    /**
     * Broadcast metrics to WebSocket for real-time monitoring
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastMetrics() {
        Map<String, Object> health = getHealthStatus();
        Map<String, Object> metrics = (Map<String, Object>) health.get("metrics");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "SYSTEM_RESOURCES");
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("cpuUsagePercentage", metrics.get("cpuUsage"));
        payload.put("memoryUsagePercentage", metrics.get("memoryUsage"));
        payload.put("memoryUsed", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        payload.put("memoryMax", Runtime.getRuntime().maxMemory());
        payload.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        payload.put("overallStatus", overallStatus);
        
        messagingTemplate.convertAndSend("/topic/monitoring", payload);
    }

    /**
     * Log a system event and broadcast to dashboard
     */
    public void logEvent(String level, String component, String message) {
        MonitoringLog log = MonitoringLog.builder()
                .level(level)
                .component(component)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
        
        monitoringLogRepository.save(log).subscribe(saved -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "SYSTEM_LOG");
            payload.put("level", saved.getLevel());
            payload.put("component", saved.getComponent());
            payload.put("message", saved.getMessage());
            payload.put("timestamp", saved.getTimestamp());
            
            messagingTemplate.convertAndSend("/topic/monitoring", payload);
        });
    }

    /**
     * Get simple dashboard summary
     */
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("status", overallStatus);
        summary.put("uptime", formatUptime());
        summary.put("requests", totalRequests.get());
        summary.put("successRate", getSuccessRate());
        summary.put("providerCount", providerStatus.size());
        summary.put("activeUsers", activeSessions);
        return summary;
    }

    private String getSuccessRate() {
        long total = totalRequests.get();
        if (total == 0) return "N/A";
        double rate = (double) successfulRequests.get() / total * 100;
        return Math.round(rate * 100) / 100.0 + "%";
    }

    private String formatUptime() {
        long days = uptimeSeconds / 86400;
        long hours = (uptimeSeconds % 86400) / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;
        return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
    }

    /**
     * Periodic health check of all providers
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void runHealthCheck() {
        logger.info("Running periodic health check...");
        lastHealthCheck = LocalDateTime.now();

        if (mcpClientManager != null) {
            try {
                Map<String, Object> status = Map.of("tools", mcpClientManager.listAllTools());
                logger.debug("MCP servers healthy: {}", status != null);
            } catch (Exception e) {
                logger.warn("MCP health check failed: {}", e.getMessage());
            }
        }
    }

    /**
     * Check if a specific provider is healthy
     */
    public boolean isProviderHealthy(String providerName) {
        String status = providerStatus.get(providerName);
        return status != null && status.equals("HEALTHY");
    }

    /**
     * Reset metrics (for testing/admin)
     */
    public void resetMetrics() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalResponseTime.set(0);
        providerErrors.clear();
        providerStatus.clear();
        providerLastCheck.clear();
        lastHealthCheck = LocalDateTime.now();
    }

    public LocalDateTime getLastHealthCheck() {
        return lastHealthCheck;
    }

    public LocalDateTime getLastDeploymentCheck() {
        return lastDeploymentCheck;
    }

    public void setDeploymentCheckTime() {
        this.lastDeploymentCheck = LocalDateTime.now();
    }
}