package com.supremeai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.supremeai.fallback.ThirdOpinionOrchestrator;
import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.learning.active.ActiveInternetScraper;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.repository.SolutionMemoryRepository;
import com.supremeai.service.browser.BrowserService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * ════════════════════════════════════════════════════════════ SupremeAI Brain — সিস্টেমের একমাত্র
 * Central AI Execution Point ════════════════════════════════════════════════════════════
 *
 * <p>আর্কিটেকচার নীতি: ────────────────────────────────────────────────────────── ✅ সিস্টেমের EVERY
 * feature এই service দিয়ে AI call করবে ✅ SupremeAI নিজেই DEFAULT brain — কোনো external AI নয় ✅
 * Admin যদি helper AI configure করেন → SupremeCore সেটা ব্যবহার করে ✅ Helper AI না থাকলে →
 * core_knowledge.json + Firebase memory ✅ সম্পূর্ণ Reactive — কোনো .block() নেই ✅ Task-aware —
 * feature অনুযায়ী সঠিক AI route করে ──────────────────────────────────────────────────────────
 *
 * <p>Usage (যেকোনো Service-এ): ───────────────────────── @Autowired SupremeAIBrain brain;
 *
 * <p>brain.think("generate spring boot controller for users") // general
 * brain.think("CODE_GENERATION", prompt) // task-specific brain.think("TRANSLATION", prompt) //
 * translation brain.think("VISION", imagePrompt) // vision
 *
 * <p>════════════════════════════════════════════════════════════
 */
/**
 * CORE KNOWLEDGE ROLE STATEMENT: - The primary job of Core Knowledge from now on is to act as the
 * central coordinator and decision maker. - It understands how to make other components work (e.g.
 * why only the browser is enough to solve 80% of tasks, when to route tasks to helper models, and
 * what prompt engineering strategies to use dynamically).
 */
@Service
public class SupremeAIBrain {

  private static final Logger logger = LoggerFactory.getLogger(SupremeAIBrain.class);

  // ── Task category constants (Admin এই categories দিয়ে providers configure করেন)
  public static final String TASK_CHAT = "CHAT";
  public static final String TASK_CODE_GENERATION = "CODE_GENERATION";
  public static final String TASK_CODE_REVIEW = "CODE_REVIEW";
  public static final String TASK_TRANSLATION = "TRANSLATION";
  public static final String TASK_VISION = "VISION";
  public static final String TASK_ANALYSIS = "ANALYSIS";
  public static final String TASK_REASONING = "REASONING";
  public static final String TASK_SELF_HEALING = "SELF_HEALING";
  public static final String TASK_SECURITY = "SECURITY";
  public static final String TASK_TESTING = "TESTING";
  public static final String TASK_ORCHESTRATION = "ORCHESTRATION";
  public static final String TASK_GENERAL = "GENERAL";

  @Autowired private AIProviderFactory providerFactory;

  @Autowired private ThirdOpinionOrchestrator fallbackOrchestrator;

  @Autowired private SupremeLearningOrchestrator learningOrchestrator;

  @Autowired private ActiveInternetScraper activeInternetScraper;

  @Autowired private BrowserService browserService;

  @Autowired private SolutionMemoryRepository solutionMemoryRepository;

  @Autowired private ProviderRepository providerRepository;

  @Autowired private UnifiedOfflineKnowledgeService unifiedOfflineKnowledgeService;

  @Autowired private EnhancedLearningService enhancedLearningService;

  // Simple in-memory stats (Admin dashboard এ দেখানো হবে)
  private final Map<String, Long> taskCallCount = new ConcurrentHashMap<>();
  private final Map<String, Long> taskSuccessCount = new ConcurrentHashMap<>();

  // ══════════════════════════════════════════════════════════
  //  PRIMARY API — সব feature এই method গুলো ব্যবহার করবে
  // ══════════════════════════════════════════════════════════

  /**
   * General-purpose AI thinking — task category ছাড়া। SupremeCore নিজে সিদ্ধান্ত নেয় কোন helper
   * AI (যদি থাকে) ব্যবহার করবে।
   *
   * @param prompt ব্যবহারকারীর প্রশ্ন বা request
   * @return AI response (সবসময় কিছু না কিছু দেবে, কখনো empty নয়)
   */
  public Mono<String> think(String prompt) {
    return think(TASK_GENERAL, prompt);
  }

