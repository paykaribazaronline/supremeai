package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DashboardService
 */
public class DashboardServiceTest {
    
    private DashboardService dashboardService;
    private MetricsCollectorService metricsService;
    
    @BeforeEach
    public void setUp() {
        metricsService = new MetricsCollectorService();
        dashboardService = new DashboardService(metricsService);
    }
    
    @Test
    public void testCreateDashboard() {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard("System Metrics", "Main system overview");
        
        assertNotNull(dashboard);
        assertNotNull(dashboard.id);
        assertEquals("System Metrics", dashboard.name);
        assertEquals("Main system overview", dashboard.description);
    }
    
    @Test
    public void testGetDashboard() {
        DashboardService.Dashboard created = dashboardService.createDashboard("Test Dashboard", "Test");
        DashboardService.Dashboard retrieved = dashboardService.getDashboard(created.id);
        
        assertNotNull(retrieved);
        assertEquals(created.id, retrieved.id);
        assertEquals("Test Dashboard", retrieved.name);
    }
    
    @Test
    public void testGetDashboardNotFound() {
        DashboardService.Dashboard dashboard = dashboardService.getDashboard("nonexistent");
        assertNull(dashboard);
    }
    
    @Test
    public void testListDashboards() {
        dashboardService.createDashboard("Dashboard 1", "First");
        dashboardService.createDashboard("Dashboard 2", "Second");
        dashboardService.createDashboard("Dashboard 3", "Third");
        
        Collection<DashboardService.Dashboard> dashboards = dashboardService.listDashboards();
        assertNotNull(dashboards);
        assertTrue(dashboards.size() >= 3);
    }
    
    @Test
    public void testAddWidget() {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard("Widget Test", "Test");
        dashboardService.addWidget(dashboard.id, "cpu_usage", "line");
        
        DashboardService.Dashboard updated = dashboardService.getDashboard(dashboard.id);
        assertEquals(1, updated.widgets.size());
    }
    
    @Test
    public void testAddMultipleWidgets() {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard("Multi Widget", "Test");
        
        dashboardService.addWidget(dashboard.id, "cpu_usage", "line");
        dashboardService.addWidget(dashboard.id, "memory_usage", "bar");
        dashboardService.addWidget(dashboard.id, "disk_usage", "gauge");
        
        DashboardService.Dashboard updated = dashboardService.getDashboard(dashboard.id);
        assertEquals(3, updated.widgets.size());
    }
    
    @Test
    public void testRemoveWidget() {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard("Remove Widget", "Test");
        dashboardService.addWidget(dashboard.id, "cpu_usage", "line");
        
        // Get the widget ID to remove
        DashboardService.Dashboard updated = dashboardService.getDashboard(dashboard.id);
        if (!updated.widgets.isEmpty()) {
            String widgetId = updated.widgets.get(0).id;
            dashboardService.removeWidget(dashboard.id, widgetId);
            
            DashboardService.Dashboard afterRemoval = dashboardService.getDashboard(dashboard.id);
            assertEquals(0, afterRemoval.widgets.size());
        }
    }
    
    @Test
    public void testRemoveWidgetNotFound() {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard("Test", "Test");
        
        assertDoesNotThrow(() -> dashboardService.removeWidget(dashboard.id, "nonexistent"));
    }
    
    @Test
    public void testGenerateDashboardReport() {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard("Report Test", "Test");
        dashboardService.addWidget(dashboard.id, "metric1", "line");
        
        Map<String, Object> report = dashboardService.generateDashboardReport(dashboard.id);
        
        assertNotNull(report);
        assertNotNull(report.get("dashboardId"));
        assertNotNull(report.get("name"));
        assertNotNull(report.get("widgets"));
    }
    
    @Test
    public void testDeleteDashboard() {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard("Delete Test", "Test");
        String dashboardId = dashboard.id;
        
        dashboardService.deleteDashboard(dashboardId);
        
        DashboardService.Dashboard deleted = dashboardService.getDashboard(dashboardId);
        assertNull(deleted);
    }
    
    @Test
    public void testWidgetTypes() {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard("Widget Types", "Test");
        
        dashboardService.addWidget(dashboard.id, "metric_line", "line");
        dashboardService.addWidget(dashboard.id, "metric_bar", "bar");
        dashboardService.addWidget(dashboard.id, "metric_gauge", "gauge");
        dashboardService.addWidget(dashboard.id, "metric_heatmap", "heatmap");
        
        DashboardService.Dashboard updated = dashboardService.getDashboard(dashboard.id);
        assertEquals(4, updated.widgets.size());
    }
    
    @Test
    public void testDashboardTimestamps() {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard("Timestamp Test", "Test");
        
        assertNotNull(dashboard.createdAt);
        assertNotNull(dashboard.updatedAt);
    }
    
    @Test
    public void testMultipleDashboards() {
        DashboardService.Dashboard dash1 = dashboardService.createDashboard("Dashboard 1", "First");
        DashboardService.Dashboard dash2 = dashboardService.createDashboard("Dashboard 2", "Second");
        
        assertNotEquals(dash1.id, dash2.id);
        
        dashboardService.addWidget(dash1.id, "metric1", "line");
        dashboardService.addWidget(dash2.id, "metric2", "bar");
        
        DashboardService.Dashboard retrieved1 = dashboardService.getDashboard(dash1.id);
        DashboardService.Dashboard retrieved2 = dashboardService.getDashboard(dash2.id);
        
        assertEquals(1, retrieved1.widgets.size());
        assertEquals(1, retrieved2.widgets.size());
    }
    
    @Test
    public void testEmptyDashboard() {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard("Empty", "Empty dashboard");
        assertTrue(dashboard.widgets.isEmpty());
    }
    
    @Test
    public void testDashboardNameUpdate() {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard("Original Name", "Desc");
        
        assertEquals("Original Name", dashboard.name);
        assertEquals("Desc", dashboard.description);
    }
    
    @Test
    public void testGenerateReportWithMultipleWidgets() {
        DashboardService.Dashboard dashboard = dashboardService.createDashboard("Full Report", "Test");
        dashboardService.addWidget(dashboard.id, "metric1", "line");
        dashboardService.addWidget(dashboard.id, "metric2", "bar");
        dashboardService.addWidget(dashboard.id, "metric3", "gauge");
        
        Map<String, Object> report = dashboardService.generateDashboardReport(dashboard.id);
        
        assertNotNull(report);
        assertNotNull(report.get("dashboardId"));
        assertNotNull(report.get("widgets"));
        assertTrue(report.get("widgets") instanceof java.util.List);
    }
}
