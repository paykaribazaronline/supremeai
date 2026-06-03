package com.supremeai.learning.active;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StackOverflowExtractor.
 * Tests authority weight, rate limits, and default values.
 */
class StackOverflowExtractorTest {

    private StackOverflowExtractor extractor = new StackOverflowExtractor();

    @Test
    void testGetSourceName_returnsStackOverflow() {
        assertEquals("StackOverflow", extractor.getSourceName());
    }

    @Test
    void testGetAuthorityWeight_returns080() {
        assertEquals(0.80, extractor.getAuthorityWeight(), 0.001);
    }

    @Test
    void testGetDescription_nonEmpty() {
        String desc = extractor.getDescription();
        assertNotNull(desc);
        assertTrue(desc.contains("StackOverflow"));
    }

    @Test
    void testIsEnabled_defaultTrue() {
        assertTrue(extractor.isEnabled());
    }

    @Test
    void testGetRateLimitPerMinute_default10() {
        assertEquals(10, extractor.getRateLimitPerMinute());
    }

    @Test
    void testExtractErrorSignature_usesTitleByDefault() {
        ActiveInternetScraper.ScrapedIssue issue = new ActiveInternetScraper.ScrapedIssue(
            "How to fix NullPointerException?",
            "Check your objects",
            "StackOverflow"
        );
        // Use default implementation from SiteExtractor interface
        try {
            String signature = extractor.extractErrorSignature(issue);
            assertEquals("How to fix NullPointerException?", signature);
        } catch (Exception e) {
            fail("extractErrorSignature should use default implementation: " + e.getMessage());
        }
    }
}
