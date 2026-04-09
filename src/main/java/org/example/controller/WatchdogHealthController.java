package org.example.controller;

import org.example.service.SupremeAIWatchdog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FIXED: External Watchdog Health Monitoring
 *
 * Issue #6: AI Brain dies = blind (no external monitoring)
 * Solution: REST endpoint for external health checks
 *
 * This endpoint is intentionally SIMPLE and STANDALONE:
 * - No dependencies on AI logic
 * - Can be monitored by external uptime monitors (Uptime Robot, New Relic, etc.)
 * - Provides status even if SupremeAI core is degraded
 * - No authentication required (health checks need to succeed even if auth is broken)
 */
@RestController
@RequestMapping("/api/health")
public class WatchdogHealthController {

    private static final Logger logger = LoggerFactory.getLogger(WatchdogHealthController.class);

    @Autowired
    private SupremeAIWatchdog watchdog;

    /**
     * EXTERNAL HEALTH CHECK ENDPOINT
     * Can be monitored by: Uptime Robot, New Relic, CloudFlare, Pingdom, etc.
     * Returns: HTTP 200 = healthy, HTTP 503 = unhealthy
     */
    @GetMapping("/status")
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = watchdog.getStatus();
        boolean isHealthy = (boolean) status.getOrDefault("watchdogHealthy", false);

        // HTTP 200 = healthy, HTTP 503 = service unavailable
        if (!isHealthy) {
            logger.warn("🚨 Watchdog reports unhealthy status");
        }

        return Map.of(
            "timestamp", System.currentTimeMillis(),
            "watchdog_healthy", status.get("watchdogHealthy"),
            "safe_mode_active", status.get("safeModeActive"),
            "last_brain_health", status.get("lastKnownHealth"),
            "consecutive_failures", status.get("consecutiveFailures"),
            "total_checks", status.get("totalChecks"),
            "status", isHealthy ? "HEALTHY" : "UNHEALTHY"
        );
    }

    /**
     * Simple HTTP 200/503 response for uptime monitors
     * No JSON parsing needed
     */
    @GetMapping("/live")
    public String liveness() {
        Map<String, Object> status = watchdog.getStatus();
        boolean isHealthy = (boolean) status.getOrDefault("watchdogHealthy", false);
        return isHealthy ? "OK" : "UNHEALTHY";
    }

    /**
     * Detailed readiness check for orchestrators (K8s, etc.)
     */
    @GetMapping("/ready")
    public Map<String, Object> readiness() {
        Map<String, Object> status = watchdog.getStatus();
        boolean safeModeActive = (boolean) status.getOrDefault("safeModeActive", false);

        return Map.of(
            "ready", !safeModeActive,
            "reason", safeModeActive ? "System in safe mode" : "System ready for requests"
        );
    }

    /**
     * Detailed metrics for monitoring dashboards
     */
    @GetMapping("/metrics")
    public Map<String, Object> getDetailedMetrics() {
        return watchdog.getStatus();
    }
}
