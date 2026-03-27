package org.example.selfhealing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache Recovery Manager
 * 
 * Handles self-healing for cache layer:
 * 1. Detect stale cache entries (TTL exceeded)
 * 2. Detect corrupted data (validation)
 * 3. Automatic cache invalidation and refresh
 * 4. Cache statistics and metrics
 */
public class CacheRecoveryManager {
    private static final Logger logger = LoggerFactory.getLogger(CacheRecoveryManager.class);
    
    private final String cacheName;
    private final Map<String, CacheEntry<?>> cache;
    private final long cacheTTLMs;
    
    public CacheRecoveryManager(String cacheName, long cacheTTLMs) {
        this.cacheName = cacheName;
        this.cacheTTLMs = cacheTTLMs;
        this.cache = new ConcurrentHashMap<>();
        logger.debug("Created cache recovery manager: {}", cacheName);
    }
    
    /**
     * Put item in cache
     */
    public <T> void put(String key, T value) {
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis(), validateData(value)));
    }
    
    /**
     * Get item from cache (with stale check)
     */
    public <T> T get(String key) {
        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (isStale(entry)) {
            logger.debug("{}Removing stale cache entry: {}", SelfHealingConfig.RECOVERY_PREFIX, key);
            cache.remove(key);
            return null;
        }
        
        if (!entry.isValid) {
            logger.warn("{}Removing corrupted cache entry: {}", SelfHealingConfig.WARNING_PREFIX, key);
            cache.remove(key);
            return null;
        }
        
        @SuppressWarnings("unchecked")
        T result = (T) entry.value;
        return result;
    }
    
    /**
     * Check if cache entry is stale
     */
    private boolean isStale(CacheEntry<?> entry) {
        long ageMs = System.currentTimeMillis() - entry.createdAt;
        return ageMs > cacheTTLMs + SelfHealingConfig.CACHE_STALE_THRESHOLD_MS;
    }
    
    /**
     * Validate cache data integrity
     */
    private boolean validateData(Object value) {
        if (value == null) {
            return false;
        }
        
        try {
            // Check if it's a Map (typical cache data)
            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                // Validate required fields based on data type
                if (map.containsKey("owner") && map.containsKey("repo")) {
                    // GitHub data validation
                    return validateGitHubData(map);
                } else if (map.containsKey("status")) {
                    // Status data validation
                    return validateStatusData(map);
                }
                return true; // Generic map is valid
            }
            return true;
        } catch (Exception e) {
            logger.warn("Cache validation error: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate GitHub data structure
     */
    private boolean validateGitHubData(Map<?, ?> map) {
        return map.get("owner") instanceof String &&
               map.get("repo") instanceof String &&
               (map.get("stars") == null || map.get("stars") instanceof Number);
    }
    
    /**
     * Validate status data structure
     */
    private boolean validateStatusData(Map<?, ?> map) {
        return map.containsKey("status") &&
               map.get("status") instanceof String &&
               (map.get("timestamp") == null || map.get("timestamp") instanceof Number);
    }
    
    /**
     * Perform cache cleanup (remove stale/invalid entries)
     */
    public CacheHealth performCleanup() {
        int staleCount = 0;
        int invalidCount = 0;
        int removedCount = 0;
        
        List<String> keysToRemove = new ArrayList<>();
        
        for (Map.Entry<String, CacheEntry<?>> entry : cache.entrySet()) {
            if (isStale(entry.getValue())) {
                staleCount++;
                keysToRemove.add(entry.getKey());
            } else if (!entry.getValue().isValid) {
                invalidCount++;
                keysToRemove.add(entry.getKey());
            }
        }
        
        for (String key : keysToRemove) {
            cache.remove(key);
            removedCount++;
        }
        
        logger.info("{}Cache cleanup for {}: removed {} entries (stale: {}, invalid: {})",
            SelfHealingConfig.RECOVERY_PREFIX, cacheName, removedCount, staleCount, invalidCount);
        
        return new CacheHealth(
            cache.size(),
            staleCount,
            invalidCount,
            removedCount,
            getCorrompionRatio()
        );
    }
    
    /**
     * Calculate cache corruption ratio
     */
    private double getCorrompionRatio() {
        if (cache.isEmpty()) return 0;
        
        int invalidCount = (int) cache.values().stream()
            .filter(entry -> !entry.isValid || isStale(entry))
            .count();
        
        return (double) invalidCount / cache.size();
    }
    
    /**
     * Clear all cache
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        logger.info("Cleared cache {}: {} entries removed", cacheName, size);
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        int total = cache.size();
        int valid = (int) cache.values().stream()
            .filter(entry -> entry.isValid && !isStale(entry))
            .count();
        int stale = total - valid;
        
        return new CacheStats(cacheName, total, valid, stale, getCorrompionRatio());
    }
    
    // ===== Inner Classes =====
    
    /**
     * Cached entry with metadata
     */
    private static class CacheEntry<T> {
        final T value;
        final long createdAt;
        final boolean isValid;
        
        CacheEntry(T value, long createdAt, boolean isValid) {
            this.value = value;
            this.createdAt = createdAt;
            this.isValid = isValid;
        }
    }
    
    /**
     * Cache health metrics
     */
    public static class CacheHealth {
        public final int totalEntries;
        public final int staleEntries;
        public final int invalidEntries;
        public final int removedEntries;
        public final double corruptionRatio;
        
        public CacheHealth(int totalEntries, int staleEntries, int invalidEntries,
                          int removedEntries, double corruptionRatio) {
            this.totalEntries = totalEntries;
            this.staleEntries = staleEntries;
            this.invalidEntries = invalidEntries;
            this.removedEntries = removedEntries;
            this.corruptionRatio = corruptionRatio;
        }
        
        @Override
        public String toString() {
            return String.format("CacheHealth{entries=%d, stale=%d, invalid=%d, removed=%d, corruption=%.1f%%}",
                totalEntries, staleEntries, invalidEntries, removedEntries, corruptionRatio * 100);
        }
    }
    
    /**
     * Cache statistics
     */
    public static class CacheStats {
        public final String cacheName;
        public final int totalEntries;
        public final int validEntries;
        public final int staleEntries;
        public final double corruptionRatio;
        
        public CacheStats(String cacheName, int totalEntries, int validEntries,
                         int staleEntries, double corruptionRatio) {
            this.cacheName = cacheName;
            this.totalEntries = totalEntries;
            this.validEntries = validEntries;
            this.staleEntries = staleEntries;
            this.corruptionRatio = corruptionRatio;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{cache='%s', total=%d, valid=%d, stale=%d, corruption=%.1f%%}",
                cacheName, totalEntries, validEntries, staleEntries, corruptionRatio * 100);
        }
    }
}
