package com.supremeai.service;

import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.learning.active.ActiveInternetScraper;
import com.supremeai.learning.active.QueryClassifier;
import com.supremeai.provider.StubLocalProvider;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * ═══════════════════════════════════════════════════════════════════════════════ NeuralChatService
 * — The Intelligent Unified Response Engine
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * <p>Merges three knowledge tiers into ONE intelligent response:
 *
 * <p>Tier 1: core_knowledge.json → Instant, offline, always available Tier 2: Browser Scraping →
 * Wikipedia, StackOverflow, MDN, GitHub, arXiv Tier 3: StubLocalProvider → Rule-based
 * Bengali/English fallback
 *
 * <p>Flow: User asks "What is React?" → Tier 1: core_knowledge.json has React patterns → use as
 * base → Tier 2: Wikipedia scrape → "React is a JavaScript library..." → enrich → Merge both into a
 * comprehensive, intelligent answer → If both fail → Tier 3: StubLocalProvider gives basic response
 *
 * <p>Design Principles: ✅ No external API keys required ✅ Works fully offline (core_knowledge
 * always available) ✅ Browser scraping enhances answers when internet is available ✅ Answers are
 * synthesized, not raw dumps ✅ Supports both Bengali and English
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Service
public class NeuralChatService {

  private static final Logger log = LoggerFactory.getLogger(NeuralChatService.class);

  private final SupremeLearningOrchestrator learningOrchestrator;
  private final ActiveInternetScraper internetScraper;
  private final QueryClassifier queryClassifier;
  private final StubLocalProvider stubLocalProvider;
  private final UnifiedOfflineKnowledgeService unifiedOfflineKnowledgeService;
  private final WebClient webClient;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  /** Maximum time to wait for browser scraping before returning core knowledge only */
  private static final Duration SCRAPE_TIMEOUT = Duration.ofSeconds(12);

  /** Minimum snippet length to consider a scraped result useful */
  private static final int MIN_USEFUL_SNIPPET_LENGTH = 30;

  /** Maximum number of web sources to include in a merged response */
  private static final int MAX_WEB_SOURCES = 3;

  @Autowired
  public NeuralChatService(
      SupremeLearningOrchestrator learningOrchestrator,
      ActiveInternetScraper internetScraper,
      QueryClassifier queryClassifier,
      StubLocalProvider stubLocalProvider,
      UnifiedOfflineKnowledgeService unifiedOfflineKnowledgeService,
      WebClient.Builder webClientBuilder) {
    this.learningOrchestrator = learningOrchestrator;
    this.internetScraper = internetScraper;
    this.queryClassifier = queryClassifier;
    this.stubLocalProvider = stubLocalProvider;
    this.unifiedOfflineKnowledgeService = unifiedOfflineKnowledgeService;
    this.webClient = webClientBuilder.build();
    this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    log.info(
        "[NeuralChat] Initialized — Tier 1 (Core Knowledge) + Tier 2 (Browser) + Tier 2.5 (DDG Instant) + Tier 3 (StubLocal) merged pipeline");
  }

  /**
   * Generate an intelligent response by merging all knowledge tiers.
   *
   * @param userMessage The user's question or prompt
   * @return Mono<NeuralResponse> containing the merged intelligent response
   */
  public Mono<NeuralResponse> generateIntelligentResponse(String userMessage) {
    if (userMessage == null || userMessage.trim().isEmpty()) {
      return Mono.just(
          NeuralResponse.fromStub(
              "আমি সুপ্রিমএআই। আপনার প্রশ্ন লিখুন, আমি সাহায্য করব।", "empty_input"));
    }

    log.info("[NeuralChat] Processing: \"{}\"", truncate(userMessage, 80));

    // ── TIER 1: Core Knowledge (instant, offline) ───────────────────────
    String coreAnswer = findCoreKnowledge(userMessage);

    if (coreAnswer != null && shouldReturnCoreOnlyWithoutScraping(userMessage)) {
      log.info("[NeuralChat] Returning core knowledge directly without external scraping.");
      return Mono.just(
          new NeuralResponse(
              coreAnswer, List.of("Core Knowledge"), 0.85, "CORE_ONLY", "core_knowledge"));
    }

    // ── Classify the query for targeted scraping ────────────────────────
    List<String> keywords = extractKeywords(userMessage);
    String domain = extractDomain(userMessage);

    // ── TIER 2: Browser Scraping (async, may timeout) ───────────────────
    Mono<List<ActiveInternetScraper.ScrapedIssue>> scrapeMono =
        internetScraper
            .scrapeKnowledge(domain, keywords)
            .collectList()
            .timeout(SCRAPE_TIMEOUT)
            .onErrorResume(
                e -> {
                  log.warn("[NeuralChat] Browser scraping failed or timed out: {}", e.getMessage());
                  return Mono.just(Collections.emptyList());
                });

    return scrapeMono.flatMap(
        scrapedResults -> {
          // ── MERGE TIERS ─────────────────────────────────────────────────
          return mergeKnowledgeTiers(userMessage, coreAnswer, scrapedResults);
        });
  }

