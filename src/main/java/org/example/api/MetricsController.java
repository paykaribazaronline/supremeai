package org.example.api;

import org.example.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Phase 4: Metrics REST API
 * Endpoints for monitoring system health and performance in real-time
 */
@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class MetricsController {

    @Autowired
    private MetricsService metricsService;

    /**
     * GET /api/metrics/health
     * Comprehensive system health check with memory, CPU, request stats
     */
    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() {
        return ResponseEntity.ok(metricsService.getSystemHealth());
    }

    /**
     * GET /api/metrics/stats
     * Generation statistics by framework and overall
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getGenerationStats() {
        return ResponseEntity.ok(metricsService.getGenerationStats());
    }

    /**
     * GET /api/metrics/alerts
     * Current system alerts (warnings, errors)
     */
    @GetMapping("/alerts")
    public ResponseEntity<?> getAlerts() {
        List<Map<String, String>> alerts = metricsService.getAlerts();
        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alerts);
        response.put("alert_count", alerts.size());
        response.put("timestamp", new Date().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/metrics/status
     * Quick status check (for load balancers, monitoring tools)
     */
    @GetMapping("/status")
    public ResponseEntity<?> getQuickStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", metricsService.getTotalErrors() == 0 ? "UP" : "DEGRADED");
        status.put("timestamp", new Date());
        return ResponseEntity.ok(status);
    }
}
