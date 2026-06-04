package com.supremeai.learning.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SolutionMemory. Tests supreme score calculation, versioning, obsoleting, and
 * recency decay.
 */
class SolutionMemoryTest {

  private SolutionMemory solution;

  @BeforeEach
  void setUp() {
    solution =
        new SolutionMemory("error_sig_123", "System.out.println('fixed');", "openai", 100L, 0.8);
  }

  @Test
  void testCalculateSupremeScore_highPerformance() {
    solution.setSuccessCount(10);
    solution.setFailureCount(0);
    solution.setExecutionTimeMs(50);
    solution.setCodeLength(20);
    solution.setSecurityScore(0.9);
    solution.setTimeless(true);

    double score = solution.calculateSupremeScore();
    assertTrue(score > 0.8, "High-performing solution should have high supreme score");
  }

  @Test
  void testCalculateSupremeScore_lowPerformance() {
    solution.setSuccessCount(1);
    solution.setFailureCount(9);
    solution.setExecutionTimeMs(5000);
    solution.setCodeLength(1000);
    solution.setSecurityScore(0.2);

    double score = solution.calculateSupremeScore();
    assertTrue(score < 0.5, "Poor solution should have low supreme score");
  }

  @Test
  void testCalculateSupremeScore_zeroAttempts_returnsZero() {
    solution.setSuccessCount(0);
    solution.setFailureCount(0);
    double score = solution.calculateSupremeScore();
    assertEquals(0.0, score);
  }

  @Test
  void testCalculateSupremeScore_timelessFlag_disablesDecay() throws InterruptedException {
    solution.setSuccessCount(5);
    solution.setFailureCount(0);
    solution.setSecurityScore(0.8);
    solution.setTimeless(true);

    double score1 = solution.calculateSupremeScore();
    // Wait a bit to let time pass
    Thread.sleep(100);
    double score2 = solution.calculateSupremeScore();

    assertEquals(score1, score2, 0.001, "Timeless solution score should not decay over time");
  }

  @Test
  void testCalculateSupremeScore_nonTimeless_decaysOverTime() throws InterruptedException {
    solution.setSuccessCount(5);
    solution.setFailureCount(0);
    solution.setSecurityScore(0.8);
    solution.setTimeless(false);

    double score1 = solution.calculateSupremeScore();

    // Mock the timestamp to 10 days ago
    solution.setTimestamp(LocalDateTime.now().minusDays(10));

    double score2 = solution.calculateSupremeScore();

    assertTrue(score2 < score1, "Non-timeless solution should decay over time");
  }

  @Test
  void testIncrementSuccess_increasesSuccessCount() {
    assertEquals(1, solution.getSuccessCount());
    solution.incrementSuccess();
    assertEquals(2, solution.getSuccessCount());
  }

  @Test
  void testIncrementFailure_increasesFailureCount() {
    assertEquals(0, solution.getFailureCount());
    solution.incrementFailure();
    assertEquals(1, solution.getFailureCount());
  }

  @Test
  void testMarkObsolete_setsObsoleteFlags() {
    solution.markObsolete("Deprecated approach");

    assertTrue(solution.isObsolete());
    assertEquals("Deprecated approach", solution.getObsoleteReason());
    assertNotNull(solution.getObsoletedAt());
  }

  @Test
  void testCreateUpdate_createsNewVersion() {
    SolutionMemory updated = solution.createUpdate("System.out.println('updated');", 80L, 0.9);

    assertEquals(2L, updated.getVersion());
    assertEquals(solution.getId(), updated.getPreviousVersionId());
    assertEquals(solution.getTriggerError(), updated.getTriggerError());
    assertEquals("System.out.println('updated');", updated.getResolvedCode());
    assertEquals(0.9, updated.getSecurityScore(), 0.001);
    // Previous counters are inherited
    assertEquals(1, updated.getSuccessCount());
    assertEquals(0, updated.getFailureCount());
  }

  @Test
  void testSetResolvedCode_updatesCodeLength() {
    assertEquals(28, solution.getCodeLength());
    solution.setResolvedCode("short");
    assertEquals(5, solution.getCodeLength());
  }

  @Test
  void testSetTimestamp_updatesTimestamp() {
    LocalDateTime now = LocalDateTime.now();
    solution.setTimestamp(now);
    assertEquals(now, solution.getTimestamp());
  }

  @Test
  void testSupremeScoreComponents_weightedCorrectly() {
    // Success rate: 1/1 = 1.0 × 0.5 = 0.5
    solution.setSuccessCount(1);
    solution.setFailureCount(0);
    solution.setExecutionTimeMs(0); // speedScore = 1 - 0 = 1.0 × 0.1 = 0.1
    solution.setCodeLength(10); // simplicityScore = 1 - 10/500 = 0.98 × 0.1 ≈ 0.098
    solution.setSecurityScore(1.0); // × 0.3 = 0.3
    solution.setTimeless(true);

    double score = solution.calculateSupremeScore();
    // Without decay: 0.5 + 0.3 + 0.1 + 0.098 = ~0.998
    assertTrue(score > 0.95 && score < 1.0, "Score should sum weighted components correctly");
  }

  @Test
  void testLineageMetadata() {
    solution.setSourceUrl("https://stackoverflow.com/q/12345");
    solution.setSourceSite("StackOverflow");
    solution.setSourceAuthority(0.85);
    solution.setExtractedBy("ActiveInternetScraper");
    solution.setValidationStatus("VALIDATED");
    solution.setValidatedBy("admin1");

    assertEquals("https://stackoverflow.com/q/12345", solution.getSourceUrl());
    assertEquals("StackOverflow", solution.getSourceSite());
    assertEquals(0.85, solution.getSourceAuthority(), 0.001);
    assertEquals("ActiveInternetScraper", solution.getExtractedBy());
    assertEquals("VALIDATED", solution.getValidationStatus());
    assertEquals("admin1", solution.getValidatedBy());
  }

  @Test
  void testGettersAndSetters() {
    solution.setId("mem_001");
    solution.setTriggerError("NullPointerException");
    solution.setWorkingAIProvider("anthropic");
    solution.setSuccessCount(5);
    solution.setFailureCount(2);
    solution.setVersion(3L);
    solution.setPreviousVersionId("mem_prev");
    solution.setObsolete(false);
    solution.setTimeless(true);

    assertEquals("mem_001", solution.getId());
    assertEquals("NullPointerException", solution.getTriggerError());
    assertEquals("anthropic", solution.getWorkingAIProvider());
    assertEquals(5, solution.getSuccessCount());
    assertEquals(2, solution.getFailureCount());
    assertEquals(3L, solution.getVersion());
    assertEquals("mem_prev", solution.getPreviousVersionId());
    assertFalse(solution.isObsolete());
    assertTrue(solution.isTimeless());
  }
}
