package com.supremeai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.supremeai.fallback.ThirdOpinionOrchestrator;
import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.learning.active.ActiveInternetScraper;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.repository.SolutionMemoryRepository;
import com.supremeai.service.browser.BrowserService;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
 * CORE KNOWLEDGE ROLE STATEMENT: - The primary job of Core Knowledge from now
 * on is to act as the
 * central coordinator and decision maker. - It understands how to make other
 * components work (e.g.
 * why only the browser is enough to solve 80% of tasks, when to route tasks to
 * helper models, and
 * what prompt engineering strategies to use dynamically).
 */
@Service
public class SupremeAIBrain {

  private static final Logger logger = LoggerFactory.getLogger(SupremeAIBrain.class);

  // ── Task category constants (Admin এই categories দিয়ে providers configure
  // করেন)
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

  @Autowired
  private AIProviderFactory providerFactory;

  @Autowired
  private ThirdOpinionOrchestrator fallbackOrchestrator;

  @Autowired
  private SupremeLearningOrchestrator learningOrchestrator;

  @Autowired
  private ActiveInternetScraper activeInternetScraper;

  @Autowired
  private BrowserService browserService;

  @Autowired
  private SolutionMemoryRepository solutionMemoryRepository;

  @Autowired
  private ProviderRepository providerRepository;

  @Autowired
  private UnifiedOfflineKnowledgeService unifiedOfflineKnowledgeService;

  @Autowired
  private EnhancedLearningService enhancedLearningService;

  @Autowired
  private ObjectMapper objectMapper;

  // Simple in-memory stats (Admin dashboard এ দেখানো হবে)
  private final Map<String, Long> taskCallCount = new ConcurrentHashMap<>();
  private final Map<String, Long> taskSuccessCount = new ConcurrentHashMap<>();

  // ══════════════════════════════════════════════════════════
  // PRIMARY API — সব feature এই method গুলো ব্যবহার করবে
  // ══════════════════════════════════════════════════════════

  /**
   * General-purpose AI thinking — task category ছাড়া। SupremeCore নিজে সিদ্ধান্ত
   * নেয় কোন helper
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
   * @param taskCategory TASK_* constants ব্যবহার করুন (যেমন:
   *                     TASK_CODE_GENERATION)
   * @param prompt       কাজের বিবরণ বা প্রশ্ন
   * @return AI response
   */
  public Mono<String> think(String taskCategory, String prompt) {
    return think(taskCategory, prompt, "NO_SIGNATURE");
  }

