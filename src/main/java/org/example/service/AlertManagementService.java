package org.example.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alert Management Service
 * Manages alert definitions, triggers, and notifications
 */
public class AlertManagementService {
    
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    private final Map<String, AlertInstance> activeAlerts = new ConcurrentHashMap<>();
    private final MetricsCollectorService metricsCollector;
    
    public AlertManagementService(MetricsCollectorService metricsCollector) {
        this.metricsCollector = metricsCollector;
    }
    
    /**
     * Create alert rule
     */
    public AlertRule createAlertRule(String name, String metricName, String condition, 
                                     double threshold, String severity) {
        String id = UUID.randomUUID().toString();
        AlertRule rule = new AlertRule(id, name, metricName, condition, threshold, severity);
        alertRules.put(id, rule);
        return rule;
    }
    
    /**
     * Get alert rule
     */
    public AlertRule getAlertRule(String ruleId) {
        return alertRules.get(ruleId);
    }
    
    /**
     * List all alert rules
     */
    public Collection<AlertRule> listAlertRules() {
        return new ArrayList<>(alertRules.values());
    }
    
    /**
     * Evaluate alerts against current metrics
     */
    public List<AlertInstance> evaluateAlerts() {
        List<AlertInstance> triggered = new ArrayList<>();
        
        for (AlertRule rule : alertRules.values()) {
            if (!rule.enabled) continue;
            
            MetricsCollectorService.MetricData metric = metricsCollector.getMetric(rule.metricName);
            if (metric == null || metric.getDataPointCount() == 0) continue;
            
            double latestValue = metric.getDataPoints().get(metric.getDataPointCount() - 1).value;
            
            boolean shouldAlert = evaluateCondition(latestValue, rule.condition, rule.threshold);
            
            if (shouldAlert) {
                String alertId = UUID.randomUUID().toString();
                AlertInstance alert = new AlertInstance(
                        alertId,
                        rule.id,
                        rule.name,
                        rule.severity,
                        latestValue,
                        rule.threshold,
                        rule.metricName
                );
                activeAlerts.put(alertId, alert);
                triggered.add(alert);
            }
        }
        
        return triggered;
    }
    
    /**
     * Evaluate condition
     */
    private boolean evaluateCondition(double value, String condition, double threshold) {
        return switch (condition) {
            case "GREATER_THAN" -> value > threshold;
            case "GREATER_THAN_OR_EQUAL" -> value >= threshold;
            case "LESS_THAN" -> value < threshold;
            case "LESS_THAN_OR_EQUAL" -> value <= threshold;
            case "EQUAL" -> value == threshold;
            case "NOT_EQUAL" -> value != threshold;
            default -> false;
        };
    }
    
    /**
     * Get active alert
     */
    public AlertInstance getActiveAlert(String alertId) {
        return activeAlerts.get(alertId);
    }
    
    /**
     * List active alerts
     */
    public Collection<AlertInstance> listActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }
    
    /**
     * Acknowledge alert
     */
    public void acknowledgeAlert(String alertId) {
        AlertInstance alert = activeAlerts.get(alertId);
        if (alert != null) {
            alert.acknowledgedAt = System.currentTimeMillis();
            alert.acknowledged = true;
        }
    }
    
    /**
     * Resolve alert
     */
    public void resolveAlert(String alertId) {
        activeAlerts.remove(alertId);
    }
    
    /**
     * Delete alert rule
     */
    public void deleteAlertRule(String ruleId) {
        alertRules.remove(ruleId);
    }
    
    /**
     * Alert Rule Model
     */
    public static class AlertRule {
        public String id;
        public String name;
        public String metricName;
        public String condition; // GREATER_THAN, LESS_THAN, EQUAL, etc.
        public double threshold;
        public String severity; // CRITICAL, WARNING, INFO
        public boolean enabled;
        public long createdAt;
        
        public AlertRule(String id, String name, String metricName, String condition, 
                        double threshold, String severity) {
            this.id = id;
            this.name = name;
            this.metricName = metricName;
            this.condition = condition;
            this.threshold = threshold;
            this.severity = severity;
            this.enabled = true;
            this.createdAt = System.currentTimeMillis();
        }
    }
    
    /**
     * Alert Instance Model
     */
    public static class AlertInstance {
        public String id;
        public String ruleId;
        public String name;
        public String severity;
        public double metricValue;
        public double threshold;
        public String metricName;
        public long triggeredAt;
        public long acknowledgedAt;
        public boolean acknowledged;
        
        public AlertInstance(String id, String ruleId, String name, String severity,
                           double metricValue, double threshold, String metricName) {
            this.id = id;
            this.ruleId = ruleId;
            this.name = name;
            this.severity = severity;
            this.metricValue = metricValue;
            this.threshold = threshold;
            this.metricName = metricName;
            this.triggeredAt = System.currentTimeMillis();
            this.acknowledged = false;
        }
    }
}
