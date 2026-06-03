package com.supremeai.learning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI Provider Performance Tracker
 *
 * Automatically records outcomes of AI provider calls to train
 * the self-learning router. Hooks into AIProviderFactory to
 * collect real-time performance data.
 *
 * Captures:
 * - Task type, input length, success/failure
 * - Latency, quality score (if available)
 * - Tokens used, cost (if available)
 *
 * Feeds into: SelfLearningRouter.updateFromOutcome()
 */
@Service
public class AIProviderPerformanceTracker {
    public AIProviderPerformanceTracker(EnhancedSelfLearningRouter enhancedRouter) {
        this.enhancedRouter = enhancedRouter;
    }

    public AIProviderPerformanceTracker(SelfLearningRouter selfLearningRouter) {
        this.selfLearningRouter = selfLearningRouter;
    }


    private static final Logger logger = LoggerFactory.getLogger(AIProviderPerformanceTracker.class);

    // In-memory buffer before batch write
    private final Map<String, OutcomeBuffer> pendingOutcomes = new ConcurrentHashMap<>();



    private final AtomicLong totalTracked = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    // ──────────────────────────────────────────────────────────────────────
    // Recording API
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Record an AI provider call outcome.
     *
     * @param taskType Type of task performed
     * @param providerId Provider name (e.g., "gemini", "deepseek", "llama")
     * @param success Whether call succeeded
     * @param latencyMs Response time in milliseconds
     * @param inputLength Length of input text
     * @param requiredSkills List of skills required (optional)
     */
    public void recordOutcome(
            String taskType,
            String providerId,
            boolean success,
            long latencyMs,
            int inputLength,
            List<String> requiredSkills) {

        String taskSignature = taskType + "_" + inputLength + "_" + System.nanoTime();

        // Record to both routers if available
        if (selfLearningRouter != null) {
            selfLearningRouter.updateFromOutcome(taskType, taskSignature, providerId, success, latencyMs);
        }

        if (enhancedRouter != null) {
            enhancedRouter.recordOutcome(taskType, taskSignature, inputLength, requiredSkills, providerId, success, latencyMs, 0.0, 0);
        }

        totalTracked.incrementAndGet();
        if (!success) errorCount.incrementAndGet();

        logger.trace("[TRACKER] Recorded: {} {} success={} latency={}ms",
            taskType, providerId, success, latencyMs);
    }

    /**
     * Record with quality score.
     */
    public void recordOutcomeWithScore(
            String taskType,
            String providerId,
            boolean success,
            long latencyMs,
            int inputLength,
            List<String> requiredSkills,
            double qualityScore,
            int tokensUsed) {

        String taskSignature = taskType + "_" + inputLength + "_" + System.nanoTime();

        if (selfLearningRouter != null) {
            // Enhanced reward calculation includes quality and token efficiency
            double reward = computeEnhancedReward(success, latencyMs, qualityScore, tokensUsed);
            // For basic router, we can't directly set reward; it computes its own
            selfLearningRouter.updateFromOutcome(taskType, taskSignature, providerId, success, latencyMs);
        }

        if (enhancedRouter != null) {
            enhancedRouter.recordOutcome(taskType, taskSignature, inputLength, requiredSkills,
                providerId, success, latencyMs, qualityScore, tokensUsed);
        }

        totalTracked.incrementAndGet();
        if (!success) errorCount.incrementAndGet();
    }

    /**
     * Convenience method to be called from AIProviderFactory after each generation.
     */
    public void afterGeneration(String taskType, String providerId, boolean success,
                                 long latencyMs, String userInput, String aiResponse) {
        if (taskType == null) taskType = "general_chat";
        int inputLength = userInput != null ? userInput.length() : 0;
        int outputLength = aiResponse != null ? aiResponse.length() : 0;
        int tokensUsed = estimateTokens(inputLength, outputLength);

        // Simple quality heuristic: non-empty response of reasonable length
        double qualityScore = 0.0;
        if (success && aiResponse != null && aiResponse.length() > 10) {
            qualityScore = 0.7;
            if (aiResponse.length() > 50) qualityScore = 0.85;
            if (aiResponse.length() > 200) qualityScore = 0.95;
        }

        recordOutcomeWithScore(taskType, providerId, success, latencyMs, inputLength,
            List.of(), qualityScore, tokensUsed);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Utility
    // ──────────────────────────────────────────────────────────────────────

    private double computeEnhancedReward(boolean success, long latencyMs, double qualityScore, int tokensUsed) {
        double reward = success ? 1.0 : -1.0;

        // Latency bonus
        if (success && latencyMs > 0) {
            reward += Math.max(0.0, 1.0 - (latencyMs / 3000.0)) * 0.3;
        }

        // Quality bonus
        reward += qualityScore * 0.4;

        // Cost efficiency: reward lower token usage (proxy for cost)
        if (tokensUsed > 0 && tokensUsed < 10000) {
            reward += 0.1;
        }

        return reward;
    }

    private int estimateTokens(int inputLen, int outputLen) {
        // Rough approximation: ~4 chars per token
        return (inputLen + outputLen) / 4;
    }

    // ──────────────────────────────────────────────────────────────────────
    // Batch processing & checkpointing
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Flush any buffered outcomes and checkpoint router state.
     */
    public void checkpoint() {
        if (enhancedRouter != null) {
            enhancedRouter.checkpointLearning();
        }
        pendingOutcomes.clear();
        logger.info("[TRACKER] Checkpoint complete. Total tracked: {}", totalTracked.get());
    }

    /**
     * Get performance statistics.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTracked", totalTracked.get());
        stats.put("errorCount", errorCount.get());
        stats.put("successRate", totalTracked.get() > 0 ?
            (double) (totalTracked.get() - errorCount.get()) / totalTracked.get() : 0.0);

        if (selfLearningRouter != null) {
            stats.put("routerStats", selfLearningRouter.getLearningStats());
        }
        if (enhancedRouter != null) {
            stats.put("enhancedRouterStats", enhancedRouter.getRouterStats());
        }

        return stats;
    }

    /**
     * Reset tracking data.
     */
    public void reset() {
        if (selfLearningRouter != null) {
            selfLearningRouter.reset();
        }
        if (enhancedRouter != null) {
            enhancedRouter.reset();
        }
        totalTracked.set(0);
        errorCount.set(0);
        pendingOutcomes.clear();
        logger.info("[TRACKER] Full reset completed");
    }

    // ──────────────────────────────────────────────────────────────────────
    // Inner models
    // ──────────────────────────────────────────────────────────────────────

    private static class OutcomeBuffer {
        final String taskType;
        final String providerId;
        final boolean success;
        final long latencyMs;
        final int inputLength;
        final long timestamp;

        OutcomeBuffer(String taskType, String providerId, boolean success, long latencyMs, int inputLength) {
            this.taskType = taskType;
            this.providerId = providerId;
            this.success = success;
            this.latencyMs = latencyMs;
            this.inputLength = inputLength;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