  /**
   * Full AI thinking — task, prompt এবং error signature সহ।
   * ThirdOpinionOrchestrator এর সাথে
   * SupremeCore orchestration একত্রিত।
   *
   * @param taskCategory   TASK_* constant
   * @param prompt         AI-কে দেওয়া instruction
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
    // (Change made: Core Knowledge is now a dynamic decision maker and does not
    // serve static
    // answers)
    boolean isComplex = task.contains("CODE_")
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
      Mono<String> browserMono = tryBrowserScraping(task, prompt).onErrorReturn("No web search results available.");

      String searchKey = (errorSignature != null && !errorSignature.equals("NO_SIGNATURE"))
          ? errorSignature
          : prompt;
      Mono<String> dbMemoryMono = solutionMemoryRepository
          .findByTriggerError(searchKey)
          .sort((a, b) -> Double.compare(b.calculateSupremeScore(), a.calculateSupremeScore()))
          .next()
          .map(sol -> sol.getResolvedCode())
          .defaultIfEmpty("No local database learning found.")
          .onErrorReturn("No local database learning found.");

      return Mono.zip(browserMono, dbMemoryMono)
          .flatMap(tuple -> chickenBrainMerge(tuple.getT1(), tuple.getT2(), prompt, task, errorSignature))
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
   * ChickenBrain merges browser search results & database learning to create a
   * better answer.
   * (Change made: ChickenBrain merges parallel results from browser and database
   * learning)
   */
  private Mono<String> chickenBrainMerge(String browserData, String dbData, String prompt, String task, String errorSignature) {
    logger.info("🧠 [ChickenBrain] Merging browser data and database learning for query: {}", truncate(prompt));

    if (isInsufficientResponse(browserData, prompt).block() && isInsufficientResponse(dbData, prompt).block()) {
      logger.info("🧠 [ChickenBrain] Both browser and DB data are insufficient. Escalating to complex AI.");
      return escalateToComplexAI(task, prompt, errorSignature);
    }

    String promptWithContext = String.format(
        "You are ChickenBrain, a high-precision hybrid intelligence synthesizer for SupremeAI. "
            + "Merge the following web search data and local database learning data into ONE cohesive answer.\n\n"
            + "USER QUESTION: \"%s\"\n\n"
            + "CONTEXT 1 (Live Web Results):\n%s\n\n"
            + "CONTEXT 2 (Local Database Learning):\n%s\n\n"
            + "STRICT INSTRUCTIONS:\n"
            + "1. Prioritize Local Memory for project-specific architecture and internal rules.\n"
            + "2. Use Web Results for the latest technical trends, documentation, and external facts.\n"
            + "3. Resolve Contradictions: If sources disagree, favor the more recent and authoritative source.\n"
            + "4. Output: Use professional Markdown. Be concise but thorough. Answer in the same language as the user question.\n"
            + "5. If information is insufficient, state what is missing instead of hallucinating.",
        prompt, browserData, dbData);

    return fallbackOrchestrator
        .executeWithSupremeIntelligence("CHAT", "chicken_brain_merge", promptWithContext)
        .flatMap(mergedResponse -> isInsufficientResponse(mergedResponse, prompt)
            .flatMap(insufficient -> {
              if (insufficient) {
                logger.info("🧠 [ChickenBrain] Merged response is insufficient per AI Judge. Escalating to complex AI.");
                return escalateToComplexAI(task, prompt, errorSignature);
              }
              return Mono.just(mergedResponse);
            }))
        .onErrorResume(
            e -> Mono.just(
                "[ChickenBrain Local Merge]\n"
                    + "We found this in search:\n"
                    + browserData
                    + "\n\nAnd this in our database:\n"
                    + dbData));
  }

  private Mono<String> escalateToComplexAI(String task, String prompt, String errorSignature) {
      return fallbackOrchestrator
          .executeWithSupremeIntelligence(task, errorSignature, prompt)
          .flatMap(aiResponse -> enhancedLearningService.learnFromInteraction("system", prompt, aiResponse).thenReturn(aiResponse))
          .onErrorResume(e -> {
              logger.warn("Escalated complex AI run failed: {}", e.getMessage());
              return Mono.just("Failed to get information from deployed AI model after all attempts.");
          });
  }

  /** Tier 3: Browser/Web Scraping fallback for real-time knowledge. */
  private Mono<String> tryBrowserScraping(String task, String prompt) {
    logger.info("[BRAIN TIER 3] Attempting web scraping via browser for task: {}", task);

    return Mono.fromCallable(
        () -> {
          String cleanQuery = prompt
              .toLowerCase()
              .replaceAll("[\\p{Punct}]", " ")
              .replaceAll("\\s+", " ")
              .trim();
          String domain = extractDomain(prompt); // Extract domain for targeted scraping
          return activeInternetScraper
              .scrapeKnowledge(domain, List.of(cleanQuery.split("\\s+")))
              .next()
              .map(issue -> issue.getSolution())
              .timeout(Duration.ofSeconds(10))
              .onErrorResume(err -> {
                  logger.warn("[BRAIN TIER 3] Web scraping failed: {}", err.getMessage());
                  return Mono.just("No web search results available.");
              });
        })
        .flatMap(mono -> mono); // Flatten the Mono<Mono<String>>
  }

