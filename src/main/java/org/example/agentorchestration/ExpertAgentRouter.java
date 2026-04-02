package org.example.agentorchestration;

import org.example.agentorchestration.learning.LearningFirebaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ADAPTIVE ORCHESTRATION TECHNIQUE 1: Mixture-of-Experts (MoE) Router
 *
 * Inspired by sparse expert-routing architectures:
 *   - 1T total params, 32B activated per token
 *   - 384 experts, 8 selected per token
 *
 * Translated to SupremeAI:
 *   - 20 total agents (all phases)
 *   - Top-K agents selected per task (default K=3)
 *   - Each agent has a routing weight per task type
 *   - Weights updated by RLVR (see RLVRTrainer)
 *
 * Key property: SPARSE activation — only the most relevant agents
 * handle each task, reducing cost and improving quality.
 */
@Service
public class ExpertAgentRouter {

    private static final Logger logger = LoggerFactory.getLogger(ExpertAgentRouter.class);

    // Default top-K agents to activate per task.
    private static final int DEFAULT_TOP_K = 3;

    // All 20 SupremeAI agents (phases 1-10)
    public static final List<String> ALL_AGENTS = Arrays.asList(
        // Phase 1-3 (Core)
        "Architect", "Builder", "Reviewer",
        // Phase 6 (Visualization & Auto-Fix)
        "A-Visual", "B-Fixer", "C-Tester",
        // Phase 7 (Multi-Platform)
        "D-iOS", "E-Web", "F-Desktop", "G-Publish",
        // Phase 8 (Security & Compliance)
        "Alpha-Security", "Beta-Compliance", "Gamma-Privacy",
        // Phase 9 (Cost Intelligence)
        "Delta-Cost", "Epsilon-Optimizer", "Zeta-Finance",
        // Phase 10 (Self-Improvement)
        "Eta-Meta", "Theta-Learning", "Iota-Knowledge", "Kappa-Evolution"
    );

    // Task types SupremeAI handles
    public enum TaskType {
        CODE_GENERATION, ARCHITECTURE_DESIGN, CODE_REVIEW,
        BUG_FIX, SECURITY_AUDIT, COST_OPTIMIZATION,
        DEPLOYMENT, TESTING, LEARNING, META_IMPROVEMENT
    }

    // routing_weights[agent][task_type] — learned through RLVR
    // Initial weights encode domain expertise (priors)
    private final Map<String, Map<TaskType, Double>> routingWeights = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private LearningFirebaseRepository firebaseRepo;

    // Shared expert: always included regardless of task.
    private static final String SHARED_EXPERT = "Architect";

    public ExpertAgentRouter() {
        initializeWeights();
    }

    /**
     * Route a task to the top-K most relevant agents.
     *
     * Algorithm:
     *   1. Compute routing score for each agent: score = weight[agent][taskType]
     *   2. Apply softmax normalization
     *   3. Select top-K agents by score
     *   4. Always include SHARED_EXPERT (like K2's 1 shared expert)
     *
     * @param taskType   the type of task
     * @param topK       how many agents to activate
     * @return           ordered list of selected agent names (highest score first)
     */
    public List<RoutingDecision> route(TaskType taskType, int topK) {
        logger.info("🔀 MoE routing task={} selecting top-{} of {} agents",
                taskType, topK, ALL_AGENTS.size());

        // Score all agents for this task type
        List<RoutingDecision> scored = ALL_AGENTS.stream()
            .map(agent -> {
                double weight = routingWeights
                    .getOrDefault(agent, Collections.emptyMap())
                    .getOrDefault(taskType, 0.1);
                return new RoutingDecision(agent, weight, taskType);
            })
            .sorted(Comparator.comparingDouble(RoutingDecision::getScore).reversed())
            .collect(Collectors.toList());

        // Apply softmax to get normalized probabilities
        applySoftmax(scored);

        // Select top-K, always include shared expert
        List<RoutingDecision> selected = scored.stream()
            .limit(topK)
            .collect(Collectors.toList());

        // Add shared expert if not already selected
        boolean hasShared = selected.stream()
            .anyMatch(d -> d.getAgentName().equals(SHARED_EXPERT));
        if (!hasShared) {
            scored.stream()
                .filter(d -> d.getAgentName().equals(SHARED_EXPERT))
                .findFirst()
                .ifPresent(shared -> {
                    selected.add(shared);
                    logger.debug("➕ Added shared expert '{}' to routing", SHARED_EXPERT);
                });
        }

        logger.info("✅ MoE selected agents: {}",
            selected.stream().map(RoutingDecision::getAgentName).collect(Collectors.joining(", ")));

        return selected;
    }

