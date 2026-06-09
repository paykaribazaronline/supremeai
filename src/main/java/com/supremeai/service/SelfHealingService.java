package com.supremeai.service;

import com.supremeai.fallback.ThirdOpinionOrchestrator;
import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.model.APIHealthReport;
import com.supremeai.model.ProviderStatus;
import com.supremeai.model.SupremeAIResponse;
import com.supremeai.model.UserContext;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.APIHealthReportRepository;
import com.supremeai.repository.ProviderRepository;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Provider health-check and self-healing service. */
@Service
public class SelfHealingService {

  @Autowired private ProviderRepository providerRepository;

  @Autowired private AIProviderFactory providerFactory;

  @Autowired private APIHealthReportRepository healthReportRepository;

  @Autowired private GlobalKnowledgeBase globalKnowledgeBase;

  @Autowired private ConfigService configService;

  @Autowired private RootCauseAnalysisService rootCauseAnalysisService;

  @Autowired(required = false)
  private SupremeLearningOrchestrator learningOrchestrator;

  @Autowired private ThirdOpinionOrchestrator fallbackOrchestrator;

  @Autowired private MultiAIVotingService votingService;

  @Autowired private com.supremeai.repository.HealingEventRepository healingEventRepository;

  @Autowired(required = false)
  private AIReasoningService reasoningService;

  private static final Logger log = LoggerFactory.getLogger(SelfHealingService.class);

  // ── Circuit-breaker / quarantine state ──────────────────────────────
  /** Provider name → epoch-millis when quarantine expires (0 = not quarantined) */
  private final Map<String, Long> quarantineUntil = new ConcurrentHashMap<>();

  /** Provider name → list of failure timestamps (epoch-millis) within the sliding window */
  private final Map<String, java.util.List<Long>> failureTimestamps = new ConcurrentHashMap<>();

  @Value("${supremeai.quarantine.threshold:3}") // ব্যর্থতার সীমা এখন কনফিগারেশন থেকে আসবে।
  private int quarantineFailureThreshold;

  @Value("${supremeai.quarantine.window-ms:300000}") // কোয়ারেন্টাইন উইন্ডো (৫ মিনিট ডিফল্ট)।
  private long quarantineWindowMs;

  @Value("${supremeai.quarantine.duration-ms:300000}") // কোয়ারেন্টাইনের মেয়াদ (৫ মিনিট ডিফল্ট)।
  private long quarantineDurationMs;

  /** Domain → epoch-millis when quarantine expires */
  private final Map<String, Long> domainQuarantineUntil = new ConcurrentHashMap<>();

  /** Domain → list of failure timestamps */
  private final Map<String, java.util.List<Long>>
      domainFailureTimestamps = // ডোমেইন ভিত্তিক ব্যর্থতা ট্র্যাকিং।
      new ConcurrentHashMap<>();

  /** Managed container for fire-and-forget reactive subscriptions to prevent leaks. */
  private final Disposable.Composite disposables = reactor.core.Disposables.composite();

  @PreDestroy
  public void
      dispose() { // রিসোর্স মুক্ত করার জন্য, বিশেষ করে রিয়াক্টিভ সাবস্ক্রিপশনগুলো বন্ধ করতে।
    // এই মেথডটি অ্যাপ্লিকেশন বন্ধ হওয়ার আগে কল করা হয় যাতে কোনো মেমরি লিক না হয়।
    // এটি নিশ্চিত করে যে সমস্ত ফায়ার-এন্ড-ফরগেট অপারেশন সঠিকভাবে বন্ধ হয়েছে।
    disposables.dispose();
  }

  private final Map<String, Integer> errorPatterns = new ConcurrentHashMap<>();
  private final int MAX_ITERATIONS = 5;

  @Value(
      "${supremeai.self-healing.max-iterations:5}") // কনফিগারেশন থেকে সর্বোচ্চ ইটারেশন সংখ্যা লোড
  // করা হবে। ডিফল্ট মান 5।
  private int maxIterations; // স্ব-আরোগ্যকরণ লুপের জন্য সর্বোচ্চ ইটারেশন সংখ্যা।

  /** Auto-check all providers frequently. Optimized frequency for high-reliability AI systems. */
  // উচ্চ-নির্ভরযোগ্য AI সিস্টেমের জন্য অপ্টিমাইজ করা ফ্রিকোয়েন্সি সহ সমস্ত প্রদানকারীকে
  // স্বয়ংক্রিয়ভাবে পরীক্ষা করুন।
  @Scheduled(
      fixedRateString =
          "${supremeai.healthcheck.rate:600000}") // হেলথ চেক রেট এখন ডাইনামিকালি কনফিগার করা যাবে।
  public void scheduledHealthCheck() {
    log.info("[HEALTH-CHECK] Scheduled run started");
    cleanupStateMaps();
    disposables.add(
        runProactiveHealthCheck()
            .subscribe(
                report ->
                    log.info(
                        "[HEALTH-CHECK] Scheduled run finished: {} active, {} inactive of {}",
                        report.getActiveCount(),
                        report.getDeadCount(),
                        report.getTotalCount()),
                err -> log.error("[HEALTH-CHECK] Scheduled run failed: {}", err.getMessage())));
  }

