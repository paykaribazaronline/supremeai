package com.supremeai.security.ratelimit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.supremeai.config.RateLimitProperties;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class RateLimitingServiceTest {

  @Mock private RateLimitProperties properties;

  @Mock private RedisRateLimiter redisRateLimiter;

  @Mock private InMemoryRateLimiter inMemoryRateLimiter;

  @InjectMocks private RateLimitingService rateLimitingService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testDelegateToRedis_whenDistributed() {
    when(properties.isDistributed()).thenReturn(true);
    when(redisRateLimiter.tryAcquire("test", 100, 60)).thenReturn(true);

    boolean result = rateLimitingService.tryAcquire("test", 100, 60);

    assertTrue(result);
    verify(redisRateLimiter).tryAcquire("test", 100, 60);
    verifyNoInteractions(inMemoryRateLimiter);
  }

  @Test
  void testDelegateToInMemory_whenNotDistributed() {
    when(properties.isDistributed()).thenReturn(false);
    when(inMemoryRateLimiter.tryAcquire("test", 100, 60)).thenReturn(true);

    boolean result = rateLimitingService.tryAcquire("test", 100, 60);

    assertTrue(result);
    verify(inMemoryRateLimiter).tryAcquire("test", 100, 60);
    verifyNoInteractions(redisRateLimiter);
  }

  @Test
  void testGetStatus() {
    when(properties.isDistributed()).thenReturn(true);
    when(redisRateLimiter.getStatus("test")).thenReturn(Map.of("tokens", 10));

    Map<String, Object> status = rateLimitingService.getStatus("test");

    assertEquals(10, status.get("tokens"));
  }

  @Test
  void testTryAcquireReturnsFalse_whenRateLimitExceeded() {
    when(properties.isDistributed()).thenReturn(false);
    when(inMemoryRateLimiter.tryAcquire("limited", 1, 60)).thenReturn(false);

    boolean result = rateLimitingService.tryAcquire("limited", 1, 60);

    assertFalse(result);
  }

  @Test
  void testGetStatusReturnsEmptyMap_whenDistributedAndNoStatus() {
    when(properties.isDistributed()).thenReturn(true);
    when(redisRateLimiter.getStatus("unknown")).thenReturn(Map.of());

    Map<String, Object> status = rateLimitingService.getStatus("unknown");

    assertTrue(status.isEmpty());
  }

  @Test
  void testDifferentKeysAreIndependent() {
    when(properties.isDistributed()).thenReturn(false);
    when(inMemoryRateLimiter.tryAcquire("key1", 5, 60)).thenReturn(true);
    when(inMemoryRateLimiter.tryAcquire("key2", 5, 60)).thenReturn(true);

    boolean result1 = rateLimitingService.tryAcquire("key1", 5, 60);
    boolean result2 = rateLimitingService.tryAcquire("key2", 5, 60);

    assertTrue(result1);
    assertTrue(result2);
    verify(inMemoryRateLimiter).tryAcquire("key1", 5, 60);
    verify(inMemoryRateLimiter).tryAcquire("key2", 5, 60);
  }
}
