package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.model.TaskProviderAssignment;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automatically benchmarks providers across task types
 * to discover their capabilities without hardcoded scores.
 *
 * When a new provider is added, this service:
 * 1. Runs standardized tests
 * 2. Measures performance
 * 3. Stores capability scores
 * 4. Auto-assigns to appropriate tasks
 */
@Service
public class ProviderCapabilityAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ProviderCapabilityAnalyzer.class);

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private ProviderInitializationService providerInitService;

    /** Task types the system supports */
    private final List<TaskDefinition> taskDefinitions = List.of(
        new TaskDefinition("code_generation", "Code Generation", "Write a Python function to reverse a linked list"),
        new TaskDefinition("code_review", "Code Review", "Review this code for bugs and suggest improvements"),
        new TaskDefinition("debugging", "Debugging", "Fix this code that has a NullPointerException"),
        new TaskDefinition("creative_writing", "Creative Writing", "Write a short story about a robot learning to paint"),
        new TaskDefinition("summarization", "Summarization", "Summarize this article in 3 sentences"),
        new TaskDefinition("question_answering", "Question Answering", "What is quantum computing? Explain simply"),
        new TaskDefinition("mathematical_reasoning", "Math Reasoning", "Solve: If a train travels 300 miles at 60mph, how long?"),
        new TaskDefinition("bengali_translation", "Bengali Translation", "Translate: I am going to school"),
        new TaskDefinition("vision_analysis", "Vision Analysis", "Describe what you see in this diagram"),
        new TaskDefinition("long_context", "Long Context Analysis", "Summarize this 10K word document")
    );

    /** Cache benchmark results temporarily */
    private final Map<String, Map<String, Double>> benchmarkCache = new ConcurrentHashMap<>();

    /**
     * Run benchmarks for a provider and return capability scores
     */
    public Mono<Map<String, Double>> benchmarkProvider(String providerName) {
        return Mono.fromCallable(() -> {
            log.info("🚀 Starting benchmark for provider: {}", providerName);
            long startTime = System.currentTimeMillis();

            Map<String, Double> scores = new HashMap<>();
            AIProvider provider;

            try {
                provider = providerFactory.getProvider(providerName);
            } catch (Exception e) {
                log.error("Provider {} not available: {}", providerName, e.getMessage());
                return scores;
            }

            // Run tests in parallel for efficiency
            List<Mono<Map.Entry<String, Double>>> benchmarkMonos = new ArrayList<>();

            for (TaskDefinition task : taskDefinitions) {
                benchmarkMonos.add(
                    Mono.<Map.Entry<String, Double>>fromCallable(() -> {
                        double score = runSingleBenchmark(provider, task);
                        log.info("  {} - {}: {:.2f}", providerName, task.getName(), score);
                        return new AbstractMap.SimpleEntry<>(task.getName(), score);
                    })
                    .onErrorResume(e -> {
                        log.warn("Benchmark failed for {} on {}: {}", providerName, task.getName(), e.getMessage());
                        return Mono.<Map.Entry<String, Double>>just(new AbstractMap.SimpleEntry<>(task.getName(), 0.0));
                    })
                );
            }

            // Execute all benchmarks and collect
            List<Map.Entry<String, Double>> results = Flux.merge(benchmarkMonos)
                .collectList()
                .block();

            for (Map.Entry<String, Double> entry : results) {
                scores.put(entry.getKey(), entry.getValue());
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Benchmark complete for {}: {} scores in {}ms", providerName, scores.size(), duration);

            // Cache results
            benchmarkCache.put(providerName, new HashMap<>(scores));

            return scores;
        });
    }

    /**
     * Run a single benchmark test for a provider on a specific task
     */
    private double runSingleBenchmark(AIProvider provider, TaskDefinition task) {
        try {
            long start = System.currentTimeMillis();
            String response = provider.generate(
                "TASK: " + task.getName() + "\n\n" + task.getTestPrompt()
            ).block();
            long latency = System.currentTimeMillis() - start;

            if (response == null || response.isEmpty()) {
                return 0.0;
            }

            // Score based on:
            // 1. Response quality (length, relevance)
            // 2. Latency (faster = better)
            // 3. Completeness

            double qualityScore = evaluateQuality(response, task);
            double latencyScore = evaluateLatency(latency);
            double completenessScore = evaluateCompleteness(response, task);

            // Weighted combination
            return (qualityScore * 0.5) + (latencyScore * 0.2) + (completenessScore * 0.3);

        } catch (Exception e) {
            log.warn("Benchmark error for task {}: {}", task.getName(), e.getMessage());
            return 0.0;
        }
    }

    /**
     * Evaluate response quality (0.0 - 1.0)
     */
    private double evaluateQuality(String response, TaskDefinition task) {
        if (response == null || response.trim().isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        String lower = response.toLowerCase();

        // Check response length (too short = likely bad, too long = potentially verbose)
        int len = response.trim().length();
        if (len >= 50 && len <= 5000) {
            score += 0.4;  // Good length
        } else if (len > 50) {
            score += 0.2;  // Very long but has content
        }

        // Check for code blocks (good for code tasks)
        if (task.getName().contains("code") || task.getName().contains("debug")) {
            if (response.contains("```") || response.contains("def ") || response.contains("function")) {
                score += 0.3;
            }
        }

        // Check for explanations
        if (lower.contains("because") || lower.contains("reason") || lower.contains("explanation")) {
            score += 0.15;
        }

        // Check for structure
        if (response.contains("\n\n") || response.contains("1.") || response.contains("Step")) {
            score += 0.15;
        }

        return Math.min(score, 1.0);
    }

    /**
     * Evaluate latency (0.0 - 1.0, lower is better)
     */
    private double evaluateLatency(long latencyMs) {
        if (latencyMs <= 1000) return 1.0;
        if (latencyMs <= 3000) return 0.8;
        if (latencyMs <= 5000) return 0.6;
        if (latencyMs <= 10000) return 0.4;
        if (latencyMs <= 20000) return 0.2;
        return 0.0;
    }

    /**
     * Evaluate completeness (0.0 - 1.0)
     */
    private double evaluateCompleteness(String response, TaskDefinition task) {
        if (response == null) return 0.0;

        String lower = response.toLowerCase();

        // Check if response actually addresses the task
        switch (task.getName()) {
            case "code_generation":
                return (lower.contains("def ") || lower.contains("function") ||
                        lower.contains("class")) ? 0.9 : 0.3;
            case "question_answering":
                return (lower.contains("?") || lower.contains("is") || lower.contains("are") ||
                        lower.length() > 100) ? 0.8 : 0.3;
            case "translation":
                return (response.length() > 20) ? 0.8 : 0.3;
            default:
                return (response.length() > 50) ? 0.7 : 0.3;
        }
    }

    /**
     * Get cached benchmark results (or null if not cached)
     */
    public Map<String, Double> getCachedScores(String providerName) {
        return benchmarkCache.get(providerName);
    }

    /**
     * Re-benchmark all active providers (scheduled nightly)
     */
    public Mono<Void> reBenchmarkAll() {
        log.info("🔄 Starting re-benchmark for all active providers...");
        // This will be called from AutomaticTaskAssigner scheduler
        return Mono.empty(); // Implementation depends on provider list source
    }

    /** Internal task definition */
    private static class TaskDefinition {
        private final String name;
        private final String label;
        private final String testPrompt;

        TaskDefinition(String name, String label, String testPrompt) {
            this.name = name;
            this.label = label;
            this.testPrompt = testPrompt;
        }

        String getName() { return name; }
        String getLabel() { return label; }
        String getTestPrompt() { return testPrompt; }
    }
}