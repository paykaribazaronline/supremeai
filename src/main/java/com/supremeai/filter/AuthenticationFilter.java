package com.supremeai.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(1) // Run early in filter chain
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Skip Firebase auth for public endpoints
        return path.startsWith("/api/health") ||
               path.startsWith("/api/status") ||
               path.startsWith("/api/auth/") ||
               path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.equals("/") ||
               path.startsWith("/index.html") ||
               path.startsWith("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        String idToken = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            idToken = authHeader.substring(7);
        }
        
        if (idToken == null || idToken.trim().isEmpty()) {
            logger.warn("Missing or invalid Authorization header for request: " + request.getRequestURI());
            sendUnauthorized(response, "Missing authentication token");
            return;
        }
        
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            
            // Extract role claims
            Object roleClaim = decodedToken.getClaims().get("role");
            Object adminClaim = decodedToken.getClaims().get("admin");
            
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            if ("ADMIN".equals(roleClaim) || Boolean.TRUE.equals(adminClaim)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            
            // Create Authentication token and set in SecurityContext
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(uid, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            logger.debug("Authenticated user: " + uid);
            filterChain.doFilter(request, response);
            
        } catch (FirebaseAuthException e) {
            logger.warn("Firebase token verification failed: " + e.getMessage());
            sendUnauthorized(response, "Invalid or expired authentication token");
        }
    }
    
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        String json = "{\"error\":\"" + message + "\",\"status\":401}";
        response.getWriter().write(json);
    }
}
