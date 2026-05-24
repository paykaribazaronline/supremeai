package com.supremeai.fallback;

import com.supremeai.cost.QuotaManager;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.learning.immunity.CodeImmunitySystem;
import com.supremeai.intelligence.profiling.AIProfiler;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.provider.AIProvider;
import com.supremeai.resilience.RetryableAIExecutor;
import com.supremeai.security.ApiKeyRotationService;
import com.supremeai.service.EnhancedLearningService;
import com.supremeai.service.RequestHedgingService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private final RequestHedgingService hedgingService;
    private final String fallbackProviderName;

    @Autowired(required = false)
    private EnhancedLearningService enhancedLearningService;

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Map<String, CircuitBreaker> providerCircuitBreakers = new ConcurrentHashMap<>();
    private final com.supremeai.repository.ProviderRepository providerRepository;

    /**
     * {@code true} when zero active AI providers are configured in Firestore.
     * The system is running in solo mode — all requests fall through to
     * core_knowledge.json / autonomous_seed or the airllm-sidecar local model.
     */
    private volatile boolean soloMode = false;

    public AIFallbackOrchestrator(QuotaManager quotaManager,
                                  GlobalKnowledgeBase knowledgeBase, CodeImmunitySystem immunitySystem,
                                  AIProfiler aiProfiler, RetryableAIExecutor retryExecutor,
                                  ApiKeyRotationService keyRotationService,
                                  AIProviderFactory providerFactory,
                                  RequestHedgingService hedgingService,
                                  com.supremeai.repository.ProviderRepository providerRepository,
                                  @Value("${supremeai.solo-mode.fallback-provider:airllm-sidecar}") String fallbackProviderName) {
        this.providerRepository = providerRepository;
        this.fallbackProviderName = fallbackProviderName;
        this.quotaManager = quotaManager;
        this.knowledgeBase = knowledgeBase;
        this.immunitySystem = immunitySystem;
        this.aiProfiler = aiProfiler;
        this.retryExecutor = retryExecutor;
        this.keyRotationService = keyRotationService;
        this.providerFactory = providerFactory;
        this.hedgingService = hedgingService;

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
        log.info("Initializing Dynamic AI Fallback Orchestrator...");
        java.util.concurrent.atomic.AtomicInteger activeCount = new java.util.concurrent.atomic.AtomicInteger(0);
        providerRepository.findAll()
            .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
            .doOnNext(p -> {
                activeCount.incrementAndGet();
                getOrCreateCircuitBreaker(p.getName().toLowerCase());
            })
            .doOnComplete(() -> {
                if (activeCount.get() == 0) {
                    soloMode = true;
                    log.warn("[SOLO MODE] No active AI providers found in Firestore at startup. "
                            + "System is operating in solo mode — all requests will use local knowledge base "
                            + "(core_knowledge.json + autonomous_seed_knowledge.json). "
                            + "Add provider entries to Firestore api_providers to restore full AI capability.");
                } else {
                    log.info("[DYNAMIC PROVIDERS] {} active providers registered at startup", activeCount.get());
                }
            })
            .subscribe();
    }

    private CircuitBreaker getOrCreateCircuitBreaker(String providerId) {
        String name = providerId.toLowerCase();
        return circuitBreakerRegistry.find(name).orElseGet(() -> {
            log.info("Creating new dynamic circuit breaker for: {}", name);
            CircuitBreaker newCb = circuitBreakerRegistry.circuitBreaker(name);
            newCb.getEventPublisher()
                    .onStateTransition(event -> log.info("Dynamic Circuit breaker {} transitioned from {} to {}",
                            name, event.getStateTransition().getFromState(), event.getStateTransition().getToState()));
            return newCb;
        });
    }

    private Mono<String> generateForProvider(String providerId, String serviceName, CircuitBreaker cb,
                                              AIProvider provider, String prompt) {
        return Mono.fromCallable(() ->
                        retryExecutor.executeWithCircuitBreaker(
                                providerId, serviceName, cb,
                                () -> {
                                    try {
                                        return (String) provider.generate(prompt)
                                                .subscribeOn(Schedulers.boundedElastic())
                                                .block(Duration.ofSeconds(60));
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        )
                ).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<String> executeWithSupremeIntelligence(String taskCategory, String errorSignature, String prompt) {
        return executeWithSupremeIntelligence(taskCategory, errorSignature, prompt, "system");
    }

    public Mono<String> executeWithSupremeIntelligence(String taskCategory, String errorSignature, String prompt, String userId) {
        return knowledgeBase.findKnownSolution(errorSignature)
            .switchIfEmpty(Mono.defer(() -> {
                String expertProviderId = aiProfiler.getBestAIForTask(taskCategory);

                return providerRepository.findByStatus("active")
                    .filter(p -> {
                        // Allow all active AI models in all work as requested by the user
                        return true;
                    })
                    .sort(Comparator.comparingInt(com.supremeai.model.APIProvider::getPriority))
                    .collectList()
                    .onErrorResume(e -> {
                        log.error("Failed to load dynamic providers: {}", e.getMessage());
                        return Mono.just(new ArrayList<com.supremeai.model.APIProvider>());
                    })
                    .flatMap(dbProviders -> {
                        List<com.supremeai.model.APIProvider> dynamicChain = new ArrayList<>();

                        // Add expert provider first
                        if (expertProviderId != null) {
                            dbProviders.stream()
                                .filter(p -> p.getName().equalsIgnoreCase(expertProviderId))
                                .findFirst()
                                .ifPresent(dynamicChain::add);
                        }

                        // Add others in priority order
                        for (com.supremeai.model.APIProvider p : dbProviders) {
                            if (expertProviderId == null || !p.getName().equalsIgnoreCase(expertProviderId)) {
                                dynamicChain.add(p);
                            }
                        }

                        // LATENCY OPTIMIZATION: If task is high priority (chat/code), use hedging for the top 2
                        if (taskCategory.equals("chat") || taskCategory.equals("code_generation")) {
                            return executeWithHedging(dynamicChain, taskCategory, errorSignature, prompt, userId);
                        }

                        return tryNextProvider(dynamicChain, 0, taskCategory, errorSignature, prompt, userId);
                    });
            }));
    }

    /**
     * Execute top 2 providers in parallel with a delay (hedging).
     */
    private Mono<String> executeWithHedging(List<com.supremeai.model.APIProvider> chain, String taskCategory, String errorSignature, String prompt, String userId) {
        if (chain.size() < 2) return tryNextProvider(chain, 0, taskCategory, errorSignature, prompt, userId);

        log.info("🚀 Hedging request to top 2 providers for latency optimization...");
        
        java.util.function.Supplier<Mono<String>> primary = () -> tryNextProvider(List.of(chain.get(0)), 0, taskCategory, errorSignature, prompt, userId);
        java.util.function.Supplier<Mono<String>> backup = () -> tryNextProvider(List.of(chain.get(1)), 0, taskCategory, errorSignature, prompt, userId);

        return Mono.fromFuture(hedgingService.executeWithHedgingAsync(List.of(
            () -> primary.get().toFuture(),
            () -> backup.get().toFuture()
        ), 30000L));
    }

    private Mono<String> tryNextProvider(List<com.supremeai.model.APIProvider> chain, int index,
                                          String taskCategory, String errorSignature, String prompt, String userId) {
        if (index >= chain.size()) {
            // ULTIMATE FAILOVER: Try Private Cloud AirLLM Sidecar before giving up
            return tryPrivateCloudFailover(taskCategory, prompt);
        }

        com.supremeai.model.APIProvider dbProvider = chain.get(index);
        String providerId = dbProvider.getName().toLowerCase();
        String serviceName = dbProvider.getType() != null ? dbProvider.getType() : providerId;

        if (!isServiceQuotaAvailable(serviceName)) {
            log.warn("Quota exhausted for {}, skipping.", serviceName);
            return tryNextProvider(chain, index + 1, taskCategory, errorSignature, prompt, userId);
        }

        CircuitBreaker cb = getOrCreateCircuitBreaker(providerId);
        if (cb.getState() == CircuitBreaker.State.OPEN) {
            log.warn("Circuit breaker OPEN for {}, skipping.", providerId);
            return tryNextProvider(chain, index + 1, taskCategory, errorSignature, prompt, userId);
        }

        long startTime = System.currentTimeMillis();
        log.info("-> Asking {} to handle task: {}", providerId, taskCategory);

        AIProvider aiProvider = providerFactory.createProviderFromConfig(dbProvider);
        if (aiProvider == null) {
            log.warn("Could not create provider instance for {}, skipping.", providerId);
            return tryNextProvider(chain, index + 1, taskCategory, errorSignature, prompt, userId);
        }

        return generateForProvider(providerId, serviceName, cb, aiProvider, prompt)
            .flatMap(generatedCode -> {
                long timeTaken = System.currentTimeMillis() - startTime;
                recordUsage(serviceName);

                boolean infected = immunitySystem.isCodeInfected(generatedCode);
                if (infected) {
                    log.error("-> [Orchestrator] AI generated toxic/broken code! Rejecting...");
                    aiProfiler.recordPerformance(taskCategory, providerId, false, timeTaken);
                    return tryNextProvider(chain, index + 1, taskCategory, errorSignature, prompt, userId);
                }

                return knowledgeBase.recordSuccessWithPermission(errorSignature, generatedCode, providerId, timeTaken, 0.95)
                        .then(Mono.fromRunnable(() -> aiProfiler.recordPerformance(taskCategory, providerId, true, timeTaken)))
                        .then(Mono.defer(() -> {
                            if (enhancedLearningService != null) {
                                Map<String, Object> requestMeta = new HashMap<>();
                                requestMeta.put("taskCategory", taskCategory);
                                requestMeta.put("errorSignature", errorSignature);
                                enhancedLearningService.learnFromAPIUsage("generateCode", providerId, timeTaken, true, requestMeta).subscribe();
                            }
                            return Mono.just(generatedCode);
                        }));
            })
            .onErrorResume(e -> {
                long timeTaken = System.currentTimeMillis() - startTime;
                log.error("Error from provider: {} on task: {}", providerId, taskCategory, e);
                aiProfiler.recordPerformance(taskCategory, providerId, false, timeTaken);
                return tryNextProvider(chain, index + 1, taskCategory, errorSignature, prompt, userId);
            });
    }

    /**
     * Final failover to private cluster-local model (Solo Mode).
     * The airllm-sidecar entry must be configured as a regular provider in Firestore api_providers.
     * No endpoint, type, or model is hardcoded here.
     */
    private Mono<String> tryPrivateCloudFailover(String taskCategory, String prompt) {
        log.warn("⚠️ ALL external providers failed. Triggering Private Cloud Failover (Solo Mode)...");

        return Mono.fromCallable(() -> {
            // Look up the fallback provider from the live DB — configuration-driven
            com.supremeai.model.APIProvider airllmConfig = null;
            try {
                airllmConfig = providerRepository.findById(fallbackProviderName)
                        .switchIfEmpty(providerRepository.findById(fallbackProviderName.toLowerCase()))
                        .block(java.time.Duration.ofSeconds(2));
            } catch (Exception e) {
                log.warn("[SoloMode] Fallback provider '{}' not found in DB by id/lowercase. Trying name lookup...", fallbackProviderName);
            }
            if (airllmConfig == null) {
                try {
                    List<com.supremeai.model.APIProvider> sidecars = providerRepository.findAll()
                            .filter(p -> fallbackProviderName.equalsIgnoreCase(p.getName()))
                            .collectList()
                            .block(java.time.Duration.ofSeconds(2));
                    if (sidecars != null && !sidecars.isEmpty()) {
                        airllmConfig = sidecars.get(0);
                    }
                } catch (Exception e) {
                    log.error("[SoloMode] Fallback provider name lookup also failed", e);
                }
            }
            if (airllmConfig == null) {
                throw new RuntimeException(
                    "Solo Mode: " + fallbackProviderName + " not configured in Firestore api_providers. "
                    + "Add a provider entry with id=" + fallbackProviderName + " to enable local model fallback.");
            }
            log.info("[SoloMode] Using fallback provider from DB: baseUrl={}, type={}",
                    airllmConfig.getBaseUrl(), airllmConfig.getType());
            return providerFactory.createProviderFromConfig(airllmConfig);
        })
        .flatMap(provider -> provider.generate(prompt))
        .timeout(Duration.ofSeconds(120))
        .onErrorResume(e -> {
            log.error("Solo Mode local model ALSO failed: {}", e.getMessage());
            return Mono.error(new RuntimeException(
                "CRITICAL: Complete System Blackout. No AI available — solo mode provider failed.", e));
        });
    }

    private boolean isServiceQuotaAvailable(String serviceName) {
        if (quotaManager.getQuotaStatus(serviceName, "Requests") == null) return true;
        return quotaManager.getQuotaStatus(serviceName, "Requests").getRemainingQuota() > 0;
    }

    private void recordUsage(String serviceName) {
        quotaManager.recordUsage(serviceName, "Requests", 1);
    }

    public Map<String, Object> getProviderHealthStatus() {
        Map<String, Object> status = new java.util.HashMap<>();
        providerCircuitBreakers.forEach((providerId, cb) -> {
            Map<String, Object> providerStatus = new java.util.HashMap<>();
            providerStatus.put("state", cb.getState().name());
            providerStatus.put("failureRate", cb.getMetrics().getFailureRate());
            providerStatus.put("slowCallRate", cb.getMetrics().getSlowCallRate());
            providerStatus.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            providerStatus.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            status.put(providerId, providerStatus);
        });
        return status;
    }

    /**
     * Returns {@code true} when the system is operating in solo mode — zero active
     * AI providers are configured in Firestore.  All requests will fall through to
     * the local knowledge base or the airllm-sidecar local model.
     */
    public boolean getSoloMode() {
        return soloMode;
    }
}
