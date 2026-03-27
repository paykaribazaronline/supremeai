package org.example.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Phase 5: Authentication Filter (Updated for JWT)
 * 
 * Validates JWT bearer tokens for REST API endpoints
 * Skips public endpoints (health, webhook, auth login/register)
 * Also accepts simple test tokens from supremeai.api.tokens config for testing
 * 
 * Token format: Authorization: Bearer <JWT_token> or Authorization: Bearer <test-token>
 * Token created by AuthenticationService.generateJWT()
 */
@Component
public class AuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    
    // Public endpoints that don't require authentication
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/webhook",
        "/api/v1/data/health",
        "/actuator/health",
        "/api/auth/login",
        "/api/auth/bootstrap",
        "/api/auth/hash-password",
        "/api/auth/register",
        "/api/auth/refresh",
        "/index.html",
        "/login.html",
        "/"
    );
    
    @Value("${supremeai.api.tokens:}")
    private String configuredTokens;
    
    private java.util.Optional<org.example.service.AuthenticationService> authService;
    private Set<String> validTestTokens = new HashSet<>();
    
    public AuthenticationFilter() {
        this.authService = java.util.Optional.empty();
    }
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setAuthService(org.example.service.AuthenticationService service) {
        this.authService = java.util.Optional.of(service);
        initializeTestTokens();
    }
    
    private void initializeTestTokens() {
        if (configuredTokens != null && !configuredTokens.isEmpty()) {
            String[] tokens = configuredTokens.split(",");
            for (String token : tokens) {
                validTestTokens.add(token.trim());
            }
            logger.debug("✅ Test tokens configured: {} tokens available", tokens.length);
        }
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        // Extract request method - available for future use
        // String method = request.getMethod();
        
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
        
        // Validate token (JWT or test token)
        if (token == null || !isValidToken(token)) {
            logger.warn("🔐 Unauthorized access attempt to {} from {}", 
                path, request.getRemoteAddr());
            
            response.setStatus(401); // HttpServletResponse.SC_UNAUTHORIZED
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Unauthorized - Invalid or missing token\"}");
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
     * Validate token - accepts JWT tokens or configured test tokens
     */
    private boolean isValidToken(String token) {
        // Check if it's a configured test token first (faster path)
        if (validTestTokens.contains(token)) {
            logger.debug("✅ Test token validated");
            return true;
        }
        
        // Fall back to JWT validation
        return isValidJWTToken(token);
    }
    
    /**
     * Validate JWT bearer token
     */
    private boolean isValidJWTToken(String token) {
        if (!authService.isPresent()) {
            logger.warn("⚠️ AuthenticationService not available - allowing request");
            return true; // Dev mode
        }
        
        try {
            authService.get().validateToken(token);
            return true;
        } catch (Exception e) {
            logger.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
