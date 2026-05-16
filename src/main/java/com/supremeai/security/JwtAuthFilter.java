package com.supremeai.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Only enforce JWT auth on API routes.
        // This keeps static UI pages (e.g. /, /login.html) publicly reachable even with invalid headers.
        if (!path.startsWith("/api/") && !path.startsWith("/telemetry/")) {
            return true;
        }

        // Skip JWT auth for explicitly public or Firebase-handled API endpoints.
        return path.startsWith("/api/health") ||
               path.startsWith("/api/status") ||
               path.startsWith("/api/auth/") ||
               path.startsWith("/api/chat/") ||
               path.startsWith("/api/ext/") ||
               path.startsWith("/api/system") ||
               path.startsWith("/telemetry/") ||
               path.startsWith("/api/config/");
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     FilterChain chain) throws ServletException, IOException {
        
        // Skip JWT validation if another filter (e.g., AuthenticationFilter) already authenticated the user
        // This allows Firebase ID tokens to be used directly by the static admin panel
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            logger.debug("User already authenticated by previous filter, skipping JWT validation");
            chain.doFilter(request, response);
            return;
        }
        
        String token = extractToken(request);
        
        try {
            if (token != null && jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUsername(token);
                String role = jwtUtil.getRole(token);
                if (role == null) {
                    throw new JwtException("JWT role is missing");
                }
                
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(
                        username, null, 
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (ExpiredJwtException e) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token expired\",\"message\":\"Please login again\"}");
            return;
        } catch (JwtException e) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid token\",\"message\":\"Authentication token is invalid\"}");
            return;
        } catch (Exception e) {
            logger.error("JWT token validation error: " + e.getMessage(), e);
        }
        
        chain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
