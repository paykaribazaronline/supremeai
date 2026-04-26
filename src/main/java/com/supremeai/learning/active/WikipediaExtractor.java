package com.supremeai.learning.active;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Wikipedia SiteExtractor - Scrapes recent tech-related edits and summaries
 * from Wikipedia's public API.
 *
 * Authority: 0.75 (mid-range - good for general knowledge, not specific bugs)
 */
@Component
public class WikipediaExtractor implements SiteExtractor {

    private static final Logger log = LoggerFactory.getLogger(WikipediaExtractor.class);

    @Override
    public String getSourceName() {
        return "Wikipedia";
    }

    @Override
    public String getDescription() {
        return "Wikipedia recent tech article changes and summaries";
    }

    @Override
    public double getAuthorityWeight() {
        return 0.75;
    }

    @Override
    public List<ActiveInternetScraper.ScrapedIssue> scrape(RestTemplate restTemplate, int limit, Object... context) throws Exception {
        List<ActiveInternetScraper.ScrapedIssue> results = new ArrayList<>();
        String url = String.format(
            "https://en.wikipedia.org/w/api.php?action=query&list=recentchanges&rcnamespace=0" +
            "&rcshow=!bot|!minor&rctype=edit|new&rclimit=%d&format=json",
            Math.min(limit, 20)
        );

        String response = restTemplate.getForObject(url, String.class);
        if (response == null) return results;

        JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        JsonNode changes = root.path("query").path("recentchanges");
        
        if (changes.isArray()) {
            int count = 0;
            for (JsonNode change : changes) {
                if (count >= limit) break;
                
                String title = change.path("title").asText("");
                if (isTechnologyRelated(title)) {
                    String summary = fetchWikipediaSummary(restTemplate, title);
                    if (summary != null && !summary.isEmpty()) {
                        ActiveInternetScraper.ScrapedIssue issue = new ActiveInternetScraper.ScrapedIssue(
                            "Tech Update: " + title,
                            summary,
                            "Wikipedia"
                        );
                        issue.setSourceAuthority(getAuthorityWeight());
                        issue.setRawConfidence(0.7);
                        results.add(issue);
                        count++;
                    }
                }
            }
        }

        log.info("[WikipediaExtractor] Scraped {} tech-related items", results.size());
        return results;
    }

    private boolean isTechnologyRelated(String title) {
        if (title == null) return false;
        String lower = title.toLowerCase();
        String[] techKeywords = {
            "programming", "software", "computer", "algorithm", "data structure",
            "web", "api", "database", "security", "network", "cloud", "ai", "machine learning",
            "javascript", "python", "java", "kotlin", "spring", "react", "android", "ios",
            "framework", "library", "opensource", "code", "development"
        };
        for (String keyword : techKeywords) {
            if (lower.contains(keyword)) return true;
        }
        return false;
    }

    private String fetchWikipediaSummary(RestTemplate restTemplate, String title) {
        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String url = String.format(
                "https://en.wikipedia.org/api/rest_v1/page/summary/%s",
                encodedTitle
            );
            String response = restTemplate.getForObject(url, String.class);
            if (response != null) {
                JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
                JsonNode extract = root.path("extract");
                return extract.isTextual() ? extract.asText("") : "";
            }
        } catch (Exception e) {
            log.debug("Failed to fetch Wikipedia summary for {}: {}", title, e.getMessage());
        }
        return null;
    }
}
