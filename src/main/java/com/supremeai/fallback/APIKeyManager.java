package com.supremeai.fallback;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class APIKeyManager {

    private final Map<AIProvider, List<String>> providerKeys = new HashMap<>();
    
    // Instead of always starting from key 0 (which causes rate limits on the first key),
    // we track the LAST USED valid key index per provider (Round Robin).
    private final Map<AIProvider, AtomicInteger> currentKeyIndex = new ConcurrentHashMap<>();
    
    // Track keys that are temporarily blocked (e.g., hit 429 Rate Limit) and when they will unblock
    private final Map<String, Long> rateLimitedKeysCooldown = new ConcurrentHashMap<>();

    public APIKeyManager() {
        addKey(AIProvider.GROQ_LLAMA3, "gsk_groq_primary_11111");
        addKey(AIProvider.GROQ_LLAMA3, "gsk_groq_backup_22222");
        addKey(AIProvider.GROQ_LLAMA3, "gsk_groq_backup_33333");
        
        addKey(AIProvider.GEMINI_PRO, "ai39_gemini_key_777");
        
        addKey(AIProvider.HUGGINGFACE_FREE, "hf_free_key_alpha");
        addKey(AIProvider.HUGGINGFACE_FREE, "hf_free_key_beta");
    }

    public void addKey(AIProvider provider, String key) {
        providerKeys.computeIfAbsent(provider, k -> new ArrayList<>()).add(key);
        currentKeyIndex.putIfAbsent(provider, new AtomicInteger(0));
    }

    /**
     * Gets the next healthy key using Round-Robin.
     * Skips keys that are currently in a cooldown state.
     */
    public String getNextHealthyKey(AIProvider provider) {
        List<String> keys = providerKeys.get(provider);
        if (keys == null || keys.isEmpty()) {
            return "default_untracked_key";
        }

        AtomicInteger index = currentKeyIndex.get(provider);
        int totalKeys = keys.size();
        
        // Try to find a healthy key within one full loop
        for (int i = 0; i < totalKeys; i++) {
            int currentIndex = index.getAndUpdate(val -> (val + 1) % totalKeys);
            String candidateKey = keys.get(currentIndex);
            
            // Check if key is in cooldown
            if (!isKeyRateLimited(candidateKey)) {
                return candidateKey; 
            }
        }
        
        return null; // All keys for this provider are currently rate-limited/dead
    }
    
    public void markKeyAsRateLimited(String key, long cooldownMs) {
        // Temporarily block this key from being used for 'cooldownMs' milliseconds
        rateLimitedKeysCooldown.put(key, System.currentTimeMillis() + cooldownMs);
    }
    
    private boolean isKeyRateLimited(String key) {
        Long unlockTime = rateLimitedKeysCooldown.get(key);
        if (unlockTime == null) return false;
        
        if (System.currentTimeMillis() > unlockTime) {
            rateLimitedKeysCooldown.remove(key); // Cooldown over, key is healthy again
            return false;
        }
        return true; // Still in cooldown
    }
}