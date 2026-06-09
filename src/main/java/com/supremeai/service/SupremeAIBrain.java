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

@Service
public class SupremeAIBrain {

  private static final Logger logger = LoggerFactory.getLogger(SupremeAIBrain.class);

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

  @Autowired private DynamicSignatureRegistry signatureRegistry;

  @Autowired private SupremeLearningOrchestrator learningOrchestrator;

  @Autowired private ActiveInternetScraper activeInternetScraper;

  @Autowired private BrowserService browserService;

  @Autowired private SolutionMemoryRepository solutionMemoryRepository;

  @Autowired private ProviderRepository providerRepository;

  @Autowired private UnifiedOfflineKnowledgeService unifiedOfflineKnowledgeService;

  @Autowired private EnhancedLearningService enhancedLearningService;

  @Autowired private ObjectMapper objectMapper;

  private final Map<String, Long> taskCallCount = new ConcurrentHashMap<>();
  private final Map<String, Long> taskSuccessCount = new ConcurrentHashMap<>();

  public Mono<String> think(String prompt) {
    return think(TASK_GENERAL, prompt);
  }

  public Mono<String> think(String taskCategory, String prompt) {
    return think(taskCategory, prompt, "NO_SIGNATURE");
  }

  public Mono<String> think(String taskCategory, String prompt, String errorSignature) {
    if (prompt == null || prompt.isBlank()) {
      return Mono.just("[SupremeAI Brain] Prompt is empty. Please provide input.");
    }

    String task = taskCategory != null ? taskCategory.toUpperCase() : TASK_GENERAL;
    trackCall(task);
    long startTime = System.currentTimeMillis();

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

    boolean isComplex = isComplexTask(task, prompt);

    if (!isComplex) {
      logger.info(
          "🧠 [CORE KNOWLEDGE] Decision: Routing NORMAL query to Browser and Database Learning in parallel.");

      Mono<String> browserMono =
          tryBrowserScraping(task, prompt).onErrorReturn(getDynamicFallback("WEB_NO_RESULT"));

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
              .defaultIfEmpty(getDynamicFallback("DB_NO_RESULT"))
              .onErrorReturn(getDynamicFallback("DB_ERROR"));

      return Mono.zip(browserMono, dbMemoryMono)
          .flatMap(
              tuple ->
                  chickenBrainMerge(tuple.getT1(), tuple.getT2(), prompt, task, errorSignature))
          .doOnNext(response -> trackSuccess(task));
    } else {
      logger.info(
          "🧠 [CORE KNOWLEDGE] Decision: Routing COMPLEX query to Deployed AI Model, and system will learn the output.");

      return fallbackOrchestrator
          .executeWithSupremeIntelligence(task, errorSignature, prompt)
          .flatMap(
              aiResponse -> {
                logger.info(
                    "🧠 [CORE KNOWLEDGE] Decision: System is learning the complex AI response");
                return enhancedLearningService
                    .learnFromInteraction("system", prompt, aiResponse)
                    .thenReturn(aiResponse);
              })
          .onErrorResume(
              e -> {
                logger.warn("Complex AI run failed, falling back: {}", e.getMessage());
                return Mono.just(getDynamicFallback("COMPLEX_AI_ERROR"));
              })
          .doOnNext(response -> trackSuccess(task));
    }
  }

  private boolean isComplexTask(String task, String prompt) {
    Set<String> complexPatterns = signatureRegistry.getSignatures("COMPLEX_TASK_PATTERNS");
    if (complexPatterns != null && !complexPatterns.isEmpty()) {
      String lowerTask = task.toLowerCase();
      String lowerPrompt = prompt.toLowerCase();
      return complexPatterns.stream().anyMatch(pattern ->
          lowerTask.contains(pattern) || lowerPrompt.contains(pattern));
    }

    boolean isComplex =
        task.contains("CODE_")
            || task.contains("REVIEW")
            || task.contains("SECURITY")
            || task.contains("TESTING")
            || task.contains("REASONING")
            || prompt.contains("generate")
            || prompt.contains("write code")
            || prompt.contains("complex");

    return isComplex;
  }

