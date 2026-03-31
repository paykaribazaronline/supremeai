package org.example.filter;

import org.example.security.RateLimitingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * Rate Limiting Filter
 * Enforces rate limits per user/project and returns 429 when exceeded
 * 
 * HTTP Status Codes:
 * - 429 Too Many Requests: Rate limit exceeded
 * - Rate-Limit-Remaining header: Tokens left in current window
 * - Retry-After header: Seconds until rate limit resets
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    @Autowired(required = false)
    private RateLimitingService rateLimitingService;
    
    @Value("${supremeai.ratelimit.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${supremeai.ratelimit.requests-per-minute:1000}")
    private int requestsPerMinute;
    
    // Paths that bypass rate limiting
    private static final Set<String> BYPASS_PATHS = Set.of(
        "/api/v1/data/health",
        "/actuator/health",
        "/health",
        "/"
    );
    
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        if (!rateLimitEnabled || rateLimitingService == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String path = request.getRequestURI();
        
        // Skip rate limiting for certain paths
        if (shouldBypassRateLimit(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Determine rater limiter identifier
            String userId = extractUserId(request);
            String projectId = extractProjectId(request);
            
            // Apply rate limiting
            if (!checkRateLimit(userId, projectId, response)) {
                // Rate limit exceeded (HTTP 429)
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
                logger.warn("Rate limit exceeded for user: {}, project: {}", userId, projectId);
                return;
            }
            
            // Add rate limit headers
            addRateLimitHeaders(userId, projectId, response);
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Error in rate limiting filter", e);
            filterChain.doFilter(request, response);
        }
    }
    
    private boolean shouldBypassRateLimit(String path) {
        return BYPASS_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private String extractUserId(HttpServletRequest request) {
        // Try to extract from JWT token claims or session
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            // In a real app, parse JWT to get userId
            return "user:" + header.substring(7).substring(0, 10);
        }
        
        // Fallback to IP address
        return getClientIp(request);
    }
    
    private String extractProjectId(HttpServletRequest request) {
        // Try to extract from URL path (e.g., /api/projects/{projectId}/...)
        String path = request.getRequestURI();
        String[] parts = path.split("/");
        
        for (int i = 0; i < parts.length - 1; i++) {
            if ("projects".equals(parts[i]) && i + 1 < parts.length) {
                return "project:" + parts[i + 1];
            }
        }
        
        return "global";
    }
    
    private boolean checkRateLimit(String userId, String projectId, HttpServletResponse response) {
        // Check user rate limit (per-user limit)
        if (!rateLimitingService.allowUserRequest(userId)) {
            return false;
        }
        
        // Check project rate limit (per-project limit)
        if (!rateLimitingService.allowProjectRequest(projectId)) {
            return false;
        }
        
        return true;
    }
    
    private void addRateLimitHeaders(String userId, String projectId, HttpServletResponse response) {
        // Add rate limit information headers
        long userRemaining = rateLimitingService.getUserRemainingTokens(userId);
        long projectRemaining = rateLimitingService.getProjectRemainingTokens(projectId);
        
        response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.min(userRemaining, projectRemaining)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + 60));
    }
    
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0];
            }
        }
        
        return request.getRemoteAddr();
    }
}
