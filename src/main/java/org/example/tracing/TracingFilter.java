package org.example.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Distributed Tracing Filter
 * Intercepts all HTTP requests and responses for tracing
 */
@Component
public class TracingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(TracingFilter.class);
    
    @Autowired
    private DistributedTracingService tracingService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip health checks and metrics to reduce noise
        if (shouldSkipTracing(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Start trace
        String traceId = tracingService.startTrace(requestPath, method);
        
        try {
            // Add request headers to trace
            tracingService.addSpanEvent("http.request", new java.util.HashMap<String, String>() {{
                put("path", requestPath);
                put("method", method);
                put("remote_addr", request.getRemoteAddr());
                put("user_agent", request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "unknown");
                put("trace_id", traceId);
            }});
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
            // Add response headers to trace
            tracingService.addSpanEvent("http.response", new java.util.HashMap<String, String>() {{
                put("status_code", String.valueOf(response.getStatus()));
                put("content_type", response.getContentType() != null ? response.getContentType() : "unknown");
            }});
            
        } catch (Exception e) {
            tracingService.recordError("Request processing failed: " + e.getMessage(), e);
            throw e;
        } finally {
            // End trace
            tracingService.endTrace();
        }
    }
    
    private boolean shouldSkipTracing(String path) {
        return path.contains("/health") || 
               path.contains("/metrics") || 
               path.contains("/prometheus") ||
               path.contains("/actuator");
    }
}
