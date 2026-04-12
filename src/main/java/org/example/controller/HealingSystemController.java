package org.example.controller;

import org.example.selfhealing.healing.*;
import org.example.selfhealing.domain.HealingAttempt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Safe Healing Loop REST API
 * 
 * Endpoints for monitoring and controlling the self-healing system:
 * - GET  /api/healing/status - System overview
 * - GET  /api/healing/watchdog - Watchdog health
 * - GET  /api/healing/circuit-breaker/{workflowId} - CB status
 * - GET  /api/healing/rate-limit - GitHub API rate limit
 * - GET  /api/healing/attempts/{workflowId} - Attempt history
 * - POST /api/healing/disable - Admin: Disable healing
 * - POST /api/healing/enable - Admin: Re-enable healing
 * 
 * All endpoints require admin authentication
 */
@RestController
@RequestMapping("/api/healing")
@CrossOrigin(origins = "*")
public class HealingSystemController {
    private static final Logger logger = LoggerFactory.getLogger(HealingSystemController.class);
    
    @Autowired(required = false)
    private SafeInfiniteHealingLoop healingLoop;
    
    @Autowired(required = false)
    private HealingCircuitBreaker circuitBreaker;
    
    @Autowired(required = false)
    private HealingStateManager stateManager;
    
    @Autowired(required = false)
    private GitHubRateLimiter rateLimiter;
    
    @Autowired(required = false)
    private SupremeAIHealingWatchdog watchdog;
    
