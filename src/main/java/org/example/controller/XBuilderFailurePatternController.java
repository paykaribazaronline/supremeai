package org.example.controller;

import org.example.service.XBuilderFailurePatternService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * X-Builder Failure Pattern Management API
 * Provides endpoints for tracking, analyzing, and learning from code generation failures
 * Helps X-Builder (code generation AI) improve by understanding failure patterns
 */
@RestController
@RequestMapping("/api/xbuilder/failures")
public class XBuilderFailurePatternController {

    @Autowired
    private XBuilderFailurePatternService failurePatternService;

    /**
     * Record a code generation failure
     * POST /api/xbuilder/failures/record
     */
    @PostMapping("/record")
    public Map<String, Object> recordFailure(@RequestBody Map<String, String> request) {
        String failureId = failurePatternService.recordFailure(
            request.get("projectId"),
            request.get("componentName"),
            request.get("framework"),
            request.get("failureType"),
            request.get("failureReason"),
            request.get("generatedCode"),
            request.get("errorMessage")
        );

        return Map.of(
            "success", true,
            "failureId", failureId,
            "recorded", failureId != null,
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Mark a failure as fixed and record the solution
     * POST /api/xbuilder/failures/{failureId}/fixed
     */
    @PostMapping("/{failureId}/fixed")
    public Map<String, Object> markFailureFixed(
            @PathVariable String failureId,
            @RequestBody Map<String, Object> request) {
        
        String fixApplied = (String) request.get("fixApplied");
        Boolean success = (Boolean) request.getOrDefault("success", true);

        return failurePatternService.markFailureFixed(failureId, fixApplied, success);
    }

    /**
     * Get failure details
     * GET /api/xbuilder/failures/{failureId}
     */
    @GetMapping("/{failureId}")
    public Map<String, Object> getFailureDetails(@PathVariable String failureId) {
        return failurePatternService.getFailureDetails(failureId);
    }

    /**
     * Get failure statistics by framework
     * GET /api/xbuilder/failures/stats/framework?framework=React
     */
    @GetMapping("/stats/framework")
    public Map<String, Object> getFailureStatsByFramework(
            @RequestParam String framework) {
        return failurePatternService.getFailureStatsByFramework(framework);
    }

    /**
     * Get all failure patterns (insights)
     * GET /api/xbuilder/failures/patterns
     */
    @GetMapping("/patterns")
    public List<Map<String, Object>> getFailurePatterns() {
        return failurePatternService.getFailurePatterns();
    }

    /**
     * Get critical failure patterns (top blockers)
     * GET /api/xbuilder/failures/patterns/critical?limit=5
     */
    @GetMapping("/patterns/critical")
    public List<Map<String, Object>> getCriticalPatterns(
            @RequestParam(defaultValue = "5") int limit) {
        return failurePatternService.getCriticalPatterns(limit);
    }

    /**
     * Get AI recommendations to avoid failures
     * GET /api/xbuilder/failures/recommendations?framework=React
     */
    @GetMapping("/recommendations")
    public Map<String, Object> getFailureRecommendations(
            @RequestParam String framework) {
        return failurePatternService.getFailureRecommendations(framework);
    }

    /**
     * Get X-Builder health score (0-100)
     * Indicates how well X-Builder is performing
     * GET /api/xbuilder/failures/health
     */
    @GetMapping("/health")
    public Map<String, Object> getXBuilderHealthScore() {
        return failurePatternService.getXBuilderHealthScore();
    }

    /**
     * Get database statistics
     * GET /api/xbuilder/failures/stats
     */
    @GetMapping("/stats")
    public Map<String, Object> getDatabaseStats() {
        return failurePatternService.getDatabaseStats();
    }

    /**
     * Clean up old failure records
     * POST /api/xbuilder/failures/cleanup?daysToKeep=30
     */
    @PostMapping("/cleanup")
    public Map<String, Object> cleanupOldRecords(
            @RequestParam(defaultValue = "30") int daysToKeep) {
        failurePatternService.cleanupOldRecords(daysToKeep);
        return Map.of(
            "success", true,
            "message", String.format("Cleaned up records older than %d days", daysToKeep),
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Health check + monitoring dashboard
     * GET /api/xbuilder/failures/dashboard
     */
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        return Map.of(
            "healthScore", failurePatternService.getXBuilderHealthScore(),
            "patterns", failurePatternService.getCriticalPatterns(5),
            "stats", failurePatternService.getDatabaseStats(),
            "timestamp", System.currentTimeMillis()
        );
    }
}
