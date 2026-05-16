package com.supremeai.controller;

import com.supremeai.learning.SelfLearningRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Learning Router Controller - Phase 4 (Plan 24)
 * Exposes Q-learning router metrics and control endpoints.
 */
@RestController
@RequestMapping("/api/admin/router")
public class RouterController {

    private static final Logger logger = LoggerFactory.getLogger(RouterController.class);

    @Autowired
    private SelfLearningRouter learningRouter;

    /**
     * GET /api/admin/router/stats
     * Get system-wide routing statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRoutingStats() {
        Map<String, Object> stats = learningRouter.getLearningStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/admin/router/agent/{agentId}
     * Get performance statistics for a specific agent.
     */
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<Map<String, Object>> getAgentStats(@PathVariable String agentId) {
        Map<String, Object> stats = learningRouter.getAgentStats(agentId);
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/admin/router/agents
     * Get statistics for all agents.
     */
    @GetMapping("/agents")
    public ResponseEntity<List<Map<String, Object>>> getAllAgentStats() {
        Map<String, Object> allStats = learningRouter.getLearningStats();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> agents = (List<Map<String, Object>>) allStats.getOrDefault(
            "agentStats", List.of());

        return ResponseEntity.ok(agents);
    }

    /**
     * POST /api/admin/router/reset
     * Reset Q-learning table (for testing/re-training).
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetRouter() {
        learningRouter.reset();
        return ResponseEntity.ok(Map.of(
            "status", "RESET",
            "message", "Routing table reset successfully"
        ));
    }

    /**
     * GET /api/admin/router/q-table
     * Get current Q-values for inspection (debugging).
     */
    @GetMapping("/q-table")
    public ResponseEntity<Map<String, Object>> getQTable() {
        Map<String, Object> result = learningRouter.getLearningStats();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/router/routing-decision
     * Get routing decision for a specific task (debugging).
     */
    @GetMapping("/routing-decision")
    public ResponseEntity<Map<String, Object>> getRoutingDecision(
            @RequestParam String taskCategory,
            @RequestParam(required = false) String taskSignature,
            @RequestParam List<String> candidateAgents) {

        SelfLearningRouter.RoutingDecision decision =
            learningRouter.routeTask(taskCategory, taskSignature, candidateAgents);

        return ResponseEntity.ok(Map.of(
            "taskCategory", taskCategory,
            "taskSignature", taskSignature,
            "selectedAgent", decision.agentId,
            "decisionType", decision.decisionType,
            "confidence", decision.confidence,
            "candidateAgents", candidateAgents
        ));
    }
}
