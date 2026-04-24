package com.supremeai.service;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Contextual AI Ranking Service that automatically selects the best AI
 * for each task type based on historical performance.
 */
@Service
public class ContextualAIRankingService {

    private static final Logger log = LoggerFactory.getLogger(ContextualAIRankingService.class);

    @Autowired
    private AIProviderFactory providerFactory;

    // Task types for categorization
    public enum TaskType {
        CODE_GENERATION,
        CODE_REVIEW,
        DEBUGGING,
        DOCUMENTATION,
        REFACTORING,
        TESTING,
        QUESTION_ANSWERING,
        SUMMARIZATION,
        TRANSLATION,
        CREATIVE_WRITING
    }

    // Performance tracking per provider per task type
    private final Map<String, Map<TaskType, TaskPerformance>> performanceTracker = new ConcurrentHashMap<>();

    // Task-specific features for ML-based selection
    private final Map<TaskType, List<String>> taskKeywords = Map.of(
        TaskType.CODE_GENERATION, List.of("build", "create", "generate", "make", "implement", "code"),
        TaskType.CODE_REVIEW, List.of("review", "check", "analyze", "inspect", "audit"),
        TaskType.DEBUGGING, List.of("debug", "fix", "error", "bug", "issue", "problem"),
        TaskType.DOCUMENTATION, List.of("document", "docs", "readme", "comment", "explain"),
        TaskType.REFACTORING, List.of("refactor", "optimize", "improve", "clean", "restructure"),
        TaskType.TESTING, List.of("test", "spec", "unittest", "integration", "testing"),
        TaskType.QUESTION_ANSWERING, List.of("what", "how", "why", "when", "where", "explain"),
        TaskType.SUMMARIZATION, List.of("summarize", "summary", "tldr", "brief"),
        TaskType.TRANSLATION, List.of("translate", "convert", "to spanish", "to french", "to german"),
        TaskType.CREATIVE_WRITING, List.of("write", "story", "blog", "article", "creative")
    );

    /**
     * Automatically select the best AI provider for a given task.
     */
    public ProviderSelection selectBestProvider(String prompt, String requestedTaskType) {
        // Detect task type from prompt if not specified
        TaskType taskType = requestedTaskType != null ?
            TaskType.valueOf(requestedTaskType) :
            detectTaskType(prompt);

        log.info("Selecting best provider for task type: {}", taskType);

        // Get all available providers
        List<String> allProviders = getAllProviderNames();

        if (allProviders.isEmpty()) {
            return new ProviderSelection(null, taskType, 0.0, "No providers available");
        }

        // Score each provider for this task
        Map<String, Double> scores = new HashMap<>();
        for (String provider : allProviders) {
            double score = calculateProviderScore(provider, taskType, prompt);
            scores.put(provider, score);
        }

        // Select best provider
        String bestProvider = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(allProviders.get(0));

        double confidence = scores.get(bestProvider) / scores.values().stream()
            .max(Double::compareTo).orElse(1.0);

        String reason = String.format("Selected based on historical performance for %s tasks (score: %.2f)",
            taskType, scores.get(bestProvider));

        return new ProviderSelection(bestProvider, taskType, confidence, reason);
    }

    /**
     * Detect task type from prompt using keyword matching.
     */
    public TaskType detectTaskType(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return TaskType.QUESTION_ANSWERING; // Default
        }

        String lowerPrompt = prompt.toLowerCase(Locale.ROOT);
        Map<TaskType, Integer> matches = new HashMap<>();

        for (TaskType task : TaskType.values()) {
            List<String> keywords = taskKeywords.getOrDefault(task, List.of());
            int count = keywords.stream()
                .mapToInt(kw -> lowerPrompt.contains(kw) ? 1 : 0)
                .sum();
            matches.put(task, count);
        }

