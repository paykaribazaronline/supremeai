package org.example.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distributed Tracing Service
 * Integrates OpenTelemetry for request tracing across services
 */
@Service
public class DistributedTracingService {
    private static final Logger logger = LoggerFactory.getLogger(DistributedTracingService.class);
    
    private final Tracer tracer;
    private final Map<String, TraceMetadata> traces = new ConcurrentHashMap<>();
    
    public DistributedTracingService() {
        try {
            // Initialize Jaeger Export
            JaegerThriftSpanExporter jaegerExporter = JaegerThriftSpanExporter.builder()
                .setEndpoint("http://localhost:14250")  // Jaeger collector endpoint
                .build();
            
            // Create SDK Tracer Provider
            SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(jaegerExporter))
                .build();
            
            // Set global provider
            OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
            
            this.tracer = GlobalOpenTelemetry.getTracer("supremeai-service");
            logger.info("✅ OpenTelemetry Tracer initialized with Jaeger");
        } catch (Exception e) {
            logger.warn("⚠️ Failed to initialize Jaeger exporter (expected if Jaeger not running): {}", e.getMessage());
            this.tracer = GlobalOpenTelemetry.getTracer("supremeai-service");
        }
    }
    
    /**
     * Start a new trace for incoming request
     */
    public String startTrace(String requestPath, String method) {
        String traceId = UUID.randomUUID().toString();
        
        Span span = tracer.spanBuilder(method + " " + requestPath)
            .startSpan();
        
        TracingContext.setTraceId(traceId);
        TracingContext.setSpanId(UUID.randomUUID().toString());
        TracingContext.setCurrentSpan(span);
        TracingContext.setRequestStartTime(System.currentTimeMillis());
        
        TraceMetadata metadata = new TraceMetadata(traceId, requestPath, method);
        traces.put(traceId, metadata);
        
        logger.debug("🟢 Trace started: {} | {} {}", traceId, method, requestPath);
        return traceId;
    }
    
    /**
     * Add event/span within current trace
     */
    public void addSpanEvent(String eventName, Map<String, String> attributes) {
        Span span = TracingContext.getCurrentSpan();
        if (span != null) {
            span.addEvent(eventName, io.opentelemetry.api.common.Attributes.builder()
                .putAll(attributes.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                    )))
                .build());
            logger.debug("📊 Span event: {} | Attributes: {}", eventName, attributes);
        }
    }
    
    /**
     * Record error in trace
     */
    public void recordError(String errorMessage, Throwable exception) {
        Span span = TracingContext.getCurrentSpan();
        if (span != null) {
            span.recordException(exception);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, errorMessage);
        }
        
        String traceId = TracingContext.getTraceId();
        if (traceId != null && traces.containsKey(traceId)) {
            traces.get(traceId).addError(errorMessage);
        }
        logger.error("❌ Error recorded in trace: {}", errorMessage, exception);
    }
    
    /**
     * End current trace
     */
    public TraceMetadata endTrace() {
        String traceId = TracingContext.getTraceId();
        Span span = TracingContext.getCurrentSpan();
        
        if (span != null) {
            span.end();
        }
        
        if (traceId != null && traces.containsKey(traceId)) {
            TraceMetadata metadata = traces.get(traceId);
            metadata.setEndTime(System.currentTimeMillis());
            metadata.setDuration(metadata.getEndTime() - metadata.getStartTime());
            
            logger.info("🟡 Trace ended: {} | Duration: {}ms | Errors: {}", 
                traceId, metadata.getDuration(), metadata.getErrorCount());
            
            TracingContext.clear();
            return metadata;
        }
        
        TracingContext.clear();
        return null;
    }
    
    /**
     * Get trace by ID
     */
    public TraceMetadata getTrace(String traceId) {
        return traces.get(traceId);
    }
    
    /**
     * Get all traces
     */
    public Collection<TraceMetadata> getAllTraces() {
        return traces.values();
    }
    
    /**
     * Get recent traces (last N)
     */
    public List<TraceMetadata> getRecentTraces(int limit) {
        return traces.values().stream()
            .sorted((a, b) -> Long.compare(b.getEndTime(), a.getEndTime()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Get traces by service/path
     */
    public List<TraceMetadata> getTracesByPath(String path) {
        return traces.values().stream()
            .filter(t -> t.getPath().contains(path))
            .toList();
    }
    
    /**
     * Get error traces
     */
    public List<TraceMetadata> getErrorTraces() {
        return traces.values().stream()
            .filter(t -> t.getErrorCount() > 0)
            .toList();
    }
    
    /**
     * Clear old traces (older than TTL)
     */
    public void cleanupOldTraces(long ttlMillis) {
        long cutoff = System.currentTimeMillis() - ttlMillis;
        traces.entrySet().removeIf(e -> e.getValue().getEndTime() < cutoff);
        logger.debug("🧹 Cleaned up traces older than {} ms", ttlMillis);
    }
    
    // Inner class for trace metadata
    public static class TraceMetadata {
        private final String traceId;
        private final String path;
        private final String method;
        private final long startTime;
        private long endTime;
        private long duration;
        private int errorCount = 0;
        private final List<String> errors = new ArrayList<>();
        
        public TraceMetadata(String traceId, String path, String method) {
            this.traceId = traceId;
            this.path = path;
            this.method = method;
            this.startTime = System.currentTimeMillis();
            this.endTime = 0;
        }
        
        public void addError(String errorMsg) {
            this.errorCount++;
            this.errors.add(errorMsg);
        }
        
        // Getters
        public String getTraceId() { return traceId; }
        public String getPath() { return path; }
        public String getMethod() { return method; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public long getDuration() { return duration; }
        public int getErrorCount() { return errorCount; }
        public List<String> getErrors() { return errors; }
        
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public void setDuration(long duration) { this.duration = duration; }
    }
}
