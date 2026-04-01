package org.example.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Enterprise Failover Manager
 * 
 * Implements multi-layer failover strategy:
 * - Provider failover (switch AI provider if primary fails)
 * - Cache fallback (use stale data if fresh unavailable)
 * - Graceful degradation
 * 
 * @author SupremeAI
 * @version 2.0 Enterprise
 */
@Service
public class FailoverManager {
    private static final Logger logger = LoggerFactory.getLogger(FailoverManager.class);
    
    private static final long CACHE_TIMEOUT_MS = 5000;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;
    
    private final EnterpriseCircuitBreakerManager circuitBreakerManager;
    private final Map<String, List<String>> providerFallbackChain = new ConcurrentHashMap<>();
    private final Map<String, Object> staleDataCache = new ConcurrentHashMap<>();
    
    public FailoverManager(EnterpriseCircuitBreakerManager circuitBreakerManager) {
        this.circuitBreakerManager = circuitBreakerManager;
        setupProviderFallbackChains();
    }
    
    /**
     * Execute with provider failover
     * Tries primary provider, then fallback providers
     */
    public <T> T executeWithProviderFailover(String serviceKey, Supplier<T> primaryProvider, 
                                            String... fallbackProviders) throws Exception {
        List<String> chain = new ArrayList<>();
        chain.add(serviceKey);
        chain.addAll(Arrays.asList(fallbackProviders));
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt < chain.size(); attempt++) {
            String currentProvider = chain.get(attempt);
            
            // Check circuit breaker
            if (!circuitBreakerManager.canProceed(currentProvider)) {
                logger.warn("Circuit breaker OPEN for provider: {}, skipping", currentProvider);
                continue;
            }
            
            try {
                logger.debug("Attempting {} (try {}/{})", currentProvider, attempt + 1, chain.size());
                T result = executeWithRetry(() -> primaryProvider.get(), currentProvider);
                circuitBreakerManager.recordSuccess(currentProvider);
                return result;
                
            } catch (Exception e) {
                lastException = e;
                circuitBreakerManager.recordFailure(currentProvider, e.getMessage());
                logger.warn("Provider {} failed: {}", currentProvider, e.getMessage());
                
                if (attempt < chain.size() - 1) {
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
        }
        
        // All providers failed
        logger.error("All providers failed for service: {}", serviceKey);
        throw lastException != null ? lastException : new Exception("All providers exhausted");
    }
    
    /**
     * Execute with cache fallback
     * Returns fresh data, or falls back to stale cached data
     */
    public <T> T executeWithCacheFallback(String cacheKey, Supplier<T> freshDataProvider, 
                                         long freshDataTimeoutMs) throws Exception {
        try {
            // Try to get fresh data
            T freshData = executeWithTimeout(freshDataProvider, freshDataTimeoutMs);
            
            // Cache the result
            staleDataCache.put(cacheKey, new CachedData<>(freshData, System.currentTimeMillis(), false));
            logger.debug("Cached fresh data: {}", cacheKey);
            
            return freshData;
            
        } catch (TimeoutException e) {
            logger.warn("Fresh data timeout for: {}, falling back to cache", cacheKey);
            
            // Fall back to stale cache
            @SuppressWarnings("unchecked")
            CachedData<T> cached = (CachedData<T>) staleDataCache.get(cacheKey);
            
            if (cached != null && cached.isValid()) {
                logger.info("Serving stale cached data (age: {}ms)", 
                    System.currentTimeMillis() - cached.cachedAt);
                return cached.data;
            }
            
            throw new Exception("No cache available and fresh data timed out");
            
        } catch (Exception e) {
            logger.error("Failed to get fresh data: {}", e.getMessage());
            
            // Fall back to stale cache
            @SuppressWarnings("unchecked")
            CachedData<T> cached = (CachedData<T>) staleDataCache.get(cacheKey);
            
            if (cached != null && cached.isValid()) {
                logger.warn("Serving stale cached data due to error");
                return cached.data;
            }
            
            throw e;
        }
    }
    
    /**
     * Execute with automatic retry and exponential backoff
     */
    private <T> T executeWithRetry(Supplier<T> supplier, String serviceName) throws Exception {
        long delayMs = 100;
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return supplier.get();
            } catch (Exception e) {
                if (attempt == MAX_RETRIES - 1) {
                    throw e;
                }
                
                long jitter = (long) (Math.random() * delayMs / 2);
                long totalBackoff = delayMs + jitter;
                
                logger.debug("Retry {} for {}, backoff: {}ms", attempt + 1, serviceName, totalBackoff);
                Thread.sleep(totalBackoff);
                
                delayMs = Math.min(delayMs * 2, 5000); // Cap at 5 seconds
            }
        }
        
        throw new Exception("Max retry attempts exceeded");
    }
    
    /**
     * Execute with timeout
     */
    private <T> T executeWithTimeout(Supplier<T> supplier, long timeoutMs) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<T> future = executor.submit(supplier::get);
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } finally {
            executor.shutdownNow();
        }
    }
    
    /**
     * Get failover chain for a service
     */
    public List<String> getFailoverChain(String serviceKey) {
        return providerFallbackChain.getOrDefault(serviceKey, Arrays.asList(serviceKey));
    }
    
    /**
     * Register custom failover chain
     */
    public void registerFailoverChain(String serviceKey, List<String> chain) {
        providerFallbackChain.put(serviceKey, new ArrayList<>(chain));
        logger.info("Registered failover chain for {}: {}", serviceKey, chain);
    }
    
    /**
     * Get cached data age in milliseconds
     */
    public long getCacheAge(String cacheKey) {
        @SuppressWarnings("unchecked")
        CachedData<Object> cached = (CachedData<Object>) staleDataCache.get(cacheKey);
        if (cached == null) return -1;
        return System.currentTimeMillis() - cached.cachedAt;
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        staleDataCache.clear();
        logger.info("Cleared all failover cache");
    }
    
    /**
     * Get failover statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cache_entries", staleDataCache.size());
        stats.put("failover_chains", providerFallbackChain.size());
        stats.put("circuit_breakers", circuitBreakerManager.getAllStatuses());
        return stats;
    }
    
    // ============ Private Setup Methods ============
    
    private void setupProviderFallbackChains() {
        // AI Provider fallback chain
        // OpenAI → Anthropic → Google → Meta → Mistral → Cohere → HuggingFace → XAI → DeepSeek → Perplexity
        List<String> aiChain = Arrays.asList(
            "openai", "anthropic", "google", "meta", "mistral", 
            "cohere", "huggingface", "xai", "deepseek", "perplexity"
        );
        providerFallbackChain.put("ai_provider", aiChain);
        
        // Database failover chain
        List<String> dbChain = Arrays.asList(
            "primary_db", "replica_db_1", "replica_db_2"
        );
        providerFallbackChain.put("database", dbChain);
        
        // Cache layer chain
        List<String> cacheChain = Arrays.asList(
            "l1_cache", "l2_cache", "stale_cache"
        );
        providerFallbackChain.put("cache", cacheChain);
    }
    
    /**
     * Internal class for caching data with metadata
     */
    private static class CachedData<T> {
        T data;
        long cachedAt;
        boolean isStale;
        static final long MAX_STALE_AGE_MS = 24 * 60 * 60 * 1000; // 24 hours
        
        CachedData(T data, long cachedAt, boolean isStale) {
            this.data = data;
            this.cachedAt = cachedAt;
            this.isStale = isStale;
        }
        
        boolean isValid() {
            long ageMs = System.currentTimeMillis() - cachedAt;
            return ageMs <= MAX_STALE_AGE_MS;
        }
    }
}
