package com.supremeai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import com.supremeai.repository.ProviderRepository;
import com.supremeai.response.ApiResponse;
import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.ContextualAIRankingService;
import com.supremeai.service.MultiAIVotingService;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class IntelligenceControllerTest {

  @Mock private AutonomousQuestioningEngine questioningEngine;

  @Mock private ContextualAIRankingService rankingService;

  @Mock private ApplicationContext applicationContext;

  @Mock private ProviderRepository providerRepository;

  @Mock private MultiAIVotingService votingService;

  private IntelligenceController intelligenceController;

  @BeforeEach
  void setUp() {
    intelligenceController = new IntelligenceController();
    setField(intelligenceController, "questioningEngine", questioningEngine);
    setField(intelligenceController, "rankingService", rankingService);
    setField(intelligenceController, "applicationContext", applicationContext);
    setField(intelligenceController, "providerRepository", providerRepository);
    lenient()
        .when(applicationContext.getBean(MultiAIVotingService.class))
        .thenReturn(votingService);
    lenient().when(providerRepository.findAll()).thenReturn(Flux.empty());
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      java.lang.reflect.Field field = IntelligenceController.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // ==================== getRankings Tests ====================

  @Test
  void getRankings_ReturnsRankingStats() {
    Map<String, Object> mockStats = new HashMap<>();
    mockStats.put("providers", List.of("gpt4", "claude", "gemini"));
    mockStats.put("totalModels", 3);

    when(rankingService.getStatistics()).thenReturn(mockStats);

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        intelligenceController.getRankings();

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().isSuccess());
              assertEquals(3, response.getBody().getData().get("totalModels"));
              return true;
            })
        .verifyComplete();

    verify(rankingService).getStatistics();
  }

  @Test
  void getRankings_ServiceError_ReturnsInternalServerError() {
    when(rankingService.getStatistics()).thenThrow(new RuntimeException("Service unavailable"));

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        intelligenceController.getRankings();

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
              assertFalse(response.getBody().isSuccess());
              return true;
            })
        .verifyComplete();
  }

  // ==================== validateInput Tests ====================

  @Test
  void validateInput_CompleteCodeRequest_ReturnsValidatedResult() {
    AutonomousQuestioningEngine.ValidationResult validationResult =
        new AutonomousQuestioningEngine.ValidationResult();
    validationResult.setOriginalInput("Write a Python function to sort a list");
    validationResult.setRequestType(AutonomousQuestioningEngine.RequestType.CODE_GENERATION);
    validationResult.setClarityScore(0.85);
    validationResult.setComplete(true);
    validationResult.setClarifyingQuestions(new ArrayList<>());

    when(questioningEngine.validateAndQuestion(anyString(), any()))
        .thenReturn(Mono.just(validationResult));

    IntelligenceController.ValidationRequest request =
        new IntelligenceController.ValidationRequest();
    request.setPrompt("Write a Python function to sort a list");
    request.setRequestType("CODE_GENERATION");

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        intelligenceController.validateInput(request);

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().isSuccess());
              Map<String, Object> data = response.getBody().getData();
              assertEquals("CODE_GENERATION", data.get("requestType"));
              assertTrue((Boolean) data.get("isComplete"));
              assertEquals(0.85, data.get("clarityScore"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void validateInput_IncompleteRequest_ReturnsClarificationQuestions() {
    AutonomousQuestioningEngine.ValidationResult validationResult =
        new AutonomousQuestioningEngine.ValidationResult();
    validationResult.setOriginalInput("Hi");
    validationResult.setRequestType(AutonomousQuestioningEngine.RequestType.GENERAL_AI);
    validationResult.setClarityScore(0.2);
    validationResult.setComplete(false);
    validationResult.setClarifyingQuestions(List.of("Could you elaborate on your request?"));

    when(questioningEngine.validateAndQuestion(anyString(), any()))
        .thenReturn(Mono.just(validationResult));

    IntelligenceController.ValidationRequest request =
        new IntelligenceController.ValidationRequest();
    request.setPrompt("Hi");
    request.setRequestType("GENERAL_AI");

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        intelligenceController.validateInput(request);

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Map<String, Object> data = response.getBody().getData();
              assertFalse((Boolean) data.get("isComplete"));
              assertTrue((Boolean) data.get("hasQuestions"));
              List<String> questions = (List<String>) data.get("clarifyingQuestions");
              assertEquals(1, questions.size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void validateInput_NullRequestType_DefaultsToGeneral() {
    AutonomousQuestioningEngine.ValidationResult validationResult =
        new AutonomousQuestioningEngine.ValidationResult();
    validationResult.setOriginalInput("Test input");
    validationResult.setRequestType(AutonomousQuestioningEngine.RequestType.GENERAL_AI);
    validationResult.setClarityScore(0.7);
    validationResult.setComplete(true);
    validationResult.setClarifyingQuestions(new ArrayList<>());

    when(questioningEngine.validateAndQuestion(anyString(), any()))
        .thenReturn(Mono.just(validationResult));

    IntelligenceController.ValidationRequest request =
        new IntelligenceController.ValidationRequest();
    request.setPrompt("Test input");
    request.setRequestType(null);

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        intelligenceController.validateInput(request);

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              return true;
            })
        .verifyComplete();

    // Verify that GENERAL_AI was used as default
    verify(questioningEngine)
        .validateAndQuestion(
            eq("Test input"), eq(AutonomousQuestioningEngine.RequestType.GENERAL_AI));
  }

  @Test
  void validateInput_Error_returnsBadRequest() {
    when(questioningEngine.validateAndQuestion(anyString(), any()))
        .thenReturn(Mono.error(new RuntimeException("Validation service error")));

    IntelligenceController.ValidationRequest request =
        new IntelligenceController.ValidationRequest();
    request.setPrompt("Test");
    request.setRequestType("CODE_GENERATION");

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        intelligenceController.validateInput(request);

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              assertFalse(response.getBody().isSuccess());
              return true;
            })
        .verifyComplete();
  }

  // ==================== executeVoting Tests ====================

  @Test
  void executeVoting_SuccessfulVote_ReturnsResult() {
    var vote1 =
        new com.supremeai.model.ProviderVote(
            "gpt4", "def sort_list(lst): return sorted(lst)", 0.95, System.currentTimeMillis());
    var vote2 =
        new com.supremeai.model.ProviderVote(
            "claude", "def sort_list(lst): return sorted(lst)", 0.89, System.currentTimeMillis());
    MultiAIVotingService.VotingResult votingResult =
        new MultiAIVotingService.VotingResult(
            "Write Python code",
            "def sort_list(lst): return sorted(lst)",
            List.of(vote1, vote2),
            0.92,
            "STRONG_CONSENSUS",
            3500L);

    when(votingService.executeEnsembleVoting(anyString(), anyList(), anyLong()))
        .thenReturn(Mono.just(votingResult));

    IntelligenceController.VotingRequest request = new IntelligenceController.VotingRequest();
    request.setPrompt("Write Python code");
    request.setModels(List.of("gpt4", "claude"));
    request.setTimeoutMs(15000L);

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        intelligenceController.executeVoting(request);

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Map<String, Object> data = response.getBody().getData();
              assertEquals("STRONG_CONSENSUS", data.get("verdict"));
              assertEquals(0.92, data.get("averageConfidence"));
              assertEquals("def sort_list(lst): return sorted(lst)", data.get("bestResponse"));
              assertEquals(2, data.get("totalModelsUsed"));
              return true;
            })
        .verifyComplete();
  }

  @Test
  void executeVoting_EmptyModelsList_UsesDefaults() {
    MultiAIVotingService.VotingResult votingResult =
        new MultiAIVotingService.VotingResult(
            "Test prompt", "Response", List.of(), 0.8, "MODERATE_CONSENSUS", 5000L);

    when(votingService.executeEnsembleVoting(anyString(), anyList(), anyLong()))
        .thenReturn(Mono.just(votingResult));

    IntelligenceController.VotingRequest request = new IntelligenceController.VotingRequest();
    request.setPrompt("Test prompt");
    request.setModels(null);

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        intelligenceController.executeVoting(request);

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              return true;
            })
        .verifyComplete();

    verify(votingService).executeEnsembleVoting(eq("Test prompt"), eq(List.of()), anyLong());
  }

  @Test
  void executeVoting_ZeroTimeout_UsesDefault() {
    MultiAIVotingService.VotingResult votingResult =
        new MultiAIVotingService.VotingResult(
            "Prompt", "Response", List.of(), 0.8, "MODERATE_CONSENSUS", 2000L);

    when(votingService.executeEnsembleVoting(anyString(), anyList(), anyLong()))
        .thenReturn(Mono.just(votingResult));

    IntelligenceController.VotingRequest request = new IntelligenceController.VotingRequest();
    request.setPrompt("Prompt");
    request.setTimeoutMs(0);

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        intelligenceController.executeVoting(request);

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              return true;
            })
        .verifyComplete();

    verify(votingService).executeEnsembleVoting(anyString(), anyList(), eq(15000L));
  }

  @Test
  void executeVoting_ServiceError_ReturnsBadRequest() {
    when(votingService.executeEnsembleVoting(anyString(), anyList(), anyLong()))
        .thenReturn(Mono.error(new RuntimeException("Voting service unavailable")));

    IntelligenceController.VotingRequest request = new IntelligenceController.VotingRequest();
    request.setPrompt("Test");
    request.setTimeoutMs(15000L);

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        intelligenceController.executeVoting(request);

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              return true;
            })
        .verifyComplete();
  }

  // ==================== getAvailableModels Tests ====================

  @Test
  void getAvailableModels_ReturnsModelList() {
    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        intelligenceController.getAvailableModels();

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Map<String, Object> data = response.getBody().getData();
              assertEquals(0, data.get("totalModels"));
              assertNotNull(data.get("models"));
              return true;
            })
        .verifyComplete();
  }

  // ==================== healthCheck Tests ====================

  @Test
  void healthCheck_ReturnsOperationalStatus() {
    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        intelligenceController.healthCheck();

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Map<String, Object> health = response.getBody().getData();
              assertEquals("operational", health.get("s3_autonomous_questioning"));
              assertEquals("operational", health.get("s4_ten_ai_voting"));
              assertNotNull(health.get("timestamp"));
              return true;
            })
        .verifyComplete();
  }

  // ==================== parseRequestType Tests (private method – use reflection)
  // ====================

  @Test
  void parseRequestType_ValidType_ReturnsEnum() throws Exception {
    java.lang.reflect.Method method =
        IntelligenceController.class.getDeclaredMethod("parseRequestType", String.class);
    method.setAccessible(true);

    AutonomousQuestioningEngine.RequestType result =
        (AutonomousQuestioningEngine.RequestType)
            method.invoke(intelligenceController, "CODE_GENERATION");

    assertEquals(AutonomousQuestioningEngine.RequestType.CODE_GENERATION, result);
  }

  @Test
  void parseRequestType_Null_ReturnsDefault() throws Exception {
    java.lang.reflect.Method method =
        IntelligenceController.class.getDeclaredMethod("parseRequestType", String.class);
    method.setAccessible(true);

    AutonomousQuestioningEngine.RequestType result =
        (AutonomousQuestioningEngine.RequestType)
            method.invoke(intelligenceController, (Object) null);

    assertEquals(AutonomousQuestioningEngine.RequestType.GENERAL_AI, result);
  }

  @Test
  void parseRequestType_Invalid_ReturnsDefault() throws Exception {
    java.lang.reflect.Method method =
        IntelligenceController.class.getDeclaredMethod("parseRequestType", String.class);
    method.setAccessible(true);

    AutonomousQuestioningEngine.RequestType result =
        (AutonomousQuestioningEngine.RequestType)
            method.invoke(intelligenceController, "INVALID_TYPE");

    assertEquals(AutonomousQuestioningEngine.RequestType.GENERAL_AI, result);
  }
}