  /** Tier 1: Search core_knowledge.json for a matching answer. */
  private String findCoreKnowledge(String query) {
    try {
      String solution = learningOrchestrator.findCoreKnowledgeSolution(query);
      if (solution != null && !solution.isEmpty()) {
        log.info("[NeuralChat] ✅ Tier 1 HIT — Core Knowledge matched");
        return solution;
      }
    } catch (Exception e) {
      log.warn("[NeuralChat] Tier 1 lookup failed: {}", e.getMessage());
    }
    log.info("[NeuralChat] ⚪ Tier 1 MISS — No core knowledge match");
    return null;
  }

  /**
   * Merge all knowledge tiers into a single, intelligent response.
   *
   * <p>Strategy: - If core_knowledge has a strong match AND web has enriching data → merge both -
   * If only core_knowledge matches → use it directly - If only web has results → synthesize from
   * web - If neither → fall back to StubLocalProvider (Tier 3)
   */
  private Mono<NeuralResponse> mergeKnowledgeTiers(
      String userMessage, String coreAnswer, List<ActiveInternetScraper.ScrapedIssue> webResults) {
    // Filter web results to only useful ones
    List<ActiveInternetScraper.ScrapedIssue> usefulWebResults =
        webResults.stream()
            .filter(
                r ->
                    r.getSolution() != null
                        && r.getSolution().length() >= MIN_USEFUL_SNIPPET_LENGTH)
            .sorted((a, b) -> Double.compare(b.getSourceAuthority(), a.getSourceAuthority()))
            .limit(MAX_WEB_SOURCES)
            .collect(Collectors.toList());

    boolean hasCoreAnswer = coreAnswer != null && !coreAnswer.isEmpty();
    boolean hasWebResults = !usefulWebResults.isEmpty();

    log.info(
        "[NeuralChat] Merging — Core: {} | Web sources: {}",
        hasCoreAnswer ? "YES" : "NO",
        usefulWebResults.size());

    // ── CASE 1: Both core knowledge AND web results available ────────
    if (hasCoreAnswer && hasWebResults) {
      String merged = synthesizeMergedResponse(userMessage, coreAnswer, usefulWebResults);
      List<String> sources =
          usefulWebResults.stream()
              .map(r -> r.getSource() + " (" + r.getTitle() + ")")
              .collect(Collectors.toList());
      sources.add(0, "Core Knowledge");

      return Mono.just(
          new NeuralResponse(
              merged,
              sources,
              calculateConfidence(coreAnswer, usefulWebResults),
              "MERGED",
              "core_knowledge + browser"));
    }

    // ── CASE 2: Only core knowledge available ───────────────────────
    if (hasCoreAnswer) {
      return Mono.just(
          new NeuralResponse(
              coreAnswer, List.of("Core Knowledge"), 0.85, "CORE_ONLY", "core_knowledge"));
    }

    // ── CASE 3: Only web results available ──────────────────────────
    if (hasWebResults) {
      String webSynthesis = synthesizeWebOnlyResponse(userMessage, usefulWebResults);
      List<String> sources =
          usefulWebResults.stream()
              .map(r -> r.getSource() + " (" + r.getTitle() + ")")
              .collect(Collectors.toList());

      return Mono.just(
          new NeuralResponse(
              webSynthesis,
              sources,
              calculateWebConfidence(usefulWebResults),
              "WEB_ONLY",
              "browser"));
    }

    // ── CASE 4: Try DuckDuckGo Instant Answer API as last-resort web search ──
    log.info("[NeuralChat] ⚠️ No scraped knowledge — trying DuckDuckGo Instant Answer API...");
    return tryDuckDuckGoInstantAnswer(userMessage)
        .flatMap(
            ddgAnswer -> {
              if (ddgAnswer != null && !ddgAnswer.isEmpty()) {
                log.info("[NeuralChat] ✅ DuckDuckGo Instant Answer found!");
                return Mono.just(
                    new NeuralResponse(
                        ddgAnswer,
                        List.of("DuckDuckGo Instant Answer"),
                        0.75,
                        "WEB_INSTANT",
                        "duckduckgo_instant"));
              }

              // ── CASE 5: Absolute fallback → UnifiedOfflineKnowledgeService ────────────
              log.info(
                  "[NeuralChat] ⚠️ All web searches failed — falling back to Unified Offline Knowledge");
              return unifiedOfflineKnowledgeService
                  .findAnswer(userMessage)
                  .map(
                      stubResponse ->
                          NeuralResponse.fromStub(stubResponse, "unified_offline_fallback"))
                  .defaultIfEmpty(
                      NeuralResponse.fromStub("আমি লোকাল মোডে সক্রিয়।", "stub_empty_fallback"));
            });
  }