  /**
   * Task-specific AI thinking — Admin task অনুযায়ী helper AI route করতে পারেন।
   *
   * @param taskCategory TASK_* constants ব্যবহার করুন (যেমন: TASK_CODE_GENERATION)
   * @param prompt কাজের বিবরণ বা প্রশ্ন
   * @return AI response
   */
  public Mono<String> think(String taskCategory, String prompt) {
    return think(taskCategory, prompt, "NO_SIGNATURE");
  }

  /**
   * Full AI thinking — task, prompt এবং error signature সহ। ThirdOpinionOrchestrator এর সাথে
   * SupremeCore orchestration একত্রিত।
   *
   * @param taskCategory TASK_* constant
   * @param prompt AI-কে দেওয়া instruction
   * @param errorSignature known error/task signature (cache lookup-এর জন্য)
   * @return AI response — সবসময় non-null, কখনো error throw করে না
   */
  public Mono<String> think(String taskCategory, String prompt, String errorSignature) {
    if (prompt == null || prompt.isBlank()) {
      return Mono.just("[SupremeAI Brain] প্রম্পট খালি। কিছু লিখুন।");
    }

    String task = taskCategory != null ? taskCategory.toUpperCase() : TASK_GENERAL;
    trackCall(task);
    long startTime = System.currentTimeMillis();

    // ── Phase 3 Optimization: Check Solution Memory Cache first ──
    if (errorSignature != null && !errorSignature.equals("NO_SIGNATURE")) {
      return solutionMemoryRepository
          .findByTriggerError(errorSignature)
          .filter(sol -> !sol.isObsolete())
          .sort((a, b) -> Double.compare(b.calculateSupremeScore(), a.calculateSupremeScore()))
          .next()
          .map(
              sol -> {
                logger.info(
                    "[BRAIN] Optimized Path: Found known solution in memory for {}",
                    errorSignature);
                trackSuccess(task);
                return sol.getResolvedCode();
              })
          .switchIfEmpty(executeWithHubOrchestration(task, prompt, errorSignature, startTime));
    }

    return executeWithHubOrchestration(task, prompt, errorSignature, startTime);
  }

  private Mono<String> executeWithHubOrchestration(
      String task, String prompt, String errorSignature, long startTime) {

    // Core Knowledge decision making: Decide if query is Normal or Complex
    // (Change made: Core Knowledge is now a dynamic decision maker and does not serve static
    // answers)
    boolean isComplex =
        task.contains("CODE_")
            || task.contains("REVIEW")
            || task.contains("SECURITY")
            || task.contains("TESTING")
            || task.contains("REASONING")
            || prompt.contains("generate")
            || prompt.contains("write code")
            || prompt.contains("complex");

    if (!isComplex) {
      logger.info(
          "🧠 [CORE KNOWLEDGE] Decision: Routing NORMAL query to Browser and Database Learning in parallel.");

      // Part 1: Browser search and Database learning queries run in parallel
      Mono<String> browserMono =
          tryBrowserScraping(task, prompt).onErrorReturn("No web search results available.");

      String searchKey =
          (errorSignature != null && !errorSignature.equals("NO_SIGNATURE"))
              ? errorSignature
              : prompt;
      Mono<String> dbMemoryMono =
          solutionMemoryRepository
              .findByTriggerError(searchKey)
              .sort((a, b) -> Double.compare(b.calculateSupremeScore(), a.calculateSupremeScore()))
              .next()
              .map(sol -> sol.getResolvedCode())
              .defaultIfEmpty("No local database learning found.")
              .onErrorReturn("No local database learning found.");

      return Mono.zip(browserMono, dbMemoryMono)
          .flatMap(tuple -> chickenBrainMerge(tuple.getT1(), tuple.getT2(), prompt))
          .doOnNext(response -> trackSuccess(task));
    } else {
      logger.info(
          "🧠 [CORE KNOWLEDGE] Decision: Routing COMPLEX query to Deployed AI Model, and system will learn the output.");

      // Part 2: Complex questions route to Deployed AI Model
      return fallbackOrchestrator
          .executeWithSupremeIntelligence(task, errorSignature, prompt)
          .flatMap(
              aiResponse -> {
                logger.info(
                    "🧠 [CORE KNOWLEDGE] Decision: System is learning the complex AI response");
                // System dynamically learns the output and stores in database memory
                return enhancedLearningService
                    .learnFromInteraction("system", prompt, aiResponse)
                    .thenReturn(aiResponse);
              })
          .onErrorResume(
              e -> {
                logger.warn("Complex AI run failed, falling back: {}", e.getMessage());
                return Mono.just("Failed to execute complex logic on deployed AI model.");
              })
          .doOnNext(response -> trackSuccess(task));
    }
  }

