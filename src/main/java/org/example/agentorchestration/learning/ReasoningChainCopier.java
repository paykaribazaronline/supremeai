package org.example.agentorchestration.learning;
import org.example.service.AgentDecisionLogger;
import org.example.service.AgentDecisionLogger.AgentDecision;
import org.example.service.SystemLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * LEVEL 3: Reasoning Chain Copier
 *
 * "Best agent-এর reasoning chain copy করা" — এটা শেখে।
 *
 * কীভাবে কাজ করে:
 *   - সফল AgentDecision-গুলো থেকে complete reasoning chain extract করে
 *   - প্রতিটা chain: [context → reasoning step 1 → step 2 → decision → outcome]
 *   - নতুন similar task এলে → সবচেয়ে similar সফল chain টা retrieve করে
 *   - সেই chain টা "template" হিসেবে নতুন task-এ apply করে
 *
 * Retrieval analogy:
 *   Attention-like pattern reuse is implemented explicitly via retrieval.
 *   Result is the same: proven reasoning replayed on new similar problems.
 *
 * Example:
 *   Past chain: "AuthFilter error → check JWT → found null token → add null check → fixed"
 *   New task: "SecurityFilter error" → retrieve above chain → replay steps
 */
@Service
public class ReasoningChainCopier {

    private static final Logger logger = LoggerFactory.getLogger(ReasoningChainCopier.class);

    // How many chains to store per agent per task type
    private static final int MAX_CHAINS_PER_SLOT = 20;

    // Similarity threshold to consider a chain a "match"
    private static final double SIMILARITY_THRESHOLD = 0.4;

    @Autowired
    private AgentDecisionLogger decisionLogger;

    @Autowired
    private AgentPatternProfiler profiler;

    @Autowired
    private SystemLearningService learningService;

    @Autowired
    private LearningFirebaseRepository firebaseRepo;

    // Store: agentName → taskType → list of successful chains
    private final Map<String, Map<String, List<ReasoningChain>>> chainStore
        = new ConcurrentHashMap<>();

    /**
     * Extract and store all successful reasoning chains from decision history.
     * Called after each successful RLVR outcome.
     */
    public int indexAllChains() {
        logger.info("🔗 Level 3: Indexing reasoning chains from decision history...");

        List<AgentDecision> allDecisions = decisionLogger.getAllDecisions();
        if (allDecisions == null || allDecisions.isEmpty()) {
            logger.info("   No decisions to index yet.");
            return 0;
        }

        // Only index successful, high-confidence decisions
        List<AgentDecision> good = allDecisions.stream()
            .filter(d -> "SUCCESS".equals(d.result) && d.confidence >= 0.7f)
            .collect(Collectors.toList());

        int indexed = 0;
        for (AgentDecision d : good) {
            if (d.agent == null || d.taskType == null || d.reasoning == null) continue;

            ReasoningChain chain = buildChain(d);
            storeChain(d.agent, d.taskType, chain);
            indexed++;
        }

        logger.info("✅ Level 3: Indexed {} reasoning chains across {} agents.",
            indexed, chainStore.size());
        return indexed;
    }

    /**
     * Store one chain (called after each successful decision in real-time).
     */
    public void recordChain(AgentDecision decision) {
        if (decision == null || !"SUCCESS".equals(decision.result)) return;
        if (decision.agent == null || decision.taskType == null) return;

        ReasoningChain chain = buildChain(decision);
        storeChain(decision.agent, decision.taskType, chain);
        logger.debug("🔗 Chain recorded: agent={} task={} confidence={:.2f}",
            decision.agent, decision.taskType, (double) decision.confidence);
    }

