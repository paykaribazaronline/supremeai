package com.supremeai.learning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.learning.service.EnhancedContentSanitizerService;
import com.supremeai.learning.active.ActiveInternetScraper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ActiveInternetScraperTest {

    @Mock
    private RestTemplate mockRestTemplate;

    @Mock
    private ObjectMapper mockObjectMapper;

    @Mock
    private EnhancedContentSanitizerService mockSanitizer;

    private ActiveInternetScraper scraper;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        scraper = new ActiveInternetScraper(mockSanitizer);
        // Inject mocks via reflection
        java.lang.reflect.Field restField = ActiveInternetScraper.class.getDeclaredField("restTemplate");
        java.lang.reflect.Field mapperField = ActiveInternetScraper.class.getDeclaredField("objectMapper");
        restField.setAccessible(true);
        mapperField.setAccessible(true);
        restField.set(scraper, mockRestTemplate);
        mapperField.set(scraper, mockObjectMapper);
    }

    @Test
    void testScrapeTrendingIssues_success() throws Exception {
        String[] responses = {
            "{ \"query\": { \"recentchanges\": [ { \"title\": \"Python programming\" } ] } }",
            "{ \"items\": [ { \"title\": \"How to fix NullPointerException in Java\", \"link\": \"https://stackoverflow.com/q/123\" } ] }"
        };
        when(mockRestTemplate.getForObject(anyString(), eq(String.class))).thenReturn(responses[0], responses[1]);
        JsonNode node1 = createMockJsonNode(responses[0]);
        JsonNode node2 = createMockJsonNode(responses[1]);
        when(mockObjectMapper.readTree(anyString())).thenReturn(node1, node2);

        List<ActiveInternetScraper.ScrapedIssue> results = scraper.scrapeTrendingIssues();

        assertNotNull(results);
        verify(mockRestTemplate, atLeastOnce()).getForObject(anyString(), eq(String.class));
    }

    @Test
    void testEstimateExecutionTime_shortCode() throws Exception {
        Method method = ActiveInternetScraper.class.getDeclaredMethod("estimateExecutionTime", String.class);
        method.setAccessible(true);
        String code = "x = 1;";
        long estimated = (long) method.invoke(scraper, code);
        assertTrue(estimated >= 10 && estimated <= 5000);
    }

    @Test
    void testEstimateExecutionTime_longCode() throws Exception {
        Method method = ActiveInternetScraper.class.getDeclaredMethod("estimateExecutionTime", String.class);
        method.setAccessible(true);
        String code = "x".repeat(1000);
        long estimated = (long) method.invoke(scraper, code);
        assertEquals(2000, estimated, 10);
    }

    @Test
    void testEstimateSecurityScore_detectsEval() throws Exception {
        Method method = ActiveInternetScraper.class.getDeclaredMethod("estimateSecurityScore", String.class);
        method.setAccessible(true);
        String code = "result = eval(user_input)";
        double score = (double) method.invoke(scraper, code);
        assertTrue(score < 0.5, "Eval should produce low security score");
    }

    @Test
    void testEstimateSecurityScore_detectsSystemCall() throws Exception {
        Method method = ActiveInternetScraper.class.getDeclaredMethod("estimateSecurityScore", String.class);
        method.setAccessible(true);
        String code = "Runtime.getRuntime().exec(\"rm -rf /\")";
        double score = (double) method.invoke(scraper, code);
        assertTrue(score < 0.5);
    }

    @Test
    void testEstimateSecurityScore_detectsPassword() throws Exception {
        Method method = ActiveInternetScraper.class.getDeclaredMethod("estimateSecurityScore", String.class);
        method.setAccessible(true);
        String code = "password = 'secret123'";
        double score = (double) method.invoke(scraper, code);
        assertTrue(score < 0.5);
    }

    @Test
    void testEstimateSecurityScore_cleanCode() throws Exception {
        Method method = ActiveInternetScraper.class.getDeclaredMethod("estimateSecurityScore", String.class);
        method.setAccessible(true);
        String code = "System.out.println(\"Hello\");";
        double score = (double) method.invoke(scraper, code);
        assertTrue(score > 0.7, "Clean code should have high security score");
    }

    @Test
    void testIsLikelyTimeless_algorithmKeywords() throws Exception {
        Method method = ActiveInternetScraper.class.getDeclaredMethod("isLikelyTimeless", ActiveInternetScraper.ScrapedIssue.class);
        method.setAccessible(true);
        ActiveInternetScraper.ScrapedIssue issue = new ActiveInternetScraper.ScrapedIssue(
            "How to implement binary search recursively", "solution", "src"
        );
        boolean result = (boolean) method.invoke(scraper, issue);
        assertTrue(result, "Algorithm topics should be timeless");
    }

    @Test
    void testIsLikelyTimeless_nonAlgorithm() throws Exception {
        Method method = ActiveInternetScraper.class.getDeclaredMethod("isLikelyTimeless", ActiveInternetScraper.ScrapedIssue.class);
        method.setAccessible(true);
        ActiveInternetScraper.ScrapedIssue issue = new ActiveInternetScraper.ScrapedIssue(
            "React 18 new features update 2025", "solution", "src"
        );
        boolean result = (boolean) method.invoke(scraper, issue);
        assertFalse(result, "Version-specific tech news is not timeless");
    }

    @Test
    void testConvertToSolution_sanitizerAccepts_returnsMemory() throws Exception {
        ActiveInternetScraper.ScrapedIssue issue = new ActiveInternetScraper.ScrapedIssue(
            "Java memory leak fix", "Use try-with-resources", "StackOverflow"
        );
        issue.setSourceAuthority(0.85);

        when(mockSanitizer.sanitizeAndValidate(any(), anyString()))
            .thenReturn(new EnhancedContentSanitizerService.SanitizationResult(true, "Passed", 1.0, 1.0));

        var memory = scraper.convertToSolution(issue, "OutOfMemoryError");

        assertNotNull(memory);
        assertEquals("OutOfMemoryError", memory.getTriggerError());
        assertEquals("Use try-with-resources", memory.getResolvedCode());
        assertEquals("StackOverflow", memory.getWorkingAIProvider());
    }

    @Test
    void testConvertToSolution_sanitizerRejects_returnsNull() throws Exception {
        ActiveInternetScraper.ScrapedIssue issue = new ActiveInternetScraper.ScrapedIssue(
            "Bad code", "password='secret'", "BadSource"
        );

        when(mockSanitizer.sanitizeAndValidate(any(), anyString()))
            .thenReturn(new EnhancedContentSanitizerService.SanitizationResult(false, "Failed", 0.0, 0.0));

        var memory = scraper.convertToSolution(issue, "error");

        assertNull(memory);
    }

    @Test
    void testScrapedIssue_gettersAndSetters() {
        ActiveInternetScraper.ScrapedIssue issue = new ActiveInternetScraper.ScrapedIssue(
            "title", "solution", "source"
        );
        issue.setSourceAuthority(0.9);
        issue.setRawConfidence(0.85);

        assertEquals(0.9, issue.getSourceAuthority(), 0.001);
        assertEquals(0.85, issue.getRawConfidence(), 0.001);
        assertEquals("title", issue.getTitle());
        assertEquals("solution", issue.getSolution());
        assertEquals("source", issue.getSource());
    }

    private JsonNode createMockJsonNode(String json) {
        JsonNode node = mock(JsonNode.class);
        when(node.path(anyString())).thenReturn(node);
        return node;
    }
}