  /**
   * ChickenBrain merges browser search results & database learning to create a better answer.
   * (Change made: ChickenBrain merges parallel results from browser and database learning)
   */
  private Mono<String> chickenBrainMerge(String browserData, String dbData, String prompt) {
    logger.info(
        "🧠 [ChickenBrain] Merging browser data and database learning for query: {}", prompt);

    String promptWithContext =
        "You are ChickenBrain. Merge the following web search data and local database learning data to answer the user question: \""
            + prompt
            + "\"\n\nContext:\n"
            + "Web Search Results:\n"
            + browserData
            + "\n\nLocal Database Learning:\n"
            + dbData;

    return fallbackOrchestrator
        .executeWithSupremeIntelligence("CHAT", "chicken_brain_merge", promptWithContext)
        .onErrorResume(
            e ->
                Mono.just(
                    "[ChickenBrain Local Merge]\n"
                        + "We found this in search:\n"
                        + browserData
                        + "\n\nAnd this in our database:\n"
                        + dbData));
  }

  /** Tier 3: Browser/Web Scraping fallback for real-time knowledge. */
  private Mono<String> tryBrowserScraping(String task, String prompt) {
    logger.info("[BRAIN TIER 3] Attempting web scraping via browser for task: {}", task);

    return Mono.fromCallable(
            () -> {
              String cleanQuery =
                  prompt
                      .toLowerCase()
                      .replaceAll("[\\p{Punct}]", " ")
                      .replaceAll("\\s+", " ")
                      .trim();
              return cleanQuery;
            })
        .flatMap(
            cleanQuery -> {
              return activeInternetScraper
                  .scrapeKnowledge(task, List.of(cleanQuery.split("\\s+")))
                  .next()
                  .map(issue -> issue.getSolution());
            })
        .timeout(java.time.Duration.ofSeconds(10))
        .onErrorResume(
            err -> {
              logger.warn("[BRAIN TIER 3] Web scraping failed: {}", err.getMessage());
              return Mono.just("[SupremeAI Core] Browser scraping failed. Please try again.");
            });
  }

  /** Demo endpoint - Guest access without authentication to test 4-layer resilience. */
  public Mono<String> thinkDemo(String prompt) {
    return think(TASK_GENERAL, prompt, "DEMO_NO_SIGNATURE");
  }

  // ══════════════════════════════════════════════════════════
  //  FEATURE-SPECIFIC CONVENIENCE METHODS
  //  (প্রতিটি feature এর জন্য clean, named API)
  // ══════════════════════════════════════════════════════════

  /** Code generate করতে */
  public Mono<String> generateCode(String specification) {
    return think(TASK_CODE_GENERATION, specification);
  }

  /** Code review করতে */
  public Mono<String> reviewCode(String code) {
    return think(TASK_CODE_REVIEW, "Review this code and provide detailed feedback:\n\n" + code);
  }

  /** Error fix suggestion দিতে */
  public Mono<String> fixError(String errorDescription, String errorSignature) {
    return think(
        TASK_SELF_HEALING,
        "Analyze this error and provide a fix:\n\n" + errorDescription,
        errorSignature);
  }

  /** Translation করতে */
  public Mono<String> translate(String text, String targetLanguage) {
    return think(
        TASK_TRANSLATION,
        String.format("Translate the following to %s:\n\n%s", targetLanguage, text));
  }

  /** Security analysis করতে */
  public Mono<String> analyzeSecurity(String code) {
    return think(
        TASK_SECURITY,
        "Perform a security audit on this code and identify vulnerabilities:\n\n" + code);
  }

  /** Test generation করতে */
  public Mono<String> generateTests(String code) {
    return think(TASK_TESTING, "Generate comprehensive unit tests for this code:\n\n" + code);
  }