    public List<RoutingDecision> route(TaskType taskType) {
        return route(taskType, DEFAULT_TOP_K);
    }

    /**
     * Update routing weight for an agent based on reward signal from RLVR.
     * Called by RLVRTrainer after verifiable outcome is known.
     *
     * @param agentName  which agent to update
     * @param taskType   which task type was performed
     * @param reward     value in [-1.0, +1.0]; positive = good, negative = bad
     * @param learningRate step size (default 0.01 from MuonClip)
     */
    public void updateWeight(String agentName, TaskType taskType, double reward, double learningRate) {
        routingWeights.computeIfAbsent(agentName, k -> new ConcurrentHashMap<>());
        Map<TaskType, Double> agentWeights = routingWeights.get(agentName);

        double currentWeight = agentWeights.getOrDefault(taskType, 0.5);
        double newWeight = currentWeight + learningRate * reward;

        // Clip to [0.01, 2.0] — MuonClip ensures no weight collapses to 0 or exploits
        newWeight = Math.max(0.01, Math.min(2.0, newWeight));

        agentWeights.put(taskType, newWeight);
        logger.debug("📈 Weight updated: agent={} task={} {:.4f} → {:.4f} (reward={:.2f})",
            agentName, taskType, currentWeight, newWeight, reward);

        // Persist to Firebase so weights survive restart
        if (firebaseRepo != null) {
            firebaseRepo.saveRoutingWeight(agentName, taskType.name(), newWeight);
        }
    }

    /**
     * Get current routing weights snapshot (for monitoring dashboard).
     */
    public Map<String, Map<TaskType, Double>> getWeightSnapshot() {
        Map<String, Map<TaskType, Double>> snapshot = new LinkedHashMap<>();
        for (String agent : ALL_AGENTS) {
            snapshot.put(agent, new LinkedHashMap<>(
                routingWeights.getOrDefault(agent, Collections.emptyMap())));
        }
        return snapshot;
    }

