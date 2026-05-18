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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
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

    @Autowired(required = false)
    private EnhancedLearningService enhancedLearningService;

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Map<String, CircuitBreaker> providerCircuitBreakers = new ConcurrentHashMap<>();
    private final com.supremeai.repository.ProviderRepository providerRepository;

    public AIFallbackOrchestrator(QuotaManager quotaManager,
                                  GlobalKnowledgeBase knowledgeBase, CodeImmunitySystem immunitySystem,
                                  AIProfiler aiProfiler, RetryableAIExecutor retryExecutor,
                                  ApiKeyRotationService keyRotationService,
                                  AIProviderFactory providerFactory,
                                  com.supremeai.repository.ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
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
        log.info("Initializing Dynamic AI Fallback Orchestrator...");
        providerRepository.findAll()
            .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
            .doOnNext(p -> getOrCreateCircuitBreaker(p.getName().toLowerCase()))
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
                        if ("CHAT".equalsIgnoreCase(taskCategory) || "COMMUNICATION".equalsIgnoreCase(taskCategory)) {
                            return p.isCanCommunicate();
                        } else {
                            return p.isCanExecuteTasks();
                        }
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

                        return tryNextProvider(dynamicChain, 0, taskCategory, errorSignature, prompt, userId);
                    });
            }));
    }

    private Mono<String> tryNextProvider(List<com.supremeai.model.APIProvider> chain, int index,
                                          String taskCategory, String errorSignature, String prompt, String userId) {
        if (index >= chain.size()) {
            return Mono.error(new RuntimeException("CRITICAL: All AI failed. Cannot execute task."));
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
}
