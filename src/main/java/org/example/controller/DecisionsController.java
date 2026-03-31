package org.example.controller;

import org.example.model.Decision;
import org.example.service.AgentDecisionLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Phase 6: Enhanced Decisions Controller
 * Manages multi-agent voting, decisions, and self-reflection logging
 * 
 * Features:
 * - Decision logging with confidence tracking
 * - Consensus voting and approval tracking
 * - Decision outcome recording and metrics
 * - Query by agent, project, confidence level
 * - Statistics and pattern learning support
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DecisionsController {

    private static final List<Decision> decisions = new ArrayList<>();

    @Autowired(required = false)
    private AgentDecisionLogger decisionLogger;

    static {
        // Initialize with sample decision
        Decision d1 = new Decision("Optimize Database Queries", "Agent-1");
        d1.setStatus("pending");
        d1.setApprovalRate(66.7);
        decisions.add(d1);
    }

    // ==================== Legacy Endpoints ====================
    
    @GetMapping("/decisions/list")
    public ResponseEntity<?> listDecisions() {
        try {
            return ResponseEntity.ok(decisions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/decisions/create")
    public ResponseEntity<?> createDecision(@RequestBody Decision decision) {
        try {
            if (decision.getId() == null) {
                decision.setId(UUID.randomUUID().toString());
            }
            if (decision.getCreatedAt() == null) {
                decision.setCreatedAt(LocalDateTime.now());
            }
            decisions.add(decision);
            return ResponseEntity.ok(Map.of("success", true, "id", decision.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Phase 6: Agent Decision Logging ====================

    /**
     * POST /api/v1/decisions/log
     * Log a new agent decision with confidence tracking
     */
    @PostMapping("/v1/decisions/log")
    public ResponseEntity<Map<String, Object>> logDecision(
            @RequestParam String agent,
            @RequestParam String taskType,
            @RequestParam String projectId,
            @RequestParam String decision,
            @RequestParam String reasoning,
            @RequestParam(defaultValue = "0.7") float confidence,
            @RequestParam(required = false) String[] alternatives) {
        
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }
        
        List<String> altList = alternatives != null ? Arrays.asList(alternatives) : new ArrayList<>();
        
        AgentDecisionLogger.AgentDecision result = decisionLogger.logDecision(
            agent, taskType, projectId, decision, reasoning, confidence, altList);
        
        Map<String, Object> response = new HashMap<>();
        response.put("decisionId", result.decisionId);
        response.put("agent", result.agent);
        response.put("decision", result.decision);
        response.put("confidence", result.confidence);
        response.put("timestamp", result.timestamp);
        response.put("status", "logged");
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/decisions/{decisionId}/vote
     * Record consensus voting for a decision
     */
    @PostMapping("/v1/decisions/{decisionId}/vote")
    public ResponseEntity<Map<String, Object>> recordVote(
            @PathVariable String decisionId,
            @RequestBody VoteRequest voteRequest) {
        
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }
        
        List<AgentDecisionLogger.AgentVote> votes = new ArrayList<>();
        for (VoteData voteData : voteRequest.votes) {
            votes.add(new AgentDecisionLogger.AgentVote(
                voteData.agent, voteData.approves, voteData.confidence, voteData.reasoning));
        }
        
        decisionLogger.logConsensusVote(decisionId, votes, voteRequest.threshold);
        
        Map<String, Object> response = new HashMap<>();
        response.put("decisionId", decisionId);
        response.put("votesRecorded", votes.size());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/decisions/{decisionId}/apply
     * Mark decision as applied
     */
    @PostMapping("/v1/decisions/{decisionId}/apply")
    public ResponseEntity<Map<String, Object>> applyDecision(
            @PathVariable String decisionId,
            @RequestParam(defaultValue = "0") long durationMs) {
        
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }
        
        decisionLogger.markDecisionApplied(decisionId, durationMs);
        
        Map<String, Object> response = new HashMap<>();
        response.put("decisionId", decisionId);
        response.put("status", "applied");
        response.put("durationMs", durationMs);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/decisions/{decisionId}/outcome
     * Record decision outcome and metrics
     */
    @PostMapping("/v1/decisions/{decisionId}/outcome")
    public ResponseEntity<Map<String, Object>> recordOutcome(
            @PathVariable String decisionId,
            @RequestBody OutcomeRequest outcomeRequest) {
        
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }
        
        decisionLogger.recordDecisionOutcome(
            decisionId,
            outcomeRequest.result,
            outcomeRequest.outcome,
            outcomeRequest.successMetric,
            outcomeRequest.patterns);
        
        Map<String, Object> response = new HashMap<>();
        response.put("decisionId", decisionId);
        response.put("result", outcomeRequest.result);
        response.put("successMetric", outcomeRequest.successMetric);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/decisions/project/{projectId}
     * Get all decisions for a project
     */
    @GetMapping("/v1/decisions/project/{projectId}")
    public ResponseEntity<Map<String, Object>> getProjectDecisions(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "100") int limit) {
        
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }
        
        List<AgentDecisionLogger.AgentDecision> decisions = decisionLogger.getProjectDecisions(projectId);
        decisions = decisions.stream()
            .skip(Math.max(0, decisions.size() - limit))
            .collect(java.util.stream.Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("projectId", projectId);
        response.put("totalDecisions", decisions.size());
        response.put("decisions", decisions);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/decisions/agent/{agentName}
     * Get all decisions made by an agent
     */
    @GetMapping("/v1/decisions/agent/{agentName}")
    public ResponseEntity<Map<String, Object>> getAgentDecisions(
            @PathVariable String agentName,
            @RequestParam(defaultValue = "50") int limit) {
        
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }
        
        List<AgentDecisionLogger.AgentDecision> decisions = decisionLogger.getAgentDecisions(agentName);
        decisions = decisions.stream()
            .skip(Math.max(0, decisions.size() - limit))
            .collect(java.util.stream.Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("agent", agentName);
        response.put("totalDecisions", decisions.size());
        response.put("decisions", decisions);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/decisions/stats
     * Get decision statistics and metrics
     */
    @GetMapping("/v1/decisions/stats")
    public ResponseEntity<Map<String, Object>> getDecisionStats() {
        if (decisionLogger == null) {
            return ResponseEntity.status(501).body(errorResponse("AgentDecisionLogger not available"));
        }
        
        Map<String, Object> stats = decisionLogger.getDecisionStats();
        return ResponseEntity.ok(stats);
    }

    // ==================== Request/Response Classes ====================

    public static class VoteRequest {
        public List<VoteData> votes;
        public float threshold = 0.7f; // 70% consensus
    }

    public static class VoteData {
        public String agent;
        public boolean approves;
        public float confidence;
        public String reasoning;
    }

    public static class OutcomeRequest {
        public String result; // SUCCESS, FAILURE, PARTIAL
        public String outcome;
        public double successMetric; // 0.0 - 1.0
        public String[] patterns;
    }

    /**
     * Helper: Generate error response
     */
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
