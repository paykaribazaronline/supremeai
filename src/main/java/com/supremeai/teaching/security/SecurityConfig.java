package com.supremeai.teaching.security;

import com.supremeai.security.ApiKeyFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private ApiKeyFilter apiKeyFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/", "/index.html", "/customer.html", "/login.html",
                               "/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/api/status/**", "/actuator/health/**").permitAll()
                .requestMatchers("/api/config/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()

                // Protected admin endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // User API management (requires authentication)
                .requestMatchers("/api/user/**").authenticated()

                // API usage endpoints (require valid API key)
                .anyRequest().permitAll() // API key filter will handle validation
            )
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
