package org.example.service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Metrics Collector Service
 * Real-time collection and aggregation of application metrics
 */
public class MetricsCollectorService {
    
    private final Map<String, MetricData> metrics = new ConcurrentHashMap<>();
    private final long retentionPeriodMs = 3600000; // 1 hour default retention
    
    /**
     * Record a metric
     */
    public void recordMetric(String name, double value, Map<String, String> tags) {
        long now = System.currentTimeMillis();
        
        MetricData metric = metrics.computeIfAbsent(name, k -> new MetricData(name));
        metric.addDataPoint(value, tags, now);
    }
    
    /**
     * Record a metric with default tags
     */
    public void recordMetric(String name, double value) {
        recordMetric(name, value, new HashMap<>());
    }
    
    /**
     * Get metric data
     */
    public MetricData getMetric(String name) {
        return metrics.get(name);
    }
    
    /**
     * Get all metrics
     */
    public Collection<MetricData> getAllMetrics() {
        return metrics.values();
    }
    
    /**
     * Get metric statistics
     */
    public Map<String, Object> getMetricStats(String name) {
        MetricData metric = metrics.get(name);
        if (metric == null) {
            return Map.of("error", "Metric not found");
        }
        
        return metric.getStatistics();
    }
    
    /**
     * Clear old metrics
     */
    public int clearOldMetrics() {
        long cutoffTime = System.currentTimeMillis() - retentionPeriodMs;
        int clearedCount = 0;
        
        for (MetricData metric : metrics.values()) {
            clearedCount += metric.removePreTime(cutoffTime);
        }
        
        return clearedCount;
    }
    
    /**
     * Get metrics for a time range
     */
    public Collection<MetricData> getMetricsInRange(long startTime, long endTime) {
        return metrics.values().stream()
                .filter(m -> m.hasDataInRange(startTime, endTime))
                .collect(Collectors.toList());
    }
    
    /**
     * Metric Data Model
     */
    public static class MetricData {
        private String name;
        private List<DataPoint> dataPoints = Collections.synchronizedList(new ArrayList<>());
        private long createdAt;
        
        public MetricData(String name) {
            this.name = name;
            this.createdAt = System.currentTimeMillis();
        }
        
        public void addDataPoint(double value, Map<String, String> tags, long timestamp) {
            dataPoints.add(new DataPoint(value, tags, timestamp));
        }
        
        public String getName() {
            return name;
        }
        
        public List<DataPoint> getDataPoints() {
            return new ArrayList<>(dataPoints);
        }
        
        public int getDataPointCount() {
            return dataPoints.size();
        }
        
        public Map<String, Object> getStatistics() {
            if (dataPoints.isEmpty()) {
                return Map.of("count", 0);
            }
            
            double[] values = dataPoints.stream()
                    .mapToDouble(dp -> dp.value)
                    .toArray();
            
            Arrays.sort(values);
            
            double sum = Arrays.stream(values).sum();
            double mean = sum / values.length;
            double median = values.length % 2 == 0 
                    ? (values[values.length / 2 - 1] + values[values.length / 2]) / 2
                    : values[values.length / 2];
            double min = values[0];
            double max = values[values.length - 1];
            
            return Map.ofEntries(
                    Map.entry("count", values.length),
                    Map.entry("sum", sum),
                    Map.entry("mean", mean),
                    Map.entry("median", median),
                    Map.entry("min", min),
                    Map.entry("max", max)
            );
        }
        
        public int removePreTime(long cutoffTime) {
            int originalSize = dataPoints.size();
            dataPoints.removeIf(dp -> dp.timestamp < cutoffTime);
            return originalSize - dataPoints.size();
        }
        
        public boolean hasDataInRange(long startTime, long endTime) {
            return dataPoints.stream()
                    .anyMatch(dp -> dp.timestamp >= startTime && dp.timestamp <= endTime);
        }
    }
    
    /**
     * Data Point Model
     */
    public static class DataPoint {
        public double value;
        public Map<String, String> tags;
        public long timestamp;
        
        public DataPoint(double value, Map<String, String> tags, long timestamp) {
            this.value = value;
            this.tags = new HashMap<>(tags);
            this.timestamp = timestamp;
        }
    }
}
