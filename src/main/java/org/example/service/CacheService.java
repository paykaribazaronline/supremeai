package org.example.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Phase 4: Caching Service
 * In-memory cache for frequently accessed data
 * Reduces database queries and improves response times
 */
@Service
public class CacheService {

    private static class CacheEntry<T> {
        T value;
        long ttl; // Time to live in ms
        long createdAt;

        CacheEntry(T value, long ttlMs) {
            this.value = value;
            this.ttl = ttlMs;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > ttl;
        }
    }

    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> accessLog = new ConcurrentLinkedQueue<>();
    
    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Store value in cache with default TTL
     */
    public <T> void put(String key, T value) {
        put(key, value, DEFAULT_TTL_MS);
    }

    /**
     * Store value in cache with custom TTL
     */
    public <T> void put(String key, T value, long ttlMs) {
        cache.put(key, new CacheEntry<>(value, ttlMs));
        accessLog.offer(key);
    }

    /**
     * Get value from cache
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        CacheEntry<?> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            accessLog.offer(key); // Log access for LRU
            return Optional.of((T) entry.value);
        }
        if (entry != null && entry.isExpired()) {
            cache.remove(key);
        }
        return Optional.empty();
    }

    /**
     * Remove entry from cache
     */
    public void remove(String key) {
        cache.remove(key);
    }

    /**
     * Clear entire cache
     */
    public void clear() {
        cache.clear();
        accessLog.clear();
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cache_size", cache.size());
        stats.put("access_log_size", accessLog.size());
        
        long expiredCount = cache.values().stream()
            .filter(CacheEntry::isExpired)
            .count();
        stats.put("expired_entries", expiredCount);
        
        Map<String, Long> keyStats = new HashMap<>();
        cache.forEach((key, entry) -> {
            keyStats.put(key, entry.ttl);
        });
        stats.put("keys", keyStats);
        
        return stats;
    }

    /**
     * Invalidate cache entries matching pattern
     */
    public void invalidatePattern(String pattern) {
        cache.keySet().stream()
            .filter(key -> key.matches(pattern))
            .forEach(cache::remove);
    }

    /**
     * Get all cache keys
     */
    public Set<String> getKeys() {
        return new HashSet<>(cache.keySet());
    }

    /**
     * Check if key exists and is not expired
     */
    public boolean exists(String key) {
        return cache.containsKey(key) && !cache.get(key).isExpired();
    }
}
