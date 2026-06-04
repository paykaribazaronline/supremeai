package com.supremeai.learning;

import com.supremeai.service.ProductionHealthMonitor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Enhanced Self-Learning Router with Advanced Q-Learning
 *
 * <p>Trains on real provider performance data to intelligently route requests to the best AI model
 * (Gemini, DeepSeek, Llama, etc.) based on: - Task type (code, analysis, creative, math, vision) -
 * Input complexity (short, medium, long) - Historical success rate per provider per task - Response
 * latency - Cost efficiency - Quality metrics (if feedback available)
 *
 * <p>Inspired by: SONA distributed router, contextual bandits
 */
@Service
public class EnhancedSelfLearningRouter {

  private static final Logger logger = LoggerFactory.getLogger(EnhancedSelfLearningRouter.class);

  // Advanced Q-learning parameters
  private static final double LEARNING_RATE = 0.15;
  private static final double DISCOUNT_FACTOR = 0.95;
  private static final double EXPLORATION_RATE_INITIAL = 0.20;
  private static final double EXPLORATION_DECAY = 0.9995;
  private static final double MIN_EXPLORATION_RATE = 0.05;

  // State encoding
  private static final int MAX_STATE_HISTORY = 5000;

  // Q-table: composite key (taskType + complexity + requiredSkill) → provider → Q-value
  private final Map<String, Map<String, Double>> qTable = new ConcurrentHashMap<>();

  // Provider feature vectors for similarity-based routing
  private final Map<String, ProviderFeatures> providerFeatures = new ConcurrentHashMap<>();

  // Task → Provider → (successCount, failureCount, totalLatency, lastUsed)
  private final Map<String, Map<String, ProviderStats>> providerStats = new ConcurrentHashMap<>();

  // Task embeddings for similarity-based routing (cosine similarity)
  private final Map<String, double[]> taskEmbeddings = new ConcurrentHashMap<>();

  // Exploration tracking
  private double currentExplorationRate = EXPLORATION_RATE_INITIAL;
  private final AtomicLong totalDecisions = new AtomicLong(0);
  private final AtomicLong successfulRoutes = new AtomicLong(0);

  @Autowired(required = false)
  private ProductionHealthMonitor healthMonitor;

  // ──────────────────────────────────────────────────────────────────────
  // Public API
  // ──────────────────────────────────────────────────────────────────────

  /**
   * Route a request to the best AI provider based on learned Q-values.
   *
   * @param taskType Type of task (e.g., "code_generation", "analysis", "creative_writing")
   * @param taskSignature Hash of request content for uniqueness
   * @param inputLength Length of input text (proxy for complexity)
   * @param requiredSkills Required capabilities (e.g., ["coding", "math", "vision"])
   * @param candidateProviders List of available provider names
   * @return RoutingDecision with selected provider and confidence
   */
  public RoutingDecision routeRequest(
      String taskType,
      String taskSignature,
      int inputLength,
      List<String> requiredSkills,
      List<String> candidateProviders) {

    String state = encodeRichState(taskType, inputLength, requiredSkills);
    totalDecisions.incrementAndGet();

    // Decay exploration rate
    decayExploration();

    // Epsilon-greedy with Boltzmann exploration for softmax
    if (Math.random() < currentExplorationRate && candidateProviders.size() > 1) {
      // Boltzmann exploration: sample based on Q-values (softmax)
      return boltzmannExplore(state, candidateProviders);
    }

    // Exploitation: select best provider
    return exploitBestProvider(state, candidateProviders);
  }

  /** Record outcome of a routing decision for training. */
  public void recordOutcome(
      String taskType,
      String taskSignature,
      int inputLength,
      List<String> requiredSkills,
      String providerId,
      boolean success,
      long latencyMs,
      double qualityScore,
      int tokensUsed) {

    String state = encodeRichState(taskType, inputLength, requiredSkills);
    double reward = computeReward(success, latencyMs, qualityScore, providerId, state);

    updateQValues(state, providerId, reward);
    updateProviderStats(taskType, providerId, success, latencyMs, qualityScore);
    updateProviderFeatures(providerId, success, latencyMs);

    if (success) successfulRoutes.incrementAndGet();

    logger.debug(
        "[ROUTER] Outcome: state={} provider={} success={} reward={:.3f}",
        state,
        providerId,
        success,
        reward);
  }

  /**
   * Get the best provider for a given task (simple version). This now expects a list of available
   * providers to remain dynamic.
   */
  public String getBestProviderForTask(String taskType, List<String> candidateProviders) {
    if (candidateProviders == null || candidateProviders.isEmpty()) {
      return "gemini"; // Default fallback
    }

    RoutingDecision decision =
        routeRequest(
            taskType,
            UUID.randomUUID().toString(),
            500, // medium complexity
            List.of(),
            candidateProviders);

    return decision.agentId;
  }

