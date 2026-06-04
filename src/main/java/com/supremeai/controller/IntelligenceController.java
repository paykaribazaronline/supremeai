package com.supremeai.controller;

import com.supremeai.repository.ProviderRepository;
import com.supremeai.response.ApiResponse;
import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.ContextualAIRankingService;
import com.supremeai.service.MultiAIVotingService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Controller for S3 (Autonomous Questioning) and S4 (10-AI Voting) systems Refactored to use
 * reactive WebFlux patterns.
 */
@RestController
@RequestMapping("/api/v2/intelligence")
public class IntelligenceController {

  private static final Logger logger = LoggerFactory.getLogger(IntelligenceController.class);

  @Autowired private AutonomousQuestioningEngine questioningEngine;

  @Autowired private ContextualAIRankingService rankingService;

  @Autowired private ApplicationContext applicationContext;

  @Autowired private ProviderRepository providerRepository;

  /** S9: Get AI Provider Rankings (Auto-Ranking) GET /api/v2/intelligence/rankings */
  @GetMapping("/rankings")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getRankings() {
    return Mono.fromCallable(() -> rankingService.getStatistics())
        .subscribeOn(Schedulers.boundedElastic())
        .map(stats -> ResponseEntity.ok(ApiResponse.ok(stats)))
        .onErrorResume(
            e ->
                Mono.just(
                    ResponseEntity.internalServerError()
                        .body(ApiResponse.error("Error fetching rankings: " + e.getMessage()))));
  }

  /** S3: Validate user input and get clarifying questions POST /api/v2/intelligence/validate */
  @PostMapping("/validate")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> validateInput(
      @RequestBody ValidationRequest request) {
    logger.info("S3: Validating input of type: {}", request.getRequestType());

    AutonomousQuestioningEngine.RequestType requestType =
        parseRequestType(request.getRequestType());

    return questioningEngine
        .validateAndQuestion(request.getPrompt(), requestType)
        .map(
            result -> {
              Map<String, Object> response = new HashMap<>();
              response.put("originalInput", result.getOriginalInput());
              response.put("requestType", result.getRequestType().toString());
              response.put("clarityScore", result.getClarityScore());
              response.put("isComplete", result.isComplete());
              response.put("clarifyingQuestions", result.getClarifyingQuestions());
              response.put("hasQuestions", result.hasQuestions());
              return ResponseEntity.ok(ApiResponse.ok(response));
            })
        .onErrorResume(
            e -> {
              logger.error("Error validating input", e);
              return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage())));
            });
  }

  /** S4: Execute 10-AI Voting System POST /api/v2/intelligence/vote */
  @PostMapping("/vote")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> executeVoting(
      @RequestBody VotingRequest request) {
    logger.info(
        "S4: Executing multi-AI voting for prompt: {}",
        request.getPrompt().substring(0, Math.min(50, request.getPrompt().length())));

    Mono<List<String>> providersMono;
    List<String> requestedModels = request.getModels();
    if (requestedModels != null && !requestedModels.isEmpty()) {
      providersMono = Mono.just(requestedModels);
    } else {
      providersMono =
          providerRepository
              .findAll()
              .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
              .map(p -> p.getName().toLowerCase())
              .collectList();
    }

    long timeoutMs = request.getTimeoutMs() > 0 ? request.getTimeoutMs() : 15000;

    return providersMono.flatMap(
        models ->
            applicationContext
                .getBean(MultiAIVotingService.class)
                .executeEnsembleVoting(request.getPrompt(), models, timeoutMs)
                .map(
                    result -> {
                      Map<String, Object> response = new HashMap<>();
                      response.put("prompt", result.getPrompt());
                      response.put("bestResponse", result.getBestResponse());
                      response.put("averageConfidence", result.getAverageConfidence());
                      response.put("verdict", result.getVerdict());
                      response.put("processingTimeMs", result.getProcessingTimeMs());
                      response.put("totalModelsUsed", result.getTotalModelsUsed());
                      response.put("totalModelsAvailable", models.size());
                      List<Map<String, Object>> votes =
                          result.getAllVotes().stream()
                              .map(
                                  vote -> {
                                    Map<String, Object> voteMap = new HashMap<>();
                                    voteMap.put("provider", vote.getProviderName());
                                    voteMap.put("confidence", vote.getConfidence());
                                    voteMap.put("timestamp", vote.getTimestamp());
                                    String resp = vote.getResponse();
                                    voteMap.put(
                                        "responsePreview",
                                        resp != null && resp.length() > 200
                                            ? resp.substring(0, 200) + "..."
                                            : resp);
                                    return voteMap;
                                  })
                              .collect(Collectors.toList());
                      response.put("votes", votes);
                      return ResponseEntity.ok(ApiResponse.ok(response));
                    })
                .onErrorResume(
                    e -> {
                      logger.error("Error executing multi-AI voting", e);
                      return Mono.just(
                          ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage())));
                    }));
  }

  /** Get list of available AI models for voting GET /api/v2/intelligence/models */
  @GetMapping("/models")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getAvailableModels() {
    return providerRepository
        .findAll()
        .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
        .map(p -> p.getName().toLowerCase())
        .collectList()
        .map(
            models -> {
              Map<String, Object> response = new HashMap<>();
              response.put("totalModels", models.size());
              response.put("models", models);
              response.put(
                  "description", "Multiple AI Models available for ensemble voting - dynamic list");
              return ResponseEntity.ok(ApiResponse.ok(response));
            });
  }

  /** Health check for intelligence systems GET /api/v2/intelligence/health */
  @GetMapping("/health")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> healthCheck() {
    return Mono.fromCallable(
            () -> {
              Map<String, Object> health = new HashMap<>();
              health.put("s3_autonomous_questioning", "operational");
              health.put("s4_ten_ai_voting", "operational");
              health.put("timestamp", System.currentTimeMillis());
              return health;
            })
        .map(health -> ResponseEntity.ok(ApiResponse.ok(health)));
  }

  private AutonomousQuestioningEngine.RequestType parseRequestType(String type) {
    if (type == null) return AutonomousQuestioningEngine.RequestType.GENERAL_AI;

    try {
      return AutonomousQuestioningEngine.RequestType.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException e) {
      return AutonomousQuestioningEngine.RequestType.GENERAL_AI;
    }
  }

  /** Request DTOs */
  public static class ValidationRequest {
    private String prompt;
    private String requestType;

    public String getPrompt() {
      return prompt;
    }

    public void setPrompt(String prompt) {
      this.prompt = prompt;
    }

    public String getRequestType() {
      return requestType;
    }

    public void setRequestType(String requestType) {
      this.requestType = requestType;
    }
  }

  public static class VotingRequest {
    private String prompt;
    private List<String> models;
    private long timeoutMs;

    public String getPrompt() {
      return prompt;
    }

    public void setPrompt(String prompt) {
      this.prompt = prompt;
    }

    public List<String> getModels() {
      return models;
    }

    public void setModels(List<String> models) {
      this.models = models;
    }

    public long getTimeoutMs() {
      return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
      this.timeoutMs = timeoutMs;
    }
  }
}
