package com.supremeai.fallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class APIKeyManager {

    private static final Logger log = LoggerFactory.getLogger(APIKeyManager.class);
    private final Map<AIProvider, List<String>> providerKeys = new HashMap<>();

    // Instead of always starting from key 0 (which causes rate limits on the first key),
    // we track the LAST USED valid key index per provider (Round Robin).
    private final Map<AIProvider, AtomicInteger> currentKeyIndex = new ConcurrentHashMap<>();

    // Track keys that are temporarily blocked (e.g., hit 429 Rate Limit) and when they will unblock
    private final Map<String, Long> rateLimitedKeysCooldown = new ConcurrentHashMap<>();

    @Autowired
    private Environment env;

    /**
     * Initializes the APIKeyManager.
     * Keys are loaded from environment variables or configuration.
     * Supports multiple keys per provider via numbered suffixes: PROVIDER_KEY, PROVIDER_KEY_1, PROVIDER_KEY_2...
     */
    @PostConstruct
    public void loadKeysFromConfig() {
        // Load keys for various providers from environment
        // Pattern: ENV_VAR for base provider, and numbered variants

        // Groq keys: GROQ_API_KEY, GROQ_API_KEY_1, GROQ_API_KEY_2, ...
        loadKeysFromEnv("GROQ_API_KEY", AIProvider.GROQ_LLAMA3);

        // Gemini keys: GEMINI_API_KEY, GEMINI_API_KEY_1, GEMINI_API_KEY_2, ...
        loadKeysFromEnv("GEMINI_API_KEY", AIProvider.GEMINI_PRO);

        // DeepSeek keys: DEEPSEEK_API_KEY, DEEPSEEK_API_KEY_1, ...
        loadKeysFromEnv("DEEPSEEK_API_KEY", AIProvider.GROQ_LLAMA3); // fallback to groq slot

        // Anthropic/Claude keys
        String claudeKey = env.getProperty("ANTHROPIC_API_KEY");
        if (claudeKey != null && !claudeKey.trim().isEmpty()) {
            addKey(AIProvider.ANTHROPIC_CLAUDE, claudeKey.trim());
        }

        // HuggingFace keys (if any)
        String hfKey = env.getProperty("HUGGINGFACE_API_KEY");
        if (hfKey != null && !hfKey.trim().isEmpty()) {
            addKey(AIProvider.HUGGINGFACE_FREE, hfKey.trim());
        }

        log.info("API keys loaded from environment: {} providers configured with {} total keys",
                providerKeys.size(),
                providerKeys.values().stream().mapToInt(List::size).sum());
    }

    private void loadKeysFromEnv(String baseEnvVar, AIProvider provider) {
        // Add base key
        String baseKey = env.getProperty(baseEnvVar);
        if (baseKey != null && !baseKey.trim().isEmpty()) {
            addKey(provider, baseKey.trim());
        }

        // Add numbered variants: _1, _2, _3, _4, _5 (support up to 5 keys)
        for (int i = 1; i <= 5; i++) {
            String numberedKey = env.getProperty(baseEnvVar + "_" + i);
            if (numberedKey != null && !numberedKey.trim().isEmpty()) {
                addKey(provider, numberedKey.trim());
            }
        }
    }

    @PostConstruct
    public void loadKeysFromConfig() {
        // Load configured keys into the manager
        if (groqApiKey != null && !groqApiKey.trim().isEmpty()) {
            addKey(AIProvider.GROQ_LLAMA3, groqApiKey.trim());
        }
        if (openaiApiKey != null && !openaiApiKey.trim().isEmpty()) {
            // For simplicity, map OpenAI to GROQ_LLAMA3 slot or add separate provider if needed
            addKey(AIProvider.GROQ_LLAMA3, openaiApiKey.trim());
        }
        if (anthropicApiKey != null && !anthropicApiKey.trim().isEmpty()) {
            // Map Anthropic to ANTHROPIC_CLAUDE
            addKey(AIProvider.ANTHROPIC_CLAUDE, anthropicApiKey.trim());
        }
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty()) {
            addKey(AIProvider.GEMINI_PRO, geminiApiKey.trim());
        }
        if (deepseekApiKey != null && !deepseekApiKey.trim().isEmpty()) {
            // DeepSeek not in enum; can be added as new provider or used as fallback
            // For now, add to GROQ as fallback
            addKey(AIProvider.GROQ_LLAMA3, deepseekApiKey.trim());
        }
        if (kimiApiKey != null && !kimiApiKey.trim().isEmpty()) {
            // Kimi not in enum; could extend later
            log.warn("Kimi API key configured but provider not yet supported in fallback chain");
        }

        log.info("API keys loaded: {} providers configured", providerKeys.size());
    }

    public void addKey(AIProvider provider, String key) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("Attempted to add null/empty key for provider: {}", provider);
            return;
        }
        providerKeys.computeIfAbsent(provider, k -> new ArrayList<>()).add(key);
        currentKeyIndex.putIfAbsent(provider, new AtomicInteger(0));
        log.debug("Added API key for provider: {} (total keys: {})", provider, providerKeys.get(provider).size());
    }

    /**
     * Gets the next healthy key using Round-Robin.
     * Skips keys that are currently in a cooldown state.
     */
    public String getNextHealthyKey(AIProvider provider) {
        List<String> keys = providerKeys.get(provider);
        if (keys == null || keys.isEmpty()) {
            log.warn("No API keys configured for provider: {}", provider);
            return null;
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

        log.warn("All keys for provider {} are currently rate-limited", provider);
        return null; // All keys for this provider are currently rate-limited/dead
    }
    
    public void markKeyAsRateLimited(String key, long cooldownMs) {
        // Temporarily block this key from being used for 'cooldownMs' milliseconds
        rateLimitedKeysCooldown.put(key, System.currentTimeMillis() + cooldownMs);
        log.debug("Key marked as rate-limited for {}ms: {}", cooldownMs, maskKey(key));
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

    /**
     * Helper to mask API keys in logs (shows first 4 and last 4 chars only)
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) return "****";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}