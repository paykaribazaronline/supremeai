package com.supremeai.controller;

import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.ContextualAIRankingService;
import com.supremeai.service.MultiAIVotingService;
import com.supremeai.model.ConsensusResult;
import com.supremeai.agentorchestration.VotingDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotingControllerTest {

    @Mock
    private MultiAIVotingService votingService;

    @Mock
    private com.supremeai.agentorchestration.RequirementAnalyzerAI requirementAnalyzer;

    private VotingController votingController;

    @BeforeEach
    void setUp() {
        votingController = new VotingController();
        setField(votingController, "votingService", votingService);
        setField(votingController, "requirementAnalyzer", requirementAnalyzer);
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

        ResponseEntity<List<String>> result = votingController.analyzeRequirement(
                Map.of("requirement", "Build an app")
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        verify(requirementAnalyzer).generateClarifyingQuestions("Build an app");
    }

    @Test
    void analyzeRequirement_EmptyRequirement_ReturnsEmptyList() {
        when(requirementAnalyzer.generateClarifyingQuestions(""))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<String>> result = votingController.analyzeRequirement(
                Map.of("requirement", "")
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }

    @Test
    void analyzeRequirement_NullRequirement_ReturnsQuestionsForNull() {
        when(requirementAnalyzer.generateClarifyingQuestions(null))
                .thenReturn(List.of("Please clarify your requirements"));

        ResponseEntity<List<String>> result = votingController.analyzeRequirement(
                Map.of("requirement", null)
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertFalse(result.getBody().isEmpty());
    }

    // ==================== conductVote Tests ====================

    @Test
    void conductVote_ValidQuestion_ReturnsDecision() {
        VotingDecision mockDecision = new VotingDecision();
        mockDecision.setAiConsensus("Use microservices");
        mockDecision.setStrength("STRONG");
        mockDecision.setConfidence(0.85);

        when(votingService.conductDecisionVote(anyString(), anyString()))
                .thenReturn(Mono.just(mockDecision));

        Mono<ResponseEntity<VotingDecision>> result = votingController.conductVote(
                Map.of("question", "Should we use microservices?", "context", "Building a large app")
        );

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals("Use microservices", response.getBody().getAiConsensus());
                    assertEquals("STRONG", response.getBody().getStrength());
                    return true;
                })
                .verifyComplete();

        verify(votingService).conductDecisionVote(eq("Should we use microservices?"), eq("Building a large app"));
    }

    @Test
    void conductVote_EmptyContext_ReturnsDecision() {
        VotingDecision mockDecision = new VotingDecision();
        mockDecision.setAiConsensus("Use monolith");

        when(votingService.conductDecisionVote(anyString(), eq("")))
                .thenReturn(Mono.just(mockDecision));

        Mono<ResponseEntity<VotingDecision>> result = votingController.conductVote(
                Map.of("question", "Which architecture?")
        );

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== voteOnQuestion Tests ====================

    @Test
    void voteOnQuestion_ValidQuestion_ReturnsConsensus() {
        ConsensusResult mockResult = new ConsensusResult(
                "What is Docker?", "Docker is containerization",
                List.of(), 0.85, "CONSENSUS_STRONG", 80.0, 1000L, 0.82
        );

        when(votingService.askConsensus(anyString(), anyList(), anyLong()))
                .thenReturn(Mono.just(mockResult));

        Mono<ResponseEntity<Object>> result = votingController.voteOnQuestion(
                Map.of("question", "What is Docker?")
        );

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertEquals("Docker is containerization", body.get("consensus"));
                    assertEquals(0.85, body.get("confidence"));
                    return true;
                })
                .verifyComplete();

        verify(votingService).askConsensus(eq("What is Docker?"), anyList(), eq(10000L));
    }

    @Test
    void voteOnQuestion_EmptyQuestion_ReturnsBadRequest() {
        Mono<ResponseEntity<Object>> result = votingController.voteOnQuestion(
                Map.of("question", "")
        );

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertEquals("Question is required", body.get("error"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void voteOnQuestion_NullQuestion_ReturnsBadRequest() {
        Mono<ResponseEntity<Object>> result = votingController.voteOnQuestion(
                Map.of()
        );

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void voteOnQuestion_CustomProviders_ReturnsConsensus() {
        ConsensusResult mockResult = new ConsensusResult(
                "Question", "Answer", List.of(), 0.8, "CONSENSUS_MODERATE", 60.0, 500L, 0.75
        );

        when(votingService.askConsensus(anyString(), anyList(), anyLong()))
                .thenReturn(Mono.just(mockResult));

        Mono<ResponseEntity<Object>> result = votingController.voteOnQuestion(
                Map.of(
                        "question", "Test question",
                        "providers", List.of("groq", "openai")
                )
        );

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== getHistory Tests ====================

    @Test
    void getHistory_WithLimit_ReturnsHistory() {
        ConsensusVote vote1 = new ConsensusVote();
        vote1.setQuestion("Q1");
        vote1.setConsensusAnswer("A1");
        ConsensusVote vote2 = new ConsensusVote();
        vote2.setQuestion("Q2");
        vote2.setConsensusAnswer("A2");

        when(votingService.getConsensusHistory(5))
                .thenReturn(Flux.just(vote1, vote2));

        Mono<ResponseEntity<Object>> result = votingController.getHistory(5);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertEquals(2, ((java.util.List) body.get("history")).size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getHistory_DefaultLimit_ReturnsHistory() {
        when(votingService.getConsensusHistory(20))
                .thenReturn(Flux.empty());

        Mono<ResponseEntity<Object>> result = votingController.getHistory(20);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
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
                .expectNextMatches(response -> {
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

    @Test
    void compareStrategies_ValidRequest_ReturnsComparison() {
        ConsensusResult mockResult = new ConsensusResult(
                "Q", "Best answer", List.of(), 0.85, "CONSENSUS_STRONG", 80.0, 500L, 0.8
        );

        when(votingService.askConsensus(anyString(), anyList(), eq(15000L)))
                .thenReturn(Mono.just(mockResult));

        Mono<ResponseEntity<Object>> result = votingController.compareStrategies(
                Map.of(
                        "question", "Best approach?",
                        "providers", List.of("groq", "openai", "claude")
                )
        );

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertEquals("Best approach?", body.get("question"));
                    Map<String, Object> innerResult = (Map<String, Object>) body.get("result");
                    assertEquals("MAJORITY", innerResult.get("strategy"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void compareStrategies_InsufficientProviders_ReturnsBadRequest() {
        Mono<ResponseEntity<Object>> result = votingController.compareStrategies(
                Map.of(
                        "question", "Test?",
                        "providers", List.of("only-one")
                )
        );

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertEquals("At least 2 providers required for comparison", body.get("error"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void compareStrategies_NullProviders_ReturnsBadRequest() {
        Mono<ResponseEntity<Object>> result = votingController.compareStrategies(
                Map.of("question", "Test?")
        );

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }
}