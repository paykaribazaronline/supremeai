package org.example.api;

import org.example.service.AlertingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Phase 4: Alerting REST API
 * Endpoints for managing and monitoring system alerts
 */
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertingController {

    @Autowired
    private AlertingService alertingService;

    /**
     * GET /api/alerts
     * Get all active alerts
     */
    @GetMapping
    public ResponseEntity<?> getActiveAlerts() {
        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alertingService.getActiveAlerts());
        response.put("count", alertingService.getActiveAlertCount());
        response.put("critical_count", alertingService.getCriticalAlertCount());
        response.put("timestamp", new Date());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/alerts/{severity}
     * Get alerts by severity (INFO, WARNING, ERROR, CRITICAL)
     */
    @GetMapping("/{severity}")
    public ResponseEntity<?> getAlertsBySeverity(@PathVariable String severity) {
        try {
            AlertingService.AlertSeverity sev = AlertingService.AlertSeverity.valueOf(severity.toUpperCase());
            List<AlertingService.Alert> alerts = alertingService.getAlertsBySeverity(sev);
            Map<String, Object> response = new HashMap<>();
            response.put("severity", severity);
            response.put("alerts", alerts);
            response.put("count", alerts.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Collections.singletonMap("error", "Invalid severity: " + severity));
        }
    }

    /**
     * GET /api/alerts/history
     * Get alert history (all past alerts)
     */
    @GetMapping("/history/all")
    public ResponseEntity<?> getAlertHistory() {
        Map<String, Object> response = new HashMap<>();
        response.put("history", alertingService.getAlertHistory());
        response.put("count", alertingService.getAlertHistory().size());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/alerts/history/recent?limit=10
     * Get recent alerts
     */
    @GetMapping("/history/recent")
    public ResponseEntity<?> getRecentAlerts(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        response.put("alerts", alertingService.getRecentAlerts(limit));
        response.put("limit", limit);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/alerts/{alertId}/resolve
     * Resolve an alert
     */
    @PostMapping("/{alertId}/resolve")
    public ResponseEntity<?> resolveAlert(@PathVariable String alertId) {
        alertingService.resolveAlert(alertId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Alert resolved");
        response.put("alert_id", alertId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/alerts/stats
     * Get alert statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getAlertStats() {
        return ResponseEntity.ok(alertingService.getAlertStats());
    }

    /**
     * POST /api/alerts/create
     * Create a manual alert (for testing)
     */
    @PostMapping("/create")
    public ResponseEntity<?> createAlert(@RequestBody Map<String, String> request) {
        String severity = request.getOrDefault("severity", "WARNING");
        String title = request.get("title");
        String message = request.get("message");

        if (title == null || message == null) {
            return ResponseEntity.badRequest()
                .body(Collections.singletonMap("error", "title and message required"));
        }

        try {
            AlertingService.AlertSeverity sev = AlertingService.AlertSeverity.valueOf(severity.toUpperCase());
            AlertingService.Alert alert = alertingService.createAlert(sev, title, message);
            return ResponseEntity.ok(alert);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Collections.singletonMap("error", "Invalid severity: " + severity));
        }
    }
}
