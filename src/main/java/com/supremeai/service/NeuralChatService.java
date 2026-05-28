package com.supremeai.service;

import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.learning.active.ActiveInternetScraper;
import com.supremeai.learning.active.QueryClassifier;
import com.supremeai.provider.StubLocalProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *  NeuralChatService — The Intelligent Unified Response Engine
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Merges three knowledge tiers into ONE intelligent response:
 *
 *   Tier 1: core_knowledge.json   → Instant, offline, always available
 *   Tier 2: Browser Scraping      → Wikipedia, StackOverflow, MDN, GitHub, arXiv
 *   Tier 3: StubLocalProvider      → Rule-based Bengali/English fallback
 *
 * Flow:
 *   User asks "What is React?"
 *     → Tier 1: core_knowledge.json has React patterns → use as base
 *     → Tier 2: Wikipedia scrape → "React is a JavaScript library..." → enrich
 *     → Merge both into a comprehensive, intelligent answer
 *     → If both fail → Tier 3: StubLocalProvider gives basic response
 *
 * Design Principles:
 *   ✅ No external API keys required
 *   ✅ Works fully offline (core_knowledge always available)
 *   ✅ Browser scraping enhances answers when internet is available
 *   ✅ Answers are synthesized, not raw dumps
 *   ✅ Supports both Bengali and English
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Service
public class NeuralChatService {

    private static final Logger log = LoggerFactory.getLogger(NeuralChatService.class);

    private final SupremeLearningOrchestrator learningOrchestrator;
    private final ActiveInternetScraper internetScraper;
    private final QueryClassifier queryClassifier;
    private final StubLocalProvider stubLocalProvider;

    /** Maximum time to wait for browser scraping before returning core knowledge only */
    private static final Duration SCRAPE_TIMEOUT = Duration.ofSeconds(6);

    /** Minimum snippet length to consider a scraped result useful */
    private static final int MIN_USEFUL_SNIPPET_LENGTH = 30;

    /** Maximum number of web sources to include in a merged response */
    private static final int MAX_WEB_SOURCES = 3;

    @Autowired
    public NeuralChatService(SupremeLearningOrchestrator learningOrchestrator,
                             ActiveInternetScraper internetScraper,
                             QueryClassifier queryClassifier,
                             StubLocalProvider stubLocalProvider) {
        this.learningOrchestrator = learningOrchestrator;
        this.internetScraper = internetScraper;
        this.queryClassifier = queryClassifier;
        this.stubLocalProvider = stubLocalProvider;
        log.info("[NeuralChat] Initialized — Tier 1 (Core Knowledge) + Tier 2 (Browser) + Tier 3 (StubLocal) merged pipeline");
    }

    /**
     * Generate an intelligent response by merging all knowledge tiers.
     *
     * @param userMessage The user's question or prompt
     * @return Mono<NeuralResponse> containing the merged intelligent response
     */
    public Mono<NeuralResponse> generateIntelligentResponse(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Mono.just(NeuralResponse.fromStub(
                "আমি সুপ্রিমএআই। আপনার প্রশ্ন লিখুন, আমি সাহায্য করব।",
                "empty_input"));
        }

        log.info("[NeuralChat] Processing: \"{}\"", truncate(userMessage, 80));

        // ── TIER 1: Core Knowledge (instant, offline) ───────────────────────
        String coreAnswer = findCoreKnowledge(userMessage);

        // ── Classify the query for targeted scraping ────────────────────────
        List<String> keywords = extractKeywords(userMessage);
        String domain = extractDomain(userMessage);

