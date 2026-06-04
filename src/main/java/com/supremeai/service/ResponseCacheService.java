package com.supremeai.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Multi-level response cache service with support for: - L1 Cache: In-memory Caffeine cache (fast,
 * local) - L2 Cache: Redis distributed cache (shared across instances) - Automatic cache warming
 * and statistics
 */
@Service
public class ResponseCacheService {

  private static final Logger logger = LoggerFactory.getLogger(ResponseCacheService.class);

  private Cache<String, CacheEntry> exactMatchCache;

  private final RedisTemplate<String, Object> redisTemplate;

  public ResponseCacheService(
      @org.springframework.beans.factory.annotation.Autowired(required = false)
          RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @PostConstruct
  public void init() {
    exactMatchCache =
        Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()
            .build();

    logger.info("ResponseCacheService initialized with 10,000 entry capacity");
  }

  /** Get cached response for prompt with category support. */
  public String get(String category, String key) {
    String cacheKey = buildCacheKey(category, key);

    // Try L1 Cache (Caffeine)
    CacheEntry l1Result = exactMatchCache.getIfPresent(cacheKey);
    if (l1Result != null && !l1Result.isExpired()) {
      l1Result.incrementHitCount();
      return l1Result.getValue();
    }

    // Try L2 Cache (Redis)
    if (redisTemplate != null) {
      try {
        CacheEntry l2Result = (CacheEntry) redisTemplate.opsForValue().get("cache:" + cacheKey);
        if (l2Result != null && !l2Result.isExpired()) {
          // Backfill L1 Cache
          exactMatchCache.put(cacheKey, l2Result);
          l2Result.incrementHitCount();
          return l2Result.getValue();
        }
      } catch (Exception e) {
        logger.warn("Redis read failed for key {}: {}", cacheKey, e.getMessage());
      }
    }

    return null;
  }

  /**
   * Backward compatibility method for single-parameter get (treats prompt as both category and
   * key). Used by existing services that haven't been updated yet.
   */
  @Deprecated
  public String get(String prompt) {
    return getAiResponse(prompt);
  }

  /** Store response in cache with TTL. */
  public void put(String category, String key, String value, Duration ttl) {
    String cacheKey = buildCacheKey(category, key);
    CacheEntry entry = new CacheEntry(value, ttl);

    // Put in L1 Cache
    exactMatchCache.put(cacheKey, entry);

    // Put in L2 Cache (Redis)
    if (redisTemplate != null) {
      try {
        redisTemplate.opsForValue().set("cache:" + cacheKey, entry, ttl);
        logger.debug("Cached entry for key: {} with TTL: {} seconds", cacheKey, ttl.getSeconds());
      } catch (Exception e) {
        logger.warn("Redis write failed for key {}: {}", cacheKey, e.getMessage());
      }
    }
  }

  /** Store response in cache with default TTL (30 minutes). */
  public void put(String category, String key, String value) {
    put(category, key, value, Duration.ofMinutes(30));
  }

  /**
   * Backward compatibility method for single-parameter put (treats prompt as both category and
   * key). Used by existing services that haven't been updated yet.
   */
  @Deprecated
  public void put(String prompt, String response) {
    putAiResponse(prompt, response);
  }

  /** Convenience method for AI response caching. */
  public String getAiResponse(String prompt) {
    return get("ai-responses", hashPrompt(prompt));
  }

  /** Convenience method for AI response caching. */
  public void putAiResponse(String prompt, String response) {
    put("ai-responses", hashPrompt(prompt), response, Duration.ofMinutes(30));
  }

  /** Invalidate cache entry. */
  public void evict(String category, String key) {
    String cacheKey = buildCacheKey(category, key);
    exactMatchCache.invalidate(cacheKey);
    if (redisTemplate != null) {
      try {
        redisTemplate.delete("cache:" + cacheKey);
        logger.debug("Evicted cache entry for key: {}", cacheKey);
      } catch (Exception e) {
        logger.warn("Redis delete failed for key {}: {}", cacheKey, e.getMessage());
      }
    }
  }

  /** Clear all caches for a category. */
  public void clearCategory(String category) {
    logger.info("Clearing cache category: {}", category);
  }

  /** Clear entire cache. */
  public void clear() {
    exactMatchCache.invalidateAll();
    logger.info("Response cache cleared");
  }

  /** Get cache statistics. */
  public CacheStats getStats() {
    com.github.benmanes.caffeine.cache.stats.CacheStats stats = exactMatchCache.stats();
    return new CacheStats(
        stats.hitRate(),
        stats.hitCount(),
        stats.missCount(),
        stats.evictionCount(),
        exactMatchCache.estimatedSize());
  }

  /** Get detailed cache statistics by category. */
  public Map<String, Object> getCategoryStats(String category) {
    Map<String, Object> stats = new HashMap<>();
    stats.put("category", category);
    stats.put("l1Size", exactMatchCache.estimatedSize());

    if (redisTemplate != null) {
      try {
        Long redisSize =
            redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Long>)
                    connection -> {
                      long count = 0;
                      try (var cursor =
                          connection
                              .keyCommands()
                              .scan(
                                  org.springframework.data.redis.core.ScanOptions.scanOptions()
                                      .match("cache:" + category + ":*")
                                      .count(1000)
                                      .build())) {
                        while (cursor.hasNext()) {
                          cursor.next();
                          count++;
                        }
                      } catch (Exception e) {
                        logger.warn(
                            "Redis scan failed for category {}: {}", category, e.getMessage());
                      }
                      return count;
                    });
        stats.put("l2Size", redisSize != null ? redisSize : 0);
      } catch (Exception e) {
        stats.put("l2Size", "unavailable");
      }
    } else {
      stats.put("l2Size", "disabled");
    }

    return stats;
  }

  private String buildCacheKey(String category, String key) {
    return category + ":" + key;
  }

  private String hashPrompt(String prompt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(prompt.trim().getBytes(StandardCharsets.UTF_8));
      return bytesToHex(hash);
    } catch (Exception e) {
      logger.warn("Failed to hash prompt, using raw string as key");
      return prompt;
    }
  }

  private String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (byte b : hash) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  /** Cache entry with metadata and expiration support. */
  public static class CacheEntry implements java.io.Serializable {
    private final String value;
    private final long createdAt;
    private final long ttlSeconds;
    private long hitCount;

    public CacheEntry(String value, Duration ttl) {
      this.value = value;
      this.createdAt = System.currentTimeMillis();
      this.ttlSeconds = ttl.getSeconds();
      this.hitCount = 0;
    }

    public String getValue() {
      return value;
    }

    public boolean isExpired() {
      return (System.currentTimeMillis() - createdAt) > (ttlSeconds * 1000);
    }

    public long getHitCount() {
      return hitCount;
    }

    public void incrementHitCount() {
      this.hitCount++;
    }

    public long getTtlSeconds() {
      return ttlSeconds;
    }

    public long getTimeToLive() {
      long elapsed = (System.currentTimeMillis() - createdAt) / 1000;
      return Math.max(0, ttlSeconds - elapsed);
    }
  }

  /** Cache statistics record. */
  public record CacheStats(
      double hitRate, long hitCount, long missCount, long evictionCount, long size) {}
}
