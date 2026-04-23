package com.supremeai.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * Multi-level response cache service
 * Level 1: In-memory exact match cache (10min TTL)
 * Level 2: Permanent cache for high frequency queries
 */
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.concurrent.TimeUnit;

@Service
public class ResponseCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ResponseCacheService.class);

    private Cache<String, String> exactMatchCache;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void init() {
        exactMatchCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats()
                .build();

        logger.info("ResponseCacheService initialized with 10,000 entry capacity");
    }

    /**
     * Get cached response for prompt
     */
    public String get(String prompt) {
        String key = hashPrompt(prompt);
        
        // Try L1 Cache (Caffeine)
        String l1Result = exactMatchCache.getIfPresent(key);
        if (l1Result != null) {
            return l1Result;
        }
        
        // Try L2 Cache (Redis)
        try {
            Object l2Result = redisTemplate.opsForValue().get("ai_resp:" + key);
            if (l2Result instanceof String) {
                // Backfill L1 Cache
                exactMatchCache.put(key, (String) l2Result);
                return (String) l2Result;
            }
        } catch (Exception e) {
            logger.warn("Redis read failed: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Store response in cache
     */
    public void put(String prompt, String response) {
        String key = hashPrompt(prompt);
        
        // Put in L1 Cache
        exactMatchCache.put(key, response);
        
        // Put in L2 Cache (Redis)
        try {
            redisTemplate.opsForValue().set("ai_resp:" + key, response, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.warn("Redis write failed: {}", e.getMessage());
        }
    }

    /**
     * Clear entire cache
     */
    public void clear() {
        exactMatchCache.invalidateAll();
        logger.info("Response cache cleared");
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = exactMatchCache.stats();
        return new CacheStats(
                stats.hitRate(),
                stats.hitCount(),
                stats.missCount(),
                exactMatchCache.estimatedSize()
        );
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

    public record CacheStats(
            double hitRate,
            long hitCount,
            long missCount,
            long size
    ) {}
}
