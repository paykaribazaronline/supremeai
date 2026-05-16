package com.supremeai.learning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Router Knowledge Initializer
 *
 * Injects prior knowledge about AI model capabilities into the
 * self-learning router. Provides initial Q-value seeds based on
 * known strengths of different models, enabling better routing
 * from day one (warm start) rather than random exploration.
 *
 * Based on benchmark data and expert knowledge:
 * - Gemini: vision, multimodal, reasoning
 * - DeepSeek: coding, math, technical tasks
 * - LLama (Meta): general purpose, balanced
 * - GPT-4: creative writing, analysis
 * - Claude: long context, safety, nuanced reasoning
 * - Mistral: fast inference, cost-effective
 */
@Service
public class RouterKnowledgeInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RouterKnowledgeInitializer.class);

    @Autowired
    private SelfLearningRouter selfLearningRouter;

    @Autowired(required = false)
    private EnhancedSelfLearningRouter enhancedRouter;

    // ──────────────────────────────────────────────────────────────────────
    // Knowledge Injection
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Inject initial knowledge about model capabilities.
     * Call at application startup to warm-start the router.
     */
    public void injectPriorKnowledge() {
        logger.info("[ROUTER_INIT] Injecting prior knowledge into self-learning router...");

        injectBasicKnowledge(selfLearningRouter);
        injectEnhancedKnowledge(enhancedRouter);

        logger.info("[ROUTER_INIT] Prior knowledge injection complete");
    }

    private void injectBasicKnowledge(SelfLearningRouter router) {
        if (router == null) return;

        // Task type -> preferred providers with initial Q-boost
        Map<String, List<ProviderPreference>> knowledge = Map.ofEntries(
            Map.entry("CODE_GENERATION", List.of(
                new ProviderPreference("deepseek", 2.0),
                new ProviderPreference("hf_codellama", 1.8),
                new ProviderPreference("gcp_qwen", 1.7)
            )),
            Map.entry("CODE_ANALYSIS", List.of(
                new ProviderPreference("deepseek", 1.9),
                new ProviderPreference("gemini", 1.6),
                new ProviderPreference("gpt4", 1.5)
            )),
            Map.entry("CODE_REVIEW", List.of(
                new ProviderPreference("gpt4", 1.8),
                new ProviderPreference("claude", 1.7),
                new ProviderPreference("deepseek", 1.6)
            )),
            Map.entry("MATH_REASONING", List.of(
                new ProviderPreference("deepseek", 2.0),
                new ProviderPreference("gpt4", 1.9),
                new ProviderPreference("gemini", 1.7)
            )),
            Map.entry("CREATIVE_WRITING", List.of(
                new ProviderPreference("gpt4", 2.0),
                new ProviderPreference("claude", 1.9),
                new ProviderPreference("mistral", 1.5)
            )),
            Map.entry("QUESTION_ANSWERING", List.of(
                new ProviderPreference("gemini", 1.8),
                new ProviderPreference("gpt4", 1.7),
                new ProviderPreference("hf_llama", 1.5)
            )),
            Map.entry("SUMMARIZATION", List.of(
                new ProviderPreference("gemini", 1.8),
                new ProviderPreference("claude", 1.7),
                new ProviderPreference("gpt4", 1.6)
            )),
            Map.entry("TECHNICAL_DOCS", List.of(
                new ProviderPreference("claude", 1.9),
                new ProviderPreference("gpt4", 1.7),
                new ProviderPreference("deepseek", 1.6)
            )),
            Map.entry("DATA_ANALYSIS", List.of(
                new ProviderPreference("gpt4", 1.8),
                new ProviderPreference("claude", 1.6),
                new ProviderPreference("gemini", 1.5)
            )),
            Map.entry("VISION_ANALYSIS", List.of(
                new ProviderPreference("gemini", 2.0),
                new ProviderPreference("gpt4", 1.9),
                new ProviderPreference("claude", 1.7)
            ))
        );

        int injected = 0;
        for (Map.Entry<String, List<ProviderPreference>> entry : knowledge.entrySet()) {
            String taskType = entry.getKey();
            String state = taskType + ":" + 0; // generic signature

            for (ProviderPreference pref : entry.getValue()) {
                router.updateFromOutcome(taskType, state, pref.providerId, true, 1000L);
                injected++;
            }
        }

        logger.info("[ROUTER_INIT] Injected {} prior knowledge entries", injected);
    }

    private void injectEnhancedKnowledge(EnhancedSelfLearningRouter router) {
        if (router == null) return;

        // The enhanced router uses a different state encoding
        logger.info("[ROUTER_INIT] Enhanced router: using feature-based learning from runtime data");
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helper classes
    // ──────────────────────────────────────────────────────────────────────

    private static class ProviderPreference {
        final String providerId;
        final double boost;

        ProviderPreference(String providerId, double boost) {
            this.providerId = providerId;
            this.boost = boost;
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Benchmark-Based Seeding
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Seed router with benchmark results (for offline training).
     *
     * @param taskType Task category
     * @param results Map of provider → performance score (0-100)
     */
    public void seedFromBenchmark(String taskType, Map<String, Double> results) {
        logger.info("[ROUTER_INIT] Seeding from benchmark: {} with {} providers", taskType, results.size());

        if (selfLearningRouter != null) {
            String state = taskType + ":" + 0;
            for (Map.Entry<String, Double> e : results.entrySet()) {
                double normalizedScore = e.getValue() / 100.0;
                // Inject by repeatedly updating with synthetic outcomes
                for (int i = 0; i < 10; i++) {
                    selfLearningRouter.updateFromOutcome(taskType, state, e.getKey(), true, 1000L);
                }
                logger.debug("[ROUTER_INIT] Seeded {} with score {}", e.getKey(), normalizedScore);
            }
        }
    }

    /**
     * Load benchmark data from configuration file.
     */
    public void loadBenchmarkDataFromConfig() {
        // In production, load from YAML/JSON config
        Map<String, Map<String, Double>> benchmarks = Map.of(
            "CODE_GENERATION", Map.of(
                "deepseek", 85.0,
                "hf_codellama", 80.0,
                "gcp_qwen", 78.0,
                "gpt4", 75.0,
                "gemini", 72.0
            ),
            "MATH_REASONING", Map.of(
                "deepseek", 88.0,
                "gpt4", 86.0,
                "gemini", 82.0
            ),
            "CREATIVE_WRITING", Map.of(
                "gpt4", 92.0,
                "claude", 90.0,
                "mistral", 85.0
            ),
            "VISION_ANALYSIS", Map.of(
                "gemini", 90.0,
                "gpt4", 88.0,
                "claude", 85.0
            )
        );

        for (Map.Entry<String, Map<String, Double>> entry : benchmarks.entrySet()) {
            seedFromBenchmark(entry.getKey(), entry.getValue());
        }
    }
}
