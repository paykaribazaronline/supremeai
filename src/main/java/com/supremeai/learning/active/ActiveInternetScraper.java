package com.supremeai.learning.active;

import com.supremeai.learning.ContentSanitizerService;
import com.supremeai.learning.knowledge.SolutionMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Active Internet Scraper - Proactively learns solutions from the internet
 * before users encounter errors. Uses free public APIs and includes
 * content sanitization and source authority weighting.
 */
@Service
public class ActiveInternetScraper {

    private static final Logger log = LoggerFactory.getLogger(ActiveInternetScraper.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ContentSanitizerService sanitizer;

    @Value("${learning.scraper.wikipedia.limit:5}")
    private int wikiLimit;

    @Value("${learning.scraper.stackoverflow.limit:3}")
    private int soLimit;

    public ActiveInternetScraper(ContentSanitizerService sanitizer) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.sanitizer = sanitizer;
    }

    /**
     * Scrape trending issues from multiple free sources with authority tracking.
     */
    public List<ScrapedIssue> scrapeTrendingIssues() {
        List<ScrapedIssue> trending = new ArrayList<>();

        // Scrape Wikipedia tech trends (authority: 0.75)
        trending.addAll(scrapeWikipediaTrends());

        // Scrape StackOverflow (authority: 0.80)
        trending.addAll(scrapeStackOverflowTrends());

        log.info("[Active Learning] Scraped {} trending issues from internet", trending.size());
        return trending;
    }

