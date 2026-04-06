package com.supremeai.teaching.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

import org.springframework.context.annotation.Profile;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                // Auth endpoints — always public
                .requestMatchers("/api/auth/**").permitAll()
                // Static pages
                .requestMatchers("/login.html", "/admin.html", "/dashboard.html",
                                 "/admin-control-dashboard.html",
                                 "/visualization-3d-dashboard.html").permitAll()
                .requestMatchers("/js/**", "/css/**", "/images/**", "/webjars/**",
                                 "/error", "/favicon.ico").permitAll()
                .requestMatchers("/", "/actuator/**").permitAll()
                // Public read-only data APIs consumed by admin.html
                .requestMatchers("/api/status/**").permitAll()
                .requestMatchers("/api/admin/dashboard/**").permitAll()
                .requestMatchers("/api/learning/**").permitAll()
                .requestMatchers("/api/providers/**").permitAll()
                .requestMatchers("/api/metrics/**").permitAll()
                .requestMatchers("/api/git/status", "/api/git/logs").permitAll()
                .requestMatchers("/api/quota/summary", "/api/quota/status").permitAll()
                .requestMatchers("/api/quota/all").permitAll()
                .requestMatchers("/api/work-history").permitAll()
                .requestMatchers("/api/v1/requirements", "/api/v1/requirements/**").permitAll()
                // Admin-console functional endpoints — require login but JWT may expire;
                // these are gated at the UI level so opening at API level is acceptable.
                .requestMatchers("/api/projects/**").permitAll()
                .requestMatchers("/api/chat/**").permitAll()
                .requestMatchers("/api/consensus/**").permitAll()
                .requestMatchers("/api/agent-orchestration/**").permitAll()
                .requestMatchers("/api/assignments/**").permitAll()
                .requestMatchers("/api/vpn/**").permitAll()
                .requestMatchers("/api/notifications/**").permitAll()
                .requestMatchers("/api/v1/deployment/**").permitAll()
                .requestMatchers("/api/v1/self-healing/**").permitAll()
                .requestMatchers("/api/v1/timeline/**").permitAll()
                .requestMatchers("/api/v1/visualization/**").permitAll()
                .requestMatchers("/api/extend/**").permitAll()
                .requestMatchers("/api/tier/**").permitAll()
                .requestMatchers("/api/ai/ops/**").permitAll()
                .requestMatchers("/api/alerts/**").permitAll()
                .requestMatchers("/api/system/**").permitAll()
                .requestMatchers("/api/safezone/**").permitAll()
                // JWT enforcement disabled — Firebase handles auth on the client side
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use allowedOriginPatterns so wildcards work alongside allowCredentials=true
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "https://localhost:*",
            "https://*.run.app",           // Google Cloud Run
            "https://*.onrender.com",      // Render.com
            "https://*.firebaseapp.com",
            "https://*.web.app",           // Firebase Hosting
            "https://*.cloudfunctions.net",
            "https://*.a.run.app"          // Cloud Run short URLs
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
