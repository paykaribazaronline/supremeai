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

/**
 * Security Headers Filter
 * Adds comprehensive security headers to all HTTP responses
 * 
 * Headers added:
 * - Content-Security-Policy (CSP): Prevents XSS attacks
 * - X-Content-Type-Options: Prevents MIME type sniffing
 * - X-Frame-Options: Prevents clickjacking
 * - X-XSS-Protection: Browser XSS protection (legacy)
 * - Strict-Transport-Security: Forces HTTPS
 * - Referrer-Policy: Controls referrer information
 * - Permissions-Policy: Controls browser features
 /** - Cache-Control: Prevents caching of sensitive data
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersFilter.class);
    
    @Value("${supremeai.security.csp.enabled:true}")
    private boolean cspEnabled;
    
    @Value("${supremeai.security.hsts.enabled:true}")
    private boolean hstsEnabled;
    
    @Value("${supremeai.security.cors.enabled:true}")
    private boolean corsEnabled;
    
    @Value("${supremeai.app.domain:localhost}")
    private String appDomain;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Add security headers
            addSecurityHeaders(response);
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Error in security headers filter", e);
            filterChain.doFilter(request, response);
        }
    }
    
    private void addSecurityHeaders(HttpServletResponse response) {
        // Content-Security-Policy - Prevents XSS attacks
        if (cspEnabled) {
            String csp = "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdn.socket.io; " +
                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                        "img-src 'self' data: https:; " +
                        "font-src 'self' https://fonts.googleapis.com https://fonts.gstatic.com; " +
                        "connect-src 'self' https: wss:; " +
                        "frame-ancestors 'none'; " +
                        "base-uri 'self'; " +
                        "form-action 'self'";
            
            response.setHeader("Content-Security-Policy", csp);
        }
        
        // X-Content-Type-Options - Prevents MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // X-Frame-Options - Prevents clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        
        // X-XSS-Protection - Browser XSS protection (legacy but supported)
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Strict-Transport-Security - Forces HTTPS (only on HTTPS)
        if (hstsEnabled) {
            // max-age: 1 year (31536000 seconds)
            response.setHeader("Strict-Transport-Security", 
                              "max-age=31536000; includeSubDomains; preload");
        }
        
        // Referrer-Policy - Controls referrer information
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions-Policy (formerly Feature-Policy) - Controls browser features
        response.setHeader("Permissions-Policy",
                          "accelerometer=(), camera=(), geolocation=(), gyroscope=(), " +
                          "magnetometer=(), microphone=(), payment=(), usb=()");
        
        // Cache-Control - Prevents caching of sensitive data
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private, max-age=0");
        
        // Pragma - Legacy cache control (HTTP/1.0)
        response.setHeader("Pragma", "no-cache");
        
        // Expires - Legacy cache expiration (HTTP/1.0)
        response.setHeader("Expires", "0");
        
        // Remove server information
        response.setHeader("Server", "SupremeAI");
        response.setHeader("X-Powered-By", "");
        
        // Add custom security headers
        response.setHeader("X-Content-Security-Policy", "default-src 'self'");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        
        logger.debug("Security headers added to response");
    }
}
