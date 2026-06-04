package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ResponseCacheServiceTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;

  @Mock private ValueOperations<String, Object> valueOperations;

  private ResponseCacheService responseCacheService;

  @BeforeEach
  void setUp() throws Exception {
    responseCacheService = new ResponseCacheService(redisTemplate);
    responseCacheService.init(); // Initialize the cache
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
  }

  @Test
  void putAndGetUseLocalCacheEvenIfRedisFails() {
    doThrow(new RuntimeException("redis unavailable"))
        .when(valueOperations)
        .set(any(), any(), anyLong(), any());

    responseCacheService.putAiResponse("prompt", "response");

    assertEquals("response", responseCacheService.getAiResponse("prompt"));
  }

  @Test
  void getBackfillsLocalCacheFromRedis() {
    ResponseCacheService.CacheEntry cacheEntry =
        new ResponseCacheService.CacheEntry("redis-response", Duration.ofMinutes(30));
    when(valueOperations.get(startsWith("cache:ai-responses:"))).thenReturn(cacheEntry);

    assertEquals("redis-response", responseCacheService.getAiResponse("prompt"));
    assertEquals("redis-response", responseCacheService.getAiResponse("prompt"));
  }

  @Test
  void clearRemovesLocalEntriesAndStatsRemainAccessible() {
    responseCacheService.putAiResponse("prompt", "response");
    responseCacheService.clear();

    assertNull(responseCacheService.getAiResponse("prompt"));
    assertNotNull(responseCacheService.getStats());
  }

  @Test
  void getStatsReflectsCacheAccess() {
    responseCacheService.putAiResponse("prompt", "response");
    responseCacheService.getAiResponse("prompt");
    responseCacheService.getAiResponse("missing");

    ResponseCacheService.CacheStats stats = responseCacheService.getStats();

    assertEquals(1L, stats.size());
    assertEquals(1L, stats.hitCount());
  }
}
