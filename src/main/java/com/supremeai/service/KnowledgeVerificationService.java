package com.supremeai.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * KnowledgeVerificationService - Verifies the integrity and quality of foundational knowledge
 * entries. Ensures critical system_learning entries exist and meet confidence thresholds.
 */
@Service
public class KnowledgeVerificationService {

  private static final Logger log = LoggerFactory.getLogger(KnowledgeVerificationService.class);

  @Autowired(required = false)
  private Firestore firestore;

  private final double defaultMinConfidence = 0.90;

  public List<String> getFoundationKnowledgeIds() {
    return List.of(
        "pattern_semantic_analysis", "rl-0001", "pattern_copilot_init", "pattern_simulator_ttl");
  }

  /**
   * Verifies a list of foundation knowledge IDs against Firestore. Checks for existence and a
   * minimum confidence score.
   *
   * @param foundationIds The list of document IDs expected to be in 'system_learning'.
   * @param minConfidence The minimum confidence score required for each entry.
   * @return A Mono containing a map with verification results (missing, low_confidence, verified).
   */
  public Mono<Map<String, Object>> verifyFoundationKnowledge(
      List<String> foundationIds, double minConfidence) {
    if (firestore == null) {
      log.warn("[KNOWLEDGE_VERIFY] Firestore not active. Cannot perform verification.");
      return Mono.just(Map.of("status", "SKIPPED", "reason", "Firestore not active"));
    }

    Map<String, Object> verificationResults = new HashMap<>();
    List<String> missing = new java.util.ArrayList<>();
    List<Map<String, Object>> lowConfidence = new java.util.ArrayList<>();
    List<Map<String, Object>> verified = new java.util.ArrayList<>();

    return Mono.defer(
        () -> {
          for (String id : foundationIds) {
            try {
              ApiFuture<DocumentSnapshot> future =
                  firestore.collection("system_learning").document(id).get();
              DocumentSnapshot document = future.get(); // Blocking call, but within Mono.defer

              if (!document.exists()) {
                missing.add(id);
                log.warn("[KNOWLEDGE_VERIFY] Foundation ID '{}' is missing.", id);
              } else {
                Double confidence = document.getDouble("confidenceScore");
                if (confidence == null || confidence < minConfidence) {
                  lowConfidence.add(
                      Map.of("id", id, "score", confidence != null ? confidence : "N/A"));
                  log.warn(
                      "[KNOWLEDGE_VERIFY] Foundation ID '{}' has low confidence: {}",
                      id,
                      confidence);
                } else {
                  verified.add(Map.of("id", id, "score", confidence));
                  log.debug(
                      "[KNOWLEDGE_VERIFY] Foundation ID '{}' verified with confidence: {}",
                      id,
                      confidence);
                }
              }
            } catch (InterruptedException | ExecutionException e) {
              log.error("[KNOWLEDGE_VERIFY] Error fetching document '{}': {}", id, e.getMessage());
              missing.add(id + " (Error: " + e.getMessage() + ")");
            }
          }
          verificationResults.put("missing_entries", missing);
          verificationResults.put("low_confidence_entries", lowConfidence);
          verificationResults.put("verified_entries", verified);
          verificationResults.put(
              "overall_status", missing.isEmpty() && lowConfidence.isEmpty() ? "PASS" : "FAIL");
          return Mono.just(verificationResults);
        });
  }

  /**
   * Verifies the foundation knowledge using default configured IDs and minimum confidence. This
   * method is suitable for scheduled tasks.
   *
   * @return A Mono containing a map with verification results.
   */
  public Mono<Map<String, Object>> verifyFoundationKnowledge() {
    return verifyFoundationKnowledge(getFoundationKnowledgeIds(), defaultMinConfidence);
  }
}
