package com.supremeai.fallback;

import com.supremeai.cost.QuotaManager;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.learning.immunity.CodeImmunitySystem;
import com.supremeai.intelligence.profiling.AIProfiler;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.resilience.RetryableAIExecutor;
import com.supremeai.security.ApiKeyRotationService;
import com.supremeai.model.UserApiKey;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
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

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Map<AIProvider, CircuitBreaker> providerCircuitBreakers = new EnumMap<>(AIProvider.class);

    private final List<AIProvider> allProviders = Arrays.asList(
            AIProvider.GROQ_LLAMA3,
            AIProvider.GEMINI_PRO,
            AIProvider.HUGGINGFACE_FREE,
            AIProvider.ANTHROPIC_CLAUDE
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
        for (AIProvider provider : allProviders) {
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
        AIProvider expertProvider = aiProfiler.getBestAIForTask(taskCategory);

        List<AIProvider> dynamicChain = new ArrayList<>();
        dynamicChain.add(expertProvider);

        for (AIProvider p : allProviders) {
            if (p != expertProvider) dynamicChain.add(p);
        }

        // STEP 3: EXECUTE WITH CIRCUIT BREAKER + RETRY
        for (AIProvider provider : dynamicChain) {
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

            try {
                log.info("-> Asking {} (Expert Mode) to handle task: {}", provider, taskCategory);

                String apiKey = resolveApiKey(userId, provider);
                String generatedCode = callAIProviderWithRetry(provider, apiKey, prompt, cb);
                long timeTaken = System.currentTimeMillis() - startTime;

                recordUsage(provider);

                if (immunitySystem.isCodeInfected(generatedCode)) {
                    log.error("-> [Orchestrator] AI generated toxic/broken code! Rejecting...");
                    aiProfiler.recordPerformance(taskCategory, provider, false, timeTaken);
                    continue;
                }

                knowledgeBase.recordSuccessWithPermission(errorSignature, generatedCode, provider.name(), timeTaken, 0.95);
                aiProfiler.recordPerformance(taskCategory, provider, true, timeTaken);

                return generatedCode;

            } catch (Exception e) {
                log.error("Error from provider: {} on task: {}", provider, taskCategory, e);
                // Continue to next provider
            }
        }

        throw new RuntimeException("CRITICAL: All AI failed. Cannot execute task.");
    }

    private boolean isServiceQuotaAvailable(AIProvider provider) {
        String serviceName = getServiceNameForProvider(provider);
        if (quotaManager.getQuotaStatus(serviceName, "Requests") == null) return true;
        return quotaManager.getQuotaStatus(serviceName, "Requests").getRemainingQuota() > 0;
    }

    private void recordUsage(AIProvider provider) {
        String serviceName = getServiceNameForProvider(provider);
        quotaManager.recordUsage(serviceName, "Requests", 1);
    }

    private String getServiceNameForProvider(AIProvider provider) {
        switch (provider) {
            case GROQ_LLAMA3: return "Groq";
            case GEMINI_PRO: return "Google";
            case ANTHROPIC_CLAUDE: return "Anthropic";
            case HUGGINGFACE_FREE: return "HuggingFace";
            default: return "Unknown";
        }
    }

    private String resolveApiKey(String userId, AIProvider provider) {
        if (userId == null || "system".equals(userId)) {
            return System.getenv(getEnvKeyForProvider(provider));
        }
        Optional<UserApiKey> key = keyRotationService.selectBestKey(userId, getServiceNameForProvider(provider));
        return key.map(UserApiKey::getApiKey)
                .orElseGet(() -> System.getenv(getEnvKeyForProvider(provider)));
    }

    private String getEnvKeyForProvider(AIProvider provider) {
        switch (provider) {
            case GROQ_LLAMA3: return "GROQ_API_KEY";
            case GEMINI_PRO: return "GOOGLE_AI_API_KEY";
            case ANTHROPIC_CLAUDE: return "ANTHROPIC_API_KEY";
            case HUGGINGFACE_FREE: return "HF_API_KEY";
            default: return "AI_API_KEY";
        }
    }

    private String callAIProviderWithRetry(AIProvider provider, String apiKey, String prompt, CircuitBreaker cb) {
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

    private String callAIProvider(AIProvider provider, String apiKey, String prompt) throws Exception {
        String providerName = mapFallbackProviderToFactoryName(provider);
        com.supremeai.provider.AIProvider realProvider = providerFactory.getProvider(providerName, apiKey);
        return realProvider.generate(prompt);
    }

    private String mapFallbackProviderToFactoryName(AIProvider provider) {
        switch (provider) {
            case GROQ_LLAMA3: return "groq";
            case GEMINI_PRO: return "gemini";
            case ANTHROPIC_CLAUDE: return "anthropic";
            case HUGGINGFACE_FREE: return "huggingface";
            default: return provider.name().toLowerCase();
        }
    }

    public Map<String, Object> getProviderHealthStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        for (AIProvider provider : allProviders) {
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
