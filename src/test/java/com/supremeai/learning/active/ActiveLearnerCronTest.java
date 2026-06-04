package com.supremeai.learning.active;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ActiveLearnerCron. Tests the scheduled learning job that scrapes trending issues
 * and records them.
 */
class ActiveLearnerCronTest {

  @Test
  void testNightlyInternetLearning_invokesScraperAndKnowledgeBase() {
    ActiveInternetScraper scraper = mock(ActiveInternetScraper.class);
    GlobalKnowledgeBase knowledgeBase = mock(GlobalKnowledgeBase.class);

    ActiveLearnerCron cron = new ActiveLearnerCron(scraper, knowledgeBase);

    // Mock scraper to return some issues
    var issues =
        java.util.Arrays.asList(
            createMockIssue("Issue 1", "Solution 1", "GitHub"),
            createMockIssue("Issue 2", "Solution 2", "StackOverflow"));
    when(scraper.scrapeTrendingIssues()).thenReturn(issues);

    cron.nightlyInternetLearning();

    // Verify scraper called
    verify(scraper, times(1)).scrapeTrendingIssues();
    // Verify knowledge base recorded each issue
    verify(knowledgeBase, times(2))
        .recordSuccessWithPermission(anyString(), anyString(), anyString(), eq(100L), eq(0.9));
  }

  @Test
  void testNightlyInternetLearning_emptyListDoesNotCallKnowledgeBase() {
    ActiveInternetScraper scraper = mock(ActiveInternetScraper.class);
    GlobalKnowledgeBase knowledgeBase = mock(GlobalKnowledgeBase.class);

    ActiveLearnerCron cron = new ActiveLearnerCron(scraper, knowledgeBase);

    when(scraper.scrapeTrendingIssues()).thenReturn(java.util.Collections.emptyList());

    cron.nightlyInternetLearning();

    verify(scraper).scrapeTrendingIssues();
    verifyNoInteractions(knowledgeBase);
  }

  @Test
  void testNightlyInternetLearning_nullListHandledGracefully() {
    ActiveInternetScraper scraper = mock(ActiveInternetScraper.class);
    GlobalKnowledgeBase knowledgeBase = mock(GlobalKnowledgeBase.class);

    ActiveLearnerCron cron = new ActiveLearnerCron(scraper, knowledgeBase);

    when(scraper.scrapeTrendingIssues()).thenReturn(null);

    assertDoesNotThrow(
        () -> {
          cron.nightlyInternetLearning();
        });

    verify(scraper).scrapeTrendingIssues();
    verifyNoInteractions(knowledgeBase);
  }

  private ActiveInternetScraper.ScrapedIssue createMockIssue(
      String title, String solution, String source) {
    ActiveInternetScraper.ScrapedIssue issue = mock(ActiveInternetScraper.ScrapedIssue.class);
    when(issue.getTitle()).thenReturn(title);
    when(issue.getSolution()).thenReturn(solution);
    when(issue.getSource()).thenReturn(source);
    return issue;
  }

  @Test
  void testNightlyInternetLearning_multipleIssuesRecorded() {
    ActiveInternetScraper scraper = mock(ActiveInternetScraper.class);
    GlobalKnowledgeBase knowledgeBase = mock(GlobalKnowledgeBase.class);

    ActiveLearnerCron cron = new ActiveLearnerCron(scraper, knowledgeBase);

    var issues = java.util.Collections.nCopies(10, createMockIssue("title", "sol", "src"));
    when(scraper.scrapeTrendingIssues()).thenReturn(issues);

    cron.nightlyInternetLearning();

    verify(scraper).scrapeTrendingIssues();
    verify(knowledgeBase, times(10))
        .recordSuccessWithPermission(anyString(), anyString(), anyString(), eq(100L), eq(0.9));
  }

  @Test
  void testNightlyInternetLearning_knowledgeBaseErrorDoesNotPropagate() {
    ActiveInternetScraper scraper = mock(ActiveInternetScraper.class);
    GlobalKnowledgeBase knowledgeBase = mock(GlobalKnowledgeBase.class);

    ActiveLearnerCron cron = new ActiveLearnerCron(scraper, knowledgeBase);

    var issues = java.util.Arrays.asList(createMockIssue("Issue", "Solution", "src"));
    when(scraper.scrapeTrendingIssues()).thenReturn(issues);
    doThrow(new RuntimeException("DB error"))
        .when(knowledgeBase)
        .recordSuccessWithPermission(anyString(), anyString(), anyString(), anyLong(), anyDouble());

    // Should not throw; job should continue or fail gracefully
    assertDoesNotThrow(
        () -> {
          cron.nightlyInternetLearning();
        });

    verify(knowledgeBase, times(1))
        .recordSuccessWithPermission(anyString(), anyString(), anyString(), anyLong(), anyDouble());
  }

  @Test
  void testNightlyInternetLearning_scraperException() {
    ActiveInternetScraper scraper = mock(ActiveInternetScraper.class);
    GlobalKnowledgeBase knowledgeBase = mock(GlobalKnowledgeBase.class);

    ActiveLearnerCron cron = new ActiveLearnerCron(scraper, knowledgeBase);

    when(scraper.scrapeTrendingIssues()).thenThrow(new RuntimeException("Scraping failed"));

    assertDoesNotThrow(
        () -> {
          cron.nightlyInternetLearning();
        });

    // No recording since scraper failed before returning issues
    verifyNoInteractions(knowledgeBase);
  }
}
