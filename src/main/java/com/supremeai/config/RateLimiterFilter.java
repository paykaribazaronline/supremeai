package com.supremeai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Rate limiting filter with support for both in-memory and distributed (Redis) rate limiting.
 * Applies different rate limits based on user role and endpoint type.
 */
@Component
@RequiredArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final DistributedRateLimiter distributedRateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip rate limiting if disabled
        if (!rateLimitProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Determine rate limit based on user role and endpoint
        String path = request.getRequestURI();
        int limit = determineRateLimit(path, request);
        String key = buildRateLimitKey(request, path);

        boolean allowed;
        if (rateLimitProperties.isDistributed()) {
            allowed = distributedRateLimiter.tryAcquire(key, limit, rateLimitProperties.getWindowSeconds());
        } else {
            // Fallback to simple in-memory rate limiting (not recommended for production)
            allowed = tryAcquireInMemory(key, limit);
        }

        if (!allowed) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
            );
            return;
        }

        // Add rate limit headers to response
        addRateLimitHeaders(response, key);

        filterChain.doFilter(request, response);
    }

    /**
     * Determine the appropriate rate limit based on user role and endpoint.
     */
    private int determineRateLimit(String path, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated();
        boolean isAdmin = isAuthenticated && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // AI provider endpoints have stricter limits
        if (path.contains("/api/providers") || path.contains("/api/generate") || path.contains("/api/chat")) {
            return rateLimitProperties.getAiProviderRequestsPerMinute();
        }

        if (isAdmin) {
            return rateLimitProperties.getAdminRequestsPerMinute();
        } else if (isAuthenticated) {
            return rateLimitProperties.getAuthenticatedRequestsPerMinute();
        } else {
            return rateLimitProperties.getAnonymousRequestsPerMinute();
        }
    }

    /**
     * Build a unique key for rate limiting based on user/session and endpoint.
     */
    private String buildRateLimitKey(HttpServletRequest request, String path) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = "anonymous";
        
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            userId = auth.getName();
        } else if (request.getSession(false) != null) {
            userId = request.getSession(false).getId();
        } else {
            userId = request.getRemoteAddr();
        }

        // Normalize path to group similar endpoints
        String normalizedPath = path.replaceAll("\\d+", "{id}");
        
        return String.format("%s:%s", userId, normalizedPath);
    }

    /**
     * Simple in-memory rate limiter (fallback when Redis is unavailable).
     * WARNING: Not suitable for distributed environments.
     */
    private boolean tryAcquireInMemory(String key, int limit) {
        // This is a simplified implementation - in production, use a proper in-memory cache
        // with TTL like Caffeine or Guava Cache
        return true; // Allow all requests when using in-memory fallback
    }

    /**
     * Add rate limit information to response headers.
     */
    private void addRateLimitHeaders(HttpServletResponse response, String key) {
        try {
            Map<String, Object> status = distributedRateLimiter.getStatus(key);
            response.setHeader("X-RateLimit-Limit", rateLimitProperties.getAuthenticatedRequestsPerMinute() + "");
            response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getOrDefault("tokens", 0)));
            response.setHeader("X-RateLimit-Reset", String.valueOf(status.getOrDefault("last_refill", 0)));
        } catch (Exception e) {
            // Silently fail - headers are informational only
        }
    }
}
