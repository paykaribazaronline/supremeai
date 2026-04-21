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
                configuration.setAllowedOrigins(java.util.List.of("*"));
                configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(java.util.List.of("*"));
                configuration.setAllowCredentials(true);
                return configuration;
            }))
            
            // Keep most endpoints open, but lock admin config APIs.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/config/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
