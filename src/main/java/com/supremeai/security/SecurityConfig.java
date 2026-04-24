package com.supremeai.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

     @Bean
     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
         http
             // Disable CSRF for API simplicity
             .csrf(AbstractHttpConfigurer::disable)
             
             // Simple CORS for development
                         .cors(cors -> cors.configurationSource(request -> {
                 var configuration = new org.springframework.web.cors.CorsConfiguration();
                 configuration.setAllowedOriginPatterns(java.util.List.of("https://supremeai.com", "https://app.supremeai.com", "http://localhost:5173", "http://localhost:3000"));
                 configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                 configuration.setAllowedHeaders(java.util.List.of("*"));
                 configuration.setAllowCredentials(true);
                 return configuration;
             }))
             
             // Require authentication for all endpoints by default
             .authorizeHttpRequests(auth -> auth
                 // Public endpoints - authentication not required
                 .requestMatchers("/api/auth/firebase-login", "/api/auth/register", "/api/auth/forgot-password", "/api/auth/validate-token").permitAll()
                 .requestMatchers("/api/chat/**").permitAll()
                 
                 // Admin-only endpoints
                 .requestMatchers("/api/admin/**").hasRole("ADMIN")
                 .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                 
                 // All other endpoints require authentication
                 .anyRequest().authenticated()
             )
             
             // Add JWT authentication filter
             .addFilterBefore(jwtAuthFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

         return http.build();
     }
     
     @Bean
     public JwtAuthFilter jwtAuthFilter() {
         return new JwtAuthFilter();
     }
}
