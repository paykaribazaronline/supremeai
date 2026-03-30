package org.example.controller;

import org.example.service.AIRankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 2 Intelligence: AI Provider Ranking Controller
 * REST API for intelligent AI provider selection and performance analysis
 */
@RestController
@RequestMapping("/api/intelligence/ranking")
public class AIRankingController {

    @Autowired(required = false)
    private AIRankingService aiRankingService;

    /**
     * GET /api/intelligence/ranking/performance
     * Get all AI providers ranked by overall performance
     */
    @GetMapping("/performance")
    public ResponseEntity<?> getRankingByPerformance() {
        if (aiRankingService == null) {
            return ResponseEntity.ok(Map.of("message", "AIRankingService not available"));
        }
        return ResponseEntity.ok(Map.of(
            "ranking", aiRankingService.rankAgentsByPerformance(),
            "strategy", "Overall Performance",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * GET /api/intelligence/ranking/task/{taskType}
     * Get providers ranked for specific task type
     */
    @GetMapping("/task/{taskType}")
    public ResponseEntity<?> getRankingForTask(@PathVariable String taskType) {
        if (aiRankingService == null) {
            return ResponseEntity.ok(Map.of("message", "AIRankingService not available"));
        }
        return ResponseEntity.ok(Map.of(
            "taskType", taskType,
            "ranking", aiRankingService.rankAgentsByTaskType(taskType),
            "strategy", "Task-Specific"
        ));
    }

    /**
     * GET /api/intelligence/ranking/cost
     * Get cost-optimized provider ranking (cheapest first)
     */
    @GetMapping("/cost")
    public ResponseEntity<?> getRankingByCost() {
        if (aiRankingService == null) {
            return ResponseEntity.ok(Map.of("message", "AIRankingService not available"));
        }
        return ResponseEntity.ok(Map.of(
            "ranking", aiRankingService.rankAgentsByCost(),
            "strategy", "Cost-Optimized",
            "note", "Cheapest providers first"
        ));
    }

    /**
     * GET /api/intelligence/ranking/speed
     * Get fastest provider ranking
     */
    @GetMapping("/speed")
    public ResponseEntity<?> getRankingBySpeed() {
        if (aiRankingService == null) {
            return ResponseEntity.ok(Map.of("message", "AIRankingService not available"));
        }
        return ResponseEntity.ok(Map.of(
            "ranking", aiRankingService.rankAgentsBySpeed(),
            "strategy", "Speed-Optimized",
            "note", "Fastest response times first"
        ));
    }

    /**
     * GET /api/intelligence/ranking/hybrid?taskType=...
     * Get hybrid ranking combining performance, task-type, and cost
     */
    @GetMapping("/hybrid")
    public ResponseEntity<?> getRankingHybrid(@RequestParam(defaultValue = "code-generation") String taskType) {
        if (aiRankingService == null) {
            return ResponseEntity.ok(Map.of("message", "AIRankingService not available"));
        }
        return ResponseEntity.ok(Map.of(
            "taskType", taskType,
            "ranking", aiRankingService.rankAgentsHybrid(taskType),
            "strategy", "Hybrid (Performance + Task + Cost)"
        ));
    }
}

