package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MetricsCollectorService
 */
public class MetricsCollectorServiceTest {
    
    private MetricsCollectorService service;
    
    @BeforeEach
    public void setUp() {
        service = new MetricsCollectorService();
    }
    
    @Test
    public void testRecordMetric() {
        service.recordMetric("cpu_usage", 45.5, Map.of("host", "server1"));
        
        MetricsCollectorService.MetricData metric = service.getMetric("cpu_usage");
        assertNotNull(metric);
        assertEquals("cpu_usage", metric.getName());
    }
    
    @Test
    public void testGetMetric() {
        service.recordMetric("memory_usage", 2048.0, Map.of());
        MetricsCollectorService.MetricData metric = service.getMetric("memory_usage");
        
        assertNotNull(metric);
        assertEquals("memory_usage", metric.getName());
    }
    
    @Test
    public void testGetMetricNotFound() {
        MetricsCollectorService.MetricData metric = service.getMetric("nonexistent");
        assertNull(metric);
    }
    
    @Test
    public void testRecordMultipleDataPoints() {
        for (int i = 0; i < 10; i++) {
            service.recordMetric("temperature", 20.0 + i, Map.of());
        }
        
        MetricsCollectorService.MetricData metric = service.getMetric("temperature");
        assertNotNull(metric);
    }
    
    @Test
    public void testGetMetricStats() {
        service.recordMetric("latency", 100.0, Map.of());
        service.recordMetric("latency", 200.0, Map.of());
        service.recordMetric("latency", 150.0, Map.of());
        
        Map<String, Object> stats = service.getMetricStats("latency");
        
        assertNotNull(stats);
        assertEquals(100.0, stats.get("min"));
        assertEquals(200.0, stats.get("max"));
        assertEquals(150.0, stats.get("mean"));
        assertEquals(3, stats.get("count"));
    }
    
    @Test
    public void testGetMetricStatsMedian() {
        for (int i = 1; i <= 5; i++) {
            service.recordMetric("values", i * 10.0, Map.of());
        }
        
        Map<String, Object> stats = service.getMetricStats("values");
        assertEquals(30.0, stats.get("median"));
    }
    
    @Test
    public void testGetMetricStatsMeanCalculation() {
        service.recordMetric("avg_test", 10.0, Map.of());
        service.recordMetric("avg_test", 20.0, Map.of());
        service.recordMetric("avg_test", 30.0, Map.of());
        
        Map<String, Object> stats = service.getMetricStats("avg_test");
        assertEquals(20.0, stats.get("mean"));
    }
    
    @Test
    public void testGetMetricsInRange() {
        long start = System.currentTimeMillis();
        service.recordMetric("range_test", 100.0, Map.of());
        long end = System.currentTimeMillis() + 1000;
        
        service.recordMetric("range_test", 200.0, Map.of());
        
        Collection<MetricsCollectorService.MetricData> metrics = service.getMetricsInRange(start, end);
        assertNotNull(metrics);
    }
    
    @Test
    public void testClearOldMetrics() {
        service.recordMetric("old_metric", 100.0, Map.of());
        service.clearOldMetrics();
        
        MetricsCollectorService.MetricData metric = service.getMetric("old_metric");
        // Metric might be cleared depending on time
        assertNotNull(metric);
    }
    
    @Test
    public void testMultipleMetricsWithTags() {
        service.recordMetric("request_count", 1.0, Map.of("endpoint", "/api/users", "method", "GET"));
        service.recordMetric("request_count", 1.0, Map.of("endpoint", "/api/posts", "method", "POST"));
        
        MetricsCollectorService.MetricData metric = service.getMetric("request_count");
        assertNotNull(metric);
    }
    
    @Test
    public void testMetricWithZeroValue() {
        service.recordMetric("zero_metric", 0.0, Map.of());
        MetricsCollectorService.MetricData metric = service.getMetric("zero_metric");
        assertNotNull(metric);
    }
    
    @Test
    public void testMetricWithNegativeValue() {
        service.recordMetric("negative_metric", -50.5, Map.of());
        MetricsCollectorService.MetricData metric = service.getMetric("negative_metric");
        assertNotNull(metric);
    }
    
    @Test
    public void testGetAllMetrics() {
        service.recordMetric("metric1", 10.0, Map.of());
        service.recordMetric("metric2", 20.0, Map.of());
        service.recordMetric("metric3", 30.0, Map.of());
        
        Collection<MetricsCollectorService.MetricData> metrics = service.getAllMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.size() >= 3);
    }
    
    @Test
    public void testConcurrentMetricRecording() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                service.recordMetric("concurrent_metric", i, Map.of());
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                service.recordMetric("concurrent_metric", i + 100, Map.of());
            }
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        MetricsCollectorService.MetricData metric = service.getMetric("concurrent_metric");
        assertNotNull(metric);
    }
    
    @Test
    public void testMetricStatsWithSingleValue() {
        service.recordMetric("single", 42.0, Map.of());
        Map<String, Object> stats = service.getMetricStats("single");
        
        assertEquals(42.0, stats.get("min"));
        assertEquals(42.0, stats.get("max"));
        assertEquals(42.0, stats.get("mean"));
    }
    
    @Test
    public void testGetMetricStatsForNonexistentMetric() {
        Map<String, Object> stats = service.getMetricStats("nonexistent");
        assertNotNull(stats);
        assertTrue(stats.containsKey("error"));
    }
}