  // ──────────────────────────────────────────────────────────────────────
  // State Encoding
  // ──────────────────────────────────────────────────────────────────────

  private String encodeRichState(String taskType, int inputLength, List<String> requiredSkills) {
    // Complexity bucket: short (<500), medium (500-2000), long (2000-5000), very-long (>5000)
    String complexity;
    if (inputLength < 500) complexity = "SHORT";
    else if (inputLength < 2000) complexity = "MEDIUM";
    else if (inputLength < 5000) complexity = "LONG";
    else complexity = "VLONG";

    // Skill signature (sorted, comma-separated)
    String skillSig =
        requiredSkills == null || requiredSkills.isEmpty()
            ? "GENERAL"
            : String.join(",", requiredSkills.stream().sorted().toList());

    return String.format("%s|%s|%s", taskType.toUpperCase(), complexity, skillSig);
  }

  private double[] computeTaskEmbedding(String taskType, List<String> skills) {
    // Simplified embedding: one-hot encoding of task type + skills
    double[] embedding = new double[20]; // 20-dim vector
    int idx =
        Arrays.asList(
                "CODE_GENERATION",
                "CODE_ANALYSIS",
                "CODE_REVIEW",
                "CREATIVE_WRITING",
                "QUESTION_ANSWERING",
                "SUMMARIZATION",
                "TRANSLATION",
                "MATH_REASONING",
                "VISUAL_ANALYSIS",
                "DATA_ANALYSIS",
                "TECHNICAL_DOCS")
            .indexOf(taskType.toUpperCase());
    if (idx >= 0) embedding[idx] = 1.0;

    // Add skill dimensions (10-19)
    for (String skill : skills) {
      int skillIdx = skill.hashCode() % 10 + 10;
      embedding[skillIdx] += 0.5;
    }

    return embedding;
  }

  // ──────────────────────────────────────────────────────────────────────
  // Q-Learning Core
  // ──────────────────────────────────────────────────────────────────────

  private RoutingDecision exploitBestProvider(String state, List<String> candidates) {
    Map<String, Double> agentValues = qTable.computeIfAbsent(state, k -> new ConcurrentHashMap<>());
    String bestAgent = null;
    double bestValue = Double.NEGATIVE_INFINITY;

    for (String agent : candidates) {
      double qValue = agentValues.getOrDefault(agent, 0.0);
      if (qValue > bestValue) {
        bestValue = qValue;
        bestAgent = agent;
      }
    }

    // If no preference, use provider features for similarity-based selection
    if (bestAgent == null) {
      bestAgent = selectByFeatureSimilarity(state, candidates);
      bestValue = 0.0;
    }

    return new RoutingDecision(bestAgent, "exploitation", bestValue);
  }

  private RoutingDecision boltzmannExplore(String state, List<String> candidates) {
    Map<String, Double> agentValues = qTable.computeIfAbsent(state, k -> new ConcurrentHashMap<>());

    // Compute softmax probabilities
    double sumExp = 0.0;
    Map<String, Double> probs = new HashMap<>();
    for (String agent : candidates) {
      double q = agentValues.getOrDefault(agent, 0.0);
      double expVal = Math.exp(q / 0.5); // temperature = 0.5
      probs.put(agent, expVal);
      sumExp += expVal;
    }

    // Normalize
    for (Map.Entry<String, Double> e : probs.entrySet()) {
      probs.put(e.getKey(), e.getValue() / sumExp);
    }

    // Sample
    double r = Math.random();
    double cum = 0.0;
    for (Map.Entry<String, Double> e : probs.entrySet()) {
      cum += e.getValue();
      if (r <= cum) {
        return new RoutingDecision(e.getKey(), "boltzmann_explore", e.getValue());
      }
    }

    // Fallback
    return new RoutingDecision(candidates.get(0), "random", 0.0);
  }

  private void updateQValues(String state, String providerId, double reward) {
    Map<String, Double> agentValues = qTable.computeIfAbsent(state, k -> new ConcurrentHashMap<>());

    double oldQ = agentValues.getOrDefault(providerId, 0.0);

    // Max future Q-value
    double maxFutureQ =
        agentValues.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

    // Q-learning update: Q(s,a) = Q(s,a) + α [r + γ max_a' Q(s',a') - Q(s,a)]
    double newQ = oldQ + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxFutureQ - oldQ);
    agentValues.put(providerId, newQ);

