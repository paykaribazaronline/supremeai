package org.example.tracing;

import io.opentelemetry.api.trace.Span;

/**
 * DistributedTracing Context Holder
 * Carries trace information across async boundaries
 */
public class TracingContext {
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> SPAN_ID = new ThreadLocal<>();
    private static final ThreadLocal<Span> CURRENT_SPAN = new ThreadLocal<>();
    private static final ThreadLocal<Long> REQUEST_START_TIME = new ThreadLocal<>();
    
    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }
    
    public static String getTraceId() {
        return TRACE_ID.get();
    }
    
    public static void setSpanId(String spanId) {
        SPAN_ID.set(spanId);
    }
    
    public static String getSpanId() {
        return SPAN_ID.get();
    }
    
    public static void setCurrentSpan(Span span) {
        CURRENT_SPAN.set(span);
    }
    
    public static Span getCurrentSpan() {
        return CURRENT_SPAN.get();
    }
    
    public static void setRequestStartTime(Long startTime) {
        REQUEST_START_TIME.set(startTime);
    }
    
    public static Long getRequestStartTime() {
        return REQUEST_START_TIME.get();
    }
    
    public static void clear() {
        TRACE_ID.remove();
        SPAN_ID.remove();
        CURRENT_SPAN.remove();
        REQUEST_START_TIME.remove();
    }
}
