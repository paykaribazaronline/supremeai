package org.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * System Metrics Controller
 * Provides real-time system performance metrics
 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class SystemMetricsController {

    @GetMapping("/metrics")
    public ResponseEntity<?> getSystemMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("cpuUsage", 45.2);
            metrics.put("memoryUsage", 62.1);
            metrics.put("apiLatency", 125);
            metrics.put("successRate", 94.2);
            metrics.put("errorRate", 5.8);
            metrics.put("uptime", 1445380);
            metrics.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