    /**
     * Retrieve the best matching reasoning chain for a new task.
     *
     * Algorithm:
     *   1. Filter chains by agent + taskType
     *   2. Score each chain's similarity to the new context
     *   3. Return the highest-scoring chain above threshold
     *   4. If no match → return null (Level 4 will generate one instead)
     *
     * @param agentName  which agent is handling the task
     * @param taskType   type of task
     * @param context    description of the new task/problem
     * @return           best matching past chain, or null if no good match
     */
    public ReasoningChain retrieveBestChain(String agentName, String taskType, String context) {
        Map<String, List<ReasoningChain>> agentStore = chainStore.get(agentName);
        if (agentStore == null || agentStore.isEmpty()) return null;

        List<ReasoningChain> candidates = agentStore.getOrDefault(taskType, Collections.emptyList());
        if (candidates.isEmpty()) return null;

        // Score each chain's similarity to the current context
        ReasoningChain best = null;
        double bestScore = -1;

        for (ReasoningChain chain : candidates) {
            double score = computeSimilarity(context, chain);
            if (score > bestScore) {
                bestScore = score;
                best = chain;
            }
        }

        if (bestScore >= SIMILARITY_THRESHOLD && best != null) {
            logger.info("🔗 Level 3: Chain match found for agent={} task={} similarity={:.2f}",
                agentName, taskType, bestScore);
            best.setRetrievalScore(bestScore);
            return best;
        }

        logger.info("🔗 Level 3: No good chain match (best={:.2f} < {:.2f}) — Level 4 needed.",
            bestScore, SIMILARITY_THRESHOLD);
        return null;
    }

    /**
     * Get all chains for a given agent (for monitoring).
     */
    public List<ReasoningChain> getChainsForAgent(String agentName) {
        Map<String, List<ReasoningChain>> agentStore = chainStore.get(agentName);
        if (agentStore == null) return Collections.emptyList();
        return agentStore.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    public int getChainCountForAgent(String agentName) {
        return getChainsForAgent(agentName).size();
    }

    /**
     * Statistics summary for the REST API.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        int totalChains = chainStore.values().stream()
            .mapToInt(m -> m.values().stream().mapToInt(List::size).sum())
            .sum();
        stats.put("total_chains_indexed", totalChains);
        stats.put("agents_with_chains", chainStore.size());
        Map<String, Integer> perAgent = new LinkedHashMap<>();
        chainStore.forEach((agent, taskMap) ->
            perAgent.put(agent, taskMap.values().stream().mapToInt(List::size).sum()));
        stats.put("chains_per_agent", perAgent);
        return stats;
    }

    public int ensureBootstrapCoverage(Map<String, List<String>> tasksByAgent) {
        if (tasksByAgent == null || tasksByAgent.isEmpty()) {
            return 0;
        }

        int seeded = 0;
        for (Map.Entry<String, List<String>> entry : tasksByAgent.entrySet()) {
            String agentName = entry.getKey();
            List<String> taskTypes = entry.getValue();
            if (agentName == null || taskTypes == null) {
                continue;
            }

            for (String taskType : taskTypes) {
                if (taskType == null || taskType.isBlank()) {
                    continue;
                }

                Map<String, List<ReasoningChain>> byTask = chainStore.computeIfAbsent(
                    agentName, ignored -> new ConcurrentHashMap<>());
                List<ReasoningChain> existing = byTask.get(taskType);
                if (existing != null && !existing.isEmpty()) {
                    continue;
                }

                storeChain(agentName, taskType, buildBootstrapChain(agentName, taskType));
                seeded++;
            }
        }

        return seeded;
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    private ReasoningChain buildChain(AgentDecision d) {
        ReasoningChain chain = new ReasoningChain();
        chain.chainId = UUID.randomUUID().toString().substring(0, 8);
        chain.agentName = d.agent;
        chain.taskType = d.taskType;
        chain.projectId = d.projectId;
        chain.confidence = d.confidence;
        chain.outcomeResult = d.result;

        // Build step-by-step chain from available decision fields
        List<String> steps = new ArrayList<>();
        if (d.reasoning != null)              steps.add("REASONING: " + d.reasoning);
        if (d.alternativesConsidered != null)
            d.alternativesConsidered.forEach(a -> steps.add("CONSIDERED: " + a));
        if (d.selectedAlternative != null)    steps.add("SELECTED: " + d.selectedAlternative);
        if (d.decision != null)               steps.add("DECISION: " + d.decision);
        if (d.outcome != null)                steps.add("OUTCOME: " + d.outcome);

        chain.steps = steps;
        chain.keywords = extractKeywords(d);
        chain.timestamp = System.currentTimeMillis();
        return chain;
    }

    private void storeChain(String agent, String taskType, ReasoningChain chain) {
        chainStore.computeIfAbsent(agent, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(taskType, k -> Collections.synchronizedList(new ArrayList<>()));

        List<ReasoningChain> list = chainStore.get(agent).get(taskType);
        list.add(chain);

        // Keep only top MAX_CHAINS by confidence
        if (list.size() > MAX_CHAINS_PER_SLOT) {
            list.sort(Comparator.comparingDouble((ReasoningChain c) -> c.confidence).reversed());
            list.subList(MAX_CHAINS_PER_SLOT, list.size()).clear();
        }

        // Persist to Firebase (not GitHub)
        firebaseRepo.saveChain(agent, taskType, chain.chainId, chain.toMap());
    }

    private ReasoningChain buildBootstrapChain(String agentName, String taskType) {
        ReasoningChain chain = new ReasoningChain();
        chain.chainId = "seed-" + UUID.randomUUID().toString().substring(0, 8);
        chain.agentName = agentName;
        chain.taskType = taskType;
        chain.projectId = "bootstrap";
        chain.confidence = 0.72f;
        chain.outcomeResult = "SUCCESS";
        chain.timestamp = System.currentTimeMillis();
        chain.steps = Arrays.asList(
            "REASONING: analyze the task context and identify the core objective",
            "CONSIDERED: compare known successful patterns for " + taskType,
            "SELECTED: choose the lowest-risk actionable path",
            "DECISION: execute an incremental " + taskType.toLowerCase(Locale.ROOT) + " plan",
            "OUTCOME: bootstrap reasoning chain available for future retrieval"
        );
        chain.keywords = new ArrayList<>(tokenize(String.join(" ", chain.steps)));
        return chain;
    }

    /**
     * Keyword-overlap similarity: simple but effective for structured agent reasoning.
     * No external embedding model needed.
     */
    private double computeSimilarity(String context, ReasoningChain chain) {
        if (context == null || chain.keywords == null || chain.keywords.isEmpty()) return 0;

        Set<String> contextWords = tokenize(context);
        long overlap = chain.keywords.stream().filter(contextWords::contains).count();
        double denominator = Math.max(contextWords.size(), chain.keywords.size());
        return denominator == 0 ? 0 : overlap / denominator;
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("[\\s,.:;!?()\\[\\]{}]+"))
            .filter(w -> w.length() > 3)
            .collect(Collectors.toSet());
    }