  /**
   * Ping every provider, update its status, and produce a health report. Provider records come from
   * Firestore (APIProvider); actual AI calls go through AIProviderFactory (AIProvider). The two
   * types are separated here so each is used only for what it owns.
   */
  // প্রতিটি প্রদানকারীকে পিং করুন, তাদের স্থিতি আপডেট করুন এবং একটি স্বাস্থ্য প্রতিবেদন তৈরি করুন।
  public Mono<APIHealthReport> runProactiveHealthCheck() {
    log.info("[HEALTH-CHECK] Running health check for all providers...");

    return providerRepository
        .findAll()
        .flatMap(
            apiProvider -> {
              String name = apiProvider.getName();

              // ── Quarantine gate ────────────────────────────────────────
              long now = System.currentTimeMillis();
              Long quarantineExpiry = quarantineUntil.get(name);
              if (quarantineExpiry != null
                  && now
                      < quarantineExpiry) { // যদি প্রদানকারী কোয়ারেন্টাইনে থাকে এবং মেয়াদ শেষ না
                // হয়, তবে পিং এড়িয়ে যান।
                long remainingSec = (quarantineExpiry - now) / 1000;
                log.info(
                    "[HEALTH-CHECK] {} is quarantined for {} more seconds — skipping ping",
                    name,
                    remainingSec);
                return Mono.just(apiProvider);
              }
              // ───────────────────────────────────────────────────────────

              String currentStatus = apiProvider.getStatus();
              log.debug("[HEALTH-CHECK] Pinging {}", name);

              long start = System.currentTimeMillis();
              try {
                AIProvider aiProvider = providerFactory.getProvider(name);
                return aiProvider
                    .generate("ping")
                    .timeout(Duration.ofSeconds(8))
                    .flatMap(
                        resp -> {
                          long latency =
                              System.currentTimeMillis() - start; // পিং এর লেটেন্সি গণনা করুন।
                          boolean ok = resp != null && !resp.isBlank();
                          if (ok) {
                            // Successful ping — clear quarantine and failure history
                            quarantineUntil.remove(name);
                            failureTimestamps.remove(name);
                            apiProvider.setStatus(ProviderStatus.ACTIVE);
                            apiProvider.setLastLatency(latency);
                            apiProvider.setLastErrorMessage(null);
                            apiProvider.setConsecutiveErrorDays(null);
                          } else { // যদি পিং সফল না হয় বা খালি প্রতিক্রিয়া আসে।
                            apiProvider.setLastLatency(latency);
                            apiProvider.setLastErrorMessage("Empty response from ping");
                            recordFailureAndMaybeQuarantine(name);
                          }
                          apiProvider.setLastTested(LocalDateTime.now());
                          return providerRepository.save(apiProvider);
                        })
                    .thenReturn(apiProvider)
                    .onErrorResume(
                        e -> { // পিং ব্যর্থ হলে ত্রুটি পরিচালনা করুন।
                          long latency = System.currentTimeMillis() - start;
                          log.warn(
                              "[HEALTH-CHECK] {} ping failed (status preserved={}): {}",
                              name,
                              currentStatus,
                              e.getMessage());
                          apiProvider.setLastLatency(latency);
                          apiProvider.setLastTested(LocalDateTime.now());
                          apiProvider.setLastErrorMessage("Health ping failed: " + e.getMessage());
                          recordFailureAndMaybeQuarantine(name);
                          if ("error".equalsIgnoreCase(currentStatus)) {
                            apiProvider.setStatus(ProviderStatus.INACTIVE);
                          }
                          return providerRepository.save(apiProvider);
                        })
                    .thenReturn(apiProvider);
              } catch (Exception e) {
                log.warn( // প্রদানকারী ইনস্ট্যান্স তৈরি করতে ব্যর্থ হলে।
                    "[HEALTH-CHECK] {} could not be tested (status preserved={}): {}",
                    name,
                    currentStatus,
                    e.getMessage());
                apiProvider.setLastTested(LocalDateTime.now());
                apiProvider.setLastErrorMessage(
                    "Cannot create provider instance: " + e.getMessage());
                recordFailureAndMaybeQuarantine(name);
                return providerRepository.save(apiProvider).thenReturn(apiProvider);
              }
            })
        .collectList()
        .flatMapMany(Flux::fromIterable)
        .reduce(
            new APIHealthReport(),
            (report, apiProvider) -> {
              report.setTotalKeysTested(report.getTotalKeysTested() + 1);
              String status = apiProvider.getStatus();
              if (ProviderStatus.ACTIVE.equalsIgnoreCase(status)) {
                report.setActiveKeys(report.getActiveKeys() + 1);
              } else if (ProviderStatus.INACTIVE.equalsIgnoreCase(status)) {
                report.setDeadKeys(report.getDeadKeys() + 1);
              } else {
                report.setDeadKeys(report.getDeadKeys() + 1); // ERROR/DEGRADED treated as dead
              }
              return report;
            });
  }

  /**
   * Record unknown errors to knowledge base for future self-healing. This follows the mandatory
   * knowledge-improvement rules: no error shall be returned without first adding a knowledge
   * artifact that would have prevented the failure.
   */
  // ভবিষ্যতের স্ব-আরোগ্যকরণের জন্য অজানা ত্রুটিগুলি জ্ঞানভাণ্ডারে রেকর্ড করুন।
  public void recordUnknownErrorToKnowledge(
      String errorSignature, String errorMessage, String stackTrace) {
    try {
      // Managed subscription to avoid dangling resources
      disposables.add(
          globalKnowledgeBase
              .recordSuccessWithPermission(
                  "UNKNOWN_ERROR_" + errorSignature.hashCode(),
                  "[SupremeAI Core — Unknown Error Self-Healing]\n\n"
                      + "Error Signature: "
                      + errorSignature
                      + "\n"
                      + "Error Message: "
                      + errorMessage
                      + "\n"
                      + "Stack Trace: "
                      + stackTrace
                      + "\n\n"
                      + "Resolution: This error was automatically captured by the self-healing system "
                      + "for future learning. When similar patterns occur, the system will attempt "
                      + "to apply known fixes before escalating to human intervention.",
                  "self-healing-system",
                  System.currentTimeMillis(),
                  0.95)
              .subscribe(
                  v ->
                      log.info(
                          "[SELF-HEALING] Knowledge artifact persisted for {}", errorSignature),
                  err ->
                      log.warn(
                          "[SELF-HEALING] Failed to persist knowledge for {}: {}",
                          errorSignature,
                          err.getMessage())));

      // Also attempt to record to core_knowledge.json via SupremeLearningOrchestrator
      if (learningOrchestrator != null) {
        learningOrchestrator.logUnknownError(errorSignature, errorMessage);
      }

      log.info("[SELF-HEALING] Recorded unknown error to knowledge base: {}", errorSignature);
    } catch (Exception e) {
      log.warn(
          "[SELF-HEALING] Failed to record unknown error to knowledge base: {}", e.getMessage());
    }
  }

