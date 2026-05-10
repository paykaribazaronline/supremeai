package com.supremeai.controller;

import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.MultiAIVotingService;
import com.supremeai.service.MultiAIConsensusService;
import com.supremeai.service.EnhancedLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.supremeai.dto.ChatRequest;
import com.supremeai.dto.FeedbackRequest;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired(required = false)
    private MultiAIVotingService consensusService;

    @Autowired
    private AutonomousQuestioningEngine questioningEngine;

    @Autowired
    private MultiAIVotingService votingService;

    @Autowired(required = false)
    private EnhancedLearningService enhancedLearningService;

    @Autowired
    private com.supremeai.service.ChatIntelligenceService intelligenceService;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER')")
    public Mono<ResponseEntity<Object>> sendMessage(@Valid @RequestBody ChatRequest request) {
        String message = request.getMessage();
        boolean skipValidation = request.isSkipValidation();

        if (message == null || message.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "Message is required")));
        }

        logger.info("Received chat message: {}", message);

        // S3: Autonomous Questioning - Validate input clarity
        if (!skipValidation) {
            var validation = questioningEngine.validateAndQuestion(message, AutonomousQuestioningEngine.RequestType.GENERAL_AI);
            if (!validation.isComplete() && validation.hasQuestions()) {
                Map<String, Object> response = new HashMap<>();
                response.put("type", "CLARIFICATION_REQUIRED");
                response.put("questions", validation.getClarifyingQuestions());
                response.put("clarityScore", validation.getClarityScore());
                response.put("message", "I need more information before I can give you a quality answer.");
                return Mono.just(ResponseEntity.ok(response));
            }
        }

        // S4: 10-AI Voting System - Execute voting across models
        try {
            var votingResult = votingService.executeEnsembleVoting(message, null, 15000L);

            String bestResponse = votingResult.getBestResponse();
            Double confidence = votingResult.getAverageConfidence();

            Map<String, Object> response = new HashMap<>();
            response.put("message", bestResponse);
            response.put("verdict", votingResult.getVerdict());
            response.put("confidence", confidence);
            response.put("modelsUsed", votingResult.getTotalModelsUsed());
            response.put("processingTimeMs", votingResult.getProcessingTimeMs());
            response.put("timestamp", java.time.Instant.now().toString());

            // ডায়নামিকভাবে মোড সনাক্ত করা
            var intent = intelligenceService.classifyIntent(message);
            response.put("mode", intent.name().toLowerCase());
            response.put("intent", intent.name());

            // Autonomous intelligence handling (rules/plans)
            intelligenceService.handleIntelligence(
                request.getAgentId() != null ? request.getAgentId() : "default",
                message, 
                intent, 
                "ADMIN", // Defaulting to ADMIN for now as it's the Command Center
                confidence
            ).subscribe();

            // Capture NLP learning from this interaction
            if (enhancedLearningService != null) {
                enhancedLearningService.learnFromNLPInteraction(
                        message,
                        bestResponse,
                        "voting_system",
                        confidence != null ? confidence : 0.5,
                        Map.of("modelsUsed", votingResult.getTotalModelsUsed())
                ).subscribe(); // Fire and forget
            }

            return Mono.just(ResponseEntity.ok(response));
        } catch (Exception e) {
            logger.error("Failed to get response via voting system", e);
            
            // Fallback to simpler consensus if voting fails
            if (consensusService != null) {
                // Use multiple providers (not just google) for better fallback resilience
                return consensusService.askConsensus(message, 
                    java.util.Arrays.asList("groq", "deepseek", "claude", "openai", "ollama"), 10000L)
                    .map(res -> ResponseEntity.ok(Map.of(
                        "message", res.getConsensusAnswer(),
                        "confidence", res.getAverageConfidence(),
                        "fallback", true
                    )));
            }
            
            return Mono.just(ResponseEntity.status(500).body(Map.of("error", "Voting system error: " + e.getMessage())));
        }
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ResponseEntity<Object>> getHistory(
            @RequestParam(required = false) String agent,
            @RequestParam(defaultValue = "50") int limit) {
        return Mono.just(ResponseEntity.ok(Map.of(
            "messages", new ArrayList<>(),
            "count", 0,
            "agent", agent != null ? agent : "default"
        )));
    }

    @PostMapping("/feedback")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER')")
    public Mono<ResponseEntity<Object>> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        String messageId = request.getMessageId();
        boolean helpful = request.isHelpful();
        String userMessage = request.getUserMessage();
        String aiResponse = request.getAiResponse();

        logger.info("Received feedback for message: {}, helpful: {}", messageId, helpful);

        // Capture learning from feedback - this is valuable for NLP improvement
        if (enhancedLearningService != null && userMessage != null && aiResponse != null) {
            double qualityScore = helpful ? 1.0 : 0.3;
            enhancedLearningService.learnFromNLPInteraction(
                    userMessage,
                    aiResponse,
                    "feedback_system",
                    qualityScore,
                    Map.of("messageId", messageId, "helpful", helpful)
            ).subscribe(); // Fire and forget
        }

        return Mono.just(ResponseEntity.ok(Map.of("status", "received")));
    }

    /**
     * ডায়নামিকভাবে মোড সনাক্ত করার হেল্পার মেথড
     */
    private String detectMode(String message) {
        String lowerMsg = message.toLowerCase();
        if (lowerMsg.contains("architect") || lowerMsg.contains("design") || lowerMsg.contains("structure")) {
            return "architect";
        } else if (lowerMsg.contains("debug") || lowerMsg.contains("fix") || lowerMsg.contains("error") || lowerMsg.contains("issue")) {
            return "debug";
        } else if (lowerMsg.contains("review") || lowerMsg.contains("audit") || lowerMsg.contains("analyze")) {
            return "review";
        } else if (lowerMsg.contains("ask") || lowerMsg.contains("what") || lowerMsg.contains("how") || lowerMsg.contains("explain")) {
            return "ask";
        } else if (lowerMsg.contains("orchestrate") || lowerMsg.contains("manage") || lowerMsg.contains("coordinate")) {
            return "orchestrator";
        } else {
            return "code";
        }
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> health() {
        return Mono.just(ResponseEntity.ok(Map.of(
            "status", "UP",
            "autonomous_questioning", "ACTIVE",
            "voting_system", "ACTIVE"
        )));
    }
}
