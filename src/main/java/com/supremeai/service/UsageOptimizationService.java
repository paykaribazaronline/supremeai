package com.supremeai.service;

import com.supremeai.model.UserApiKey;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.security.ApiKeyRotationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for optimizing AI API usage across providers.
 *
 * Features:
 * - Response caching to avoid duplicate API calls for identical prompts
 * - Smart model selection based on task complexity and cost
 * - Per-user usage tracking and cost attribution
 * - Key rotation awareness (prefers keys with lower usage)
 * - Provider health tracking (avoids recently failed providers)
 */
@Service
public class UsageOptimizationService {

    private static final Logger log = LoggerFactory.getLogger(UsageOptimizationService.class);

    @Autowired
    private UserApiKeyRepository userApiKeyRepository;

    @Autowired
    private ApiKeyRotationService keyRotationService;

    /**
     * In-memory response cache. Key = hash of (userId + provider + model + prompt).
     * This avoids duplicate API calls for the same prompt within the cache TTL.
     */
    private final ConcurrentHashMap<String, CachedResponse> responseCache = new ConcurrentHashMap<>();

    private static final int CACHE_TTL_MINUTES = 30;
    private static final int MAX_CACHE_SIZE = 1000;

    /**
     * Model tiers for cost-optimized selection.
     * Simple tasks use cheaper models; complex tasks use more capable ones.
     */
    private static final Map<String, ModelTier> MODEL_TIERS = Map.ofEntries(
            // Budget tier - for simple tasks
            Map.entry("gpt-4.1-nano", new ModelTier("budget", 0.0001)),
            Map.entry("gpt-4.1-mini", new ModelTier("economy", 0.0003)),
            Map.entry("llama-3.1-8b-instant", new ModelTier("budget", 0.00005)),
            Map.entry("gemini-2.0-flash-lite", new ModelTier("budget", 0.000075)),
            Map.entry("gemini-2.0-flash", new ModelTier("economy", 0.0001)),
            Map.entry("claude-3-5-haiku-20241022", new ModelTier("economy", 0.0008)),
            Map.entry("deepseek-chat", new ModelTier("economy", 0.00014)),
            Map.entry("mistral-small-latest", new ModelTier("economy", 0.0002)),
            // Standard tier - for moderate tasks
            Map.entry("gpt-4o", new ModelTier("standard", 0.0025)),
            Map.entry("gpt-4.1", new ModelTier("standard", 0.002)),
            Map.entry("gemini-2.5-flash-preview-04-17", new ModelTier("standard", 0.00015)),
            Map.entry("llama-3.3-70b-versatile", new ModelTier("standard", 0.0006)),
            Map.entry("deepseek-reasoner", new ModelTier("standard", 0.00055)),
            // Premium tier - for complex tasks
            Map.entry("gemini-2.5-pro-preview-03-25", new ModelTier("premium", 0.00125)),
            Map.entry("claude-sonnet-4-20250514", new ModelTier("premium", 0.003)),
            Map.entry("claude-3-7-sonnet-20250219", new ModelTier("premium", 0.003)),
            Map.entry("o4-mini", new ModelTier("premium", 0.0015)),
            Map.entry("mistral-large-latest", new ModelTier("premium", 0.002)),
            Map.entry("grok-3", new ModelTier("premium", 0.003)),
            // Top tier - for critical/reasoning tasks
            Map.entry("o3", new ModelTier("top", 0.015)),
            Map.entry("grok-3-mini", new ModelTier("premium", 0.001))
    );

    private static final String DEFAULT_TIER = "standard";

    /**
     * Check the response cache for a previous result.
     * Returns null if not cached or cache expired.
     */
    public String getCachedResponse(String userId, String provider, String model, String prompt) {
        String cacheKey = buildCacheKey(userId, provider, model, prompt);
        CachedResponse cached = responseCache.get(cacheKey);

        if (cached == null) return null;

        if (cached.isExpired()) {
            responseCache.remove(cacheKey);
            return null;
        }

        log.debug("Cache hit for key: {}", cacheKey.substring(0, 16) + "...");
        return cached.response;
    }

