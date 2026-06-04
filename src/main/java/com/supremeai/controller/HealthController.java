package com.supremeai.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check endpoint with dependency status. Provides real-time status of all critical
 * dependencies.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController implements HealthIndicator {

  private final org.springframework.beans.factory.ObjectProvider<RedisConnectionFactory>
      redisConnectionFactoryProvider;
  private final CircuitBreakerRegistry circuitBreakerRegistry;

  @org.springframework.beans.factory.annotation.Value("${supremeai.redis.mock-online:false}")
  private boolean mockOnline;

  public HealthController(
      org.springframework.beans.factory.ObjectProvider<RedisConnectionFactory>
          redisConnectionFactoryProvider,
      CircuitBreakerRegistry circuitBreakerRegistry) {
    this.redisConnectionFactoryProvider = redisConnectionFactoryProvider;
    this.circuitBreakerRegistry = circuitBreakerRegistry;
  }

  @Override
  public Health health() {
    Map<String, Object> details = new HashMap<>();

    // Check Redis
    if (mockOnline) {
      details.put("redis", "UP (MOCKED)");
    } else {
      RedisConnectionFactory factory = redisConnectionFactoryProvider.getIfAvailable();
      if (factory != null) {
        try {
          factory.getConnection().ping();
          details.put("redis", "UP");
        } catch (Exception e) {
          details.put("redis", "DOWN: " + e.getMessage());
        }
      } else {
        details.put("redis", "DISABLED");
      }
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
    if (mockOnline) {
      health.put("redis", Map.of("status", "UP", "mocked", true));
    } else {
      RedisConnectionFactory factory = redisConnectionFactoryProvider.getIfAvailable();
      if (factory != null) {
        try {
          factory.getConnection().ping();
          health.put("redis", Map.of("status", "UP"));
        } catch (Exception e) {
          health.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
        }
      } else {
        health.put("redis", Map.of("status", "DISABLED"));
      }
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
