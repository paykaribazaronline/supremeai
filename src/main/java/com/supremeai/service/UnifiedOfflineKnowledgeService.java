package com.supremeai.service;

import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.provider.StubLocalProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * UnifiedOfflineKnowledgeService handles the Core Knowledge of the system.
 *
 * <p>CORE KNOWLEDGE MAIN ROLE & RESPONSIBILITY: - From now on, the Core Knowledge acts as the
 * primary decision-making brain of the system. - It determines HOW to route and coordinate other
 * components (e.g., deciding when to use the BrowserService/web automation, when to call a deployed
 * helper AI, and how to apply prompt engineering decisions dynamically). - It orchestrates other
 * services rather than just holding static answers.
 */
@Service
public class UnifiedOfflineKnowledgeService {

  private static final Logger log = LoggerFactory.getLogger(UnifiedOfflineKnowledgeService.class);

  private final SupremeLearningOrchestrator learningOrchestrator;
  private final StubLocalProvider stubLocalProvider;
  private final SoloModeService soloModeService;

  @Autowired
  public UnifiedOfflineKnowledgeService(
      SupremeLearningOrchestrator learningOrchestrator,
      StubLocalProvider stubLocalProvider,
      SoloModeService soloModeService) {
    this.learningOrchestrator = learningOrchestrator;
    this.stubLocalProvider = stubLocalProvider;
    this.soloModeService = soloModeService;
  }

  /**
   * Decides if a query is normal or complex. (Change made: Core Knowledge no longer provides static
   * answers; it behaves as coordinator/decision-maker)
   */
  public Mono<Boolean> isQueryComplex(String query) {
    if (query == null || query.trim().isEmpty()) {
      return Mono.just(false);
    }
    String lower = query.toLowerCase();
    boolean isComplex =
        lower.contains("generate")
            || lower.contains("write code")
            || lower.contains("review")
            || lower.contains("vulnerability")
            || lower.contains("complex")
            || lower.contains("audit")
            || lower.contains("refactor")
            || lower.contains("optimize");
    return Mono.just(isComplex);
  }

  /**
   * Fallback routing if needed. (Change made: Delegated static answers to browser / database
   * learning / cloud models)
   */
  public Mono<String> findAnswer(String query) {
    return isQueryComplex(query)
        .flatMap(
            complex -> {
              if (complex) {
                log.info("🧠 [Core Knowledge Decision] Route to Local Model");
                return soloModeService.askLocalModel(query)
                    .onErrorResume(e -> stubLocalProvider.generate(query));
              } else {
                log.info("🧠 [Core Knowledge Decision] Route to Browser and Database Learning");
                return Mono.just("ROUTE_TO_BROWSER_AND_DATABASE");
              }
            });
  }
}
