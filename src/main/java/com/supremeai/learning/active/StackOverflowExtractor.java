package com.supremeai.learning.active;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * StackOverflow SiteExtractor - Scrapes hot programming questions from StackOverflow
 * using the StackExchange API.
 *
 * Authority: 0.80 (high for code Q&A)
 */
@Component
public class StackOverflowExtractor implements SiteExtractor {

    private static final Logger log = LoggerFactory.getLogger(StackOverflowExtractor.class);

    @Override
    public String getSourceName() {
        return "StackOverflow";
    }

    @Override
    public String getDescription() {
        return "Hot programming questions from StackOverflow";
    }

    @Override
    public double getAuthorityWeight() {
        return 0.80;
    }

    @Override
    public List<ActiveInternetScraper.ScrapedIssue> scrape(RestTemplate restTemplate, int limit, Object... context) throws Exception {
        List<ActiveInternetScraper.ScrapedIssue> results = new ArrayList<>();
        String url = String.format(
            "https://api.stackexchange.com/2.3/questions?order=desc&sort=hot" +
            "&tagged=python%2Cjava%2Cjavascript%2Cspring-boot%2Creactjs&site=stackoverflow" +
            "&pagesize=%d&filter=withbody",
            Math.min(limit, 20)
        );

        String response = restTemplate.getForObject(url, String.class);
        if (response == null) return results;

        JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        JsonNode items = root.path("items");
        
        if (items.isArray()) {
            for (JsonNode question : items) {
                String title = question.path("title").textValue();
                String link = question.path("link").textValue();
                
                if (!title.isEmpty() && !link.isEmpty()) {
                    String answer = "Check StackOverflow for solution: " + link;
                    ActiveInternetScraper.ScrapedIssue issue = new ActiveInternetScraper.ScrapedIssue(
                        title,
                        answer,
                        "StackOverflow"
                    );
                    issue.setSourceAuthority(getAuthorityWeight());
                    issue.setRawConfidence(0.75);
                    results.add(issue);
                }
            }
        }

        log.info("[StackOverflowExtractor] Scraped {} hot questions", results.size());
        return results;
    }
}