    /**
     * Scrape trending tech articles from Wikipedia.
     * Authority weight: 0.75 (mid-range - secondary source).
     */
    private List<ScrapedIssue> scrapeWikipediaTrends() {
        List<ScrapedIssue> results = new ArrayList<>();
        try {
            String url = String.format(
                "https://en.wikipedia.org/w/api.php?action=query&list=recentchanges&rcnamespace=0" +
                "&rcshow=!bot|!minor&rctype=edit|new&rclimit=%d&format=json",
                wikiLimit
            );

            String response = restTemplate.getForObject(url, String.class);
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode changes = root.path("query").path("recentchanges");
                if (changes.isArray()) {
                    for (JsonNode change : changes) {
                        String title = change.path("title").asText("");
                        if (isTechnologyRelated(title)) {
                            String snippet = "Wikipedia update on: " + title;
                            String solution = fetchWikipediaSummary(title);
                            if (solution != null && !solution.isEmpty()) {
                                ScrapedIssue issue = new ScrapedIssue(
                                    title,
                                    solution,
                                    "Wikipedia"
                                );
                                issue.setSourceAuthority(SourceAuthority.WIKIPEDIA.getWeight());
                                issue.setRawConfidence(0.7); // Base confidence for Wikipedia
                                results.add(issue);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to scrape Wikipedia: {}", e.getMessage());
        }
        return results;
    }

    private boolean isTechnologyRelated(String title) {
        if (title == null) return false;
        String lower = title.toLowerCase();
        String[] techKeywords = {
            "programming", "software", "computer", "algorithm", "data structure",
            "web", "api", "database", "security", "network", "cloud", "ai", "machine learning",
            "javascript", "python", "java", "kotlin", "spring", "react", "android", "ios"
        };
        for (String keyword : techKeywords) {
            if (lower.contains(keyword)) return true;
        }
        return false;
    }

    private String fetchWikipediaSummary(String title) {
        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String url = String.format(
                "https://en.wikipedia.org/api/rest_v1/page/summary/%s",
                encodedTitle
            );
            String response = restTemplate.getForObject(url, String.class);
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode extract = root.path("extract");
                return extract.isTextual() ? extract.asText("") : "";
            }
        } catch (Exception e) {
            log.debug("Failed to fetch Wikipedia summary for {}: {}", title, e.getMessage());
        }
        return null;
    }

    /**
     * Scrape trending questions from StackOverflow using StackExchange API.
     * Authority weight: 0.80 (high for Q&A).
     */
    private List<ScrapedIssue> scrapeStackOverflowTrends() {
        List<ScrapedIssue> results = new ArrayList<>();
        try {
            String url = String.format(
                "https://api.stackexchange.com/2.3/questions?order=desc&sort=hot" +
                "&tagged=python%2Cjava%2Cjavascript%2Cspring-boot%2Creactjs&site=stackoverflow" +
                "&pagesize=%d&filter=withbody",
                soLimit
            );

            String response = restTemplate.getForObject(url, String.class);
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode items = root.path("items");
                if (items.isArray()) {
                    for (JsonNode question : items) {
                        String title = question.path("title").asText("");
                        String link = question.path("link").asText("");
                        String answer = "Check StackOverflow for solution: " + link;

                        ScrapedIssue issue = new ScrapedIssue(title, answer, "StackOverflow");
                        issue.setSourceAuthority(SourceAuthority.STACK_OVERFLOW.getWeight());
                        issue.setRawConfidence(0.75);
                        results.add(issue);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to scrape StackOverflow: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Convert ScrapedIssue to SolutionMemory, applying sanitization.
     * Returns null if solution fails sanitization.
     */
    public SolutionMemory convertToSolution(ScrapedIssue issue, String errorSignature) {
        SolutionMemory candidate = new SolutionMemory(
            errorSignature != null ? errorSignature : issue.getTitle(),
            issue.getSolution(),
            issue.getSource(),
            estimateExecutionTime(issue.getSolution()),
            estimateSecurityScore(issue.getSolution())
        );
        candidate.setTimeless(isLikelyTimeless(issue));

        // Apply source authority as metadata (stored separately or in confidence calculation)
        // Could also be used in GlobalKnowledgeBase merging logic

        // Sanitize before returning
        if (sanitizer.sanitizeAndValidate(candidate, issue.getSource())) {
            return candidate;
        } else {
            log.warn("[Sanitizer] Dropped solution from {} (failed validation)", issue.getSource());
            return null;
        }
    }

    /**
     * Heuristically estimate execution time for solution.
     */
    private long estimateExecutionTime(String code) {
        // Simple heuristic: longer code likely slower
        return Math.min(5000, Math.max(10, code.length() * 2));
    }

    /**
     * Basic security score estimation (presence of unsafe patterns).
     */
    private double estimateSecurityScore(String code) {
        String lower = code.toLowerCase();
        if (lower.contains("eval(") || lower.contains("exec(") || lower.contains("system(")) {
            return 0.2;
        }
        if (lower.contains("password") || lower.contains("secret") || lower.contains("token")) {
            return 0.3;
        }
        return 0.8; // Default decent score
    }

    /**
     * Determine if a solution is timeless (algorithmic knowledge doesn't expire).
     */
    private boolean isLikelyTimeless(ScrapedIssue issue) {
        String title = issue.getTitle().toLowerCase();
        String[] timelessKeywords = {"algorithm", "reverse", "sort", "search", "tree", "graph", "recursion"};
        for (String kw : timelessKeywords) {
            if (title.contains(kw)) return true;
        }
        return false;
    }

    /**
     * Simple DTO representing a scraped issue/solution pair.
     */
    public static class ScrapedIssue {
        private String title;
        private String solution;
        private String source;
        private double sourceAuthority;
        private double rawConfidence;

        public ScrapedIssue(String title, String solution, String source) {
            this.title = title;
            this.solution = solution;
            this.source = source;
            this.sourceAuthority = 0.5;
            this.rawConfidence = 0.5;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSolution() { return solution; }
        public void setSolution(String solution) { this.solution = solution; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public double getSourceAuthority() { return sourceAuthority; }
        public void setSourceAuthority(double authority) { this.sourceAuthority = authority; }
        public double getRawConfidence() { return rawConfidence; }
        public void setRawConfidence(double rawConfidence) { this.rawConfidence = rawConfidence; }
    }
}