    logger.trace("[ROUTER] Updated Q({}, {}): {:.4f} → {:.4f}", state, providerId, oldQ, newQ);
  }

  // ──────────────────────────────────────────────────────────────────────
  // Reward Calculation
  // ──────────────────────────────────────────────────────────────────────

  private double computeReward(
      boolean success, long latencyMs, double qualityScore, String providerId, String state) {

    double reward = 0.0;

    // Success reward/penalty
    reward += success ? 1.0 : -1.0;

    // Latency bonus (faster = better, up to 2s threshold)
    if (success && latencyMs > 0) {
      double latencyBonus = Math.max(0.0, 1.0 - (latencyMs / 3000.0));
      reward += latencyBonus * 0.3;
    }

    // Quality bonus (if available)
    if (qualityScore > 0) {
      reward += (qualityScore / 100.0) * 0.4;
    }

    // Cost efficiency (bonus for using free tier providers)
    if (isFreeTierProvider(providerId)) {
      reward += 0.1;
    }

    // MoE (Mixture of Experts) Bonus: highly prioritize MoE models for better reasoning &
    // efficiency
    if (isMoeModel(providerId)) {
      reward += 0.3;
    }

    return reward;
  }

  private boolean isMoeModel(String providerId) {
    String lower = providerId.toLowerCase();
    return lower.contains("mixtral")
        || lower.contains("deepseek")
        || lower.contains("grok")
        || lower.contains("dbrx")
        || lower.contains("moe");
  }

  private boolean isFreeTierProvider(String providerId) {
    return providerId.startsWith("hf_")
        || providerId.startsWith("gcp_")
        || providerId.startsWith("render_")
        || providerId.equals("ollama");
  }

  // ──────────────────────────────────────────────────────────────────────
  // Provider Profiling
  // ──────────────────────────────────────────────────────────────────────

  private void updateProviderStats(
      String taskType, String providerId, boolean success, long latencyMs, double quality) {
    Map<String, ProviderStats> statsMap =
        providerStats.computeIfAbsent(taskType, k -> new ConcurrentHashMap<>());
    ProviderStats stats = statsMap.computeIfAbsent(providerId, k -> new ProviderStats());

    stats.totalRequests.incrementAndGet();
    if (success) stats.successfulRequests.incrementAndGet();
    stats.totalLatency.addAndGet(latencyMs);
    stats.avgQualityScore = (stats.avgQualityScore * 0.8 + quality * 0.2); // EMA
    stats.lastUsed = System.currentTimeMillis();
  }

  private void updateProviderFeatures(String providerId, boolean success, long latencyMs) {
    ProviderFeatures features =
        providerFeatures.computeIfAbsent(providerId, k -> new ProviderFeatures());
    features.update(success, latencyMs);
  }

  private String selectByFeatureSimilarity(String state, List<String> candidates) {
    // Fallback: pick provider with best recent performance
    String best = candidates.get(0);
    double bestScore = -1.0;

    for (String provider : candidates) {
      ProviderFeatures f = providerFeatures.get(provider);
      if (f != null) {
        double score = f.getRecentSuccessRate();
        if (score > bestScore) {
          bestScore = score;
          best = provider;
        }
      }
    }
    return best;
  }

  // ──────────────────────────────────────────────────────────────────────
  // Exploration & Decay
  // ──────────────────────────────────────────────────────────────────────

  private void decayExploration() {
    if (currentExplorationRate > MIN_EXPLORATION_RATE) {
      currentExplorationRate *= EXPLORATION_DECAY;
    }
  }

  // ──────────────────────────────────────────────────────────────────────
  // Statistics & Introspection
  // ──────────────────────────────────────────────────────────────────────

  public Map<String, Object> getRouterStats() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("totalStates", qTable.size());
    stats.put("totalDecisions", totalDecisions.get());
    stats.put("successfulRoutes", successfulRoutes.get());
    stats.put(
        "overallSuccessRate",
        totalDecisions.get() > 0 ? (double) successfulRoutes.get() / totalDecisions.get() : 0.0);
    stats.put("currentExplorationRate", currentExplorationRate);
    stats.put("learningRate", LEARNING_RATE);
    stats.put("discountFactor", DISCOUNT_FACTOR);

    // Provider-specific stats
    Map<String, Object> providerStatsMap = new HashMap<>();
    for (Map.Entry<String, ProviderFeatures> e : providerFeatures.entrySet()) {
      Map<String, Object> pstats = new HashMap<>();
      pstats.put("recentSuccessRate", e.getValue().getRecentSuccessRate());
      pstats.put("avgLatencyMs", e.getValue().getAvgLatency());
      pstats.put("totalRoutes", e.getValue().totalRoutes.get());
      providerStatsMap.put(e.getKey(), pstats);
    }
    stats.put("providers", providerStatsMap);

    // Top state-action pairs
    List<Map<String, Object>> topPairs = getTopQValues(10);
    stats.put("topQValues", topPairs);

    return stats;
  }

  public Map<String, Object> getProviderPerformance(String providerId) {
    Map<String, Object> perf = new HashMap<>();
    ProviderFeatures f = providerFeatures.get(providerId);
    if (f != null) {
      perf.put("successRate", f.getRecentSuccessRate());
      perf.put("avgLatencyMs", f.getAvgLatency());
      perf.put("totalRoutes", f.totalRoutes.get());
    }
    return perf;
  }

  public Map<String, Object> getTaskPerformance(String taskType) {
    Map<String, Object> taskPerf = new HashMap<>();
    Map<String, ProviderStats> statsMap = providerStats.get(taskType);
    if (statsMap != null) {
      Map<String, Map<String, Object>> providerMetrics = new HashMap<>();
      for (Map.Entry<String, ProviderStats> e : statsMap.entrySet()) {
        Map<String, Object> metrics = new HashMap<>();
        ProviderStats s = e.getValue();
        metrics.put("totalRequests", s.totalRequests.get());
        metrics.put(
            "successRate",
            s.totalRequests.get() > 0
                ? (double) s.successfulRequests.get() / s.totalRequests.get()
                : 0.0);
        metrics.put(
            "avgLatencyMs",
            s.totalRequests.get() > 0
                ? (double) s.totalLatency.get() / s.totalRequests.get()
                : 0.0);
        metrics.put("qualityScore", s.avgQualityScore);
        providerMetrics.put(e.getKey(), metrics);
      }
      taskPerf.put("providers", providerMetrics);
    }
    return taskPerf;
  }

  private List<Map<String, Object>> getTopQValues(int limit) {
    List<Map.Entry<String, Double>> allEntries = new ArrayList<>();
    for (Map.Entry<String, Map<String, Double>> stateEntry : qTable.entrySet()) {
      for (Map.Entry<String, Double> actionEntry : stateEntry.getValue().entrySet()) {
        allEntries.add(
            new AbstractMap.SimpleImmutableEntry<>(
                stateEntry.getKey() + "→" + actionEntry.getKey(), actionEntry.getValue()));
      }
    }

    return allEntries.stream()
        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
        .limit(limit)
        .map(
            e -> {
              Map<String, Object> m = new HashMap<>();
              m.put("pair", e.getKey());
              m.put("qValue", e.getValue());
              return m;
            })
        .toList();
  }

  /** Reset learning (for re-training) */
  public void reset() {
    qTable.clear();
    providerStats.clear();
    taskEmbeddings.clear();
    providerFeatures.clear();
    currentExplorationRate = EXPLORATION_RATE_INITIAL;
    totalDecisions.set(0);
    successfulRoutes.set(0);
    logger.info("Enhanced self-learning router fully reset");
  }

  /** Flush intermediate state to persistent storage */
  public void checkpointLearning() {
    // In production, snapshot qTable and providerStats to Firestore
    logger.info(
        "[ROUTER] Checkpoint: states={}, explorationRate={:.3f}",
        qTable.size(),
        currentExplorationRate);
  }

  // ──────────────────────────────────────────────────────────────────────
  // Inner Models
  // ──────────────────────────────────────────────────────────────────────

  public static class RoutingDecision {
    public final String agentId;
    public final String decisionType;
    public final double confidence;

    public RoutingDecision(String agentId, String decisionType, double confidence) {
      this.agentId = agentId;
      this.decisionType = decisionType;
      this.confidence = confidence;
    }
  }

  private static class ProviderStats {
    final AtomicInteger totalRequests = new AtomicInteger(0);
    final AtomicInteger successfulRequests = new AtomicInteger(0);
    final AtomicLong totalLatency = new AtomicLong(0);
    volatile double avgQualityScore = 0.0;
    volatile long lastUsed;
  }

  private static class ProviderFeatures {
    private final Deque<Boolean> recentOutcomes = new ArrayDeque<>();
    private final Deque<Long> recentLatencies = new ArrayDeque<>();
    final AtomicInteger totalRoutes = new AtomicInteger(0);
    private static final int WINDOW_SIZE = 100;

    void update(boolean success, long latency) {
      totalRoutes.incrementAndGet();
      recentOutcomes.addLast(success);
      recentLatencies.addLast(latency);

      if (recentOutcomes.size() > WINDOW_SIZE) {
        recentOutcomes.removeFirst();
        recentLatencies.removeFirst();
      }
    }

    double getRecentSuccessRate() {
      if (recentOutcomes.isEmpty()) return 0.5;
      return recentOutcomes.stream().filter(b -> b).count() / (double) recentOutcomes.size();
    }

    double getAvgLatency() {
      if (recentLatencies.isEmpty()) return 0.0;
      return recentLatencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }
  }
}
