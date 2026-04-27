package com.supremeai.controller;

import com.supremeai.service.ChatProcessingService;
import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.TenAIVotingSystem;
import com.supremeai.service.MultiAIConsensusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/api/chat-legacy")
public class UserChatController {

    private static final Logger logger = LoggerFactory.getLogger(UserChatController.class);

    private final ChatProcessingService chatProcessingService;
    private final AutonomousQuestioningEngine questioningEngine;
    private final TenAIVotingSystem votingSystem;
    private final MultiAIConsensusService consensusService;

    public UserChatController(ChatProcessingService chatProcessingService,
                             AutonomousQuestioningEngine questioningEngine,
                             TenAIVotingSystem votingSystem,
                             MultiAIConsensusService consensusService) {
        this.chatProcessingService = chatProcessingService;
        this.questioningEngine = questioningEngine;
        this.votingSystem = votingSystem;
        this.consensusService = consensusService;
    }

    @PostMapping("/message")
    public Mono<ResponseEntity<Map<String, Object>>> sendMessage(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("user_id");
        String message = (String) request.get("message");
        Boolean isAdmin = (Boolean) request.get("is_admin");

        if (userId == null || message == null) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("error", "user_id and message are required")));
        }

        // Classify and store message
        Map<String, Object> classificationResult = chatProcessingService.processMessage(
            userId, message, isAdmin != null && isAdmin
        );

        // If needs admin confirmation (rule/plan/command), don't generate AI response
        if (classificationResult.get("needs_confirmation") != null && 
            (boolean) classificationResult.get("needs_confirmation")) {
            String responseText = "I've detected a " + classificationResult.get("item_type") + 
                ": \"" + classificationResult.get("content") + "\". It has been sent for admin approval.";
            return Mono.just(ResponseEntity.ok(Map.of(
                "response", responseText,
                "agentName", "SupremeAI Classifier",
                "confidence", classificationResult.get("confidence"),
                "message", classificationResult.get("reason"),
                "requires_confirmation", true,
                "item_id", classificationResult.get("item_id"),
                "item_type", classificationResult.get("item_type"),
                "status", "pending"
            )));
        }

        // For normal messages, use AI voting system to generate response
        try {
            logger.info("Getting AI response for user message: {}", message);
            
            // Use 10-AI voting system (existing ChatController logic)
            var votingResult = votingSystem.executeVoting(message, null, 15000L);
            
            String bestResponse = votingResult.getBestResponse();
            Double confidence = votingResult.getAverageConfidence();
            
            Map<String, Object> aiResponse = new HashMap<>();
            aiResponse.put("response", bestResponse);
            aiResponse.put("agentName", "SupremeAI Consensus");
            aiResponse.put("confidence", confidence);
            aiResponse.put("message", classificationResult.get("reason"));
            aiResponse.put("requires_confirmation", false);
            aiResponse.put("status", "completed");
            aiResponse.put("processingTimeMs", votingResult.getProcessingTimeMs());
            aiResponse.put("modelsUsed", votingResult.getTotalModelsUsed());
            aiResponse.put("chat_id", classificationResult.get("chat_id"));
            
            return Mono.just(ResponseEntity.ok(aiResponse));
            
        } catch (Exception e) {
            logger.error("AI voting failed, falling back to consensus: {}", e.getMessage());
            
            // Fallback to simpler consensus
            return consensusService.askAllAIs(message, 
                    Arrays.asList("groq", "deepseek", "claude", "openai", "ollama"), 10000L)
                .map(res -> {
                    Map<String, Object> fallback = new HashMap<>();
                    fallback.put("response", res.getConsensusAnswer());
                    fallback.put("agentName", "SupremeAI Fallback");
                    fallback.put("confidence", res.getAverageConfidence());
                    fallback.put("message", classificationResult.get("reason"));
                    fallback.put("requires_confirmation", false);
                    fallback.put("status", "completed");
                    fallback.put("fallback", true);
                    fallback.put("chat_id", classificationResult.get("chat_id"));
                    return ResponseEntity.ok(fallback);
                })
                .onErrorResume(err -> {
                    logger.error("All AI systems failed", err);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("response", "I'm sorry, I'm having trouble connecting to AI systems. Please try again later.");
                    errorResponse.put("agentName", "SupremeAI Error");
                    errorResponse.put("confidence", 0.0);
                    errorResponse.put("status", "error");
                    errorResponse.put("chat_id", classificationResult.get("chat_id"));
                    return Mono.just(ResponseEntity.ok(errorResponse));
                });
        }
    }

    // Legacy endpoint for backward compatibility (maps to /api/chat/message)
    @PostMapping("/send")
    public Mono<ResponseEntity<Map<String, Object>>> sendMessageLegacy(@RequestBody Map<String, Object> request) {
        // Map 'message' param to expected format
        String message = (String) request.get("message");
        if (message != null) {
            request.put("user_id", "legacy_user");
            request.put("is_admin", false);
        }
        return sendMessage(request);
    }

    @GetMapping("/history")
    public Mono<ResponseEntity<Map<String, Object>>> getHistory(
            @RequestParam(required = false) String user_id,
            @RequestParam(defaultValue = "100") int limit) {
        List<Map<String, Object>> history = chatProcessingService.getChatHistory(user_id, limit);
        return Mono.just(ResponseEntity.ok(Map.of(
            "success", true,
            "chat_history", history
        )));
    }

    @PostMapping("/feedback")
    public Mono<ResponseEntity<Map<String, Object>>> submitFeedback(@RequestBody Map<String, Object> request) {
        // Store feedback for learning
        logger.info("Received feedback: {}", request);
        return Mono.just(ResponseEntity.ok(Map.of("status", "received")));
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "UP",
            "chat_classifier", "ACTIVE",
            "ai_voting", "ACTIVE"
        )));
    }
}
