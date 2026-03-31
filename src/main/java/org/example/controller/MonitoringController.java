package org.example.controller;

import org.example.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Monitoring Controller
 * REST endpoints for metrics, dashboards, and alerts
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {
    
    @Autowired
    private MetricsCollectorService metricsCollector;
    
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private AlertManagementService alertService;
    
    @Autowired
    private PerformanceMonitoringService performanceMonitoring;
    
    // ============ Metrics Endpoints ============
    
    /**
     * Record a metric
     */
    @PostMapping("/metrics")
    public ResponseEntity<Map<String, String>> recordMetric(
            @RequestParam String name,
            @RequestParam double value,
            @RequestParam(required = false) Map<String, String> tags) {
        if (tags == null) {
            tags = new HashMap<>();
        }
        metricsCollector.recordMetric(name, value, tags);
        return ResponseEntity.ok(Map.of("status", "success", "metric", name));
    }
    
    /**
     * Get metric by name
     */
    @GetMapping("/metrics/{name}")
    public ResponseEntity<Map<String, Object>> getMetric(@PathVariable String name) {
        MetricsCollectorService.MetricData metric = metricsCollector.getMetric(name);
        if (metric == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("name", metric.getName());
        response.put("dataPoints", metric.getDataPointCount());
        response.put("statistics", metric.getStatistics());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getAllMetrics() {
        Collection<MetricsCollectorService.MetricData> metrics = metricsCollector.getAllMetrics();
        List<Map<String, Object>> metricsList = new ArrayList<>();
        
        for (MetricsCollectorService.MetricData metric : metrics) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", metric.getName());
            m.put("dataPoints", metric.getDataPointCount());
            m.put("statistics", metric.getStatistics());
            metricsList.add(m);
        }
        
        return ResponseEntity.ok(Map.of("metrics", metricsList, "count", metricsList.size()));
    }
    
    /**
     * Get metric statistics
     */
    @GetMapping("/metrics/{name}/stats")
    public ResponseEntity<Map<String, Object>> getMetricStats(@PathVariable String name) {
        Map<String, Object> stats = metricsCollector.getMetricStats(name);
        if (stats.containsKey("error")) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Clear old metrics
     */
    @PostMapping("/metrics/clear")
    public ResponseEntity<Map<String, Object>> clearOldMetrics() {
        int cleared = metricsCollector.clearOldMetrics();
        return ResponseEntity.ok(Map.of("status", "success", "clearedCount", cleared));
    }
    
    // ============ Dashboard Endpoints ============
    
    /**
     * Create dashboard
     */
    @PostMapping("/dashboards")
    public ResponseEntity<DashboardService.Dashboard> createDashboard(
            @RequestParam String name,
            @RequestParam String description) {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard(name, description);
        return ResponseEntity.ok(dashboard);
    }
    
    /**
     * Get dashboard
     */
    @GetMapping("/dashboards/{id}")
    public ResponseEntity<DashboardService.Dashboard> getDashboard(@PathVariable String id) {
        DashboardService.Dashboard dashboard = dashboardService.getDashboard(id);
        if (dashboard == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dashboard);
    }
    
    /**
     * List all dashboards
     */
    @GetMapping("/dashboards")
    public ResponseEntity<Map<String, Object>> listDashboards() {
        Collection<DashboardService.Dashboard> dashboards = dashboardService.listDashboards();
        return ResponseEntity.ok(Map.of("dashboards", dashboards, "count", dashboards.size()));
    }
    
    /**
     * Add widget to dashboard
     */
    @PostMapping("/dashboards/{id}/widgets")
    public ResponseEntity<Map<String, String>> addWidget(
            @PathVariable String id,
            @RequestParam String metricName,
            @RequestParam String widgetType) {
        dashboardService.addWidget(id, metricName, widgetType);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "dashboardId", id,
                "metricName", metricName,
                "widgetType", widgetType
        ));
    }
    
    /**
     * Generate dashboard report
     */
    @GetMapping("/dashboards/{id}/report")
    public ResponseEntity<Map<String, Object>> generateDashboardReport(@PathVariable String id) {
        Map<String, Object> report = dashboardService.generateDashboardReport(id);
        if (report.containsKey("error")) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(report);
    }
    
    /**
     * Delete dashboard
     */
    @DeleteMapping("/dashboards/{id}")
    public ResponseEntity<Map<String, String>> deleteDashboard(@PathVariable String id) {
        dashboardService.deleteDashboard(id);
        return ResponseEntity.ok(Map.of("status", "success", "dashboardId", id));
    }
    
    // ============ Alert Endpoints ============
    
    /**
     * Create alert rule
     */
    @PostMapping("/alerts/rules")
    public ResponseEntity<AlertManagementService.AlertRule> createAlertRule(
            @RequestParam String name,
            @RequestParam String metricName,
            @RequestParam String condition,
            @RequestParam double threshold,
            @RequestParam String severity) {
        AlertManagementService.AlertRule rule = alertService.createAlertRule(
                name, metricName, condition, threshold, severity
        );
        return ResponseEntity.ok(rule);
    }
    
    /**
     * Get alert rule
     */
    @GetMapping("/alerts/rules/{id}")
    public ResponseEntity<AlertManagementService.AlertRule> getAlertRule(@PathVariable String id) {
        AlertManagementService.AlertRule rule = alertService.getAlertRule(id);
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rule);
    }
    
    /**
     * List all alert rules
     */
    @GetMapping("/alerts/rules")
    public ResponseEntity<Map<String, Object>> listAlertRules() {
        Collection<AlertManagementService.AlertRule> rules = alertService.listAlertRules();
        return ResponseEntity.ok(Map.of("rules", rules, "count", rules.size()));
    }
    
    /**
     * Evaluate alerts
     */
    @PostMapping("/alerts/evaluate")
    public ResponseEntity<Map<String, Object>> evaluateAlerts() {
        List<AlertManagementService.AlertInstance> triggered = alertService.evaluateAlerts();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "triggeredAlerts", triggered,
                "count", triggered.size()
        ));
    }
    
    /**
     * Get active alerts
     */
    @GetMapping("/alerts/active")
    public ResponseEntity<Map<String, Object>> getActiveAlerts() {
        Collection<AlertManagementService.AlertInstance> alerts = alertService.listActiveAlerts();
        return ResponseEntity.ok(Map.of("alerts", alerts, "count", alerts.size()));
    }
    
    /**
     * Acknowledge alert
     */
    @PostMapping("/alerts/{id}/acknowledge")
    public ResponseEntity<Map<String, String>> acknowledgeAlert(@PathVariable String id) {
        alertService.acknowledgeAlert(id);
        return ResponseEntity.ok(Map.of("status", "success", "alertId", id));
    }
    
    /**
     * Resolve alert
     */
    @PostMapping("/alerts/{id}/resolve")
    public ResponseEntity<Map<String, String>> resolveAlert(@PathVariable String id) {
        alertService.resolveAlert(id);
        return ResponseEntity.ok(Map.of("status", "success", "alertId", id));
    }
    
    /**
     * Delete alert rule
     */
    @DeleteMapping("/alerts/rules/{id}")
    public ResponseEntity<Map<String, String>> deleteAlertRule(@PathVariable String id) {
        alertService.deleteAlertRule(id);
        return ResponseEntity.ok(Map.of("status", "success", "ruleId", id));
    }
    
    // ============ Performance Monitoring Endpoints ============
    
    /**
     * Start a trace span
     */
    @PostMapping("/performance/spans")
    public ResponseEntity<Map<String, String>> startSpan(
            @RequestParam String traceId,
            @RequestParam String spanName,
            @RequestParam(required = false) String parentSpanId) {
        PerformanceMonitoringService.TraceSpan span = performanceMonitoring.startSpan(
                traceId, spanName, parentSpanId
        );
        return ResponseEntity.ok(Map.of(
                "spanId", span.spanId,
                "traceId", span.traceId,
                "spanName", span.spanName
        ));
    }
    
    /**
     * End a trace span
     */
    @PostMapping("/performance/spans/{spanId}/end")
    public ResponseEntity<Map<String, String>> endSpan(@PathVariable String spanId) {
        performanceMonitoring.endSpan(spanId);
        return ResponseEntity.ok(Map.of("status", "success", "spanId", spanId));
    }
    
    /**
     * Get trace spans
     */
    @GetMapping("/performance/traces/{traceId}")
    public ResponseEntity<Map<String, Object>> getTraceSpans(@PathVariable String traceId) {
        List<PerformanceMonitoringService.TraceSpan> spans = performanceMonitoring.getTraceSpans(traceId);
        return ResponseEntity.ok(Map.of("traceId", traceId, "spans", spans, "count", spans.size()));
    }
    
    /**
     * Get method metrics
     */
    @GetMapping("/performance/methods/{methodName}")
    public ResponseEntity<Map<String, Object>> getMethodMetrics(@PathVariable String methodName) {
        PerformanceMonitoringService.PerformanceMetrics metrics = performanceMonitoring.getMethodMetrics(methodName);
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("methodName", metrics.methodName);
        response.put("callCount", metrics.callCount);
        response.put("totalDuration", metrics.totalDuration);
        response.put("avgDuration", metrics.getAverageDuration());
        response.put("minDuration", metrics.minDuration);
        response.put("maxDuration", metrics.maxDuration);
        response.put("medianDuration", metrics.getMedianDuration());
        response.put("p99Duration", metrics.getP99Duration());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get performance report
     */
    @GetMapping("/performance/report")
    public ResponseEntity<Map<String, Object>> getPerformanceReport() {
        return ResponseEntity.ok(performanceMonitoring.getPerformanceReport());
    }
}
