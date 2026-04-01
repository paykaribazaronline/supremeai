package org.example.controller;

import org.example.service.ServerMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Server Status & Health Controller
 * Provides comprehensive server metrics and health information
 */
@RestController
public class ServerStatusController {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerStatusController.class);
    
    @Autowired
    private ServerMetricsService metricsService;
    
    /**
     * GET /api/status/health
     * Comprehensive health check with detailed metrics
     */
    @GetMapping("/api/status/health")
    public Map<String, Object> getDetailedHealth() {
        logger.info("✓ Detailed health check requested");
        return metricsService.getMetrics();
    }
    
    /**
     * GET /api/status/summary
     * Quick status summary (lightweight)
     */
    @GetMapping("/api/status/summary")
    public Map<String, Object> getStatusSummary() {
        logger.info("✓ Status summary requested");
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("status", "UP");
        summary.put("version", "3.5");
        summary.put("message", "🚀 SupremeAI Cloud Server is Running!");
        summary.put("timestamp", System.currentTimeMillis());
        
        return summary;
    }
    
    /**
     * GET /api/status/performance
     * Performance metrics only
     */
    @GetMapping("/api/status/performance")
    public Map<String, Object> getPerformance() {
        logger.info("✓ Performance metrics requested");
        
        Map<String, Object> metrics = metricsService.getMetrics();
        Map<String, Object> performance = new HashMap<>();
        
        performance.put("status", metrics.get("status"));
        performance.put("uptime", metrics.get("uptime"));
        performance.put("heap", metrics.get("heap"));
        performance.put("cpu", new HashMap<String, Object>() {{
            put("processCpuUsage", metrics.get("processCpuUsage"));
            put("systemCpuUsage", metrics.get("systemCpuUsage"));
        }});
        performance.put("requests", new HashMap<String, Object>() {{
            put("total", metrics.get("totalRequests"));
            put("errors", metrics.get("totalErrors"));
            put("errorRate", metrics.get("errorRate"));
        }});
        
        return performance;
    }
}