        return matches.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(e -> e.getValue() > 0)
            .map(Map.Entry::getKey)
            .orElse(TaskType.QUESTION_ANSWERING);
    }

    /**
     * Calculate how good a provider is for a specific task.
     */
    private double calculateProviderScore(String provider, TaskType taskType, String prompt) {
        Map<TaskType, TaskPerformance> providerPerf = performanceTracker
            .computeIfAbsent(provider, k -> new ConcurrentHashMap<>());

        TaskPerformance perf = providerPerf.get(taskType);
        if (perf == null) {
            // No historical data - use default ranking
            return getDefaultScore(provider, taskType);
        }

        // Score = weighted combination of metrics
        double successRateScore = perf.successRate;
        double avgResponseTimeScore = 1.0 / (1.0 + perf.averageResponseTimeMs / 1000.0); // Normalize
        double avgQualityScore = perf.averageQualityScore / 5.0; // Normalize to 0-1
        double recencyScore = calculateRecencyScore(perf.lastUsed);

        // Weights
        return successRateScore * 0.4 +
               avgResponseTimeScore * 0.2 +
               avgQualityScore * 0.3 +
               recencyScore * 0.1;
    }

    /**
     * Get default score for providers with no history.
     */
    private double getDefaultScore(String provider, TaskType taskType) {
        // Some providers are known to be better at certain tasks
        Map<String, Map<TaskType, Double>> defaultScores = new HashMap<>();

        // Example: GPT-4 is generally good at code
        defaultScores.computeIfAbsent("openai", k -> new HashMap<>())
            .put(TaskType.CODE_GENERATION, 0.9);
        defaultScores.get("openai").put(TaskType.DEBUGGING, 0.85);

        // Claude is good at analysis
        defaultScores.computeIfAbsent("anthropic", k -> new HashMap<>())
            .put(TaskType.CODE_REVIEW, 0.9);
        defaultScores.get("anthropic").put(TaskType.DOCUMENTATION, 0.88);

        return defaultScores
            .getOrDefault(provider, Map.of())
            .getOrDefault(taskType, 0.5); // Default 0.5 if unknown
    }

    /**
     * Calculate recency score (higher if used recently for this task).
     */
    private double calculateRecencyScore(long lastUsed) {
        if (lastUsed == 0) return 0.5;
        long hoursSince = (System.currentTimeMillis() - lastUsed) / (1000 * 60 * 60);
        return Math.max(0.0, 1.0 - (hoursSince / 24.0)); // Decay over 24 hours
    }

    /**
     * Record the outcome of a task to improve future selections.
     */
    public void recordTaskOutcome(String provider, TaskType taskType, boolean success,
                                  long responseTimeMs, double qualityScore) {
        Map<TaskType, TaskPerformance> providerPerf = performanceTracker
            .computeIfAbsent(provider, k -> new ConcurrentHashMap<>());

        TaskPerformance perf = providerPerf.computeIfAbsent(taskType,
            k -> new TaskPerformance());

        // Update with exponential moving average
        double alpha = 0.3;
        perf.successRate = success ?
            perf.successRate + alpha * (1.0 - perf.successRate) :
            perf.successRate - alpha * perf.successRate;

        perf.averageResponseTimeMs =
            (perf.averageResponseTimeMs * (perf.totalTasks) + responseTimeMs) / (perf.totalTasks + 1);
        perf.averageQualityScore =
            (perf.averageQualityScore * (perf.totalTasks) + qualityScore) / (perf.totalTasks + 1);

        perf.totalTasks++;
        perf.lastUsed = System.currentTimeMillis();

        log.info("Recorded outcome for {} on {}: success={}, quality={}", provider, taskType, success, qualityScore);
    }

    /**
     * Get all available provider names dynamically from factory.
     */
    private List<String> getAllProviderNames() {
        try {
            // Get all registered providers from the factory
            String[] providerNames = providerFactory.getAllProviderNames();
            return java.util.Arrays.asList(providerNames);
        } catch (Exception e) {
            log.error("Failed to get provider names from factory", e);
            return List.of();
        }
    }

    /**
     * Get ranking for a specific task type.
     */
    public List<ProviderRanking> getRankingsForTask(TaskType taskType) {
        List<ProviderRanking> rankings = new ArrayList<>();

        for (Map.Entry<String, Map<TaskType, TaskPerformance>> entry : performanceTracker.entrySet()) {
            String provider = entry.getKey();
            TaskPerformance perf = entry.getValue().get(taskType);

            if (perf != null) {
                rankings.add(new ProviderRanking(
                    provider,
                    taskType.name(),
                    perf.successRate,
                    perf.averageResponseTimeMs,
                    perf.averageQualityScore,
                    perf.totalTasks
                ));
            }
        }

        rankings.sort((a, b) -> Double.compare(b.successRate, a.successRate));
        return rankings;
    }

    /**
     * Get statistics for monitoring.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProviders", performanceTracker.size());
        stats.put("totalTaskTypes", TaskType.values().length);

        int totalRecords = performanceTracker.values().stream()
            .mapToInt(m -> m.values().stream().mapToInt(p -> p.totalTasks).sum())
            .sum();
        stats.put("totalTaskRecords", totalRecords);

        return stats;
    }

    // ── Data Classes ──────────────────────────────────────────────────────────

    public static class ProviderSelection {
        public final String providerName;
        public final TaskType taskType;
        public final double confidence;
        public final String reason;

        public ProviderSelection(String providerName, TaskType taskType,
                                double confidence, String reason) {
            this.providerName = providerName;
            this.taskType = taskType;
            this.confidence = confidence;
            this.reason = reason;
        }
    }

    public static class ProviderRanking {
        public final String provider;
        public final String taskType;
        public final double successRate;
        public final double avgResponseTimeMs;
        public final double avgQualityScore;
        public final int totalTasks;

        public ProviderRanking(String provider, String taskType, double successRate,
                               double avgResponseTimeMs, double avgQualityScore, int totalTasks) {
            this.provider = provider;
            this.taskType = taskType;
            this.successRate = successRate;
            this.avgResponseTimeMs = avgResponseTimeMs;
            this.avgQualityScore = avgQualityScore;
            this.totalTasks = totalTasks;
        }
    }

    private static class TaskPerformance {
        double successRate = 0.5;
        double averageResponseTimeMs = 1000.0;
        double averageQualityScore = 3.0;
        int totalTasks = 0;
        long lastUsed = 0;
    }
}
