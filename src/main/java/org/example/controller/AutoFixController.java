package org.example.controller;

import org.example.service.AutoFixLoopService;
import org.example.service.AutoFixLoopService.FixAttempt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Phase 6: Auto-Fix Loop REST API
 * Provides endpoints to trigger auto-fix processes and monitor results
 */
@RestController
@RequestMapping("/api/v1/autofix")
public class AutoFixController {

    @Autowired
    private AutoFixLoopService autoFixLoopService;

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
        health.put("endpoints", new String[]{
            "/api/v1/autofix/fix-error (POST)",
            "/api/v1/autofix/stats (GET)",
            "/api/v1/autofix/attempt/{id} (GET)",
            "/api/v1/autofix/recent (GET)"
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
