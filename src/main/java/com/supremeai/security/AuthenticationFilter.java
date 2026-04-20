package com.supremeai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// @Component // Disabled: using Firebase auth filter instead
public class AuthenticationFilter extends OncePerRequestFilter {

    @Value("${supremeai.api.key:dev-api-key-taste-phase}")
    private String expectedApiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Skip auth for public endpoints
        return path.startsWith("/api/health") ||
               path.startsWith("/api/status") ||
               path.startsWith("/static/") ||
               path.equals("/") ||
               path.startsWith("/index.html");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        String apiKey = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            apiKey = authHeader.substring(7);
        }
        
        // Simple API key check (taste phase)
        if (apiKey != null && apiKey.equals(expectedApiKey)) {
            // Valid API key
            filterChain.doFilter(request, response);
        } else {
            // For now, just allow but log (taste phase - permissive)
            logger.warn("Request without valid API key: " + request.getRequestURI());
            filterChain.doFilter(request, response);
        }
    }
}
