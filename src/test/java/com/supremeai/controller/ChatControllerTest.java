package com.supremeai.controller;

import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.ChatIntelligenceService;
import com.supremeai.service.MultiAIVotingService;
import com.supremeai.service.MultiAIConsensusService;
import com.supremeai.service.EnhancedLearningService;
import com.supremeai.dto.ChatRequest;
import com.supremeai.dto.FeedbackRequest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

    @ExtendWith(MockitoExtension.class)
    class ChatControllerTest {

    @Mock
    private MultiAIVotingService consensusService;

    @Mock
    private AutonomousQuestioningEngine questioningEngine;

    @Mock
    private MultiAIVotingService votingService;

    @Mock
    private EnhancedLearningService enhancedLearningService;

    @Mock
    private ChatIntelligenceService intelligenceService;

    private ChatController chatController;

    @BeforeEach
    void setUp() {
        chatController = new ChatController();
        // Inject mocks via reflection since @Autowired is used
        setField(chatController, "consensusService", consensusService);
        setField(chatController, "questioningEngine", questioningEngine);
        setField(chatController, "votingService", votingService);
        setField(chatController, "enhancedLearningService", enhancedLearningService);
        setField(chatController, "intelligenceService", intelligenceService);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = ChatController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
            // Also set the circuit breaker and retry to avoid NPE
            field = ChatController.class.getDeclaredField("aiCircuitBreaker");
            field.setAccessible(true);
            field.set(target, CircuitBreaker.ofDefaults("test"));
            field = ChatController.class.getDeclaredField("aiRetry");
            field.setAccessible(true);
            field.set(target, Retry.ofDefaults("test"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== sendMessage - Empty Message Tests ====================

    @Test
    void sendMessage_EmptyMessage_ReturnsBadRequest() {
        ChatRequest request = new ChatRequest();
        request.setMessage("");
        request.setSkipValidation(false);

        ResponseEntity<Object> result = chatController.sendMessage(request);

        assertEquals(400, result.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("Message is required", body.get("error"));
    }

    @Test
    void sendMessage_NullMessage_ReturnsBadRequest() {
        ChatRequest request = new ChatRequest();
        request.setMessage(null);
        request.setSkipValidation(false);

        ResponseEntity<Object> result = chatController.sendMessage(request);

        assertEquals(400, result.getStatusCode().value());
    }

    // ==================== sendMessage - Validation Tests ====================

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

        ResponseEntity<Object> result = chatController.sendMessage(request);

        assertEquals(200, result.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("CLARIFICATION_REQUIRED", body.get("type"));
        assertTrue(((List) body.get("questions")).size() > 0);

        verify(questioningEngine).validateAndQuestion(anyString(), any());
    }

    // ==================== sendMessage - Voting Success Tests ====================

    @Test
    void sendMessage_SuccessfulVoting_ReturnsResponse() {
        ChatRequest request = new ChatRequest();
        request.setMessage("Explain Java streams");
        request.setSkipValidation(true);

        AutonomousQuestioningEngine.ValidationResult validationResult =
                new AutonomousQuestioningEngine.ValidationResult();
        validationResult.setComplete(true);

        MultiAIVotingService.VotingResult votingResult = new MultiAIVotingService.VotingResult(
                "Explain Java streams",
                "Java streams are...",
                List.of(),
                0.85,
                "STRONG_CONSENSUS",
                1500L
        );

        when(questioningEngine.validateAndQuestion(anyString(), any()))
                .thenReturn(Mono.just(validationResult));
        when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
                .thenReturn(Mono.just(votingResult));
        when(intelligenceService.classifyIntent(anyString()))
                .thenReturn(ChatIntelligenceService.Intent.CASUAL);
        doNothing().when(intelligenceService).handleIntelligence(anyString(), anyString(), any(), anyString(), anyDouble());
        when(enhancedLearningService.learnFromNLPInteraction(anyString(), anyString(), anyString(), anyDouble(), anyMap()))
                .thenReturn(Mono.empty());

        ResponseEntity<Object> result = chatController.sendMessage(request);

        assertEquals(200, result.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("Java streams are...", body.get("message"));
        assertEquals("STRONG_CONSENSUS", body.get("verdict"));
        assertEquals(0.85, body.get("confidence"));
    }

    // ==================== sendMessage - Circuit Breaker Fallback Tests ====================

    @Test
    void sendMessage_VotingFails_ConsensusFallback() {
        ChatRequest request = new ChatRequest();
        request.setMessage("What is Python?");
        request.setSkipValidation(true);

        AutonomousQuestioningEngine.ValidationResult validationResult =
                new AutonomousQuestioningEngine.ValidationResult();
        validationResult.setComplete(true);

        when(questioningEngine.validateAndQuestion(anyString(), any()))
                .thenReturn(Mono.just(validationResult));
        when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
                .thenReturn(Mono.error(new RuntimeException("All providers failed")));
        when(consensusService.askConsensus(anyString(), anyList(), anyLong()))
                .thenReturn(Mono.just(new com.supremeai.model.ConsensusResult(
                        "What is Python?", "Python is a programming language",
                        List.of(), 0.8, "CONSENSUS_STRONG", 0.8, 1000L, 0.85
                )));

        ResponseEntity<Object> result = chatController.sendMessage(request);

        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertTrue((Boolean) body.get("fallback"));
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

        when(questioningEngine.validateAndQuestion(anyString(), any()))
                .thenReturn(Mono.just(validationResult));
        when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
                .thenReturn(Mono.error(new RuntimeException("All providers failed")));
        when(consensusService.askConsensus(anyString(), anyList(), anyLong()))
                .thenReturn(Mono.error(new RuntimeException("Consensus also failed")));

        ResponseEntity<Object> result = chatController.sendMessage(request);

        assertEquals(503, result.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("AI services temporarily unavailable", body.get("error"));
    }

    // ==================== history Endpoint Tests ====================

    @Test
    void getHistory_ReturnsEmptyHistory() {
        ResponseEntity<Object> result = chatController.getHistory("default", 50).block();

        assertEquals(200, result.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals(0, ((java.util.List) body.get("messages")).size());
        assertEquals("default", body.get("agent"));
    }

    @Test
    void getHistory_WithAgent_ReturnsAgentHistory() {
        ResponseEntity<Object> result = chatController.getHistory("agent-123", 10).block();

        assertEquals(200, result.getStatusCode().value());
        assertEquals("agent-123", ((Map<String, Object>) result.getBody()).get("agent"));
    }

    // ==================== feedback Endpoint Tests ====================

    @Test
    void submitFeedback_PositiveFeedback_NoError() {
        FeedbackRequest request = new FeedbackRequest();
        request.setMessageId("msg-123");
        request.setUserMessage("What is Java?");
        request.setAiResponse("Java is a programming language");
        request.setHelpful(true);

        when(enhancedLearningService.learnFromNLPInteraction(anyString(), anyString(), anyString(), anyDouble(), anyMap()))
                .thenReturn(Mono.empty());

        ResponseEntity<Object> result = chatController.submitFeedback(request).block();

        assertEquals(200, result.getStatusCode().value());
        assertEquals("received", ((Map<String, Object>) result.getBody()).get("status"));

        verify(enhancedLearningService).learnFromNLPInteraction(
                eq("What is Java?"), eq("Java is a programming language"),
                eq("feedback_system"), eq(1.0), anyMap()
        );
    }

    @Test
    void submitFeedback_NegativeFeedback_LearnsWithLowScore() {
        FeedbackRequest request = new FeedbackRequest();
        request.setMessageId("msg-456");
        request.setUserMessage("How to sort?");
        request.setAiResponse("Bad answer");
        request.setHelpful(false);

        when(enhancedLearningService.learnFromNLPInteraction(anyString(), anyString(), anyString(), anyDouble(), anyMap()))
                .thenReturn(Mono.empty());

        ResponseEntity<Object> result = chatController.submitFeedback(request).block();

        assertEquals(200, result.getStatusCode().value());

        verify(enhancedLearningService).learnFromNLPInteraction(
                eq("How to sort?"), eq("Bad answer"),
                eq("feedback_system"), eq(0.3), anyMap()
        );
    }

    @Test
    void submitFeedback_NullMessageId_StillAccepts() {
        FeedbackRequest request = new FeedbackRequest();
        request.setMessageId(null);
        request.setUserMessage("Test");
        request.setAiResponse("Response");
        request.setHelpful(true);

        when(enhancedLearningService.learnFromNLPInteraction(anyString(), anyString(), anyString(), anyDouble(), anyMap()))
                .thenReturn(Mono.empty());

        ResponseEntity<Object> result = chatController.submitFeedback(request).block();

        assertEquals(200, result.getStatusCode().value());
    }

    // ==================== health Endpoint Tests ====================

    @Test
    void health_ReturnsOperationalStatus() {
        ResponseEntity<Object> result = chatController.health().block();

        assertEquals(200, result.getStatusCode().value());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("UP", body.get("status"));
        assertEquals("ACTIVE", body.get("autonomous_questioning"));
        assertEquals("ACTIVE", body.get("voting_system"));
    }

    // ==================== detectMode Helper Tests ====================

    @Test
    void detectMode_ArchitectKeywords_ReturnsArchitect() {
        assertEquals("architect", invokeDetectMode(chatController, "Design the architecture for a microservice"));
    }

    @Test
    void detectMode_DebugKeywords_ReturnsDebug() {
        assertEquals("debug", invokeDetectMode(chatController, "I have an error in my code, help me fix it"));
    }

    @Test
    void detectMode_ReviewKeywords_ReturnsReview() {
        assertEquals("review", invokeDetectMode(chatController, "Can you review my code?"));
    }

    @Test
    void detectMode_AskKeywords_ReturnsAsk() {
        assertEquals("ask", invokeDetectMode(chatController, "What is dependency injection?"));
    }

    @Test
    void detectMode_OrchestrateKeywords_ReturnsOrchestrator() {
        assertEquals("orchestrator", invokeDetectMode(chatController, "Orchestrate the deployment pipeline"));
    }

    @Test
    void detectMode_Default_ReturnsCode() {
        assertEquals("code", invokeDetectMode(chatController, "Write a REST API"));
    }

    private String invokeDetectMode(ChatController controller, String message) {
        try {
            java.lang.reflect.Method method = ChatController.class.getDeclaredMethod("detectMode", String.class);
            method.setAccessible(true);
            return (String) method.invoke(controller, message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}