  /**
   * Synthesize a merged response from core knowledge + web results. Core knowledge provides the
   * structural answer; web enriches with real-time data.
   */
  private String synthesizeMergedResponse(
      String userMessage, String coreAnswer, List<ActiveInternetScraper.ScrapedIssue> webResults) {
    StringBuilder merged = new StringBuilder();

    // Start with the core knowledge (authoritative, verified)
    merged.append(coreAnswer);

    // Add web-enriched context
    merged.append("\n\n---\n📡 **Additional Context from Live Sources:**\n\n");

    for (int i = 0; i < webResults.size(); i++) {
      ActiveInternetScraper.ScrapedIssue result = webResults.get(i);
      String snippet = cleanSnippet(result.getSolution());
      if (snippet.isEmpty()) continue;

      merged
          .append("**")
          .append(result.getTitle())
          .append("** (")
          .append(result.getSource())
          .append(")\n");
      merged.append(snippet).append("\n\n");
    }

    return merged.toString().trim();
  }

  /** Synthesize a response purely from web results when core knowledge has no match. */
  private String synthesizeWebOnlyResponse(
      String userMessage, List<ActiveInternetScraper.ScrapedIssue> webResults) {
    StringBuilder response = new StringBuilder();

    // Pick the highest-authority result as the primary answer
    ActiveInternetScraper.ScrapedIssue primary = webResults.get(0);
    String primarySnippet = cleanSnippet(primary.getSolution());

    response.append("📖 **").append(primary.getTitle()).append("**\n\n");
    response.append(primarySnippet);

    // Add supporting sources if available
    if (webResults.size() > 1) {
      response.append("\n\n---\n📡 **Related Information:**\n\n");
      for (int i = 1; i < webResults.size(); i++) {
        ActiveInternetScraper.ScrapedIssue result = webResults.get(i);
        String snippet = cleanSnippet(result.getSolution());
        if (snippet.isEmpty()) continue;

        response
            .append("• **")
            .append(result.getTitle())
            .append("** (")
            .append(result.getSource())
            .append("): ");
        // Trim to first 200 chars for supporting sources
        response.append(snippet.length() > 200 ? snippet.substring(0, 200) + "..." : snippet);
        response.append("\n\n");
      }
    }

    response.append("\n*Source: ").append(primary.getSource()).append("*");
    return response.toString().trim();
  }

  /**
   * Calculate confidence score for merged responses. Core knowledge adds base confidence; web
   * sources add incremental confidence.
   */
  private double calculateConfidence(
      String coreAnswer, List<ActiveInternetScraper.ScrapedIssue> webResults) {
    double base = 0.80; // Core knowledge confidence

    // Web sources add incremental confidence based on authority
    double webBonus =
        webResults.stream()
            .mapToDouble(
                r -> r.getSourceAuthority() * 0.05) // Each high-authority source adds up to 5%
            .sum();

    return Math.min(0.98, base + webBonus);
  }

  /** Calculate confidence for web-only responses. */
  private double calculateWebConfidence(List<ActiveInternetScraper.ScrapedIssue> webResults) {
    return webResults.stream()
        .mapToDouble(r -> r.getSourceAuthority() * r.getRawConfidence())
        .average()
        .orElse(0.5);
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  KEYWORD & DOMAIN EXTRACTION
  // ══════════════════════════════════════════════════════════════════════════

  /**
   * Extract meaningful keywords from the user's message for targeted scraping. Removes stop words
   * and short words, keeps technical terms.
   */
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
        .map(
            w ->
                w.replaceAll(
                    "[^a-zA-Z0-9\\u0980-\\u09FF#+.-]", "")) // Keep Bengali, English, special
        .filter(w -> w.length() > 2)
        .filter(w -> !stopWords.contains(w))
        .distinct()
        .limit(5)
        .collect(Collectors.toList());
  }

