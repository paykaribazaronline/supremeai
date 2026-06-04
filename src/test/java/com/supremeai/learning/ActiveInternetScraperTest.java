package com.supremeai.learning;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.supremeai.learning.active.ActiveInternetScraper;
import com.supremeai.learning.active.QueryClassifier;
import com.supremeai.learning.service.EnhancedContentSanitizerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

class ActiveInternetScraperTest {

  @Mock private WebClient.Builder mockWebClientBuilder;

  @Mock private EnhancedContentSanitizerService mockSanitizer;

  @Mock private QueryClassifier mockQueryClassifier;

  @Mock private WebClient mockWebClient;

  private ActiveInternetScraper scraper;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    when(mockWebClientBuilder.build()).thenReturn(mockWebClient);
    scraper = new ActiveInternetScraper(mockWebClientBuilder, mockSanitizer, mockQueryClassifier);
  }

  @Test
  void testScrapeTrendingIssues_success() throws Exception {
    List<ActiveInternetScraper.ScrapedIssue> results = scraper.scrapeTrendingIssues();

    assertNotNull(results);
  }

  @Test
  void testConvertToSolution_sanitizerAccepts_returnsMemory() throws Exception {
    ActiveInternetScraper.ScrapedIssue issue =
        new ActiveInternetScraper.ScrapedIssue(
            "Java memory leak fix", "Use try-with-resources", "StackOverflow");
    issue.setSourceAuthority(0.85);

    when(mockSanitizer.sanitizeAndValidate(any(), anyString()))
        .thenReturn(
            new EnhancedContentSanitizerService.SanitizationResult(true, "Passed", 1.0, 1.0));

    var memory = scraper.convertToSolution(issue, "OutOfMemoryError");

    assertNotNull(memory);
    assertEquals("OutOfMemoryError", memory.getTriggerError());
    assertEquals("Use try-with-resources", memory.getResolvedCode());
    assertEquals("StackOverflow", memory.getWorkingAIProvider());
  }

  @Test
  void testConvertToSolution_sanitizerRejects_returnsNull() throws Exception {
    ActiveInternetScraper.ScrapedIssue issue =
        new ActiveInternetScraper.ScrapedIssue("Bad code", "password='secret'", "BadSource");

    when(mockSanitizer.sanitizeAndValidate(any(), anyString()))
        .thenReturn(
            new EnhancedContentSanitizerService.SanitizationResult(false, "Failed", 0.0, 0.0));

    var memory = scraper.convertToSolution(issue, "error");

    assertNull(memory);
  }

  @Test
  void testScrapedIssue_gettersAndSetters() {
    ActiveInternetScraper.ScrapedIssue issue =
        new ActiveInternetScraper.ScrapedIssue("title", "solution", "source");
    issue.setSourceAuthority(0.9);
    issue.setRawConfidence(0.85);

    assertEquals(0.9, issue.getSourceAuthority(), 0.001);
    assertEquals(0.85, issue.getRawConfidence(), 0.001);
    assertEquals("title", issue.getTitle());
    assertEquals("solution", issue.getSolution());
    assertEquals("source", issue.getSource());
  }
}