    /**
     * Store a response in the cache.
     */
    public void cacheResponse(String userId, String provider, String model, String prompt, String response) {
        // Evict old entries if cache is too large
        if (responseCache.size() >= MAX_CACHE_SIZE) {
            evictExpiredEntries();
            if (responseCache.size() >= MAX_CACHE_SIZE) {
                // Remove oldest entries
                responseCache.entrySet().stream()
                        .sorted(Comparator.comparingLong(e -> e.getValue().cachedAt))
                        .limit(100)
                        .forEach(e -> responseCache.remove(e.getKey()));
            }
        }

        String cacheKey = buildCacheKey(userId, provider, model, prompt);
        responseCache.put(cacheKey, new CachedResponse(response));
    }

    /**
     * Select the best model for a given task complexity.
     *
     * @param userId     The user making the request
     * @param complexity Task complexity: "simple", "moderate", "complex", "critical"
     * @return Recommended model name and the API key to use
     */
    public Optional<SelectedModel> selectModelForTask(String userId, String complexity) {
        String targetTier = mapComplexityToTier(complexity);

        // Find all active keys for this user
        List<UserApiKey> keys = userApiKeyRepository
                .findByUserIdAndStatus(userId, "active")
                .collectList().block();

        if (keys == null || keys.isEmpty()) return Optional.empty();

        // Group keys by provider
        Map<String, List<UserApiKey>> keysByProvider = new LinkedHashMap<>();
        for (UserApiKey key : keys) {
            keysByProvider.computeIfAbsent(key.getProvider().toLowerCase(), k -> new ArrayList<>()).add(key);
        }

        // Find the cheapest model in the target tier that the user has a key for
        List<Map.Entry<String, ModelTier>> candidates = new ArrayList<>();
        for (Map.Entry<String, ModelTier> entry : MODEL_TIERS.entrySet()) {
            if (entry.getValue().tier.equals(targetTier) || isHigherTier(entry.getValue().tier, targetTier)) {
                // Check if user has a key for this model's provider
                String modelProvider = getProviderForModel(entry.getKey());
                if (modelProvider != null && keysByProvider.containsKey(modelProvider)) {
                    candidates.add(entry);
                }
            }
        }

        if (candidates.isEmpty()) {
            // Fallback: use any available key with its default model
            UserApiKey anyKey = keys.get(0);
            return Optional.of(new SelectedModel(
                    getDefaultModelForProvider(anyKey.getProvider()),
                    anyKey.getApiKey(),
                    anyKey.getProvider(),
                    anyKey.getBaseUrl()
            ));
        }

        // Sort by cost (cheapest first within the target tier)
        candidates.sort(Comparator.comparingDouble(e -> e.getValue().costPerRequest));

        Map.Entry<String, ModelTier> selected = candidates.get(0);
        String modelProvider = getProviderForModel(selected.getKey());

        // Pick the least-used key for this provider
        UserApiKey bestKey = keysByProvider.getOrDefault(modelProvider, keys).stream()
                .min(Comparator.comparingLong(k -> k.getRequestCount() != null ? k.getRequestCount() : 0L))
                .orElse(keys.get(0));

        return Optional.of(new SelectedModel(
                selected.getKey(),
                bestKey.getApiKey(),
                bestKey.getProvider(),
                bestKey.getBaseUrl()
        ));
    }

    /**
     * Record usage of an API key (for cost tracking and rotation awareness).
     */
    public void recordKeyUsage(String keyId, double estimatedCost) {
        UserApiKey key = userApiKeyRepository.findById(keyId).block();
        if (key != null) {
            key.recordUsage(estimatedCost);
            userApiKeyRepository.save(key).block();
        }
    }

