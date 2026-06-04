package com.supremeai.learning.active;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WikipediaExtractor.
 * Tests tech-related filtering and Wikipedia API parsing.
 */
class WikipediaExtractorTest {

    private WikipediaExtractor extractor;

    @Mock
    private RestTemplate mockRestTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        extractor = new WikipediaExtractor();
    }

    @Test
    void testGetSourceName_returnsWikipedia() {
        assertEquals("Wikipedia", extractor.getSourceName());
    }

    @Test
    void testGetAuthorityWeight_returns075() {
        assertEquals(0.75, extractor.getAuthorityWeight(), 0.001);
    }

    @Test
    void testGetDescription_nonEmpty() {
        assertNotNull(extractor.getDescription());
        assertTrue(extractor.getDescription().length() > 0);
    }

    @Test
    void testIsTechnologyRelated_techKeywords() {
        assertTrue(invokeIsTechnologyRelated("Python programming language"));
        assertTrue(invokeIsTechnologyRelated("Machine learning algorithms"));
        assertTrue(invokeIsTechnologyRelated("React framework release"));
        assertTrue(invokeIsTechnologyRelated("Docker containerization"));
    }

    @Test
    void testIsTechnologyRelated_nonTechKeywords() {
        assertFalse(invokeIsTechnologyRelated("Sports news today"));
        assertFalse(invokeIsTechnologyRelated("Celebrity gossip"));
        assertFalse(invokeIsTechnologyRelated("Cooking recipes"));
    }

    @Test
    void testIsTechnologyRelated_null_returnsFalse() {
        assertFalse(invokeIsTechnologyRelated(null));
    }

    @Test
    void testIsEnabled_defaultTrue() {
        assertTrue(extractor.isEnabled());
    }

    @Test
    void testGetRateLimitPerMinute_default10() {
        assertEquals(10, extractor.getRateLimitPerMinute());
    }

    private boolean invokeIsTechnologyRelated(String title) {
        try {
            var method = WikipediaExtractor.class.getDeclaredMethod("isTechnologyRelated", String.class);
            method.setAccessible(true);
            return (boolean) method.invoke(extractor, title);
        } catch (Exception e) {
            fail("Failed: " + e.getMessage());
            return false;
        }
    }
}
