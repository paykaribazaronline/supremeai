package org.example.controller;

import org.example.service.AutoFixLoopService;
import org.example.service.AutoFixLoopService.FixAttempt;
import org.example.service.AutoFixDecisionIntegrator;
import org.example.service.ErrorDetector;
import org.example.service.ErrorAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 6: Auto-Fix Loop REST API
 * Provides endpoints to trigger auto-fix processes and monitor results
 * Week 3-4 Enhancement: Integrated decision logging and consensus tracking
 */
@RestController
@RequestMapping("/api/v1/autofix")
public class AutoFixController {

    @Autowired
    private AutoFixLoopService autoFixLoopService;
    
    @Autowired(required = false)
    private AutoFixDecisionIntegrator decisionIntegrator;
    
    private final Map<String, AutoFixDecisionIntegrator.IntegratedFixResult> integratedResults = 
        new ConcurrentHashMap<>();

    /**
     * POST /api/v1/autofix/fix-error
     * Trigger auto-fix for a given error
     * 
     * Request body:
     * {
     *   "error": "NullPointerException at line 42",
     *   "context": {
     *     "language": "java",
     *     "framework": "spring-boot",
     *     "file": "MyService.java"
     *   }
     * }
     * 
     * @return Fix attempt with results
     */
    @PostMapping("/fix-error")
    public ResponseEntity<Map<String, Object>> fixError(@RequestBody Map<String, Object> request) {
        try {
            String error = (String) request.get("error");
            @SuppressWarnings("unchecked")
            Map<String, Object> context = (Map<String, Object>) request.getOrDefault("context", new HashMap<>());
            
            if (error == null || error.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(errorResponse("Error message is required"));
            }
            
            FixAttempt attempt = autoFixLoopService.autoFixError(error, context);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", attempt.success);
            response.put("attemptId", attempt.id);
            response.put("error", attempt.error);
            response.put("message", attempt.resultMessage);
            
            if (attempt.appliedFix != null) {
                response.put("appliedFix", new HashMap<String, Object>() {{
                    put("description", attempt.appliedFix.description);
                    put("technique", attempt.appliedFix.technique);
                    put("confidence", attempt.appliedFix.confidence);
                    put("code", attempt.appliedFix.code);
                }});
            }
            
            response.put("candidatesGenerated", attempt.candidates.size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(errorResponse("Error during auto-fix: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/autofix/stats
     * Get auto-fix success statistics
     * 
     * @return Success rate, total attempts, breakdown
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAutoFixStats() {
        try {
            Map<String, Object> stats = autoFixLoopService.getFixStats();
            stats.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(errorResponse("Error retrieving stats: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/autofix/attempt/{attemptId}
     * Get details of a specific fix attempt
     * 
     * @param attemptId ID of the fix attempt
     * @return Detailed attempt information with candidates
     */
    @GetMapping("/attempt/{attemptId}")
    public ResponseEntity<Map<String, Object>> getAttemptDetails(@PathVariable String attemptId) {
        try {
            FixAttempt attempt = autoFixLoopService.getAttemptDetails(attemptId);
            
            if (attempt == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("attemptId", attempt.id);
            response.put("error", attempt.error);
            response.put("success", attempt.success);
            response.put("resultMessage", attempt.resultMessage);
            response.put("timestamp", attempt.timestamp);
            
            // Add candidates with details
            List<Map<String, Object>> candidatesData = new ArrayList<>();
            for (AutoFixLoopService.FixCandidate candidate : attempt.candidates) {
                candidatesData.add(new HashMap<String, Object>() {{
                    put("id", candidate.id);
                    put("description", candidate.description);
                    put("technique", candidate.technique);
                    put("confidence", candidate.confidence);
                    put("tested", candidate.tested);
                    put("passed", candidate.passed);
                    put("estimatedTimeMs", candidate.estimatedTimeMs);
                    put("code", candidate.code);
                }});
            }
            response.put("candidates", candidatesData);
            
            if (attempt.appliedFix != null) {
                response.put("appliedFix", new HashMap<String, Object>() {{
                    put("id", attempt.appliedFix.id);
                    put("description", attempt.appliedFix.description);
                    put("technique", attempt.appliedFix.technique);
                    put("confidence", attempt.appliedFix.confidence);
                }});
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(errorResponse("Error retrieving attempt: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/autofix/recent?limit=10
     * Get recent fix attempts
     * 
     * @param limit Number of recent attempts to return
     * @return List of recent attempts
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentAttempts(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<FixAttempt> attempts = autoFixLoopService.getRecentAttempts(Math.min(limit, 50));
            
            List<Map<String, Object>> attemptsList = new ArrayList<>();
            for (FixAttempt attempt : attempts) {
                attemptsList.add(new HashMap<String, Object>() {{
                    put("attemptId", attempt.id);
                    put("error", attempt.error.substring(0, Math.min(100, attempt.error.length())));
                    put("success", attempt.success);
                    put("timestamp", attempt.timestamp);
                    put("candidatesCount", attempt.candidates.size());
                    put("message", attempt.resultMessage);
                }});
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("attempts", attemptsList);
            response.put("count", attemptsList.size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(errorResponse("Error retrieving recent attempts: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/autofix/fix-with-decisions
     * Phase 6 Week 3-4: Auto-fix with integrated decision logging
     * 
     * Request body:
     * {
     *   "error": "NullPointerException at line 42",
     *   "projectId": "myapp",
     *   "language": "java",
     *   "framework": "spring-boot"
     * }
     * 
     * Response includes decision ID and voting records
     */
    @PostMapping("/fix-with-decisions")
    public ResponseEntity<Map<String, Object>> fixWithDecisions(
            @RequestParam String error,
            @RequestParam String projectId,
            @RequestParam(defaultValue = "java") String language,
            @RequestParam(defaultValue = "spring-boot") String framework) {
        
        if (decisionIntegrator == null) {
            return ResponseEntity.status(501).body(Map.of(
                "error", "Decision integration not available",
                "status", "implementation-pending"
            ));
        }
        
        try {
            // Prepare context
            Map<String, Object> context = new HashMap<>();
            context.put("projectId", projectId);
            context.put("language", language);
            context.put("framework", framework);
            
            // Run integrated fix with decision logging
            AutoFixDecisionIntegrator.IntegratedFixResult result = 
                decisionIntegrator.autoFixWithDecisions(error, context, autoFixLoopService);
            
            integratedResults.put(result.fixId, result);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", result.fixAttempt.success ? "SUCCESS" : "FAILED");
            response.put("fixId", result.fixId);
            response.put("decisionId", result.decisionId);
            response.put("message", result.fixAttempt.resultMessage);
            response.put("decisionLogged", result.decisionLogged);
            response.put("votingRecorded", result.votingRecorded);
            response.put("outcomeRecorded", result.outcomeRecorded);
            response.put("confidence", result.consensusConfidence);
            response.put("totalTime", result.totalTimeMs);
            
            if (result.fixAttempt.appliedFix != null) {
                response.put("appliedFix", Map.of(
                    "description", result.fixAttempt.appliedFix.description,
                    "technique", result.fixAttempt.appliedFix.technique,
                    "confidence", result.fixAttempt.appliedFix.confidence
                ));
            }
            
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(errorResponse("Integration fix failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v1/autofix/integrated/{fixId}
     * Get status of an integrated fix with decision tracking
     */
    @GetMapping("/integrated/{fixId}")
    public ResponseEntity<Map<String, Object>> getIntegratedFixStatus(@PathVariable String fixId) {
        AutoFixDecisionIntegrator.IntegratedFixResult result = integratedResults.get(fixId);
        
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("fixId", fixId);
        response.put("decisionId", result.decisionId);
        response.put("status", result.fixAttempt.success ? "SUCCESS" : "FAILED");
        response.put("message", result.fixAttempt.resultMessage);
        response.put("confidence", result.consensusConfidence);
        response.put("decisionLogged", result.decisionLogged);
        response.put("votingRecorded", result.votingRecorded);
        response.put("outcomeRecorded", result.outcomeRecorded);
        response.put("totalTime", result.totalTimeMs);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/autofix/integrated-stats?projectId=...
     * Get integrated fix statistics with decision tracking
     */
    @GetMapping("/integrated-stats")
    public ResponseEntity<Map<String, Object>> getIntegratedFixStats(
            @RequestParam String projectId) {
        
        long totalFixes = integratedResults.values().stream()
            .filter(r -> r.fixAttempt.timestamp > 0).count();
        
        long successfulFixes = integratedResults.values().stream()
            .filter(r -> r.fixAttempt.success).count();
        
        long withDecisions = integratedResults.values().stream()
            .filter(r -> r.decisionLogged && r.votingRecorded).count();
        
        float successRate = totalFixes > 0 ? 
            (successfulFixes / (float) totalFixes) : 0.0f;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("projectId", projectId);
        stats.put("totalFixAttempts", totalFixes);
        stats.put("successfulFixes", successfulFixes);
        stats.put("failedFixes", totalFixes - successfulFixes);
        stats.put("withDecisionLogging", withDecisions);
        stats.put("successRate", successRate);
        stats.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/v1/autofix/health
     * Check auto-fix service health
     * 
     * @return Service status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AutoFixLoopService");
        health.put("decisionIntegration", decisionIntegrator != null ? "ENABLED" : "DISABLED");
        health.put("endpoints", new String[]{
            "/api/v1/autofix/fix-error (POST) - Basic auto-fix",
            "/api/v1/autofix/fix-with-decisions (POST) - Week 3-4: Integrated decision logging",
            "/api/v1/autofix/stats (GET)",
            "/api/v1/autofix/integrated-stats (GET) - Week 3-4: Decision tracking stats",
            "/api/v1/autofix/attempt/{id} (GET)",
            "/api/v1/autofix/integrated/{id} (GET) - Week 3-4: Integrated fix status"
        });
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    /**
     * Helper: Create error response
     */
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
