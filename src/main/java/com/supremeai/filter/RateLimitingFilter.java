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

    // Default rate limit: 100 requests per minute per IP+endpoint
    private static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 100;
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static class RateLimitInfo {
        AtomicInteger count = new AtomicInteger(0);
        Instant windowStart = Instant.now();
    }

    // In-memory store: key = IP:URI, value = rate limit info
    private final Map<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Skip rate limiting for health checks and static assets
        return path.startsWith("/api/health") ||
               path.startsWith("/api/status") ||
               path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.equals("/") ||
               path.startsWith("/index.html");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String key = getKey(request);
        RateLimitInfo info = rateLimitCache.computeIfAbsent(key, k -> new RateLimitInfo());
        
        // Reset window if older than 1 minute
        if (Duration.between(info.windowStart, Instant.now()).toMinutes() >= 1) {
            info.count.set(0);
            info.windowStart = Instant.now();
        }
        
        if (info.count.incrementAndGet() <= DEFAULT_RATE_LIMIT_PER_MINUTE) {
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            logger.warn("Rate limit exceeded for key: " + key);
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json");
            String body = "{\"error\":\"Rate limit exceeded\",\"limit\":" + DEFAULT_RATE_LIMIT_PER_MINUTE + ",\"retryAfter\":60}";
            response.getWriter().write(body);
        }
    }
    
    private String getKey(HttpServletRequest request) {
        String ip = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        String uri = request.getRequestURI();
        return ip + ":" + uri;
    }
}
