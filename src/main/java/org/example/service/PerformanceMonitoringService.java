package org.example.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performance Monitoring Service
 * Tracks request performance and distributed tracing
 */
public class PerformanceMonitoringService {
    
    private final Map<String, TraceSpan> spans = new ConcurrentHashMap<>();
    private final Map<String, PerformanceMetrics> methodMetrics = new ConcurrentHashMap<>();
    
    /**
     * Start a trace span
     */
    public TraceSpan startSpan(String traceId, String spanName, String parentSpanId) {
        String spanId = UUID.randomUUID().toString();
        TraceSpan span = new TraceSpan(spanId, traceId, spanName, parentSpanId);
        spans.put(spanId, span);
        return span;
    }
    
    /**
     * End a span
     */
    public void endSpan(String spanId) {
        TraceSpan span = spans.get(spanId);
        if (span != null) {
            span.endTime = System.currentTimeMillis();
            
            // Record performance metrics
            recordMethodPerformance(span.spanName, span.getDuration());
        }
    }
    
    /**
     * Get span
     */
    public TraceSpan getSpan(String spanId) {
        return spans.get(spanId);
    }
    
    /**
     * Get all spans for a trace
     */
    public List<TraceSpan> getTraceSpans(String traceId) {
        return spans.values().stream()
                .filter(s -> s.traceId.equals(traceId))
                .toList();
    }
    
    /**
     * Record method performance
     */
    private void recordMethodPerformance(String methodName, long duration) {
        PerformanceMetrics metrics = methodMetrics.computeIfAbsent(
                methodName,
                k -> new PerformanceMetrics(methodName)
        );
        metrics.addDuration(duration);
    }
    
    /**
     * Get method performance metrics
     */
    public PerformanceMetrics getMethodMetrics(String methodName) {
        return methodMetrics.get(methodName);
    }
    
    /**
     * Get all method metrics
     */
    public Collection<PerformanceMetrics> getAllMethodMetrics() {
        return new ArrayList<>(methodMetrics.values());
    }
    
    /**
     * Get performance report
     */
    public Map<String, Object> getPerformanceReport() {
        Map<String, Object> report = new HashMap<>();
        List<Map<String, Object>> methods = new ArrayList<>();
        
        for (PerformanceMetrics metrics : methodMetrics.values()) {
            Map<String, Object> methodData = new HashMap<>();
            methodData.put("name", metrics.methodName);
            methodData.put("callCount", metrics.callCount);
            methodData.put("totalDuration", metrics.totalDuration);
            methodData.put("avgDuration", metrics.getAverageDuration());
            methodData.put("minDuration", metrics.minDuration);
            methodData.put("maxDuration", metrics.maxDuration);
            methods.add(methodData);
        }
        
        report.put("methods", methods);
        report.put("generatedAt", System.currentTimeMillis());
        return report;
    }
    
    /**
     * Trace Span Model
     */
    public static class TraceSpan {
        public String spanId;
        public String traceId;
        public String spanName;
        public String parentSpanId;
        public long startTime;
        public long endTime;
        public Map<String, String> tags = new HashMap<>();
        
        public TraceSpan(String spanId, String traceId, String spanName, String parentSpanId) {
            this.spanId = spanId;
            this.traceId = traceId;
            this.spanName = spanName;
            this.parentSpanId = parentSpanId;
            this.startTime = System.currentTimeMillis();
        }
        
        public long getDuration() {
            return endTime - startTime;
        }
        
        public void addTag(String key, String value) {
            tags.put(key, value);
        }
    }
    
    /**
     * Performance Metrics Model
     */
    public static class PerformanceMetrics {
        public String methodName;
        public long callCount;
        public long totalDuration;
        public long minDuration = Long.MAX_VALUE;
        public long maxDuration = Long.MIN_VALUE;
        private final List<Long> durations = Collections.synchronizedList(new ArrayList<>());
        
        public PerformanceMetrics(String methodName) {
            this.methodName = methodName;
            this.callCount = 0;
            this.totalDuration = 0;
        }
        
        public void addDuration(long duration) {
            callCount++;
            totalDuration += duration;
            minDuration = Math.min(minDuration, duration);
            maxDuration = Math.max(maxDuration, duration);
            durations.add(duration);
        }
        
        public double getAverageDuration() {
            return callCount > 0 ? (double) totalDuration / callCount : 0;
        }
        
        public long getMedianDuration() {
            if (durations.isEmpty()) return 0;
            
            List<Long> sorted = new ArrayList<>(durations);
            Collections.sort(sorted);
            
            return sorted.size() % 2 == 0
                    ? (sorted.get(sorted.size() / 2 - 1) + sorted.get(sorted.size() / 2)) / 2
                    : sorted.get(sorted.size() / 2);
        }
        
        public double getP99Duration() {
            if (durations.isEmpty()) return 0;
            
            List<Long> sorted = new ArrayList<>(durations);
            Collections.sort(sorted);
            
            int index = (int) Math.ceil(sorted.size() * 0.99) - 1;
            return index >= 0 ? sorted.get(index) : 0;
        }
    }
}
