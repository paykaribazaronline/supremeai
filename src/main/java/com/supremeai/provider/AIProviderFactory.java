package com.supremeai.provider;

import com.supremeai.service.AIProviderService;
import com.supremeai.service.AIRankingService;
import com.supremeai.service.AIRankingService.ProviderRanking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating AI provider instances
 * Supports 10 AI models for voting system (S4)
 */
@Component
public class AIProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(AIProviderFactory.class);

    @Autowired
    private AIProviderService aiProviderService;

    @Autowired
    private com.supremeai.service.AIRankingService aiRankingService;

    @Value("${ai.providers.airllm.endpoint:}")
    private String airllmEndpoint;

    @Value("${ai.providers.airllm.model:mistralai/Mistral-7B-Instruct-v0.3}")
    private String airllmModel;

    @Autowired(required = false)
    private OllamaProvider ollamaProvider;

    // Cache for provider health status
    private final Map<String, Boolean> providerHealthCache = new ConcurrentHashMap<>();

    public AIProvider getProvider(String name) {
        return getProvider(name, null);
    }

    public AIProvider getProvider(String name, String overrideApiKey) {
        String key = (overrideApiKey != null && !overrideApiKey.isEmpty()) 
                     ? overrideApiKey 
                     : aiProviderService.getActiveKey(name.toLowerCase());

        switch (name.toLowerCase()) {
            // Core 10 AI Models for S4 Voting System
            case "gpt4":
            case "openai":
                return new OpenAIProvider(key);

            case "claude":
            case "anthropic":
                return new AnthropicProvider(key);

            case "gemini":
                return new GeminiProvider(key);

            case "groq":
                return new GroqProvider(key);

            case "deepseek":
                return new DeepSeekProvider(key);

            case "ollama":
                if (ollamaProvider == null) {
                    logger.error("Ollama provider bean not found. Add @Profile exclusion or enable in config.");
                    throw new IllegalStateException("Ollama provider not available. Check Spring configuration.");
                }
                return ollamaProvider;

            case "huggingface":
                return new HuggingFaceProvider(key);

            case "airllm":
                return new AirLLMProvider(airllmEndpoint, key, airllmModel);

            case "kimi":
                return new KimiProvider(key);

            case "mistral":
                return new MistralProvider(key);

            case "stepfun":
                return new StepFunProvider(key);

            default:
                throw new IllegalArgumentException("Unknown AI provider: " + name + ". Supported: gpt4, claude, gemini, groq, deepseek, ollama, huggingface, airllm, kimi, mistral, stepfun");
        }
    }

    /**
     * Get the best provider for a specific task type based on rankings and health
     * @param taskType Type of task (e.g., "code_generation", "code_analysis", "question_answering")
     * @return Best available AI provider for the task
     */
    public AIProvider getBestProviderForTask(String taskType) {
        logger.debug("Finding best provider for task: {}", taskType);

        // Try to get ranked providers for this task
        try {
            List<ProviderRanking> rankings = aiRankingService.getRankings();

            if (rankings != null && !rankings.isEmpty()) {
                // Try providers in order of ranking
                for (ProviderRanking ranking : rankings) {
                    try {
                        AIProvider provider = getProvider(ranking.getProvider());
                        if (isProviderHealthy(provider)) {
                            logger.info("Using ranked provider {} for task {}", ranking.getProvider(), taskType);
                            return provider;
                        }
                    } catch (Exception e) {
                        logger.warn("Ranked provider {} unavailable: {}", ranking.getProvider(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get provider rankings for task {}: {}", taskType, e.getMessage());
        }

        // Fall back to default provider
        logger.info("No ranked providers available for task {}, using default", taskType);
        return getDefaultProvider();
    }

    /**
     * Get the default/healthiest available provider
     * @return A working AI provider
     */
    public AIProvider getDefaultProvider() {
        // Preferred providers in order (free tier first)
        String[] preferredProviders = {"stepfun", "groq", "deepseek", "ollama", "gpt4", "claude", "gemini", "mistral"};

        // Try preferred providers first
        for (String providerName : preferredProviders) {
            try {
                AIProvider provider = getProvider(providerName);
                if (isProviderHealthy(provider)) {
                    logger.info("Using {} as default provider", providerName);
                    return provider;
                }
            } catch (Exception e) {
                logger.warn("Preferred provider {} unavailable: {}", providerName, e.getMessage());
            }
        }

        // Try all supported providers
        for (String providerName : getSupportedProviders()) {
            try {
                AIProvider provider = getProvider(providerName);
                if (isProviderHealthy(provider)) {
                    logger.info("Using {} as fallback default provider", providerName);
                    return provider;
                }
            } catch (Exception e) {
                logger.debug("Provider {} unavailable: {}", providerName, e.getMessage());
            }
        }

        throw new RuntimeException("No working AI provider available");
    }

    /**
     * Check if a provider is healthy and responsive
     * @param provider The provider to check
     * @return true if the provider is healthy
     */
    private boolean isProviderHealthy(AIProvider provider) {
        String providerName = provider.getName();

        // Check cache first
        if (providerHealthCache.containsKey(providerName)) {
            return providerHealthCache.get(providerName);
        }

        // Perform health check
        try {
            String testResponse = provider.generate("test").block();
            boolean isHealthy = testResponse != null && !testResponse.isEmpty();
            providerHealthCache.put(providerName, isHealthy);
            return isHealthy;
        } catch (Exception e) {
            logger.debug("Health check failed for {}: {}", providerName, e.getMessage());
            providerHealthCache.put(providerName, false);
            return false;
        }
    }

    /**
     * Get list of all supported provider names
     */
    public String[] getSupportedProviders() {
        return new String[]{"gpt4", "claude", "gemini", "groq", "deepseek", "ollama", "huggingface", "airllm", "kimi", "mistral", "stepfun"};
    }

    /**
     * Get list of all supported provider names (alias for getSupportedProviders)
     */
    public String[] getAllProviderNames() {
        return getSupportedProviders();
    }

    /**
     * Get all available provider instances
     * @return List of all provider instances
     */
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

    /**
     * Get all available provider IDs
     * @return List of provider IDs
     */
    public List<String> getAvailableProviderIds() {
        return Arrays.asList(getSupportedProviders());
    }

    /**
     * Clear the provider health cache
     */
    public void clearHealthCache() {
        providerHealthCache.clear();
        logger.info("Provider health cache cleared");
    }

    private String resolveKey(String override, String fallback) {
        return (override != null && !override.isEmpty()) ? override : fallback;
    }
}
