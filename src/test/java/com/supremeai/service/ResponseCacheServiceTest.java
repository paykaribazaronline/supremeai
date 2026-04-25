package com.supremeai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResponseCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ResponseCacheService responseCacheService;

    @BeforeEach
    void setUp() throws Exception {
        responseCacheService = new ResponseCacheService();
        java.lang.reflect.Field field = ResponseCacheService.class.getDeclaredField("redisTemplate");
        field.setAccessible(true);
        field.set(responseCacheService, redisTemplate);
        responseCacheService.init();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void putAndGetUseLocalCacheEvenIfRedisFails() {
        doThrow(new RuntimeException("redis unavailable"))
            .when(valueOperations).set(any(), any(), anyLong(), any());

        responseCacheService.put("prompt", "response");

        assertEquals("response", responseCacheService.get("prompt"));
    }

    @Test
    void getBackfillsLocalCacheFromRedis() {
        when(valueOperations.get(startsWith("ai_resp:"))).thenReturn("redis-response");

        assertEquals("redis-response", responseCacheService.get("prompt"));
        assertEquals("redis-response", responseCacheService.get("prompt"));
    }

    @Test
    void clearRemovesLocalEntriesAndStatsRemainAccessible() {
        responseCacheService.put("prompt", "response");
        responseCacheService.clear();

        assertNull(responseCacheService.get("prompt"));
        assertNotNull(responseCacheService.getStats());
    }

    @Test
    void getStatsReflectsCacheAccess() {
        responseCacheService.put("prompt", "response");
        responseCacheService.get("prompt");
        responseCacheService.get("missing");

        ResponseCacheService.CacheStats stats = responseCacheService.getStats();

        assertEquals(1L, stats.size());
        assertEquals(1L, stats.hitCount());
    }
}
