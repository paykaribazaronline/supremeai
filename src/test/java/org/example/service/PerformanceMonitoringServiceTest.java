package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PerformanceMonitoringService
 */
public class PerformanceMonitoringServiceTest {
    
    private PerformanceMonitoringService service;
    
    @BeforeEach
    public void setUp() {
        service = new PerformanceMonitoringService();
    }
    
    @Test
    public void testStartSpan() {
        PerformanceMonitoringService.TraceSpan span = service.startSpan(
                "trace-123",
                "GET /users",
                null
        );
        
        assertNotNull(span);
        assertEquals("trace-123", span.traceId);
        assertEquals("GET /users", span.spanName);
        assertNull(span.parentSpanId);
    }
    
    @Test
    public void testEndSpan() {
        PerformanceMonitoringService.TraceSpan span = service.startSpan(
                "trace-123",
                "GET /users",
                null
        );
        
        try {
            Thread.sleep(10); // Simulate work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        service.endSpan(span.spanId);
        
        PerformanceMonitoringService.TraceSpan ended = service.getSpan(span.spanId);
        assertNotNull(ended);
        assertTrue(ended.getDuration() >= 0);
    }
    
    @Test
    public void testGetSpan() {
        PerformanceMonitoringService.TraceSpan span = service.startSpan("trace-1", "operation", null);
        PerformanceMonitoringService.TraceSpan retrieved = service.getSpan(span.spanId);
        
        assertNotNull(retrieved);
        assertEquals(span.spanId, retrieved.spanId);
    }
    
    @Test
    public void testGetSpanNotFound() {
        PerformanceMonitoringService.TraceSpan span = service.getSpan("nonexistent");
        assertNull(span);
    }
    
    @Test
    public void testGetTraceSpans() {
        String traceId = "trace-123";
        PerformanceMonitoringService.TraceSpan span1 = service.startSpan(traceId, "op1", null);
        PerformanceMonitoringService.TraceSpan span2 = service.startSpan(traceId, "op2", span1.spanId);
        
        List<PerformanceMonitoringService.TraceSpan> spans = service.getTraceSpans(traceId);
        
        assertNotNull(spans);
        assertTrue(spans.size() >= 2);
    }
    
    @Test
    public void testTraceSpanWithParent() {
        PerformanceMonitoringService.TraceSpan parent = service.startSpan("trace-1", "parent", null);
        PerformanceMonitoringService.TraceSpan child = service.startSpan("trace-1", "child", parent.spanId);
        
        assertEquals(parent.spanId, child.parentSpanId);
    }
    
    @Test
    public void testSpanDuration() throws InterruptedException {
        PerformanceMonitoringService.TraceSpan span = service.startSpan("trace-1", "op", null);
        Thread.sleep(50);
        service.endSpan(span.spanId);
        
        PerformanceMonitoringService.TraceSpan ended = service.getSpan(span.spanId);
        assertTrue(ended.getDuration() >= 50);
    }
    
    @Test
    public void testRecordMethodPerformance() {
        PerformanceMonitoringService.TraceSpan span = service.startSpan("trace-1", "calculateSum", null);
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        service.endSpan(span.spanId);
        
        PerformanceMonitoringService.PerformanceMetrics metrics = service.getMethodMetrics("calculateSum");
        assertNotNull(metrics);
    }
    
    @Test
    public void testGetMethodMetrics() {
        PerformanceMonitoringService.TraceSpan span = service.startSpan("trace-1", "testMethod", null);
        service.endSpan(span.spanId);
        
        PerformanceMonitoringService.PerformanceMetrics metrics = service.getMethodMetrics("testMethod");
        assertNotNull(metrics);
        assertEquals("testMethod", metrics.methodName);
    }
    
    @Test
    public void testGetAllMethodMetrics() {
        service.startSpan("trace-1", "method1", null);
        service.startSpan("trace-1", "method2", null);
        service.startSpan("trace-1", "method3", null);
        
        Collection<PerformanceMonitoringService.PerformanceMetrics> metrics = service.getAllMethodMetrics();
        assertNotNull(metrics);
    }
    
    @Test
    public void testMethodMetricsCallCount() {
        for (int i = 0; i < 5; i++) {
            PerformanceMonitoringService.TraceSpan span = service.startSpan("trace-1", "countMethod", null);
            service.endSpan(span.spanId);
        }
        
        PerformanceMonitoringService.PerformanceMetrics metrics = service.getMethodMetrics("countMethod");
        assertEquals(5, metrics.callCount);
    }
    
    @Test
    public void testMethodMetricsAverageDuration() {
        for (int i = 0; i < 3; i++) {
            PerformanceMonitoringService.TraceSpan span = service.startSpan("trace-1", "avgMethod", null);
            service.endSpan(span.spanId);
        }
        
        PerformanceMonitoringService.PerformanceMetrics metrics = service.getMethodMetrics("avgMethod");
        assertTrue(metrics.getAverageDuration() >= 0);
    }
    
    @Test
    public void testMethodMetricsMinMax() {
        long[] durations = {10, 20, 30, 40, 50};
        
        for (long duration : durations) {
            PerformanceMonitoringService.TraceSpan span = service.startSpan("trace-1", "minMaxMethod", null);
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            service.endSpan(span.spanId);
        }
        
        PerformanceMonitoringService.PerformanceMetrics metrics = service.getMethodMetrics("minMaxMethod");
        assertTrue(metrics.minDuration <= metrics.maxDuration);
    }
    
    @Test
    public void testPerformanceReport() {
        PerformanceMonitoringService.TraceSpan span = service.startSpan("trace-1", "reportMethod", null);
        service.endSpan(span.spanId);
        
        Map<String, Object> report = service.getPerformanceReport();
        
        assertNotNull(report);
        assertNotNull(report.get("generatedAt"));
        assertNotNull(report.get("methods"));
    }
    
    @Test
    public void testSpanTags() {
        PerformanceMonitoringService.TraceSpan span = service.startSpan("trace-1", "taggedOp", null);
        span.addTag("user", "user123");
        span.addTag("endpoint", "/api/users");
        
        assertEquals("user123", span.tags.get("user"));
        assertEquals("/api/users", span.tags.get("endpoint"));
    }
    
    @Test
    public void testMultipleConcurrentSpans() throws InterruptedException {
        String traceId = "concurrent-trace";
        
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                PerformanceMonitoringService.TraceSpan span = service.startSpan(traceId, "op1", null);
                service.endSpan(span.spanId);
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                PerformanceMonitoringService.TraceSpan span = service.startSpan(traceId, "op2", null);
                service.endSpan(span.spanId);
            }
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        List<PerformanceMonitoringService.TraceSpan> spans = service.getTraceSpans(traceId);
        assertTrue(spans.size() >= 20);
    }
    
    @Test
    public void testMethodMetricsMedian() {
        for (long i = 1; i <= 5; i++) {
            PerformanceMonitoringService.TraceSpan span = service.startSpan("trace-1", "medianMethod", null);
            service.endSpan(span.spanId);
        }
        
        PerformanceMonitoringService.PerformanceMetrics metrics = service.getMethodMetrics("medianMethod");
        assertTrue(metrics.getMedianDuration() >= 0);
    }
    
    @Test
    public void testMethodMetricsP99() {
        for (int i = 0; i < 100; i++) {
            PerformanceMonitoringService.TraceSpan span = service.startSpan("trace-1", "p99Method", null);
            service.endSpan(span.spanId);
        }
        
        PerformanceMonitoringService.PerformanceMetrics metrics = service.getMethodMetrics("p99Method");
        assertTrue(metrics.getP99Duration() >= 0);
    }
}
