package com.supremeai.provider;

import com.supremeai.service.AIProviderService;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.service.ContextualAIRankingService;
import com.supremeai.service.ProviderMetadataService;
import com.supremeai.service.ProviderTypeRegistry;
import com.supremeai.learning.SelfLearningRouter;
import com.supremeai.learning.EnhancedSelfLearningRouter;
import com.supremeai.model.APIProvider;
import com.supremeai.model.ProviderTypeConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AIProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(AIProviderFactory.class);

    private static final int HEALTH_CACHE_MAX_SIZE = 25;
    private static final long HEALTH_CACHE_TTL_MS = 30_000;

    private final com.github.benmanes.caffeine.cache.LoadingCache<String, Boolean> providerHealthCache = Caffeine.newBuilder()
            .maximumSize(HEALTH_CACHE_MAX_SIZE)
            .expireAfterWrite(Duration.ofMillis(HEALTH_CACHE_TTL_MS))
            .build(providerName -> {
                try {
                    AIProvider provider = getProvider(providerName);
                    String testResponse = provider.generate("test")
                            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                            .block(Duration.ofSeconds(3));
                    return testResponse != null && !testResponse.isEmpty();
                } catch (Exception e) {
                    logger.debug("Health check failed for {}: {}", providerName, e.getMessage());
                    return false;
                }
            });

    @Autowired
    private ProviderMetadataService providerMetadataService;

    @Autowired
    private ProviderTypeRegistry providerTypeRegistry;

    @Autowired
    private AIProviderService aiProviderService;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired(required = false)
    private SelfLearningRouter selfLearningRouter;

    @Autowired(required = false)
    private EnhancedSelfLearningRouter enhancedRouter;

    @Autowired
    private com.supremeai.agent.AgentRuleService ruleService;

    @Autowired
    @Lazy
    private ContextualAIRankingService contextualRankingService;

    private void injectMetadataService(AIProvider provider) {
        if (provider instanceof AbstractHttpProvider httpProvider) {
            httpProvider.setProviderMetadataService(this.providerMetadataService);
        }
    }

    public AIProvider getProvider(String name) {
        return getProvider(name, null);
    }

    public AIProvider getProvider(String name, String overrideApiKey) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider name cannot be null or empty.");
        }

        String normalizedName = name.toLowerCase().trim();
        logger.info("[Factory] getProvider called: name={}, normalizedName={}, overrideApiKeyLength={}", name, normalizedName, overrideApiKey != null ? overrideApiKey.length() : "null");

        if (isLocalFirstExcluded(normalizedName)) {
            logger.info("[Factory] Provider '{}' is excluded under local-first policy; routing to stub/local fallback.", normalizedName);
            return stubLocalProvider;
        }

        APIProvider metadata = providerMetadataService != null ? providerMetadataService.getMetadata(normalizedName) : null;

        if (metadata == null) {
            logger.info("[Factory] Provider '{}' not found in cache. Attempting DB query fallback...", normalizedName);
            try {
                Mono<APIProvider> lookupChain = Mono.empty();
                if (providerRepository != null) {
                    lookupChain = providerRepository.findById(name)
                        .switchIfEmpty(providerRepository.findById(normalizedName))
                        .switchIfEmpty(
                            providerRepository.findAll()
                                .filter(p -> {
                                    String pName = p.getName() != null ? p.getName().toLowerCase().trim() : "";
                                    String pId = p.getId() != null ? p.getId().toLowerCase().trim() : "";
                                    String pDocId = p.getDocumentId() != null ? p.getDocumentId().toLowerCase().trim() : "";
                                    return pName.equals(normalizedName) || pId.equals(normalizedName) || pDocId.equals(normalizedName);
                                })
                                .next()
                        )
                        .timeout(Duration.ofSeconds(5));
                }
                APIProvider dbProvider = lookupChain
                        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                        .block(Duration.ofSeconds(5));
                if (dbProvider != null) {
                    metadata = dbProvider;
                    logger.info("[Factory] Fallback found provider '{}' in DB", normalizedName);
                }
            } catch (Exception e) {
                logger.error("[Factory] DB query fallback failed for '{}': {}", normalizedName, e.getMessage(), e);
            }
        }

        logger.info("[Factory] Metadata found for '{}': {}", normalizedName, metadata != null ? "YES (baseUrl=" + metadata.getBaseUrl() + ")" : "NO");

        if (metadata != null && metadata.getBaseUrl() != null && !metadata.getBaseUrl().isBlank()) {
            String key = resolveKey(overrideApiKey, metadata.getApiKey(), normalizedName);
            String defaultModel = resolveModel(metadata);
            logger.info("[Cloud-Only] Resolving provider '{}' from Firestore: baseUrl={}, model={}", normalizedName, metadata.getBaseUrl(), defaultModel);
            SupremeCloudProvider provider = new SupremeCloudProvider(key, normalizedName, defaultModel, metadata.getBaseUrl());
            injectMetadataService(provider);
            return provider;
        }

        ProviderTypeConfig typeConfig = providerTypeRegistry != null ? providerTypeRegistry.getTypeConfig(normalizedName) : null;
        logger.info("[Factory] TypeConfig found for '{}': {}", normalizedName, typeConfig != null ? "YES (baseUrl=" + typeConfig.getDefaultBaseUrl() + ")" : "NO");
        if (typeConfig != null && typeConfig.getDefaultBaseUrl() != null && !typeConfig.getDefaultBaseUrl().isBlank()) {
            String key = resolveKey(overrideApiKey, null, normalizedName);
            String defaultModel = typeConfig.getDefaultModel();
            if (defaultModel == null || defaultModel.isBlank()) {
                throw new IllegalStateException(
                    "No default model configured for provider type '" + normalizedName + "'. "
                    + "Register a defaultModel in provider_types."
                );
            }
            logger.info("[Cloud-Only] Resolving provider '{}' from provider_types: baseUrl={}, model={}", normalizedName, typeConfig.getDefaultBaseUrl(), defaultModel);
            SupremeCloudProvider provider = new SupremeCloudProvider(key, normalizedName, defaultModel, typeConfig.getDefaultBaseUrl());
            injectMetadataService(provider);
            return provider;
        }

        logger.warn("[Factory] Provider '{}' not found in Firestore api_providers or provider_types. Using dynamic default.", normalizedName);
        try {
            AIProvider defaultProvider = getDefaultProvider();
            if (defaultProvider == null) {
                throw new IllegalArgumentException("Unknown AI provider: " + name + " and no healthy default available.");
            }
            return defaultProvider;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("[Factory] getDefaultProvider failed for '{}': {}", normalizedName, e.getMessage(), e);
            throw new IllegalArgumentException("Unknown AI provider: " + name + " and no healthy default available.", e);
        }
    }

    private String resolveKey(String overrideApiKey, String metadataApiKey, String providerName) {
        if (overrideApiKey != null && !overrideApiKey.isEmpty()) return overrideApiKey;
        if (metadataApiKey != null && !metadataApiKey.isEmpty()) return metadataApiKey;
        String serviceKey = aiProviderService != null ? aiProviderService.getActiveKey(providerName) : null;
        return serviceKey != null ? serviceKey : "";
    }

    private String resolveModel(APIProvider metadata) {
        if (metadata.getModels() != null && !metadata.getModels().isEmpty()) {
            return metadata.getModels().get(0);
        }
        if (metadata.getModelName() != null && !metadata.getModelName().isBlank()) {
            return metadata.getModelName();
        }
        ProviderTypeConfig typeConfig = providerTypeRegistry != null ? providerTypeRegistry.getTypeConfig(metadata.getType()) : null;
        if (typeConfig != null && typeConfig.getDefaultModel() != null && !typeConfig.getDefaultModel().isBlank()) {
            return typeConfig.getDefaultModel();
        }
        throw new IllegalStateException(
            "No model configured for provider '" + metadata.getName() + "'. "
            + "Set 'modelName' or 'models' in the Firestore api_providers document, "
            + "or register a defaultModel in provider_types for type='" + metadata.getType() + "'.");
    }

    public AIProvider getEnforcedProvider(String name) {
        String normalizedName = name.toLowerCase().trim();
        if (isLocalFirstExcluded(normalizedName)) {
            logger.info("[Factory] getEnforcedProvider routed excluded provider '{}' to stub/local fallback.", normalizedName);
            return stubLocalProvider;
        }
        return new RuleEnforcingAIProvider(getProvider(name), ruleService);
    }

    public AIProvider getEnforcedProvider(String name, String overrideApiKey) {
        String normalizedName = name.toLowerCase().trim();
        if (isLocalFirstExcluded(normalizedName)) {
            logger.info("[Factory] getEnforcedProvider routed excluded provider '{}' to stub/local fallback.", normalizedName);
            return stubLocalProvider;
        }
        return new RuleEnforcingAIProvider(getProvider(name, overrideApiKey), ruleService);
    }

    public Mono<AIProvider> getBestProviderForTask(String taskType) {
        logger.debug("Finding best provider for task: {}", taskType);
        return Mono.fromCallable(() -> {
            List<String> candidates = getHealthyProviders();

            if (candidates.isEmpty()) {
                throw new RuntimeException("No healthy AI providers available");
            }

            if (enhancedRouter != null) {
                try {
                    String chosen = enhancedRouter.getBestProviderForTask(taskType, candidates);
                    if (chosen != null && candidates.contains(chosen)) {
                        logger.info("[ROUTER] Enhanced router selected {} for {}", chosen, taskType);
                        return getProvider(chosen);
                    }
                } catch (Exception e) {
                    logger.warn("Enhanced router failed: {}", e.getMessage());
                }
            }

            return selectProviderByRankingOrFallback(taskType, candidates);
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private List<String> getHealthyProviders() {
        List<String> healthy = new ArrayList<>();
        for (String providerName : getSupportedProviders()) {
            try {
                AIProvider provider = getProvider(providerName);
                if (isProviderHealthy(provider)) {
                    healthy.add(providerName);
                }
            } catch (Exception e) {
                // skip
            }
        }
        return healthy;
    }

    private AIProvider selectProviderByRankingOrFallback(String taskType, List<String> candidates) {
        try {
            ContextualAIRankingService.TaskType rankingTaskType = ContextualAIRankingService.TaskType.QUESTION_ANSWERING;
            try {
                rankingTaskType = ContextualAIRankingService.TaskType.valueOf(taskType.toUpperCase());
            } catch (IllegalArgumentException ignored) {}

            List<ContextualAIRankingService.ProviderRanking> rankings = contextualRankingService.getRankingsForTask(rankingTaskType);

            if (rankings != null && !rankings.isEmpty()) {
                for (ContextualAIRankingService.ProviderRanking ranking : rankings) {
                    if (candidates.contains(ranking.provider.toLowerCase())) {
                        try {
                            AIProvider provider = getProvider(ranking.provider);
                            logger.info("Using ranked provider {} for task {}", ranking.provider, taskType);
                            return provider;
                        } catch (Exception e) {
                            logger.warn("Ranked provider {} unavailable: {}", ranking.provider, e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Ranking service error for task {}: {}", taskType, e.getMessage());
        }

        logger.info("No ranked provider available for task {}, using default", taskType);
        return getDefaultProvider();
    }

    @Autowired
    private StubLocalProvider stubLocalProvider;

    public AIProvider getDefaultProvider() {
        logger.info("Dynamically searching for default provider from metadata cache (local-first mode)");

        List<APIProvider> activeProviders = new ArrayList<>();
        if (providerMetadataService != null) {
            providerMetadataService.getAllMetadata().values().stream()
                    .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
                    .forEach(activeProviders::add);
        }

        if (activeProviders.isEmpty()) {
            logger.warn("Metadata cache empty for getDefaultProvider, querying database as fallback...");
            try {
                List<APIProvider> dbList = providerRepository.findByStatus("active")
                        .collectList()
                        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                        .block(Duration.ofSeconds(5));
                if (dbList != null) {
                    activeProviders.addAll(dbList);
                }
            } catch (Exception e) {
                logger.error("Failed to query fallback active providers", e);
            }
        }

        if (activeProviders.isEmpty()) {
            logger.info("[LOCAL-FIRST] No external AI providers configured. Using StubLocalProvider for offline operation.");
            return stubLocalProvider;
        }

        AIProvider provider = activeProviders.stream()
                .filter(p -> !isLocalFirstExcluded(p.getName()))
                .sorted(Comparator.comparingInt(APIProvider::getPriority))
                .map(this::createProviderFromConfig)
                .filter(Objects::nonNull)
                .filter(this::isProviderHealthy_offElastic)
                .findFirst()
                .orElse(stubLocalProvider);

        return provider;
    }

    private boolean isProviderHealthy_offElastic(AIProvider provider) {
        String providerName = provider.getName();
        try {
            return providerHealthCache.get(providerName);
        } catch (Exception e) {
            logger.debug("Health check failed for {}: {}", providerName, e.getMessage());
            return false;
        }
    }

    private boolean isProviderHealthy(AIProvider provider) {
        String providerName = provider.getName();
        try {
            return providerHealthCache.get(providerName);
        } catch (Exception e) {
            logger.debug("Health check failed for {}: {}", providerName, e.getMessage());
            return false;
        }
    }

    public String[] getSupportedProviders() {
        if (providerMetadataService != null) {
            Map<String, APIProvider> allMeta = providerMetadataService.getAllMetadata();
            if (!allMeta.isEmpty()) {
                return allMeta.values().stream()
                        .map(APIProvider::getName)
                        .filter(name -> !isLocalFirstExcluded(name))
                        .toArray(String[]::new);
            }
        }

        logger.warn("Metadata cache empty for getSupportedProviders, querying database reactively...");
        try {
            List<APIProvider> list = providerRepository.findAll()
                    .collectList()
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .block(Duration.ofSeconds(5));
            if (list != null) {
                return list.stream()
                        .map(APIProvider::getName)
                        .filter(name -> !isLocalFirstExcluded(name))
                        .toArray(String[]::new);
            }
        } catch (Exception e) {
            logger.error("Failed to query supported providers synchronously", e);
        }
        return new String[0];
    }

    private boolean isLocalFirstExcluded(String normalizedName) {
        if (normalizedName == null) return true;
        String n = normalizedName.toLowerCase();
        return n.equals("groq")
                || n.equals("openai")
                || n.equals("anthropic")
                || n.equals("deepseek");
    }

    public String[] getAllProviderNames() {
        return getSupportedProviders();
    }

    public List<AIProvider> getAllProviders() {
        List<AIProvider> providers = new ArrayList<>();
        for (String providerName : getSupportedProviders()) {
            try {
                providers.add(getProvider(providerName));
            } catch (Exception e) {
                logger.debug("Could not create provider instance for {}: {}", providerName, e.getMessage());
            }
        }
        return providers;
    }

    public List<String> getAvailableProviderIds() {
        return Arrays.asList(getSupportedProviders());
    }

    public Flux<String> getActiveHelperProviderIds() {
        return providerRepository.findByStatus("active")
                .map(APIProvider::getId);
    }

    public void clearHealthCache() {
        providerHealthCache.invalidateAll();
        logger.info("Provider health cache cleared");
    }

    public AIProvider createProviderFromConfig(APIProvider config) {
        if (config == null) return null;

        String name = config.getName() != null ? config.getName() : "Unknown";
        String apiKey = config.getApiKey() != null && !config.getApiKey().isEmpty()
                        ? config.getApiKey()
                        : aiProviderService.getActiveKey(name.toLowerCase());
        String baseUrl = config.getBaseUrl();
        String defaultModel = resolveModel(config);

        if (baseUrl != null && !baseUrl.isEmpty()) {
            logger.info("Dynamically creating cloud provider: {} with baseUrl: {}", name, baseUrl);
            return new RuleEnforcingAIProvider(new SupremeCloudProvider(apiKey, name, defaultModel, baseUrl), ruleService);
        }

        logger.error("Cannot create provider {}: no baseUrl configured in Firestore", name);
        return null;
    }

    public Mono<Boolean> checkProviderHealth(APIProvider config) {
        return Mono.fromCallable(() -> {
            AIProvider provider = createProviderFromConfig(config);
            return provider != null && isProviderHealthy_offElastic(provider);
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
}
