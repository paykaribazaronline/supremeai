package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.service.ServerMetricsService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Scan all org.example sub-packages (including .api, .controller, .service, etc.)
// and com.supremeai.teaching for security configuration.
@SpringBootApplication(scanBasePackages = {
    "org.example",
    "com.supremeai.teaching"
})
@RestController
public class Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    
    @Autowired(required = false)
    private ServerMetricsService metricsService;

    public static void main(String[] args) {
        try {
            logger.info("🚀 Starting SupremeAI Backend Service...");
            SpringApplication.run(Application.class, args);
            logger.info("✅ SupremeAI Backend Service started successfully!");
        } catch (Exception e) {
            logger.error("❌ Failed to start SupremeAI Backend Service", e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    @GetMapping("/")
    public Map<String, Object> home() {
        logger.info("✓ Home endpoint accessed");
        
        Map<String, Object> response = new LinkedHashMap<>();
        
        // ========== HEADER SECTION ==========
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("SupremeAI", "UP ✅");
        header.put("Version", "3.5");
        header.put("Environment", "Cloud (uc.a.run.app)");
        response.put("", header);  // Blank key for visual grouping
        
        // ========== QUICK METRICS SECTION ==========
        if (metricsService != null) {
            Map<String, Object> metrics = metricsService.getMetrics();
            
            Map<String, Object> quickStatus = new LinkedHashMap<>();
            quickStatus.put("Uptime", metrics.get("uptime"));
            quickStatus.put("Timestamp", metrics.get("serviceTime"));
            response.put("⏰ Time", quickStatus);
            
            // Memory compact format
            @SuppressWarnings("unchecked")
            Map<String, Object> heap = (Map<String, Object>) metrics.get("heap");
            if (heap != null) {
                String memStatus = String.format("%dMB/%dMB (%d%%)", 
                    heap.get("usedMB"),
                    heap.get("maxMB"),
                    heap.get("usagePercent")
                );
                response.put("💾 Memory", memStatus);
            }
            
            // Performance metrics
            Map<String, Object> perf = new LinkedHashMap<>();
            perf.put("CPU", metrics.get("processCpuUsage"));
            perf.put("Requests", metrics.get("totalRequests"));
            perf.put("Errors", String.format("%d (0%% ✅)", metrics.get("totalErrors")));
            perf.put("Connections", metrics.get("activeConnections"));
            response.put("📊 Performance", perf);
        }
        
        // ========== API ENDPOINTS SECTION ==========
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("Health", "/api/status/health");
        endpoints.put("Summary", "/api/status/summary");
        endpoints.put("Metrics", "/api/status/performance");
        endpoints.put("Tracing", "/api/tracing/stats");
        endpoints.put("Resilience", "/api/resilience/summary");
        response.put("🔗 Endpoints", endpoints);
        
        return response;
    }

    @GetMapping("/actuator/health")
    public Map<String, Object> health() {
        logger.info("✓ Health check endpoint accessed");
        
        if (metricsService != null) {
            return metricsService.getMetrics();
        }
        
        // Fallback if service not available
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "UP");
        fallback.put("timestamp", System.currentTimeMillis());
        return fallback;
    }
}