  /**
   * Demo endpoint - Guest access without authentication to test 4-layer
   * resilience.
   */
  public Mono<String> thinkDemo(String prompt) {
    return think(TASK_GENERAL, prompt, "DEMO_NO_SIGNATURE");
  }

  // ══════════════════════════════════════════════════════════
  // FEATURE-SPECIFIC CONVENIENCE METHODS
  // (প্রতিটি feature এর জন্য clean, named API)
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
   * Phase 1: Hub Identification for Super-Hub Orchestrator. Maps user intent to a
   * specific Hub ID.
   */
  public Mono<String> identifyHub(String query) {
    return Mono.fromCallable(
        () -> {
          Map<String, String> hubInfo = learningOrchestrator.identifyBestHub(query);
          String hubName = hubInfo.get("hub").toLowerCase();

          if (hubName.contains("development"))
            return "dev_hub";
          if (hubName.contains("language") || hubName.contains("marketing"))
            return "lang_hub";
          if (hubName.contains("security"))
            return "security_hub";
          if (hubName.contains("visual")
              || hubName.contains("voice")
              || hubName.contains("multimodal"))
            return "multimodal_hub";
          if (hubName.contains("memory"))
            return "memory_hub";

          return "lang_hub"; // Default fallback
        });
  }

  // ══════════════════════════════════════════════════════════
  // ADMIN STATS
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
  // PHASE 2: LEARNING ORCHESTRATOR INTEGRATION
  // ══════════════════════════════════════════════════════════

  /**
   * Phase 2: Expose orchestrator learning stats for admin dashboard. Returns
   * intent classification
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
  // INTERNAL HELPERS
  // ══════════════════════════════════════════════════════════

  private void trackCall(String task) {
    taskCallCount.merge(task, 1L, Long::sum);
  }

  private void trackSuccess(String task) {
    taskSuccessCount.merge(task, 1L, Long::sum);
  }

  private String truncate(String text) {
    if (text == null)
      return "";
    return text.length() > 100 ? text.substring(0, 100) + "..." : text;
  }

  /**
   * AI Judge — determines whether a merged response actually answers the user's
   * question. Uses ChickenBrain to avoid brittle string matching.
   */
  private Mono<Boolean> isInsufficientResponse(String response, String userPrompt) {
    if (response == null || response.trim().isEmpty()) {
      return Mono.just(true);
    }
    String aiJudgePrompt =
        "You are a strict quality judge for SupremeAI. "
            + "Given the user question and a candidate answer, decide if the answer "
            + "is SUFFICIENT or INSUFFICIENT.\n\n"
            + "INSUFFICIENT means: the answer is empty, off-topic, contradictory, or "
            + "does not actually address the question.\n\n"
            + "SUFFICIENT means: the answer contains relevant, factual information that "
            + "directly addresses the user question.\n\n"
            + "Respond with EXACTLY ONE WORD: either \"SUFFICIENT\" or \"INSUFFICIENT\".\n\n"
            + "USER QUESTION: \"" + userPrompt + "\"\n\n"
            + "CANDIDATE ANSWER:\n" + response;

    return fallbackOrchestrator
        .executeWithSupremeIntelligence("CHAT", "quality_judge", aiJudgePrompt)
        .map(judgment -> {
          String lower = judgment.trim().toUpperCase();
          boolean insufficient = lower.contains("INSUFFICIENT") || lower.contains("NO") || lower.contains("FAIL");
          logger.info("[ChickenBrain Judge] Quality judgment: {} -> {}", insufficient ? "INSUFFICIENT" : "SUFFICIENT", truncate(judgment));
          return insufficient;
        })
        .onErrorResume(e -> {
          logger.warn("[ChickenBrain Judge] Evaluation failed, falling back to heuristic: {}", e.getMessage());
          return Mono.just(heuristicInsufficientCheck(response));
        });
  }

