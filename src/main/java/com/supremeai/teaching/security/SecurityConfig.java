package com.supremeai.teaching.security;

import com.supremeai.security.ApiKeyFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private ApiKeyFilter apiKeyFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain publicEndpointsFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                new AntPathRequestMatcher("/"),
                new AntPathRequestMatcher("/index.html"),
                new AntPathRequestMatcher("/admin.html"),
                new AntPathRequestMatcher("/customer.html"),
                new AntPathRequestMatcher("/login.html"),
                new AntPathRequestMatcher("/static/**"),
                new AntPathRequestMatcher("/css/**"),
                new AntPathRequestMatcher("/js/**"),
                new AntPathRequestMatcher("/images/**"),
                new AntPathRequestMatcher("/api/status/**"),
                new AntPathRequestMatcher("/actuator/health/**"),
                new AntPathRequestMatcher("/api/config/**"),
                new AntPathRequestMatcher("/api/auth/**")
            )
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain applicationFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
