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
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "question", vote.getQuestion(),
                "voteId", vote.getId(),
                "responses", vote.getTotalResponses(),
                "winningResponse", vote.getWinningResponse(),
                "consensusPercentage", vote.getConsensusPercentage(),
                "confidenceScore", vote.getConfidenceScore(),
                "learnings", vote.getLearnings(),
                "timestamp", vote.getTimestamp()
            ));
            
        } catch (Exception e) {
            logger.error("❌ Consensus error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
                    .map(v -> Map.of(
                        "id", v.getId(),
                        "question", v.getQuestion(),
                        "winningResponse", v.getWinningResponse(),
                        "consensusPercentage", v.getConsensusPercentage(),
                        "providersConsulted", v.getTotalResponses(),
                        "timestamp", v.getTimestamp()
                    ))
                    .toList()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ========== PRIVATE HELPERS ==========
    
    private User extractUser(String authHeader) throws Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7);
        return authService.validateToken(token);
    }
}