    /**
     * Get top-3 agents for each task type (routing summary).
     */
    public Map<TaskType, List<String>> getRoutingSummary() {
        Map<TaskType, List<String>> summary = new LinkedHashMap<>();
        for (TaskType tt : TaskType.values()) {
            List<String> top3 = route(tt, 3).stream()
                .map(RoutingDecision::getAgentName)
                .collect(Collectors.toList());
            summary.put(tt, top3);
        }
        return summary;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /** Softmax normalization over routing scores (in-place). */
    private void applySoftmax(List<RoutingDecision> decisions) {
        double max = decisions.stream()
            .mapToDouble(RoutingDecision::getScore).max().orElse(1.0);
        double sum = decisions.stream()
            .mapToDouble(d -> Math.exp(d.getScore() - max)).sum();
        decisions.forEach(d -> d.setProbability(Math.exp(d.getScore() - max) / sum));
    }

    /** Initialize routing weights with domain-expertise priors. */
    private void initializeWeights() {
        // Prior knowledge: each agent has natural affinity for certain tasks
        Map<String, Map<TaskType, Double>> priors = new LinkedHashMap<>();

        // Core agents
        priors.put("Architect",       taskMap(1.8, 0.4, 0.6, 0.4, 0.5, 0.3, 0.5, 0.4, 0.4, 0.9));
        priors.put("Builder",         taskMap(1.5, 0.3, 0.4, 1.2, 0.4, 0.3, 0.8, 0.7, 0.3, 0.4));
        priors.put("Reviewer",        taskMap(0.5, 0.6, 1.8, 0.9, 0.8, 0.4, 0.4, 0.9, 0.4, 0.5));
        // Phase 6
        priors.put("A-Visual",        taskMap(0.3, 0.5, 0.4, 0.4, 0.3, 0.2, 0.3, 0.4, 0.5, 0.3));
        priors.put("B-Fixer",         taskMap(0.6, 0.3, 0.7, 1.7, 0.6, 0.3, 0.4, 0.8, 0.5, 0.4));
        priors.put("C-Tester",        taskMap(0.4, 0.3, 0.9, 0.8, 0.5, 0.3, 0.4, 1.8, 0.3, 0.4));
        // Phase 7
        priors.put("D-iOS",           taskMap(0.8, 0.5, 0.5, 0.7, 0.4, 0.3, 0.8, 0.6, 0.3, 0.3));
        priors.put("E-Web",           taskMap(0.9, 0.5, 0.5, 0.7, 0.4, 0.3, 0.9, 0.6, 0.3, 0.3));
        priors.put("F-Desktop",       taskMap(0.8, 0.5, 0.5, 0.7, 0.4, 0.3, 0.8, 0.6, 0.3, 0.3));
        priors.put("G-Publish",       taskMap(0.4, 0.3, 0.3, 0.4, 0.3, 0.3, 1.8, 0.4, 0.3, 0.3));
        // Phase 8
        priors.put("Alpha-Security",  taskMap(0.5, 0.6, 0.8, 0.7, 1.9, 0.4, 0.5, 0.7, 0.4, 0.5));
        priors.put("Beta-Compliance", taskMap(0.4, 0.5, 0.7, 0.5, 1.5, 0.4, 0.4, 0.5, 0.4, 0.4));
        priors.put("Gamma-Privacy",   taskMap(0.4, 0.5, 0.7, 0.5, 1.6, 0.4, 0.4, 0.5, 0.4, 0.4));
        // Phase 9
        priors.put("Delta-Cost",      taskMap(0.3, 0.4, 0.4, 0.4, 0.3, 1.8, 0.6, 0.4, 0.4, 0.5));
        priors.put("Epsilon-Optimizer",taskMap(0.4, 0.5, 0.5, 0.6, 0.4, 1.7, 0.7, 0.5, 0.5, 0.6));
        priors.put("Zeta-Finance",    taskMap(0.3, 0.4, 0.4, 0.3, 0.3, 1.6, 0.4, 0.3, 0.4, 0.4));
        // Phase 10
        priors.put("Eta-Meta",        taskMap(0.6, 0.8, 0.6, 0.5, 0.5, 0.5, 0.4, 0.5, 0.9, 1.8));
        priors.put("Theta-Learning",  taskMap(0.5, 0.5, 0.6, 0.6, 0.5, 0.4, 0.4, 0.5, 1.8, 0.8));
        priors.put("Iota-Knowledge",  taskMap(0.5, 0.6, 0.5, 0.5, 0.5, 0.4, 0.4, 0.5, 1.7, 0.7));
        priors.put("Kappa-Evolution", taskMap(0.5, 0.7, 0.6, 0.5, 0.5, 0.5, 0.4, 0.5, 0.9, 1.9));

        routingWeights.putAll(priors);
        logger.info("🧠 MoE Router initialized: {} agents, {} task types",
            ALL_AGENTS.size(), TaskType.values().length);
    }

    /** Build a task-type weight map from ordered doubles matching TaskType enum order. */
    private Map<TaskType, Double> taskMap(double... values) {
        Map<TaskType, Double> map = new EnumMap<>(TaskType.class);
        TaskType[] types = TaskType.values();
        for (int i = 0; i < Math.min(types.length, values.length); i++) {
            map.put(types[i], values[i]);
        }
        return map;
    }

    // ── RoutingDecision (value object) ────────────────────────────────────────

    public static class RoutingDecision {
        private final String agentName;
        private double score;
        private double probability;
        private final TaskType taskType;
        private final long timestamp = System.currentTimeMillis();

        public RoutingDecision(String agentName, double score, TaskType taskType) {
            this.agentName = agentName;
            this.score = score;
            this.taskType = taskType;
        }

        public String getAgentName()          { return agentName; }
        public double getScore()               { return score; }
        public double getProbability()         { return probability; }
        public TaskType getTaskType()          { return taskType; }
        public long getTimestamp()             { return timestamp; }
        public void setProbability(double p)   { this.probability = p; }
        public void setScore(double s)         { this.score = s; }

        @Override
        public String toString() {
            return String.format("%s(%.3f)", agentName, probability);
        }
    }
}
