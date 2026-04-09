package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Capability-Based AI Routing Service
 *
 * Problem: Multi-AI Consensus Voting
 * - Different models produce different response formats/styles
 * - Voting on incompatible response types is meaningless
 * - Example: "Use Spring Boot" vs "I recommend Spring Framework"
 *   → Same meaning, but text-based voting sees them as different
 * - Impossible to reach consensus on semantics
 * - Result: Votes don't reflect truth; just who happened to phrase similar
 *
 * Solution: Task-Based Capability Routing
 * Instead of: "Ask all 10 AIs the same question & vote"
 * Do this: "Route task to specialists + synthesize results"
 *
 * Model Specializations:
 * - OpenAI GPT-4: Code generation, architecture, reasoning (0.95 score)
 * - Anthropic Claude: Safety, compliance, security analysis (0.93 score)
 * - Google Gemini: System design, optimization, knowledge (0.91 score)
 * - Meta Llama: Efficiency, open-source patterns (0.85 score)
 * - Mistral: Code completions, lightweight tasks (0.80 score)
 * - Cohere: Semantic understanding, NLP tasks (0.78 score)
 * - HuggingFace: ML/data pipelines, transformers (0.75 score)
 * - xAI Grok: Reasoning, edge cases (0.82 score)
 * - DeepSeek: Deep reasoning, mathematical (0.80 score)
 * - Perplexity: Web search, current knowledge (0.88 score)
 *
 * Task Types:
 * 1. CODE_GENERATION → OpenAI, Meta, Mistal
 * 2. SECURITY_REVIEW → Anthropic, OpenAI
 * 3. ARCHITECTURE_DESIGN → Google, OpenAI, xAI
 * 4. API_DESIGN → Google, OpenAI
 * 5. OPTIMIZATION → Google, DeepSeek
 * 6. COMPLIANCE_CHECK → Anthropic, Google
 * 7. DATA_PIPELINE → HuggingFace, DeepSeek
 * 8. KNOWLEDGE_LOOKUP → Perplexity, Google
 * 9. REASONING → Anthropic, xAI, DeepSeek
 * 10. CREATIVE → OpenAI, Anthropic
 *
 * Synthesis:
 * - Combine insights from specialist models
 * - Weight by expertise (OpenAI code generation gets higher weight)
 * - Identify consensus areas (all agree on X)
 * - Flag disagreement areas (specialists differ on Y)
 * - Return: structured result with reasoning
 *
 * Example Flow:
 *  Question: "Design a REST API for video processing"
 *  Task Type: ARCHITECTURE_DESIGN + API_DESIGN
 *  Routing:
 *    - Google Gemini (system design) → Architecture patterns
 *    - OpenAI GPT-4 (API expert) → REST endpoint design
 *    - Anthropic Claude (safety) → Security considerations
 *  Synthesis:
 *    - Architecture from Gemini (best expertise)
 *    - Endpoint patterns from OpenAI (best expertise)
 *    - Security notes from Anthropic (best expertise)
 *  Result: Comprehensive answer using each model's strengths
 */
@Service
public class CapabilityBasedAIRoutingService {
    private static final Logger logger = LoggerFactory.getLogger(CapabilityBasedAIRoutingService.class);

    // Task type enumeration
    public enum TaskType {
        CODE_GENERATION("Generating code, implementing features, debugging"),
        SECURITY_REVIEW("Security scanning, vulnerability analysis, safety checks"),
        ARCHITECTURE_DESIGN("System design, microservices, scalability planning"),
        API_DESIGN("REST API design, endpoint definition, contracts"),
        OPTIMIZATION("Performance tuning, resource optimization, efficiency"),
        COMPLIANCE_CHECK("GDPR, SOC2, regulatory compliance verification"),
        DATA_PIPELINE("Data processing, ETL pipelines, data engineering"),
        KNOWLEDGE_LOOKUP("Factual lookups, current information, web search"),
        REASONING("Complex reasoning, multi-step logic, edge cases"),
        CREATIVE("Creative solutions, novel approaches, brainstorming");

        private final String description;

        TaskType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Model capabilities and scores
    private final Map<String, Map<TaskType, Double>> modelCapabilities = new HashMap<>();

