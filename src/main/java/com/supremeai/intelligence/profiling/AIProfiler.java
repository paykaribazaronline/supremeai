package com.supremeai.intelligence.profiling;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Learns which AI is actually good at which specific task. E.g., Groq might be fast at Java, but
 * Gemini might be better at SQL fixes.
 */
@Service
public class AIProfiler {

  private static final Logger log = LoggerFactory.getLogger(AIProfiler.class);
  // Map: TaskCategory -> (Map: ProviderId -> TaskPerformanceProfile)
  private final Map<String, Map<String, TaskPerformanceProfile>> providerProfiles =
      new ConcurrentHashMap<>();

  public void recordPerformance(
      String taskCategory, String provider, boolean success, long executionTimeMs) {
    Map<String, TaskPerformanceProfile> categoryProfiles =
        providerProfiles.computeIfAbsent(taskCategory, k -> new ConcurrentHashMap<>());
    TaskPerformanceProfile profile =
        categoryProfiles.computeIfAbsent(provider, k -> new TaskPerformanceProfile());

    profile.update(success, executionTimeMs);
    log.debug(
        "[AI Profiler] Updated {} for task '{}'. Success Rate: {:.1f}%, Avg Speed: {}ms",
        provider, taskCategory, profile.getSuccessRate() * 100, profile.getAverageSpeedMs());
  }

  /** Recommends the best AI for a specific task based on historical REAL-WORLD performance. */
  public String getBestAIForTask(String taskCategory) {
    Map<String, TaskPerformanceProfile> categoryProfiles = providerProfiles.get(taskCategory);

    if (categoryProfiles == null || categoryProfiles.isEmpty()) {
      // No historical data yet, return null so the orchestrator can use database priority
      return null;
    }

    String bestProviderId = null;
    double highestScore = -1.0;

    for (Map.Entry<String, TaskPerformanceProfile> entry : categoryProfiles.entrySet()) {
      double score = entry.getValue().calculateOverallScore();
      if (score > highestScore) {
        highestScore = score;
        bestProviderId = entry.getKey();
      }
    }

    log.info("[AI Profiler] Selected {} as the expert for task: {}", bestProviderId, taskCategory);
    return bestProviderId;
  }
}
