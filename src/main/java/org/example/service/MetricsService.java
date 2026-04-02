package org.example.service;

import org.springframework.stereotype.Service;
import java.lang.management.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase 4: Advanced Metrics Service
 * Tracks system health, performance metrics, and generation statistics
 * Enables real-time monitoring dashboards and automated alerting
 */
@Service
public class MetricsService {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalSuccessfulGenerations = new AtomicLong(0);
    private final AtomicLong totalFailedGenerations = new AtomicLong(0);
    
    private final Map<String, Long> generationCountByFramework = new ConcurrentHashMap<>();
    private final Map<String, Double> averageGenerationTime = new ConcurrentHashMap<>();
    private final List<Long> requestLatencies = Collections.synchronizedList(new ArrayList<>());
    
    private final long startTime = System.currentTimeMillis();

    /**
     * Record a generation attempt
     */
    public void recordGeneration(String framework, long durationMs, boolean success) {
        totalRequests.incrementAndGet();
        
        if (success) {
            totalSuccessfulGenerations.incrementAndGet();
        } else {
            totalFailedGenerations.incrementAndGet();
            totalErrors.incrementAndGet();
        }
        
        generationCountByFramework.put(
            framework, 
            generationCountByFramework.getOrDefault(framework, 0L) + 1
        );
        
        // Update average time
        Long count = generationCountByFramework.get(framework);
        Double currentAvg = averageGenerationTime.getOrDefault(framework, 0.0);
        Double newAvg = (currentAvg * (count - 1) + durationMs) / count;
        averageGenerationTime.put(framework, newAvg);
        
        requestLatencies.add(durationMs);
        if (requestLatencies.size() > 1000) {
            requestLatencies.remove(0); // Keep last 1000
        }
    }

    /**
     * Get comprehensive system health
     */
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("uptime_ms", System.currentTimeMillis() - startTime);
        
        // Memory info
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        Map<String, Object> memoryInfo = new HashMap<>();
        memoryInfo.put("heap_used_mb", memory.getHeapMemoryUsage().getUsed() / (1024 * 1024));
        memoryInfo.put("heap_max_mb", memory.getHeapMemoryUsage().getMax() / (1024 * 1024));
        memoryInfo.put("heap_committed_mb", memory.getHeapMemoryUsage().getCommitted() / (1024 * 1024));
        health.put("memory", memoryInfo);
        
        // CPU info (using standard Java APIs)
        Map<String, Object> cpuInfo = new HashMap<>();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double systemLoadAvg = osBean.getSystemLoadAverage();
        cpuInfo.put("system_load_average", systemLoadAvg >= 0 ? systemLoadAvg : 0);
        cpuInfo.put("available_processors", osBean.getAvailableProcessors());
        health.put("cpu", cpuInfo);
        
        // Request metrics
        Map<String, Object> requests = new HashMap<>();
        requests.put("total", totalRequests.get());
        requests.put("successful", totalSuccessfulGenerations.get());
        requests.put("failed", totalFailedGenerations.get());
        requests.put("success_rate", totalRequests.get() > 0 ? 
            (double) totalSuccessfulGenerations.get() / totalRequests.get() * 100 : 0);
        health.put("requests", requests);
        
        // Latency info
        if (!requestLatencies.isEmpty()) {
            double avgLatency = requestLatencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
            long maxLatency = Collections.max(requestLatencies);
            long minLatency = Collections.min(requestLatencies);
            
            Map<String, Object> latency = new HashMap<>();
            latency.put("average_ms", avgLatency);
            latency.put("max_ms", maxLatency);
            latency.put("min_ms", minLatency);
            latency.put("p95_ms", calculatePercentile(requestLatencies, 0.95));
            latency.put("p99_ms", calculatePercentile(requestLatencies, 0.99));
            health.put("latency", latency);
        }
        
        return health;
    }

    /**
     * Get generation statistics by framework
     */
    public Map<String, Object> getGenerationStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        stats.put("total_generations", totalSuccessfulGenerations.get() + totalFailedGenerations.get());
        stats.put("successful", totalSuccessfulGenerations.get());
        stats.put("failed", totalFailedGenerations.get());
        stats.put("error_rate", totalRequests.get() > 0 ? 
            (double) totalErrors.get() / totalRequests.get() * 100 : 0);
        
        Map<String, Object> byFramework = new LinkedHashMap<>();
        generationCountByFramework.forEach((framework, count) -> {
            Map<String, Object> frameworkStats = new HashMap<>();
            frameworkStats.put("count", count);
            frameworkStats.put("average_time_ms", averageGenerationTime.get(framework));
            byFramework.put(framework, frameworkStats);
        });
        stats.put("by_framework", byFramework);
        stats.put("last_updated", Instant.now().toString());
        
        return stats;
    }

    /**
     * Get alerts if metrics exceed thresholds
     */
    public List<Map<String, String>> getAlerts() {
        List<Map<String, String>> alerts = new ArrayList<>();
        
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        long heapMax = memory.getHeapMemoryUsage().getMax();
        double heapUsagePercent = (double) heapUsed / heapMax * 100;
        
        if (heapUsagePercent > 85) {
            Map<String, String> alert = new HashMap<>();
            alert.put("severity", "WARNING");
            alert.put("message", String.format("High heap memory usage: %.2f%%", heapUsagePercent));
            alert.put("timestamp", Instant.now().toString());
            alerts.add(alert);
        }
        
        if (totalFailedGenerations.get() > 0 && 
            (double) totalFailedGenerations.get() / totalRequests.get() > 0.1) {
            Map<String, String> alert = new HashMap<>();
            alert.put("severity", "ERROR");
            alert.put("message", String.format("High error rate: %.2f%%", 
                (double) totalFailedGenerations.get() / totalRequests.get() * 100));
            alert.put("timestamp", Instant.now().toString());
            alerts.add(alert);
        }
        
        return alerts;
    }

    private long calculatePercentile(List<Long> values, double percentile) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) (sorted.size() * percentile) - 1;
        return index >= 0 && index < sorted.size() ? sorted.get(index) : 0;
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getTotalErrors() {
        return totalErrors.get();
    }
}
