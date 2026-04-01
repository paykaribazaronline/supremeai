package org.example.controller;

import org.example.selfhealing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Self-Healing REST Controller
 * 
 * Exposes self-healing diagnostics and management endpoints:
 * - System health status
 * - Service-specific diagnostics
 * - Circuit breaker information
 * - Cache statistics
 * - Recovery triggers
 */
@RestController
@RequestMapping("/api/v1/self-healing")
public class SelfHealingController {
    private static final Logger logger = LoggerFactory.getLogger(SelfHealingController.class);
    
    @Autowired(required = false)
    private SelfHealingService selfHealingService;
    
    /**
     * Get comprehensive system health report
     * GET /api/v1/self-healing/system-health
     */
    @GetMapping("/system-health")
    public ResponseEntity<?> getSystemHealth() {
        if (selfHealingService == null) {
            return ResponseEntity.status(503).body(Map.of(
                "status", "unavailable",
                "message", "Self-Healing Service not initialized",
                "reason", "SelfHealingService bean not available"
            ));
        }
        try {
            SelfHealingService.SystemHealthReport report = selfHealingService.getSystemHealthReport();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("isRunning", report.isRunning);
            response.put("generatedAt", report.generatedAt);
            response.put("circuitBreakers", report.circuitBreakerStates);
            response.put("serviceMetrics", transformMetrics(report.serviceMetrics));
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting system health", e);
            return createErrorResponse("Failed to get system health: " + e.getMessage());
        }
    }
    
    /**
     * Get diagnostics for a specific service
     * GET /api/v1/self-healing/service/{serviceName}
     */
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<?> getServiceDiagnostics(@PathVariable String serviceName) {
        try {
            SelfHealingService.ServiceDiagnostics diag = selfHealingService.getServiceDiagnostics(serviceName);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("serviceName", diag.serviceName);
            response.put("circuitBreaker", diag.circuitBreakerState);
            
            if (diag.metrics != null) {
                response.put("metrics", transformMetric(diag.metrics));
                response.put("history", diag.history);
            }
            
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting service diagnostics", e);
            return createErrorResponse("Failed to get diagnostics for " + serviceName + ": " + e.getMessage());
        }
    }
    
    /**
     * Start self-healing system
     * POST /api/v1/self-healing/start
     */
    @PostMapping("/start")
    public ResponseEntity<?> startSelfHealing() {
        try {
            selfHealingService.start();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Self-healing system started");
            response.put("isRunning", selfHealingService.isRunning());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error starting self-healing", e);
            return createErrorResponse("Failed to start self-healing: " + e.getMessage());
        }
    }
    
    /**
     * Stop self-healing system
     * POST /api/v1/self-healing/stop
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stopSelfHealing() {
        try {
            selfHealingService.stop();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Self-healing system stopped");
            response.put("isRunning", selfHealingService.isRunning());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error stopping self-healing", e);
            return createErrorResponse("Failed to stop self-healing: " + e.getMessage());
        }
    }
    
    /**
     * Trigger recovery for a service
     * POST /api/v1/self-healing/recover/{serviceName}
     */
    @PostMapping("/recover/{serviceName}")
    public ResponseEntity<?> triggerRecovery(@PathVariable String serviceName) {
        try {
            SelfHealingService.ServiceDiagnostics diag = selfHealingService.getServiceDiagnostics(serviceName);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Recovery triggered for " + serviceName);
            response.put("serviceName", serviceName);
            
            if (diag.metrics != null) {
                response.put("healthBefore", transformMetric(diag.metrics));
            }
            
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error triggering recovery", e);
            return createErrorResponse("Failed to trigger recovery: " + e.getMessage());
        }
    }
    
    /**
     * Get circuit breaker status
     * GET /api/v1/self-healing/circuit-breaker/{serviceName}
     */
    @GetMapping("/circuit-breaker/{serviceName}")
    public ResponseEntity<?> getCircuitBreakerStatus(@PathVariable String serviceName) {
        try {
            CircuitBreaker breaker = selfHealingService.getOrCreateCircuitBreaker(serviceName);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("serviceName", serviceName);
            response.put("state", breaker.getState().toString());
            response.put("failureCount", breaker.getFailureCount());
            response.put("successCount", breaker.getSuccessCount());
            response.put("isClosed", breaker.isClosed());
            response.put("isOpen", breaker.isOpen());
            response.put("isHalfOpen", breaker.isHalfOpen());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting circuit breaker status", e);
            return createErrorResponse("Failed to get circuit breaker status: " + e.getMessage());
        }
    }
    
    /**
     * Health check endpoint for self-healing system
     * GET /api/v1/self-healing/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "healthy");
        response.put("component", "SelfHealingService");
        response.put("isRunning", selfHealingService.isRunning());
        response.put("lastCheckTime", selfHealingService.getLastComprehensiveCheckTime());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    // ===== Helper Methods =====
    
    private Map<String, Object> transformMetrics(Map<String, HealthMonitor.HealthMetrics> metrics) {
        Map<String, Object> transformed = new LinkedHashMap<>();
        for (Map.Entry<String, HealthMonitor.HealthMetrics> entry : metrics.entrySet()) {
            transformed.put(entry.getKey(), transformMetric(entry.getValue()));
        }
        return transformed;
    }
    
    private Map<String, Object> transformMetric(HealthMonitor.HealthMetrics metric) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("state", metric.state.toString());
        map.put("totalRequests", metric.totalRequests);
        map.put("failedRequests", metric.failedRequests);
        map.put("errorRate", String.format("%.2f%%", metric.errorRate * 100));
        map.put("avgResponseTimeMs", String.format("%.0f", metric.avgResponseTimeMs));
        map.put("highResponseTimeCount", metric.highResponseTimeCount);
        map.put("consecutiveFailures", metric.consecutiveFailures);
        map.put("timeSinceLastCheckMs", metric.timeSinceLastCheckMs);
        return map;
    }
    
    private ResponseEntity<?> createErrorResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "error");
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.internalServerError().body(response);
    }
}
