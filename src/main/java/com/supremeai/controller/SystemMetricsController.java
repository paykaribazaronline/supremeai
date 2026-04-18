package com.supremeai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class SystemMetricsController {

    @GetMapping("/resources")
    public ResponseEntity<Map<String, Object>> getResourceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Memory Metrics
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        metrics.put("memoryUsed", memoryMXBean.getHeapMemoryUsage().getUsed());
        metrics.put("memoryMax", memoryMXBean.getHeapMemoryUsage().getMax());
        
        // CPU Metrics
        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
        metrics.put("cpuLoad", osMXBean.getSystemLoadAverage());
        metrics.put("availableProcessors", osMXBean.getAvailableProcessors());
        
        // Time
        metrics.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(metrics);
    }
}
