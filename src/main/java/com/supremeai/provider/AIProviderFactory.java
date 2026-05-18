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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AIProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(AIProviderFactory.class);

    @Autowired
    private ProviderMetadataService providerMetadataService;

    @Autowired
    private ProviderTypeRegistry providerTypeRegistry;

    @Autowired
    private AIProviderService aiProviderService;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    @Lazy
    private ContextualAIRankingService contextualRankingService;

    @Autowired(required = false)
    private SelfLearningRouter selfLearningRouter;

    @Autowired(required = false)
    private EnhancedSelfLearningRouter enhancedRouter;

    @Autowired
    private com.supremeai.agent.AgentRuleService ruleService;

    private final Map<String, Boolean> providerHealthCache = new ConcurrentHashMap<>();

    private void injectMetadataService(AIProvider provider) {
        if (provider instanceof AbstractHttpProvider) {
            try {
                java.lang.reflect.Field field = AbstractHttpProvider.class.getDeclaredField("providerMetadataService");
                field.setAccessible(true);
                field.set(provider, this.providerMetadataService);
            } catch (Exception e) {
                logger.warn("Could not inject providerMetadataService into provider: {}", e.getMessage());
            }
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

        APIProvider metadata = providerMetadataService != null ? providerMetadataService.getMetadata(normalizedName) : null;
        logger.info("[Factory] Metadata found for '{}': {}", normalizedName, metadata != null ? "YES (baseUrl=" + metadata.getBaseUrl() + ")" : "NO");

        if (metadata != null && metadata.getBaseUrl() != null && !metadata.getBaseUrl().isBlank()) {
            String key = resolveKey(overrideApiKey, metadata.getApiKey(), normalizedName);
            String defaultModel = resolveModel(metadata);
            logger.info("[Cloud-Only] Resolving provider '{}' from Firestore: baseUrl={}, model={}",
                    normalizedName, metadata.getBaseUrl(), defaultModel);
            SupremeCloudProvider provider = new SupremeCloudProvider(key, normalizedName, defaultModel, metadata.getBaseUrl());
            injectMetadataService(provider);
            return provider;
        }

        ProviderTypeConfig typeConfig = providerTypeRegistry != null ? providerTypeRegistry.getTypeConfig(normalizedName) : null;
        logger.info("[Factory] TypeConfig found for '{}': {}", normalizedName, typeConfig != null ? "YES (baseUrl=" + typeConfig.getDefaultBaseUrl() + ")" : "NO");
        if (typeConfig != null && typeConfig.getDefaultBaseUrl() != null && !typeConfig.getDefaultBaseUrl().isBlank()) {
            String key = resolveKey(overrideApiKey, null, normalizedName);
            String defaultModel = typeConfig.getDefaultModel() != null ? typeConfig.getDefaultModel() : "default";
            logger.info("[Cloud-Only] Resolving provider '{}' from provider_types: baseUrl={}, model={}",
                    normalizedName, typeConfig.getDefaultBaseUrl(), defaultModel);
            SupremeCloudProvider provider = new SupremeCloudProvider(key, normalizedName, defaultModel, typeConfig.getDefaultBaseUrl());
            injectMetadataService(provider);
            return provider;
        }

        logger.warn("[Factory] Provider '{}' not found in Firestore api_providers or provider_types. Using dynamic default.", normalizedName);
        try {
            return getDefaultProvider();
        } catch (Exception e) {
            logger.error("[Factory] getDefaultProvider failed for '{}': {}", normalizedName, e.toString());
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
        if (typeConfig != null && typeConfig.getDefaultModel() != null) {
            return typeConfig.getDefaultModel();
        }
        return "default";
    }

    public AIProvider getEnforcedProvider(String name) {
        return new RuleEnforcingAIProvider(getProvider(name), ruleService);
    }

    public AIProvider getEnforcedProvider(String name, String overrideApiKey) {
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

    public AIProvider getDefaultProvider() {
        logger.info("Dynamically searching for healthiest default provider from metadata cache");

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
                        .block(java.time.Duration.ofSeconds(5));
                if (dbList != null) {
                    activeProviders.addAll(dbList);
                }
            } catch (Exception e) {
                logger.error("Failed to query fallback active providers", e);
            }
        }

        if (activeProviders.isEmpty()) {
            throw new RuntimeException("No working AI provider available in database/cache");
        }

        return activeProviders.stream()
                .sorted(Comparator.comparingInt(APIProvider::getPriority))
                .map(this::createProviderFromConfig)
                .filter(Objects::nonNull)
                .filter(this::isProviderHealthy_offElastic)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No working AI provider available and healthy"));
    }

    /**
     * Off-elastic copy of {@link #isProviderHealthy(AIProvider)} so callers on the
     * Netty event-loop never block on the health-check.
     */
    private boolean isProviderHealthy_offElastic(AIProvider provider) {
        String providerName = provider.getName();
        if (providerHealthCache.containsKey(providerName)) {
            return providerHealthCache.get(providerName);
        }
        try {
            String testResponse = provider.generate("test")
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .block(java.time.Duration.ofSeconds(3));
            boolean isHealthy = testResponse != null && !testResponse.isEmpty();
            providerHealthCache.put(providerName, isHealthy);
            return isHealthy;
        } catch (Exception e) {
            logger.debug("Health check failed for {}: {}", providerName, e.getMessage());
            providerHealthCache.put(providerName, false);
            return false;
        }
    }

    private boolean isProviderHealthy(AIProvider provider) {
        String providerName = provider.getName();

        if (providerHealthCache.containsKey(providerName)) {
            return providerHealthCache.get(providerName);
        }

        try {
            String testResponse = provider.generate("test")
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .block(java.time.Duration.ofSeconds(3));
            boolean isHealthy = testResponse != null && !testResponse.isEmpty();
            providerHealthCache.put(providerName, isHealthy);
            return isHealthy;
        } catch (Exception e) {
            logger.debug("Health check failed for {}: {}", providerName, e.getMessage());
            providerHealthCache.put(providerName, false);
            return false;
        }
    }

    public String[] getSupportedProviders() {
        if (providerMetadataService != null) {
            Map<String, APIProvider> allMeta = providerMetadataService.getAllMetadata();
            if (!allMeta.isEmpty()) {
                return allMeta.values().stream()
                        .map(APIProvider::getName)
                        .toArray(String[]::new);
            }
        }

        logger.warn("Metadata cache empty for getSupportedProviders, querying database reactively...");
        try {
            List<APIProvider> list = providerRepository.findAll()
                    .collectList()
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .block(java.time.Duration.ofSeconds(5));
            if (list != null) {
                return list.stream().map(APIProvider::getName).toArray(String[]::new);
            }
        } catch (Exception e) {
            logger.error("Failed to query supported providers synchronously", e);
        }
        return new String[0];
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
        providerHealthCache.clear();
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
}
