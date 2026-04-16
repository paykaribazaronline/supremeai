package com.supremeai.security;

import com.supremeai.service.QuotaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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

        // Check for API key in header
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            sendErrorResponse(response, "API key required", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Validate API key and check quota
        if (!quotaService.hasQuotaRemaining(apiKey)) {
            sendErrorResponse(response, "API quota exceeded", HttpServletResponse.SC_TOO_MANY_REQUESTS);
            return;
        }

        // Increment usage
        if (!quotaService.incrementUsage(apiKey)) {
            sendErrorResponse(response, "API quota exceeded", HttpServletResponse.SC_TOO_MANY_REQUESTS);
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