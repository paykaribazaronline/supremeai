package com.supremeai.intelligence.profiling;

import com.supremeai.fallback.AIProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Learns which AI is actually good at which specific task.
 * E.g., Groq might be fast at Java, but Gemini might be better at SQL fixes.
 */
@Service
public class AIProfiler {

    private static final Logger log = LoggerFactory.getLogger(AIProfiler.class);
    // Map: TaskCategory -> (Map: AIProvider -> TaskPerformanceProfile)
    // E.g., "SQL_FIX" -> { GROQ_LLAMA3 -> [SuccessRate: 90%, AvgSpeed: 200ms] }
    private final Map<String, Map<AIProvider, TaskPerformanceProfile>> providerProfiles = new ConcurrentHashMap<>();

    public void recordPerformance(String taskCategory, AIProvider provider, boolean success, long executionTimeMs) {
        Map<AIProvider, TaskPerformanceProfile> categoryProfiles = providerProfiles.computeIfAbsent(taskCategory, k -> new ConcurrentHashMap<>());
        TaskPerformanceProfile profile = categoryProfiles.computeIfAbsent(provider, k -> new TaskPerformanceProfile());

        profile.update(success, executionTimeMs);
        log.debug("[AI Profiler] Updated {} for task '{}'. Success Rate: {:.1f}%, Avg Speed: {}ms",
                provider.name(), taskCategory, profile.getSuccessRate() * 100, profile.getAverageSpeedMs());
    }

    /**
     * Recommends the best AI for a specific task based on historical REAL-WORLD performance.
     */
    public AIProvider getBestAIForTask(String taskCategory) {
        Map<AIProvider, TaskPerformanceProfile> categoryProfiles = providerProfiles.get(taskCategory);

        if (categoryProfiles == null || categoryProfiles.isEmpty()) {
            // No historical data yet, fallback to default primary (e.g., GROQ)
            return AIProvider.GROQ_LLAMA3;
        }

        AIProvider bestProvider = null;
        double highestScore = -1.0;

        for (Map.Entry<AIProvider, TaskPerformanceProfile> entry : categoryProfiles.entrySet()) {
            double score = entry.getValue().calculateOverallScore();
            if (score > highestScore) {
                highestScore = score;
                bestProvider = entry.getKey();
            }
        }

        log.info("[AI Profiler] Selected {} as the expert for task: {}", bestProvider, taskCategory);
        return bestProvider;
    }
}