package com.supremeai.security;

import com.supremeai.service.QuotaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Autowired
    private QuotaService quotaService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip filter for certain paths
        if (path.startsWith("/api/auth/") ||
            path.startsWith("/api/status/") ||
            path.startsWith("/api/admin/") ||
            path.startsWith("/api/config/") ||
            path.equals("/api/user/apis") && request.getMethod().equals("GET") ||
            path.startsWith("/api/user/apis/") && (request.getMethod().equals("POST") || request.getMethod().equals("DELETE"))) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check for API key in header (either X-API-Key or Authorization Bearer)
        String apiKey = null;

        // Check X-API-Key header (legacy support)
        apiKey = request.getHeader("X-API-Key");

        // If not found, check Authorization header for Bearer token
        if (apiKey == null || apiKey.trim().isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                apiKey = authHeader.substring(7); // Remove "Bearer " prefix
            }
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            sendErrorResponse(response, "API key required", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Validate API key and check quota
        if (!quotaService.hasQuotaRemaining(apiKey)) {
            sendErrorResponse(response, "API quota exceeded", 429);
            return;
        }

        // Increment usage
        if (!quotaService.incrementUsage(apiKey)) {
            sendErrorResponse(response, "API quota exceeded", 429);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\":\"%s\"}", message));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Don't filter static resources, login pages, etc.
        return path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.endsWith(".html") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg") ||
               path.endsWith(".jpeg") ||
               path.endsWith(".gif") ||
               path.endsWith(".ico");
    }
}
