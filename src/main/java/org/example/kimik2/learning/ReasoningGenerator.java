package org.example.kimik2.learning;

import org.example.service.AIAPIService;
import org.example.service.SystemLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LEVEL 4: Reasoning Generator
 *
 * "নিজে reasoning generate করা" — Level 3 এর চেয়ে এক ধাপ উপরে।
 *
 * Level 3 vs Level 4 পার্থক্য:
 *   Level 3: "এই exact chain আগে কাজ করেছিল → copy করো"
 *   Level 4: "এই pattern দেখেছি → নতুন situation-এ নিজে reasoning তৈরি করো"
 *
 * কীভাবে কাজ করে (Few-Shot Prompting via external AI):
 *   1. Level 2 থেকে: এই agent-এর top reasoning patterns নাও
 *   2. Level 3 থেকে: সবচেয়ে কাছের 3টা সফল chain নাও
 *   3. এগুলো "examples" হিসেবে AI-কে দাও
 *   4. AI নতুন task-এর জন্য ওই style-এ reasoning generate করে
 *   5. Generated reasoning → AgenticToolLoop-এ plan হিসেবে ব্যবহার
 *
 * এটা LLM fine-tuning ছাড়াই Level 4 achieve করার practical way।
 * Kimi K2 করে internally — আমরা করি externally via few-shot.
 * ফলাফল almost same: proven patterns থেকে নতুন reasoning তৈরি।
 */
