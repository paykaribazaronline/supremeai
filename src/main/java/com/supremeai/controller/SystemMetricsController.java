package com.supremeai.controller;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class SystemMetricsController {

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

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

        // DB Pool Metrics
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            if (hikari.getHikariPoolMXBean() != null) {
                metrics.put("dbActiveConnections", hikari.getHikariPoolMXBean().getActiveConnections());
                metrics.put("dbIdleConnections", hikari.getHikariPoolMXBean().getIdleConnections());
                metrics.put("dbTotalConnections", hikari.getHikariPoolMXBean().getTotalConnections());
            }
        }

        // Redis Metrics
        if (redisConnectionFactory != null) {
            try {
                metrics.put("redisStatus", redisConnectionFactory.getConnection().ping());
            } catch (Exception e) {
                metrics.put("redisStatus", "DOWN");
            }
        }
        
        // Time
        metrics.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(metrics);
    }
}
