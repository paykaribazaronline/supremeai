package com.supremeai.intelligence.profiling;

import com.supremeai.fallback.AIProvider;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Learns which AI is actually good at which specific task.
 * E.g., Groq might be fast at Java, but Gemini might be better at SQL fixes.
 */
@Service
public class AIProfiler {

    // Map: TaskCategory -> (Map: AIProvider -> TaskPerformanceProfile)
    // E.g., "SQL_FIX" -> { GROQ_LLAMA3 -> [SuccessRate: 90%, AvgSpeed: 200ms] }
    private final Map<String, Map<AIProvider, TaskPerformanceProfile>> providerProfiles = new ConcurrentHashMap<>();

    public void recordPerformance(String taskCategory, AIProvider provider, boolean success, long executionTimeMs) {
        Map<AIProvider, TaskPerformanceProfile> categoryProfiles = providerProfiles.computeIfAbsent(taskCategory, k -> new ConcurrentHashMap<>());
        TaskPerformanceProfile profile = categoryProfiles.computeIfAbsent(provider, k -> new TaskPerformanceProfile());

        profile.update(success, executionTimeMs);
        System.out.println(String.format("[AI Profiler] Updated %s for task '%s'. Success Rate: %.1f%%, Avg Speed: %dms", 
                provider.name(), taskCategory, profile.getSuccessRate() * 100, profile.getAverageSpeedMs()));
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

        System.out.println("[AI Profiler] Selected " + bestProvider + " as the expert for task: " + taskCategory);
        return bestProvider;
    }
}