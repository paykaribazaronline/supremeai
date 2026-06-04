package com.supremeai.controller;

import com.supremeai.response.ApiResponse;
import com.supremeai.service.ProductionHealthMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * System Metrics Controller - Phase 4
 * Provides system resource metrics for the monitoring dashboard.
 */
@RestController
@RequestMapping("/api/system/metrics")
public class SystemMetricsController {

    @Autowired
    private ProductionHealthMonitor healthMonitor;

    /**
     * GET /api/system/metrics/resources
     * Get current system resource utilization.
     */
    @GetMapping("/resources")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ApiResponse<Map<String, Object>>> getResources() {
        return Mono.fromCallable(() -> {
            Map<String, Object> health = healthMonitor.getHealthStatus();
            Map<String, Object> metrics = (Map<String, Object>) health.get("metrics");
            
            Map<String, Object> data = new HashMap<>();
            data.put("cpuUsagePercentage", metrics.get("cpuUsage"));
            data.put("memoryUsagePercentage", metrics.get("memoryUsage"));
            data.put("memoryUsed", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            data.put("memoryMax", Runtime.getRuntime().maxMemory());
            data.put("availableProcessors", Runtime.getRuntime().availableProcessors());
            data.put("timestamp", System.currentTimeMillis());
            
            return ApiResponse.success("System metrics retrieved successfully", data);
        });
    }
}