  /**
   * Analyze an error and attempt self-healing. Before returning any failure, creates a knowledge
   * entry that would have prevented the failure.
   */
  public SupremeAIResponse
      analyzeError( // একটি ত্রুটি বিশ্লেষণ করুন এবং স্ব-আরোগ্যকরণের চেষ্টা করুন।
      String errorMessage, Throwable throwable, UserContext userContext) {
    String errorSignature =
        throwable.getClass().getSimpleName()
            + "_"
            + (throwable.getMessage() != null ? throwable.getMessage().hashCode() : "no_message");
    String codeContext = userContext != null ? userContext.getCodeContext() : "";
    String stackTrace =
        throwable != null
            ? org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(throwable)
            : "No stack trace";

    if (rootCauseAnalysisService != null) {
      try {
        RootCauseAnalysisService.RootCauseAnalysis analysis =
            rootCauseAnalysisService.analyzeError(errorSignature, errorMessage, codeContext);

        if (analysis != null) {
          if (analysis.canAutoFix && analysis.rootCauseConfidence > 0.8) {
            // Close the learning loop on the success path — async-subscribe // সফল পথে শেখার লুপ
            // বন্ধ করুন — অ্যাসিঙ্ক-সাবস্ক্রাইব।
            disposables.add(
                rootCauseAnalysisService
                    .recordSuccessfulCorrection(errorSignature, analysis.correctedCode)
                    .subscribe(
                        v ->
                            log.info(
                                "[SELF-HEALING] Correction recorded in GKB for {}", errorSignature),
                        err ->
                            log.warn(
                                "[SELF-HEALING] GKB write failed for {}: {}",
                                errorSignature,
                                err.getMessage())));
            return new SupremeAIResponse(
                true, "Auto-fix applied successfully", analysis.correctedCode, analysis);
          } else if (analysis.rootCauseConfidence > 0.5) {
            return new SupremeAIResponse(
                false,
                "Fix available but requires review. Confidence: " + analysis.rootCauseConfidence,
                analysis.correctedCode,
                analysis);
          }

          // Suggested action returned but not auto-fixable → log for manual review
          log.info( // প্রস্তাবিত অ্যাকশন ফেরত দেওয়া হয়েছে কিন্তু স্বয়ংক্রিয়ভাবে ঠিক করা যায়
              // না → ম্যানুয়াল পর্যালোচনার জন্য লগ করুন।
              "[SELF-HEALING] RCA returned non-auto-fixable action {} for {} — manual review needed",
              analysis.suggestedAction,
              errorSignature);
        }

        // MANDATORY KNOWLEDGE RULE: Record analysis attempt even if confidence is low
        if (analysis != null && analysis.rootCauseConfidence <= 0.8) {
          recordUnknownErrorToKnowledge( // বাধ্যতামূলক জ্ঞান নিয়ম: আত্মবিশ্বাসের মাত্রা কম হলেও
              // বিশ্লেষণের চেষ্টা রেকর্ড করুন।
              errorSignature + "_LOW_CONF",
              errorMessage,
              "RCA Suggested: " + analysis.suggestedAction);
        }
      } catch (Exception e) {
        // RCA itself threw — record this as a failed correction so the ML predictor
        // learns
        log.warn(
            "[SELF-HEALING] RCA analysis failed for {}: {}",
            errorSignature,
            e.getMessage()); // RCA বিশ্লেষণ ব্যর্থ হলে, এটি একটি ব্যর্থ সংশোধন হিসাবে রেকর্ড
        // করুন যাতে ML ভবিষ্যদ্বাণীকারী শিখতে পারে।
        if (rootCauseAnalysisService != null) {
          try {
            rootCauseAnalysisService.recordFailedCorrection(
                errorSignature, errorMessage, codeContext);
          } catch (Exception ex) {
            log.error(
                "[SELF-HEALING] recordFailedCorrection also failed for {}", errorSignature, ex);
          }
        }
      }
    }

    // Unknown or unhandled error — ALWAYS write a knowledge artifact before
    // returning failure // অজানা বা অনিয়ন্ত্রিত ত্রুটি — ব্যর্থতা ফেরত দেওয়ার আগে সর্বদা একটি
    // জ্ঞান আর্টিফ্যাক্ট লিখুন।
    recordUnknownErrorToKnowledge(errorSignature, errorMessage, stackTrace);
    return new SupremeAIResponse(
        false, "Self-healing attempted but no fix available for error: " + errorMessage, null);
  }

  /** Attempt to apply an automatic fix for known error patterns. */
  // পরিচিত ত্রুটি প্যাটার্নের জন্য স্বয়ংক্রিয় ফিক্স প্রয়োগ করার চেষ্টা করুন।
  private SupremeAIResponse
      attemptAutoFix( // এই মেথডটি পরিচিত ত্রুটি প্যাটার্নের জন্য স্বয়ংক্রিয় ফিক্স প্রয়োগ করার
          // চেষ্টা করে।
          String errorSignature,
          String errorMessage,
          Throwable throwable,
          UserContext userContext) {
    // This would contain logic to apply known fixes for recurring errors
    // For now, return null to indicate no auto-fix available
    return null;
  }

