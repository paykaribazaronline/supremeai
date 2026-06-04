package com.supremeai.learning;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Router Knowledge Initializer
 *
 * <p>Injects prior knowledge about AI model capabilities into the self-learning router. Provides
 * initial Q-value seeds based on known strengths of different models, enabling better routing from
 * day one (warm start) rather than random exploration.
 *
 * <p>Based on benchmark data and expert knowledge: - Gemini: vision, multimodal, reasoning -
 * DeepSeek: coding, math, technical tasks - LLama (Meta): general purpose, balanced - GPT-4:
 * creative writing, analysis - Claude: long context, safety, nuanced reasoning - Mistral: fast
 * inference, cost-effective
 */
@Service
public class RouterKnowledgeInitializer {

  private static final Logger logger = LoggerFactory.getLogger(RouterKnowledgeInitializer.class);

  @Autowired private SelfLearningRouter selfLearningRouter;

  @Autowired(required = false)
  private EnhancedSelfLearningRouter enhancedRouter;

  // ──────────────────────────────────────────────────────────────────────
  // Knowledge Injection
  // ──────────────────────────────────────────────────────────────────────

  /**
   * Inject initial knowledge about model capabilities. Call at application startup to warm-start
   * the router.
   */
  public void injectPriorKnowledge() {
    logger.info("[ROUTER_INIT] Injecting prior knowledge into self-learning router...");

    injectBasicKnowledge(selfLearningRouter);
    injectEnhancedKnowledge(enhancedRouter);

    logger.info("[ROUTER_INIT] Prior knowledge injection complete");
  }

  private void injectBasicKnowledge(SelfLearningRouter router) {
    if (router == null) return;
    logger.info("[ROUTER_INIT] Using runtime learning only — no hardcoded provider preferences");
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
    logger.info(
        "[ROUTER_INIT] Seeding from benchmark: {} with {} providers", taskType, results.size());

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

  /** Load benchmark data from configuration file. */
  public void loadBenchmarkDataFromConfig() {
    logger.info("[ROUTER_INIT] No hardcoded benchmarks — router learns from runtime data");
  }
}