  /** General reasoning/analysis করতে */
  public Mono<String> analyze(String topic) {
    return think(TASK_ANALYSIS, topic);
  }

  /** Vision/image analysis করতে */
  public Mono<String> analyzeImage(String imageDescription) {
    return think(TASK_VISION, imageDescription);
  }

  /** Chat/conversational response */
  public Mono<String> chat(String message) {
    return think(TASK_CHAT, message);
  }

  /** Agent orchestration decisions */
  public Mono<String> orchestrate(String agentTask) {
    return think(TASK_ORCHESTRATION, agentTask);
  }

  /**
   * Phase 1: Hub Identification for Super-Hub Orchestrator. Maps user intent to a specific Hub ID.
   */
  public Mono<String> identifyHub(String query) {
    return Mono.fromCallable(
        () -> {
          Map<String, String> hubInfo = learningOrchestrator.identifyBestHub(query);
          String hubName = hubInfo.get("hub").toLowerCase();

          if (hubName.contains("development")) return "dev_hub";
          if (hubName.contains("language") || hubName.contains("marketing")) return "lang_hub";
          if (hubName.contains("security")) return "security_hub";
          if (hubName.contains("visual")
              || hubName.contains("voice")
              || hubName.contains("multimodal")) return "multimodal_hub";
          if (hubName.contains("memory")) return "memory_hub";

          return "lang_hub"; // Default fallback
        });
  }

  // ══════════════════════════════════════════════════════════
  //  ADMIN STATS
  // ══════════════════════════════════════════════════════════

  /** Admin dashboard এর জন্য Brain-এর usage statistics */
  public Mono<Map<String, Object>> getBrainStats() {
    return providerFactory
        .getActiveHelperProviderIds()
        .collectList()
        .timeout(java.time.Duration.ofSeconds(3))
        .onErrorReturn(java.util.Collections.emptyList())
        .map(
            helpers -> {
              Map<String, Object> stats = new java.util.LinkedHashMap<>();
              stats.put("totalTaskCalls", taskCallCount);
              stats.put("totalSuccesses", taskSuccessCount);
              stats.put("availableHelperAIs", helpers);
              stats.put("defaultProvider", "SupremeAI-Core");
              return stats;
            });
  }

  // ══════════════════════════════════════════════════════════
  //  PHASE 2: LEARNING ORCHESTRATOR INTEGRATION
  // ══════════════════════════════════════════════════════════

  /**
   * Phase 2: Expose orchestrator learning stats for admin dashboard. Returns intent classification
   * counts, hub routing distribution, and correction history summary.
   */
  public Mono<Map<String, Object>> getLearningStats() {
    return Mono.fromCallable(
        () -> {
          Map<String, Object> stats = new java.util.LinkedHashMap<>();
          try {
            JsonNode root = learningOrchestrator.getRootNode();
            JsonNode reservoir = root.path("learning_reservoir");
            ArrayNode corrections = (ArrayNode) reservoir.path("recent_corrections");

            stats.put("totalCorrections", corrections.size());
            stats.put(
                "systemVersion", root.path("system_identity").path("version").asText("unknown"));
            stats.put("intentTaxonomyCategories", root.path("intent_taxonomy").size());

            // Hub routing distribution from recent corrections
            Map<String, Long> hubDistribution = new LinkedHashMap<>();
            for (JsonNode c : corrections) {
              String hub = c.path("corrected_hub").asText("unknown");
              hubDistribution.merge(hub, 1L, Long::sum);
            }
            stats.put("hubRoutingDistribution", hubDistribution);
          } catch (Exception e) {
            logger.warn("[BRAIN] Could not collect learning stats: {}", e.getMessage());
            stats.put("error", e.getMessage());
          }
          return stats;
        });
  }

  // ══════════════════════════════════════════════════════════
  //  INTERNAL HELPERS
  // ══════════════════════════════════════════════════════════

  private void trackCall(String task) {
    taskCallCount.merge(task, 1L, Long::sum);
  }

  private void trackSuccess(String task) {
    taskSuccessCount.merge(task, 1L, Long::sum);
  }

  private String truncate(String text) {
    if (text == null) return "";
    return text.length() > 100 ? text.substring(0, 100) + "..." : text;
  }
}
