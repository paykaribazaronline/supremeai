package com.supremeai.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Arrays;

/**
 * Multi-tier caching configuration for SupremeAI.
 * 
 * L1: Caffeine (local, in-memory) - 10k entries, 10min TTL
 * L2: Redis (distributed) - 30min TTL
 * 
 * Provides fast local caching with distributed consistency.
 */
@Configuration
public class CacheConfig {

    @Value("${cache.l1.max-size:10000}")
    private int l1MaxSize;

    @Value("${cache.l1.expire-after-write:10}")
    private int l1ExpireAfterWriteMinutes;

    @Value("${cache.l2.expire-after-write:30}")
    private int l2ExpireAfterWriteMinutes;

    /**
     * L1 Cache: Caffeine for fast local caching
     * - 10,000 entries maximum
     * - 10 minute TTL
     * - Fast in-memory access for frequently requested data
     */


    /**
     * L1 Cache Manager for local caching
     */
    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(l1MaxSize)
            .expireAfterWrite(Duration.ofMinutes(l1ExpireAfterWriteMinutes))
            .recordStats());
        cacheManager.setCacheNames(Arrays.asList("prompts", "patterns", "responses", "providers"));
        return cacheManager;
    }

    /**
     * L2 Cache: Redis for distributed caching
     * - 30 minute TTL
     * - Shared across all instances
     * - JSON serialization for complex objects
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(l2ExpireAfterWriteMinutes))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(config)
            .transactionAware()
            .build();
    }
}