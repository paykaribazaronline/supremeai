package com.supremeai.provider;

import com.supremeai.service.AIProviderService;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.service.ContextualAIRankingService;
import com.supremeai.learning.SelfLearningRouter;
import com.supremeai.learning.EnhancedSelfLearningRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating AI provider instances
 * Supports AI models for voting system (S4)
 */
@Component
public class AIProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(AIProviderFactory.class);

    @Autowired
    private AIProviderService aiProviderService;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    @Lazy
    private ContextualAIRankingService contextualRankingService;

    @Autowired(required = false)
    private OllamaProvider ollamaProvider;

    @Autowired(required = false)
    private SelfLearningRouter selfLearningRouter;

    @Autowired(required = false)
    private EnhancedSelfLearningRouter enhancedRouter;

    @Autowired
    private com.supremeai.agent.AgentRuleService ruleService;

    // Cache for provider health status
    private final Map<String, Boolean> providerHealthCache = new ConcurrentHashMap<>();

    public AIProvider getProvider(String name) {
        return getProvider(name, null);
    }

    public AIProvider getProvider(String name, String overrideApiKey) {
        if (name == null || name.trim().isEmpty()) {
            logger.warn("Attempted to get provider with null or empty name. Falling back to default.");
            return getDefaultProvider();
        }

        String normalizedName = name.toLowerCase().trim();
        String activeKey = aiProviderService.getActiveKey(normalizedName);
        String key = (overrideApiKey != null && !overrideApiKey.isEmpty()) 
                     ? overrideApiKey 
                     : activeKey;

        switch (normalizedName) {
            // Core AI Models for S4 Voting System
            case "gpt4":
            case "openai":
                return new OpenAIProvider(key);

            case "claude":
            case "anthropic":
                return new AnthropicProvider(key);

            case "gemini":
            case "google":
                return new GeminiProvider(key);

            case "groq":
                return new GroqProvider(key);

            case "deepseek":
                return new DeepSeekProvider(key);

            case "ollama":
            case "local":
                if (ollamaProvider == null) {
                    logger.error("Ollama provider bean not found. Add @Profile exclusion or enable in config.");
                    throw new IllegalStateException("Ollama provider not available. Check Spring configuration.");
                }
                return ollamaProvider;

            case "huggingface":
                return new HuggingFaceProvider(key);

            case "kimi":
                return new KimiProvider(key);

            case "mistral":
                return new MistralProvider(key);

            case "stepfun":
                return new StepFunProvider(key);

            case "codegeex4":
            case "codegeex":
                return new CodeGeeX4Provider(key);

            case "gcp_qwen":
                return new SupremeCloudProvider(key, "gcp_qwen", "qwen2.5-coder:7b", "https://supreme-ai-qwen-coder-565236080752.us-central1.run.app");
            case "gcp_llama":
                return new SupremeCloudProvider(key, "gcp_llama", "llama3.1:8b", "https://supreme-ai-llama-3-1-565236080752.us-central1.run.app");
            case "gcp_phi":
                return new SupremeCloudProvider(key, "gcp_phi", "phi3", "https://supreme-ai-phi-3-565236080752.us-central1.run.app");
            case "gcp_nomic":
                return new SupremeCloudProvider(key, "gcp_nomic", "nomic-embed-text", "https://supreme-ai-nomic-embed-565236080752.us-central1.run.app");
            case "hf_deepseek":
                return new SupremeCloudProvider(key, "hf_deepseek", "deepseek-coder-v2", "https://supreme-ai-deepseek-pro-565236080752.us-central1.run.app");
            
            // HuggingFace Serverless Inference Endpoints
            case "hf_mistral":
                return new SupremeCloudProvider(key, "hf_mistral", "mistralai/Mistral-7B-Instruct-v0.3", "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.3");
            case "hf_llama":
                return new SupremeCloudProvider(key, "hf_llama", "meta-llama/Meta-Llama-3-8B-Instruct", "https://api-inference.huggingface.co/models/meta-llama/Meta-Llama-3-8B-Instruct");
            case "hf_codellama":
                return new SupremeCloudProvider(key, "hf_codellama", "codellama/CodeLlama-7B-Instruct-hf", "https://api-inference.huggingface.co/models/codellama/CodeLlama-7B-Instruct-hf");
            case "hf_phi":
                return new SupremeCloudProvider(key, "hf_phi", "microsoft/Phi-3-mini-4k-instruct", "https://api-inference.huggingface.co/models/microsoft/Phi-3-mini-4k-instruct");
            
            // Render Free Tier Endpoints
            case "render_tinyllama":
                return new SupremeCloudProvider(key, "render_tinyllama", "tinyllama-1.1b", System.getenv().getOrDefault("RENDER_TINYLLAMA_URL", "https://tinyllama.onrender.com"));
            case "render_phi3":
                return new SupremeCloudProvider(key, "render_phi3", "phi-3-mini", System.getenv().getOrDefault("RENDER_PHI3_URL", "https://phi3.onrender.com"));
            case "render_phi2":
                return new SupremeCloudProvider(key, "render_phi2", "phi-2", System.getenv().getOrDefault("RENDER_PHI2_URL", "https://phi2.onrender.com"));
            case "render_qwen":
                return new SupremeCloudProvider(key, "render_qwen", "qwen-0.5b", System.getenv().getOrDefault("RENDER_QWEN_URL", "https://qwen.onrender.com"));

            // HuggingFace Specialized Models
            case "hf_phi_vision":
                return new SupremeCloudProvider(key, "hf_phi_vision", "microsoft/Phi-3-vision-128k-instruct", "https://api-inference.huggingface.co/models/microsoft/Phi-3-vision-128k-instruct");
            case "hf_paligemma":
                return new SupremeCloudProvider(key, "hf_paligemma", "google/paligemma-3b-mix-448", "https://api-inference.huggingface.co/models/google/paligemma-3b-mix-448");
            case "hf_e5_large":
                return new SupremeCloudProvider(key, "hf_e5_large", "intfloat/multilingual-e5-large", "https://api-inference.huggingface.co/models/intfloat/multilingual-e5-large");
            case "hf_bge":
                return new SupremeCloudProvider(key, "hf_bge", "BAAI/bge-large-en-v1.5", "https://api-inference.huggingface.co/models/BAAI/bge-large-en-v1.5");

            default:
                logger.warn("Unknown AI provider request: {}. Falling back to default if possible.", normalizedName);
                try {
                    return getDefaultProvider();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unknown AI provider: " + name + " and no healthy default available.");
                }
        }
    }

    public AIProvider getEnforcedProvider(String name) {
        return new RuleEnforcingAIProvider(getProvider(name), ruleService);
    }

    public AIProvider getEnforcedProvider(String name, String overrideApiKey) {
        return new RuleEnforcingAIProvider(getProvider(name, overrideApiKey), ruleService);
    }

    /**
     * Get the best provider for a specific task based on rankings and health
     * @param taskType Type of task (e.g., "code_generation", "code_analysis", "question_answering")
     * @return Best available AI provider for the task
     */
    public AIProvider getBestProviderForTask(String taskType) {
        logger.debug("Finding best provider for task: {}", taskType);

        // Extract required skills from task type
        List<String> requiredSkills = extractSkillsFromTaskType(taskType);

        // Get candidate providers (healthy ones)
        List<String> candidates = getHealthyProviders();

        if (candidates.isEmpty()) {
            throw new RuntimeException("No healthy AI providers available");
        }

        // Use enhanced router if available
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

        // Fallback to original method
        return selectProviderByRankingOrFallback(taskType, candidates, requiredSkills);
    }

    /**
     * Extract skill requirements from task type
     */
    private List<String> extractSkillsFromTaskType(String taskType) {
        String tt = taskType.toLowerCase();
        List<String> skills = new ArrayList<>();

        if (tt.contains("code") || tt.contains("generation")) {
            skills.add("coding");
        }
        if (tt.contains("analysis") || tt.contains("analyze")) {
            skills.add("analysis");
        }
        if (tt.contains("creative") || tt.contains("writing")) {
            skills.add("creative");
        }
        if (tt.contains("math") || tt.contains("logic")) {
            skills.add("math");
        }
        if (tt.contains("vision") || tt.contains("image")) {
            skills.add("vision");
        }
        if (tt.contains("summar") || tt.contains("qa") || tt.contains("question")) {
            skills.add("understanding");
        }

        return skills;
    }

    /**
     * Get all currently healthy providers
     */
    private List<String> getHealthyProviders() {
        List<String> healthy = new ArrayList<>();
        String[] allProviders = getSupportedProviders();
        for (String providerName : allProviders) {
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

    /**
     * Original fallback routing: ranking → default
     */
    private AIProvider selectProviderByRankingOrFallback(String taskType, List<String> candidates, List<String> requiredSkills) {
        // Try contextual ranking service
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

        // Fallback to default provider selection
        logger.info("No ranked provider available for task {}, using default", taskType);
        return getDefaultProvider();
    }

    /**
     * Get the default/healthiest available provider
     * @return A working AI provider
     */
    public AIProvider getDefaultProvider() {
        logger.info("Dynamically searching for healthiest default provider from database");

        return providerRepository.findByStatus("ACTIVE")
                .collectList()
                .blockOptional()
                .orElse(Collections.emptyList())
                .stream()
                .sorted(Comparator.comparingInt(com.supremeai.model.APIProvider::getPriority))
                .map(this::createProviderFromConfig)
                .filter(Objects::nonNull)
                .filter(this::isProviderHealthy)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No working AI provider available in database"));
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
     * Get list of all supported provider names from database
     */
    public String[] getSupportedProviders() {
        return providerRepository.findAll()
                .map(com.supremeai.model.APIProvider::getName)
                .collectList()
                .blockOptional()
                .orElse(Collections.emptyList())
                .toArray(new String[0]);
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

    /**
     * Creates an AI provider instance directly from database configuration.
     * This enables dynamic provider support without code changes.
     */
    public AIProvider createProviderFromConfig(com.supremeai.model.APIProvider config) {
        if (config == null) return null;
        
        String name = config.getName() != null ? config.getName() : "Unknown";
        String type = config.getType() != null ? config.getType().toLowerCase() : "unknown";
        String apiKey = config.getApiKey() != null && !config.getApiKey().isEmpty() 
                        ? config.getApiKey() 
                        : aiProviderService.getActiveKey(name.toLowerCase());
        String baseUrl = config.getBaseUrl();
        String defaultModel = (config.getModels() != null && !config.getModels().isEmpty()) 
                             ? config.getModels().get(0) 
                             : "default";

        logger.info("Dynamically creating provider: {} of type: {}", name, type);

        // Try to use hardcoded logic if it's a known standard provider type
        try {
            // First try by type, then by name
            AIProvider provider;
            try {
                provider = getProvider(type, apiKey);
            } catch (Exception e) {
                provider = getProvider(name, apiKey);
            }
            return new RuleEnforcingAIProvider(provider, ruleService);
        } catch (IllegalArgumentException e) {
            // Not a standard provider, use generic SupremeCloudProvider for custom endpoints
            if (baseUrl != null && !baseUrl.isEmpty()) {
                logger.info("Using generic SupremeCloudProvider for custom endpoint: {}", baseUrl);
                return new RuleEnforcingAIProvider(new SupremeCloudProvider(apiKey, name, defaultModel, baseUrl), ruleService);
            }
            logger.error("Cannot create provider {}: {}", name, e.getMessage());
            return null;
        }
    }
}
