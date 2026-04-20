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
@Service
public class ResponseCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ResponseCacheService.class);

    private Cache<String, String> exactMatchCache;

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
        return exactMatchCache.getIfPresent(key);
    }

    /**
     * Store response in cache
     */
    public void put(String prompt, String response) {
        String key = hashPrompt(prompt);
        exactMatchCache.put(key, response);
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
