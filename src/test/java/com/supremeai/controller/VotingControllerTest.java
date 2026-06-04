package com.supremeai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.supremeai.agentorchestration.VotingDecision;
import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ConsensusVote;
import com.supremeai.service.MultiAIVotingService;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class VotingControllerTest {

  @Mock private MultiAIVotingService votingService;

  @Mock private com.supremeai.agentorchestration.RequirementAnalyzerAI requirementAnalyzer;

  @Mock private com.supremeai.provider.AIProviderFactory providerFactory;

  private VotingController votingController;

  @BeforeEach
  void setUp() {
    votingController = new VotingController();
    setField(votingController, "votingService", votingService);
    setField(votingController, "requirementAnalyzer", requirementAnalyzer);
    setField(votingController, "providerFactory", providerFactory);
    lenient()
        .when(providerFactory.getAvailableProviderIds())
        .thenReturn(List.of("groq", "openai", "anthropic", "ollama"));
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      java.lang.reflect.Field field = VotingController.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // ==================== analyzeRequirement Tests ====================

  @Test
  void analyzeRequirement_ValidRequirement_ReturnsQuestions() {
    when(requirementAnalyzer.generateClarifyingQuestions(anyString()))
        .thenReturn(List.of("What technology?", "What scale?"));

    ResponseEntity<List<String>> result =
        votingController.analyzeRequirement(Map.of("requirement", "Build an app"));

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(2, result.getBody().size());
    verify(requirementAnalyzer).generateClarifyingQuestions("Build an app");
  }

  @Test
  void analyzeRequirement_EmptyRequirement_ReturnsEmptyList() {
    when(requirementAnalyzer.generateClarifyingQuestions("")).thenReturn(Collections.emptyList());

    ResponseEntity<List<String>> result =
        votingController.analyzeRequirement(Map.of("requirement", ""));

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertTrue(result.getBody().isEmpty());
  }

  @Test
  void analyzeRequirement_NullRequirement_ReturnsQuestionsForNull() {
    when(requirementAnalyzer.generateClarifyingQuestions(null))
        .thenReturn(List.of("Please clarify your requirements"));

    Map<String, String> request = new HashMap<>();
    request.put("requirement", null);
    ResponseEntity<List<String>> result = votingController.analyzeRequirement(request);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertFalse(result.getBody().isEmpty());
  }

  // ==================== conductVote Tests ====================
  // Note: conductVote returns Mono<ResponseEntity<VotingDecision>> — use StepVerifier

  @Test
  void conductVote_ValidQuestion_ReturnsDecision() {
    VotingDecision mockDecision = new VotingDecision();
    mockDecision.setAiConsensus("Use microservices");
    mockDecision.setConfidence(0.9);

    when(votingService.conductDecisionVote(anyString(), anyString()))
        .thenReturn(Mono.just(mockDecision));

    Mono<ResponseEntity<VotingDecision>> result =
        votingController.conductVote(Map.of("question", "Which architecture?"));

    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
              assertNotNull(response.getBody().getAiConsensus());
            })
        .verifyComplete();

    verify(votingService).conductDecisionVote(eq("Which architecture?"), eq(""));
  }

  @Test
  void conductVote_WithContext_ReturnsDecisionForContext() {
    VotingDecision mockDecision = new VotingDecision();
    mockDecision.setAiConsensus("Use serverless");
    mockDecision.setConfidence(0.85);

    when(votingService.conductDecisionVote(anyString(), anyString()))
        .thenReturn(Mono.just(mockDecision));

    Mono<ResponseEntity<VotingDecision>> result =
        votingController.conductVote(
            Map.of("question", "Architecture?", "context", "High traffic"));

    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody().getAiConsensus());
            })
        .verifyComplete();
  }

  @Test
  void conductVote_EmptyContext_ReturnsDecision() {
    VotingDecision mockDecision = new VotingDecision();
    mockDecision.setAiConsensus("Use monolith");

    when(votingService.conductDecisionVote(anyString(), eq("")))
        .thenReturn(Mono.just(mockDecision));

    Mono<ResponseEntity<VotingDecision>> result =
        votingController.conductVote(Map.of("question", "Which architecture?"));

    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertNotNull(response.getBody());
            })
        .verifyComplete();
  }

  @Test
  void conductVote_NullQuestion_ReturnsBadRequest() {
    Mono<ResponseEntity<VotingDecision>> result = votingController.conductVote(Map.of());

    StepVerifier.create(result)
        .assertNext(response -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()))
        .verifyComplete();
  }

  // ==================== voteOnQuestion Tests ====================
  // Note: voteOnQuestion returns ResponseEntity<Object> synchronously — NOT Mono

  @Test
  void voteOnQuestion_ValidQuestion_ReturnsConsensus() {
    ConsensusResult mockResult =
        new ConsensusResult(
            "What is Docker?",
            "Docker is containerization",
            List.of(),
            0.85,
            "CONSENSUS_STRONG",
            80.0,
            1000L,
            0.82);

    when(votingService.askConsensus(anyString(), anyList(), eq(10000L)))
        .thenReturn(Mono.just(mockResult));

    Mono<ResponseEntity<Object>> result =
        votingController.voteOnQuestion(Map.of("question", "What is Docker?"));

    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Map<String, Object> body = (Map<String, Object>) response.getBody();
              assertEquals("Docker is containerization", body.get("consensus"));
              assertEquals(0.85, body.get("confidence"));
            })
        .verifyComplete();
  }

  @Test
  void voteOnQuestion_EmptyQuestion_ReturnsBadRequest() {
    Mono<ResponseEntity<Object>> result = votingController.voteOnQuestion(Map.of("question", ""));

    StepVerifier.create(result)
        .assertNext(response -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()))
        .verifyComplete();
  }

  @Test
  void voteOnQuestion_NullQuestion_ReturnsBadRequest() {
    Mono<ResponseEntity<Object>> result = votingController.voteOnQuestion(Map.of());

    StepVerifier.create(result)
        .assertNext(response -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()))
        .verifyComplete();
  }

  @Test
  void voteOnQuestion_CustomProviders_ReturnsConsensus() {
    ConsensusResult mockResult =
        new ConsensusResult(
            "Question", "Answer", List.of(), 0.8, "CONSENSUS_MODERATE", 60.0, 500L, 0.75);

    when(votingService.askConsensus(anyString(), anyList(), anyLong()))
        .thenReturn(Mono.just(mockResult));

    Mono<ResponseEntity<Object>> result =
        votingController.voteOnQuestion(
            Map.of("question", "Test question", "providers", List.of("groq", "openai")));

    StepVerifier.create(result)
        .assertNext(response -> assertEquals(HttpStatus.OK, response.getStatusCode()))
        .verifyComplete();
  }

  // ==================== getHistory Tests ====================
  // Note: getHistory returns ResponseEntity<Object> synchronously — NOT Mono

  @Test
  void getHistory_WithLimit_ReturnsHistory() {
    ConsensusVote vote1 = new ConsensusVote();
    vote1.setQuestion("Q1");
    vote1.setConsensusAnswer("A1");
    vote1.setConsensusPercentage(0.5);
    vote1.setConsensusStrength("STRONG");
    ConsensusVote vote2 = new ConsensusVote();
    vote2.setQuestion("Q2");
    vote2.setConsensusAnswer("A2");
    vote2.setConsensusPercentage(0.5);
    vote2.setConsensusStrength("MODERATE");

    when(votingService.getConsensusHistory(5)).thenReturn(Flux.just(vote1, vote2));

    Mono<ResponseEntity<Object>> result = votingController.getHistory(5);

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Map<String, Object> body = (Map<String, Object>) response.getBody();
              assertEquals(2, ((java.util.List) body.get("history")).size());
              return true;
            })
        .verifyComplete();
  }

  @Test
  void getHistory_DefaultLimit_ReturnsHistory() {
    when(votingService.getConsensusHistory(20)).thenReturn(Flux.empty());

    Mono<ResponseEntity<Object>> result = votingController.getHistory(20);

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              Map<String, Object> body = (Map<String, Object>) response.getBody();
              assertEquals(0, ((java.util.List) body.get("history")).size());
              return true;
            })
        .verifyComplete();
  }

  // ==================== health Endpoint Test ====================

  @Test
  void health_ReturnsUpStatus() {
    Mono<ResponseEntity<Object>> result = votingController.health();

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Map<String, Object> body = (Map<String, Object>) response.getBody();
              assertEquals("UP", body.get("status"));
              assertEquals("MultiAIVotingService", body.get("service"));
              assertNotNull(body.get("providers"));
              return true;
            })
        .verifyComplete();
  }

  // ==================== compareStrategies Tests ====================
  // Note: compareStrategies returns Mono<ResponseEntity<Object>>

  @Test
  void compareStrategies_ValidRequest_ReturnsComparison() {
    ConsensusResult mockResult =
        new ConsensusResult(
            "Q", "Best answer", List.of(), 0.85, "CONSENSUS_STRONG", 80.0, 500L, 0.8);

    when(votingService.askConsensus(anyString(), anyList(), eq(15000L)))
        .thenReturn(Mono.just(mockResult));

    Mono<ResponseEntity<Object>> result =
        votingController.compareStrategies(
            Map.of("question", "Best approach?", "providers", List.of("groq", "openai", "claude")));

    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Map<String, Object> body =
                  (Map<String, Object>)
                      (response.getStatusCode().is2xxSuccessful() ? response.getBody() : null);
              assertNotNull(body);
              assertEquals("Best approach?", body.get("question"));
              Map<String, Object> innerResult = (Map<String, Object>) body.get("result");
              assertEquals("MAJORITY", innerResult.get("strategy"));
            })
        .verifyComplete();
  }

  @Test
  void compareStrategies_InsufficientProviders_ReturnsBadRequest() {
    Mono<ResponseEntity<Object>> result =
        votingController.compareStrategies(
            Map.of("question", "Test?", "providers", List.of("only-one")));

    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Map<String, Object> body = (Map<String, Object>) response.getBody();
              assertEquals("At least 2 providers required for comparison", body.get("error"));
            })
        .verifyComplete();
  }

  @Test
  void compareStrategies_NullProviders_ReturnsBadRequest() {
    Mono<ResponseEntity<Object>> result =
        votingController.compareStrategies(Map.of("question", "Test?"));

    StepVerifier.create(result)
        .assertNext(response -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()))
        .verifyComplete();
  }
}
