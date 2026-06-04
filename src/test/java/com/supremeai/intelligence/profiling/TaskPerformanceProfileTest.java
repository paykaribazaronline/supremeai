package com.supremeai.intelligence.profiling;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for TaskPerformanceProfile. Tests performance tracking, success rate calculation, and
 * overall scoring.
 */
class TaskPerformanceProfileTest {

  private int getTotalAttempts(TaskPerformanceProfile profile) {
    return (int) ReflectionTestUtils.getField(profile, "totalAttempts");
  }

  private int getSuccessCount(TaskPerformanceProfile profile) {
    return (int) ReflectionTestUtils.getField(profile, "successCount");
  }

  @Test
  void testDefaultValues() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    assertEquals(0, getTotalAttempts(profile));
    assertEquals(0, getSuccessCount(profile));
    assertEquals(0, profile.getAverageSpeedMs());
    assertEquals(0.0, profile.getSuccessRate());
    assertEquals(0.0, profile.calculateOverallScore());
  }

  @Test
  void testUpdate_success() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(true, 100);
    profile.update(true, 200);

    assertEquals(2, getTotalAttempts(profile));
    assertEquals(2, getSuccessCount(profile));
    assertEquals(150, profile.getAverageSpeedMs());
    assertEquals(1.0, profile.getSuccessRate());
  }

  @Test
  void testUpdate_failure() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(false, 100);
    profile.update(false, 200);

    assertEquals(2, getTotalAttempts(profile));
    assertEquals(0, getSuccessCount(profile));
    assertEquals(150, profile.getAverageSpeedMs());
    assertEquals(0.0, profile.getSuccessRate());
  }

  @Test
  void testUpdate_mixedSuccessAndFailure() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(true, 100); // success
    profile.update(false, 150); // fail
    profile.update(true, 200); // success
    profile.update(true, 250); // success

    assertEquals(4, getTotalAttempts(profile));
    assertEquals(3, getSuccessCount(profile));
    assertEquals(175, profile.getAverageSpeedMs());
    assertEquals(0.75, profile.getSuccessRate());
  }

  @Test
  void testCalculateOverallScore_perfectPerformance() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(true, 100);
    profile.update(true, 100);
    profile.update(true, 100);

    double score = profile.calculateOverallScore();

    assertEquals(0.70 + (1000.0 / 100 * 0.30), score, 0.01);
  }

  @Test
  void testCalculateOverallScore_poorPerformance() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(false, 1000);
    profile.update(false, 2000);
    profile.update(false, 3000);

    double score = profile.calculateOverallScore();

    assertEquals(0.0, score, 0.01);
  }

  @Test
  void testCalculateOverallScore_mediumPerformance() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(true, 500);
    profile.update(false, 500);
    profile.update(true, 500);
    profile.update(false, 500);

    double score = profile.calculateOverallScore();
    // 50% success, avg speed 500ms -> 1000/500 = 2 -> 2*0.3=0.6 + 0.5*0.7=0.35 -> 0.95
    assertEquals(0.95, score, 0.01);
  }

  @Test
  void testCalculateOverallScore_zeroSpeedPrevention() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(true, 0);

    double score = profile.calculateOverallScore();

    // avgSpeed=0 gets clamped to 1 to avoid division by zero
    // Score = 1.0 * 0.7 + (1000.0 / 1) * 0.3 = 0.7 + 300 = 300.7
    assertEquals(300.7, score, 0.01);
  }

  @Test
  void testUpdate_concurrentAccess() throws Exception {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();
    int threads = 10;
    int updatesPerThread = 50;
    Thread[] threadList = new Thread[threads];

    for (int t = 0; t < threads; t++) {
      threadList[t] =
          new Thread(
              () -> {
                for (int i = 0; i < updatesPerThread; i++) {
                  profile.update(true, 100);
                }
              });
      threadList[t].start();
    }

    for (Thread t : threadList) {
      t.join();
    }

    assertEquals(threads * updatesPerThread, getTotalAttempts(profile));
    assertEquals(threads * updatesPerThread, getSuccessCount(profile));
  }

  @Test
  void testUpdate_concurrentMixedAccess() throws Exception {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();
    int threads = 5;

    Thread[] writers = new Thread[threads];
    for (int t = 0; t < threads; t++) {
      final int threadNum = t;
      writers[t] =
          new Thread(
              () -> {
                for (int i = 0; i < 20; i++) {
                  profile.update(i % 2 == 0, 100 + threadNum * 10);
                }
              });
      writers[t].start();
    }

    for (Thread t : writers) {
      t.join();
    }

    assertEquals(threads * 20, getTotalAttempts(profile));
    assertTrue(getSuccessCount(profile) > 0);
  }

  @Test
  void testAverageSpeedCalculation_varyingSpeeds() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(true, 100);
    profile.update(true, 300);
    profile.update(true, 500);
    profile.update(true, 700);

    // Total = 1600, count = 4, avg = 400
    assertEquals(400, profile.getAverageSpeedMs());
  }

  @Test
  void testSuccessRate_singleSuccess() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(true, 100);

    assertEquals(1.0, profile.getSuccessRate());
  }

  @Test
  void testSuccessRate_singleFailure() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(false, 100);

    assertEquals(0.0, profile.getSuccessRate());
  }

  @Test
  void testScoreWeighting_70PercentSuccess30PercentSpeed() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(true, 100); // 100ms -> speed factor = 10
    profile.update(true, 100);

    double score = profile.calculateOverallScore();

    // 100% success * 0.7 + (1000/100)*0.3 = 0.7 + 3.0 = 3.7
    assertEquals(3.7, score, 0.01);
  }

  @Test
  void testUpdate_largeNumberOfAttempts() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    for (int i = 0; i < 10000; i++) {
      profile.update(i % 2 == 0, 100 + i);
    }

    assertEquals(10000, getTotalAttempts(profile));
    assertEquals(5000, getSuccessCount(profile));
    assertTrue(profile.getAverageSpeedMs() > 0);
  }

  @Test
  void testCalculateOverallScore_verySlowSpeed() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(true, 10000); // 10 seconds

    double score = profile.calculateOverallScore();

    // Success rate 1.0 * 0.7 + (1000/10000)*0.3 = 0.7 + 0.03 = 0.73
    assertEquals(0.73, score, 0.01);
  }

  @Test
  void testCalculateOverallScore_whenAvgSpeedIsVeryFast() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    profile.update(true, 1); // 1ms (super fast)

    double score = profile.calculateOverallScore();

    // 1.0 * 0.7 + (1000/1)*0.3 = 0.7 + 300 = 300.7
    assertEquals(300.7, score, 0.01);
  }

  @Test
  void testUpdate_manyFastSuccesses() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    for (int i = 0; i < 100; i++) {
      profile.update(true, 1);
    }

    assertEquals(100, getTotalAttempts(profile));
    assertEquals(100, getSuccessCount(profile));
    assertEquals(1, profile.getAverageSpeedMs());
    assertEquals(1.0, profile.getSuccessRate());
  }

  @Test
  void testRecordPerformance_negativeExecutionTime() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();

    assertDoesNotThrow(
        () -> {
          profile.update(true, -100);
        });
  }

  @Test
  void testAverageSpeedWithSingleUpdate() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();
    profile.update(true, 250);

    assertEquals(250, profile.getAverageSpeedMs());
  }

  @Test
  void testScoreWhenNoUpdates() {
    TaskPerformanceProfile profile = new TaskPerformanceProfile();
    double score = profile.calculateOverallScore();
    assertEquals(0.0, score);
  }
}
