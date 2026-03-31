package org.example;

import org.example.filter.AuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private final AuthenticationFilter authenticationFilter;

    public Application(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    public static void main(String[] args) {
        try {
            logger.info("Starting SupremeAI Backend Service...");
            SpringApplication.run(Application.class, args);
            logger.info("SupremeAI Backend Service started successfully");
        } catch (Exception e) {
            logger.error("Failed to start SupremeAI Backend Service", e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/login.html",
                    "/actuator/health",
                    "/api/v1/data/health",
                    "/api/auth/login",
                    "/api/auth/bootstrap",
                    "/api/auth/hash-password",
                    "/api/auth/register",
                    "/api/auth/refresh",
                    "/error"
                ).permitAll()
                .requestMatchers("/webhook/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @GetMapping("/")
    public String home() {
        logger.info("Home endpoint accessed");
        return "SupremeAI Cloud Server is Running! Version: 3.5";
    }

    @GetMapping("/actuator/health")
    public String health() {
        logger.info("Health check endpoint accessed");
        return "{\"status\":\"UP\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}";
    }
}
