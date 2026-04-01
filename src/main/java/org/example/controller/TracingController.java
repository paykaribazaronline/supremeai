package org.example.controller;

import org.example.tracing.DistributedTracingService;
import org.example.tracing.TracingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Distributed Tracing REST Controller
 * Exposes tracing data and diagnostics
 */
@RestController
@RequestMapping("/api/tracing")
public class TracingController {
    private static final Logger logger = LoggerFactory.getLogger(TracingController.class);
    
    @Autowired
    private DistributedTracingService tracingService;
    
    /**
     * Get trace by ID
     */
    @GetMapping("/trace/{traceId}")
    public Map<String, Object> getTrace(@PathVariable String traceId) {
        logger.info("Fetching trace: {}", traceId);
        
        DistributedTracingService.TraceMetadata trace = tracingService.getTrace(traceId);
        if (trace == null) {
            return new LinkedHashMap<String, Object>() {{
                put("error", "Trace not found");
                put("trace_id", traceId);
            }};
        }
        
        return new LinkedHashMap<String, Object>() {{
            put("trace_id", trace.getTraceId());
            put("path", trace.getPath());
            put("method", trace.getMethod());
            put("start_time", trace.getStartTime());
            put("end_time", trace.getEndTime());
            put("duration_ms", trace.getDuration());
            put("status", trace.getErrorCount() > 0 ? "ERROR" : "SUCCESS");
            put("error_count", trace.getErrorCount());
            put("errors", trace.getErrors());
        }};
    }
    
    /**
     * Get recent traces
     */
    @GetMapping("/traces/recent")
    public Map<String, Object> getRecentTraces(@RequestParam(defaultValue = "10") int limit) {
        logger.info("Fetching {} recent traces", limit);
        
        List<DistributedTracingService.TraceMetadata> traces = tracingService.getRecentTraces(limit);
        
        return new LinkedHashMap<String, Object>() {{
            put("count", traces.size());
            put("traces", traces.stream().map(t -> new LinkedHashMap<String, Object>() {{
                put("trace_id", t.getTraceId());
                put("path", t.getPath());
                put("method", t.getMethod());
                put("duration_ms", t.getDuration());
                put("status", t.getErrorCount() > 0 ? "ERROR" : "SUCCESS");
                put("error_count", t.getErrorCount());
            }}).collect(Collectors.toList()));
        }};
    }
    
    /**
     * Get traces by path
     */
    @GetMapping("/traces/path")
    public Map<String, Object> getTracesByPath(@RequestParam String path) {
        logger.info("Fetching traces for path: {}", path);
        
        List<DistributedTracingService.TraceMetadata> traces = tracingService.getTracesByPath(path);
        
        return new LinkedHashMap<String, Object>() {{
            put("path", path);
            put("count", traces.size());
            put("traces", traces.stream().map(t -> new LinkedHashMap<String, Object>() {{
                put("trace_id", t.getTraceId());
                put("duration_ms", t.getDuration());
                put("status", t.getErrorCount() > 0 ? "ERROR" : "SUCCESS");
            }}).collect(Collectors.toList()));
        }};
    }
    
    /**
     * Get error traces
     */
    @GetMapping("/traces/errors")
    public Map<String, Object> getErrorTraces() {
        logger.info("Fetching error traces");
        
        List<DistributedTracingService.TraceMetadata> traces = tracingService.getErrorTraces();
        
        return new LinkedHashMap<String, Object>() {{
            put("total_errors", traces.size());
            put("traces", traces.stream().map(t -> new LinkedHashMap<String, Object>() {{
                put("trace_id", t.getTraceId());
                put("path", t.getPath());
                put("duration_ms", t.getDuration());
                put("error_count", t.getErrorCount());
                put("errors", t.getErrors());
            }}).collect(Collectors.toList()));
        }};
    }
    
    /**
     * Get tracing statistics
     */
    @GetMapping("/stats")
    public Map<String, Object> getTracingStats() {
        logger.info("Fetching tracing statistics");
        
        Collection<DistributedTracingService.TraceMetadata> allTraces = tracingService.getAllTraces();
        
        long totalTraces = allTraces.size();
        long errorTraces = allTraces.stream().filter(t -> t.getErrorCount() > 0).count();
        double avgDuration = allTraces.stream()
            .mapToLong(DistributedTracingService.TraceMetadata::getDuration)
            .average()
            .orElse(0);
        
        return new LinkedHashMap<String, Object>() {{
            put("total_traces", totalTraces);
            put("error_count", errorTraces);
            put("success_count", totalTraces - errorTraces);
            put("error_rate", totalTraces > 0 ? (errorTraces * 100.0) / totalTraces : 0);
            put("avg_duration_ms", avgDuration);
            put("current_trace_id", TracingContext.getTraceId());
        }};
    }
    
    /**
     * Cleanup old traces
     */
    @PostMapping("/cleanup")
    public Map<String, Object> cleanupOldTraces(@RequestParam(defaultValue = "3600000") long ttlMillis) {
        logger.info("Cleaning up traces older than {} ms", ttlMillis);
        
        long before = tracingService.getAllTraces().size();
        tracingService.cleanupOldTraces(ttlMillis);
        long after = tracingService.getAllTraces().size();
        
        return new LinkedHashMap<String, Object>() {{
            put("status", "success");
            put("traces_before", before);
            put("traces_after", after);
            put("traces_removed", before - after);
            put("ttl_minutes", ttlMillis / 60000);
        }};
    }
}
