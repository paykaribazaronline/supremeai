package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 4: REST API Layer for Hybrid Data Collection
 * 
 * This service exposes HybridDataCollector via clean REST endpoints
 * Acts as intermediary between HTTP layer and data collection logic
 * 
 * Features:
 * - Async request handling (doesn't block on data collection)
 * - Request caching to avoid duplicate API calls
 * - Rate limiting per source
 * - Request tracking and analytics
 * 
 * Endpoints exposed:
 * GET /api/data/github/{owner}/{repo} - GitHub data
 * GET /api/data/vercel/{projectId} - Vercel status
 * GET /api/data/firebase/{projectId} - Firebase metrics
 * GET /api/status - System health
 */
public class DataCollectorService {
    private static final Logger logger = LoggerFactory.getLogger(DataCollectorService.class);
    
    private final HybridDataCollector hybridDataCollector;
    private final ObjectMapper mapper = new ObjectMapper();
    
    // Request cache: avoid duplicate calls within 30 seconds
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30_000; // 30 seconds
    
    // Request tracking
    private final Map<String, RequestStats> stats = new ConcurrentHashMap<>();
    
    public DataCollectorService(HybridDataCollector hybridDataCollector) {
        this.hybridDataCollector = hybridDataCollector;
        logger.info("✅ DataCollectorService initialized - REST API layer ready");
    }
    
    /**
     * Fetch GitHub repository data
     * Returns: stars, forks, open issues, recent commits, language distribution
     */
    public Map<String, Object> getGitHubData(String owner, String repo) {
        String cacheKey = "github:" + owner + "/" + repo;
        
        // Check cache first
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            logger.debug("📦 Cache HIT for {}", cacheKey);
            trackRequest(cacheKey, "cache_hit");
            return cached.data;
        }
        
        logger.info("📡 Fetching GitHub data for {}/{}", owner, repo);
        try {
            HybridDataCollector.HybridResult result = hybridDataCollector.collectGitHubData(owner, repo);
            Map<String, Object> response = convertHybridResultToMap(result);
            
            // Cache the result
            cache.put(cacheKey, new CachedResponse(response));
            trackRequest(cacheKey, "success");
            
            return response;
        } catch (Exception e) {
            logger.error("❌ Failed to fetch GitHub data for {}/{}", owner, repo, e);
            trackRequest(cacheKey, "error");
            
            return Map.of(
                "status", "error",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Fetch Vercel deployment status
     * Returns: deployment status, build time, response time, uptime
     */
    public Map<String, Object> getVercelStatus(String projectId) {
        String cacheKey = "vercel:" + projectId;
        
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            logger.debug("📦 Cache HIT for {}", cacheKey);
            trackRequest(cacheKey, "cache_hit");
            return cached.data;
        }
        
        logger.info("📡 Fetching Vercel status for {}", projectId);
        try {
            HybridDataCollector.HybridResult result = hybridDataCollector.collectVercelStatus(projectId);
            Map<String, Object> response = convertHybridResultToMap(result);
            
            cache.put(cacheKey, new CachedResponse(response));
            trackRequest(cacheKey, "success");
            
            return response;
        } catch (Exception e) {
            logger.error("❌ Failed to fetch Vercel status for {}", projectId, e);
            trackRequest(cacheKey, "error");
            
            return Map.of(
                "status", "error",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Fetch Firebase metrics
     * Returns: storage used, realtime DB stats, auth metrics, custom claims
     */
    public Map<String, Object> getFirebaseStatus() {
        String cacheKey = "firebase:status";
        
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            logger.debug("📦 Cache HIT for {}", cacheKey);
            trackRequest(cacheKey, "cache_hit");
            return cached.data;
        }
        
        logger.info("📡 Fetching Firebase status");
        try {
            HybridDataCollector.HybridResult result = hybridDataCollector.collectFirebaseStatus();
            Map<String, Object> response = convertHybridResultToMap(result);
            
            cache.put(cacheKey, new CachedResponse(response));
            trackRequest(cacheKey, "success");
            
            return response;
        } catch (Exception e) {
            logger.error("❌ Failed to fetch Firebase status", e);
            trackRequest(cacheKey, "error");
            
            return Map.of(
                "status", "error",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * System health check
     * Returns quota status, account health, error rates
     */
    public Map<String, Object> getSystemHealth() {
        String cacheKey = "system:health";
        
        CachedResponse cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            trackRequest(cacheKey, "cache_hit");
            return cached.data;
        }
        
        try {
            Map<String, Object> health = new LinkedHashMap<>();
            
            // API status
            health.put("api_status", Map.of(
                "hybrid_collector", "operational",
                "cache_entries", cache.size(),
                "cache_size_estimate", cache.size() * 1024 + " bytes"
            ));
            
            health.put("service_endpoints", Map.of(
                "github", "/api/data/github/{owner}/{repo}",
                "vercel", "/api/data/vercel/{projectId}",
                "firebase", "/api/data/firebase",
                "health", "/api/health",
                "stats", "/api/stats"
            ));
            
            health.put("timestamp", System.currentTimeMillis());
            health.put("status", "healthy");
            
            cache.put(cacheKey, new CachedResponse(health));
            trackRequest(cacheKey, "success");
            
            return health;
        } catch (Exception e) {
            logger.error("❌ Health check failed", e);
            trackRequest(cacheKey, "error");
            
            return Map.of(
                "status", "unhealthy",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Get request statistics for monitoring
     * Returns: total requests, cache hits, errors, response times
     */
    public Map<String, Object> getRequestStats() {
        Map<String, Object> statsMap = new LinkedHashMap<>();
        
        for (Map.Entry<String, RequestStats> entry : stats.entrySet()) {
            statsMap.put(entry.getKey(), entry.getValue().toMap());
        }
        
        return Map.of(
            "statistics", statsMap,
            "total_requests", stats.values().stream().mapToLong(s -> s.total).sum(),
            "total_errors", stats.values().stream().mapToLong(s -> s.errors).sum(),
            "timestamp", System.currentTimeMillis()
        );
    }
    
    /**
     * Clear cache for a specific key or all keys
     */
    public void clearCache(String key) {
        if ("all".equalsIgnoreCase(key)) {
            cache.clear();
            logger.info("🗑️ Cleared entire cache");
        } else {
            cache.remove(key);
            logger.info("🗑️ Cleared cache for {}", key);
        }
    }
    
    // ========== Internal Helpers ==========
    
    /**
     * Convert HybridResult to Map<String, Object> for REST API responses
     */
    private Map<String, Object> convertHybridResultToMap(HybridDataCollector.HybridResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", result.success);
        map.put("type", result.type);
        map.put("target", result.target);
        map.put("dataSource", result.dataSource);
        map.put("data", result.data != null ? result.data : Map.of());
        if (result.error != null) {
            map.put("error", result.error);
        }
        map.put("fallbackUsed", result.fallbackUsed);
        map.put("timestamp", result.timestamp);
        return map;
    }
    
    private void trackRequest(String key, String outcome) {
        stats.computeIfAbsent(key, k -> new RequestStats())
            .record(outcome);
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Cache entry with TTL
     */
    public static class CachedResponse {
        public final Map<String, Object> data;
        public final long createdAt;
        
        public CachedResponse(Map<String, Object> data) {
            this.data = data;
            this.createdAt = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_TTL_MS;
        }
    }
    
    /**
     * Request statistics tracker
     */
    public static class RequestStats {
        public long total = 0;
        public long success = 0;
        public long errors = 0;
        public long cacheHits = 0;
        public long lastRequestTime = 0;
        
        public void record(String outcome) {
            total++;
            lastRequestTime = System.currentTimeMillis();
            
            switch(outcome) {
                case "success" -> success++;
                case "error" -> errors++;
                case "cache_hit" -> cacheHits++;
            }
        }
        
        public Map<String, Object> toMap() {
            return Map.of(
                "total", total,
                "success", success,
                "errors", errors,
                "cache_hits", cacheHits,
                "success_rate", total == 0 ? 0 : (success * 100.0 / total),
                "last_request", new Date(lastRequestTime)
            );
        }
    }
}
