package com.supremeai.fallback;

import com.supremeai.cost.QuotaManager;
import com.supremeai.intelligence.profiling.AIProfiler;
import com.supremeai.learning.immunity.CodeImmunitySystem;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.provider.StubLocalProvider;
import com.supremeai.resilience.RetryableAIExecutor;
import com.supremeai.security.ApiKeyRotationService;
import com.supremeai.service.EnhancedLearningService;
import com.supremeai.service.RequestHedgingService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ThirdOpinionOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(ThirdOpinionOrchestrator.class);
  private final QuotaManager quotaManager;
  private final GlobalKnowledgeBase knowledgeBase;
  private final CodeImmunitySystem immunitySystem;
  private final AIProfiler aiProfiler;
  private final RetryableAIExecutor retryExecutor;
  private final ApiKeyRotationService keyRotationService;
  private final AIProviderFactory providerFactory;
  private final RequestHedgingService hedgingService;
  private final String thirdOpinionProviderName;

  @Autowired(required = false)
  private EnhancedLearningService enhancedLearningService;

  @Autowired private StubLocalProvider stubLocalProvider;

  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final Map<String, CircuitBreaker> providerCircuitBreakers = new ConcurrentHashMap<>();
  private final com.supremeai.repository.ProviderRepository providerRepository;

  private volatile boolean soloMode = false;

  public ThirdOpinionOrchestrator(
      QuotaManager quotaManager,
      GlobalKnowledgeBase knowledgeBase,
      CodeImmunitySystem immunitySystem,
      AIProfiler aiProfiler,
      RetryableAIExecutor retryExecutor,
      ApiKeyRotationService keyRotationService,
      AIProviderFactory providerFactory,
      RequestHedgingService hedgingService,
      com.supremeai.repository.ProviderRepository providerRepository,
      @Value("${supremeai.solo-mode.third-opinion-provider:airllm-sidecar}")
          String thirdOpinionProviderName) {
    this.providerRepository = providerRepository;
    this.thirdOpinionProviderName = thirdOpinionProviderName;
    this.quotaManager = quotaManager;
    this.knowledgeBase = knowledgeBase;
    this.immunitySystem = immunitySystem;
    this.aiProfiler = aiProfiler;
    this.retryExecutor = retryExecutor;
    this.keyRotationService = keyRotationService;
    this.providerFactory = providerFactory;
    this.hedgingService = hedgingService;

    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();

    this.circuitBreakerRegistry = CircuitBreakerRegistry.of(config);
  }

  @PostConstruct
  public void init() {
    log.info("Initializing Third-Opinion Orchestrator (local-first semantics)...");
    java.util.concurrent.atomic.AtomicInteger activeCount =
        new java.util.concurrent.atomic.AtomicInteger(0);
    providerRepository
        .findAll()
        .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
        .doOnNext(
            p -> {
              activeCount.incrementAndGet();
              getOrCreateCircuitBreaker(p.getName().toLowerCase());
            })
        .doOnComplete(
            () -> {
              if (activeCount.get() == 0) {
                soloMode = true;
                log.warn(
                    "[SOLO MODE] No active third-party AI providers found in Firestore at startup. "
                        + "System is operating with CoreKnowledge + Browser as primary capability. "
                        + "Third-party AI may be enabled from the Admin dashboard as a third-opinion helper.");
              } else {
                log.info(
                    "[DYNAMIC PROVIDERS] {} active third-party providers registered at startup",
                    activeCount.get());
              }
            })
        .subscribe();
  }

  private CircuitBreaker getOrCreateCircuitBreaker(String providerId) {
    String name = providerId.toLowerCase();
    return circuitBreakerRegistry
        .find(name)
        .orElseGet(
            () -> {
              log.info("Creating new dynamic circuit breaker for: {}", name);
              CircuitBreaker newCb = circuitBreakerRegistry.circuitBreaker(name);
              newCb
                  .getEventPublisher()
                  .onStateTransition(
                      event ->
                          log.info(
                              "Dynamic Circuit breaker {} transitioned from {} to {}",
                              name,
                              event.getStateTransition().getFromState(),
                              event.getStateTransition().getToState()));
              return newCb;
            });
  }

  private Mono<String> generateForProvider(
      String providerId,
      String serviceName,
      CircuitBreaker cb,
      AIProvider provider,
      String prompt) {
    return provider
        .generate(prompt)
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(Duration.ofSeconds(15));
  }

  public Mono<String> executeWithSupremeIntelligence(
      String taskCategory, String errorSignature, String prompt) {
    return executeWithSupremeIntelligence(taskCategory, errorSignature, prompt, "system");
  }

  public Mono<String> executeWithSupremeIntelligence(
      String taskCategory, String errorSignature, String prompt, String userId) {
    return knowledgeBase
        .findKnownSolution(errorSignature)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[Local-First] System running in Local-First mode. CoreKnowledge + Browser are primary.");

                  Mono<String> soloExecution = tryLocalThirdOpinionProvider(taskCategory, prompt);

                  return providerRepository
                      .findByStatus("active")
                      .filter(
                          p ->
                              !thirdOpinionProviderName.equalsIgnoreCase(p.getName())
                                  && !thirdOpinionProviderName.equalsIgnoreCase(p.getType()))
                      .collectList()
                      .onErrorResume(
                          e -> {
                            log.warn(
                                "[Local-First] Failed to load dynamic providers: {}. Operating purely offline.",
                                e.getMessage());
                            return Mono.just(new ArrayList<com.supremeai.model.APIProvider>());
                          })
                      .flatMap(
                          activeCloudProviders -> {
                            if (activeCloudProviders.isEmpty()) {
                              log.info(
                                  "[Local-First] No active third-party cloud AI models configured. Relying purely on CoreKnowledge + Browser.");
                              return soloExecution;
                            }

                            log.info(
                                "[Local-First] Active third-party models found. They will be consulted only as optional third-opinions...");

                            String expertProviderId = aiProfiler.getBestAIForTask(taskCategory);
                            com.supremeai.model.APIProvider cloudProvider =
                                activeCloudProviders.stream()
                                    .filter(p -> p.getName().equalsIgnoreCase(expertProviderId))
                                    .findFirst()
                                    .orElse(activeCloudProviders.get(0));

                            AIProvider aiProvider =
                                providerFactory.createProviderFromConfig(cloudProvider);
                            if (aiProvider == null) {
                              return soloExecution;
                            }

                            String serviceName =
                                cloudProvider.getType() != null
                                    ? cloudProvider.getType()
                                    : cloudProvider.getName();
                            CircuitBreaker cb =
                                getOrCreateCircuitBreaker(cloudProvider.getName().toLowerCase());

                            if (!isServiceQuotaAvailable(serviceName)
                                || cb.getState() == CircuitBreaker.State.OPEN) {
                              log.info(
                                  "[Local-First] Third-party provider {} quota exhausted or circuit open. Skipping third-opinion.",
                                  cloudProvider.getName());
                              return soloExecution;
                            }

                            return generateForProvider(
                                    cloudProvider.getName(), serviceName, cb, aiProvider, prompt)
                                .timeout(Duration.ofSeconds(8))
                                .flatMap(
                                    cloudCode -> {
                                      boolean infected = immunitySystem.isCodeInfected(cloudCode);
                                      if (infected) {
                                        return Mono.error(
                                            new RuntimeException("Third-party code infected"));
                                      }
                                      log.info(
                                          "[Local-First] Third-party assistant provided a third-opinion successfully.");
                                      return Mono.just(cloudCode);
                                    })
                                .onErrorResume(
                                    err -> {
                                      log.warn(
                                          "[Local-First] Third-party connection not available or timeout: {}. Continuing with CoreKnowledge + Browser.",
                                          err.getMessage());
                                      return Mono.empty();
                                    })
                                .switchIfEmpty(soloExecution);
                          })
                      .onErrorResume(
                          e -> {
                            log.warn(
                                "[Local-First] General error in Local-First pipeline: {}. Running purely offline.",
                                e.getMessage());
                            return soloExecution;
                          });
                }));
  }

  private Mono<String> executeWithHedging(
      List<com.supremeai.model.APIProvider> chain,
      String taskCategory,
      String errorSignature,
      String prompt,
      String userId) {
    if (chain.size() < 2)
      return tryNextProvider(chain, 0, taskCategory, errorSignature, prompt, userId);

    log.info("🚀 Hedging request to top 2 providers for latency optimization...");
    java.util.function.Supplier<Mono<String>> primary =
        () ->
            tryNextProvider(List.of(chain.get(0)), 0, taskCategory, errorSignature, prompt, userId);
    java.util.function.Supplier<Mono<String>> backup =
        () ->
            tryNextProvider(List.of(chain.get(1)), 0, taskCategory, errorSignature, prompt, userId);

    return Mono.fromFuture(
        hedgingService.executeWithHedgingAsync(
            List.of(() -> primary.get().toFuture(), () -> backup.get().toFuture()), 30000L));
  }

  private Mono<String> tryNextProvider(
      List<com.supremeai.model.APIProvider> chain,
      int index,
      String taskCategory,
      String errorSignature,
      String prompt,
      String userId) {
    if (index >= chain.size()) {
      return tryLocalThirdOpinionProvider(taskCategory, prompt);
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
        .flatMap(
            generatedCode -> {
              long timeTaken = System.currentTimeMillis() - startTime;
              recordUsage(serviceName);

              return coReasonWithLocalModel(taskCategory, prompt, generatedCode)
                  .flatMap(
                      optimizedCode -> {
                        boolean infected = immunitySystem.isCodeInfected(optimizedCode);
                        if (infected) {
                          log.error(
                              "-> [Orchestrator] Optimized code is toxic! Skipping third-opinion optimization and returning original...");
                          return Mono.just(generatedCode);
                        }
                        return Mono.just(optimizedCode);
                      })
                  .flatMap(
                      finalCode -> {
                        return knowledgeBase
                            .recordSuccessWithPermission(
                                errorSignature, finalCode, providerId, timeTaken, 0.95)
                            .then(
                                Mono.fromRunnable(
                                    () ->
                                        aiProfiler.recordPerformance(
                                            taskCategory, providerId, true, timeTaken)))
                            .then(
                                Mono.defer(
                                    () -> {
                                      if (enhancedLearningService != null) {
                                        Map<String, Object> requestMeta = new HashMap<>();
                                        requestMeta.put("taskCategory", taskCategory);
                                        requestMeta.put("errorSignature", errorSignature);
                                        enhancedLearningService
                                            .learnFromAPIUsage(
                                                "generateCode",
                                                providerId,
                                                timeTaken,
                                                true,
                                                requestMeta)
                                            .subscribe();
                                      }
                                      return Mono.just(finalCode);
                                    }));
                      });
            })
        .onErrorResume(
            e -> {
              long timeTaken = System.currentTimeMillis() - startTime;
              log.error("Error from provider: {} on task: {}", providerId, taskCategory, e);
              aiProfiler.recordPerformance(taskCategory, providerId, false, timeTaken);
              return tryNextProvider(
                  chain, index + 1, taskCategory, errorSignature, prompt, userId);
            });
  }

  private Mono<String> tryLocalThirdOpinionProvider(String taskCategory, String prompt) {
    log.warn(
        "⚠️ All consulted third-party providers failed or were skipped. Using local mode (fully offline)...");

    return Mono.fromCallable(
            () -> {
              // Use stub provider for fully offline operation
              log.info("[Local-First] Using StubLocalProvider for offline operation.");
              return stubLocalProvider;
            })
        .flatMap(provider -> provider.generate(prompt))
        .timeout(Duration.ofSeconds(120))
        .onErrorResume(
            e -> {
              log.error("Local provider failed: {}", e.getMessage());
              // FALLBACK: Return static response instead of error
              return Mono.just(
                  "🤖 **SupremeAI লোকাল-ফার্স্ট মোড সক্রিয়**\n\n"
                      + "কোনো বাইরের AI API কী ছাড়াই আমি আপনার সাহায্য করছি।\n\n"
                      + "আপনার প্রশ্ন: \""
                      + prompt
                      + "\"\n\n"
                      + "এই মুহূর্তে আমি লোকাল কোড-বেসড রুলস ব্যবহার করছি। আরও নির্দিষ্ট কিছু জানালে আমি আরও ভালো সাহায্য করতে পারব।");
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
    providerCircuitBreakers.forEach(
        (providerId, cb) -> {
          Map<String, Object> providerStatus = new java.util.HashMap<>();
          providerStatus.put("state", cb.getState().name());
          providerStatus.put("failureRate", cb.getMetrics().getFailureRate());
          providerStatus.put("slowCallRate", cb.getMetrics().getSlowCallRate());
          providerStatus.put(
              "numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
          providerStatus.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
          status.put(providerId, providerStatus);
        });
    return status;
  }

  private Mono<String> coReasonWithLocalModel(
      String taskCategory, String prompt, String originalOutput) {
    if (!"code_generation".equalsIgnoreCase(taskCategory)
        && !"error_fixing".equalsIgnoreCase(taskCategory)) {
      return Mono.just(originalOutput);
    }

    log.info(
        "[Live Co-Reasoning] Invoking local model to audit and optimize third-party AI response...");

    String auditPrompt =
        "You are SupremeAI Co-Pilot (Live local intelligence layer).\n"
            + "Audit the following generated code based on the user prompt.\n"
            + "Fix any obvious syntax errors, optimize it, and output ONLY the clean, ready-to-run updated code without markdown wrapper formatting.\n\n"
            + "User prompt: "
            + prompt
            + "\n\n"
            + "Original Code:\n"
            + originalOutput;

    return Mono.fromCallable(
            () -> {
              com.supremeai.model.APIProvider localConfig = null;
              try {
                localConfig =
                    providerRepository
                        .findById(thirdOpinionProviderName)
                        .switchIfEmpty(
                            providerRepository.findById(thirdOpinionProviderName.toLowerCase()))
                        .block(java.time.Duration.ofSeconds(1));
              } catch (Exception e) {
              }
              if (localConfig == null) {
                localConfig = new com.supremeai.model.APIProvider();
                localConfig.setName(thirdOpinionProviderName);
                localConfig.setBaseUrl("http://localhost:8081");
                localConfig.setType("airllm-sidecar");
                localConfig.setStatus("active");
                localConfig.setModelName("airllm-sidecar");
              }
              return providerFactory.createProviderFromConfig(localConfig);
            })
        .flatMap(provider -> provider.generate(auditPrompt))
        .timeout(Duration.ofSeconds(15))
        .onErrorResume(
            e -> {
              log.warn(
                  "[Live Co-Reasoning] Local co-reasoning timed out or failed; returning original third-party response: {}",
                  e.getMessage());
              return Mono.just(originalOutput);
            })
        .map(
            optimized -> {
              if (optimized == null
                  || optimized.isBlank()
                  || optimized.contains("Offline Mode Fallback response")
                  || optimized.contains("System is currently downloading")) {
                return originalOutput;
              }
              log.info(
                  "[Live Co-Reasoning] Third-party response successfully optimized and polished by local model!");
              return optimized;
            });
  }

  public boolean getSoloMode() {
    return soloMode;
  }
}
