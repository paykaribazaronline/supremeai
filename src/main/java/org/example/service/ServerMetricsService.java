package org.example.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@SuppressWarnings("deprecation")
public class ServerMetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerMetricsService.class);
    private static final long STARTUP_TIME = System.currentTimeMillis();
    
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalActiveConnections = new AtomicLong(0);
    
    /**
     * Increment request counter
     */
    public void recordRequest() {
        totalRequests.incrementAndGet();
    }
    
    /**
     * Record error
     */
    public void recordError() {
        totalErrors.incrementAndGet();
    }
    
    /**
     * Update active connections
     */
    public void updateActiveConnections(long count) {
        totalActiveConnections.set(count);
    }
    
    /**
     * Get comprehensive server metrics
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Basic Info
        metrics.put("status", "UP");
        metrics.put("version", "3.5");
        metrics.put("timestamp", System.currentTimeMillis());
        metrics.put("serviceTime", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        
        // Uptime
        long uptimeMs = System.currentTimeMillis() - STARTUP_TIME;
        metrics.put("uptime", formatUptime(uptimeMs));
        metrics.put("uptimeMs", uptimeMs);
        
        // Request Metrics
        metrics.put("totalRequests", totalRequests.get());
        metrics.put("totalErrors", totalErrors.get());
        metrics.put("errorRate", calculateErrorRate());
        
        // Connection Metrics
        metrics.put("activeConnections", totalActiveConnections.get());
        
        // Memory Metrics
        metrics.putAll(getMemoryMetrics());
        
        // CPU Metrics
        metrics.putAll(getCpuMetrics());
        
        // JVM Metrics
        metrics.putAll(getJvmMetrics());
        
        logger.info("✓ Server metrics collected successfully");
        return metrics;
    }
    
    private Map<String, Object> getMemoryMetrics() {
        Map<String, Object> memory = new HashMap<>();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        
        memory.put("heap", new HashMap<String, Object>() {{
            put("usedMB", heapUsed / (1024 * 1024));
            put("maxMB", heapMax / (1024 * 1024));
            put("usagePercent", (int) ((heapUsed * 100) / heapMax));
        }});
        
        return memory;
    }
    
    private Map<String, Object> getCpuMetrics() {
        Map<String, Object> cpu = new HashMap<>();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        try {
            // Try to use Sun-specific extended traits if available
            java.lang.reflect.Method processCpuMethod = 
                osBean.getClass().getMethod("getProcessCpuUsage");
            java.lang.reflect.Method systemCpuMethod = 
                osBean.getClass().getMethod("getSystemCpuUsage");
            
            double processCpu = (double) processCpuMethod.invoke(osBean) * 100;
            double systemCpu = (double) systemCpuMethod.invoke(osBean) * 100;
            
            cpu.put("processCpuUsage", String.format("%.2f%%", processCpu));
            cpu.put("systemCpuUsage", String.format("%.2f%%", systemCpu));
        } catch (Exception e) {
            // Fallback if Sun-specific methods not available
            cpu.put("processCpuUsage", "N/A");
            cpu.put("systemCpuUsage", "N/A");
        }
        
        cpu.put("availableProcessors", osBean.getAvailableProcessors());
        
        return cpu;
    }
    
    private Map<String, Object> getJvmMetrics() {
        Map<String, Object> jvm = new HashMap<>();
        
        jvm.put("javaVersion", System.getProperty("java.version"));
        jvm.put("javaVendor", System.getProperty("java.vendor"));
        jvm.put("osName", System.getProperty("os.name"));
        jvm.put("osVersion", System.getProperty("os.version"));
        
        return jvm;
    }
    
    private double calculateErrorRate() {
        long total = totalRequests.get();
        if (total == 0) return 0;
        return (totalErrors.get() * 100.0) / total;
    }
    
    private String formatUptime(long ms) {
        long days = ms / (1000 * 60 * 60 * 24);
        long hours = (ms / (1000 * 60 * 60)) % 24;
        long minutes = (ms / (1000 * 60)) % 60;
        long seconds = (ms / 1000) % 60;
        
        return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
    }
}
