package org.example.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private static final String ADMIN_AUTH_COOKIE = "supremeai_admin_token";
    
    // Public endpoints that don't require authentication
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/webhook",
        "/api/v1/data/health",
        "/actuator/health",
        "/api/auth/login",
        "/api/auth/firebase-login",
        "/api/auth/bootstrap",
        "/api/auth/hash-password",
        "/api/auth/register",
        "/api/auth/refresh",
        "/index.html",
        "/login.html",
        "/"
    );

    private static final Set<String> ADMIN_PAGE_PATHS = Set.of(
        "/admin",
        "/admin.html",
        "/admin-control-dashboard.html"
    );

    private static final Set<String> ADMIN_API_PREFIXES = Set.of(
        "/api/admin/",
        "/api/auth/register",
        "/api/auth/users"
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
        
        String token = extractToken(request);
        org.example.model.User authenticatedUser = authenticateToken(token);

        if (authenticatedUser == null) {
            logger.warn("\uD83D\uDD10 Unauthorized access attempt to {} from {}",
                path, request.getRemoteAddr());
            writeUnauthorizedResponse(request, response, path);
            return;
        }

        if (isAdminProtectedPath(path) && !isAdminUser(authenticatedUser)) {
            logger.warn("⛔ Non-admin access attempt to {} by {} from {}",
                path, authenticatedUser.getUsername(), request.getRemoteAddr());
            writeForbiddenResponse(request, response, path);
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
            .anyMatch(p -> p.equals("/") ? path.equals("/") : path.startsWith(p));
    }

    private boolean isAdminProtectedPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        if (ADMIN_PAGE_PATHS.contains(path)) {
            return true;
        }

        return ADMIN_API_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            return authHeader.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (ADMIN_AUTH_COOKIE.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }

        return null;
    }
    
    /**
     * Validate token - accepts JWT tokens or configured test tokens
     */
    private org.example.model.User authenticateToken(String token) {
        // Check if it's a configured test token first (faster path)
        if (validTestTokens.contains(token)) {
            logger.debug("✅ Test token validated");
            return new org.example.model.User();
        }
        
        // Fall back to JWT validation
        return validateJWTToken(token);
    }
    
    /**
     * Validate JWT bearer token
     */
    private org.example.model.User validateJWTToken(String token) {
        if (!authService.isPresent()) {
            logger.warn("⚠️ AuthenticationService not available - rejecting protected request");
            return null;
        }
        
        try {
            return authService.get().validateToken(token);
        } catch (Exception e) {
            logger.debug("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    private boolean isAdminUser(org.example.model.User user) {
        return authService.isPresent() && authService.get().isAdmin(user);
    }

    private void writeUnauthorizedResponse(HttpServletRequest request,
                                           HttpServletResponse response,
                                           String path) throws IOException {
        if (wantsHtmlResponse(request, path)) {
            response.sendRedirect("/login.html?redirect=" + URLEncoder.encode(path, StandardCharsets.UTF_8));
            return;
        }

        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        long ts = System.currentTimeMillis();
        response.getWriter().write(
            "{\"error\":\"unauthorized\",\"message\":\"Missing or invalid authorization token\","
            + "\"status\":401,\"path\":\"" + path + "\",\"timestamp\":\"" + ts + "\"}");
    }

    private void writeForbiddenResponse(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String path) throws IOException {
        if (wantsHtmlResponse(request, path)) {
            response.sendRedirect("/login.html?denied=admin");
            return;
        }

        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        long ts = System.currentTimeMillis();
        response.getWriter().write(
            "{\"error\":\"forbidden\",\"message\":\"Admin access required\","
            + "\"status\":403,\"path\":\"" + path + "\",\"timestamp\":\"" + ts + "\"}");
    }

    private boolean wantsHtmlResponse(HttpServletRequest request, String path) {
        String accept = request.getHeader("Accept");
        return (accept != null && accept.contains("text/html"))
            || ADMIN_PAGE_PATHS.contains(path)
            || (path != null && path.endsWith(".html"));
    }
}
