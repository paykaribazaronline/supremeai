package com.supremeai.filter;

import org.springframework.stereotype.Component;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Only enforce Firebase auth on API routes.
        // This keeps static UI pages (e.g. /admin.html, /login.html) publicly reachable.
        if (!path.startsWith("/api/") && !path.startsWith("/telemetry/")) {
            return true;
        }

        // Skip Firebase auth for explicitly public API endpoints.
        return path.startsWith("/api/health") ||
               path.startsWith("/api/status") ||
               path.startsWith("/api/auth/") ||
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
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Skip if already authenticated (e.g., by JwtAuthFilter for backend JWT tokens)
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            logger.debug("User already authenticated, skipping Firebase token verification");
            filterChain.doFilter(request, response);
            return;
        }
        
        String authHeader = request.getHeader("Authorization");
        String guestHeader = request.getHeader("X-Guest-Access");
        String idToken = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            idToken = authHeader.substring(7);
        }
        
        if ("true".equals(guestHeader) || "GUEST_MODE".equals(idToken)) {
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_GUEST"));
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken("guest_user", null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }
        
        if (idToken == null || idToken.trim().isEmpty()) {
            logger.debug("No authentication token found in header, continuing filter chain");
            filterChain.doFilter(request, response);
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
            
            logger.debug("Authenticated user via Firebase ID token: " + uid);
            filterChain.doFilter(request, response);
            
        } catch (FirebaseAuthException e) {
            // Firebase ID token validation failed - this could be a backend JWT or invalid token.
            // If there's a token but it's not a valid Firebase ID token, let JwtAuthFilter handle it.
            logger.debug("Firebase ID token verification failed, allowing JwtAuthFilter to try: " + e.getMessage());
            filterChain.doFilter(request, response);
        }
    }
    
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        String json = "{\"error\":\"" + message + "\",\"status\":401}";
        response.getWriter().write(json);
    }
}
