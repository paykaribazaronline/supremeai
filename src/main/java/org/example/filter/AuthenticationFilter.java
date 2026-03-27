package org.example.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Phase 5: Authentication Filter
 * 
 * Validates bearer tokens for REST API endpoints
 * Skips public endpoints (health, webhook)
 * 
 * Token format: Authorization: Bearer <token>
 * Tokens must be configured in environment: SUPREMEAI_API_TOKENS (comma-separated)
 */
@Component
public class AuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    
    // Public endpoints that don't require authentication
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/webhook",
        "/api/v1/data/health",
        "/actuator/health"
    );
    
    // Valid API tokens from environment
    private final Set<String> validTokens;
    
    public AuthenticationFilter() {
        this.validTokens = parseTokens();
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip authentication for public paths
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Extract bearer token
        String authHeader = request.getHeader("Authorization");
        String token = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        // Validate token
        if (token == null || !isValidToken(token)) {
            logger.warn("🔐 Unauthorized access attempt to {} from {}", 
                path, request.getRemoteAddr());
            
            response.setStatus(401); // HttpServletResponse.SC_UNAUTHORIZED
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized\"}");
            return;
        }
        
        logger.debug("✅ Authentication passed for {}", path);
        filterChain.doFilter(request, response);
    }
    
    /**
     * Check if path is public (requires no auth)
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
            .anyMatch(path::startsWith);
    }
    
    /**
     * Validate bearer token
     */
    private boolean isValidToken(String token) {
        if (validTokens.isEmpty()) {
            logger.warn("⚠️ No valid tokens configured - skipping auth");
            return true; // Dev mode
        }
        
        return validTokens.contains(token);
    }
    
    /**
     * Parse API tokens from environment variable
     * Format: SUPREMEAI_API_TOKENS=token1,token2,token3
     */
    private Set<String> parseTokens() {
        String tokensEnv = System.getenv("SUPREMEAI_API_TOKENS");
        
        if (tokensEnv == null || tokensEnv.isEmpty()) {
            logger.warn("⚠️ No SUPREMEAI_API_TOKENS configured");
            return new HashSet<>();
        }
        
        return Set.of(tokensEnv.split(","));
    }
}
