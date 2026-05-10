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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
             .cors(cors -> cors.configurationSource(corsConfigurationSource()))
             .csrf(csrf -> csrf
                 .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                 .ignoringRequestMatchers("/api/auth/**", "/api/health/**", "/api/ext/**")
             )
             .sessionManagement(session -> 
                 session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            // Security headers configuration
                 .headers(headers -> headers
                 .contentSecurityPolicy(csp -> csp
                     .policyDirectives("default-src 'self'; " +
                         "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://trusted.cdn.com https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                         "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                         "img-src 'self' data: https:; " +
                         "font-src 'self' https://fonts.gstatic.com; " +
                         "connect-src 'self' wss: https://identitytoolkit.googleapis.com https://securetoken.googleapis.com https://*.googleapis.com https://*.firebaseio.com https://api.supremeai.com http://localhost:* ws://localhost:* http://127.0.0.1:* ws://127.0.0.1:*; " +
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
                    "/customer",
                    "/customer.html",
                    "/android-generator.html",
                    "/*.js",
                    "/*.css",
                    "/*.svg",
                    "/*.json",
                    "/*.ico",
                    "/assets/**",
                    "/static/**",
                    "/manifest.json",
                    "/sw.js",
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
                    "/api/v1/chat/completions",
                    "/api/ext/**",
                    "/admin",
                    "/admin/**",
                    "/admin/index.html"
                ).permitAll()
                 .requestMatchers("/api/debug/**").hasRole("ADMIN")
                 .requestMatchers("/api/security/**").hasRole("ADMIN")
                 .requestMatchers("/api/admin/**").hasRole("ADMIN")
                 .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                 .requestMatchers("/api/v1/agents/**").hasRole("ADMIN")
                 .requestMatchers("/api/v1/optimization/**").hasRole("ADMIN")
                 .requestMatchers("/api/v1/phase6/**").hasRole("ADMIN")
                 .requestMatchers("/api/phase7/**").hasRole("ADMIN")
                 .requestMatchers("/api/v1/agents/phase8/**").hasRole("ADMIN")
                 .requestMatchers("/api/v1/agents/phase9/**").hasRole("ADMIN")
                 .requestMatchers("/api/v1/agents/phase10/**").hasRole("ADMIN")
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

    /**
     * CORS configuration for API endpoints.
     * Allows localhost development and production domains.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://*.supremeai.com",
            "https://supremeai.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
