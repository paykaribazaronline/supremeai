package com.supremeai.config;

import com.supremeai.filter.AuthenticationFilter;
import com.supremeai.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration with enhanced security headers.
 * Implements defense-in-depth with CSP, HSTS, XSS protection, and frame options.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private AuthenticationFilter authenticationFilter;
    
    @Autowired
    private JwtAuthFilter jwtAuthFilter;
    
    // Define AuthenticationFilter as a bean since it's no longer a @Component
    @Bean
    public AuthenticationFilter authenticationFilter() {
        return new AuthenticationFilter();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // CSRF disabled for stateless JWT authentication
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Security headers configuration
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://trusted.cdn.com; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                        "img-src 'self' data: https:; " +
                        "font-src 'self' https://fonts.gstatic.com; " +
                        "connect-src 'self' wss: https://api.supremeai.com; " +
                        "frame-ancestors 'none'; " +
                        "base-uri 'self'; " +
                        "form-action 'self'")
                )
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000) // 1 year
                    .includeSubDomains(true)
                )
                .xssProtection(xss -> xss.disable()) // Modern browsers use CSP instead
                .frameOptions(frame -> frame.deny())
            )
            .authorizeHttpRequests(auth -> auth
                // Allow static resources from common locations
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                // Public endpoints - specific routes only (not wildcards for security)
                .requestMatchers(
                   "/",
                    "/login",
                    "/login.html",
                    "/admin",
                    "/admin.html",
                    "/customer",
                    "/customer.html",
                    "/android-generator.html",
                    "/*.js",
                    "/*.css",
                    "/*.svg",
                    "/api/auth/firebase-login",
                    "/api/auth/register",
                    "/api/auth/forgot-password",
                    "/api/auth/validate-token",
                    "/api/health",
                    "/api/status",
                    "/api/config/firebase",
                    "/api/config/public",
                    "/__/firebase/**",
                    "/ws/**",
                    "/error",
                    "/api/v1/chat/completions"
                ).permitAll()
                 .requestMatchers("/api/debug/**").hasRole("ADMIN")
                 .requestMatchers("/api/security/**").hasRole("ADMIN")
                 .requestMatchers("/api/admin/**").hasRole("ADMIN")
                 .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                 .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, ex2) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Please login\"}");
                })
                .accessDeniedHandler((req, res, ex2) -> {
                    res.setStatus(403);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                }))
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