    public CapabilityBasedAIRoutingService() {
        initializeModelCapabilities();
    }

    /**
     * Initialize model capability matrix
     */
    private void initializeModelCapabilities() {
        // OpenAI GPT-4: Excellent at code, architecture, reasoning
        modelCapabilities.put("openai", Map.ofEntries(
            Map.entry(TaskType.CODE_GENERATION, 0.99),
            Map.entry(TaskType.ARCHITECTURE_DESIGN, 0.97),
            Map.entry(TaskType.REASONING, 0.96),
            Map.entry(TaskType.SECURITY_REVIEW, 0.92),
            Map.entry(TaskType.API_DESIGN, 0.98),
            Map.entry(TaskType.OPTIMIZATION, 0.90),
            Map.entry(TaskType.COMPLIANCE_CHECK, 0.85),
            Map.entry(TaskType.DATA_PIPELINE, 0.80),
            Map.entry(TaskType.KNOWLEDGE_LOOKUP, 0.80),
            Map.entry(TaskType.CREATIVE, 0.95)
        ));

        // Anthropic Claude: Excellent at safety, compliance, reasoning
        modelCapabilities.put("anthropic", Map.ofEntries(
            Map.entry(TaskType.SECURITY_REVIEW, 0.98),
            Map.entry(TaskType.COMPLIANCE_CHECK, 0.97),
            Map.entry(TaskType.REASONING, 0.95),
            Map.entry(TaskType.CODE_GENERATION, 0.92),
            Map.entry(TaskType.ARCHITECTURE_DESIGN, 0.90),
            Map.entry(TaskType.CREATIVE, 0.94),
            Map.entry(TaskType.API_DESIGN, 0.88),
            Map.entry(TaskType.OPTIMIZATION, 0.82),
            Map.entry(TaskType.DATA_PIPELINE, 0.75),
            Map.entry(TaskType.KNOWLEDGE_LOOKUP, 0.78)
        ));

        // Google Gemini: Excellent at system design, knowledge
        modelCapabilities.put("google", Map.ofEntries(
            Map.entry(TaskType.ARCHITECTURE_DESIGN, 0.96),
            Map.entry(TaskType.KNOWLEDGE_LOOKUP, 0.95),
            Map.entry(TaskType.OPTIMIZATION, 0.94),
            Map.entry(TaskType.CODE_GENERATION, 0.91),
            Map.entry(TaskType.REASONING, 0.93),
            Map.entry(TaskType.API_DESIGN, 0.90),
            Map.entry(TaskType.SECURITY_REVIEW, 0.88),
            Map.entry(TaskType.DATA_PIPELINE, 0.85),
            Map.entry(TaskType.COMPLIANCE_CHECK, 0.82),
            Map.entry(TaskType.CREATIVE, 0.85)
        ));

        // Meta Llama: Good at code, efficient, open-source patterns
        modelCapabilities.put("meta", Map.ofEntries(
            Map.entry(TaskType.CODE_GENERATION, 0.88),
            Map.entry(TaskType.OPTIMIZATION, 0.85),
            Map.entry(TaskType.ARCHITECTURE_DESIGN, 0.82),
            Map.entry(TaskType.REASONING, 0.80),
            Map.entry(TaskType.API_DESIGN, 0.81),
            Map.entry(TaskType.SECURITY_REVIEW, 0.78),
            Map.entry(TaskType.DATA_PIPELINE, 0.80),
            Map.entry(TaskType.COMPLIANCE_CHECK, 0.72),
            Map.entry(TaskType.KNOWLEDGE_LOOKUP, 0.70),
            Map.entry(TaskType.CREATIVE, 0.75)
        ));

        // Anthropic Claude: Similar to above, let's reuse...
        // (In practice, you'd calibrate these based on benchmark results)

        // Perplexity: Excellent at knowledge lookup (has web search)
        modelCapabilities.put("perplexity", Map.ofEntries(
            Map.entry(TaskType.KNOWLEDGE_LOOKUP, 0.96),
            Map.entry(TaskType.REASONING, 0.88),
            Map.entry(TaskType.CODE_GENERATION, 0.80),
            Map.entry(TaskType.ARCHITECTURE_DESIGN, 0.82),
            Map.entry(TaskType.CREATIVE, 0.85),
            Map.entry(TaskType.API_DESIGN, 0.78),
            Map.entry(TaskType.SECURITY_REVIEW, 0.75),
            Map.entry(TaskType.OPTIMIZATION, 0.75),
            Map.entry(TaskType.DATA_PIPELINE, 0.70),
            Map.entry(TaskType.COMPLIANCE_CHECK, 0.70)
        ));

        // Fill in remaining models with reasonable defaults
        String[] models = {"mistral", "cohere", "huggingface", "xai", "deepseek"};
        for (String model : models) {
            modelCapabilities.put(model,
                allTaskTypes().stream()
                    .collect(Collectors.toMap(
                        t -> t,
                        t -> 0.75  // Default middling score
                    ))
            );
        }
    }

