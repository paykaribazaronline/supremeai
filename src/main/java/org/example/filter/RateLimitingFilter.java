package org.example.filter;

import org.example.config.RateLimiterConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Rate Limiting Filter
 * Apply rate limits to all HTTP requests
 * Blocks requests when user exceeds their rate limit
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    @Autowired(required = false)
    private RateLimiterConfiguration.RateLimiterService rateLimiterService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        if (rateLimiterService == null) {
            // If rate limiter not initialized, proceed
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Skip rate limiting for health check and metrics endpoints
            String path = request.getRequestURI();
            if (path.contains("/health") || path.contains("/metrics") || path.contains("/actuator")) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Extract user and role from request
            String userId = extractUserId(request);
            String role = extractRole(request);
            
            // Check rate limit
            if (!rateLimiterService.allowRequest(userId, role)) {
                logger.warn("Rate limit exceeded for user: {} (role: {})", userId, role);
                response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Rate limit exceeded. Please try again later.\"}");
                return;
            }
            
            // Add rate limit info to response headers
            var stats = rateLimiterService.getRemainingTokens(userId);
            response.setHeader("X-RateLimit-Remaining", String.valueOf(stats));
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("Error in rate limiting filter", e);
            // Don't block the request if there's an error with rate limiting
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * Extract user ID from request
     * Can be from Bearer token, header, or cookie
     */
    private String extractUserId(HttpServletRequest request) {
        // Try to get from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Use token as user ID
        }
        
        // Try to get from X-User-ID header
        String userId = request.getHeader("X-User-ID");
        if (userId != null) {
            return userId;
        }
        
        // Use remote IP as fallback
        return request.getRemoteAddr();
    }
    
    /**
     * Extract role from request
     */
    private String extractRole(HttpServletRequest request) {
        String role = request.getHeader("X-User-Role");
        return role != null ? role : "USER";
    }
}
