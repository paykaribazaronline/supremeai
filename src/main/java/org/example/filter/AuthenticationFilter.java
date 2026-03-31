package org.example.filter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates bearer tokens for protected REST API endpoints.
 */
@Component
public class AuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
        "/",
        "/webhook",
        "/index.html",
        "/login.html",
        "/actuator/health",
        "/api/v1/data/health",
        "/api/auth/login",
        "/api/auth/bootstrap",
        "/api/auth/hash-password",
        "/api/auth/register",
        "/api/auth/refresh"
    );
    private static final List<String> PUBLIC_PATH_PREFIXES = List.of("/webhook/");

    @Value("${supremeai.api.tokens:}")
    private String configuredTokens;

    private Optional<org.example.service.AuthenticationService> authService = Optional.empty();
    private final Set<String> validTestTokens = new HashSet<>();

    @PostConstruct
    public void init() {
        initializeTestTokens();
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setAuthService(org.example.service.AuthenticationService service) {
        this.authService = Optional.of(service);
    }

    private void initializeTestTokens() {
        validTestTokens.clear();
        if (configuredTokens == null || configuredTokens.isBlank()) {
            return;
        }

        for (String token : configuredTokens.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                validTestTokens.add(trimmed);
            }
        }
        logger.debug("Configured {} test tokens", validTestTokens.size());
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<UsernamePasswordAuthenticationToken> authentication = resolveAuthentication(
            request.getHeader("Authorization")
        );
        if (authentication.isEmpty()) {
            logger.warn("Unauthorized access attempt to {} from {}", path, request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authorization token");
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication.get());
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_EXACT_PATHS.contains(path)
            || PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Optional<UsernamePasswordAuthenticationToken> resolveAuthentication(String authHeader) {
        String token = extractBearerToken(authHeader);
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        if (validTestTokens.contains(token)) {
            List<String> roles = token.toLowerCase(Locale.ROOT).contains("admin")
                ? List.of("ROLE_ADMIN")
                : List.of("ROLE_TEST");
            return Optional.of(createAuthentication(token, roles));
        }

        return resolveJwtAuthentication(token);
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null) {
            return null;
        }

        int separator = authHeader.indexOf(' ');
        if (separator <= 0) {
            return null;
        }

        String scheme = authHeader.substring(0, separator);
        if (!"bearer".equalsIgnoreCase(scheme)) {
            return null;
        }

        return authHeader.substring(separator + 1).trim();
    }

    private Optional<UsernamePasswordAuthenticationToken> resolveJwtAuthentication(String token) {
        if (authService.isEmpty()) {
            logger.warn("AuthenticationService not available - rejecting non-test token");
            return Optional.empty();
        }

        try {
            User user = authService.get().validateToken(token);
            List<String> authorities = new ArrayList<>();
            authorities.add("ROLE_" + user.getRole().toUpperCase(Locale.ROOT));
            authorities.addAll(user.getPermissions());
            return Optional.of(createAuthentication(user.getUsername(), authorities));
        } catch (Exception e) {
            logger.debug("Invalid JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private UsernamePasswordAuthenticationToken createAuthentication(String principal, List<String> authorities) {
        return new UsernamePasswordAuthenticationToken(
            principal,
            null,
            authorities.stream()
                .filter(Objects::nonNull)
                .filter(authority -> !authority.isBlank())
                .map(SimpleGrantedAuthority::new)
                .toList()
        );
    }
}