    /**
     * Infer task type from question text
     */
    public TaskType inferTaskType(String question) {
        if (question == null) return TaskType.CODE_GENERATION;

        String q = question.toLowerCase();

        if (matchesPattern(q, "security|vulnerable|exploit|attack|authentication|encrypt")) {
            return TaskType.SECURITY_REVIEW;
        }
        if (matchesPattern(q, "design|architecture|microservice|scalable|pattern|structure")) {
            return TaskType.ARCHITECTURE_DESIGN;
        }
        if (matchesPattern(q, "api|endpoint|rest|http|route|request|response|contract")) {
            return TaskType.API_DESIGN;
        }
        if (matchesPattern(q, "optimize|performance|speed|efficient|memory|cpu|latency")) {
            return TaskType.OPTIMIZATION;
        }
        if (matchesPattern(q, "compliance|gdpr|ccpa|sox|pci|regulation|requirement")) {
            return TaskType.COMPLIANCE_CHECK;
        }
        if (matchesPattern(q, "pipeline|etl|data|batch|stream|spark|hadoop")) {
            return TaskType.DATA_PIPELINE;
        }
        if (matchesPattern(q, "what|when|where|how|why|current|latest|today|news")) {
            return TaskType.KNOWLEDGE_LOOKUP;
        }
        if (matchesPattern(q, "reason|analyze|explain|deduce|prove|logic|think deeply")) {
            return TaskType.REASONING;
        }
        if (matchesPattern(q, "generate|code|implement|write|build|create|fix|debug")) {
            return TaskType.CODE_GENERATION;
        }

        return TaskType.CODE_GENERATION;  // Default
    }

    /**
     * Route question to best specialists
     *
     * @param question The task/question
     * @param availableModels List of available AI models
     * @param topN Return top N specialists
     * @return List of specialist models ranked by capability
     */
    public List<String> routeToSpecialists(String question, List<String> availableModels, int topN) {
        TaskType taskType = inferTaskType(question);
        return routeToSpecialists(taskType, availableModels, topN);
    }

    /**
     * Route task type to best specialists
     */
    public List<String> routeToSpecialists(TaskType taskType, List<String> availableModels, int topN) {
        List<Map.Entry<String, Double>> scores = new ArrayList<>();

        for (String model : availableModels) {
            String normalizedModel = normalizeModelName(model);
            Map<TaskType, Double> capabilities = modelCapabilities.getOrDefault(normalizedModel, new HashMap<>());
            Double score = capabilities.getOrDefault(taskType, 0.7);
            scores.add(new AbstractMap.SimpleEntry<>(model, score));
        }

        List<String> specialists = scores.stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(topN)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        logger.info("🎯 Routed {} to {} specialists: {} (scores: {})",
            taskType.name(),
            specialists.size(),
            specialists,
            scores.stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(e -> e.getKey() + ":" + String.format("%.2f", e.getValue()))
                .collect(Collectors.toList())
        );

        return specialists;
    }

    /**
     * Get model capability scores for a task type
     */
    public Map<String, Double> getCapabilityScores(TaskType taskType, List<String> models) {
        Map<String, Double> scores = new HashMap<>();
        for (String model : models) {
            String normalizedModel = normalizeModelName(model);
            Map<TaskType, Double> capabilities = modelCapabilities.getOrDefault(normalizedModel, new HashMap<>());
            scores.put(model, capabilities.getOrDefault(taskType, 0.7));
        }
        return scores;
    }

