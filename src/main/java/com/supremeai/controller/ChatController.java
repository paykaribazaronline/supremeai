package com.supremeai.controller;

import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.TenAIVotingSystem;
import com.supremeai.service.MultiAIConsensusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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
    private MultiAIConsensusService consensusService;

    @Autowired
    private AutonomousQuestioningEngine questioningEngine;

    @Autowired
    private TenAIVotingSystem votingSystem;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER')")
    public Mono<ResponseEntity<Object>> sendMessage(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        Boolean skipValidation = (Boolean) request.getOrDefault("skipValidation", false);

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
            var votingResult = votingSystem.executeVoting(message, null, 15000L);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", votingResult.getBestResponse());
            response.put("verdict", votingResult.getVerdict());
            response.put("confidence", votingResult.getAverageConfidence());
            response.put("modelsUsed", votingResult.getTotalModelsUsed());
            response.put("processingTimeMs", votingResult.getProcessingTimeMs());
            response.put("timestamp", java.time.Instant.now().toString());

            return Mono.just(ResponseEntity.ok(response));
        } catch (Exception e) {
            logger.error("Failed to get response via voting system", e);
            
            // Fallback to simpler consensus if voting fails
            if (consensusService != null) {
                var res = consensusService.askAllAIs(message, List.of("google"), 10000L);
                return Mono.just(ResponseEntity.ok(Map.of(
                    "message", res.getConsensusAnswer(),
                    "confidence", res.getAverageConfidence(),
                    "fallback", true
                )));
            }
            
            return Mono.just(ResponseEntity.status(500).body(Map.of("error", "Voting system error: " + e.getMessage())));
        }
    }

    @GetMapping("/history")
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
    public Mono<ResponseEntity<Object>> submitFeedback(@RequestBody Map<String, Object> request) {
        String messageId = (String) request.get("messageId");
        Boolean helpful = (Boolean) request.get("helpful");

        logger.info("Received feedback for message: {}, helpful: {}", messageId, helpful);

        return Mono.just(ResponseEntity.ok(Map.of("status", "received")));
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
