package com.supremeai.fallback;

import com.supremeai.cost.QuotaManager;
import com.supremeai.learning.active.ActiveInternetScraper;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.learning.immunity.CodeImmunitySystem;
import com.supremeai.intelligence.profiling.AIProfiler;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.provider.AIProvider;
import com.supremeai.client.PocketLabClient;
import com.supremeai.service.AIRankingService;
import com.supremeai.provider.StubLocalProvider;
import com.supremeai.resilience.RetryableAIExecutor;
import com.supremeai.security.ApiKeyRotationService;
import com.supremeai.service.EnhancedLearningService;
import com.supremeai.service.RequestHedgingService;
import com.supremeai.service.browser.BrowserService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.supremeai.utils.GitHelper;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AIFallbackOrchestrator {
    public AIFallbackOrchestrator(EnhancedLearningService enhancedLearningService, com.supremeai.learning.SupremeLearningOrchestrator learningOrchestrator) {
        this.enhancedLearningService = enhancedLearningService;
        this.learningOrchestrator = learningOrchestrator;
    }


    private final PocketLabClient pocketLabClient;

    private static final Logger log = LoggerFactory.getLogger(AIFallbackOrchestrator.class);
    private final QuotaManager quotaManager;
    private final GlobalKnowledgeBase knowledgeBase;
    private final CodeImmunitySystem immunitySystem;
    private final AIProfiler aiProfiler;
    private final RetryableAIExecutor retryExecutor;
    private final ApiKeyRotationService keyRotationService;
    private final AIProviderFactory providerFactory;
    private final AIRankingService aiRankingService;
    private final RequestHedgingService hedgingService;
    private final ActiveInternetScraper activeInternetScraper;
    private final BrowserService browserService;
    private final String fallbackProviderName;
    private final StubLocalProvider stubLocalProvider;



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
                                   PocketLabClient pocketLabClient,
                                   GlobalKnowledgeBase knowledgeBase, CodeImmunitySystem immunitySystem,
                                   AIProfiler aiProfiler, RetryableAIExecutor retryExecutor,
                                   ApiKeyRotationService keyRotationService,
                                   AIProviderFactory providerFactory,
                                   AIRankingService aiRankingService,
                                   RequestHedgingService hedgingService,
                                   ActiveInternetScraper activeInternetScraper,
                                   BrowserService browserService,
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
        this.activeInternetScraper = activeInternetScraper;
        this.browserService = browserService;
        this.stubLocalProvider = new StubLocalProvider();
        this.pocketLabClient = pocketLabClient;
        this.aiRankingService = aiRankingService;

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
        AtomicInteger activeCount = new AtomicInteger(0);
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

    private void conditionallyCommitAndPush() {
        try {
            // Only push when we are on main branch, have uncommitted changes, and a dry‑run reports pending pushes
            if (GitHelper.isOnMainBranch() && GitHelper.hasChanges() && GitHelper.dryPushHasPending()) {
                GitHelper.commitAndPushAll();
                log.info("[GitHelper] Conditional commit & push executed.");
            } else {
                log.info("[GitHelper] No conditional push needed (branch={}, changes={}, dryRunPending={}).",
                        GitHelper.isOnMainBranch(), GitHelper.hasChanges(), GitHelper.dryPushHasPending());
            }
        } catch (Exception e) {
            log.error("Background Git sync failed: {}", e.getMessage(), e);
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000)
    public void refreshSoloModeStatus() {
        providerRepository.findAll()
            .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
            .count()
            .subscribe(count -> {
                boolean newSoloMode = (count == 0);
                if (newSoloMode != soloMode) {
                    soloMode = newSoloMode;
                    log.info("[SoloMode] Status changed dynamically to: {}", soloMode ? "SOLO" : "CONNECTED");
                }
            });
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
        Mono<String> operation = provider.generate(prompt)
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(15));
        return retryExecutor.executeWithCircuitBreakerReactive(providerId, serviceName, cb, operation);
    }

    public Mono<String> executeWithSupremeIntelligence(String taskCategory, String errorSignature, String prompt) {
        return executeWithSupremeIntelligence(taskCategory, errorSignature, prompt, "system");
    }

    public Mono<String> executeWithSupremeIntelligence(String taskCategory, String errorSignature, String prompt, String userId) {
        // Tier 2: Check for Pocket Lab (tiny model) routing
        return tryPocketLab(userId, prompt)
                .switchIfEmpty(
                        // Tier 2 fallback: Firestore Memory lookup
                        knowledgeBase.findKnownSolution(errorSignature)
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.info("[BRAIN TIER 3] No solution in Firestore. Attempting web scraping via browser...");
                                    return tryBrowserScraping(taskCategory, prompt);
                                }))
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.info("[BRAIN TIER 4] No web scrape result. Trying Helper AI providers...");
                                    return tryHelperAIProviders(taskCategory, errorSignature, prompt, userId);
                                }))
                );
    }

    private Mono<String> tryPocketLab(String userId, String prompt) {
        log.info("[BRAIN TIER 2] Checking Pocket Lab client for user {}", userId);
        return pocketLabClient.predict(prompt);
    }

    /**
     * Tier 4: Try Helper AI providers (Cloud/Sidecar fallback).
     */
    private Mono<String> tryHelperAIProviders(String taskCategory, String errorSignature, String prompt, String userId) {
        // Retrieve provider rankings and sort active providers accordingly
            return providerRepository.findAll()
                .filter(p -> !fallbackProviderName.equalsIgnoreCase(p.getName()) && !fallbackProviderName.equalsIgnoreCase(p.getType()))
                .collectList()
                .flatMap(activeCloudProviders -> {
                    // Get rankings
                    List<com.supremeai.service.AIRankingService.ProviderRanking> rankings = aiRankingService.getRankings();
                    // Map provider name to ranking value for quick lookup
                    Map<String, Double> rankMap = rankings.stream()
                        .collect(java.util.stream.Collectors.toMap(r -> r.getProvider(), r -> r.getValueScore()));
                    // Sort providers by ranking (higher score first)
                    activeCloudProviders.sort((a, b) -> {
                        double ra = rankMap.getOrDefault(a.getName(), 0.0);
                        double rb = rankMap.getOrDefault(b.getName(), 0.0);
                        return Double.compare(rb, ra);
                    });

                if (activeCloudProviders.isEmpty()) {
                    log.info("[Solo-First] No active cloud AI models configured. Relying purely on local Solo Mode.");
                    return tryPrivateCloudFailover(taskCategory, prompt);
                }

                log.info("[Solo-First] Active cloud models found. Checking connection and taking help opportunistically...");
                
                String expertProviderId = aiProfiler.getBestAIForTask(taskCategory);
                com.supremeai.model.APIProvider cloudProvider = activeCloudProviders.stream()
                    .filter(p -> p.getName().equalsIgnoreCase(expertProviderId))
                    .findFirst()
                    .orElse(activeCloudProviders.get(0));

                AIProvider aiProvider = providerFactory.createProviderFromConfig(cloudProvider);
                if (aiProvider == null) {
                    return tryPrivateCloudFailover(taskCategory, prompt);
                }

                String serviceName = cloudProvider.getType() != null ? cloudProvider.getType() : cloudProvider.getName();
                CircuitBreaker cb = getOrCreateCircuitBreaker(cloudProvider.getName().toLowerCase());

                if (!isServiceQuotaAvailable(serviceName) || cb.getState() == CircuitBreaker.State.OPEN) {
                    log.info("[Solo-First] Cloud provider {} quota exhausted or circuit open. Bypassing cloud assist.", cloudProvider.getName());
                    return tryPrivateCloudFailover(taskCategory, prompt);
                }

                return generateForProvider(cloudProvider.getName(), serviceName, cb, aiProvider, prompt)
                    .timeout(Duration.ofSeconds(8))
                    .flatMap(cloudCode -> {
                        boolean infected = immunitySystem.isCodeInfected(cloudCode);
                        if (infected) {
                            return Mono.error(new RuntimeException("Cloud code infected"));
                        }
                        log.info("[Solo-First] Cloud assistant successfully provided help!");
                        return Mono.just(cloudCode);
                    })
                    .onErrorResume(err -> {
                        log.warn("[Solo-First] Cloud connection not available or timeout: {}. Continuing purely with Solo Mode.", err.getMessage());
                        return tryPrivateCloudFailover(taskCategory, prompt);
                    });
            });
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

                return coReasonWithLocalModel(taskCategory, prompt, generatedCode)
                    .flatMap(optimizedCode -> {
                        boolean infected = immunitySystem.isCodeInfected(optimizedCode);
                        if (infected) {
                            log.error("-> [Orchestrator] Optimized code is toxic! Falling back to original...");
                            return Mono.just(generatedCode);
                        }
                        return Mono.just(optimizedCode);
                    })
                    .flatMap(finalCode -> {
                        return knowledgeBase.recordSuccessWithPermission(errorSignature, finalCode, providerId, timeTaken, 0.95)
                                .then(Mono.fromRunnable(() -> aiProfiler.recordPerformance(taskCategory, providerId, true, timeTaken)))
                                .then(Mono.defer(() -> {
                                    if (enhancedLearningService != null) {
                                        Map<String, Object> requestMeta = new HashMap<>();
                                        requestMeta.put("taskCategory", taskCategory);
                                        requestMeta.put("errorSignature", errorSignature);
                                        enhancedLearningService.learnFromAPIUsage("generateCode", providerId, timeTaken, true, requestMeta).subscribe();
                                    }
                                    return Mono.just(finalCode);
                                }));
                    });
            })
            .onErrorResume(e -> {
                long timeTaken = System.currentTimeMillis() - startTime;
                log.error("Error from provider: {} on task: {}", providerId, taskCategory, e);
                aiProfiler.recordPerformance(taskCategory, providerId, false, timeTaken);
                return tryNextProvider(chain, index + 1, taskCategory, errorSignature, prompt, userId);
            });
    }

    /**
     * Tier 3: Attempt to scrape web knowledge via browser automation.
     */
    private Mono<String> tryBrowserScraping(String taskCategory, String prompt) {
        log.info("[BRAIN TIER 3] Attempting web scraping for task: {}", taskCategory);
        
        return Mono.fromCallable(() -> {
            String cleanQuery = prompt.toLowerCase()
                    .replaceAll("[\\p{Punct}]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            
            String keywords = String.join(" ", cleanQuery.split("\\s+"));
            return keywords;
        })
        .flatMap(keywords -> {
            // Use ActiveInternetScraper to scrape knowledge
            return activeInternetScraper.scrapeKnowledge(taskCategory, List.of(keywords.split("\\s+")))
                    .next()
                    .map(issue -> {
                        log.info("[BRAIN TIER 3] Web scraping found result from: {}", issue.getSource());
                        return issue.getSolution();
                    });
        })
        .timeout(Duration.ofSeconds(10))
        .onErrorResume(err -> {
            log.warn("[BRAIN TIER 3] Web scraping failed or timed out: {}", err.getMessage());
            return Mono.empty();
        });
    }

    /**
     * Final failover to private cluster-local model (Solo Mode).
     * The airllm-sidecar entry must be configured as a regular provider in Firestore api_providers.
     * No endpoint, type, or model is hardcoded here.
     */
    private Mono<String> tryPrivateCloudFailover(String taskCategory, String prompt) {
        log.warn("ALL external providers failed. Triggering Solo Mode (fully offline)...");
        recordIntelligenceGap(taskCategory, prompt, "ALL_EXTERNAL_PROVIDERS_FAILED");

        Mono<com.supremeai.model.APIProvider> lookupById = providerRepository.findById(fallbackProviderName)
                .switchIfEmpty(providerRepository.findById(fallbackProviderName.toLowerCase()))
                .onErrorResume(e -> {
                    log.warn("[SoloMode] Fallback provider '{}' lookup by id/lowercase failed: {}", fallbackProviderName, e.getMessage());
                    return Mono.empty();
                });

        Mono<com.supremeai.model.APIProvider> lookupByName = providerRepository.findAll()
                .filter(p -> fallbackProviderName.equalsIgnoreCase(p.getName()))
                .next()
                .onErrorResume(e -> {
                    log.error("[SoloMode] Fallback provider name lookup failed", e);
                    return Mono.empty();
                });

        return lookupById
                .switchIfEmpty(lookupByName)
                .flatMap(airllmConfig -> {
                    log.info("[SoloMode] Using fallback provider: baseUrl={}, type={}",
                            airllmConfig.getBaseUrl(), airllmConfig.getType());
                    AIProvider provider = providerFactory.createProviderFromConfig(airllmConfig);
                    if (provider != null) {
                        return provider.generate(prompt);
                    }
                    log.info("[SoloMode] Failed to create provider from config. Using StubLocalProvider.");
                    return stubLocalProvider.generate(prompt);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("[SoloMode] No local sidecar found in DB. Using StubLocalProvider for fully offline operation.");
                    return stubLocalProvider.generate(prompt);
                }))
                .timeout(Duration.ofSeconds(120))
                .onErrorResume(e -> {
                    log.error("Local model unavailable: {}", e.getMessage());
                    // FALLBACK: Use stub provider instead of throwing critical error
                    if (stubLocalProvider != null) {
                        return stubLocalProvider.generate("Offline fallback: " + prompt);
                    }
                    return Mono.just("আমি লোকাল মোডে সক্রিয়। কোনো বাইরের API কী দরকার পড়ে না।");
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
     * Live Co-Reasoning Layer: The local AirLLM sidecar acts as an active
     * quality auditor, code optimiser, and local reasoning co-pilot.
     * It runs alongside active cloud providers to polish and verify their outputs.
     */
    private Mono<String> coReasonWithLocalModel(String taskCategory, String prompt, String originalOutput) {
        // Only run co-reasoning for code and complex logic tasks to save latency
        if (!"code_generation".equalsIgnoreCase(taskCategory) && !"error_fixing".equalsIgnoreCase(taskCategory)) {
            return Mono.just(originalOutput);
        }

        log.info("[Live Co-Reasoning] Invoking local GGUF model to audit and optimize cloud AI response...");

        String auditPrompt = "You are SupremeAI Co-Pilot (Live local intelligence layer).\n" +
                "Audit the following generated code based on the user prompt.\n" +
                "Fix any obvious syntax errors, optimize it, and output ONLY the clean, ready-to-run updated code without markdown wrapper formatting.\n\n" +
                "User prompt: " + prompt + "\n\n" +
                "Original Code:\n" + originalOutput;

        return providerRepository.findById(fallbackProviderName)
            .switchIfEmpty(providerRepository.findById(fallbackProviderName.toLowerCase()))
            .timeout(Duration.ofSeconds(1))
            .onErrorResume(e -> {
                // Fallback to default config if DB lookup fails
                com.supremeai.model.APIProvider defaultConfig = new com.supremeai.model.APIProvider();
                defaultConfig.setName(fallbackProviderName);
                defaultConfig.setBaseUrl("http://localhost:8081");
                defaultConfig.setType("airllm-sidecar");
                defaultConfig.setStatus("active");
                defaultConfig.setModelName("airllm-sidecar");
                return Mono.just(defaultConfig);
            })
            .switchIfEmpty(Mono.fromCallable(() -> {
                com.supremeai.model.APIProvider defaultConfig = new com.supremeai.model.APIProvider();
                defaultConfig.setName(fallbackProviderName);
                defaultConfig.setBaseUrl("http://localhost:8081");
                defaultConfig.setType("airllm-sidecar");
                defaultConfig.setStatus("active");
                defaultConfig.setModelName("airllm-sidecar");
                return defaultConfig;
            }))
            .map(config -> providerFactory.createProviderFromConfig(config))
        .flatMap(provider -> provider.generate(auditPrompt))
        .timeout(Duration.ofSeconds(15)) // Strict 15s timeout to avoid dragging cloud response times
        .onErrorResume(e -> {
            log.warn("[Live Co-Reasoning] Local co-reasoning timed out or failed; returning original cloud response: {}", e.getMessage());
            return Mono.just(originalOutput);
        })
        .map(optimized -> {
            if (optimized == null || optimized.isBlank() || optimized.contains("Offline Mode Fallback response") || optimized.contains("System is currently downloading")) {
                return originalOutput; // Fallback to original if sidecar returned a template stub
            }
            log.info("[Live Co-Reasoning] Cloud response successfully optimized and polished by local model!");
            return optimized;
        });
    }

    /**
     * Returns {@code true} when the system is operating in solo mode — zero active
     * AI providers are configured in Firestore.  All requests will fall through to
     * the local knowledge base or the airllm-sidecar local model.
     */
    public boolean getSoloMode() {
        return soloMode;
    }

    private void recordIntelligenceGap(String taskCategory, String prompt, String failReason) {
        log.warn("[Intelligence Gap] Recorded for future training. Task Category: {}, Reason: {}", taskCategory, failReason);
        if (learningOrchestrator != null) {
            try {
                learningOrchestrator.detectIntelligenceGap(taskCategory);
            } catch (Exception e) {
                log.warn("Failed to notify learningOrchestrator of gap: {}", e.getMessage());
            }
        }
    }
}
