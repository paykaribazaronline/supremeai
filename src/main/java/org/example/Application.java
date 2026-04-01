package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.service.ServerMetricsService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootApplication(scanBasePackages = {
    "org.example.service",
    "org.example.controller",  // Our REST Controllers (QuotaController, UserTierController, etc.)
    "org.example.config"       // Spring Configuration
    // Explicitly excluding: org.example.api (legacy controllers with missing dependencies)
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

    // 🔓 FIX: স্প্রিং সিকিউরিটি আপাতত সব রিকোয়েস্ট পারমিট করবে যাতে ক্লাউড রান চেক করতে পারে
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        return http.build();
    }

    @GetMapping("/")
    public Map<String, Object> home() {
        logger.info("✓ Home endpoint accessed");
        
        Map<String, Object> response = new LinkedHashMap<>();
        
        // Header
        response.put("🚀 Status", "UP ✅");
        response.put("📱 Server", "SupremeAI Cloud Server");
        response.put("📦 Version", "3.5");
        
        // Quick metrics from service if available
        if (metricsService != null) {
            Map<String, Object> metrics = metricsService.getMetrics();
            
            // Essential quick info
            response.put("⏰ Uptime", metrics.get("uptime"));
            response.put("🕐 Current Time", metrics.get("serviceTime"));
            
            // Memory section
            @SuppressWarnings("unchecked")
            Map<String, Object> heap = (Map<String, Object>) metrics.get("heap");
            if (heap != null) {
                response.put("💾 Memory", String.format("%dMB / %dMB (%d%%)", 
                    heap.get("usedMB"),
                    heap.get("maxMB"),
                    heap.get("usagePercent")
                ));
            }
            
            // CPU section
            response.put("⚙️  CPU Usage", 
                metrics.get("processCpuUsage") + " (process)");
            
            // Requests section
            response.put("📊 Total Requests", metrics.get("totalRequests"));
            response.put("❌ Errors", String.format("%d (%.2f%%)", 
                metrics.get("totalErrors"),
                metrics.get("errorRate")
            ));
            response.put("🔌 Active Connections", metrics.get("activeConnections"));
        }
        
        // Timestamp
        response.put("⏱️  Timestamp", System.currentTimeMillis());
        
        // Available endpoints section
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("📋 Full Health Check", "/api/status/health");
        endpoints.put("⚡ Quick Status", "/api/status/summary");
        endpoints.put("📈 Performance Metrics", "/api/status/performance");
        response.put("🔗 API Endpoints", endpoints);
        
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
