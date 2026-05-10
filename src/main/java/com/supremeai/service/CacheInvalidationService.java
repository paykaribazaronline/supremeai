package com.supremeai.service;

import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache invalidation service for multi-tier caching strategy.
 * Handles automatic and manual cache invalidation with event-driven updates.
 */
@Slf4j
@Service
public class CacheInvalidationService {

    private final Cache<String, Object> l1Cache;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;
    
    // Track cache keys by pattern for bulk invalidation
    private final ConcurrentHashMap<String, Set<String>> keyPatterns = new ConcurrentHashMap<>();
    
    // Metrics
    private final Timer invalidationTimer;

    public CacheInvalidationService(
            Cache<String, Object> l1Cache,
            StringRedisTemplate redisTemplate,
            MeterRegistry meterRegistry) {
        this.l1Cache = l1Cache;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
        this.invalidationTimer = Timer.builder("cache.invalidation.duration")
                .description("Time taken for cache invalidation")
                .register(meterRegistry);
    }

    /**
     * Invalidate a single cache entry across all tiers.
     */
    public void invalidate(String key) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Invalidate L1 cache
            l1Cache.invalidate(key);
            
            // Invalidate L2 cache (Redis)
            redisTemplate.delete(key);
            
            log.debug("Invalidated cache key: {}", key);
        } finally {
            sample.stop(invalidationTimer);
        }
    }

    /**
     * Invalidate cache entries matching a pattern.
     */
    public void invalidatePattern(String pattern) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Invalidate L1 cache entries matching pattern
            l1Cache.asMap().keySet().removeIf(key -> key.matches(pattern.replace("*", ".*")));
            
            // Invalidate L2 cache entries matching pattern
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            
            log.info("Invalidated cache pattern: {} ({} keys)", pattern, keys != null ? keys.size() : 0);
        } finally {
            sample.stop(invalidationTimer);
        }
    }

    /**
     * Invalidate all caches (use with caution).
     */
    public void invalidateAll() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Invalidate L1 cache
            l1Cache.invalidateAll();
            
            // Invalidate L2 cache (Redis)
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            
            log.warn("Invalidated all caches");
        } finally {
            sample.stop(invalidationTimer);
        }
    }

    /**
     * Invalidate cache for a specific prompt type.
     */
    public void invalidatePromptCache(String promptType) {
        String pattern = "prompt:" + promptType + ":*";
        invalidatePattern(pattern);
    }

    /**
     * Invalidate cache for a specific provider.
     */
    public void invalidateProviderCache(String provider) {
        String pattern = "provider:" + provider + ":*";
        invalidatePattern(pattern);
    }

    /**
     * Schedule periodic cache cleanup for expired entries.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupExpiredEntries() {
        log.debug("Running cache cleanup for expired entries");
        // Caffeine handles this automatically, but we can log stats
        log.info("L1 Cache stats: {}", l1Cache.stats());
    }

    /**
     * Get cache statistics.
     */
    public String getCacheStats() {
        return String.format("L1 Cache: %s", l1Cache.stats());
    }
}