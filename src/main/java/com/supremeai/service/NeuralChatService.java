package com.supremeai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.learning.active.ActiveInternetScraper;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * ═══════════════════════════════════════════════════════════════════════════════ NeuralChatService
 * — The Intelligent Strategic Router (Decision Maker)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * <p>Core Knowledge's new role: Strategic Router — decides WHERE to get answers instead of
 * providing fixed answers.
 *
 * <p>Strategy (from core_knowledge.json): - BROWSER_SCRAPE: Real-time web data from multiple
 * sources - CLOUD_AI: Complex logic or creative answers via deployed cloud AI - CORE_ONLY: Instant
 * local response, no external calls needed
 *
 * <p>Flow: 1. Identify strategy from core knowledge 2. If CLOUD_AI → route to cloud AI 3. If
 * BROWSER_SCRAPE → fetch domains from DB (scrape mappings) → scrape all sources → merge with
 * ChickenBrain AI 4. If CORE_ONLY → return instant response
 *
 * <p>Tiers: MERGED | CORE_ONLY | WEB_ONLY | HYBRID | CLOUD
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Service
public class NeuralChatService {

  private static final Logger log = LoggerFactory.getLogger(NeuralChatService.class);

  private final SupremeLearningOrchestrator learningOrchestrator;
  private final ActiveInternetScraper internetScraper;
  private final ResourceLoader resourceLoader;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private List<Map<String, Object>> categoryMappings = new ArrayList<>();

  private static final Duration SCRAPE_TIMEOUT = Duration.ofSeconds(12);
  private static final int MIN_USEFUL_SNIPPET_LENGTH = 30;

  @Autowired
  public NeuralChatService(
      SupremeLearningOrchestrator learningOrchestrator,
      ActiveInternetScraper internetScraper,
      ResourceLoader resourceLoader) {
    this.learningOrchestrator = learningOrchestrator;
    this.internetScraper = internetScraper;
    this.resourceLoader = resourceLoader;
    loadCategoryMappings();
    log.info("[NeuralChat] Initialized — Strategic Router (Core Knowledge → Strategy → Action)");
  }

  /** Load scrape domain mappings from classpath JSON on startup. */
  private void loadCategoryMappings() {
    try {
      var resource = resourceLoader.getResource("classpath:scrape_domain_mappings.json");
      if (resource.exists()) {
        categoryMappings =
            objectMapper.readValue(
                resource.getInputStream(), new TypeReference<List<Map<String, Object>>>() {});
        log.info("[NeuralChat] Loaded {} category domain mappings", categoryMappings.size());
      } else {
        log.warn("[NeuralChat] scrape_domain_mappings.json not found");
      }
    } catch (Exception e) {
      log.error("[NeuralChat] Failed to load category mappings: {}", e.getMessage());
    }
  }

