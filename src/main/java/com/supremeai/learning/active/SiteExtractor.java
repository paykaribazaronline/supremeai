package com.supremeai.learning.active;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * SiteExtractor interface for pluggable scraping architecture.
 * Each site implementation knows how to extract issues/solutions from its domain.
 *
 * Benefits:
 * - Easy to add new sites without modifying ActiveInternetScraper
 * - Each extractor encapsulates site-specific logic
 * - Can be enabled/disabled via configuration
 * - Promotes code reusability and testing
 */
@Component
public interface SiteExtractor {

    /**
     * Unique identifier for this extractor.
     */
    String getSourceName();

    /**
     * Human-readable description.
     */
    String getDescription();

    /**
     * Authority weight for conflict resolution (0.0-1.0).
     */
    double getAuthorityWeight();

    /**
     * Scrape this site and return issues.
     */
    List<ActiveInternetScraper.ScrapedIssue> scrape(RestTemplate restTemplate, int limit, Object... context) throws Exception;

    /**
     * Is this site currently enabled? (driven by config)
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Rate limit: max requests per minute this extractor should make.
     */
    default int getRateLimitPerMinute() {
        return 10;
    }

    /**
     * Try to extract an error signature or topic from scraped content.
     * Used to classify the issue for knowledge base storage.
     */
    default String extractErrorSignature(ActiveInternetScraper.ScrapedIssue issue) {
        return issue.getTitle();
    }
}
