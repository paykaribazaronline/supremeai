package com.supremeai.learning.active;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Active Internet Scraper - Proactively learns solutions from the internet
 * before users encounter errors. Uses free public APIs.
 */
@Service
public class ActiveInternetScraper {

    private static final Logger log = LoggerFactory.getLogger(ActiveInternetScraper.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${learning.scraper.wikipedia.limit:5}")
    private int wikiLimit;

    @Value("${learning.scraper.stackoverflow.limit:3}")
    private int soLimit;

    public ActiveInternetScraper() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Scrape trending issues from multiple free sources:
     * - Wikipedia (latest tech news/updates)
     * - StackOverflow (trending questions)
     * - GitHub (popular issues via search)
     */
    public List<ScrapedIssue> scrapeTrendingIssues() {
        List<ScrapedIssue> trending = new ArrayList<>();

        // 1. Scrape Wikipedia tech trends
        trending.addAll(scrapeWikipediaTrends());

        // 2. Scrape StackOverflow trending questions
        trending.addAll(scrapeStackOverflowTrends());

        // 3. Could add more sources (GitHub, blogs)
        // trending.addAll(scrapeGitHubTrending());

        log.info("[Active Learning] Scraped {} trending issues from internet", trending.size());
        return trending;
    }

    /**
     * Scrape trending tech articles from Wikipedia.
     * Uses Wikipedia API to fetch recent changes in technology pages.
     */
    private List<ScrapedIssue> scrapeWikipediaTrends() {
        List<ScrapedIssue> results = new ArrayList<>();
        try {
            // Wikipedia API: recent changes in technology category
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
                        String title = change.path("title").asText();
                        // Extract potential tech topics
                        if (isTechnologyRelated(title)) {
                            String snippet = "Wikipedia update on: " + title;
                            String solution = fetchWikipediaSummary(title);
                            results.add(new ScrapedIssue(
                                "Tech Update: " + title,
                                solution != null ? solution : "Check Wikipedia for latest information on " + title,
                                "Wikipedia"
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to scrape Wikipedia: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Check if a Wikipedia title is technology-related.
     */
    private boolean isTechnologyRelated(String title) {
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

    /**
     * Fetch Wikipedia summary for a given title.
     */
    private String fetchWikipediaSummary(String title) {
        try {
            String encodedTitle = java.net.URLEncoder.encode(title, "UTF-8");
            String url = String.format(
                "https://en.wikipedia.org/api/rest_v1/page/summary/%s",
                encodedTitle
            );
            String response = restTemplate.getForObject(url, String.class);
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode extract = root.path("extract");
                return extract.isTextual() ? extract.asText() : "";
            }
        } catch (Exception e) {
            log.debug("Failed to fetch Wikipedia summary for {}: {}", title, e.getMessage());
        }
        return null;
    }

    /**
     * Scrape trending questions from StackOverflow using StackExchange API.
     */
    private List<ScrapedIssue> scrapeStackOverflowTrends() {
        List<ScrapedIssue> results = new ArrayList<>();
        try {
            // StackExchange API: recent popular questions tagged with programming languages
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
                        String title = question.path("title").asText();
                        String body = question.path("body").asText("");
                        String answer = "Check StackOverflow for solution: " + question.path("link").asText("");
                        
                        results.add(new ScrapedIssue(
                            title,
                            answer,
                            "StackOverflow"
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to scrape StackOverflow: {}", e.getMessage());
        }
        return results;
    }

    // Alternative: Could implement GitHub trending issues scraping via GitHub API
    // Would require GitHub token for higher rate limits
    /*
    private List<ScrapedIssue> scrapeGitHubTrending() {
        // Implementation using GitHub Search API
    }
    */
}

class ScrapedIssue {
    public String titleOrError;
    public String potentialSolution;
    public String source;

    public ScrapedIssue(String title, String solution, String source) {
        this.titleOrError = title;
        this.potentialSolution = solution;
        this.source = source;
    }
}