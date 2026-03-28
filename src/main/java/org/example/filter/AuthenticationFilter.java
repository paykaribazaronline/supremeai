package org.example.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Phase 5: Authentication Filter (Updated for JWT)
 *
 * Validates JWT bearer tokens for REST API endpoints.
 * Skips public endpoints (health, webhook, auth login/register).
 * Also accepts simple test tokens from supremeai.api.tokens config for testing.
 *
 * Token format: Authorization: Bearer <JWT_token>  or  Authorization: Bearer <test-token>
 */
@Component
public class AuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    // Public endpoints that do not require authentication.
    // NOTE: "/" was intentionally removed — startsWith("/") matches every path.
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
        "/login.html"
    );

    @Value("${supremeai.api.tokens:test-token,valid-token,admin-token,dev-token,user-token}")
    private String configuredTokens;

    private java.util.Optional<org.example.service.AuthenticationService> authService;
    private final Set<String> validTestTokens = new HashSet<>();

    /**
     * Default constructor for non-Spring use (e.g., unit tests).
     * Attempts to load configured tokens from the test classpath so that
     * tests using {@code new AuthenticationFilter()} still get a populated token set.
     */
    public AuthenticationFilter() {
        this.authService = java.util.Optional.empty();
        // Try to load tokens from test classpath resource so plain-Mockito tests work.
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("application-test.properties")) {
            if (is != null) {
                props.load(is);
                String tokens = props.getProperty("supremeai.api.tokens", "");
                if (!tokens.isEmpty()) {
                    configuredTokens = tokens;
                }
            }
        } catch (IOException ignored) {
            // Silently fall back to empty token list
        }
        initializeTestTokens();
    }

    @PostConstruct
    public void postConstruct() {
        // In Spring context, @Value has already been injected. Re-initialize so
        // tokens are populated even when AuthenticationService is absent.
        initializeTestTokens();
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setAuthService(org.example.service.AuthenticationService service) {
        this.authService = java.util.Optional.of(service);
        initializeTestTokens();
    }

    private void initializeTestTokens() {
        if (configuredTokens != null && !configuredTokens.isEmpty()) {
            Arrays.stream(configuredTokens.split(","))
                  .map(String::trim)
                  .filter(t -> !t.isEmpty())
                  .forEach(validTestTokens::add);
            logger.debug("✅ Test tokens configured: {} tokens available", validTestTokens.size());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod(); // Log request method for observability

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract bearer token (case-insensitive "bearer " prefix)
        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            token = authHeader.substring(7); // keep exact casing of the token itself
        }

        // Validate token (configured test token or JWT)
        if (token == null || token.isEmpty() || !isValidToken(token)) {
            logger.warn("🔐 Unauthorized {} {} from {}", method, path, request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or invalid authorization token");
            return;
        }

        logger.debug("✅ Authentication passed for {} {}", method, path);
        filterChain.doFilter(request, response);
    }

    /**
     * Returns true when the path does not require authentication.
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Validates a token against the configured token list, then falls back to JWT.
     */
    private boolean isValidToken(String token) {
        if (validTestTokens.contains(token)) {
            logger.debug("✅ Test token validated");
            return true;
        }
        return isValidJWTToken(token);
    }

    /**
     * Validates a JWT bearer token via the injected AuthenticationService.
     * Returns false when no auth service is available (no dev-mode passthrough).
     */
    private boolean isValidJWTToken(String token) {
        if (!authService.isPresent()) {
            logger.debug("⚠️ AuthenticationService not available — rejecting non-listed token");
            return false;
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
