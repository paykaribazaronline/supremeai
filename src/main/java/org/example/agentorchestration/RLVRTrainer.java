package org.example.agentorchestration;

import org.example.service.SystemLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ADAPTIVE ORCHESTRATION TECHNIQUE 2: Reinforcement Learning with Verifiable Rewards (RLVR)
 *
 * How this orchestration layer learns:
 *   - For tasks with verifiable outcomes (code compiles ✅/❌, tests pass ✅/❌),
 *     the reward signal is EXACT — not from a human judge.
 *   - This is far more reliable than RLHF (human feedback) for agentic tasks.
 *   - RLVR drives the model to prefer agents/approaches that produce verifiable success.
 *
 * Translated to SupremeAI:
 *   - After each agent operation, we measure a VERIFIABLE outcome:
 *       • Build result → compiled or not
 *       • Test result  → passed or failed
 *       • Deployment   → healthy or not
 *       • Security scan → OWASP issues found or not
 *   - Reward = f(outcome) fed back to MoE Router to update agent weights
 *   - Confidence scores and patterns recorded in SystemLearning for long-term memory
 */
@Service
public class RLVRTrainer {

    private static final Logger logger = LoggerFactory.getLogger(RLVRTrainer.class);

    // Learning rate — conservative to avoid instability (like MuonClip)
    private static final double LEARNING_RATE = 0.01;

    // Reward values for verifiable outcomes
    private static final double REWARD_BUILD_PASS    = +1.0;
    private static final double REWARD_BUILD_FAIL    = -0.5;
    private static final double REWARD_TEST_PASS     = +0.8;
    private static final double REWARD_TEST_FAIL     = -0.4;
    private static final double REWARD_DEPLOY_OK     = +0.9;
    private static final double REWARD_DEPLOY_FAIL   = -0.6;
    private static final double REWARD_SECURITY_PASS = +0.7;
    private static final double REWARD_SECURITY_FAIL = -0.3;
    private static final double REWARD_SELF_HEAL_OK  = +0.6;
    private static final double REWARD_SELF_HEAL_FAIL = -0.2;

    @Autowired
    private ExpertAgentRouter moeRouter;

    @Autowired
    private SystemLearningService learningService;

    @Autowired
    private MuonClipOptimizer muonClip;

    // Rolling performance stats per agent
    private final Map<String, AgentPerformance> performanceMap = new ConcurrentHashMap<>();

    public enum VerifiableOutcome {
        BUILD_PASS,  BUILD_FAIL,
        TEST_PASS,   TEST_FAIL,
        DEPLOY_OK,   DEPLOY_FAIL,
        SECURITY_OK, SECURITY_FAIL,
        SELF_HEAL_OK, SELF_HEAL_FAIL
    }

    /**
     * Record a verifiable outcome and update agent weights (RLVR training step).
     *
     * Flow:
     *   1. Map outcome → reward signal
     *   2. Apply MuonClip gradient clipping
     *   3. Update MoE routing weights for participating agents
     *   4. Record to AgentDecisionLogger (audit trail)
     *   5. Record pattern to SystemLearning (long-term memory)
     *
     * @param agentNames  agents that participated in the task
     * @param taskType    what type of task was performed
     * @param outcome     the verifiable result
     * @param details     extra context (error message, test output, etc.)
     */
    public TrainingResult recordOutcome(List<String> agentNames,
                                        ExpertAgentRouter.TaskType taskType,
                                        VerifiableOutcome outcome,
                                        String details) {
        double reward = mapReward(outcome);
        boolean positive = reward > 0;

        logger.info("🎯 RLVR training step: agents={} task={} outcome={} reward={:.2f}",
            agentNames, taskType, outcome, reward);

        // Apply MuonClip and update MoE weights for each participating agent
        List<String> updatedAgents = new ArrayList<>();
        for (String agent : agentNames) {
            double clippedLR = muonClip.clip(reward, LEARNING_RATE);
            moeRouter.updateWeight(agent, taskType, reward, clippedLR);
            updatePerformanceStats(agent, taskType, outcome, reward);
            updatedAgents.add(agent);
        }

        // Record to SystemLearning for long-term pattern memory
        String category = taskType.name().toLowerCase();
        String pattern = agentNames.toString() + " → " + outcome.name();
        if (positive) {
            learningService.recordPattern(category, pattern, details);
        } else {
            learningService.recordError(category, pattern + ": " + details, null,
                "Outcome: " + outcome.name() + " | Agents: " + agentNames);
        }

        TrainingResult result = new TrainingResult(
            agentNames, taskType, outcome, reward, updatedAgents);

        logger.info("✅ RLVR step complete: {} agents updated, reward={:.2f}, positive={}",
            updatedAgents.size(), reward, positive);
        return result;
    }