        // ── TIER 2: Browser Scraping (async, may timeout) ───────────────────
        Mono<List<ActiveInternetScraper.ScrapedIssue>> scrapeMono =
            internetScraper.scrapeKnowledge(domain, keywords)
                .collectList()
                .timeout(SCRAPE_TIMEOUT)
                .onErrorResume(e -> {
                    log.warn("[NeuralChat] Browser scraping failed or timed out: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                });

        return scrapeMono.map(scrapedResults -> {
            // ── MERGE TIERS ─────────────────────────────────────────────────
            return mergeKnowledgeTiers(userMessage, coreAnswer, scrapedResults);
        });
    }

    /**
     * Tier 1: Search core_knowledge.json for a matching answer.
     */
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
     * Strategy:
     *   - If core_knowledge has a strong match AND web has enriching data → merge both
     *   - If only core_knowledge matches → use it directly
     *   - If only web has results → synthesize from web
     *   - If neither → fall back to StubLocalProvider (Tier 3)
     */
    private NeuralResponse mergeKnowledgeTiers(String userMessage, String coreAnswer,
                                                List<ActiveInternetScraper.ScrapedIssue> webResults) {
        // Filter web results to only useful ones
        List<ActiveInternetScraper.ScrapedIssue> usefulWebResults = webResults.stream()
            .filter(r -> r.getSolution() != null && r.getSolution().length() >= MIN_USEFUL_SNIPPET_LENGTH)
            .sorted((a, b) -> Double.compare(b.getSourceAuthority(), a.getSourceAuthority()))
            .limit(MAX_WEB_SOURCES)
            .collect(Collectors.toList());

        boolean hasCoreAnswer = coreAnswer != null && !coreAnswer.isEmpty();
        boolean hasWebResults = !usefulWebResults.isEmpty();

        log.info("[NeuralChat] Merging — Core: {} | Web sources: {}",
            hasCoreAnswer ? "YES" : "NO", usefulWebResults.size());

        // ── CASE 1: Both core knowledge AND web results available ────────
        if (hasCoreAnswer && hasWebResults) {
            String merged = synthesizeMergedResponse(userMessage, coreAnswer, usefulWebResults);
            List<String> sources = usefulWebResults.stream()
                .map(r -> r.getSource() + " (" + r.getTitle() + ")")
                .collect(Collectors.toList());
            sources.add(0, "Core Knowledge");

            return new NeuralResponse(merged, sources, calculateConfidence(coreAnswer, usefulWebResults),
                "MERGED", "core_knowledge + browser");
        }

        // ── CASE 2: Only core knowledge available ───────────────────────
        if (hasCoreAnswer) {
            return new NeuralResponse(coreAnswer, List.of("Core Knowledge"), 0.85,
                "CORE_ONLY", "core_knowledge");
        }

        // ── CASE 3: Only web results available ──────────────────────────
        if (hasWebResults) {
            String webSynthesis = synthesizeWebOnlyResponse(userMessage, usefulWebResults);
            List<String> sources = usefulWebResults.stream()
                .map(r -> r.getSource() + " (" + r.getTitle() + ")")
                .collect(Collectors.toList());

            return new NeuralResponse(webSynthesis, sources,
                calculateWebConfidence(usefulWebResults), "WEB_ONLY", "browser");
        }

        // ── CASE 4: Nothing found → Tier 3 StubLocalProvider ────────────
        log.info("[NeuralChat] ⚠️ No knowledge found — falling back to Tier 3 (StubLocal)");
        String stubResponse = stubLocalProvider.generate(userMessage).block();
        return NeuralResponse.fromStub(stubResponse, "stub_local_fallback");
    }

    /**
     * Synthesize a merged response from core knowledge + web results.
     * Core knowledge provides the structural answer; web enriches with real-time data.
     */
    private String synthesizeMergedResponse(String userMessage, String coreAnswer,
                                             List<ActiveInternetScraper.ScrapedIssue> webResults) {
        StringBuilder merged = new StringBuilder();

        // Start with the core knowledge (authoritative, verified)
        merged.append(coreAnswer);

        // Add web-enriched context
        merged.append("\n\n---\n📡 **Additional Context from Live Sources:**\n\n");

        for (int i = 0; i < webResults.size(); i++) {
            ActiveInternetScraper.ScrapedIssue result = webResults.get(i);
            String snippet = cleanSnippet(result.getSolution());
            if (snippet.isEmpty()) continue;

            merged.append("**").append(result.getTitle()).append("** (")
                  .append(result.getSource()).append(")\n");
            merged.append(snippet).append("\n\n");
        }

        return merged.toString().trim();
    }

    /**
     * Synthesize a response purely from web results when core knowledge has no match.
     */
    private String synthesizeWebOnlyResponse(String userMessage,
                                              List<ActiveInternetScraper.ScrapedIssue> webResults) {
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

                response.append("• **").append(result.getTitle()).append("** (")
                        .append(result.getSource()).append("): ");
                // Trim to first 200 chars for supporting sources
                response.append(snippet.length() > 200 ? snippet.substring(0, 200) + "..." : snippet);
                response.append("\n\n");
            }
        }

        response.append("\n*Source: ").append(primary.getSource()).append("*");
        return response.toString().trim();
    }

