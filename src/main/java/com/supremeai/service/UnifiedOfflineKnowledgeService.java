package com.supremeai.service;

import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.provider.StubLocalProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UnifiedOfflineKnowledgeService {

  private static final Logger log = LoggerFactory.getLogger(UnifiedOfflineKnowledgeService.class);

  private final SupremeLearningOrchestrator learningOrchestrator;
  private final StubLocalProvider stubLocalProvider;

  @Autowired
  public UnifiedOfflineKnowledgeService(
      SupremeLearningOrchestrator learningOrchestrator, StubLocalProvider stubLocalProvider) {
    this.learningOrchestrator = learningOrchestrator;
    this.stubLocalProvider = stubLocalProvider;
  }

  /**
   * Finds answer from core knowledge (Tier 1) first. If not found, falls back to StubLocalProvider
   * (Tier 3).
   *
   * @param query The user question
   * @return Mono<String> containing the answer
   */
  public Mono<String> findAnswer(String query) {
    if (query == null || query.trim().isEmpty()) {
      return Mono.just("আমি সুপ্রিমএআই। আপনার প্রশ্ন লিখুন, আমি সাহায্য করব।");
    }

    return Mono.fromCallable(
            () -> {
              try {
                String solution = learningOrchestrator.findCoreKnowledgeSolution(query);
                if (solution != null && !solution.isEmpty()) {
                  log.info("[UnifiedOfflineKnowledge] ✅ Tier 1 Hit — Core Knowledge matched");
                  return solution;
                }
              } catch (Exception e) {
                log.warn("[UnifiedOfflineKnowledge] Tier 1 lookup failed: {}", e.getMessage());
              }
              return "";
            })
        .flatMap(
            ans -> {
              if (!ans.isEmpty()) {
                return Mono.just(ans);
              }
              log.info(
                  "[UnifiedOfflineKnowledge] ⚪ Tier 1 Miss — Falling back to StubLocalProvider (Tier 3)");
              return stubLocalProvider.generate(query);
            })
        .defaultIfEmpty("আমি লোকাল মোডে সক্রিয়।");
  }
}
