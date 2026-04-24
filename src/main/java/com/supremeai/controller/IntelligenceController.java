package com.supremeai.controller;

import com.supremeai.service.AIRankingService;
import com.supremeai.service.AutonomousQuestioningEngine;
import com.supremeai.service.TenAIVotingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for S3 (Autonomous Questioning) and S4 (10-AI Voting) systems
 */
@RestController
@RequestMapping("/api/v2/intelligence")
public class IntelligenceController {

    private static final Logger logger = LoggerFactory.getLogger(IntelligenceController.class);

    @Autowired
    private AutonomousQuestioningEngine questioningEngine;

    @Autowired
    private TenAIVotingSystem votingSystem;

    @Autowired
    private AIRankingService rankingService;

    /**
     * S9: Get AI Provider Rankings (Auto-Ranking)
     * GET /api/v2/intelligence/rankings
     */
    @GetMapping("/rankings")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER')")
    public ResponseEntity<?> getRankings() {
        return ResponseEntity.ok(rankingService.getRankings());
    }

    /**
     * S3: Validate user input and get clarifying questions
     * POST /api/v2/intelligence/validate
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER')")
    public ResponseEntity<?> validateInput(@RequestBody ValidationRequest request) {
        try {
            logger.info("S3: Validating input of type: {}", request.getRequestType());

            AutonomousQuestioningEngine.RequestType requestType = 
                parseRequestType(request.getRequestType());

            AutonomousQuestioningEngine.ValidationResult result = 
                questioningEngine.validateAndQuestion(request.getPrompt(), requestType);

            Map<String, Object> response = new HashMap<>();
            response.put("originalInput", result.getOriginalInput());
            response.put("requestType", result.getRequestType().toString());
            response.put("clarityScore", result.getClarityScore());
            response.put("isComplete", result.isComplete());
            response.put("clarifyingQuestions", result.getClarifyingQuestions());
            response.put("hasQuestions", result.hasQuestions());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error validating input", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * S4: Execute 10-AI Voting System
     * POST /api/v2/intelligence/vote
     */
    @PostMapping("/vote")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'AGENT_MANAGER')")
    public ResponseEntity<?> executeVoting(@RequestBody VotingRequest request) {
        try {
            logger.info("S4: Executing 10-AI voting for prompt: {}", 
                       request.getPrompt().substring(0, Math.min(50, request.getPrompt().length())));

            List<String> models = request.getModels();
            if (models == null || models.isEmpty()) {
                models = List.of(TenAIVotingSystem.TEN_AI_MODELS);
            }

            long timeoutMs = request.getTimeoutMs() > 0 ? request.getTimeoutMs() : 15000;

            TenAIVotingSystem.VotingResult result = votingSystem.executeVoting(
                request.getPrompt(), 
                models, 
                timeoutMs
            );

            Map<String, Object> response = new HashMap<>();
            response.put("prompt", result.getPrompt());
            response.put("bestResponse", result.getBestResponse());
            response.put("averageConfidence", result.getAverageConfidence());
            response.put("verdict", result.getVerdict());
            response.put("processingTimeMs", result.getProcessingTimeMs());
            response.put("totalModelsUsed", result.getTotalModelsUsed());
            response.put("totalModelsAvailable", TenAIVotingSystem.TEN_AI_MODELS.length);

            // Add individual votes
            List<Map<String, Object>> votes = result.getAllVotes().stream()
                .map(vote -> {
                    Map<String, Object> voteMap = new HashMap<>();
                    voteMap.put("provider", vote.getProviderName());
                    voteMap.put("confidence", vote.getConfidence());
                    voteMap.put("timestamp", vote.getTimestamp());
                    // Truncate response for summary
                    String resp = vote.getResponse();
                    voteMap.put("responsePreview", resp.length() > 200 ? resp.substring(0, 200) + "..." : resp);
                    return voteMap;
                })
                .collect(java.util.stream.Collectors.toList());

            response.put("votes", votes);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error executing 10-AI voting", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get list of available AI models for voting
     * GET /api/v2/intelligence/models
     */
    @GetMapping("/models")
    public ResponseEntity<?> getAvailableModels() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalModels", TenAIVotingSystem.TEN_AI_MODELS.length);
        response.put("models", TenAIVotingSystem.TEN_AI_MODELS);
        response.put("description", "10 AI Models available for ensemble voting");
        return ResponseEntity.ok(response);
    }

    /**
     * Health check for intelligence systems
     * GET /api/v2/intelligence/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("s3_autonomous_questioning", "operational");
        health.put("s4_ten_ai_voting", "operational");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    private AutonomousQuestioningEngine.RequestType parseRequestType(String type) {
        if (type == null) return AutonomousQuestioningEngine.RequestType.GENERAL_AI;
        
        try {
            return AutonomousQuestioningEngine.RequestType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AutonomousQuestioningEngine.RequestType.GENERAL_AI;
        }
    }

    /**
     * Request DTOs
     */
    public static class ValidationRequest {
        private String prompt;
        private String requestType;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getRequestType() { return requestType; }
        public void setRequestType(String requestType) { this.requestType = requestType; }
    }

    public static class VotingRequest {
        private String prompt;
        private List<String> models;
        private long timeoutMs;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public List<String> getModels() { return models; }
        public void setModels(List<String> models) { this.models = models; }
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    }
}