  private boolean heuristicInsufficientCheck(String response) {
    if (response == null || response.trim().isEmpty()) {
      return true;
    }
    String lowerResponse = response.toLowerCase();
    return lowerResponse.contains("no web search results available.")
        || lowerResponse.contains("no local database learning found.")
        || lowerResponse.contains("i couldn't find any information")
        || lowerResponse.contains("failed to get information")
        || lowerResponse.contains("no information found")
        || lowerResponse.contains("আমি কোনো তথ্য খুঁজে পাইনি")
        || lowerResponse.contains("আমি দুঃখিত, আপনার অনুরোধটি প্রক্রিয়া করতে পারিনি।")
        || lowerResponse.contains("ChickenBrain Local Merge]");
  }

  /**
   * Extract meaningful keywords from the user's message for targeted scraping.
   * Removes stop words
   * and short words, keeps technical terms.
   */
  private List<String> extractKeywords(String message) {
    Set<String> stopWords = Set.of(
        "what", "is", "the", "a", "an", "in", "on", "of", "to", "for", "and", "or", "how", "do",
        "does", "can", "could", "would", "should", "this", "that", "with", "from", "by", "at",
        "it", "be", "are", "was", "were", "been", "being", "have", "has", "had", "will", "i",
        "me", "my", "you", "your", "we", "our", "they", "them", "tell", "explain", "about",
        "please", "help", "want", "need", "কি", "কী", "কেন", "কোথায়", "কিভাবে", "কীভাবে",
        "আমি", "আমার", "এটা", "এটি", "সেটা", "তুমি", "তোমার", "করো", "করুন", "বলো", "বলুন");

    return Arrays.stream(message.toLowerCase().split("\\s+"))
        .map(
            w -> w.replaceAll(
                "[^a-zA-Z0-9\\u0980-\\u09FF#+.-]", "")) // Keep Bengali, English, special
        .filter(w -> w.length() > 2)
        .filter(w -> !stopWords.contains(w))
        .distinct()
        .limit(5)
        .collect(Collectors.toList());
  }

  /** Extract the primary domain/topic from the message for scraping. */
  private String extractDomain(String message) {
    String lower = message.toLowerCase();

    // Tech-specific domain detection
    Map<String, String> domainMap = new LinkedHashMap<>();
    domainMap.put("react", "React JavaScript framework");
    domainMap.put("spring boot", "Spring Boot Java framework");
    domainMap.put("spring", "Spring framework Java");
    domainMap.put("python", "Python programming language");
    domainMap.put("javascript", "JavaScript programming");
    domainMap.put("typescript", "TypeScript programming");
    domainMap.put("java", "Java programming language");
    domainMap.put("docker", "Docker containerization");
    domainMap.put("kubernetes", "Kubernetes container orchestration");
    domainMap.put("database", "database management system");
    domainMap.put("postgresql", "PostgreSQL database");
    domainMap.put("mongodb", "MongoDB NoSQL database");
    domainMap.put("sql", "SQL database query language");
    domainMap.put("api", "REST API development");
    domainMap.put("machine learning", "machine learning artificial intelligence");
    domainMap.put("deep learning", "deep learning neural network");
    domainMap.put("flutter", "Flutter mobile development");
    domainMap.put("node", "Node.js JavaScript runtime");
    domainMap.put("css", "CSS web styling");
    domainMap.put("html", "HTML web markup");
    domainMap.put("git", "Git version control");
    domainMap.put("linux", "Linux operating system");
    domainMap.put("aws", "Amazon Web Services cloud");
    domainMap.put("gcp", "Google Cloud Platform");
    domainMap.put("firebase", "Firebase Google platform");

    for (Map.Entry<String, String> entry : domainMap.entrySet()) {
      if (lower.contains(entry.getKey())) {
        return entry.getValue();
      }
    }

    // Fallback: use the longest meaningful words as domain
    List<String> keywords = extractKeywords(message);
    return keywords.isEmpty() ? message : String.join(" ", keywords);
  }
}
