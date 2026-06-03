package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Cache warming service for frequently accessed prompts and patterns.
 * Pre-loads cache on startup to ensure fast response times.
 */
@Service
public class CacheWarmingService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmingService.class);

    private final CacheManager cacheManager;
    private final Executor asyncTaskExecutor;

    // Frequently accessed prompt patterns
    private static final List<String> COMMON_PROMPTS = List.of(
        "Explain quantum computing",
        "Write a Python function to sort an array",
        "How to optimize database queries",
        "Best practices for REST API design",
        "Explain machine learning concepts"
    );

    public CacheWarmingService(CacheManager cacheManager, Executor asyncTaskExecutor) {
        this.cacheManager = cacheManager;
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting cache warming for frequently accessed prompts...");
        
        CompletableFuture.runAsync(() -> {
            warmPromptCache();
            warmPatternCache();
            log.info("Cache warming completed successfully");
        }, asyncTaskExecutor);
    }

    /**
     * Warm the prompt cache with common queries
     */
    private void warmPromptCache() {
        Cache promptsCache = cacheManager.getCache("prompts");
        if (promptsCache == null) {
            log.warn("Prompts cache not found, skipping warming");
            return;
        }

        for (String prompt : COMMON_PROMPTS) {
            String cacheKey = "prompt:" + prompt.hashCode();
            // Pre-populate with placeholder - actual values will be cached on first use
            promptsCache.put(cacheKey, createPlaceholderResponse(prompt));
        }
        log.info("Warmed {} common prompts in cache", COMMON_PROMPTS.size());
    }

    /**
     * Warm the pattern cache with common patterns
     */
    private void warmPatternCache() {
        Cache patternsCache = cacheManager.getCache("patterns");
        if (patternsCache == null) {
            log.warn("Patterns cache not found, skipping warming");
            return;
        }

        // Common coding patterns
        patternsCache.put("pattern:sort", "Sorting algorithms: quicksort, mergesort, heapsort");
        patternsCache.put("pattern:search", "Search algorithms: binary search, linear search");
        patternsCache.put("pattern:cache", "Cache patterns: LRU, LFU, TTL-based");
        
        log.info("Warmed common patterns in cache");
    }

    private String createPlaceholderResponse(String prompt) {
        return "Cached response for: " + prompt;
    }
}