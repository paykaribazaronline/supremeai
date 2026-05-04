package com.supremeai.fallback;

import com.supremeai.cost.QuotaManager;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.learning.immunity.CodeImmunitySystem;
import com.supremeai.intelligence.profiling.AIProfiler;
import com.supremeai.provider.AIProviderType;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.resilience.RetryableAIExecutor;
import com.supremeai.security.ApiKeyRotationService;
import com.supremeai.model.UserApiKey;
import com.supremeai.service.EnhancedLearningService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AIFallbackOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AIFallbackOrchestrator.class);
    private final QuotaManager quotaManager;
    private final GlobalKnowledgeBase knowledgeBase;
    private final CodeImmunitySystem immunitySystem;
    private final AIProfiler aiProfiler;
    private final RetryableAIExecutor retryExecutor;
    private final ApiKeyRotationService keyRotationService;
    private final AIProviderFactory providerFactory;

    @Autowired(required = false)
    private EnhancedLearningService enhancedLearningService;

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Map<AIProviderType, CircuitBreaker> providerCircuitBreakers = new EnumMap<>(AIProviderType.class);

    private final List<AIProviderType> allProviders = Arrays.asList(
            AIProviderType.GROQ_LLAMA3,
            AIProviderType.GEMINI_PRO,
            AIProviderType.HUGGINGFACE_FREE,
            AIProviderType.ANTHROPIC_CLAUDE,
            AIProviderType.OPENAI,
            AIProviderType.DEEPSEEK,
            AIProviderType.KIMI,
            AIProviderType.MISTRAL,
            AIProviderType.AIRLLM,
            AIProviderType.OLLAMA
    );

    public AIFallbackOrchestrator(QuotaManager quotaManager,
                                  GlobalKnowledgeBase knowledgeBase, CodeImmunitySystem immunitySystem,
                                  AIProfiler aiProfiler, RetryableAIExecutor retryExecutor,
                                  ApiKeyRotationService keyRotationService,
                                  AIProviderFactory providerFactory) {
        this.quotaManager = quotaManager;
        this.knowledgeBase = knowledgeBase;
        this.immunitySystem = immunitySystem;
        this.aiProfiler = aiProfiler;
        this.retryExecutor = retryExecutor;
        this.keyRotationService = keyRotationService;
        this.providerFactory = providerFactory;

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(config);
    }

    @PostConstruct
    public void init() {
        for (AIProviderType provider : allProviders) {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(provider.name().toLowerCase());
            providerCircuitBreakers.put(provider, cb);

            cb.getEventPublisher()
                    .onStateTransition(event -> log.info("Circuit breaker {} transitioned from {} to {}",
                            provider, event.getStateTransition().getFromState(), event.getStateTransition().getToState()));
        }
    }

    public String executeWithSupremeIntelligence(String taskCategory, String errorSignature, String prompt) {
        return executeWithSupremeIntelligence(taskCategory, errorSignature, prompt, "system");
    }

    /**
     * Execute with multi-provider fallback, per-provider circuit breakers,
     * retry with backoff, and API key rotation.
     *
     * @param userId Optional user ID for API key selection
     */
    public String executeWithSupremeIntelligence(String taskCategory, String errorSignature, String prompt, String userId) {

        // STEP 1: CHECK GLOBAL KNOWLEDGE BASE
        String knownSolution = knowledgeBase.findKnownSolution(errorSignature);
        if (knownSolution != null) return knownSolution;

        // STEP 2: DYNAMICALLY RE-ORDER THE FALLBACK CHAIN
        AIProviderType expertProvider = aiProfiler.getBestAIForTask(taskCategory);

        List<AIProviderType> dynamicChain = new ArrayList<>();
        dynamicChain.add(expertProvider);

        for (AIProviderType p : allProviders) {
            if (p != expertProvider) dynamicChain.add(p);
        }

        // STEP 3: EXECUTE WITH CIRCUIT BREAKER + RETRY
        for (AIProviderType provider : dynamicChain) {
            if (!isServiceQuotaAvailable(provider)) {
                log.warn("Quota exhausted for {}, skipping.", provider);
                continue;
            }

            CircuitBreaker cb = providerCircuitBreakers.get(provider);
            if (cb != null && cb.getState() == CircuitBreaker.State.OPEN) {
                log.warn("Circuit breaker OPEN for {}, skipping.", provider);
                continue;
            }

            long startTime = System.currentTimeMillis();

            log.info("-> Asking {} (Expert Mode) to handle task: {}", provider, taskCategory);

            String apiKey = resolveApiKey(userId, provider);
            String generatedCode = null;
            
            try {
                generatedCode = callAIProviderWithRetry(provider, apiKey, prompt, cb);
                long timeTaken = System.currentTimeMillis() - startTime;

                recordUsage(provider);

                if (immunitySystem.isCodeInfected(generatedCode)) {
                    log.error("-> [Orchestrator] AI generated toxic/broken code! Rejecting...");
                    aiProfiler.recordPerformance(taskCategory, provider, false, timeTaken);
                    
                    // Capture ecosystem learning for infected code
                    if (enhancedLearningService != null) {
                        Map<String, Object> requestMeta = new HashMap<>();
                        requestMeta.put("taskCategory", taskCategory);
                        requestMeta.put("errorSignature", errorSignature);
                        requestMeta.put("infected", true);
                        
                        enhancedLearningService.learnFromAPIUsage(
                                "generateCode",
                                provider.name(),
                                timeTaken,
                                false,
                                requestMeta
                        ).subscribe(); // Fire and forget
                    }
                    
                    continue;
                }

                knowledgeBase.recordSuccessWithPermission(errorSignature, generatedCode, provider.name(), timeTaken, 0.95);
                aiProfiler.recordPerformance(taskCategory, provider, true, timeTaken);

                // Capture ecosystem learning for successful API call
                if (enhancedLearningService != null) {
                    Map<String, Object> requestMeta = new HashMap<>();
                    requestMeta.put("taskCategory", taskCategory);
                    requestMeta.put("errorSignature", errorSignature);
                    requestMeta.put("codeLength", generatedCode != null ? generatedCode.length() : 0);
                    
                    enhancedLearningService.learnFromAPIUsage(
                            "generateCode",
                            provider.name(),
                            timeTaken,
                            true,
                            requestMeta
                    ).subscribe(); // Fire and forget
                }

                return generatedCode;

            } catch (Exception e) {
                long timeTaken = System.currentTimeMillis() - startTime;
                log.error("Error from provider: {} on task: {}", provider, taskCategory, e);
                aiProfiler.recordPerformance(taskCategory, provider, false, timeTaken);
                
                // Capture ecosystem learning for failed API call
                if (enhancedLearningService != null) {
                    Map<String, Object> requestMeta = new HashMap<>();
                    requestMeta.put("taskCategory", taskCategory);
                    requestMeta.put("errorSignature", errorSignature);
                    requestMeta.put("errorMessage", e.getMessage());
                    
                    enhancedLearningService.learnFromAPIUsage(
                            "generateCode",
                            provider.name(),
                            timeTaken,
                            false,
                            requestMeta
                    ).subscribe(); // Fire and forget
                }
                
                // Continue to next provider ONCE ONLY - no retries per provider
            }
        }

        throw new RuntimeException("CRITICAL: All AI failed. Cannot execute task.");
    }

    private boolean isServiceQuotaAvailable(AIProviderType provider) {
        String serviceName = getServiceNameForProvider(provider);
        if (quotaManager.getQuotaStatus(serviceName, "Requests") == null) return true;
        return quotaManager.getQuotaStatus(serviceName, "Requests").getRemainingQuota() > 0;
    }

    private void recordUsage(AIProviderType provider) {
        String serviceName = getServiceNameForProvider(provider);
        quotaManager.recordUsage(serviceName, "Requests", 1);
    }

    private String getServiceNameForProvider(AIProviderType provider) {
        switch (provider) {
            case GROQ_LLAMA3: return "Groq";
            case GEMINI_PRO: return "Google";
            case ANTHROPIC_CLAUDE: return "Anthropic";
            case HUGGINGFACE_FREE: return "HuggingFace";
            case OPENAI: return "OpenAI";
            case DEEPSEEK: return "DeepSeek";
            case KIMI: return "Kimi";
            case MISTRAL: return "Mistral";
            case AIRLLM: return "AirLLM";
            case OLLAMA: return "Ollama";
            default: throw new IllegalArgumentException("Unknown AI provider: " + provider);
        }
    }

    private String resolveApiKey(String userId, AIProviderType provider) {
        if (userId == null || "system".equals(userId)) {
            return System.getenv(getEnvKeyForProvider(provider));
        }
        Optional<UserApiKey> key = keyRotationService.selectBestKey(userId, getServiceNameForProvider(provider));
        return key.map(k -> keyRotationService.getDecryptedApiKey(k))
                .orElseGet(() -> System.getenv(getEnvKeyForProvider(provider)));
    }

    private String getEnvKeyForProvider(AIProviderType provider) {
        switch (provider) {
            case GROQ_LLAMA3: return "GROQ_API_KEY";
            case GEMINI_PRO: return "GEMINI_API_KEY";
            case ANTHROPIC_CLAUDE: return "ANTHROPIC_API_KEY";
            case HUGGINGFACE_FREE: return "HF_API_KEY";
            case OPENAI: return "OPENAI_API_KEY";
            case DEEPSEEK: return "DEEPSEEK_API_KEY";
            case KIMI: return "KIMI_API_KEY";
            case MISTRAL: return "MISTRAL_API_KEY";
            case AIRLLM: return "AIRLLM_API_KEY";
            case OLLAMA: return null; // Ollama uses local endpoint, no API key needed
            default: return "AI_API_KEY";
        }
    }

    private String callAIProviderWithRetry(AIProviderType provider, String apiKey, String prompt, CircuitBreaker cb) {
        String serviceName = getServiceNameForProvider(provider);
        return retryExecutor.executeWithCircuitBreaker(
                provider.name(), serviceName, cb,
                () -> {
                    try {
                        return callAIProvider(provider, apiKey, prompt);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private String callAIProvider(AIProviderType provider, String apiKey, String prompt) throws Exception {
        String providerName = mapFallbackProviderToFactoryName(provider);
        com.supremeai.provider.AIProvider realProvider = providerFactory.getProvider(providerName, apiKey);
        return realProvider.generate(prompt).block();
    }

    private String mapFallbackProviderToFactoryName(AIProviderType provider) {
        switch (provider) {
            case GROQ_LLAMA3: return "groq";
            case GEMINI_PRO: return "gemini";
            case ANTHROPIC_CLAUDE: return "anthropic";
            case HUGGINGFACE_FREE: return "huggingface";
            case OPENAI: return "openai";
            case DEEPSEEK: return "deepseek";
            case KIMI: return "kimi";
            case MISTRAL: return "mistral";
            case AIRLLM: return "airllm";
            case OLLAMA: return "ollama";
            default: return provider.name().toLowerCase();
        }
    }

    public Map<String, Object> getProviderHealthStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        for (AIProviderType provider : allProviders) {
            CircuitBreaker cb = providerCircuitBreakers.get(provider);
            Map<String, Object> providerStatus = new java.util.HashMap<>();
            if (cb != null) {
                providerStatus.put("state", cb.getState().name());
                providerStatus.put("failureRate", cb.getMetrics().getFailureRate());
                providerStatus.put("slowCallRate", cb.getMetrics().getSlowCallRate());
                providerStatus.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
                providerStatus.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            } else {
                providerStatus.put("state", "UNKNOWN");
            }
            providerStatus.put("quotaAvailable", isServiceQuotaAvailable(provider));
            status.put(provider.name(), providerStatus);
        }
        return status;
    }
}