  private String getDynamicFallback(String key) {
    return signatureRegistry.getSignatures("FALLBACK_MESSAGES")
        .stream()
        .filter(s -> s.startsWith(key + ":"))
        .map(s -> s.substring(key.length() + 1))
        .findFirst()
        .orElseGet(() -> getGenericFallback(key));
  }

  private String getGenericFallback(String key) {
    return switch (key) {
      case "WEB_NO_RESULT" -> "No web search results available.";
      case "DB_NO_RESULT" -> "No local database learning found.";
      case "DB_ERROR" -> "Error accessing knowledge base.";
      case "COMPLEX_AI_ERROR" -> "Failed to execute complex logic on deployed AI model.";
      default -> "No information available.";
    };
  }

  private Mono<String> chickenBrainMerge(
      String browserData, String dbData, String prompt, String task, String errorSignature) {
    logger.info(
        "🧠 [ChickenBrain] Merging browser data and database learning for query: {}",
        truncate(prompt));

    if (isInsufficientResponse(browserData, prompt).block()
        && isInsufficientResponse(dbData, prompt).block()) {
      logger.info(
          "🧠 [ChickenBrain] Both browser and DB data are insufficient. Escalating to complex AI.");
      return escalateToComplexAI(task, prompt, errorSignature);
    }

    String promptWithContext =
        String.format(
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
        .flatMap(
            mergedResponse ->
                isInsufficientResponse(mergedResponse, prompt)
                    .flatMap(
                        insufficient -> {
                          if (insufficient) {
                            logger.info(
                                "🧠 [ChickenBrain] Merged response is insufficient per AI Judge. Escalating to complex AI.");
                            return escalateToComplexAI(task, prompt, errorSignature);
                          }
                          return Mono.just(mergedResponse);
                        }))
        .onErrorResume(
            e ->
                Mono.just(
                    "[ChickenBrain Local Merge]\n"
                        + "We found this in search:\n"
                        + browserData
                        + "\n\nAnd this in our database:\n"
                        + dbData));
  }

  private Mono<String> escalateToComplexAI(String task, String prompt, String errorSignature) {
    return fallbackOrchestrator
        .executeWithSupremeIntelligence(task, errorSignature, prompt)
        .flatMap(
            aiResponse ->
                enhancedLearningService
                    .learnFromInteraction("system", prompt, aiResponse)
                    .thenReturn(aiResponse))
        .onErrorResume(
            e -> {
              logger.warn("Escalated complex AI run failed: {}", e.getMessage());
              return Mono.just(getDynamicFallback("COMPLEX_AI_ERROR"));
            });
  }

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
              String domain = extractDynamicDomain(prompt);
              return activeInternetScraper
                  .scrapeKnowledge(domain, List.of(cleanQuery.split("\\s+")))
                  .next()
                  .map(issue -> issue.getSolution())
                  .timeout(Duration.ofSeconds(10))
                  .onErrorResume(
                      err -> {
                        logger.warn("[BRAIN TIER 3] Web scraping failed: {}", err.getMessage());
                        return Mono.just(getDynamicFallback("WEB_NO_RESULT"));
                      });
            })
        .flatMap(mono -> mono);
  }

  public Mono<String> thinkDemo(String prompt) {
    return think(TASK_GENERAL, prompt, "DEMO_NO_SIGNATURE");
  }

  public Mono<String> generateCode(String specification) {
    return think(TASK_CODE_GENERATION, specification);
  }

  public Mono<String> reviewCode(String code) {
    return think(TASK_CODE_REVIEW, "Review this code and provide detailed feedback:\n\n" + code);
  }

  public Mono<String> fixError(String errorDescription, String errorSignature) {
    return think(
        TASK_SELF_HEALING,
        "Analyze this error and provide a fix:\n\n" + errorDescription,
        errorSignature);
  }

  public Mono<String> translate(String text, String targetLanguage) {
    return think(
        TASK_TRANSLATION,
        String.format("Translate the following to %s:\n\n%s", targetLanguage, text));
  }

  public Mono<String> analyzeSecurity(String code) {
    return think(
        TASK_SECURITY,
        "Perform a security audit on this code and identify vulnerabilities:\n\n" + code);
  }

  public Mono<String> generateTests(String code) {
    return think(TASK_TESTING, "Generate comprehensive unit tests for this code:\n\n" + code);
  }

  public Mono<String> analyze(String topic) {
    return think(TASK_ANALYSIS, topic);
  }

  public Mono<String> analyzeImage(String imageDescription) {
    return think(TASK_VISION, imageDescription);
  }

  public Mono<String> chat(String message) {
    return think(TASK_CHAT, message);
  }

  public Mono<String> orchestrate(String agentTask) {
    return think(TASK_ORCHESTRATION, agentTask);
  }

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

          return "lang_hub";
        });
  }

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
            + "USER QUESTION: \""
            + userPrompt
            + "\"\n\n"
            + "CANDIDATE ANSWER:\n"
            + response;

    return fallbackOrchestrator
        .executeWithSupremeIntelligence("CHAT", "quality_judge", aiJudgePrompt)
        .map(
            judgment -> {
              String lower = judgment.trim().toUpperCase();
              boolean insufficient =
                  lower.contains("INSUFFICIENT") || lower.contains("NO") || lower.contains("FAIL");
              logger.info(
                  "[ChickenBrain Judge] Quality judgment: {} -> {}",
                  insufficient ? "INSUFFICIENT" : "SUFFICIENT",
                  truncate(judgment));
              return insufficient;
            })
        .onErrorResume(
            e -> {
              logger.warn(
                  "[ChickenBrain Judge] Evaluation failed, falling back to dynamic rule: {}",
                  e.getMessage());
              return Mono.just(dynamicHeuristicCheck(response));
            });
  }

  private boolean dynamicHeuristicCheck(String response) {
    Set<String> insufficientMarkers = signatureRegistry.getSignatures("INSUFFICIENT_MARKERS");
    if (insufficientMarkers != null && !insufficientMarkers.isEmpty()) {
      String lowerResponse = response.toLowerCase();
      return insufficientMarkers.stream()
          .anyMatch(marker -> lowerResponse.contains(marker.toLowerCase()));
    }
    return response == null || response.trim().isEmpty();
  }

  private List<String> extractKeywords(String message) {
    Set<String> stopWords = signatureRegistry.getSignatures("STOP_WORDS");
    Set<String> effectiveStopWords = stopWords != null && !stopWords.isEmpty()
        ? stopWords
        : Set.of("the", "a", "an", "and", "or", "is", "are", "was", "were", "be", "been");

    return Arrays.stream(message.toLowerCase().split("\\s+"))
        .map(
            w ->
                w.replaceAll(
                    "[^a-zA-Z0-9\\u0980-\\u09FF#+.-]", ""))
        .filter(w -> w.length() > 2)
        .filter(w -> !effectiveStopWords.contains(w))
        .distinct()
        .limit(5)
        .collect(Collectors.toList());
  }

  private String extractDynamicDomain(String message) {
    Set<String> domains = signatureRegistry.getSignatures("DOMAIN_MAPPINGS");
    if (domains != null && !domains.isEmpty()) {
      String lower = message.toLowerCase();
      return domains.stream()
          .map(d -> d.contains(":") ? d.split(":")[0].trim() : d)
          .filter(keyword -> lower.contains(keyword.toLowerCase()))
          .findFirst()
          .orElseGet(() -> extractKeywords(message).isEmpty() ? message : String.join(" ", extractKeywords(message)));
    }
    return extractKeywords(message).isEmpty() ? message : String.join(" ", extractKeywords(message));
  }
}