package com.supremeai.service;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service to manage and clear system-wide caches. Handles both Spring @Cacheable stores and
 * Redis-based persistent caches.
 */
@Service
public class CacheManagementService {
  private static final Logger log = LoggerFactory.getLogger(CacheManagementService.class);

  @Autowired private CacheManager cacheManager;

  @Autowired(required = false)
  private StringRedisTemplate redisTemplate;

  /** Clears all Spring-managed runtime caches (e.g., ai_responses, system_learning). */
  public void clearAllRuntimeCaches() {
    log.info("[CACHE] Initiating global runtime cache clearance...");
    cacheManager
        .getCacheNames()
        .forEach(
            name -> {
              Objects.requireNonNull(cacheManager.getCache(name)).clear();
              log.info("[CACHE] Successfully evicted all entries from: {}", name);
            });
    log.info("[CACHE] Global runtime cache clearance completed.");
  }
}
