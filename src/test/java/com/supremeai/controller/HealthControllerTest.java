package com.supremeai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

  @Mock private ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;

  @Mock private RedisConnectionFactory redisConnectionFactory;

  @Mock private RedisConnection redisConnection;

  @Mock private CircuitBreakerRegistry circuitBreakerRegistry;

  @Mock private CircuitBreaker circuitBreaker;

  private HealthController controller;

  @BeforeEach
  void setUp() {
    when(redisConnectionFactoryProvider.getIfAvailable()).thenReturn(redisConnectionFactory);
    controller = new HealthController(redisConnectionFactoryProvider, circuitBreakerRegistry);
  }

  @Test
  void getHealth_shouldReturnUp_whenRedisIsAvailable() {
    when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
    when(redisConnection.ping()).thenReturn("PONG");
    when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Collections.emptySet());

    Map<String, Object> health = controller.getHealth();

    assertEquals("UP", health.get("status"));
    assertNotNull(health.get("timestamp"));
    Map<?, ?> redis = (Map<?, ?>) health.get("redis");
    assertEquals("UP", redis.get("status"));
  }

  @Test
  void getHealth_shouldReturnRedisDown_whenRedisUnavailable() {
    when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
    when(redisConnection.ping()).thenThrow(new RuntimeException("Connection refused"));
    when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Collections.emptySet());

    Map<String, Object> health = controller.getHealth();

    assertEquals("UP", health.get("status"));
    Map<?, ?> redis = (Map<?, ?>) health.get("redis");
    assertEquals("DOWN", redis.get("status"));
    assertTrue(redis.get("error").toString().contains("Connection refused"));
  }

  @Test
  void getHealth_shouldIncludeCircuitBreakerStates() {
    when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
    when(redisConnection.ping()).thenReturn("PONG");
    when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Set.of(circuitBreaker));
    when(circuitBreaker.getName()).thenReturn("ai-provider");
    when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.CLOSED);

    Map<String, Object> health = controller.getHealth();

    Map<?, ?> cbStates = (Map<?, ?>) health.get("circuitBreakers");
    assertNotNull(cbStates);
    assertEquals("CLOSED", cbStates.get("ai-provider"));
  }

  @Test
  void getHealth_shouldReturnEmptyCircuitBreakers_whenNoneRegistered() {
    when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
    when(redisConnection.ping()).thenReturn("PONG");
    when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Collections.emptySet());

    Map<String, Object> health = controller.getHealth();

    Map<?, ?> cbStates = (Map<?, ?>) health.get("circuitBreakers");
    assertNotNull(cbStates);
    assertTrue(cbStates.isEmpty());
  }

  @Test
  void health_actuatorShouldReturnUpWithRedisDetails() {
    when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
    when(redisConnection.ping()).thenReturn("PONG");
    when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Set.of(circuitBreaker));
    when(circuitBreaker.getName()).thenReturn("test-cb");
    when(circuitBreaker.getState()).thenReturn(CircuitBreaker.State.OPEN);

    Health health = controller.health();

    assertEquals(Health.up().build().getStatus(), health.getStatus());
    Map<String, Object> details = health.getDetails();
    assertEquals("UP", details.get("redis"));
    assertNotNull(details.get("circuitBreakers"));
  }

  @Test
  void health_actuatorShouldReturnRedisDownDetails() {
    when(redisConnectionFactory.getConnection()).thenThrow(new RuntimeException("Redis down"));
    when(circuitBreakerRegistry.getAllCircuitBreakers()).thenReturn(Collections.emptySet());

    Health health = controller.health();

    Map<String, Object> details = health.getDetails();
    assertTrue(details.get("redis").toString().contains("DOWN"));
  }
}
