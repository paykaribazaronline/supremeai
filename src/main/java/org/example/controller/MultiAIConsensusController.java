package org.example.controller;

import org.example.model.ConsensusVote;
import org.example.service.MultiAIConsensusService;
import org.example.service.AuthenticationService;
import org.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Multi-AI Consensus Controller
 * Admin views how SupremeAI learns from the configured AI provider set
 */
@RestController
@RequestMapping("/api/consensus")
public class MultiAIConsensusController {
    private static final Logger logger = LoggerFactory.getLogger(MultiAIConsensusController.class);
    
    @Autowired
    private MultiAIConsensusService consensusService;
    
    @Autowired
    private AuthenticationService authService;
    
    /**
     * POST /api/consensus/ask
        * Ask all configured AI providers a question, get consensus
     */
    @PostMapping("/ask")
    public ResponseEntity<?> askAllAI(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String question = request.get("question");
            if (question == null || question.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Question required"));
            }
            
            logger.info("👤 {} asking configured AI providers: {}", user.getUsername(), question);
            
            ConsensusVote vote = consensusService.askAllAI(question);
            
            if (vote == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Consensus service returned no result"));
            }
            
            Map<String, Object> responseBody = new LinkedHashMap<>();
            responseBody.put("status", "success");
            responseBody.put("question", vote.getQuestion());
            responseBody.put("voteId", vote.getId());
            responseBody.put("responses", vote.getTotalResponses());
            responseBody.put("winningResponse", vote.getWinningResponse() != null
                ? vote.getWinningResponse()
                : "[No consensus reached — all configured providers failed to respond]");
            responseBody.put("consensusPercentage", vote.getConsensusPercentage());
            responseBody.put("confidenceScore", vote.getConfidenceScore());
            responseBody.put("learnings", vote.getLearnings());
            responseBody.put("timestamp", vote.getTimestamp());
            return ResponseEntity.ok(responseBody);
            
        } catch (Exception e) {
            logger.error("❌ Consensus error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Consensus failed: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/consensus/history
     * View all past consensus votes
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            List<ConsensusVote> votes = consensusService.getVoteHistory();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "totalVotes", votes.size(),
                "votes", votes.stream()
                    .map(v -> {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("id", v.getId());
                        entry.put("question", v.getQuestion() != null ? v.getQuestion() : "");
                        entry.put("winningResponse", v.getWinningResponse() != null
                            ? v.getWinningResponse()
                            : "[No consensus reached]");
                        entry.put("consensusPercentage", v.getConsensusPercentage());
                        entry.put("providersConsulted", v.getTotalResponses());
                        entry.put("timestamp", v.getTimestamp());
                        return entry;
                    })
                    .toList()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Failed to retrieve history: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/consensus/stats
     * View consensus statistics and learning metrics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Map<String, Object> stats = consensusService.getConsensusStats();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "stats", stats,
                "message", "SupremeAI is learning from " + stats.get("totalProvidersConsulted") + " AI perspective points",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Failed to retrieve stats: " + e.getMessage()));
        }
    }
    
    // ========== PRIVATE HELPERS ==========
    
    /**
     * Extract user from Bearer token. Requires valid authentication.
     * Auth is handled by Spring Security + Firebase + JWT validation.
     */
    private User extractUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        try {
            String token = authHeader.substring(7);
            if (token.isEmpty()) {
                throw new IllegalArgumentException("Bearer token is empty");
            }
            User user = authService.validateToken(token);
            if (user != null) return user;
            throw new IllegalArgumentException("Token validation returned null");
        } catch (Exception e) {
            logger.warn("Authentication failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired token: " + e.getMessage());
        }
    }
}