  private boolean shouldReturnCoreOnlyWithoutScraping(String message) {
    if (message == null) {
      return false;
    }

    String lower = message.toLowerCase(Locale.ROOT);
    if (lower.contains("what is")
        || lower.contains("what are")
        || lower.contains("explain")
        || lower.contains("define")
        || lower.contains("tell me about")
        || lower.contains("who is")
        || lower.contains("when is")) {
      return true;
    }

    return false;
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

  /** Clean a scraped snippet: remove HTML tags, excessive whitespace, truncate. */
  private String cleanSnippet(String raw) {
    if (raw == null) return "";
    return raw.replaceAll("<[^>]*>", "") // Remove HTML tags
        .replaceAll("&[a-z]+;", " ") // Remove HTML entities
        .replaceAll("\\s+", " ") // Collapse whitespace
        .trim();
  }

  private String truncate(String text, int maxLength) {
    if (text == null) return "";
    return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  DUCKDUCKGO INSTANT ANSWER API (free, no key, JSON-based)
  // ══════════════════════════════════════════════════════════════════════════

  /**
   * Try DuckDuckGo Instant Answer API as a fast last-resort web search. This is a free JSON API
   * that returns instant answers for many queries. URL:
   * https://api.duckduckgo.com/?q=QUERY&format=json&no_html=1
   */
  private Mono<String> tryDuckDuckGoInstantAnswer(String query) {
    try {
      String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
      String url =
          "https://api.duckduckgo.com/?q="
              + encodedQuery
              + "&format=json&no_html=1&skip_disambig=1";

      return webClient
          .get()
          .uri(url)
          .header("User-Agent", "SupremeAI/1.0")
          .retrieve()
          .bodyToMono(String.class)
          .timeout(Duration.ofSeconds(8))
          .map(
              json -> {
                if (json == null || json.isEmpty()) return "";

                try {
                  com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);

                  StringBuilder answer = new StringBuilder();

                  // 1. AbstractText — the main instant answer
                  String abstractText = root.path("AbstractText").asText("");
                  String abstractSource = root.path("AbstractSource").asText("");
                  String heading = root.path("Heading").asText("");

                  if (!abstractText.isEmpty() && abstractText.length() > 30) {
                    if (!heading.isEmpty()) {
                      answer.append("## ").append(heading).append("\n\n");
                    }
                    answer.append(abstractText);
                    if (!abstractSource.isEmpty()) {
                      answer.append("\n\n*Source: ").append(abstractSource).append("*");
                    }
                  }

                  // 2. RelatedTopics — additional context
                  com.fasterxml.jackson.databind.JsonNode relatedTopics =
                      root.path("RelatedTopics");
                  if (relatedTopics.isArray() && relatedTopics.size() > 0 && answer.length() < 50) {
                    int count = 0;
                    for (com.fasterxml.jackson.databind.JsonNode topic : relatedTopics) {
                      String text = topic.path("Text").asText("");
                      if (!text.isEmpty() && text.length() > 20) {
                        if (count == 0 && answer.length() == 0) {
                          answer.append("## ").append(query).append("\n\n");
                          answer.append("Here's what I found:\n\n");
                        }
                        answer.append("• ").append(text).append("\n\n");
                        count++;
                        if (count >= 4) break;
                      }
                    }
                  }

                  // 3. Answer field (for factual queries like "age of earth")
                  String directAnswer = root.path("Answer").asText("");
                  if (!directAnswer.isEmpty() && answer.length() == 0) {
                    answer.append(directAnswer);
                  }

                  String result = answer.toString().trim();
                  return result.length() > 30 ? result : "";

                } catch (Exception e) {
                  log.warn(
                      "[NeuralChat] DuckDuckGo Instant Answer JSON parsing failed: {}",
                      e.getMessage());
                  return "";
                }
              })
          .onErrorResume(
              e -> {
                log.warn("[NeuralChat] DuckDuckGo Instant Answer API failed: {}", e.getMessage());
                return Mono.just("");
              });

    } catch (Exception e) {
      log.warn("[NeuralChat] DuckDuckGo Instant Answer failed: {}", e.getMessage());
      return Mono.just("");
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  RESPONSE DATA CLASS
  // ══════════════════════════════════════════════════════════════════════════

  /** Encapsulates the neural chat response with metadata about sources and confidence. */
  public static class NeuralResponse {
    private final String answer;
    private final List<String> sources;
    private final double confidence;
    private final String tier; // MERGED, CORE_ONLY, WEB_ONLY, STUB_FALLBACK
    private final String pipeline; // Which pipeline produced this response

    public NeuralResponse(
        String answer, List<String> sources, double confidence, String tier, String pipeline) {
      this.answer = answer;
      this.sources = sources != null ? sources : List.of();
      this.confidence = confidence;
      this.tier = tier;
      this.pipeline = pipeline;
    }

    public static NeuralResponse fromStub(String answer, String reason) {
      return new NeuralResponse(answer, List.of("StubLocalProvider"), 0.5, "STUB_FALLBACK", reason);
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
