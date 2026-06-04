package com.supremeai.learning.active;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for SiteExtractor interface. Tests default method behaviors. */
class SiteExtractorTest {

  @Test
  void testDefaultIsEnabled_returnsTrue() {
    SiteExtractor extractor =
        new SiteExtractor() {
          @Override
          public String getSourceName() {
            return "test";
          }

          @Override
          public String getDescription() {
            return "test";
          }

          @Override
          public double getAuthorityWeight() {
            return 0.5;
          }

          @Override
          public java.util.List<ActiveInternetScraper.ScrapedIssue> scrape(
              org.springframework.web.client.RestTemplate restTemplate,
              int limit,
              Object... context) {
            return java.util.Collections.emptyList();
          }
        };

    assertTrue(extractor.isEnabled());
  }

  @Test
  void testDefaultGetRateLimitPerMinute_returnsTen() {
    SiteExtractor extractor =
        new SiteExtractor() {
          @Override
          public String getSourceName() {
            return "test";
          }

          @Override
          public String getDescription() {
            return "test";
          }

          @Override
          public double getAuthorityWeight() {
            return 0.5;
          }

          @Override
          public java.util.List<ActiveInternetScraper.ScrapedIssue> scrape(
              org.springframework.web.client.RestTemplate restTemplate,
              int limit,
              Object... context) {
            return java.util.Collections.emptyList();
          }
        };

    assertEquals(10, extractor.getRateLimitPerMinute());
  }

  @Test
  void testDefaultExtractErrorSignature_returnsTitle() {
    // Create a real ScrapedIssue using its constructor
    ActiveInternetScraper.ScrapedIssue issue =
        new ActiveInternetScraper.ScrapedIssue(
            "NullPointerException in Main", "Check for null", "test");

    SiteExtractor extractor =
        new SiteExtractor() {
          @Override
          public String getSourceName() {
            return "test";
          }

          @Override
          public String getDescription() {
            return "test";
          }

          @Override
          public double getAuthorityWeight() {
            return 0.5;
          }

          @Override
          public java.util.List<ActiveInternetScraper.ScrapedIssue> scrape(
              org.springframework.web.client.RestTemplate restTemplate,
              int limit,
              Object... context) {
            return null;
          }
        };

    String signature = extractor.extractErrorSignature(issue);
    assertEquals("NullPointerException in Main", signature);
  }

  @Test
  void testOverriddenExtractErrorSignature_usesOverride() {
    ActiveInternetScraper.ScrapedIssue issue =
        new ActiveInternetScraper.ScrapedIssue("Error title", "solution", "src");

    SiteExtractor extractor =
        new SiteExtractor() {
          @Override
          public String getSourceName() {
            return "test";
          }

          @Override
          public String getDescription() {
            return "test";
          }

          @Override
          public double getAuthorityWeight() {
            return 0.5;
          }

          @Override
          public java.util.List<ActiveInternetScraper.ScrapedIssue> scrape(
              org.springframework.web.client.RestTemplate restTemplate,
              int limit,
              Object... context) {
            return null;
          }

          @Override
          public String extractErrorSignature(ActiveInternetScraper.ScrapedIssue issue) {
            return "Custom_" + issue.getTitle().toUpperCase();
          }
        };

    assertEquals("Custom_ERROR TITLE", extractor.extractErrorSignature(issue));
  }

  @Test
  void testMultipleDefaultMethodsConsistent() {
    SiteExtractor extractor =
        new SiteExtractor() {
          @Override
          public String getSourceName() {
            return "test";
          }

          @Override
          public String getDescription() {
            return "test";
          }

          @Override
          public double getAuthorityWeight() {
            return 0.5;
          }

          @Override
          public java.util.List<ActiveInternetScraper.ScrapedIssue> scrape(
              org.springframework.web.client.RestTemplate restTemplate,
              int limit,
              Object... context) {
            return null;
          }
        };

    assertTrue(extractor.isEnabled());
    assertEquals(10, extractor.getRateLimitPerMinute());
  }
}
