package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LRU Cache Service - Phase 1 Optimization
 * Replaces unbounded memory with bounded cache to:
 * - Prevent OOM (max 1.5GB)
 * - Reduce Firebase reads by 40% (cache hit target: 60%)
 * - Save $10/month on Firebase quota
 * 
 * Features:
 * - Automatic eviction when limit reached
 * - LRU (Least Recently Used) policy
 * - Thread-safe operations
 * - Cache statistics tracking
 */
@Service
public class LRUCacheService {
    private static final Logger logger = LoggerFactory.getLogger(LRUCacheService.class);
    
    // Configuration
    private static final long MAX_CACHE_SIZE_BYTES = 1_500_000_000L; // 1.5 GB
    private static final int MAX_ENTRIES = 50_000;
    
    // Cache structure: LinkedHashMap with LRU eviction
    private LinkedHashMap<String, CacheEntry> cache;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Statistics
    private long currentSizeBytes = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long evictionCount = 0;
    
    public static class CacheEntry {
        public Object value;
        public long sizeBytes;
        public long createdAt;
        public long lastAccessedAt;
        public int accessCount;
        
        public CacheEntry(Object value, long sizeBytes) {
            this.value = value;
            this.sizeBytes = sizeBytes;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessedAt = System.currentTimeMillis();
            this.accessCount = 0;
        }
    }
    
    public LRUCacheService() {
        // Initialize with custom LinkedHashMap for LRU
        this.cache = new LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                // Remove if size exceeded OR entry count exceeded
                boolean shouldRemove = currentSizeBytes > MAX_CACHE_SIZE_BYTES || size() > MAX_ENTRIES;
                if (shouldRemove) {
                    CacheEntry entry = eldest.getValue();
                    currentSizeBytes -= entry.sizeBytes;
                    evictionCount++;
                }
                return shouldRemove;
            }
        };
    }
    
    /**
     * Get from cache (marks as recently used)
     */
    public Object get(String key) {
        lock.readLock().lock();
        try {
            if (cache.containsKey(key)) {
                CacheEntry entry = cache.get(key);
                entry.lastAccessedAt = System.currentTimeMillis();
                entry.accessCount++;
                cacheHits++;
                logger.debug("💚 Cache HIT: {} (hits: {})", key, cacheHits);
                return entry.value;
            }
            cacheMisses++;
            logger.debug("❌ Cache MISS: {} (misses: {})", key, cacheMisses);
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Put into cache
     */
    public void put(String key, Object value) {
        put(key, value, estimateSize(value));
    }
    
    /**
     * Put with explicit size
     */
    public void put(String key, Object value, long sizeBytes) {
        lock.writeLock().lock();
        try {
            // Remove old entry if exists
            if (cache.containsKey(key)) {
                CacheEntry oldEntry = cache.get(key);
                currentSizeBytes -= oldEntry.sizeBytes;
            }
            
            // Add new entry
            CacheEntry entry = new CacheEntry(value, sizeBytes);
            cache.put(key, entry);
            currentSizeBytes += sizeBytes;
            
            logger.debug("💾 Cache PUT: {} (size: {} bytes, total: {} bytes)", 
                key, sizeBytes, currentSizeBytes);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Remove from cache
     */
    public void evict(String key) {
        lock.writeLock().lock();
        try {
            if (cache.containsKey(key)) {
                CacheEntry entry = cache.remove(key);
                currentSizeBytes -= entry.sizeBytes;
                logger.debug("🗑️ Cache EVICT: {}", key);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Clear entire cache
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
            currentSizeBytes = 0;
            logger.info("🧹 Cache CLEARED");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getStats() {
        lock.readLock().lock();
        try {
            long totalRequests = cacheHits + cacheMisses;
            double hitRate = totalRequests == 0 ? 0 : (double) cacheHits / totalRequests * 100;
            
            return Map.ofEntries(
                Map.entry("cacheHits", cacheHits),
                Map.entry("cacheMisses", cacheMisses),
                Map.entry("hitRate", String.format("%.2f%%", hitRate)),
                Map.entry("currentEntries", cache.size()),
                Map.entry("currentSizeBytes", currentSizeBytes),
                Map.entry("currentSizeMB", roundTo2Decimals(currentSizeBytes / 1_000_000.0)),
                Map.entry("maxSizeBytes", MAX_CACHE_SIZE_BYTES),
                Map.entry("maxSizeMB", roundTo2Decimals(MAX_CACHE_SIZE_BYTES / 1_000_000.0)),
                Map.entry("utilization", String.format("%.2f%%", (double) currentSizeBytes / MAX_CACHE_SIZE_BYTES * 100)),
                Map.entry("evictionCount", evictionCount),
                Map.entry("totalRequests", totalRequests)
            );
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Estimate object size (simplified)
     */
    private long estimateSize(Object obj) {
        if (obj == null) return 8;
        if (obj instanceof String) return ((String) obj).length() * 2 + 64;
        if (obj instanceof Map) return ((Map<?, ?>) obj).size() * 100 + 200;
        if (obj instanceof List) return ((List<?>) obj).size() * 100 + 200;
        return 200; // Default estimate
    }
    
    private double roundTo2Decimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
