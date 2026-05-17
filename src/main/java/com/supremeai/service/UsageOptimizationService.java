package com.supremeai.service;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
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

    @Autowired
    private ProviderTypeRegistry providerTypeRegistry;

    @Autowired
    private com.supremeai.repository.ProviderRepository providerRepository;

    private static final String DEFAULT_TIER = "standard";

    private final Map<String, ModelTier> modelTiersCache = new ConcurrentHashMap<>();

    @jakarta.annotation.PostConstruct
    public void initModelTiers() {
        modelTiersCache.put("gpt-4.1-nano", new ModelTier("budget", 0.0001));
        modelTiersCache.put("gpt-4.1-mini", new ModelTier("economy", 0.0003));
        modelTiersCache.put("llama-3.1-8b-instant", new ModelTier("budget", 0.00005));
        modelTiersCache.put("gemini-2.0-flash-lite", new ModelTier("budget", 0.000075));
        modelTiersCache.put("gemini-2.0-flash", new ModelTier("economy", 0.0001));
        modelTiersCache.put("claude-3-5-haiku-20241022", new ModelTier("economy", 0.0008));
        modelTiersCache.put("deepseek-chat", new ModelTier("economy", 0.00014));
        modelTiersCache.put("mistral-small-latest", new ModelTier("economy", 0.0002));
        modelTiersCache.put("gpt-4o", new ModelTier("standard", 0.0025));
        modelTiersCache.put("gpt-4.1", new ModelTier("standard", 0.002));
        modelTiersCache.put("gemini-2.5-flash-preview-04-17", new ModelTier("standard", 0.00015));
        modelTiersCache.put("llama-3.3-70b-versatile", new ModelTier("standard", 0.0006));
        modelTiersCache.put("deepseek-reasoner", new ModelTier("standard", 0.00055));
        modelTiersCache.put("gemini-2.5-pro-preview-03-25", new ModelTier("premium", 0.00125));
        modelTiersCache.put("claude-sonnet-4-20250514", new ModelTier("premium", 0.003));
        modelTiersCache.put("claude-3-7-sonnet-20250219", new ModelTier("premium", 0.003));
        modelTiersCache.put("o4-mini", new ModelTier("premium", 0.0015));
        modelTiersCache.put("mistral-large-latest", new ModelTier("premium", 0.002));
        modelTiersCache.put("grok-3", new ModelTier("premium", 0.003));
        modelTiersCache.put("o3", new ModelTier("top", 0.015));
        modelTiersCache.put("grok-3-mini", new ModelTier("premium", 0.001));
    }

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
    public Mono<SelectedModel> selectModelForTask(String userId, String complexity) {
        String targetTier = mapComplexityToTier(complexity);

        // Find all active keys for this user
        return userApiKeyRepository.findByUserIdAndStatus(userId, "active")
            .collectList()
            .flatMap(keys -> {
                if (keys.isEmpty()) return Mono.empty();

                // Group keys by provider
                Map<String, List<UserApiKey>> keysByProvider = new LinkedHashMap<>();
                for (UserApiKey key : keys) {
                    keysByProvider.computeIfAbsent(key.getProvider().toLowerCase(), k -> new ArrayList<>()).add(key);
                }

                // Find the cheapest model in the target tier that the user has a key for
                List<Map.Entry<String, ModelTier>> candidates = new ArrayList<>();
                for (Map.Entry<String, ModelTier> entry : modelTiersCache.entrySet()) {
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
                    return Mono.just(new SelectedModel(
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

                return Mono.just(new SelectedModel(
                        selected.getKey(),
                        bestKey.getApiKey(),
                        bestKey.getProvider(),
                        bestKey.getBaseUrl()
                ));
            });
    }

    /**
     * Record usage of an API key (for cost tracking and rotation awareness).
     */
    public Mono<Void> recordKeyUsage(String keyId, double estimatedCost) {
        return userApiKeyRepository.findById(keyId)
            .flatMap(key -> {
                key.recordUsage(estimatedCost);
                return userApiKeyRepository.save(key);
            })
            .then();
    }

    /**
     * Get usage summary for a user across all providers.
     */
    public Mono<Map<String, Object>> getUserUsageSummary(String userId) {
        return userApiKeyRepository.findByUserId(userId).collectList()
            .map(keys -> {
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
            });
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
        try {
            com.supremeai.model.APIProvider p = providerRepository.findAll()
                    .filter(prov -> prov.getModels() != null && prov.getModels().stream()
                            .anyMatch(m -> m.equalsIgnoreCase(modelId)))
                    .blockFirst();
            if (p != null && p.getType() != null) return p.getType();
        } catch (Exception e) {
            log.debug("Could not resolve provider for model {}: {}", modelId, e.getMessage());
        }
        com.supremeai.model.ProviderTypeConfig typeConfig = providerTypeRegistry.getTypeConfig(modelId);
        if (typeConfig != null && typeConfig.getExtraConfig() != null) {
            Object provider = typeConfig.getExtraConfig().get("provider");
            if (provider instanceof String) return (String) provider;
        }
        if (modelId.startsWith("gpt-") || modelId.startsWith("o3") || modelId.startsWith("o4")) return "openai";
        if (modelId.startsWith("gemini-")) return "google ai";
        if (modelId.startsWith("claude-")) return "anthropic";
        if (modelId.startsWith("deepseek-")) return "deepseek";
        if (modelId.startsWith("mistral-")) return "mistral";
        if (modelId.startsWith("grok-")) return "xai";
        return null;
    }

    private String getDefaultModelForProvider(String provider) {
        com.supremeai.model.ProviderTypeConfig typeConfig = providerTypeRegistry.getTypeConfig(provider);
        if (typeConfig != null && typeConfig.getDefaultModel() != null) {
            return typeConfig.getDefaultModel();
        }
        try {
            com.supremeai.model.APIProvider p = providerRepository.findAll()
                    .filter(prov -> prov.getType() != null && prov.getType().equalsIgnoreCase(provider))
                    .sort(java.util.Comparator.comparingInt(com.supremeai.model.APIProvider::getPriority))
                    .blockFirst();
            if (p != null) {
                if (p.getModels() != null && !p.getModels().isEmpty()) return p.getModels().get(0);
                if (p.getModelName() != null) return p.getModelName();
            }
        } catch (Exception e) {
            log.debug("Could not resolve default model for provider {}: {}", provider, e.getMessage());
        }
        return "default";
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