    /**
     * Calculate confidence score for merged responses.
     * Core knowledge adds base confidence; web sources add incremental confidence.
     */
    private double calculateConfidence(String coreAnswer, List<ActiveInternetScraper.ScrapedIssue> webResults) {
        double base = 0.80; // Core knowledge confidence

        // Web sources add incremental confidence based on authority
        double webBonus = webResults.stream()
            .mapToDouble(r -> r.getSourceAuthority() * 0.05) // Each high-authority source adds up to 5%
            .sum();

        return Math.min(0.98, base + webBonus);
    }

    /**
     * Calculate confidence for web-only responses.
     */
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
     * Extract meaningful keywords from the user's message for targeted scraping.
     * Removes stop words and short words, keeps technical terms.
     */
    private List<String> extractKeywords(String message) {
        Set<String> stopWords = Set.of(
            "what", "is", "the", "a", "an", "in", "on", "of", "to", "for",
            "and", "or", "how", "do", "does", "can", "could", "would", "should",
            "this", "that", "with", "from", "by", "at", "it", "be", "are",
            "was", "were", "been", "being", "have", "has", "had", "will",
            "i", "me", "my", "you", "your", "we", "our", "they", "them",
            "tell", "explain", "about", "please", "help", "want", "need",
            "কি", "কী", "কেন", "কোথায়", "কিভাবে", "কীভাবে", "আমি", "আমার",
            "এটা", "এটি", "সেটা", "তুমি", "তোমার", "করো", "করুন", "বলো", "বলুন"
        );

        return Arrays.stream(message.toLowerCase().split("\\s+"))
            .map(w -> w.replaceAll("[^a-zA-Z0-9\\u0980-\\u09FF#+.-]", "")) // Keep Bengali, English, special
            .filter(w -> w.length() > 2)
            .filter(w -> !stopWords.contains(w))
            .distinct()
            .limit(5)
            .collect(Collectors.toList());
    }

    /**
     * Extract the primary domain/topic from the message for scraping.
     */
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

    /**
     * Clean a scraped snippet: remove HTML tags, excessive whitespace, truncate.
     */
    private String cleanSnippet(String raw) {
        if (raw == null) return "";
        return raw
            .replaceAll("<[^>]*>", "")           // Remove HTML tags
            .replaceAll("&[a-z]+;", " ")         // Remove HTML entities
            .replaceAll("\\s+", " ")             // Collapse whitespace
            .trim();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RESPONSE DATA CLASS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Encapsulates the neural chat response with metadata about sources and confidence.
     */
    public static class NeuralResponse {
        private final String answer;
        private final List<String> sources;
        private final double confidence;
        private final String tier;        // MERGED, CORE_ONLY, WEB_ONLY, STUB_FALLBACK
        private final String pipeline;    // Which pipeline produced this response

        public NeuralResponse(String answer, List<String> sources, double confidence,
                              String tier, String pipeline) {
            this.answer = answer;
            this.sources = sources != null ? sources : List.of();
            this.confidence = confidence;
            this.tier = tier;
            this.pipeline = pipeline;
        }

        public static NeuralResponse fromStub(String answer, String reason) {
            return new NeuralResponse(answer, List.of("StubLocalProvider"), 0.5,
                "STUB_FALLBACK", reason);
        }

        public String getAnswer() { return answer; }
        public List<String> getSources() { return sources; }
        public double getConfidence() { return confidence; }
        public String getTier() { return tier; }
        public String getPipeline() { return pipeline; }

        /**
         * Convert to a Map suitable for JSON API responses.
         */
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