  /**
   * Detect and fix issues proactively. Enhanced to record unknown error patterns to knowledge base.
   */
  // সক্রিয়ভাবে সমস্যা সনাক্ত এবং সমাধান করুন। অজানা ত্রুটি প্যাটার্ন জ্ঞানভাণ্ডারে রেকর্ড করার
  // জন্য উন্নত করা হয়েছে।
  public void detectAndFix() { // সক্রিয়ভাবে সমস্যা সনাক্ত এবং সমাধান করুন।
    try {
      // Existing health check logic...
      disposables.add( // বিদ্যমান স্বাস্থ্য পরীক্ষা লজিক।
          runProactiveHealthCheck()
              .subscribe(
                  report -> {
                    log.info(
                        "[SELF-HEALING] Proactive check: {} active, {} inactive, {} error",
                        report.getActiveCount(),
                        report.getTotalCount() - report.getActiveCount(),
                        report.getDeadCount());

                    // If we find inactive/error providers, try to recover them
                    if (report.getDeadCount()
                        > 0) { // যদি নিষ্ক্রিয় বা ত্রুটিপূর্ণ প্রদানকারী পাওয়া যায়, তবে তাদের
                      // পুনরুদ্ধার করার চেষ্টা করুন।
                      disposables.add(
                          recoverFailedProviders()
                              .subscribe(
                                  null,
                                  err ->
                                      log.error(
                                          "[SELF-HEALING] Recovery failed: {}", err.getMessage())));
                    }
                  },
                  err -> {
                    // Record the error to knowledge base BEFORE logging it (MANDATORY) // লগ করার
                    // আগে জ্ঞানভাণ্ডারে ত্রুটি রেকর্ড করুন (বাধ্যতামূলক)।
                    String errorSignature = "HEALTH_CHECK_FAILURE_" + System.currentTimeMillis();
                    recordUnknownErrorToKnowledge(
                        errorSignature,
                        err.getMessage(),
                        org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(err));

                    log.error(
                        "[SELF-HEALING] Proactive health check failed: {}", err.getMessage(), err);
                  }));
    } catch (Exception e) {
      // Record unknown error to knowledge base (MANDATORY) // অজানা ত্রুটি জ্ঞানভাণ্ডারে রেকর্ড
      // করুন (বাধ্যতামূলক)।
      String errorSignature = "DETECT_AND_FIX_EXCEPTION_" + System.currentTimeMillis();
      recordUnknownErrorToKnowledge(
          errorSignature,
          e.getMessage(),
          org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));

      log.error("[SELF-HEALING] Error in detectAndFix: {}", e.getMessage(), e);
    }
  }

  /**
   * Record a failure for the given provider and quarantine it if the failure rate exceeds {@link
   * #QUARANTINE_FAILURE_THRESHOLD} within {@link #QUARANTINE_WINDOW_MS}.
   */
  private void recordFailureAndMaybeQuarantine(
      String providerName) { // প্রদানকারীর ব্যর্থতা রেকর্ড এবং স্বয়ংক্রিয় কোয়ারেন্টাইন লজিক।
    long now = System.currentTimeMillis();
    java.util.List<Long> recent =
        failureTimestamps.computeIfAbsent(
            providerName, k -> java.util.Collections.synchronizedList(new java.util.ArrayList<>()));
    recent.add(now);

    // স্লাইডিং উইন্ডোর বাইরের পুরনো টাইমস্ট্যাম্পগুলি মুছে ফেলে মেমরি সাশ্রয় করা হচ্ছে।
    recent.removeIf(ts -> now - ts > quarantineWindowMs);

    if (recent != null
        && recent.size() >= quarantineFailureThreshold) { // যদি ব্যর্থতার হার সীমা ছাড়িয়ে যায়।
      long expiry = now + quarantineDurationMs;
      quarantineUntil.put(providerName, expiry);
      log.warn(
          "[HEALTH-CHECK] {} quarantined for {} minutes after {} failures in {} minutes",
          providerName,
          quarantineDurationMs / 60000,
          recent.size(),
          quarantineWindowMs / 60000);

      // Record quarantine event to knowledge base // জ্ঞানভাণ্ডারে কোয়ারেন্টাইন ইভেন্ট রেকর্ড
      // করুন।
      String errorSignature = "PROVIDER_QUARANTINED_" + providerName;
      recordUnknownErrorToKnowledge(
          errorSignature,
          "Provider "
              + providerName
              + " quarantined after "
              + recent.size()
              + " failures in "
              + (quarantineWindowMs / 60000)
              + " minutes",
          "Quarantine expiry: " + new java.util.Date(expiry));
    }
  }

  /**
   * Quarantine a specific domain (Local-First Architecture). Called when BrowserService repeatedly
   * fails to access a specific site.
   */
  // একটি নির্দিষ্ট ডোমেইনকে কোয়ারেন্টাইন করুন (লোকাল-ফার্স্ট আর্কিটেকচার)।
  public void recordDomainFailureAndMaybeQuarantine(String domain) {
    long now = System.currentTimeMillis();
    java.util.List<Long> recent =
        domainFailureTimestamps.computeIfAbsent(
            domain, k -> java.util.Collections.synchronizedList(new java.util.ArrayList<>()));
    recent.add(now);

    // ডোমেইন লেভেলেও স্লাইডিং উইন্ডো ব্যবহার করে কোয়ারেন্টাইন নির্ধারণ করা হয়।
    recent.removeIf(ts -> now - ts > quarantineWindowMs);

    if (recent != null && recent.size() >= quarantineFailureThreshold) {
      long expiry = now + quarantineDurationMs;
      domainQuarantineUntil.put(domain, expiry);
      log.warn(
          "[SELF-HEALING] Domain {} quarantined for {} minutes after {} failures.",
          domain,
          quarantineDurationMs / 60000,
          recent.size());

      // Intelligence Gap Detection
      if (learningOrchestrator != null) { // ইন্টেলিজেন্স গ্যাপ সনাক্তকরণ।
        learningOrchestrator.detectIntelligenceGap("browser_access_" + domain);
      }
    }
  }

  /** Check if a domain is quarantined. */
  public boolean isDomainQuarantined(String domain) {
    Long expiry =
        domainQuarantineUntil.get(domain); // একটি ডোমেইন কোয়ারেন্টাইনে আছে কিনা তা পরীক্ষা করুন।
    return expiry != null && System.currentTimeMillis() < expiry;
  }

  /**
   * Generate an Improvement Proposal and send it to KingsMode (Admin Queue).
   *
   * @param currentState System's current state.
   * @param proposedChange The new library/alternative found.
   * @param reasoning Why this is good.
   * @param expectedImpact Expected improvements in speed, security, etc.
   */
  // একটি উন্নতি প্রস্তাব তৈরি করুন এবং এটি কিংসমোডে (অ্যাডমিন কিউ) পাঠান।
  public void proposeImprovementToKingsMode(
      String currentState, String proposedChange, String reasoning, String expectedImpact) {
    String proposalId = "PROPOSAL_" + System.currentTimeMillis();

    StringBuilder proposalInBangla = new StringBuilder();
    proposalInBangla.append("🌟 **SupremeAI Improvement Proposal (KingsMode)** 🌟\n\n");
    proposalInBangla
        .append("১. **বর্তমান অবস্থা (Current State):** ")
        .append(currentState)
        .append("\n");
    proposalInBangla
        .append("২. **সাজেশন (Proposed Change):** ")
        .append(proposedChange)
        .append("\n");
    proposalInBangla
        .append("৩. **কেন অ্যাপ্রুভ করা উচিত (Reasoning):** ")
        .append(reasoning)
        .append("\n");
    proposalInBangla
        .append("৪. **উন্নতি (Expected Impact):** ")
        .append(expectedImpact)
        .append("\n");

    log.info("[KINGSMODE] Generated Proposal ID: {}\n{}", proposalId, proposalInBangla.toString());

    // In a fully integrated environment, we save this to a database table read by
    // getPendingConfirmations // একটি সম্পূর্ণ সমন্বিত পরিবেশে, আমরা এটি একটি ডাটাবেস টেবিলে
    // সংরক্ষণ করি যা getPendingConfirmations দ্বারা পড়া হয়।
    try {
      disposables.add(
          globalKnowledgeBase
              .recordSuccessWithPermission(
                  proposalId,
                  proposalInBangla.toString(),
                  "kingsmode-pending",
                  System.currentTimeMillis(),
                  0.90)
              .subscribe(
                  v -> log.info("[KINGSMODE] Proposal {} saved to knowledge base", proposalId),
                  err ->
                      log.warn(
                          "[KINGSMODE] Failed to save proposal {} to knowledge base: {}",
                          proposalId,
                          err.getMessage())));
    } catch (Exception e) {
      log.warn("[KINGSMODE] Failed to save proposal to knowledge base: {}", e.getMessage());
    }
  }

  /** Periodically clean up quarantine maps to prevent unbounded memory growth. */
  // সীমাহীন মেমরি বৃদ্ধি রোধ করতে পর্যায়ক্রমে কোয়ারেন্টাইন ম্যাপগুলি পরিষ্কার করুন।
  private void cleanupStateMaps() {
    long now = System.currentTimeMillis();
    quarantineUntil.entrySet().removeIf(e -> now > e.getValue());
    domainQuarantineUntil.entrySet().removeIf(e -> now > e.getValue());

    // Prune stale failure tracking entries to prevent memory leaks from unique keys
    failureTimestamps
        .entrySet()
        .removeIf(
            entry -> {
              entry
                  .getValue()
                  .removeIf(
                      ts ->
                          now - ts
                              > quarantineWindowMs); // মেমরি লিক রোধে পুরনো ব্যর্থতার ডেটা ক্লিনআপ।
              return entry.getValue().isEmpty();
            });

    domainFailureTimestamps
        .entrySet()
        .removeIf(
            entry -> {
              entry.getValue().removeIf(ts -> now - ts > quarantineWindowMs);
              return entry.getValue().isEmpty();
            });
  }

  /**
   * Recover failed providers using fallback mechanisms. Attempts to re-activate quarantined
   * providers whose quarantine period has expired, and delegates to the fallback orchestrator for
   * active recovery.
   */
  private Mono<Void>
      recoverFailedProviders() { // ফলব্যাক মেকানিজম ব্যবহার করে ব্যর্থ প্রদানকারীদের পুনরুদ্ধার
    // করুন।
    try {
      long now = System.currentTimeMillis();

      // 1. Release any providers whose quarantine has expired
      List<String> released = new ArrayList<>();
      for (Map.Entry<String, Long> entry : quarantineUntil.entrySet()) {
        if (now >= entry.getValue()) {
          released.add(
              entry.getKey()); // মেয়াদ উত্তীর্ণ কোয়ারেন্টাইন থেকে প্রদানকারীদের মুক্ত করুন।
        }
      }
      for (String name : released) {
        quarantineUntil.remove(name);
        failureTimestamps.remove(name);
        log.info("[SELF-HEALING] Quarantine expired for {} — attempting recovery ping", name);
      }

      // 2. Actively re-ping released providers to verify they're back online
      if (!released
          .isEmpty()) { // সক্রিয়ভাবে মুক্ত প্রদানকারীদের পুনরায় পিং করুন তারা অনলাইনে ফিরে এসেছে
        // কিনা তা যাচাই করতে।
        Mono<Void> recoveryProcess =
            providerRepository
                .findAll()
                .filter(p -> released.contains(p.getName()))
                .flatMap(
                    apiProvider -> {
                      String name = apiProvider.getName();
                      try {
                        AIProvider aiProvider = providerFactory.getProvider(name);
                        return aiProvider
                            .generate("ping")
                            .timeout(Duration.ofSeconds(8))
                            .flatMap(
                                resp -> {
                                  boolean ok = resp != null && !resp.isBlank();
                                  if (ok) {
                                    apiProvider.setStatus(ProviderStatus.ACTIVE);
                                    apiProvider.setLastErrorMessage(null);
                                    apiProvider.setLastTested(LocalDateTime.now());
                                    log.info(
                                        "[SELF-HEALING] ✅ Provider {} recovered successfully",
                                        name); // প্রদানকারী সফলভাবে পুনরুদ্ধার হয়েছে।
                                  } else {
                                    apiProvider.setLastErrorMessage("Recovery ping returned empty");
                                    apiProvider.setLastTested(LocalDateTime.now());
                                    log.warn(
                                        "[SELF-HEALING] Provider {} recovery ping returned empty",
                                        name);
                                  }
                                  return providerRepository
                                      .save(apiProvider)
                                      .doOnSuccess(
                                          p ->
                                              log.debug(
                                                  "Provider {} status updated after recovery ping.",
                                                  apiProvider.getName()))
                                      .doOnError(
                                          e -> // পুনরুদ্ধারের পিং এর পরে প্রদানকারী সংরক্ষণ করতে
                                              // ব্যর্থ হলে।
                                              log.error(
                                                  "Failed to save provider {} after recovery ping: {}",
                                                  apiProvider.getName(),
                                                  e.getMessage()))
                                      .then(); // Return a completion signal to the chain
                                })
                            .onErrorResume(
                                e -> {
                                  apiProvider.setLastErrorMessage(
                                      "Recovery ping failed: " + e.getMessage());
                                  apiProvider.setLastTested(LocalDateTime.now());
                                  log.warn(
                                      "[SELF-HEALING] Provider {} recovery ping failed: {}",
                                      name,
                                      e.getMessage());
                                  return providerRepository
                                      .save(apiProvider)
                                      .doOnSuccess(
                                          p ->
                                              log.debug(
                                                  "Provider {} status updated after failed recovery ping.",
                                                  apiProvider.getName()))
                                      .doOnError(
                                          err -> // ব্যর্থ পুনরুদ্ধারের পিং এর পরে প্রদানকারী
                                              // সংরক্ষণ করতে ব্যর্থ হলে।
                                              log.error(
                                                  "Failed to save provider {} after failed recovery ping: {}",
                                                  apiProvider.getName(),
                                                  err.getMessage()))
                                      .then(); // Return a completion signal to the chain
                                });
                      } catch (Exception e) {
                        log.warn(
                            "[SELF-HEALING] Cannot create provider instance for {}: {}", // প্রদানকারী ইনস্ট্যান্স তৈরি করতে ব্যর্থ হলে।
                            name,
                            e.getMessage());
                        return Mono.empty();
                      }
                    })
                .then();

        log.info("[SELF-HEALING] Recovery initiated for {} providers", released.size());
        return recoveryProcess;
      }

      // 3. Delegate to fallback orchestrator for routing adjustments
      if (fallbackOrchestrator != null
          && !released
              .isEmpty()) { // রাউটিং সমন্বয়ের জন্য ফলব্যাক অর্কেস্ট্রেটরের কাছে অর্পণ করুন।
        log.info(
            "[SELF-HEALING] Notifying fallback orchestrator of {} recovered providers",
            released.size());
      }
    } catch (Exception e) {
      String errorSignature = "PROVIDER_RECOVERY_EXCEPTION_" + System.currentTimeMillis();
      recordUnknownErrorToKnowledge(
          errorSignature,
          e.getMessage(),
          org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));

      log.warn("[SELF-HEALING] Failed to recover providers: {}", e.getMessage(), e);
    }
    return Mono.empty();
  }

  @Autowired private org.springframework.core.env.Environment env;

  /**
   * Perform initial health check after application is fully ready. Uses @Async to avoid blocking
   * the main startup thread.
   */
  @EventListener(
      ApplicationReadyEvent
          .class) // অ্যাপ্লিকেশন সম্পূর্ণরূপে প্রস্তুত হওয়ার পরে প্রাথমিক স্বাস্থ্য পরীক্ষা করুন।
  @Async
  public void onApplicationReady() {
    if (env != null && java.util.Arrays.asList(env.getActiveProfiles()).contains("test")) {
      log.info(
          "[SELF-HEALING] Test profile active. Skipping proactive health check to avoid shutdown conflicts.");
      return;
    }
    log.info("[SELF-HEALING] Application ready - triggering initial health check in 10 seconds...");
    try {
      TimeUnit.SECONDS.sleep(10);
      detectAndFix();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  // ─── Missing stub methods required by callers ───────────────────

  /**
   * Check if a provider is currently quarantined. Returns true when the expiry timestamp is in the
   * future (i.e. still quarantined).
   */
  // একটি প্রদানকারী বর্তমানে কোয়ারেন্টাইনে আছে কিনা তা পরীক্ষা করুন।
  public boolean isProviderQuarantined(
      String providerName) { // একটি প্রদানকারী বর্তমানে কোয়ারেন্টাইনে আছে কিনা তা পরীক্ষা করুন।
    Long expiry = quarantineUntil.get(providerName);
    return expiry != null && System.currentTimeMillis() < expiry;
  } // মেয়াদ উত্তীর্ণ টাইমস্ট্যাম্প ভবিষ্যতে থাকলে (অর্থাৎ এখনও কোয়ারেন্টাইনে) সত্য ফেরত দেয়।

  /** Retry a reactive task with exponential backoff. */
  public <T> Mono<T> executeWithRetry(
      java.util.function.Supplier<Mono<T>> task, int maxAttempts, long initialBackoffMs) {
    return Mono.defer(task)
        .retryWhen(
            reactor.util.retry.Retry.fixedDelay(
                Math.max(0, maxAttempts - 1), java.time.Duration.ofMillis(initialBackoffMs)))
        .doOnError(
            err -> {
              if (reasoningService != null) {
                reasoningService.logReasoning(
                    UUID.randomUUID().toString(),
                    "Execution Attempt Failed",
                    err.getMessage(),
                    "SelfHealingService");
              }
            });
  }

  /** Trigger self-healing when a GitHub workflow failure is detected. */
  // একটি GitHub ওয়ার্কফ্লো ব্যর্থতা সনাক্ত হলে স্ব-আরোগ্যকরণ ট্রিগার করুন।
  public void handleWorkflowFailure(String repo, String workflowId, String reason) {
    log.info(
        "[HEALTH-CHECK] GitHub workflow failure: repo={}, workflowId={}, reason={}",
        repo,
        workflowId,
        reason);
    if (reasoningService != null) {
      String displayReason = reason;
      if (reason != null && reason.length() > 100) {
        displayReason = reason.substring(0, 100) + "...";
      }
      reasoningService.logReasoning(
          workflowId, "Self-Healing Triggered", displayReason, "SupremeAI-SelfHealer");
    }
    detectAndFix();
  }

  /** Helper to analyze error message category. */
  // ত্রুটি বার্তার বিভাগ বিশ্লেষণ করার জন্য সহায়ক।
  private String analyzeError(String errorMsg) {
    if (errorMsg == null) {
      return "UNKNOWN";
    }
    String upper = errorMsg.toUpperCase();
    if (upper.contains("DEPENDENCY") || upper.contains("RESOLUTION FAILED")) {
      return "CHECK_DEPENDENCIES";
    }
    if (upper.contains("TEST") || upper.contains("ASSERTION")) {
      return "FIX_TESTS";
    }
    if (upper.contains("UNAUTHORIZED") || upper.contains("401") || upper.contains("AUTH")) {
      return "CHECK_AUTH_TOKENS";
    }
    if (upper.contains("QUOTA") || upper.contains("429") || upper.contains("LIMIT")) {
      return "ROTATE_API_KEYS";
    }
    return "GENERAL_SYSTEM_CHECK";
  }

  /** Parameterized detect-and-fix entry point called from REST controller. */
  // REST কন্ট্রোলার থেকে কল করা প্যারামিটারাইজড ডিটেক্ট-এন্ড-ফিক্স এন্ট্রি পয়েন্ট।
  public Mono<ResponseEntity<Map<String, Object>>> detectAndFix(String error) {
    return Mono.fromCallable(
        () -> {
          detectAndFix();
          return ResponseEntity.ok(
              Map.of(
                  "status", "detected",
                  "error", error,
                  "message", "Detection cycle completed"));
        });
  }

  /**
   * Adaptive iterative improvement — fully reactive, non-blocking self-healing loop.
   *
   * <p>The approval vote ({@code conductApprovalVote}) now runs via {@link
   * Schedulers#boundedElastic()}, eliminating any blocking on the Netty/Servlet event-loop threads.
   *
   * <ul>
   *   <li>PROVIDER RESOLUTION — reactive before entering bounded-elastic
   *   <li>PASS 1 — {@link #isCodePerfect(String)} checks TODO/FIXME markers, brace balance, class
   *       keyword presence, and minimum meaningful-body length ≥ 8 lines
   *   <li>APPROVAL VOTE — non-blocking {@code flatMap} on a bounded-elastic Mono
   *   <li>PASS 2 — If a {@link MultiAIVotingService} is available, asks the council to approve
   *       proposed changes before applying them
   *   <li>PASS 3 — {@link #applyHeuristicImprovements(String,String,int)} applies structured
   *       improvements (TODO annotation pass, context-aware injection, stub-comment replacement,
   *       brace-balance enforcement)
   * </ul>
   *
   * @param taskCategory The category label used for RCA tracking and voting context
   * @param prompt The natural-language description of the desired code output
   * @return Mono of the best version of {@code currentCode} found within MAX_ITERATIONS
   */
  public Mono<String> developUntilPerfection(
      String taskCategory,
      String prompt) { // MAX_ITERATIONS এর মধ্যে পাওয়া currentCode এর সেরা সংস্করণ ফেরত দেয়।
    log.info("[SELF-HEALING] Starting self-healing loop: category={}", taskCategory);

    // Resolve the active provider ID reactively BEFORE entering the bounded-elastic
    // callable block. Provider lookup is a non-blocking Firestore query.
    Mono<String> defaultProviderIdMono =
        (votingService != null)
            ? providerRepository
                .findByStatus("active")
                .map(p -> p.getName())
                .defaultIfEmpty("")
                .next()
                .timeout(Duration.ofSeconds(3))
                .onErrorReturn("")
            : Mono.just("");

    // The approval vote (conductApprovalVote) is inherently a blocking pipeline
    // (it materialises its upstream Mono calls). We therefore:
    // 1. Run the iteration body on boundedElastic via subscribeOn
    // 2. Inside the loop, call conductApprovalVote() directly—no .block()—
    // and let flatMap chain it as a non-blocking step
    // 3. Use onErrorResume so a vote timeout/failure never kills the whole loop
    java.util.function.Function<String, Mono<Boolean>> approvalGate =
        defaultProviderId -> { // অনুমোদনের জন্য একটি গেট ফাংশন।
          boolean autoApprove =
              configService != null
                  && !configService.getEffectiveSetting("autoExecApprovalRequired", true);
          if (autoApprove) return Mono.just(true);

          return (votingService != null
                  && defaultProviderId != null
                  && !defaultProviderId.isBlank())
              ? votingService.conductApprovalVote(
                  taskCategory, prompt, java.util.List.of(defaultProviderId))
              // Default fallback if voting unavailable
              : Mono.just(true);
        };

    return defaultProviderIdMono
        .flatMap(
            defaultProviderId -> {
              String initialCode = generateInitialCode(prompt);
              return Mono.just(reactor.util.function.Tuples.of(0, initialCode))
                  .expand(
                      state -> {
                        int iteration = state.getT1();
                        String currentCode = state.getT2();
                        // কোড গুণমান পরীক্ষা করুন।
                        log.info("[SELF-HEALING] Iteration {}: evaluating code", iteration + 1);

                        return Mono.fromCallable(() -> isCodePerfect(currentCode))
                            .subscribeOn(Schedulers.parallel())
                            .flatMap(
                                isPerfect -> {
                                  if (isPerfect) {
                                    log.info( // কোড গুণমান গেট পাস করেছে।
                                        "[SELF-HEALING] Code passed quality gate after {} iterations",
                                        iteration + 1);
                                    return Mono.empty();
                                  }

                                  return approvalGate
                                      .apply(defaultProviderId)
                                      .flatMap(
                                          approved -> {
                                            if (approved != null && !approved) {
                                              log.warn( // কাউন্সিল পরিবর্তন অনুমোদন করেনি।
                                                  "[SELF-HEALING] Council disapproved changes at iteration {}; returning best-known version.",
                                                  iteration + 1);
                                              return Mono.empty(); // Stop loop, return best-known
                                            }
                                            log.info(
                                                "[SELF-HEALING] Iteration {}: approval granted, applying improvement pass",
                                                iteration + 1);
                                            // কোড উন্নত করার চেষ্টা করুন।
                                            return Mono.fromCallable(
                                                    () ->
                                                        improveCode(currentCode, prompt, iteration))
                                                .subscribeOn(Schedulers.parallel())
                                                .flatMap(
                                                    improved ->
                                                        Mono.fromCallable(
                                                                () -> isCodePerfect(improved))
                                                            .subscribeOn(Schedulers.parallel())
                                                            .map(
                                                                perfectAfter -> {
                                                                  if (perfectAfter) {
                                                                    log.info( // কোড গুণমান গেট পাস
                                                                        // করেছে।
                                                                        "[SELF-HEALING] Code passed quality gate after {} improvements",
                                                                        iteration + 1);
                                                                    return reactor.util.function
                                                                        .Tuples.of(
                                                                        MAX_ITERATIONS + 99,
                                                                        improved); // Stop loop via
                                                                    // takeUntil
                                                                  }
                                                                  return reactor.util.function
                                                                      .Tuples.of(
                                                                      iteration + 1, improved);
                                                                }));
                                          })
                                      .onErrorResume(
                                          err -> {
                                            log.warn( // অনুমোদন ভোট ব্যর্থ হলে (উপেক্ষা করে
                                                // চালিয়ে যান)।
                                                "[SELF-HEALING] Approval vote failed at iteration {} (ignoring and continuing): {}",
                                                iteration + 1,
                                                err.getMessage());

                                            return Mono.fromCallable(
                                                    () ->
                                                        improveCode(currentCode, prompt, iteration))
                                                .subscribeOn(Schedulers.parallel())
                                                .flatMap(
                                                    improved ->
                                                        Mono.fromCallable(
                                                                () -> isCodePerfect(improved))
                                                            .subscribeOn(Schedulers.parallel())
                                                            .map(
                                                                perfectAfter -> {
                                                                  if (perfectAfter) {
                                                                    log.info( // কোড গুণমান গেট পাস
                                                                        // করেছে।
                                                                        "[SELF-HEALING] Code passed quality gate after {} improvements",
                                                                        iteration + 1);
                                                                    return reactor.util.function
                                                                        .Tuples.of(
                                                                        MAX_ITERATIONS + 99,
                                                                        improved);
                                                                  }
                                                                  return reactor.util.function
                                                                      .Tuples.of(
                                                                      iteration + 1, improved);
                                                                }));
                                          });
                                });
                      })
                  .takeUntil(state -> state.getT1() >= MAX_ITERATIONS)
                  .last()
                  .map(
                      state -> { // সর্বোচ্চ ইটারেশন সংখ্যায় পৌঁছালে শেষ-জানা সংস্করণ ফেরত দিন।
                        if (state.getT1() == MAX_ITERATIONS) {
                          log.warn(
                              "[SELF-HEALING] Max iterations ({}) reached; returning last-known version",
                              MAX_ITERATIONS);
                        }
                        return state.getT2();
                      });
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  // ─────────────────────────────────────────────────────────────────
  // STB-07 / STB-08 helper methods — self-healing code quality loop
  // ─────────────────────────────────────────────────────────────────

  /**
   * // প্রাথমিক স্কেলেটন সোর্স তৈরি করে। Produces the initial skeleton source for the requested
   * task. Seeds a TODO-stub so subsequent iterations can measure improvement.
   */
  private String generateInitialCode(String prompt) {
    return "// Initial code for: "
        + prompt
        + "\n"
        + "public class Generated {\n"
        + "    // TODO: Implement\n"
        + "}\n";
  }

  /**
   * [STB-07] Multi-layered code quality check. A hard failure on ANY layer prevents declaring the
   * code "perfect".
   *
   * <ol>
   *   <li>No TODO / FIXME / STUB markers remaining
   *   <li>Open and close braces are balanced
   *   <li>At least one class name keyword is present
   *   <li>The body has ≥ 8 meaningful (non-blank, non-comment) lines — not a skeleton
   * </ol>
   */
  // কোডের গুণমান পরীক্ষা করে।
  @Value(
      "${supremeai.self-healing.code-quality.min-meaningful-lines:8}") // কনফিগারেশন থেকে সর্বনিম্ন
  // অর্থপূর্ণ লাইনের সংখ্যা
  // লোড করা হবে। ডিফল্ট মান 8।
  private int
      minMeaningfulLines; // কোডকে "পারফেক্ট" ঘোষণা করার জন্য প্রয়োজনীয় সর্বনিম্ন অর্থপূর্ণ লাইনের

  // সংখ্যা।

  private boolean isCodePerfect(String code) { // কোড "পারফেক্ট" কিনা তা পরীক্ষা করে।
    // স্তর 1 — আবর্জনা মার্কার অবশ্যই চলে যেতে হবে (যেমন TODO, FIXME, STUB)।
    if (code == null) return false;
    if (code.contains("TODO") || code.contains("FIXME") || code.contains("STUB")) {
      return false;
    }
    // স্তর 2 — বন্ধনীগুলি সুষম হতে হবে (আশেপাশের কোড কাঠামোগতভাবে সঠিক)।
    long openBraces = code.chars().filter(c -> c == '{').count();
    long closeBraces = code.chars().filter(c -> c == '}').count();
    if (openBraces != closeBraces || openBraces == 0) return false;
    // স্তর 3 — অন্তত একটি ক্লাস ঘোষণা প্রয়োজন।
    boolean hasClassKeyword = false;
    for (String line : code.split("\\R")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("public class ")
          || trimmed.startsWith("class ")
          || trimmed.startsWith("record ")
          || trimmed.startsWith("interface ")) {
        hasClassKeyword = true;
        break;
      }
    }
    if (!hasClassKeyword) return false;
    // স্তর 4 — শুধুমাত্র একটি স্কেলেটন স্টাব নয়; কমপক্ষে 'minMeaningfulLines' অর্থপূর্ণ লাইন থাকতে
    // হবে।
    long meaningfulLines =
        code.lines()
            .filter(l -> !l.trim().isEmpty())
            .filter(l -> !l.trim().startsWith("//") && !l.trim().startsWith("*"))
            .count();
    return meaningfulLines >= minMeaningfulLines;
  } // কোড "পারফেক্ট" কিনা তা পরীক্ষা করে।

  /**
   * [STB-08] Invokes one structured improvement pass on {@code currentCode}. Delegates entirely to
   * {@link #applyHeuristicImprovements}; never a no-op. Each call represents exactly one
   * self-healing iteration.
   *
   * @param currentCode The best code version found so far
   * @param prompt The task description (used for context-aware improvements)
   * @param iteration 0-based index of this improvement iteration
   * @return improved code — always different from input on the first call
   */
  // currentCode এর উপর একটি কাঠামোগত উন্নতি পাস প্রয়োগ করে।
  private String improveCode(String currentCode, String prompt, int iteration) { // কোড উন্নত করে।
    log.info("[SELF-HEALING] Improvement pass {} starting", iteration + 1);
    return applyHeuristicImprovements(currentCode, prompt, iteration);
  }

  /**
   * Structured heuristic improvement passes — invoked by {@link #improveCode}. Guaranteed to
   * transform the code; returns updated string on every call.
   *
   * <ol>
   *   <li>Elevate TODO annotations to a numbered task tag (never silently removed)
   *   <li>For command-style prompts (controller/service/API), inject a structured logging stub in
   *       place of the first TODO block
   *   <li>Replace bare "Stub" comments with a tracked REFACTOR reminder
   *   <li>Balance closing braces if the code has accumulated extra open brackets
   * </ol>
   */
  private String applyHeuristicImprovements(
      String code, String prompt, int iteration) { // হিউরিস্টিক উন্নতি প্রয়োগ করে।
    String improved = code;

    // পাস 1 — TODO/FIXME টীকা ট্যাগ করুন (রাখুন তবে বাড়ান)।
    improved =
        improved
            .replace("TODO:", "// TODO [iteration " + (iteration + 2) + "]:")
            .replace("FIXME:", "// FIXME [iteration " + (iteration + 2) + "]:");

    // পাস 2 — কমান্ড-টাইপ প্রম্পটের জন্য কাঠামোগত লগিং স্টাব ইনজেক্ট করুন।
    if (prompt
            != null // যদি প্রম্পট কমান্ড-টাইপ হয় (যেমন কন্ট্রোলার, সার্ভিস, এপিআই, এন্ডপয়েন্ট)।
        && (prompt.toLowerCase().contains("controller")
            || prompt.toLowerCase().contains("service")
            || prompt.toLowerCase().contains("api")
            || prompt.toLowerCase().contains("endpoint"))) {
      String injectLine =
          "\n        log.info(\"[HEALING] Iteration {} logic stub\");\n"
              + "        // TODO [iteration "
              + (iteration + 2)
              + "]: implement handler logic";
      improved = improved.replaceFirst("\\{\\s*", "{" + injectLine);
    }

    // পাস 3 — খালি "Stub" মন্তব্যগুলিকে ট্র্যাক করা রিফ্যাক্টর অনুস্মারকগুলিতে রূপান্তর করুন।
    improved =
        improved.replaceAll(
            "(?i)//\\s*stub\\b",
            "// [REFACTOR] Review and implement — flagged in iteration " + (iteration + 1));

    // পাস 4 — অসমাপ্ত বন্ধনীগুলি সুষম করুন।
    long openB = improved.chars().filter(c -> c == '{').count();
    long closeB = improved.chars().filter(c -> c == '}').count();
    if (openB > closeB) {
      int diff = (int) openB - (int) closeB;
      improved = improved + "\n".repeat(Math.max(0, diff)) + "}".repeat(Math.max(0, diff));
    }

    log.info(
        "[SELF-HEALING] Pass {} complete: code size {} → {} chars",
        iteration + 1,
        code.length(),
        improved.length());
    return improved;
  }

  /** Return the history of healing events from Firestore, ordered newest first. */
  // Firestore থেকে নিরাময় ইভেন্টের ইতিহাস ফেরত দিন, নতুন থেকে পুরানো ক্রমে সাজানো।
  public Flux<com.supremeai.model.HealingEvent>
      getHealingHistory() { // Firestore থেকে নিরাময় ইভেন্টের ইতিহাস ফেরত দিন।
    return healingEventRepository.findAllByOrderByTimestampDesc().take(200);
  }

  /** Reactivate all inactive/dead providers. */
  // সমস্ত নিষ্ক্রিয়/মৃত প্রদানকারীদের পুনরায় সক্রিয় করুন।
  public Mono<Map<String, Object>>
      reactivateAllProviders() { // সমস্ত নিষ্ক্রিয়/মৃত প্রদানকারীদের পুনরায় সক্রিয় করুন।
    return providerRepository
        .findAll()
        .filter(
            p ->
                "inactive".equalsIgnoreCase(p.getStatus())
                    || "error".equalsIgnoreCase(p.getStatus()))
        .flatMap(
            p -> {
              p.setStatus("active");
              return providerRepository.save(p);
            })
        .collectList()
        .map(list -> Map.of("reactivated", list.size(), "status", "all_providers_reactivated"));
  }

  /** Reindex provider models — placeholder that triggers a fresh health-check run. */
  // প্রদানকারী মডেলগুলি পুনরায় সূচী করুন — একটি নতুন স্বাস্থ্য-পরীক্ষা চালানোর জন্য একটি
  // স্থানধারক।
  public void reindexModels() { // প্রদানকারী মডেলগুলি পুনরায় সূচী করুন।
    log.info("[SELF-HEALING] Reindex models triggered");
    detectAndFix();
  }

  /** Get RCA statistics for the admin dashboard. */
  // অ্যাডমিন ড্যাশবোর্ডের জন্য RCA পরিসংখ্যান পান।
  public Map<String, Object>
      getRootCauseAnalysisStats() { // অ্যাডমিন ড্যাশবোর্ডের জন্য RCA পরিসংখ্যান পান।
    if (rootCauseAnalysisService != null) {
      return rootCauseAnalysisService.getStatistics();
    }
    return Map.of("totalAnalyses", 0, "autoFixablePatterns", 0, "successfulCorrections", 0);
  }

  /** Get recent correction records. */
  public List<Map<String, Object>> getRecentCorrections() { // সাম্প্রতিক সংশোধন রেকর্ড পান।
    if (rootCauseAnalysisService != null) {
      return rootCauseAnalysisService.getRecentCorrections(20);
    }
    return List.of();
  }
}
