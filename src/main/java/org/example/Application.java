package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Phase 5: Spring Boot Application Entry Point
 * 
 * Starts REST API server on port 8080
 * Initializes all Spring components and services
 * 
 * Configuration:
 * - Port: 8080 (configurable via application.properties)
 * - Context path: /
 * - Servlet: DispatcherServlet
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "org.example.controller",
    "org.example.service",
    "org.example.filter",
    "org.example.exception",
    "org.example.config"
})
public class Application {
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
