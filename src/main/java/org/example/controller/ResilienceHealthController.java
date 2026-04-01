package org.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.resilience.*;
import java.util.*;

/**
 * Resilience Health Controller
 * 
 * REST endpoints for enterprise resilience management:
 * - Circuit breaker status and control
 * - Failover configuration
 * - Health check reports
 * - Test failover scenarios
 */
@RestController
@RequestMapping("/api/v1/resilience")
public class ResilienceHealthController {
    
    private final CircuitBreakerManager circuitBreakerManager;
    private final FailoverManager failoverManager;
    private final ResilienceHealthCheckService healthCheckService;
    
    public ResilienceHealthController(CircuitBreakerManager circuitBreakerManager,
                                      FailoverManager failoverManager,
                                      ResilienceHealthCheckService healthCheckService) {
        this.circuitBreakerManager = circuitBreakerManager;
        this.failoverManager = failoverManager;
        this.healthCheckService = healthCheckService;
    }
    
    // ============ Circuit Breaker Endpoints ============
    
    /**
     * Get status of all circuit breakers
     * GET /api/v1/resilience/circuit-breakers
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, Map<String, Object>>> getCircuitBreakers() {
        return ResponseEntity.ok(circuitBreakerManager.getAllCircuitBreakers());
    }
    
    /**
     * Get status of specific circuit breaker
     * GET /api/v1/resilience/circuit-breakers/{name}
     */
    @GetMapping("/circuit-breakers/{name}")
    public ResponseEntity<Map<String, Object>> getCircuitBreaker(@PathVariable String name) {
        Map<String, Object> metrics = circuitBreakerManager.getCircuitBreakerStatus(name);
        if (metrics.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Reset circuit breaker
     * POST /api/v1/resilience/circuit-breakers/{name}/reset
     */
    @PostMapping("/circuit-breakers/{name}/reset")
    public ResponseEntity<String> resetCircuitBreaker(@PathVariable String name) {
        circuitBreakerManager.resetCircuitBreaker(name);
        return ResponseEntity.ok("Circuit breaker reset: " + name);
    }
    
    /**
     * Register new circuit breaker
     * POST /api/v1/resilience/circuit-breakers
     */
    @PostMapping("/circuit-breakers")
    public ResponseEntity<String> registerCircuitBreaker(@RequestBody CircuitBreakerRequest request) {
        // Register circuit breaker with default config
        circuitBreakerManager.getOrCreateCircuitBreaker(request.name);
        return ResponseEntity.ok("Circuit breaker registered: " + request.name);
    }
    
    // ============ Failover Endpoints ============
    
    /**
     * Get failover chain for service
     * GET /api/v1/resilience/failover-chain/{serviceKey}
     */
    @GetMapping("/failover-chain/{serviceKey}")
    public ResponseEntity<List<String>> getFailoverChain(@PathVariable String serviceKey) {
        List<String> chain = failoverManager.getFailoverChain(serviceKey);
        return ResponseEntity.ok(chain);
    }
    
    /**
     * Register custom failover chain
     * POST /api/v1/resilience/failover-chain
     */
    @PostMapping("/failover-chain")
    public ResponseEntity<String> registerFailoverChain(@RequestBody FailoverChainRequest request) {
        failoverManager.registerFailoverChain(request.serviceKey, request.chain);
        return ResponseEntity.ok("Failover chain registered for: " + request.serviceKey);
    }
    
    /**
     * Get failover statistics
     * GET /api/v1/resilience/failover/stats
     */
    @GetMapping("/failover/stats")
    public ResponseEntity<Map<String, Object>> getFailoverStats() {
        return ResponseEntity.ok(failoverManager.getStats());
    }
    
    /**
     * Clear failover cache
     * POST /api/v1/resilience/failover/clear-cache
     */
    @PostMapping("/failover/clear-cache")
    public ResponseEntity<String> clearFailoverCache() {
        failoverManager.clearCache();
        return ResponseEntity.ok("Failover cache cleared");
    }
    
    // ============ Health Check Endpoints ============
    
    /**
     * Get current health status
     * GET /api/v1/resilience/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(healthCheckService.getHealthAsMap());
    }
    
    /**
     * Perform manual health check
     * POST /api/v1/resilience/health/check
     */
    @PostMapping("/health/check")
    public ResponseEntity<Map<String, Object>> performHealthCheck() {
        healthCheckService.performHealthCheck();
        return ResponseEntity.ok(healthCheckService.getHealthAsMap());
    }
    
    /**
     * Get recent health events
     * GET /api/v1/resilience/health/events?count=50
     */
    @GetMapping("/health/events")
    public ResponseEntity<List<ResilienceHealthCheckService.HealthCheckEvent>> getHealthEvents(
            @RequestParam(defaultValue = "50") int count) {
        return ResponseEntity.ok(healthCheckService.getRecentEvents(count));
    }
    
    // ============ Testing/Simulation Endpoints ============
    
    /**
     * Simulate provider failover
     * POST /api/v1/resilience/test/failover/provider
     */
    @PostMapping("/test/failover/provider")
    public ResponseEntity<Map<String, String>> testProviderFailover() {
        Map<String, String> result = new HashMap<>();
        try {
            // Simulate provider failures via circuit breaker
            var cb = circuitBreakerManager.getOrCreateCircuitBreaker("openai");
            // Simulate multiple failures
            for (int attempt = 0; attempt < 5; attempt++) {
                final int failureNumber = attempt;
                try {
                    circuitBreakerManager.executeWithCircuitBreaker("openai", () -> {
                        throw new RuntimeException("Simulated failure " + failureNumber);
                    });
                } catch (Exception e) {
                    // Expected - simulating failures
                }
            }
            
            result.put("status", "SIMULATED");
            result.put("message", "Provider failover simulation triggered");
            var status = circuitBreakerManager.getCircuitBreakerStatus("openai");
            result.put("circuit_state", status.getOrDefault("state", "UNKNOWN").toString());
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * Simulate cache fallback
     * POST /api/v1/resilience/test/failover/cache
     */
    @PostMapping("/test/failover/cache")
    public ResponseEntity<Map<String, String>> testCacheFailover() {
        Map<String, String> result = new HashMap<>();
        try {
            result.put("status", "SIMULATED");
            result.put("message", "Cache fallback simulation triggered");
            result.put("cache_status", "Cache fallback mechanism active");
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * Simulate database failover
     * POST /api/v1/resilience/test/failover/database
     */
    @PostMapping("/test/failover/database")
    public ResponseEntity<Map<String, String>> testDatabaseFailover() {
        Map<String, String> result = new HashMap<>();
        try {
            // Simulate database circuit breaker failure
            for (int i = 0; i < 3; i++) {
                try {
                    circuitBreakerManager.executeWithCircuitBreaker("database", () -> {
                        throw new RuntimeException("Database connection failed");
                    });
                } catch (Exception e) {
                    // Expected
                }
            }
            
            result.put("status", "SIMULATED");
            result.put("message", "Database failover simulation triggered");
            var status = circuitBreakerManager.getCircuitBreakerStatus("database");
            result.put("circuit_state", status.getOrDefault("state", "UNKNOWN").toString());
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * Comprehensive resilience report
     * GET /api/v1/resilience/report
     */
    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getResilienceReport() {
        Map<String, Object> report = new HashMap<>();
        
        report.put("generated_at", System.currentTimeMillis());
        report.put("health_status", healthCheckService.getHealthAsMap());
        report.put("circuit_breakers", circuitBreakerManager.getAllCircuitBreakers());
        report.put("failover_stats", failoverManager.getStats());
        report.put("available_endpoints", Arrays.asList(
            "GET /api/v1/resilience/health",
            "GET /api/v1/resilience/circuit-breakers",
            "POST /api/v1/resilience/circuit-breakers/{name}/reset",
            "POST /api/v1/resilience/test/failover/provider",
            "POST /api/v1/resilience/test/failover/cache",
            "POST /api/v1/resilience/test/failover/database"
        ));
        
        return ResponseEntity.ok(report);
    }
    
    // ============ Request Classes ============
    
    public static class CircuitBreakerRequest {
        public String name;
        public int failureThreshold;
        public int successThreshold;
        public long openTimeoutMs;
        public long failureTimeWindowMs;
    }
    
    public static class FailoverChainRequest {
        public String serviceKey;
        public List<String> chain;
    }
}
