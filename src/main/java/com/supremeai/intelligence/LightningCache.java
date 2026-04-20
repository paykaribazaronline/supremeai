package com.supremeai.intelligence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LightningCache {
    
    // RAM-based L1 Cache. Lookups take literally 1 millisecond.
    private final Map<String, String> l1MemoryCache = new ConcurrentHashMap<>();

    /**
     * Hash the code to create a unique fingerprint.
     * If the exact same code file is uploaded, we don't analyze it again.
     */
    public String getCachedAnalysis(String codeHash) {
        return l1MemoryCache.get(codeHash);
    }

    public void cacheResult(String codeHash, String result) {
        // Keep cache size manageable (e.g., evict old entries if > 10,000)
        if (l1MemoryCache.size() > 10000) {
            l1MemoryCache.clear(); // Simplified eviction. Real world uses LRU.
        }
        l1MemoryCache.put(codeHash, result);
    }
}