    private List<String> extractKeywords(AgentDecision d) {
        String combined = String.join(" ",
            d.reasoning != null ? d.reasoning : "",
            d.decision != null ? d.decision : "",
            d.outcome != null ? d.outcome : "");
        return new ArrayList<>(tokenize(combined));
    }

    // ── ReasoningChain value object ─────────────────────────────────────────────

    public static class ReasoningChain {
        public String chainId;
        public String agentName;
        public String taskType;
        public String projectId;
        public float confidence;
        public String outcomeResult;
        public List<String> steps = new ArrayList<>();
        public List<String> keywords = new ArrayList<>();
        public long timestamp;
        private double retrievalScore;

        public void setRetrievalScore(double s) { this.retrievalScore = s; }
        public double getRetrievalScore()        { return retrievalScore; }

        /**
         * Format this chain as a numbered prompt for an AI to follow.
         * Used by Level 4 as a few-shot example.
         */
        public String toPromptExample() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Past Successful Reasoning Chain ===\n");
            sb.append("Agent: ").append(agentName)
              .append(" | Task: ").append(taskType)
              .append(" | Confidence: ").append(String.format("%.2f", confidence)).append("\n");
            sb.append("Steps:\n");
            for (int i = 0; i < steps.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
            sb.append("Result: ").append(outcomeResult).append("\n");
            sb.append("=======================================\n");
            return sb.toString();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("chainId", chainId);
            m.put("agent", agentName);
            m.put("taskType", taskType);
            m.put("confidence", confidence);
            m.put("steps", steps);
            m.put("outcome", outcomeResult);
            m.put("retrievalScore", Math.round(retrievalScore * 100.0) / 100.0);
            return m;
        }
    }
}
