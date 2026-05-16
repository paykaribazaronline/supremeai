package com.supremeai.learning;

import com.supremeai.service.ProductionHealthMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Self-Learning Router - Plan 24 Phase 4
 * Implements SONA-style Q-learning for adaptive agent routing.
 * Routes tasks to the optimal agent/provider based on learned performance patterns.
 *
 * Inspired by Ruflo's self-learning router (https://github.com/ruvnet/ruflo)
 */
@Service
public class SelfLearningRouter {

    private static final Logger logger = LoggerFactory.getLogger(SelfLearningRouter.class);

    // Q-learning parameters
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.9;
    private static final double EXPLORATION_RATE = 0.15;

    // State-action value table: state -> agent -> Q-value
    private final Map<String, Map<String, Double>> qTable = new ConcurrentHashMap<>();

    // Reward history for learning
    private final Map<String, List<TaskOutcome>> rewardHistory = new ConcurrentHashMap<>();

    // Task routing statistics
    private final Map<String, AtomicInteger> routeCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> successCounts = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private ProductionHealthMonitor healthMonitor;

    /**
     * Route a task to the best agent based on learned Q-values
     */
    public RoutingDecision routeTask(String taskCategory, String taskSignature, List<String> candidateAgents) {
        String state = encodeState(taskCategory, taskSignature);

        // Epsilon-greedy: occasionally explore
        if (Math.random() < EXPLORATION_RATE && candidateAgents.size() > 1) {
            String randomAgent = candidateAgents.get(new Random().nextInt(candidateAgents.size()));
            return new RoutingDecision(randomAgent, "exploration", 0.0);
        }

        // Select agent with highest Q-value
        String bestAgent = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        Map<String, Double> agentValues = qTable.computeIfAbsent(state, k -> new ConcurrentHashMap<>());

        for (String agent : candidateAgents) {
            double qValue = agentValues.getOrDefault(agent, 0.0);
            if (qValue > bestValue) {
                bestValue = qValue;
                bestAgent = agent;
            }
        }

        // If no preference, pick first
        if (bestAgent == null) {
            bestAgent = candidateAgents.get(0);
        }

        return new RoutingDecision(bestAgent, "exploitation", bestValue);
    }

    /**
     * Update Q-values based on task outcome
     */
    public void updateFromOutcome(String taskCategory, String taskSignature,
                                   String agentId, boolean success, long durationMs) {
        String state = encodeState(taskCategory, taskSignature);
        double reward = computeReward(success, durationMs);

        Map<String, Double> agentValues = qTable.computeIfAbsent(state, k -> new ConcurrentHashMap<>());

        // Get current Q-value
        double oldQ = agentValues.getOrDefault(agentId, 0.0);

        // Estimate optimal future Q-value
        double maxFutureQ = agentValues.values().stream()
                .mapToDouble(Double::doubleValue)
                .max().orElse(0.0);

        // Q-learning update rule
        double newQ = oldQ + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxFutureQ - oldQ);
        agentValues.put(agentId, newQ);

        // Record outcome
        TaskOutcome outcome = new TaskOutcome(agentId, success, reward, durationMs, System.currentTimeMillis());
        rewardHistory.computeIfAbsent(state, k -> new ArrayList<>()).add(outcome);

        // Track statistics
        routeCounts.computeIfAbsent(agentId, k -> new AtomicInteger(0)).incrementAndGet();
        if (success) {
            successCounts.computeIfAbsent(agentId, k -> new AtomicInteger(0)).incrementAndGet();
        }

        // Trim history to prevent memory growth
        trimHistory(state, 1000);

        logger.debug("Updated Q-value for {} in state={}: {} -> {}", agentId, state, oldQ, newQ);
    }

    /**
     * Compute reward signal from task outcome
     */
    private double computeReward(boolean success, long durationMs) {
        double reward = 0.0;

        // Success bonus
        if (success) {
            reward += 1.0;
        } else {
            reward -= 1.0;
        }

        // Speed bonus (faster = better, normalized to 0-1 second range)
        double speedBonus = Math.max(0, 1.0 - (durationMs / 3000.0));
        reward += speedBonus * 0.3;

        return reward;
    }

    /**
     * Encode task features into a state key
     */
    private String encodeState(String category, String signature) {
        // Simplified state encoding
        return category + ":" + (signature != null ? signature.hashCode() : 0);
    }

    /**
     * Trim reward history to max entries
     */
    private void trimHistory(String state, int maxEntries) {
        List<TaskOutcome> history = rewardHistory.get(state);
        if (history != null && history.size() > maxEntries) {
            // Keep most recent entries
            rewardHistory.put(state, new ArrayList<>(history.subList(history.size() - maxEntries, history.size())));
        }
    }

    /**
     * Get agent performance statistics
     */
    public Map<String, Object> getAgentStats(String agentId) {
        Map<String, Object> stats = new HashMap<>();

        AtomicInteger routes = routeCounts.getOrDefault(agentId, new AtomicInteger(0));
        AtomicInteger successes = successCounts.getOrDefault(agentId, new AtomicInteger(0));

        stats.put("totalRoutes", routes.get());
        stats.put("successfulRoutes", successes.get());
        stats.put("successRate", routes.get() > 0 ? (double) successes.get() / routes.get() : 0.0);

        // Get average Q-value across all states
        double avgQ = qTable.values().stream()
                .mapToDouble(m -> m.getOrDefault(agentId, 0.0))
                .average().orElse(0.0);
        stats.put("avgQValue", avgQ);

        return stats;
    }

    /**
     * Get system-wide learning statistics
     */
    public Map<String, Object> getLearningStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalStates", qTable.size());
        stats.put("totalRoutes", routeCounts.values().stream().mapToInt(AtomicInteger::get).sum());
        stats.put("totalSuccesses", successCounts.values().stream().mapToInt(AtomicInteger::get).sum());

        int totalRoutes = 0, totalSuccesses = 0;
        for (AtomicInteger r : routeCounts.values()) totalRoutes += r.get();
        for (AtomicInteger s : successCounts.values()) totalSuccesses += s.get();

        stats.put("overallSuccessRate", totalRoutes > 0 ? (double) totalSuccesses / totalRoutes : 0.0);
        stats.put("learningRate", LEARNING_RATE);
        stats.put("explorationRate", EXPLORATION_RATE);
        stats.put("discountFactor", DISCOUNT_FACTOR);

        return stats;
    }

    /**
     * Reset learning data (for testing/re-training)
     */
    public void reset() {
        qTable.clear();
        rewardHistory.clear();
        routeCounts.clear();
        successCounts.clear();
        logger.info("Self-learning router reset");
    }

    // ──────────────────────────────────────────────────────────────────────
    // Inner classes
    // ──────────────────────────────────────────────────────────────────────

    public static class RoutingDecision {
        public final String agentId;
        public final String decisionType; // "exploration" or "exploitation"
        public final double confidence;

        public RoutingDecision(String agentId, String decisionType, double confidence) {
            this.agentId = agentId;
            this.decisionType = decisionType;
            this.confidence = confidence;
        }
    }

    public static class TaskOutcome {
        public final String agentId;
        public final boolean success;
        public final double reward;
        public final long durationMs;
        public final long timestamp;

        public TaskOutcome(String agentId, boolean success, double reward, long durationMs, long timestamp) {
            this.agentId = agentId;
            this.success = success;
            this.reward = reward;
            this.durationMs = durationMs;
            this.timestamp = timestamp;
        }
    }
}