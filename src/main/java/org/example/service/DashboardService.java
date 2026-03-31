package org.example.service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dashboard Service
 * Generates and manages monitoring dashboards
 */
public class DashboardService {
    
    private final Map<String, Dashboard> dashboards = new ConcurrentHashMap<>();
    private final MetricsCollectorService metricsCollector;
    
    public DashboardService(MetricsCollectorService metricsCollector) {
        this.metricsCollector = metricsCollector;
    }
    
    /**
     * Create a new dashboard
     */
    public Dashboard createDashboard(String name, String description) {
        String id = UUID.randomUUID().toString();
        Dashboard dashboard = new Dashboard(id, name, description);
        dashboards.put(id, dashboard);
        return dashboard;
    }
    
    /**
     * Get dashboard by ID
     */
    public Dashboard getDashboard(String dashboardId) {
        return dashboards.get(dashboardId);
    }
    
    /**
     * List all dashboards
     */
    public Collection<Dashboard> listDashboards() {
        return new ArrayList<>(dashboards.values());
    }
    
    /**
     * Add widget to dashboard
     */
    public void addWidget(String dashboardId, String metricName, String widgetType) {
        Dashboard dashboard = dashboards.get(dashboardId);
        if (dashboard != null) {
            DashboardWidget widget = new DashboardWidget(
                    UUID.randomUUID().toString(),
                    metricName,
                    widgetType
            );
            dashboard.addWidget(widget);
        }
    }
    
    /**
     * Remove widget from dashboard
     */
    public void removeWidget(String dashboardId, String widgetId) {
        Dashboard dashboard = dashboards.get(dashboardId);
        if (dashboard != null) {
            dashboard.removeWidget(widgetId);
        }
    }
    
    /**
     * Generate dashboard report
     */
    public Map<String, Object> generateDashboardReport(String dashboardId) {
        Dashboard dashboard = dashboards.get(dashboardId);
        if (dashboard == null) {
            return Map.of("error", "Dashboard not found");
        }
        
        Map<String, Object> report = new HashMap<>();
        report.put("dashboardId", dashboard.id);
        report.put("name", dashboard.name);
        report.put("description", dashboard.description);
        report.put("createdAt", dashboard.createdAt);
        report.put("updatedAt", dashboard.updatedAt);
        
        List<Map<String, Object>> widgets = new ArrayList<>();
        for (DashboardWidget widget : dashboard.getWidgets()) {
            Map<String, Object> widgetData = new HashMap<>();
            widgetData.put("id", widget.id);
            widgetData.put("metricName", widget.metricName);
            widgetData.put("type", widget.type);
            
            MetricsCollectorService.MetricData metric = metricsCollector.getMetric(widget.metricName);
            if (metric != null) {
                widgetData.put("stats", metric.getStatistics());
            }
            
            widgets.add(widgetData);
        }
        report.put("widgets", widgets);
        
        return report;
    }
    
    /**
     * Delete dashboard
     */
    public void deleteDashboard(String dashboardId) {
        dashboards.remove(dashboardId);
    }
    
    /**
     * Dashboard Model
     */
    public static class Dashboard {
        public String id;
        public String name;
        public String description;
        public List<DashboardWidget> widgets = Collections.synchronizedList(new ArrayList<>());
        public long createdAt;
        public long updatedAt;
        
        public Dashboard(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.createdAt = System.currentTimeMillis();
            this.updatedAt = System.currentTimeMillis();
        }
        
        public void addWidget(DashboardWidget widget) {
            widgets.add(widget);
            this.updatedAt = System.currentTimeMillis();
        }
        
        public void removeWidget(String widgetId) {
            widgets.removeIf(w -> w.id.equals(widgetId));
            this.updatedAt = System.currentTimeMillis();
        }
        
        public List<DashboardWidget> getWidgets() {
            return new ArrayList<>(widgets);
        }
    }
    
    /**
     * Dashboard Widget Model
     */
    public static class DashboardWidget {
        public String id;
        public String metricName;
        public String type; // line, bar, gauge, heatmap
        
        public DashboardWidget(String id, String metricName, String type) {
            this.id = id;
            this.metricName = metricName;
            this.type = type;
        }
    }
}