    /**
     * Compute expected reward for a routing decision BEFORE execution
     * (used by orchestrator to pick best route).
     *
     * Uses rolling average of past rewards for each agent×taskType pair.
     */
    public double estimateReward(String agentName, ExpertAgentRouter.TaskType taskType) {
        AgentPerformance perf = performanceMap.get(agentName);
        if (perf == null) return 0.5; // optimistic prior for unseen agents
        return perf.getAverageReward(taskType);
    }

    /**
     * Return full performance leaderboard (for monitoring dashboard).
     */
    public List<Map<String, Object>> getLeaderboard() {
        return ExpertAgentRouter.ALL_AGENTS.stream().map(agent -> {
            AgentPerformance perf = performanceMap.getOrDefault(
                agent, new AgentPerformance(agent));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("agent", agent);
            row.put("total_tasks", perf.totalTasks);
            row.put("success_rate_pct", Math.round(perf.successRate() * 100));
            row.put("avg_reward", Math.round(perf.overallAvgReward() * 100.0) / 100.0);
            row.put("best_task_type", perf.bestTaskType());
            return row;
        }).collect(Collectors.toList());
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private double mapReward(VerifiableOutcome outcome) {
        switch (outcome) {
            case BUILD_PASS:    return REWARD_BUILD_PASS;
            case BUILD_FAIL:    return REWARD_BUILD_FAIL;
            case TEST_PASS:     return REWARD_TEST_PASS;
            case TEST_FAIL:     return REWARD_TEST_FAIL;
            case DEPLOY_OK:     return REWARD_DEPLOY_OK;
            case DEPLOY_FAIL:   return REWARD_DEPLOY_FAIL;
            case SECURITY_OK:   return REWARD_SECURITY_PASS;
            case SECURITY_FAIL: return REWARD_SECURITY_FAIL;
            case SELF_HEAL_OK:  return REWARD_SELF_HEAL_OK;
            case SELF_HEAL_FAIL:return REWARD_SELF_HEAL_FAIL;
            default:            return 0.0;
        }
    }

    private void updatePerformanceStats(String agent, ExpertAgentRouter.TaskType taskType,
                                        VerifiableOutcome outcome, double reward) {
        performanceMap.computeIfAbsent(agent, AgentPerformance::new)
            .record(taskType, reward, reward > 0);
    }

    // ── Value objects ─────────────────────────────────────────────────────────

    public static class TrainingResult {
        public final List<String> agents;
        public final ExpertAgentRouter.TaskType taskType;
        public final VerifiableOutcome outcome;
        public final double reward;
        public final List<String> updatedAgents;
        public final long timestamp = System.currentTimeMillis();

        public TrainingResult(List<String> agents, ExpertAgentRouter.TaskType taskType,
                              VerifiableOutcome outcome, double reward, List<String> updated) {
            this.agents = agents;
            this.taskType = taskType;
            this.outcome = outcome;
            this.reward = reward;
            this.updatedAgents = updated;
        }
    }

    /** Rolling performance stats for one agent. */
    static class AgentPerformance {
        final String name;
        int totalTasks = 0;
        int successCount = 0;
        double rewardSum = 0;
        final Map<ExpertAgentRouter.TaskType, double[]> taskRewards = new EnumMap<>(ExpertAgentRouter.TaskType.class);

        AgentPerformance(String name) { this.name = name; }

        void record(ExpertAgentRouter.TaskType task, double reward, boolean success) {
            totalTasks++;
            rewardSum += reward;
            if (success) successCount++;
            taskRewards.computeIfAbsent(task, k -> new double[]{0, 0});
            double[] pair = taskRewards.get(task);
            pair[0] += reward; // sum
            pair[1]++;         // count
        }

        double successRate() {
            return totalTasks == 0 ? 0 : (double) successCount / totalTasks;
        }

        double overallAvgReward() {
            return totalTasks == 0 ? 0 : rewardSum / totalTasks;
        }

        double getAverageReward(ExpertAgentRouter.TaskType task) {
            double[] pair = taskRewards.get(task);
            if (pair == null || pair[1] == 0) return 0.5;
            return pair[0] / pair[1];
        }

        String bestTaskType() {
            return taskRewards.entrySet().stream()
                .filter(e -> e.getValue()[1] > 0)
                .max(Comparator.comparingDouble(e -> e.getValue()[0] / e.getValue()[1]))
                .map(e -> e.getKey().name())
                .orElse("N/A");
        }
    }
}
