package com.supremeai.controller;

import com.supremeai.service.ChatProcessingService;
import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.MultiAIVotingService;
import com.supremeai.model.ConsensusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserChatControllerTest {

    @Mock
    private ChatProcessingService chatProcessingService;

    @Mock
    private AutonomousQuestioningEngine questioningEngine;

    @Mock
    private MultiAIVotingService votingService;

    @Mock
    private MultiAIVotingService consensusService;

    private UserChatController userChatController;

    @BeforeEach
    void setUp() {
        userChatController = new UserChatController(
                chatProcessingService, questioningEngine, votingService, consensusService
        );
    }

    // ==================== sendMessage - Validation Tests ====================

    @Test
    void sendMessage_NullUserId_ReturnsBadRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("message", "Hello");
        // userId is null

        Mono<ResponseEntity<Map<String, Object>>> result = userChatController.sendMessage(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(400, response.getStatusCode().value());
                    assertTrue(response.getBody().get("error").toString().contains("required"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void sendMessage_NullMessage_ReturnsBadRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("user_id", "user-123");
        // message is null

        Mono<ResponseEntity<Map<String, Object>>> result = userChatController.sendMessage(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(400, response.getStatusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void sendMessage_ValidRequest_ReturnsResponse() {
        Map<String, Object> request = new HashMap<>();
        request.put("user_id", "user-123");
        request.put("message", "Hello, how are you?");
        request.put("is_admin", false);

        Map<String, Object> classificationResult = new HashMap<>();
        classificationResult.put("needs_confirmation", false);
        classificationResult.put("reason", "normal conversation");
        classificationResult.put("chat_id", "chat-123");
        classificationResult.put("item_type", "general");
        classificationResult.put("confidence", 0.95);

        ConsensusResult consensusResult = new ConsensusResult(
                "Hello, how are you?", "I'm doing well, thank you!",
                List.of(), 0.88, "CONSENSUS_STRONG", 85.0, 500L, 0.85
        );

        when(chatProcessingService.processMessage(anyString(), anyString(), anyBoolean()))
                .thenReturn(Mono.just(classificationResult));
        when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
                .thenReturn(Mono.just(consensusResult));

        Mono<ResponseEntity<Map<String, Object>>> result = userChatController.sendMessage(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals("I'm doing well, thank you!", response.getBody().get("response"));
                    assertEquals("SupremeAI Consensus", response.getBody().get("agentName"));
                    assertEquals(false, response.getBody().get("requires_confirmation"));
                    return true;
                })
                .verifyComplete();

        verify(chatProcessingService).processMessage("user-123", "Hello, how are you?", false);
        verify(votingService).executeEnsembleVoting(eq("Hello, how are you?"), any(), anyLong());
    }

    // ==================== sendMessage - Admin Request Tests ====================

    @Test
    void sendMessage_AdminRequest_ProcessesCorrectly() {
        Map<String, Object> request = new HashMap<>();
        request.put("user_id", "admin-user");
        request.put("message", "Generate a report");
        request.put("is_admin", true);

        Map<String, Object> classificationResult = new HashMap<>();
        classificationResult.put("needs_confirmation", false);
        classificationResult.put("reason", "admin task");
        classificationResult.put("chat_id", "chat-admin");
        classificationResult.put("confidence", 0.9);

        ConsensusResult consensusResult = new ConsensusResult(
                "Generate a report", "Here is your report...",
                List.of(), 0.9, "CONSENSUS_STRONG", 90.0, 300L, 0.9
        );

        when(chatProcessingService.processMessage(anyString(), anyString(), eq(true)))
                .thenReturn(Mono.just(classificationResult));
        when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
                .thenReturn(Mono.just(consensusResult));

        Mono<ResponseEntity<Map<String, Object>>> result = userChatController.sendMessage(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== sendMessage - Confirmation Required Tests ====================

    @Test
    void sendMessage_NeedsConfirmation_ReturnsPendingResponse() {
        Map<String, Object> request = new HashMap<>();
        request.put("user_id", "user-456");
        request.put("message", "Delete all records");
        request.put("is_admin", false);

        Map<String, Object> classificationResult = new HashMap<>();
        classificationResult.put("needs_confirmation", true);
        classificationResult.put("item_type", "destructive_action");
        classificationResult.put("content", "Delete all records from database");
        classificationResult.put("confidence", 0.99);
        classificationResult.put("reason", "Potential destructive action detected");

        when(chatProcessingService.processMessage(anyString(), anyString(), anyBoolean()))
                .thenReturn(Mono.just(classificationResult));

        Mono<ResponseEntity<Map<String, Object>>> result = userChatController.sendMessage(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals("CLARIFICATION_REQUIRED",
                            ((Map<String, Object>) response.getBody().get("response")).get("type"));
                    assertTrue((Boolean) response.getBody().get("requires_confirmation"));
                    assertEquals("pending", response.getBody().get("status"));
                    return true;
                })
                .verifyComplete();

        verify(votingService, never()).executeEnsembleVoting(anyString(), any(), anyLong());
    }

    // ==================== sendMessage Fallback Tests ====================

    @Test
    void sendMessage_VotingFails_ConsensusFallback() {
        Map<String, Object> request = new HashMap<>();
        request.put("user_id", "user-789");
        request.put("message", "Tell me a joke");
        request.put("is_admin", false);

        Map<String, Object> classificationResult = new HashMap<>();
        classificationResult.put("needs_confirmation", false);
        classificationResult.put("reason", "normal");
        classificationResult.put("chat_id", "chat-456");
        classificationResult.put("confidence", 0.9);

        ConsensusResult fallbackResult = new ConsensusResult(
                "Tell me a joke", "Why did the programmer quit? Because he didn't get arrays.",
                List.of(), 0.8, "CONSENSUS_MODERATE", 70.0, 2000L, 0.78
        );

        when(chatProcessingService.processMessage(anyString(), anyString(), anyBoolean()))
                .thenReturn(Mono.just(classificationResult));
        when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
                .thenReturn(Mono.error(new RuntimeException("Voting service unavailable")));
        when(consensusService.askConsensus(anyString(), anyList(), anyLong()))
                .thenReturn(Mono.just(fallbackResult));

        Mono<ResponseEntity<Map<String, Object>>> result = userChatController.sendMessage(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertTrue((Boolean) response.getBody().get("fallback"));
                    assertEquals("SupremeAI Fallback", response.getBody().get("agentName"));
                    return true;
                })
                .verifyComplete();

        verify(consensusService).askConsensus(anyString(), anyList(), anyLong());
    }

    @Test
    void sendMessage_AllServicesFail_ReturnsErrorResponse() {
        Map<String, Object> request = new HashMap<>();
        request.put("user_id", "user-999");
        request.put("message", "Hello");

        Map<String, Object> classificationResult = new HashMap<>();
        classificationResult.put("needs_confirmation", false);
        classificationResult.put("reason", "error test");
        classificationResult.put("chat_id", "chat-error");

        when(chatProcessingService.processMessage(anyString(), anyString(), anyBoolean()))
                .thenReturn(Mono.just(classificationResult));
        when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
                .thenReturn(Mono.error(new RuntimeException("Voting failed")));
        when(consensusService.askConsensus(anyString(), anyList(), anyLong()))
                .thenReturn(Mono.error(new RuntimeException("Consensus failed")));

        Mono<ResponseEntity<Map<String, Object>>> result = userChatController.sendMessage(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals("error", response.getBody().get("status"));
                    return true;
                })
                .verifyComplete();
    }

    // ==================== sendMessageLegacy Tests ====================

    @Test
    void sendMessageLegacy_MapsToSendMessage() {
        Map<String, Object> request = new HashMap<>();
        request.put("message", "Hello from legacy");

        // Since message is not null, it will set user_id and is_admin
        Map<String, Object> classificationResult = new HashMap<>();
        classificationResult.put("needs_confirmation", false);
        classificationResult.put("reason", "legacy test");
        classificationResult.put("chat_id", "chat-legacy");

        ConsensusResult consensusResult = new ConsensusResult(
                "Hello from legacy", "Legacy response",
                List.of(), 0.7, "CONSENSUS_MODERATE", 70.0, 1000L, 0.7
        );

        when(chatProcessingService.processMessage(anyString(), anyString(), anyBoolean()))
                .thenReturn(Mono.just(classificationResult));
        when(votingService.executeEnsembleVoting(anyString(), any(), anyLong()))
                .thenReturn(Mono.just(consensusResult));

        Mono<ResponseEntity<Map<String, Object>>> result = userChatController.sendMessageLegacy(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode().value() == 200)
                .verifyComplete();
    }

    // ==================== getHistory Tests ====================

    @Test
    void getHistory_ValidRequest_ReturnsHistory() {
        when(chatProcessingService.getChatHistory("user-123", 50))
                .thenReturn(Mono.just(List.of(
                        Map.of("message", "Hello", "response", "Hi there"),
                        Map.of("message", "How are you?", "response", "Good")
                )));

        Mono<ResponseEntity<Map<String, Object>>> result =
                userChatController.getHistory("user-123", 50);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    Map<String, Object> body = response.getBody();
                    assertEquals(true, body.get("success"));
                    assertEquals(2, ((java.util.List) body.get("chat_history")).size());
                    assertEquals("user-123", body.get("user_id"));
                    return true;
                })
                .verifyComplete();

        verify(chatProcessingService).getChatHistory("user-123", 50);
    }

    // ==================== feedback Tests ====================

    @Test
    void submitFeedback_ValidFeedback_ReturnsReceived() {
        Map<String, Object> request = Map.of(
                "messageId", "msg-001",
                "userMessage", "How to code?",
                "aiResponse", "Code with practice",
                "helpful", true
        );

        Mono<ResponseEntity<Map<String, Object>>> result = userChatController.submitFeedback(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertEquals("received", response.getBody().get("status"));
                    return true;
                })
                .verifyComplete();
    }

    // ==================== health Tests ====================

    @Test
    void health_ReturnsActiveStatus() {
        Mono<ResponseEntity<Map<String, Object>>> result = userChatController.health();

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    Map<String, Object> body = response.getBody();
                    assertEquals("UP", body.get("status"));
                    assertEquals("ACTIVE", body.get("chat_classifier"));
                    assertEquals("ACTIVE", body.get("ai_voting"));
                    return true;
                })
                .verifyComplete();
    }
}