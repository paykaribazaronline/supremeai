package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

/**
 * Service to manage and clear system-wide caches.
 * Handles both Spring @Cacheable stores and Redis-based persistent caches.
 */
@Service
public class CacheManagementService {
    public CacheManagementService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public CacheManagementService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    private static final Logger log = LoggerFactory.getLogger(CacheManagementService.class);



    /**
     * Clears all Spring-managed runtime caches (e.g., ai_responses, system_learning).
     */
    public void clearAllRuntimeCaches() {
        log.info("[CACHE] Initiating global runtime cache clearance...");
        cacheManager.getCacheNames().forEach(name -> {
            Objects.requireNonNull(cacheManager.getCache(name)).clear();
            log.info("[CACHE] Successfully evicted all entries from: {}", name);
        });
        log.info("[CACHE] Global runtime cache clearance completed.");
    }
}