  /** Public API to reload category mappings (call after file updates). */
  public void reloadCategoryMappings() {
    loadCategoryMappings();
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  MAIN ENTRY POINT — Strategic Router
  // ══════════════════════════════════════════════════════════════════════════

  /**
   * Generate an intelligent response by following the strategy from core knowledge.
   *
   * @param userMessage The user's question or prompt
   * @return Mono<NeuralResponse> containing the merged intelligent response
   */
  public Mono<NeuralResponse> generateIntelligentResponse(String userMessage) {
    if (userMessage == null || userMessage.trim().isEmpty()) {
      return Mono.just(
          new NeuralResponse(
              "আমি সুপ্রিমএআই। আপনার প্রশ্ন লিখুন, আমি সাহায্য করব।",
              List.of(),
              0.0,
              "NONE",
              "empty_input"));
    }

    log.info("[NeuralChat] Processing: \"{}\"", truncate(userMessage, 80));

    return identifyStrategy(userMessage)
        .flatMap(
            strategy -> {
              if ("CLOUD_AI".equals(strategy)) {
                log.info("[NeuralChat] Strategy: Option 2 - Deployed AI at Cloud Server");
                return handleCloudAI(userMessage);
              } else if ("CORE_ONLY".equals(strategy)) {
                log.info(
                    "[NeuralChat] Strategy: Option 1a - Core Knowledge Only (no external calls)");
                return handleCoreOnly(userMessage);
              } else {
                log.info("[NeuralChat] Strategy: Option 1 - Dynamic Browser Scraping");
                return handleBrowserScraping(userMessage);
              }
            });
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  STRATEGY IDENTIFICATION (from core_knowledge.json)
  // ══════════════════════════════════════════════════════════════════════════

  private Mono<String> identifyStrategy(String message) {
    try {
      String strategy = learningOrchestrator.findCoreKnowledgeStrategy(message);
      log.info("[NeuralChat] Strategy from core knowledge: {}", strategy);
      return Mono.just(strategy);
    } catch (Exception e) {
      log.warn(
          "[NeuralChat] Strategy lookup failed, defaulting to BROWSER_SCRAPE: {}", e.getMessage());
      return Mono.just("BROWSER_SCRAPE");
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  STRATEGY HANDLERS
  // ══════════════════════════════════════════════════════════════════════════

  private Mono<NeuralResponse> handleCloudAI(String userMessage) {
    return Mono.just(
        new NeuralResponse(
            "Clo dAI processing not yet connected — use Browser Scraping for real-time data.",
            List.of("Cloud AI Provider"),
            0.85,
            "CLOUD",
            "cloud_ai_stub"));
  }

  private Mono<NeuralResponse> handleCoreOnly(String userMessage) {
    return Mono.fromCallable(
        () -> {
          try {
            String solution = learningOrchestrator.findCoreKnowledgeSolution(userMessage);
            if (solution != null && !solution.isEmpty()) {
              log.info("[NeuralChat] ✅ CORE_ONLY HIT");
              return new NeuralResponse(
                  solution, List.of("Core Knowledge"), 0.95, "CORE_ONLY", "core_knowledge");
            }
          } catch (Exception e) {
            log.warn("[NeuralChat] Core knowledge lookup failed: {}", e.getMessage());
          }
          return new NeuralResponse(
              "আমি এখনও এই প্রশ্নের সঠিক উত্তর জানি না। অনুর partnership করছি।",
              List.of(),
              0.3,
              "CORE_ONLY",
              "core_only_miss");
        });
  }

  private Mono<NeuralResponse> handleBrowserScraping(String userMessage) {
    List<String> keywords = extractKeywords(userMessage);
    return fetchBestDomainsFromDB(userMessage)
        .flatMap(
            domains -> {
              List<Mono<List<ActiveInternetScraper.ScrapedIssue>>> scrapers =
                  domains.stream()
                      .limit(3)
                      .map(
                          domain ->
                              internetScraper
                                  .scrapeKnowledge(domain, keywords)
                                  .collectList()
                                  .timeout(Duration.ofSeconds(10))
                                  .onErrorReturn(List.of()))
                      .toList();
              return Mono.zip(
                      scrapers,
                      resultsArr -> {
                        List<ActiveInternetScraper.ScrapedIssue> allResults = new ArrayList<>();
                        for (Object r : resultsArr) {
                          if (r instanceof List<?> list) {
                            list.stream()
                                .filter(item -> item instanceof ActiveInternetScraper.ScrapedIssue)
                                .map(item -> (ActiveInternetScraper.ScrapedIssue) item)
                                .forEach(allResults::add);
                          }
                        }
                        return allResults;
                      })
                  .flatMap(
                      results -> {
                        if (results.isEmpty()) {
                          return Mono.just(
                              new NeuralResponse(
                                  "দুঃখিত, কিছু খুঁজে পাওয়া যায়নি।",
                                  List.of(),
                                  0.0,
                                  "NONE",
                                  "no_results"));
                        }
                        return mergeWithChickenBrainAI(userMessage, results);
                      });
            });
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  DATABASE-DRIVEN DOMAIN SELECTION (from scrape_domain_mappings.json)
  // ══════════════════════════════════════════════════════════════════════════

  private Mono<List<String>> fetchBestDomainsFromDB(String query) {
    return Mono.fromCallable(() -> selectDomainsFromCategoryMapping(query));
  }

  private List<String> selectDomainsFromCategoryMapping(String query) {
    if (categoryMappings.isEmpty()) {
      return List.of("wikipedia.org", "stackoverflow.com", "github.com");
    }
    String lowerQuery = query.toLowerCase(Locale.ROOT);
    String bestCategoryId = null;
    int bestMatchCount = 0;
    for (Map<String, Object> category : categoryMappings) {
      List<String> keywords = (List<String>) category.getOrDefault("keywords", List.of());
      int matchCount = 0;
      for (String kw : keywords) {
        if (lowerQuery.contains(kw.toLowerCase())) {
          matchCount++;
        }
      }
      if (matchCount > bestMatchCount) {
        bestMatchCount = matchCount;
        bestCategoryId = (String) category.get("categoryId");
      }
    }
    if (bestCategoryId != null) {
      for (Map<String, Object> category : categoryMappings) {
        if (bestCategoryId.equals(category.get("categoryId"))) {
          List<Map<String, String>> targetWebsites =
              (List<Map<String, String>>) category.get("targetWebsites");
          if (targetWebsites != null && !targetWebsites.isEmpty()) {
            List<String> domains =
                targetWebsites.stream()
                    .map(w -> w.getOrDefault("domain", ""))
                    .filter(d -> !d.isBlank())
                    .limit(3)
                    .collect(Collectors.toList());
            if (!domains.isEmpty()) {
              log.info(
                  "[NeuralChat] {} domains selected from category '{}': {}",
                  domains.size(),
                  bestCategoryId,
                  domains);
              return domains;
            }
          }
        }
      }
    }
    List<String> fallback = List.of("wikipedia.org", "stackoverflow.com", "github.com");
    log.info("[NeuralChat] No category match, using fallback domains: {}", fallback);
    return fallback;
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  CHICKENBRAIN AI MULTI-SOURCE MERGER
  // ══════════════════════════════════════════════════════════════════════════

  private Mono<NeuralResponse> mergeWithChickenBrainAI(
      String query, List<ActiveInternetScraper.ScrapedIssue> results) {
    List<ActiveInternetScraper.ScrapedIssue> usefulResults =
        results.stream()
            .filter(
                r ->
                    r.getSolution() != null
                        && r.getSolution().length() >= MIN_USEFUL_SNIPPET_LENGTH)
            .sorted((a, b) -> Double.compare(b.getSourceAuthority(), a.getSourceAuthority()))
            .limit(3)
            .collect(Collectors.toList());

    if (usefulResults.isEmpty()) {
      return Mono.just(
          new NeuralResponse(
              "পর্যাপ্ত তথ্য পাওয়া যায়নি।", List.of(), 0.0, "NONE", "no_useful_results"));
    }

    String mergedContent =
        usefulResults.stream()
            .map(r -> "[" + r.getSource() + "]\n" + r.getSolution())
            .collect(Collectors.joining("\n\n"));

    String chickenBrainPrompt =
        "You are ChickenBrain, a high-precision hybrid intelligence synthesizer for SupremeAI. "
            + "Merge the following multiple web search results into ONE cohesive, expert-level response.\n\n"
            + "STRICT INSTRUCTIONS:\n"
            + "1. Prioritize more authoritative and recent sources.\n"
            + "2. Resolve contradictions by favoring the more reliable source.\n"
            + "3. Use professional Markdown formatting.\n"
            + "4. Answer in the same language as the user question.\n"
            + "5. If information is insufficient, state what is missing.\n\n"
            + "USER QUESTION: \""
            + query
            + "\"\n\n"
            + "SEARCH RESULTS:\n"
            + mergedContent;

    return Mono.just(
        new NeuralResponse(
            mergedContent,
            usefulResults.stream()
                .map(r -> r.getSource() + " (" + r.getTitle() + ")")
                .collect(Collectors.toList()),
            calculateWebConfidence(usefulResults),
            "HYBRID",
            "chickenbrain_ai"));
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  KEYWORD & UTILITY HELPERS
  // ══════════════════════════════════════════════════════════════════════════

  private List<String> extractKeywords(String message) {
    Set<String> stopWords =
        Set.of(
            "what", "is", "the", "a", "an", "in", "on", "of", "to", "for", "and", "or", "how", "do",
            "does", "can", "could", "would", "should", "this", "that", "with", "from", "by", "at",
            "it", "be", "are", "was", "were", "been", "being", "have", "has", "had", "will", "i",
            "me", "my", "you", "your", "we", "our", "they", "them", "tell", "explain", "about",
            "please", "help", "want", "need", "কি", "কী", "কেন", "কোথায়", "কিভাবে", "কীভাবে",
            "আমি", "আমার", "এটা", "এটি", "সেটা", "তুমি", "তোমার", "করো", "করুন", "বলো", "বলুন");

    return Arrays.stream(message.toLowerCase().split("\\s+"))
        .map(w -> w.replaceAll("[^a-zA-Z0-9\\u0980-\\u09FF#+.-]", ""))
        .filter(w -> w.length() > 2)
        .filter(w -> !stopWords.contains(w))
        .distinct()
        .limit(5)
        .collect(Collectors.toList());
  }

  private double calculateWebConfidence(List<ActiveInternetScraper.ScrapedIssue> webResults) {
    return webResults.stream().mapToDouble(r -> r.getSourceAuthority()).average().orElse(0.5);
  }

  private String truncate(String text, int maxLength) {
    if (text == null) return "";
    return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  RESPONSE DATA CLASS
  // ══════════════════════════════════════════════════════════════════════════

  /** Encapsulates the neural chat response with metadata about sources and confidence. */
  public static class NeuralResponse {
    private final String answer;
    private final List<String> sources;
    private final double confidence;
    private final String tier;
    private final String pipeline;

    public NeuralResponse(
        String answer, List<String> sources, double confidence, String tier, String pipeline) {
      this.answer = answer;
      this.sources = sources != null ? sources : List.of();
      this.confidence = confidence;
      this.tier = tier;
      this.pipeline = pipeline;
    }

    public String getAnswer() {
      return answer;
    }

    public List<String> getSources() {
      return sources;
    }

    public double getConfidence() {
      return confidence;
    }

    public String getTier() {
      return tier;
    }

    public String getPipeline() {
      return pipeline;
    }

    /** Convert to a Map suitable for JSON API responses. */
    public Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("response", answer);
      map.put("confidence", confidence);
      map.put("tier", tier);
      map.put("pipeline", pipeline);
      map.put("sources", sources);
      map.put("sourceCount", sources.size());
      map.put("timestamp", java.time.Instant.now().toString());
      return map;
    }
  }
}
