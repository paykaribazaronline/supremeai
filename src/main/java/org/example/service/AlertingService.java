package org.example.service;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Phase 4: Alerting Service
 * Monitors system metrics and triggers alerts when thresholds are exceeded
 * Supports webhook notifications and internal alert queues
 */
@Service
public class AlertingService {

    public enum AlertSeverity { INFO, WARNING, ERROR, CRITICAL }

    public static class Alert {
        public String id;
        public AlertSeverity severity;
        public String title;
        public String message;
        public Instant timestamp;
        public Map<String, Object> metadata;
        public boolean resolved;

        public Alert(AlertSeverity severity, String title, String message) {
            this.id = UUID.randomUUID().toString();
            this.severity = severity;
            this.title = title;
            this.message = message;
            this.timestamp = Instant.now();
            this.metadata = new HashMap<>();
            this.resolved = false;
        }
    }

    private final Queue<Alert> alertQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, Alert> activeAlerts = new ConcurrentHashMap<>();
    private final List<Alert> alertHistory = Collections.synchronizedList(new ArrayList<>());
    private final int MAX_HISTORY = 500;

    // Thresholds
    private final double MEMORY_THRESHOLD = 85.0; // 85%
    private final double ERROR_RATE_THRESHOLD = 10.0; // 10%
    private final long RESPONSE_TIME_THRESHOLD = 5000; // 5 seconds

    /**
     * Create and queue an alert
     */
    public Alert createAlert(AlertSeverity severity, String title, String message) {
        Alert alert = new Alert(severity, title, message);
        alertQueue.offer(alert);
        activeAlerts.put(alert.id, alert);
        addToHistory(alert);
        return alert;
    }

    /** Convenience: send a WARNING-level alert */
    public void sendAlert(String title, String message) {
        createAlert(AlertSeverity.WARNING, title, message);
    }

    /** Convenience: send a CRITICAL-level alert */
    public void sendCriticalAlert(String title, String message) {
        createAlert(AlertSeverity.CRITICAL, title, message);
    }

    /**
     * Resolve an alert
     */
    public void resolveAlert(String alertId) {
        Alert alert = activeAlerts.remove(alertId);
        if (alert != null) {
            alert.resolved = true;
            addToHistory(alert);
        }
    }

    /**
     * Get all active alerts
     */
    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }

    /**
     * Get active alerts by severity
     */
    public List<Alert> getAlertsBySeverity(AlertSeverity severity) {
        return activeAlerts.values().stream()
            .filter(alert -> alert.severity == severity)
            .toList();
    }

    /**
     * Get alert history
     */
    public List<Alert> getAlertHistory() {
        return new ArrayList<>(alertHistory);
    }

    /**
     * Get recent alerts (last N)
     */
    public List<Alert> getRecentAlerts(int limit) {
        int start = Math.max(0, alertHistory.size() - limit);
        return alertHistory.subList(start, alertHistory.size());
    }

    /**
     * Check memory and create alerts if needed
     */
    public void checkMemoryHealth() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double usagePercent = (double) usedMemory / maxMemory * 100;

        if (usagePercent > MEMORY_THRESHOLD) {
            createAlert(
                AlertSeverity.WARNING,
                "High Memory Usage",
                String.format("Memory usage at %.2f%% (%.2f MB / %.2f MB)",
                    usagePercent, usedMemory / 1024.0 / 1024.0, maxMemory / 1024.0 / 1024.0)
            );
        }
    }

    /**
     * Check error rate and create alerts
     */
    public void checkErrorRate(double errorRatePercent) {
        if (errorRatePercent > ERROR_RATE_THRESHOLD) {
            createAlert(
                AlertSeverity.ERROR,
                "High Error Rate",
                String.format("Error rate at %.2f%% (threshold: %.2f%%)", 
                    errorRatePercent, ERROR_RATE_THRESHOLD)
            );
        }
    }

    /**
     * Check response time and create alerts
     */
    public void checkResponseTime(long responseTimeMs) {
        if (responseTimeMs > RESPONSE_TIME_THRESHOLD) {
            createAlert(
                AlertSeverity.WARNING,
                "Slow Response Time",
                String.format("Response time: %d ms (threshold: %d ms)", 
                    responseTimeMs, RESPONSE_TIME_THRESHOLD)
            );
        }
    }

    /**
     * Get alert statistics
     */
    public Map<String, Object> getAlertStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("active_alerts", activeAlerts.size());
        
        Map<String, Long> bySeverity = new HashMap<>();
        activeAlerts.values().forEach(alert ->
            bySeverity.merge(alert.severity.name(), 1L, Long::sum)
        );
        stats.put("by_severity", bySeverity);
        stats.put("history_size", alertHistory.size());
        
        return stats;
    }

    private void addToHistory(Alert alert) {
        alertHistory.add(alert);
        if (alertHistory.size() > MAX_HISTORY) {
            alertHistory.remove(0);
        }
    }

    public int getActiveAlertCount() {
        return activeAlerts.size();
    }

    public int getCriticalAlertCount() {
        return (int) activeAlerts.values().stream()
            .filter(a -> a.severity == AlertSeverity.CRITICAL)
            .count();
    }
}
