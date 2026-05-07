package com.supremeai.config;

import com.supremeai.security.ratelimit.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        int limit = determineRateLimit(path, request);
        String key = buildRateLimitKey(request, path);

        if (!rateLimiter.tryAcquire(key, limit, rateLimitProperties.getWindowSeconds())) {
            String clientIp = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            log.warn("Rate limit exceeded for key: {} on path: {}. Limit: {}. Client IP: {}, User-Agent: {}", 
                key, path, limit, clientIp, userAgent);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
            );
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Request allowed for key: {} on path: {}", key, path);
        }

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
     * Add rate limit information to response headers.
     */
    private void addRateLimitHeaders(HttpServletResponse response, String key) {
        try {
            Map<String, Object> status = rateLimiter.getStatus(key);
            response.setHeader("X-RateLimit-Limit", rateLimitProperties.getAuthenticatedRequestsPerMinute() + "");
            response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getOrDefault("tokens", status.getOrDefault("count", 0))));
            response.setHeader("X-RateLimit-Reset", String.valueOf(status.getOrDefault("last_refill", status.getOrDefault("window_start", 0))));
        } catch (Exception e) {
            // Silently fail - headers are informational only
        }
    }
}