    /**
     * Synthesize results from multiple specialists
     */
    public Map<String, Object> synthesizeResults(
        TaskType taskType,
        Map<String, String> specialistResponses
    ) {
        Map<String, Object> synthesis = new HashMap<>();
        synthesis.put("task_type", taskType.name());
        synthesis.put("task_description", taskType.getDescription());
        synthesis.put("specialists_consulted", specialistResponses.size());
        synthesis.put("specialist_responses", specialistResponses);

        // Identify consensus areas
        String commonThemes = extractCommonThemes(specialistResponses.values());
        synthesis.put("common_themes", commonThemes);

        // Identify disagreement areas
        List<String> disagreements = identifyDisagreements(specialistResponses.values());
        synthesis.put("areas_of_disagreement", disagreements);

        // Generate synthesis
        String synthesized = generateSynthesis(taskType, specialistResponses);
        synthesis.put("synthesized_answer", synthesized);

        // Confidence based on specialist agreement
        double consensus = calculateConsensusConfidence(specialistResponses.values());
        synthesis.put("confidence", consensus);

        return synthesis;
    }

    /**
     * Extract common themes from responses
     */
    private String extractCommonThemes(Collection<String> responses) {
        // Simple implementation - in production use NLP/embeddings
        Map<String, Integer> wordCounts = new HashMap<>();
        for (String response : responses) {
            for (String word : response.split("\\W+")) {
                if (word.length() > 4) {
                    wordCounts.put(word.toLowerCase(), wordCounts.getOrDefault(word, 0) + 1);
                }
            }
        }

        return wordCounts.entrySet().stream()
            .filter(e -> e.getValue() >= responses.size() / 2)  // Mentioned in >50%
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(5)
            .map(e -> e.getKey() + " (" + e.getValue() + "/" + responses.size() + ")")
            .collect(Collectors.joining(", "));
    }

    /**
     * Identify areas where specialists disagree
     */
    private List<String> identifyDisagreements(Collection<String> responses) {
        // Simplified - in production use semantic similarity
        List<String> disagreements = new ArrayList<>();
        if (responses.size() <= 1) return disagreements;

        // Check reference counts (if different, likely disagreement)
        Set<String> technologies = new HashSet<>();
        for (String response : responses) {
            if (response.toLowerCase().contains("spring")) technologies.add("Spring");
            if (response.toLowerCase().contains("django")) technologies.add("Django");
            if (response.toLowerCase().contains("rails")) technologies.add("Rails");
        }

        if (technologies.size() > 1) {
            disagreements.add("Technology stack varies across specialists: " + technologies);
        }

        return disagreements;
    }

    /**
     * Generate synthesis of specialist responses
     */
    private String generateSynthesis(TaskType taskType, Map<String, String> specialistResponses) {
        StringBuilder synthesis = new StringBuilder();
        synthesis.append("Synthesis based on ").append(specialistResponses.size()).append(" specialist responses:\n");

        for (Map.Entry<String, String> entry : specialistResponses.entrySet()) {
            String model = entry.getKey();
            String response = entry.getValue();
            synthesis.append("\n").append(model).append(": ").append(response.substring(0, Math.min(150, response.length())));
        }

        return synthesis.toString();
    }

    /**
     * Calculate confidence from specialist agreement
     */
    private double calculateConsensusConfidence(Collection<String> responses) {
        if (responses.size() <= 1) return 0.5;
        // Simplified - in production compute semantic similarity
        return Math.min(1.0, 0.6 + (responses.size() * 0.1));
    }

    // Helper methods
    private boolean matchesPattern(String text, String pattern) {
        return Pattern.compile(pattern).matcher(text).find();
    }

    private String normalizeModelName(String model) {
        return model.toLowerCase()
            .replaceAll("-.*", "")
            .replaceAll("_.*", "");
    }

    private List<TaskType> allTaskTypes() {
        return Arrays.asList(TaskType.values());
    }
}
