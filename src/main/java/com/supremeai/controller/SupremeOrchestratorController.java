package com.supremeai.controller;

import com.supremeai.intelligence.SystemSuggestionService;
import com.supremeai.learning.SupremeLearningOrchestrator;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * SupremeOrchestratorController
 *
 * <p>Provides API access to the Super-Hub orchestration logic, including intent identification,
 * correction recording, and model evaluation.
 */
@RestController
@RequestMapping("/api/supreme")
@PreAuthorize("hasRole('ADMIN')")
public class SupremeOrchestratorController {

  @Autowired private SupremeLearningOrchestrator orchestrator;

  @Autowired private SystemSuggestionService suggestionService;

  @PostMapping("/identify-hub")
  public Mono<Map<String, String>> identifyHub(@RequestBody Map<String, String> request) {
    String query = request.get("query");
    return Mono.just(orchestrator.identifyBestHub(query));
  }

  @PostMapping("/record-correction")
  public Mono<String> recordCorrection(@RequestBody Map<String, String> request) {
    String originalIntent = request.get("originalIntent");
    String correctedHub = request.get("correctedHub");
    String feedback = request.get("feedback");

    orchestrator.recordCorrection(originalIntent, correctedHub, feedback);
    return Mono.just("Correction recorded in the learning reservoir.");
  }

  @PostMapping("/evaluate-model")
  public Mono<String> evaluateModel(@RequestBody Map<String, String> request) {
    String sourceUrl = request.get("sourceUrl");
    String currentModelId = request.getOrDefault("currentModelId", "default-hub-model");

    return suggestionService.evaluateModelLink(sourceUrl, currentModelId);
  }
}
