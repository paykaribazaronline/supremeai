package com.supremeai.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.supremeai.dto.ChatRequest;
import com.supremeai.dto.FeedbackRequest;
import com.supremeai.model.APIProvider;
import com.supremeai.model.ChatMessage;
import com.supremeai.repository.ChatHistoryRepository;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.ChatIntelligenceService;
import com.supremeai.service.EnhancedLearningService;
import com.supremeai.service.MultiAIVotingService;
import com.supremeai.service.NeuralChatService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
class ChatControllerTest {

  @Mock private MultiAIVotingService consensusService;

  @Mock private AutonomousQuestioningEngine questioningEngine;

  @Mock private MultiAIVotingService votingService;

  @Mock private EnhancedLearningService enhancedLearningService;

  @Mock private ChatIntelligenceService intelligenceService;

  @Mock private ChatHistoryRepository chatHistoryRepository;

  @Mock private ProviderRepository providerRepository;

  @Mock private NeuralChatService neuralChatService;

  @org.mockito.InjectMocks
  private ChatController chatController;

  @BeforeEach
  void setUp() {

    lenient().when(providerRepository.findByStatus(anyString())).thenReturn(Flux.empty());
    lenient().when(chatHistoryRepository.save(any(ChatMessage.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    lenient().when(chatHistoryRepository.findByUserIdOrderByTimestampAsc(anyString()))
        .thenReturn(Flux.empty());
    lenient().when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
        .thenReturn(Mono.empty());
    lenient().when(consensusService.executeEnsembleVoting(anyString(), any(), anyLong()))
        .thenReturn(Mono.empty());

    NeuralChatService.NeuralResponse defaultResponse =
        new NeuralChatService.NeuralResponse(
            "AI temporarily unavailable, using intelligent offline fallback response",
            List.of(),
            0.0,
            "CORE_ONLY",
            "core_knowledge");
    lenient().when(neuralChatService.generateIntelligentResponse(anyString()))
        .thenReturn(Mono.just(defaultResponse));
  }

  private ResponseEntity<Object> blockResponse(Mono<ResponseEntity<Object>> mono) {
    ResponseEntity<Object> response = mono.block();
    assertTrue(response.getStatusCode().is2xxSuccessful());
    return response;
  }

  private void assertBadRequestFromMono(Mono<ResponseEntity<Object>> mono) {
    StepVerifier.create(mono)
        .expectNextMatches(r -> r.getStatusCode().is4xxClientError())
        .verifyComplete();
  }

  private void assertServerErrorFromMono(Mono<ResponseEntity<Object>> mono) {
    StepVerifier.create(mono)
        .expectNextMatches(r -> r.getStatusCode().is5xxServerError())
        .verifyComplete();
  }

  @Test
  void sendMessage_EmptyMessage_ReturnsBadRequest() {
    ChatRequest request = new ChatRequest();
    request.setMessage("");
    request.setSkipValidation(false);

    assertBadRequestFromMono(chatController.sendMessage(request));
  }

  @Test
  void sendMessage_NullMessage_ReturnsBadRequest() {
    ChatRequest request = new ChatRequest();
    request.setMessage(null);
    request.setSkipValidation(false);

    assertBadRequestFromMono(chatController.sendMessage(request));
  }

  @Test
  void sendMessage_IncompleteInput_ReturnsClarification() {
    ChatRequest request = new ChatRequest();
    request.setMessage("hi");
    request.setSkipValidation(false);

    AutonomousQuestioningEngine.ValidationResult validationResult =
        new AutonomousQuestioningEngine.ValidationResult();
    validationResult.setOriginalInput("hi");
    validationResult.setClarityScore(0.3);
    validationResult.setComplete(false);
    validationResult.setClarifyingQuestions(List.of("Could you elaborate?"));
    validationResult.setRequestType(AutonomousQuestioningEngine.RequestType.GENERAL_AI);

    when(questioningEngine.validateAndQuestion(anyString(), any()))
        .thenReturn(Mono.just(validationResult));

    ResponseEntity<Object> response = blockResponse(chatController.sendMessage(request));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals("CLARIFICATION_REQUIRED", body.get("type"));
    assertFalse(((List<?>) body.get("questions")).isEmpty());
  }

  @Test
  void sendMessage_SuccessfulVoting_ReturnsResponse() {
    ChatRequest request = new ChatRequest();
    request.setMessage("How do Java streams work?");
    request.setSkipValidation(true);

    AutonomousQuestioningEngine.ValidationResult validationResult =
        new AutonomousQuestioningEngine.ValidationResult();
    validationResult.setComplete(true);

    MultiAIVotingService.VotingResult votingResult =
        new MultiAIVotingService.VotingResult(
            "Explain Java streams",
            "Java streams are...",
            List.of(),
            0.85,
            "STRONG_CONSENSUS",
            1500L);

    when(questioningEngine.validateAndQuestion(anyString(), any()))
        .thenReturn(Mono.just(validationResult));
    when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
        .thenReturn(Mono.just(votingResult));
    when(intelligenceService.classifyIntent(anyString()))
        .thenReturn(ChatIntelligenceService.Intent.CASUAL);
    when(intelligenceService.handleIntelligence(
            anyString(), anyString(), any(), anyString(), anyDouble()))
        .thenReturn(Mono.empty());
    when(enhancedLearningService.learnFromNLPInteraction(
            anyString(), anyString(), anyString(), anyDouble(), anyMap()))
        .thenReturn(Mono.empty());

    ResponseEntity<Object> response = blockResponse(chatController.sendMessage(request));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals("Java streams are...", body.get("message"));
    assertEquals("STRONG_CONSENSUS", body.get("verdict"));
    assertEquals(0.85, body.get("confidence"));
  }

  @Test
  void sendMessage_DirectAnswerPrefersLocalKnowledgeAndSkipsVoting() {
    ChatRequest request = new ChatRequest();
    request.setMessage("What is llm?");
    request.setSkipValidation(false);

    AutonomousQuestioningEngine.ValidationResult validationResult =
        new AutonomousQuestioningEngine.ValidationResult();
    validationResult.setComplete(true);
    validationResult.setResponseStrategy(
        AutonomousQuestioningEngine.ResponseStrategy.DIRECT_ANSWER);

    NeuralChatService.NeuralResponse neuralResponse =
        new NeuralChatService.NeuralResponse(
            "LLM is a large language model.",
            List.of("Core Knowledge"),
            0.87,
            "CORE_ONLY",
            "core_knowledge");

    when(questioningEngine.validateAndQuestion(anyString(), any()))
        .thenReturn(Mono.just(validationResult));
    when(neuralChatService.generateIntelligentResponse(anyString()))
        .thenReturn(Mono.just(neuralResponse));
    when(intelligenceService.classifyIntent(anyString()))
        .thenReturn(ChatIntelligenceService.Intent.INFO_COLLECTION);

    ResponseEntity<Object> response = blockResponse(chatController.sendMessage(request));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals("LLM is a large language model.", body.get("message"));
    assertTrue((Boolean) body.get("localMode"));
    assertEquals("core_knowledge", body.get("pipeline"));

    verify(votingService, never()).executeEnsembleVoting(anyString(), any(), anyLong());
  }

  @Test
  void sendMessage_SkipValidationDirectAnswerStillUsesLocalKnowledge() {
    ChatRequest request = new ChatRequest();
    request.setMessage("What is llm?");
    request.setSkipValidation(true);

    AutonomousQuestioningEngine.ValidationResult validationResult =
        new AutonomousQuestioningEngine.ValidationResult();
    validationResult.setComplete(true);
    validationResult.setResponseStrategy(
        AutonomousQuestioningEngine.ResponseStrategy.DIRECT_ANSWER);

    NeuralChatService.NeuralResponse neuralResponse =
        new NeuralChatService.NeuralResponse(
            "LLM is a large language model.",
            List.of("Core Knowledge"),
            0.87,
            "CORE_ONLY",
            "core_knowledge");

    when(questioningEngine.validateAndQuestion(anyString(), any()))
        .thenReturn(Mono.just(validationResult));
    when(neuralChatService.generateIntelligentResponse(anyString()))
        .thenReturn(Mono.just(neuralResponse));
    when(intelligenceService.classifyIntent(anyString()))
        .thenReturn(ChatIntelligenceService.Intent.INFO_COLLECTION);

    ResponseEntity<Object> response = blockResponse(chatController.sendMessage(request));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals("LLM is a large language model.", body.get("message"));
    assertTrue((Boolean) body.get("localMode"));
    verify(votingService, never()).executeEnsembleVoting(anyString(), any(), anyLong());
  }

  @Test
  void handleChatMessage_ReturnsValidResponse() {
    ChatRequest request = new ChatRequest();
    request.setMessage("Explain CI");
    request.setSessionId("session-123");

    when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
        .thenReturn(
            Mono.just(
                new MultiAIVotingService.VotingResult(
                    "Explain CI",
                    "Explain CI",
                    List.of(),
                    0.92,
                    "STRONG_CONSENSUS",
                    1000L)));

    ResponseEntity<Object> messageResponse = blockResponse(chatController.handleChatMessage(request));

    assertNotNull(messageResponse);
    assertEquals(HttpStatus.OK, messageResponse.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> messageBody = (Map<String, Object>) messageResponse.getBody();
    assertEquals("success", messageBody.get("message"));
    assertEquals("Explain CI", messageBody.get("response"));
    assertEquals("session-123", messageBody.get("sessionId"));
    assertTrue((Boolean) messageBody.get("localMode"));
  }

  @Test
  void sendMessage_VotingFails_ConsensusFallback() {
    ChatRequest request = new ChatRequest();
    request.setMessage("What is Python?");
    request.setSkipValidation(true);

    AutonomousQuestioningEngine.ValidationResult validationResult =
        new AutonomousQuestioningEngine.ValidationResult();
    validationResult.setComplete(true);

    APIProvider provider = new APIProvider();
    provider.setName("openai");
    provider.setStatus("active");
    lenient().when(providerRepository.findByStatus("active")).thenReturn(Flux.just(provider));

    lenient().when(questioningEngine.validateAndQuestion(anyString(), any()))
        .thenReturn(Mono.just(validationResult));
    lenient().when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
        .thenReturn(Mono.error(new RuntimeException("All providers failed")));

    NeuralChatService.NeuralResponse neuralResponse =
        new NeuralChatService.NeuralResponse(
            "Python is a programming language", List.of("Core Knowledge"), 0.8, "CORE_ONLY", "core_knowledge");
    lenient().when(neuralChatService.generateIntelligentResponse(anyString()))
        .thenReturn(Mono.just(neuralResponse));
    lenient().when(intelligenceService.classifyIntent(anyString()))
        .thenReturn(ChatIntelligenceService.Intent.CASUAL);

    ResponseEntity<Object> response = blockResponse(chatController.sendMessage(request));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertTrue((Boolean) body.get("localMode"));
    assertEquals("Python is a programming language", body.get("message"));
  }

  @Test
  void sendMessage_AllServicesFail_ReturnsServiceUnavailable() {
    ChatRequest request = new ChatRequest();
    request.setMessage("Hello");
    request.setSkipValidation(true);

    AutonomousQuestioningEngine.ValidationResult validationResult =
        new AutonomousQuestioningEngine.ValidationResult();
    validationResult.setComplete(true);

    APIProvider provider = new APIProvider();
    provider.setName("openai");
    provider.setStatus("active");
    lenient().when(providerRepository.findByStatus("active")).thenReturn(Flux.just(provider));

    when(questioningEngine.validateAndQuestion(anyString(), any()))
        .thenReturn(Mono.just(validationResult));
    when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
        .thenReturn(Mono.error(new RuntimeException("All providers failed")));
    when(neuralChatService.generateIntelligentResponse(anyString()))
        .thenReturn(Mono.error(new RuntimeException("NeuralChatService also failed")));

    assertServerErrorFromMono(chatController.sendMessage(request));
  }

  @Test
  void getHistory_ReturnsEmptyHistory() {
    ResponseEntity<Object> response = blockResponse(chatController.getHistory("default", 50));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertTrue(((List<?>) body.get("messages")).isEmpty());
    assertEquals("default", body.get("agent"));
  }

  @Test
  void getHistory_WithAgent_ReturnsAgentHistory() {
    ResponseEntity<Object> response = blockResponse(chatController.getHistory("agent-123", 10));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("agent-123", ((Map<String, Object>) response.getBody()).get("agent"));
  }

  @Test
  void submitFeedback_PositiveFeedback_NoError() {
    FeedbackRequest request = new FeedbackRequest();
    request.setMessageId("msg-123");
    request.setUserMessage("What is Java?");
    request.setAiResponse("Java is a programming language");
    request.setHelpful(true);

    lenient().when(enhancedLearningService.learnFromNLPInteraction(
            anyString(), anyString(), anyString(), anyDouble(), anyMap()))
        .thenReturn(Mono.empty());

    ResponseEntity<Object> response = blockResponse(chatController.submitFeedback(request));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals("received", body.get("status"));
  }

  @Test
  void submitFeedback_NegativeFeedback_LearnsWithLowScore() {
    FeedbackRequest request = new FeedbackRequest();
    request.setMessageId("msg-456");
    request.setUserMessage("How to sort?");
    request.setAiResponse("Bad answer");
    request.setHelpful(false);

    lenient().when(enhancedLearningService.learnFromNLPInteraction(
            anyString(), anyString(), anyString(), anyDouble(), anyMap()))
        .thenReturn(Mono.empty());

    ResponseEntity<Object> response = blockResponse(chatController.submitFeedback(request));

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void submitFeedback_NullMessageId_StillAccepts() {
    FeedbackRequest request = new FeedbackRequest();
    request.setMessageId(null);
    request.setUserMessage("Test");
    request.setAiResponse("Response");
    request.setHelpful(true);

    lenient().when(enhancedLearningService.learnFromNLPInteraction(
            anyString(), anyString(), anyString(), anyDouble(), anyMap()))
        .thenReturn(Mono.empty());

    ResponseEntity<Object> response = blockResponse(chatController.submitFeedback(request));

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void health_ReturnsOperationalStatus() {
    ResponseEntity<Object> response = blockResponse(chatController.health());

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertEquals("UP", body.get("status"));
    assertEquals("ACTIVE", body.get("autonomous_questioning"));
    assertEquals("ACTIVE", body.get("voting_system"));
  }

  @Test
  void detectorMode_ArchitectKeywords_ReturnsArchitect() {
    assertEquals("architect", invokeDetectMode(chatController, "Design the architecture for a microservice"));
  }

  @Test
  void detectorMode_DebugKeywords_ReturnsDebug() {
    assertEquals("debug", invokeDetectMode(chatController, "I have an error in my code, help me fix it"));
  }

  @Test
  void detectorMode_ReviewKeywords_ReturnsReview() {
    assertEquals("review", invokeDetectMode(chatController, "Can you review my code?"));
  }

  @Test
  void detectorMode_AskKeywords_ReturnsAsk() {
    assertEquals("ask", invokeDetectMode(chatController, "What is dependency injection?"));
  }

  @Test
  void detectorMode_OrchestrateKeywords_ReturnsOrchestrator() {
    assertEquals("orchestrator", invokeDetectMode(chatController, "Orchestrate the deployment pipeline"));
  }

  @Test
  void detectorMode_Default_ReturnsCode() {
    assertEquals("code", invokeDetectMode(chatController, "Write a REST API"));
  }

  private static String invokeDetectMode(ChatController controller, String message) {
    try {
      java.lang.reflect.Method method =
          ChatController.class.getDeclaredMethod("detectMode", String.class);
      method.setAccessible(true);
      return (String) method.invoke(controller, message);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
