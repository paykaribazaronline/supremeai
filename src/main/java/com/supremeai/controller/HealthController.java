package com.supremeai.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint with dependency status.
 * Provides real-time status of all critical dependencies.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public HealthController(RedisConnectionFactory redisConnectionFactory,
                           CircuitBreakerRegistry circuitBreakerRegistry) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        
        // Check Redis
        try {
            redisConnectionFactory.getConnection().ping();
            details.put("redis", "UP");
        } catch (Exception e) {
            details.put("redis", "DOWN: " + e.getMessage());
        }
        
        // Check circuit breakers
        Map<String, String> circuitBreakerStates = new HashMap<>();
        for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
            circuitBreakerStates.put(cb.getName(), cb.getState().name());
        }
        details.put("circuitBreakers", circuitBreakerStates);
        
        return Health.up().withDetails(details).build();
    }

    @GetMapping
    public Map<String, Object> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        
        // Redis status
        try {
            redisConnectionFactory.getConnection().ping();
            health.put("redis", Map.of("status", "UP"));
        } catch (Exception e) {
            health.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
        }
        
        // Circuit breaker states
        Map<String, String> cbStates = new HashMap<>();
        for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
            cbStates.put(cb.getName(), cb.getState().name());
        }
        health.put("circuitBreakers", cbStates);
        
        return health;
    }
}
