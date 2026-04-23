package com.supremeai.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(0) // Run before authentication
public class RateLimitingFilter extends OncePerRequestFilter {

    // Default rate limit: 100 requests per minute per API key (or IP if no key)
    private static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 100;
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static class RateLimitInfo {
        AtomicInteger count = new AtomicInteger(0);
        Instant windowStart = Instant.now();
    }

    // In-memory store: key = rateLimitKey, value = rate limit info
    private final Map<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Skip rate limiting for health checks, static assets, and auth endpoints
        // (auth endpoints are already secured by Firebase ID token verification)
        return path.startsWith("/api/health") ||
               path.startsWith("/api/status") ||
               path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.equals("/") ||
               path.startsWith("/index.html") ||
               path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String rateLimitKey = getRateLimitKey(request);
        RateLimitInfo info = rateLimitCache.computeIfAbsent(rateLimitKey, k -> new RateLimitInfo());
        
        // Reset window if older than 1 minute
        if (Duration.between(info.windowStart, Instant.now()).toMinutes() >= 1) {
            info.count.set(0);
            info.windowStart = Instant.now();
        }
        
        int currentCount = info.count.incrementAndGet();
        if (currentCount <= DEFAULT_RATE_LIMIT_PER_MINUTE) {
            // Add rate limit headers to response
            response.setHeader("X-RateLimit-Limit", String.valueOf(DEFAULT_RATE_LIMIT_PER_MINUTE));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(DEFAULT_RATE_LIMIT_PER_MINUTE - currentCount));
            response.setHeader("X-RateLimit-Reset", String.valueOf(Duration.between(Instant.now(), info.windowStart.plus(Duration.ofMinutes(1))).getSeconds()));
            
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            logger.warn("Rate limit exceeded for key: " + rateLimitKey);
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json");
            response.setHeader("X-RateLimit-Limit", String.valueOf(DEFAULT_RATE_LIMIT_PER_MINUTE));
            response.setHeader("X-RateLimit-Remaining", "0");
            String body = "{\"error\":\"Rate limit exceeded\",\"limit\":" + DEFAULT_RATE_LIMIT_PER_MINUTE + ",\"retryAfter\":60}";
            response.getWriter().write(body);
        }
    }
    
    /**
     * Get rate limiting key for the request.
     * Uses API key if present, otherwise falls back to IP + URI.
     */
    private String getRateLimitKey(HttpServletRequest request) {
        // Try to extract API key (for API endpoints)
        String apiKey = extractApiKey(request);
        if (apiKey != null) {
            return "APIKEY:" + apiKey;
        }
        
        // Fall back to IP + URI for endpoints without API key
        String ip = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        String uri = request.getRequestURI();
        return ip + ":" + uri;
    }
    
    /**
     * Extract API key from Authorization header (Bearer token) or API-Key header.
     */
    private String extractApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // For Firebase JWT, use a hash of the token to identify the user
            // This provides per-user rate limiting across all their API keys
            return "USER:" + token.hashCode();
        }
        
        // Check for X-API-Key header
        String apiKeyHeader = request.getHeader("X-API-Key");
        if (apiKeyHeader != null && !apiKeyHeader.isEmpty()) {
            return apiKeyHeader;
        }
        
        return null;
    }
}