    /**
     * GET /api/healing/status
     * 
     * System overview - key metrics
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getHealingStatus() {
        logger.info("GET /api/healing/status");
        
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("timestamp", System.currentTimeMillis());
        
        // Watchdog status
        if (watchdog != null) {
            status.put("watchdog", watchdog.getWatchdogStatus());
        }
        
        // Rate limiter status
        if (rateLimiter != null) {
            status.put("rateLimiter", rateLimiter.getStatus());
        }
        
        // Recent stats
        if (stateManager != null) {
            status.put("recentStats", stateManager.getAttemptStats(60)); // Last hour
            status.put("commonErrors", stateManager.getMostCommonErrors(5));
            status.put("bestStrategies", stateManager.getBestFixStrategies());
        }
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * GET /api/healing/watchdog
     * 
     * Watchdog health check
     */
    @GetMapping("/watchdog")
    public ResponseEntity<Map<String, Object>> getWatchdogStatus() {
        logger.info("GET /api/healing/watchdog");
        
        if (watchdog == null) {
            return ResponseEntity.ok(Map.of("error", "Watchdog not available"));
        }
        
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("health", watchdog.healthCheck() ? "HEALTHY" : "UNHEALTHY");
        status.put("status", watchdog.getWatchdogStatus());
        status.put("autoHealingEnabled", watchdog.isAutoHealingEnabled());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * GET /api/healing/circuit-breaker/{workflowId}
     * 
     * Check circuit breaker status for a specific workflow
     */
    @GetMapping("/circuit-breaker/{workflowId}")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus(
            @PathVariable String workflowId) {
        logger.info("GET /api/healing/circuit-breaker/{}", workflowId);
        
        if (circuitBreaker == null) {
            return ResponseEntity.ok(Map.of("error", "Circuit breaker not available"));
        }
        
        return ResponseEntity.ok(circuitBreaker.getStatus(workflowId));
    }
    
    /**
     * GET /api/healing/rate-limit
     * 
     * GitHub API rate limit status
     */
    @GetMapping("/rate-limit")
    public ResponseEntity<Map<String, Object>> getRateLimit() {
        logger.info("GET /api/healing/rate-limit");
        
        if (rateLimiter == null) {
            return ResponseEntity.ok(Map.of("error", "Rate limiter not available"));
        }
        
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("rateLimitStatus", rateLimiter.getStatus());
        status.put("isAtRisk", rateLimiter.isAtRisk());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * GET /api/healing/attempts/{workflowId}
     * 
     * Get healing attempt history for a workflow
     */
    @GetMapping("/attempts/{workflowId}")
    public ResponseEntity<Map<String, Object>> getAttemptHistory(
            @PathVariable String workflowId,
            @RequestParam(defaultValue = "10") int limit) {
        logger.info("GET /api/healing/attempts/{}", workflowId);
        
        if (stateManager == null) {
            return ResponseEntity.ok(Map.of("error", "State manager not available"));
        }
        
        List<HealingAttempt> attempts = stateManager.getWorkflowHistory(workflowId);
        
        // Limit results
        List<HealingAttempt> limited = attempts.stream()
                .limit(limit)
                .toList();
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workflowId", workflowId);
        result.put("totalAttempts", attempts.size());
        result.put("recentAttempts", limited);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * POST /api/healing/disable
     * 
     * Admin action: Disable auto-healing
     */
    @PostMapping("/disable")
    public ResponseEntity<Map<String, Object>> disableHealing() {
        logger.warn("POST /api/healing/disable - DISABLED by admin");
        
        if (watchdog == null) {
            return ResponseEntity.ok(Map.of("error", "Watchdog not available"));
        }
        
        watchdog.disableAutoHealing();
        
        return ResponseEntity.ok(Map.of(
            "message", "Auto-healing DISABLED",
            "autoHealingEnabled", watchdog.isAutoHealingEnabled()
        ));
    }
    
    /**
     * POST /api/healing/enable
     * 
     * Admin action: Re-enable auto-healing
     */
    @PostMapping("/enable")
    public ResponseEntity<Map<String, Object>> enableHealing() {
        logger.info("POST /api/healing/enable - ENABLED by admin");
        
        if (watchdog == null) {
            return ResponseEntity.ok(Map.of("error", "Watchdog not available"));
        }
        
        watchdog.enableAutoHealing();
        
        return ResponseEntity.ok(Map.of(
            "message", "Auto-healing RE-ENABLED",
            "autoHealingEnabled", watchdog.isAutoHealingEnabled()
        ));
    }
    
    /**
     * POST /api/healing/retry/{workflowId}
     * 
     * Admin action: Manually retry healing for a workflow
     */
    @PostMapping("/retry/{workflowId}")
    public ResponseEntity<Map<String, Object>> retryHealing(
            @PathVariable String workflowId) {
        logger.info("POST /api/healing/retry/{}", workflowId);
        
        if (circuitBreaker == null) {
            return ResponseEntity.ok(Map.of("error", "Circuit breaker not available"));
        }
        
        // Reset circuit breaker for manual retry
        circuitBreaker.recordSuccess(workflowId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Circuit breaker reset for manual retry",
            "workflowId", workflowId
        ));
    }
    
    /**
     * GET /api/healing/diagnostics
     * 
     * Full system diagnostics for troubleshooting
     */
    @GetMapping("/diagnostics")
    public ResponseEntity<Map<String, Object>> getDiagnostics() {
        logger.info("GET /api/healing/diagnostics");
        
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("timestamp", System.currentTimeMillis());
        
        // Watchdog
        if (watchdog != null) {
            diagnostics.put("watchdog", watchdog.getWatchdogStatus());
            diagnostics.put("healingEnabled", watchdog.isAutoHealingEnabled());
        }
        
        // Rate limiter
        if (rateLimiter != null) {
            diagnostics.put("rateLimit", rateLimiter.getStatus());
        }
        
        // Recent attempts
        if (stateManager != null) {
            diagnostics.put("last1HourStats", stateManager.getAttemptStats(60));
            diagnostics.put("last10MinStats", stateManager.getAttemptStats(10));
            diagnostics.put("recentAttempts", stateManager.getRecentAttempts(60));
        }
        
        return ResponseEntity.ok(diagnostics);
    }
    
    /**
     * Health check endpoint (for monitoring/K8s)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        if (watchdog == null) {
            return ResponseEntity.status(503).body(Map.of("status", "unavailable"));
        }
        
        boolean healthy = watchdog.healthCheck();
        int status = healthy ? 200 : 503;
        
        return ResponseEntity.status(status).body(Map.of(
            "status", healthy ? "healthy" : "unhealthy",
            "autoHealingEnabled", watchdog.isAutoHealingEnabled()
        ));
    }
}
