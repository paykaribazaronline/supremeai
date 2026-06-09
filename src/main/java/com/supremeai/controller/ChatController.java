package com.supremeai.controller;

import com.supremeai.dto.ChatRequest;
import com.supremeai.dto.FeedbackRequest;
import com.supremeai.model.ChatMessage;
import com.supremeai.repository.ChatHistoryRepository;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.EnhancedLearningService;
import com.supremeai.service.MultiAIVotingService;
import com.supremeai.service.NeuralChatService.NeuralResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

  private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

  @Autowired(required = false)
  private MultiAIVotingService consensusService;

  @Autowired private AutonomousQuestioningEngine questioningEngine;

  @Autowired private MultiAIVotingService votingService;

  @Autowired(required = false)
  private EnhancedLearningService enhancedLearningService;

  @Autowired private com.supremeai.service.ChatIntelligenceService intelligenceService;

  @Autowired private ChatHistoryRepository chatHistoryRepository;

  @Autowired private ProviderRepository providerRepository;

  @Autowired private com.supremeai.service.NeuralChatService neuralChatService;

  @Autowired private com.supremeai.service.DynamicSignatureRegistry signatureRegistry;

  private final CircuitBreaker aiCircuitBreaker;
  private final Retry aiRetry;

  public ChatController() {
    this.aiCircuitBreaker = CircuitBreaker.ofDefaults("aiVotingService");
    this.aiRetry = Retry.ofDefaults("aiVotingService");
  }

  @Deprecated(since = "2.0", forRemoval = true)
  @PostMapping("/send")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
  public Mono<ResponseEntity<Object>> sendMessage(@Valid @RequestBody ChatRequest request) {
    if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().build());
    }
    logger.warn("Deprecated /api/chat/send endpoint called. Using legacy chat flow.");
    return questioningEngine
        .validateAndQuestion(
            request.getMessage(), AutonomousQuestioningEngine.RequestType.GENERAL_AI)
        .flatMap(
            validation ->
                executeResponseForRequest(
                    request, request.getMessage(), validation, request.isSkipValidation()))
        .switchIfEmpty(
            Mono.defer(
                () ->
                    executeResponseForRequest(
                        request, request.getMessage(), null, request.isSkipValidation())));
  }

  private boolean isSkipValidationDirectAnswer(String message) {
    if (message == null) {
      return false;
    }
    String lower = message.toLowerCase();
    return lower.startsWith("what is")
        || lower.startsWith("what are")
        || lower.startsWith("explain")
        || lower.startsWith("define")
        || lower.startsWith("tell me about")
        || lower.startsWith("who is")
        || lower.startsWith("when is");
  }

  private Mono<ResponseEntity<Object>> executeResponseForRequest(
      ChatRequest request,
      String message,
      AutonomousQuestioningEngine.ValidationResult validation,
      boolean skipValidation) {

    if (!skipValidation && validation != null && !validation.isComplete()) {
      Map<String, Object> response = new HashMap<>();
      response.put("type", "CLARIFICATION_REQUIRED");
      response.put("clarity", validation.getClarityScore());
      response.put("questions", validation.getClarifyingQuestions());
      response.put("options", validation.getOptions());
      return Mono.just(ResponseEntity.ok((Object) response));
    }

    // Level-0: Direct Response Bypass RAG for Greetings
    if (validation != null
        && validation.getIntentType() == AutonomousQuestioningEngine.IntentType.GREETING) {
      Map<String, Object> response = new HashMap<>();
      response.put(
          "message",
          "হ্যালো! আমি SupremeAI. আজ আমি আপনার প্রজেক্ট বা কোডিংয়ের কাজে কীভাবে সহায়তা করতে পারি?");
      response.put("confidence", 1.0);
      response.put("sources", List.of("Local Memory"));
      response.put("tier", "LEVEL_0_BYPASS");
      response.put("pipeline", "direct_greeting");
      response.put("localMode", true);
      response.put("mode", "greeting");
      response.put("intent", "GREETING");
      return Mono.just(ResponseEntity.ok((Object) response));
    }

    boolean preferLocal =
        (validation != null
                && validation.getResponseStrategy()
                    == AutonomousQuestioningEngine.ResponseStrategy.DIRECT_ANSWER)
            || (skipValidation && isSkipValidationDirectAnswer(message));

    if (preferLocal) {
      return neuralChatService
          .generateIntelligentResponse(message)
          .flatMap(
              neuralResponse -> {
                if (shouldUseLocalResponse(neuralResponse)) {
                  return Mono.just(buildLocalResponse(neuralResponse, message));
                }
                logger.info(
                    "[ChatController] Local direct answer produced weak response; falling back to voting.");
                return executeVotingAndResponse(request, message);
              })
          .onErrorResume(
              err -> {
                logger.warn(
                    "[ChatController] Local direct answer path failed: {}. Falling back to voting.",
                    err.getMessage());
                return executeVotingAndResponse(request, message);
              });
    }

    return executeVotingAndResponse(request, message);
  }

  private boolean shouldUseLocalResponse(NeuralResponse neuralResponse) {
    if (neuralResponse == null) {
      return false;
    }
    String tier = neuralResponse.getTier();
    if (tier == null) {
      return false;
    }
    if (tier.equals("STUB_FALLBACK")) {
      return false;
    }
    return neuralResponse.getConfidence() >= 0.55;
  }

  private ResponseEntity<Object> buildLocalResponse(NeuralResponse neuralResponse, String message) {
    Map<String, Object> response = new HashMap<>();
    response.put("message", neuralResponse.getAnswer());
    response.put("confidence", neuralResponse.getConfidence());
    response.put("sources", neuralResponse.getSources());
    response.put("tier", neuralResponse.getTier());
    response.put("pipeline", neuralResponse.getPipeline());
    response.put("localMode", true);

    var intent = intelligenceService.classifyIntent(message);
    response.put("mode", intent.name().toLowerCase());
    response.put("intent", intent.name());

    return ResponseEntity.ok((Object) response);
  }

  private Mono<ResponseEntity<Object>> executeVotingAndResponse(
      ChatRequest request, String message) {
    return votingService
        .executeEnsembleVoting(message, null, 15000L)
        .flatMap(
            votingResult -> {
              if (votingResult == null || votingResult.getBestResponse() == null) {
                return neuralChatService
                    .generateIntelligentResponse(message)
                    .map(
                        neuralResponse -> {
                          Map<String, Object> response = new HashMap<>();
                          response.put("message", neuralResponse.getAnswer());
                          response.put("confidence", neuralResponse.getConfidence());
                          response.put("sources", neuralResponse.getSources());
                          response.put("tier", neuralResponse.getTier());
                          response.put("pipeline", neuralResponse.getPipeline());
                          response.put("localMode", true);

                          var intent = intelligenceService.classifyIntent(message);
                          response.put("mode", intent.name().toLowerCase());
                          response.put("intent", intent.name());

                          return ResponseEntity.ok((Object) response);
                        });
              }

              String bestResponse = votingResult.getBestResponse();
              Double confidence = votingResult.getAverageConfidence();

              Map<String, Object> response = new HashMap<>();
              response.put("message", bestResponse);
              response.put("verdict", votingResult.getVerdict());
              response.put("confidence", confidence);
              response.put("modelsUsed", votingResult.getTotalModelsUsed());
              response.put("processingTimeMs", votingResult.getProcessingTimeMs());
              response.put("timestamp", java.time.Instant.now().toString());

              var intent = intelligenceService.classifyIntent(message);
              response.put("mode", intent.name().toLowerCase());
              response.put("intent", intent.name());

              intelligenceService
                  .handleIntelligence(
                      request.getAgentId() != null ? request.getAgentId() : "default",
                      message,
                      intent,
                      "ADMIN",
                      confidence)
                  .subscribe(
                      result -> logger.debug("Intelligence handled successfully"),
                      error -> logger.error("Error handling intelligence: {}", error.getMessage()));

              if (enhancedLearningService != null) {
                enhancedLearningService
                    .learnFromNLPInteraction(
                        message,
                        bestResponse,
                        "voting_system",
                        confidence != null ? confidence : 0.5,
                        Map.of("modelsUsed", votingResult.getTotalModelsUsed()))
                    .subscribe(
                        saved -> logger.info("Successfully learned from NLP interaction"),
                        error ->
                            logger.error("Failed to capture NLP learning: {}", error.getMessage()));
              }

              return Mono.just(ResponseEntity.ok((Object) response));
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  logger.info(
                      "Voting service returned empty. Routing to intelligent offline fallback pipeline...");
                  if (neuralChatService == null) {
                    return Mono.just(
                        ResponseEntity.status(503)
                            .body((Object) Map.of("error", "AI services temporarily unavailable")));
                  }
                  return neuralChatService
                      .generateIntelligentResponse(message)
                      .map(
                          neuralResponse -> {
                            Map<String, Object> response = new HashMap<>();
                            response.put("message", neuralResponse.getAnswer());
                            response.put("confidence", neuralResponse.getConfidence());
                            response.put("sources", neuralResponse.getSources());
                            response.put("tier", neuralResponse.getTier());
                            response.put("pipeline", neuralResponse.getPipeline());
                            response.put("localMode", true);

                            var intent = intelligenceService.classifyIntent(message);
                            response.put("mode", intent.name().toLowerCase());
                            response.put("intent", intent.name());

                            return ResponseEntity.ok((Object) response);
                          })
                      .onErrorResume(
                          err -> {
                            logger.error(
                                "Intelligent fallback pipeline failed as well: {}",
                                err.getMessage());
                            return Mono.just(
                                ResponseEntity.status(503)
                                    .body(
                                        (Object)
                                            Map.of(
                                                "error", "AI services temporarily unavailable")));
                          });
                }))
        .onErrorResume(
            e -> {
              logger.error(
                  "Failed to get response via voting system. Routing to intelligent offline fallback pipeline...",
                  e);
              if (neuralChatService == null) {
                return Mono.just(
                    ResponseEntity.status(503)
                        .body((Object) Map.of("error", "AI services temporarily unavailable")));
              }
              return neuralChatService
                  .generateIntelligentResponse(message)
                  .map(
                      neuralResponse -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", neuralResponse.getAnswer());
                        response.put("confidence", neuralResponse.getConfidence());
                        response.put("sources", neuralResponse.getSources());
                        response.put("tier", neuralResponse.getTier());
                        response.put("pipeline", neuralResponse.getPipeline());
                        response.put("localMode", true);

                        var intent = intelligenceService.classifyIntent(message);
                        response.put("mode", intent.name().toLowerCase());
                        response.put("intent", intent.name());

                        return ResponseEntity.ok((Object) response);
                      })
                  .onErrorResume(
                      err -> {
                        logger.error(
                            "Intelligent fallback pipeline failed as well: {}", err.getMessage());
                        return Mono.just(
                            ResponseEntity.status(503)
                                .body(
                                    (Object)
                                        Map.of("error", "AI services temporarily unavailable")));
                      });
            });
  }

  @GetMapping("/history")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
  public Mono<ResponseEntity<Object>> getHistory(
      @RequestParam(required = false) String agent, @RequestParam(defaultValue = "50") int limit) {
    return Mono.just(
        ResponseEntity.ok(
            Map.of(
                "messages",
                new ArrayList<>(),
                "count",
                0,
                "agent",
                agent != null ? agent : "default")));
  }

  @PostMapping("/feedback")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
  public Mono<ResponseEntity<Object>> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
    String messageId = request.getMessageId();
    boolean helpful = request.isHelpful();
    String userMessage = request.getUserMessage();
    String aiResponse = request.getAiResponse();

    logger.info("Received feedback for message: {}, helpful: {}", messageId, helpful);

    if (enhancedLearningService != null
        && userMessage != null
        && aiResponse != null
        && messageId != null) {
      double qualityScore = helpful ? 1.0 : 0.3;
      enhancedLearningService
          .learnFromNLPInteraction(
              userMessage,
              aiResponse,
              "feedback_system",
              qualityScore,
              Map.of("messageId", messageId, "helpful", helpful))
          .subscribe(
              saved -> logger.info("Successfully captured learning from feedback"),
              error -> logger.error("Failed to capture feedback learning: {}", error.getMessage()));
    }

    return Mono.just(ResponseEntity.ok(Map.of("status", "received")));
  }

  private String detectMode(String message) {
    String lowerMsg = message.toLowerCase();
    if (lowerMsg.contains("architect")
        || lowerMsg.contains("design")
        || lowerMsg.contains("structure")) {
      return "architect";
    } else if (lowerMsg.contains("debug")
        || lowerMsg.contains("fix")
        || lowerMsg.contains("error")
        || lowerMsg.contains("issue")) {
      return "debug";
    } else if (lowerMsg.contains("review")
        || lowerMsg.contains("audit")
        || lowerMsg.contains("analyze")) {
      return "review";
    } else if (lowerMsg.contains("ask")
        || lowerMsg.contains("what")
        || lowerMsg.contains("how")
        || lowerMsg.contains("explain")) {
      return "ask";
    } else if (lowerMsg.contains("orchestrate")
        || lowerMsg.contains("manage")
        || lowerMsg.contains("coordinate")) {
      return "orchestrator";
    } else {
      return "code";
    }
  }

  @GetMapping("/health")
  public Mono<ResponseEntity<Object>> health() {
    return Mono.just(
        ResponseEntity.ok(
            Map.of(
                "status", "UP",
                "autonomous_questioning", "ACTIVE",
                "voting_system", "ACTIVE")));
  }

  @PostMapping("/message")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
  public Mono<ResponseEntity<Object>> handleChatMessage(@Valid @RequestBody ChatRequest request) {
    return processChatWithHistory(request);
  }

  @PostMapping(
      value = "/stream",
      produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER', 'GUEST')")
  public Flux<String> handleChatStream(@Valid @RequestBody ChatRequest request) {
    String sessionId = request.getSessionId();
    if (sessionId == null || sessionId.trim().isEmpty()) {
      sessionId = "default-session";
    }

    String finalSessionId = sessionId;
    String userMessage = request.getMessage();

    ChatMessage userMsg = new ChatMessage();
    userMsg.setId(java.util.UUID.randomUUID().toString());
    userMsg.setUserId(finalSessionId);
    userMsg.setContent(userMessage);
    userMsg.setRole("user");
    userMsg.setTimestamp(LocalDateTime.now());

    return chatHistoryRepository
        .save(userMsg)
        .thenMany(chatHistoryRepository.findByUserIdOrderByTimestampAsc(finalSessionId))
        .collectList()
        .flatMapMany(
            history -> {
              StringBuilder fullHistoryBuilder = new StringBuilder();
              for (ChatMessage pastMsg : history) {
                String role =
                    pastMsg.getRole() != null
                        ? pastMsg.getRole()
                        : (pastMsg.isAdmin() ? "admin" : "user");
                fullHistoryBuilder
                    .append(role.toUpperCase())
                    .append(": ")
                    .append(pastMsg.getContent())
                    .append("\n");
              }

              if (history.isEmpty()
                  || !history.get(history.size() - 1).getContent().equals(userMessage)) {
                fullHistoryBuilder.append("USER: ").append(userMessage).append("\n");
              }

              return Mono.just(fullHistoryBuilder.toString())
                  .flatMapMany(
                      summarizedHistory -> {
                        String contextualPrompt =
                            "You are SupremeAI, a highly intelligent coding and development assistant. Maintain a friendly and helpful tone.\n"
                                + "Below is the conversation context:\n\n"
                                + summarizedHistory
                                + "\n\nAI: ";

                        com.fasterxml.jackson.databind.ObjectMapper mapper =
                            new com.fasterxml.jackson.databind.ObjectMapper();
                        return votingService
                            .streamVotes(contextualPrompt, null, 15000L)
                            .map(
                                vote -> {
                                  try {
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("provider", vote.getProviderName());
                                    data.put("response", vote.getResponse());
                                    data.put("confidence", vote.getConfidence());
                                    return mapper.writeValueAsString(data);
                                  } catch (Exception e) {
                                    return "{\"error\":\"Serialization failed\"}";
                                  }
                                });
                      });
            });
  }

  private Mono<ResponseEntity<Object>> processChatWithHistory(ChatRequest request) {
    if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
      return Mono.just(
          ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty")));
    }

    String sessionId = request.getSessionId();
    if (sessionId == null || sessionId.trim().isEmpty()) {
      sessionId = "default-session";
    }

    String finalSessionId = sessionId;
    String userMessage = request.getMessage();

    ChatMessage userMsg = new ChatMessage();
    userMsg.setId(java.util.UUID.randomUUID().toString());
    userMsg.setUserId(finalSessionId);
    userMsg.setContent(userMessage);
    userMsg.setRole("user");
    userMsg.setTimestamp(LocalDateTime.now());

    return chatHistoryRepository
        .save(userMsg)
        .thenMany(chatHistoryRepository.findByUserIdOrderByTimestampAsc(finalSessionId))
        .collectList()
        .flatMap(
            history -> {
              StringBuilder fullHistoryBuilder = new StringBuilder();
              for (ChatMessage pastMsg : history) {
                String role =
                    pastMsg.getRole() != null
                        ? pastMsg.getRole()
                        : (pastMsg.isAdmin() ? "admin" : "user");
                fullHistoryBuilder
                    .append(role.toUpperCase())
                    .append(": ")
                    .append(pastMsg.getContent())
                    .append("\n");
              }

              if (history.isEmpty()
                  || !history.get(history.size() - 1).getContent().equals(userMessage)) {
                fullHistoryBuilder.append("USER: ").append(userMessage).append("\n");
              }

              return Mono.just(fullHistoryBuilder.toString())
                  .flatMap(
                      summarizedHistory -> {
                        String contextualPrompt =
                            "You are SupremeAI, a highly intelligent coding and development assistant. Maintain a friendly and helpful tone.\n"
                                + "Below is the conversation context:\n\n"
                                + summarizedHistory
                                + "\n\nAI: ";

                        return votingService
                            .executeEnsembleVoting(contextualPrompt, null, 15000L)
                            .flatMap(
                                votingResult -> {
                                  String rawResponse =
                                      (votingResult != null)
                                          ? votingResult.getBestResponse()
                                          : null;
                                  if (rawResponse == null) {
                                    return neuralChatService
                                        .generateIntelligentResponse(userMessage)
                                        .flatMap(
                                            neuralResponse -> {
                                              String bestResponse = neuralResponse.getAnswer();
                                              ChatMessage aiMsg = new ChatMessage();
                                              aiMsg.setId(java.util.UUID.randomUUID().toString());
                                              aiMsg.setUserId(finalSessionId);
                                              aiMsg.setContent(bestResponse);
                                              aiMsg.setRole("ai");
                                              aiMsg.setTimestamp(LocalDateTime.now());

                                              return chatHistoryRepository
                                                  .save(aiMsg)
                                                  .map(
                                                      savedAiMsg -> {
                                                        Map<String, Object> response =
                                                            new HashMap<>();
                                                        response.put("success", true);
                                                        response.put("message", "success");
                                                        response.put("response", bestResponse);
                                                        response.put("sessionId", finalSessionId);
                                                        response.put(
                                                            "timestamp",
                                                            java.time.Instant.now().toString());
                                                        response.put("localMode", true);
                                                        response.put(
                                                            "sources", neuralResponse.getSources());
                                                        response.put(
                                                            "tier", neuralResponse.getTier());
                                                        response.put(
                                                            "pipeline",
                                                            neuralResponse.getPipeline());
                                                        return ResponseEntity.ok((Object) response);
                                                      });
                                            });
                                  }

                                  ChatMessage aiMsg = new ChatMessage();
                                  aiMsg.setId(java.util.UUID.randomUUID().toString());
                                  aiMsg.setUserId(finalSessionId);
                                  aiMsg.setContent(rawResponse);
                                  aiMsg.setRole("ai");
                                  aiMsg.setTimestamp(LocalDateTime.now());

                                  return chatHistoryRepository
                                      .save(aiMsg)
                                      .map(
                                          savedAiMsg -> {
                                            Map<String, Object> response = new HashMap<>();
                                            response.put("success", true);
                                            response.put("message", "success");
                                            response.put("response", rawResponse);
                                            response.put("sessionId", finalSessionId);
                                            response.put(
                                                "timestamp", java.time.Instant.now().toString());
                                            response.put("localMode", true);
                                            return ResponseEntity.ok((Object) response);
                                          });
                                });
                      })
                  .onErrorResume(
                      e -> {
                        logger.error(
                            "Failed to process chat with history for session: {}",
                            finalSessionId,
                            e);

                        return neuralChatService
                            .generateIntelligentResponse(userMessage)
                            .flatMap(
                                neuralResponse -> {
                                  String localResponse = neuralResponse.getAnswer();

                                  ChatMessage aiMsg = new ChatMessage();
                                  aiMsg.setId(java.util.UUID.randomUUID().toString());
                                  aiMsg.setUserId(finalSessionId);
                                  aiMsg.setContent(localResponse);
                                  aiMsg.setRole("ai");
                                  aiMsg.setTimestamp(LocalDateTime.now());

                                  return chatHistoryRepository
                                      .save(aiMsg)
                                      .map(
                                          savedAiMsg -> {
                                            Map<String, Object> response = new HashMap<>();
                                            response.put("success", true);
                                            response.put(
                                                "message",
                                                "AI temporarily unavailable, using intelligent offline fallback response");
                                            response.put("response", localResponse);
                                            response.put("sessionId", finalSessionId);
                                            response.put(
                                                "timestamp", java.time.Instant.now().toString());
                                            response.put("localMode", true);
                                            response.put("sources", neuralResponse.getSources());
                                            response.put("tier", neuralResponse.getTier());
                                            response.put("pipeline", neuralResponse.getPipeline());
                                            return ResponseEntity.ok((Object) response);
                                          });
                                });
                      });
            });
  }

  private String generateLocalFallbackResponse(String prompt) {
    String p = prompt.toLowerCase();

    Set<String> greetingTriggers = signatureRegistry.getSignatures("GREETING_PATTERNS");
    if (greetingTriggers.stream().anyMatch(trigger -> p.contains(trigger.toLowerCase()))) {
      return signatureRegistry.getSignatures("RESPONSE_TEMPLATES").stream()
          .filter(t -> t.startsWith("GREETING_RESPONSE:"))
          .map(t -> t.substring("GREETING_RESPONSE:".length()))
          .findFirst()
          .orElseGet(() -> getLocalizedGreeting());
    }

    Set<String> helpTriggers = signatureRegistry.getSignatures("HELP_PATTERNS");
    if (helpTriggers.stream().anyMatch(trigger -> p.contains(trigger.toLowerCase()))) {
      return signatureRegistry.getSignatures("RESPONSE_TEMPLATES").stream()
          .filter(t -> t.startsWith("HELP_RESPONSE:"))
          .map(t -> t.substring("HELP_RESPONSE:".length()))
          .findFirst()
          .orElse("I can help you with your coding needs.");
    }

    Set<String> techPatterns = signatureRegistry.getSignatures("TECHNOLOGY_PATTERNS");
    Optional<String> techMatch =
        techPatterns.stream()
            .filter(pattern -> pattern.contains(":"))
            .filter(pattern -> p.contains(pattern.split(":")[0].toLowerCase()))
            .findFirst();

    if (techMatch.isPresent()) {
      String responseTemplate = techMatch.get().split(":")[1];
      return signatureRegistry.getSignatures("RESPONSE_TEMPLATES").stream()
          .filter(t -> t.startsWith(responseTemplate + ":"))
          .map(t -> t.substring(responseTemplate.length() + 1))
          .findFirst()
          .orElse("I can help with that technology.");
    }

    String defaultTemplate =
        signatureRegistry.getSignatures("RESPONSE_TEMPLATES").stream()
            .filter(t -> t.startsWith("DEFAULT_RESPONSE:"))
            .map(t -> t.substring("DEFAULT_RESPONSE:".length()))
            .findFirst()
            .orElse(getDefaultCapabilitiesMessage());

    return String.format(defaultTemplate, prompt);
  }

  private String getLocalizedGreeting() {
    return signatureRegistry.getSignatures("GREETING_RESPONSES").stream()
        .findFirst()
        .orElseGet(() -> "Hello! I'm SupremeAI. How can I help you today?");
  }

  private String getDefaultCapabilitiesMessage() {
    Set<String> topics = signatureRegistry.getSignatures("KNOWN_TOPICS");
    Set<String> categories = signatureRegistry.getSignatures("TOPIC_CATEGORIES");

    StringBuilder sb = new StringBuilder();
    sb.append("I can help you with many topics!\n\n");

    for (String category : categories) {
      Set<String> categoryTopics = signatureRegistry.getSignatures(category + "_TOPICS");
      if (!categoryTopics.isEmpty()) {
        sb.append(String.format("**%s:** %s\n", category, String.join(", ", categoryTopics)));
      }
    }

    sb.append("\nYour question: \"").append("...").append("\"\n\n");
    sb.append(
        signatureRegistry.getSignatures("FOOTER_MESSAGES").stream()
            .findFirst()
            .orElse("Please try rephrasing for more specific help!"));

    return sb.toString();
  }
}