@Service
public class ReasoningGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ReasoningGenerator.class);

    // Minimum examples to include in few-shot prompt
    private static final int FEW_SHOT_EXAMPLES = 3;

    @Autowired
    private AgentPatternProfiler profiler;

    @Autowired
    private ReasoningChainCopier chainCopier;

    @Autowired
    private AIAPIService aiService;

    @Autowired
    private SystemLearningService learningService;

    // Cache generated reasonings to avoid duplicate AI calls
    private final Map<String, GeneratedReasoning> reasoningCache = new LinkedHashMap<>() {
        protected boolean removeEldestEntry(Map.Entry<String, GeneratedReasoning> e) {
            return size() > 200; // keep last 200
        }
    };

    /**
     * Generate reasoning for a new task using few-shot examples from Levels 2 & 3.
     *
     * Full flow:
     *   Level 2 patterns + Level 3 chains → few-shot prompt → AI generates reasoning
     *   → parse into steps → return as AgenticToolLoop plan
     *
     * @param agentName   the agent that will execute the task
     * @param taskType    type of task
     * @param newContext  description of the new problem/goal
     * @return            generated reasoning with step-by-step plan
     */
    public GeneratedReasoning generateReasoning(String agentName, String taskType,
                                                String newContext) {
        String cacheKey = agentName + "|" + taskType + "|" + newContext.hashCode();
        if (reasoningCache.containsKey(cacheKey)) {
            logger.debug("⚡ Level 4: Cache hit for agent={} task={}", agentName, taskType);
            return reasoningCache.get(cacheKey);
        }

        logger.info("🧠 Level 4: Generating reasoning for agent={} task={}", agentName, taskType);

        // ── STEP 1: Gather Level 2 patterns ────────────────────────────────────
        AgentPatternProfiler.AgentProfile profile = profiler.getProfile(agentName);
        List<String> knownPatterns = profile.topReasoningPatterns;

        // ── STEP 2: Gather Level 3 chains ──────────────────────────────────────
        List<ReasoningChainCopier.ReasoningChain> nearestChains = findNearestChains(
            agentName, taskType, newContext, FEW_SHOT_EXAMPLES);

        // ── STEP 3: Build few-shot prompt ───────────────────────────────────────
        String prompt = buildFewShotPrompt(agentName, taskType, newContext,
            knownPatterns, nearestChains);

        // ── STEP 4: Call AI API ─────────────────────────────────────────────────
        String aiResponse = callAI(prompt, agentName);

        // ── STEP 5: Parse response into structured steps ────────────────────────
        GeneratedReasoning result = parseResponse(aiResponse, agentName, taskType, newContext);
        result.fewShotExamplesUsed = nearestChains.size();
        result.patternsUsed = knownPatterns.size();

        // ── STEP 6: Cache + persist ─────────────────────────────────────────────
        reasoningCache.put(cacheKey, result);
        learningService.recordPattern(
            "level4-generated",
            agentName + " generated " + result.steps.size() + " steps for " + taskType,
            "Few-shot from " + nearestChains.size() + " chains + " + knownPatterns.size() + " patterns"
        );

        logger.info("✅ Level 4: Generated {} steps (examples={} patterns={} confidence={:.2f})",
            result.steps.size(), nearestChains.size(), knownPatterns.size(), result.confidence);

        return result;
    }

    /**
     * Generate a full routing + reasoning plan for multiple agents.
     * Used by KimiK2Orchestrator when no custom plan is provided.
     */
    public List<String> generateToolPlan(String agentName, String taskType, String context) {
        GeneratedReasoning reasoning = generateReasoning(agentName, taskType, context);
        return reasoning.steps.stream()
            .map(step -> mapStepToTool(step, context))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Stats for monitoring.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("cache_size", reasoningCache.size());
        stats.put("few_shot_examples_setting", FEW_SHOT_EXAMPLES);
        long successful = reasoningCache.values().stream()
            .filter(r -> r.confidence >= 0.7).count();
        stats.put("high_confidence_generated", successful);
        return stats;
    }

    public int getGeneratedReasoningCount(String agentName) {
        if (agentName == null || agentName.isBlank()) {
            return 0;
        }
        return (int) reasoningCache.values().stream()
            .filter(reasoning -> agentName.equals(reasoning.agentName))
            .count();
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    private List<ReasoningChainCopier.ReasoningChain> findNearestChains(
            String agentName, String taskType, String context, int k) {

        List<ReasoningChainCopier.ReasoningChain> allChains =
            chainCopier.getChainsForAgent(agentName);

        // Score by keyword similarity and return top-k
        return allChains.stream()
            .filter(c -> taskType.equals(c.taskType))
            .sorted(Comparator.comparingDouble(c ->
                -computeSimilarity(context, c.keywords)))
            .limit(k)
            .collect(Collectors.toList());
    }

    private String buildFewShotPrompt(String agentName, String taskType, String context,
                                      List<String> patterns,
                                      List<ReasoningChainCopier.ReasoningChain> chains) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are the SupremeAI agent: ").append(agentName).append(".\n");
        sb.append("Task type: ").append(taskType).append("\n\n");

        // Level 2: known patterns
        if (!patterns.isEmpty()) {
            sb.append("Your known reasoning patterns (from past successful tasks):\n");
            patterns.forEach(p -> sb.append("  - ").append(p).append("\n"));
            sb.append("\n");
        }

        // Level 3: few-shot examples
        if (!chains.isEmpty()) {
            sb.append("Past successful reasoning chains for similar tasks:\n");
            chains.forEach(c -> sb.append(c.toPromptExample()).append("\n"));
        }

        sb.append("NEW TASK:\n").append(context).append("\n\n");
        sb.append("Following the same reasoning style as the examples above, ");
        sb.append("provide a step-by-step reasoning chain to solve this task. ");
        sb.append("Format each step on a new line starting with a number. ");
        sb.append("Be specific and actionable. Maximum 7 steps.\n");

        return sb.toString();
    }

    private String callAI(String prompt, String agentName) {
        try {
            if (aiService != null) {
                String response = aiService.callAI(agentName, prompt,
                    aiService.getFallbackChainForProvider(agentName));
                if (response != null && !response.isBlank()) return response;
            }
        } catch (Exception e) {
            logger.warn("Level 4: AI call failed ({}), using pattern-based fallback", e.getMessage());
        }
        // Fallback: generate a safe structural response
        return "1. Analyze the current state and identify the problem\n" +
               "2. Review past patterns for similar issues\n" +
               "3. Apply the most effective known solution\n" +
               "4. Verify the result\n" +
               "5. Record the outcome for future learning";
    }

    private GeneratedReasoning parseResponse(String aiResponse, String agentName,
                                             String taskType, String context) {
        GeneratedReasoning result = new GeneratedReasoning();
        result.agentName = agentName;
        result.taskType = taskType;
        result.originalContext = context;
        result.rawResponse = aiResponse;

        // Parse numbered steps: "1. Do X", "2. Do Y", etc.
        String[] lines = aiResponse.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.matches("^\\d+\\..*") && trimmed.length() > 4) {
                String step = trimmed.replaceFirst("^\\d+\\.\\s*", "").trim();
                if (!step.isBlank()) result.steps.add(step);
            }
        }

        // Fallback if parsing failed
        if (result.steps.isEmpty()) {
            result.steps.add("analyze: " + context.substring(0, Math.min(context.length(), 50)));
            result.steps.add("apply_fix: description=" + context.substring(0, Math.min(context.length(), 50)));
        }

        // Confidence: higher if we had good few-shot examples
        result.confidence = 0.5 + (result.steps.size() >= 3 ? 0.2 : 0.0);
        result.timestamp = System.currentTimeMillis();
        return result;
    }

    private String mapStepToTool(String step, String context) {
        String lower = step.toLowerCase();
        if (lower.contains("run test") || lower.contains("verify"))
            return "run_tests";
        if (lower.contains("analyze") || lower.contains("identify") || lower.contains("review"))
            return "analyze_error:error=" + sanitize(context);
        if (lower.contains("fix") || lower.contains("apply") || lower.contains("correct"))
            return "apply_fix:description=" + sanitize(step);
        if (lower.contains("build") || lower.contains("compile"))
            return "check_build_status";
        if (lower.contains("deploy"))
            return "check_deployment_health";
        if (lower.contains("commit") || lower.contains("save"))
            return "commit_changes:message=fix: " + sanitize(step);
        if (lower.contains("security") || lower.contains("scan"))
            return "run_security_scan";
        return "log_info:message=" + sanitize(step);
    }

    private double computeSimilarity(String context, List<String> keywords) {
        if (context == null || keywords == null || keywords.isEmpty()) return 0;
        Set<String> contextWords = Arrays.stream(context.toLowerCase().split("\\s+"))
            .filter(w -> w.length() > 3).collect(Collectors.toSet());
        long overlap = keywords.stream().filter(contextWords::contains).count();
        return (double) overlap / Math.max(contextWords.size(), keywords.size());
    }

    private String sanitize(String input) {
        if (input == null) return "task";
        return input.replaceAll("[^a-zA-Z0-9 _\\-.]", "")
            .trim().substring(0, Math.min(input.length(), 60));
    }

    // ── GeneratedReasoning value object ─────────────────────────────────────────

    public static class GeneratedReasoning {
        public String agentName;
        public String taskType;
        public String originalContext;
        public String rawResponse;
        public List<String> steps = new ArrayList<>();
        public double confidence;
        public int fewShotExamplesUsed;
        public int patternsUsed;
        public long timestamp;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("agent", agentName);
            m.put("taskType", taskType);
            m.put("steps", steps);
            m.put("confidence", Math.round(confidence * 100.0) / 100.0);
            m.put("few_shot_examples_used", fewShotExamplesUsed);
            m.put("patterns_used", patternsUsed);
            m.put("generated_at", timestamp);
            return m;
        }
    }
}