    /**
     * Get usage summary for a user across all providers.
     */
    public Map<String, Object> getUserUsageSummary(String userId) {
        List<UserApiKey> keys = userApiKeyRepository.findByUserId(userId).collectList().block();
        if (keys == null) keys = Collections.emptyList();

        long totalRequests = keys.stream()
                .mapToLong(k -> k.getRequestCount() != null ? k.getRequestCount() : 0L)
                .sum();

        double totalCost = keys.stream()
                .mapToDouble(k -> k.getEstimatedCost() != null ? k.getEstimatedCost() : 0.0)
                .sum();

        long keysNeedingRotation = keys.stream()
                .filter(UserApiKey::needsRotation)
                .count();

        int cacheSize = responseCache.size();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRequests", totalRequests);
        summary.put("totalCost", Math.round(totalCost * 100.0) / 100.0);
        summary.put("totalKeys", keys.size());
        summary.put("activeKeys", keys.stream().filter(k -> "active".equals(k.getStatus())).count());
        summary.put("keysNeedingRotation", keysNeedingRotation);
        summary.put("cacheSize", cacheSize);
        summary.put("cacheTTLMinutes", CACHE_TTL_MINUTES);

        return summary;
    }

    // ─── Private helpers ──────────────────────────────────────────────

    private String buildCacheKey(String userId, String provider, String model, String prompt) {
        int hash = Objects.hash(userId, provider, model, prompt);
        return userId + ":" + provider + ":" + model + ":" + hash;
    }

    private void evictExpiredEntries() {
        responseCache.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    private String mapComplexityToTier(String complexity) {
        return switch (complexity.toLowerCase()) {
            case "simple" -> "budget";
            case "moderate" -> "economy";
            case "complex" -> "standard";
            case "critical" -> "premium";
            default -> DEFAULT_TIER;
        };
    }

    private boolean isHigherTier(String tier, String target) {
        int tierLevel = getTierLevel(tier);
        int targetLevel = getTierLevel(target);
        return tierLevel >= targetLevel;
    }

    private int getTierLevel(String tier) {
        return switch (tier) {
            case "budget" -> 0;
            case "economy" -> 1;
            case "standard" -> 2;
            case "premium" -> 3;
            case "top" -> 4;
            default -> 2;
        };
    }

    private String getProviderForModel(String modelId) {
        if (modelId.startsWith("gpt-") || modelId.startsWith("o3") || modelId.startsWith("o4")) return "openai";
        if (modelId.startsWith("gemini-")) return "google ai";
        if (modelId.startsWith("claude-")) return "anthropic";
        if (modelId.contains("groq")) return "groq";
        if (modelId.startsWith("llama-") && modelId.contains("versatile")) return "groq";
        if (modelId.startsWith("deepseek-")) return "deepseek";
        if (modelId.startsWith("mistral-")) return "mistral";
        if (modelId.startsWith("grok-")) return "xai";
        return null;
    }

    private String getDefaultModelForProvider(String provider) {
        return switch (provider.toLowerCase()) {
            case "openai" -> "gpt-4.1-mini";
            case "google ai" -> "gemini-2.0-flash";
            case "anthropic" -> "claude-3-5-haiku-20241022";
            case "groq" -> "llama-3.1-8b-instant";
            case "deepseek" -> "deepseek-chat";
            case "mistral" -> "mistral-small-latest";
            case "xai" -> "grok-3-mini";
            default -> "gpt-4.1-mini";
        };
    }

    // ─── Inner classes ──────────────────────────────────────────────

    private static class CachedResponse {
        final String response;
        final long cachedAt;

        CachedResponse(String response) {
            this.response = response;
            this.cachedAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > CACHE_TTL_MINUTES * 60 * 1000L;
        }
    }

    private static class ModelTier {
        final String tier;
        final double costPerRequest; // Approximate cost per 1K tokens

        ModelTier(String tier, double costPerRequest) {
            this.tier = tier;
            this.costPerRequest = costPerRequest;
        }
    }

    /**
     * Result of model selection: which model, API key, provider, and base URL to use.
     */
    public static class SelectedModel {
        public final String modelId;
        public final String apiKey;
        public final String provider;
        public final String baseUrl;

        public SelectedModel(String modelId, String apiKey, String provider, String baseUrl) {
            this.modelId = modelId;
            this.apiKey = apiKey;
            this.provider = provider;
            this.baseUrl = baseUrl;
        }
    }
}
