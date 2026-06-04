package com.supremeai.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Multi-tier caching configuration for SupremeAI.
 *
 * <p>L1: Caffeine (local, in-memory) - 100k entries, 30min TTL L2: Redis (distributed) - 120min TTL
 *
 * <p>Provides fast local caching with distributed consistency.
 */
@Configuration
public class CacheConfig {

  @Value("${cache.l1.max-size:100000}")
  private int l1MaxSize;

  @Value("${cache.l1.expire-after-write:30}")
  private int l1ExpireAfterWriteMinutes;

  @Value("${cache.l2.expire-after-write:120}")
  private int l2ExpireAfterWriteMinutes;

  /**
   * L1 Cache: Caffeine for fast local caching - 100,000 entries maximum - 30 minute TTL - Fast
   * in-memory access for frequently requested data
   */

  /** L1 Cache Manager for local caching */
  @Bean
  @Primary
  public CaffeineCacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(
        Caffeine.newBuilder()
            .maximumSize(l1MaxSize)
            .expireAfterWrite(Duration.ofMinutes(l1ExpireAfterWriteMinutes))
            .recordStats());
    cacheManager.setCacheNames(
        Arrays.asList(
            "prompts",
            "patterns",
            "responses",
            "providers",
            "ai_responses",
            "user_sessions",
            "system_learning",
            "scrapedContent"));
    return cacheManager;
  }

  /** Raw Caffeine Cache bean for direct cache invalidation operations. */
  @Bean
  public Cache<String, Object> l1Cache() {
    return Caffeine.newBuilder()
        .maximumSize(l1MaxSize)
        .expireAfterWrite(Duration.ofMinutes(l1ExpireAfterWriteMinutes))
        .recordStats()
        .build();
  }

  /**
   * L2 Cache: Redis for distributed caching - 30 minute TTL - Shared across all instances - JSON
   * serialization for complex objects
   *
   * <p>Note: RedisCacheManager is now provided by RedisConfig.
   */
}
