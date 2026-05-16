package com.supremeai.config;

import com.supremeai.filter.AuthenticationFilter;
import com.supremeai.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Autowired
    private AuthenticationFilter authenticationFilter;
    
    @Value("${cors.allowed-origins:}")
    private String allowedOriginsCsv;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
             .cors(cors -> cors.configurationSource(corsConfigurationSource()))
             .csrf(csrf -> csrf
                 .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                 .ignoringRequestMatchers("/api/auth/**", "/api/health/**", "/api/ext/**", "/api/browser/**", "/api/system/**", "/api/admin/**", "/api/workflows/**", "/ws/**")
             )
             .sessionManagement(session -> 
                 session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
             // Security headers configuration
             .headers(headers -> headers
                 .contentSecurityPolicy(csp -> csp
                     .policyDirectives(
                         "default-src 'self'; " +
                         "script-src 'self' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                         "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                         "img-src 'self' data: blob: https:; " +
                         "font-src 'self' https://fonts.gstatic.com; " +
                         "connect-src 'self' https://identitytoolkit.googleapis.com https://securetoken.googleapis.com https://*.googleapis.com https://*.firebaseio.com https://api.supremeai.com https://supremeai-a.web.app https://supremeai-a.firebaseapp.com https://*.run.app wss: ws://localhost:8080 ws://localhost:5173; " +
                         "frame-ancestors 'none'; " +
                         "base-uri 'self'; " +
                         "form-action 'self'; " +
                         "object-src 'none'"
                     )
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
                    "/api/system/**",
                    "/api/system/health",
                    "/api/v2/intelligence/**",
                    "/api/generate/**",
                    "/public/**",
                    "/telemetry/**",
                    "/__/firebase/**",
                    "/ws/**",
                    "/error",
                    "/api/v1/chat/completions",
                    "/api/chat/**",
                    "/api/admin/chat/**",
                    "/api/ext/**",
                    "/api/workflows/**",
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
             .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
             .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
         
         return http.build();
     }

    /**
     * CORS configuration for API endpoints.
     * Uses whitelist from application properties (cors.allowed-origins).
     * Falls back to localhost development and production domain.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        List<String> origins;
        if (allowedOriginsCsv != null && !allowedOriginsCsv.trim().isEmpty()) {
            origins = Arrays.stream(allowedOriginsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } else {
            // Safe defaults for development and production
            origins = Arrays.asList(
                "http://localhost:5173",   // Dashboard dev
                "http://localhost:3000",   // Admin/dev alternative
                "https://supremeai-a.web.app"  // Production
            );
        }
        
        configuration.setAllowedOriginPatterns(origins);
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With", 
            "Accept", 
            "Origin", 
            "Access-Control-Request-Method", 
            "Access-Control-Request-Headers",
            "X-CSRF-TOKEN",
            "X-Firebase-Id-Token"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin", 
            "Access-Control-Allow-Credentials",
            "Content-Disposition"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
