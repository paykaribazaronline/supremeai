package com.supremeai.controller;

import com.supremeai.service.UsageOptimizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for API usage optimization.
 *
 * Endpoints:
 *   GET  /api/optimization/usage          - Get usage summary
 *   POST /api/optimization/select-model    - Get recommended model for a task
 *   GET  /api/optimization/cache/stats     - Get cache statistics
 */
@RestController
@RequestMapping("/api/optimization")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UsageOptimizationController {

    @Autowired
    private UsageOptimizationService optimizationService;

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Not authenticated");
        }
        return auth.getName();
    }

    /**
     * GET /api/optimization/usage - Get current user's usage summary.
     */
    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> getUsageSummary() {
        String userId = getCurrentUserId();
        Map<String, Object> summary = optimizationService.getUserUsageSummary(userId);
        return ResponseEntity.ok(summary);
    }

    /**
     * POST /api/optimization/select-model - Get recommended model for a task.
     * Body: { "complexity": "simple|moderate|complex|critical" }
     */
    @PostMapping("/select-model")
    public ResponseEntity<Map<String, Object>> selectModel(@RequestBody Map<String, String> body) {
        String userId = getCurrentUserId();
        String complexity = body.getOrDefault("complexity", "moderate");

        Optional<UsageOptimizationService.SelectedModel> selected =
                optimizationService.selectModelForTask(userId, complexity);

        if (selected.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "status", "no_keys",
                    "message", "No API keys configured. Add API keys in the API Keys Manager."
            ));
        }

        UsageOptimizationService.SelectedModel model = selected.get();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("modelId", model.modelId);
        response.put("provider", model.provider);
        response.put("baseUrl", model.baseUrl);
        // Don't expose the actual API key in the response
        response.put("hasKey", true);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/optimization/cache/stats - Get cache hit/miss statistics.
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        String userId = getCurrentUserId();
        Map<String, Object> summary = optimizationService.getUserUsageSummary(userId);
        Map<String, Object> cacheStats = new LinkedHashMap<>();
        cacheStats.put("cacheSize", summary.get("cacheSize"));
        cacheStats.put("cacheTTLMinutes", summary.get("cacheTTLMinutes"));
        return ResponseEntity.ok(cacheStats);
    }
}
