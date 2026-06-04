package com.supremeai.learning;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for LearningActivityLogService. Tests logging methods produce expected behavior
 * without exceptions.
 */
class LearningActivityLogServiceTest {

  @Test
  void testGenerateSessionId_returnsUniqueIds() {
    LearningActivityLogService service = new LearningActivityLogService();

    String id1 = service.generateSessionId();
    String id2 = service.generateSessionId();

    assertNotNull(id1);
    assertNotNull(id2);
    assertTrue(id1.startsWith("learn_"));
    assertTrue(id2.startsWith("learn_"));
    assertNotEquals(id1, id2, "Generated IDs should be unique");
  }

  @Test
  void testLogSiteAccess_grantedLogsInfo() {
    LearningActivityLogService service = new LearningActivityLogService();
    // Should not throw
    assertDoesNotThrow(
        () -> service.logSiteAccess("https://example.com", true, "allowed", "admin"));
  }

  @Test
  void testLogSiteAccess_deniedLogsWarn() {
    LearningActivityLogService service = new LearningActivityLogService();
    assertDoesNotThrow(
        () -> service.logSiteAccess("https://blocked.com", false, "denied", "admin"));
  }

  @Test
  void testLogScrapingSession_successAndFailure() {
    LearningActivityLogService service = new LearningActivityLogService();
    assertDoesNotThrow(
        () -> {
          service.logScrapingSession("Wikipedia", 10, 1500L, true);
          service.logScrapingSession("StackOverflow", 0, 0, false);
        });
  }

  @Test
  void testLogProposalEvent_allEventTypes() {
    LearningActivityLogService service = new LearningActivityLogService();
    assertDoesNotThrow(
        () -> {
          service.logProposalEvent("prop_001", "SUBMITTED", "New suggestion", "admin1");
          service.logProposalEvent("prop_001", "APPROVED", "Meets criteria", "admin2");
          service.logProposalEvent("prop_001", "REJECTED", "Low quality", "system");
        });
  }

  @Test
  void testLogSolutionEvent_allEventTypes() {
    LearningActivityLogService service = new LearningActivityLogService();
    assertDoesNotThrow(
        () -> {
          service.logSolutionEvent("sol_001", "CREATE", "Initial version");
          service.logSolutionEvent("sol_001", "UPDATE", "Version 2");
          service.logSolutionEvent("sol_001", "OBSOLETE", "Superseded");
        });
  }

  @Test
  void testLogQuotaUsage_variousValues() {
    LearningActivityLogService service = new LearningActivityLogService();
    assertDoesNotThrow(
        () -> {
          service.logQuotaUsage("user1", "SCRAPE", 5, 95);
          service.logQuotaUsage("user2", "LEARN", 10, 40);
        });
  }

  @Test
  void testLogSanitization_approvedAndRejected() {
    LearningActivityLogService service = new LearningActivityLogService();
    assertDoesNotThrow(
        () -> {
          service.logSanitization("Wikipedia", "hash123", true, "clean");
          service.logSanitization("RandomBlog", "hash456", false, "contains secrets");
        });
  }
}
