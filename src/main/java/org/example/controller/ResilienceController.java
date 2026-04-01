package org.example.controller;

import org.example.config.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Resilience Management Controller
 * Exposes resilience metrics and management endpoints
 * Allows admins to monitor and control rate limiting, circuit breakers, and failover
 */
@RestController
@RequestMapping("/api/resilience")
public class ResilienceController {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilienceController.class);
    
    @Autowired(required = false)
    private RateLimiterConfiguration.RateLimiterService rateLimiterService;
    
    @Autowired(required = false)
    private CircuitBreakerConfiguration.CircuitBreakerService circuitBreakerService;
    
    @Autowired(required = false)
    private FailoverConfiguration.FailoverService failoverService;
    
    @Autowired(required = false)
    private ResilienceWrapperService resilienceWrapperService;
    
    /**
     * ==================== RATE LIMITING ENDPOINTS ====================
     */
    
    /**
     * Get rate limit status for a user
     */
    @GetMapping("/rate-limit/status/{userId}")
    public ResponseEntity<?> getRateLimitStatus(@PathVariable String userId) {
        if (rateLimiterService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Rate limiter not initialized"));
        }
        
        var stats = rateLimiterService.getBucketStats(userId);
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "availableTokens", stats.availableTokens,
            "timeToWaitMs", stats.timeToWaitMs
        ));
    }
    
    /**
     * Get remaining tokens for a user
     */
    @GetMapping("/rate-limit/remaining/{userId}")
    public ResponseEntity<?> getRemainingTokens(@PathVariable String userId) {
        if (rateLimiterService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Rate limiter not initialized"));
        }
        
        long remaining = rateLimiterService.getRemainingTokens(userId);
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "remainingTokens", remaining
        ));
    }
    
    /**
     * Reset rate limit for a user (admin only)
     */
    @PostMapping("/rate-limit/reset/{userId}")
    public ResponseEntity<?> resetRateLimit(@PathVariable String userId) {
        if (rateLimiterService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Rate limiter not initialized"));
        }
        
        rateLimiterService.resetRateLimit(userId);
        logger.info("✅ Rate limit reset for user: {}", userId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Rate limit reset successfully",
            "userId", userId
        ));
    }
    
    /**
     * ==================== CIRCUIT BREAKER ENDPOINTS ====================
     */
    
    /**
     * Get all circuit breaker metrics
     */
    @GetMapping("/circuit-breaker/metrics")
    public ResponseEntity<?> getCircuitBreakerMetrics() {
        if (circuitBreakerService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Circuit breaker service not initialized"));
        }
        
        var allMetrics = circuitBreakerService.getAllMetrics();
        Map<String, Object> response = new LinkedHashMap<>();
        
        allMetrics.forEach((breaker, metrics) -> {
            response.put(breaker, Map.of(
                "successCount", metrics.getSuccessCount(),
                "failureCount", metrics.getFailureCount(),
                "totalCalls", metrics.getTotalCalls(),
                "failureRate", String.format("%.2f%%", metrics.getFailureRate())
            ));
        });
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get metrics for specific circuit breaker
     */
    @GetMapping("/circuit-breaker/metrics/{breakerName}")
    public ResponseEntity<?> getCircuitBreakerMetrics(@PathVariable String breakerName) {
        if (circuitBreakerService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Circuit breaker service not initialized"));
        }
        
        var metrics = circuitBreakerService.getMetrics(breakerName);
        return ResponseEntity.ok(Map.of(
            "breakerName", breakerName,
            "successCount", metrics.getSuccessCount(),
            "failureCount", metrics.getFailureCount(),
            "totalCalls", metrics.getTotalCalls(),
            "failureRate", String.format("%.2f%%", metrics.getFailureRate())
        ));
    }
    
    /**
     * Reset circuit breaker metrics (admin only)
     */
    @PostMapping("/circuit-breaker/reset/{breakerName}")
    public ResponseEntity<?> resetCircuitBreakerMetrics(@PathVariable String breakerName) {
        if (circuitBreakerService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Circuit breaker service not initialized"));
        }
        
        circuitBreakerService.resetMetrics(breakerName);
        logger.info("✅ Circuit breaker metrics reset: {}", breakerName);
        
        return ResponseEntity.ok(Map.of(
            "message", "Circuit breaker metrics reset successfully",
            "breakerName", breakerName
        ));
    }
    
    /**
     * Get circuit breaker state
     */
    @GetMapping("/circuit-breaker/state/{breakerName}")
    public ResponseEntity<?> getCircuitBreakerState(@PathVariable String breakerName) {
        if (resilienceWrapperService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Resilience wrapper not initialized"));
        }
        
        String state = resilienceWrapperService.getCircuitBreakerState(breakerName);
        return ResponseEntity.ok(Map.of(
            "breakerName", breakerName,
            "state", state,
            "isOpen", state.equals("OPEN")
        ));
    }
    
    /**
     * ==================== FAILOVER ENDPOINTS ====================
     */
    
    /**
     * Get failover group status
     */
    @GetMapping("/failover/group/{groupName}")
    public ResponseEntity<?> getFailoverGroupStatus(@PathVariable String groupName) {
        if (failoverService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Failover service not initialized"));
        }
        
        var groupStatus = failoverService.getGroupStatus(groupName);
        if (groupStatus == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
            "groupName", groupStatus.groupName,
            "healthyCount", groupStatus.healthyCount,
            "unhealthyCount", groupStatus.unhealthyCount,
            "totalCount", groupStatus.totalCount,
            "healthPercentage", String.format("%.1f%%", (double) groupStatus.healthyCount / groupStatus.totalCount * 100)
        ));
    }
    
    /**
     * Get all endpoint statuses
     */
    @GetMapping("/failover/endpoints")
    public ResponseEntity<?> getAllEndpointStatuses() {
        if (failoverService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Failover service not initialized"));
        }
        
        var statuses = failoverService.getAllEndpointStatuses();
        return ResponseEntity.ok(statuses);
    }
    
    /**
     * Get healthy endpoint for failover group
     */
    @GetMapping("/failover/healthy/{groupName}")
    public ResponseEntity<?> getHealthyEndpoint(@PathVariable String groupName) {
        if (failoverService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Failover service not initialized"));
        }
        
        String endpoint = failoverService.getHealthyEndpoint(groupName);
        if (endpoint == null) {
            return ResponseEntity.status(503).body(Map.of("error", "No healthy endpoints available"));
        }
        
        return ResponseEntity.ok(Map.of(
            "groupName", groupName,
            "healthyEndpoint", endpoint
        ));
    }
    
    /**
     * Mark endpoint as healthy (admin only)
     */
    @PostMapping("/failover/endpoint/healthy")
    public ResponseEntity<?> markEndpointHealthy(@RequestParam String endpoint) {
        if (failoverService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Failover service not initialized"));
        }
        
        failoverService.markEndpointHealthy(endpoint);
        logger.info("✅ Endpoint marked healthy: {}", endpoint);
        
        return ResponseEntity.ok(Map.of(
            "message", "Endpoint marked healthy",
            "endpoint", endpoint
        ));
    }
    
    /**
     * Mark endpoint as unhealthy (admin only)
     */
    @PostMapping("/failover/endpoint/unhealthy")
    public ResponseEntity<?> markEndpointUnhealthy(
            @RequestParam String endpoint,
            @RequestParam(defaultValue = "Manual action") String reason
    ) {
        if (failoverService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Failover service not initialized"));
        }
        
        failoverService.markEndpointUnhealthy(endpoint, reason);
        logger.info("⚠️ Endpoint marked unhealthy: {} - {}", endpoint, reason);
        
        return ResponseEntity.ok(Map.of(
            "message", "Endpoint marked unhealthy",
            "endpoint", endpoint,
            "reason", reason
        ));
    }
    
    /**
     * ==================== HEALTH & STATUS ENDPOINTS ====================
     */
    
    /**
     * Get overall resilience status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getResilienceStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        
        // Rate limiter status
        status.put("rateLimiter", Map.of(
            "enabled", rateLimiterService != null,
            "status", rateLimiterService != null ? "ACTIVE" : "DISABLED"
        ));
        
        // Circuit breaker status
        Map<String, Object> circuitBreakerStatus = new LinkedHashMap<>();
        if (circuitBreakerService != null) {
            var allMetrics = circuitBreakerService.getAllMetrics();
            circuitBreakerStatus.put("enabled", true);
            circuitBreakerStatus.put("activeBreakers", allMetrics.size());
            circuitBreakerStatus.put("breakers", allMetrics.keySet());
        } else {
            circuitBreakerStatus.put("enabled", false);
            circuitBreakerStatus.put("status", "DISABLED");
        }
        status.put("circuitBreaker", circuitBreakerStatus);
        
        // Failover status
        status.put("failover", Map.of(
            "enabled", failoverService != null,
            "status", failoverService != null ? "ACTIVE" : "DISABLED"
        ));
        
        return ResponseEntity.ok(status);
    }
}
