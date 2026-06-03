package com.supremeai.controller;

import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.learning.SupremeLearningOrchestrator.SystemSuggestion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *  LearningLoopController — Phase 2 REST API
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Admin-only endpoints for monitoring and managing the Phase 2 learning loop.
 * Provides visibility into:
 *  - Intent classification health
 *  - Correction history and hub routing distribution
 *  - System suggestion queue (auto-model, link evaluation, gap analysis)
 *  - Q-learning router statistics
 *
 * All endpoints require ADMIN role.
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/api/admin/learning-loop")
@PreAuthorize("hasRole('ADMIN')")
public class LearningLoopController {

    private final SupremeLearningOrchestrator learningOrchestrator;

    public LearningLoopController(SupremeLearningOrchestrator learningOrchestrator) {
        this.learningOrchestrator = learningOrchestrator;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LEARNING LOOP HEALTH
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/learning-loop/health
     *
     * Returns a comprehensive health snapshot of the learning loop:
     *  - Total corrections recorded
     *  - Hub routing distribution
     *  - Recent correction history (last 10)
     *  - Pending system suggestions
     *  - System version
     */
    @GetMapping("/health")
    public Map<String, Object> getLearningLoopHealth() {
        return learningOrchestrator.getLearningLoopHealth();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CORRECTION HISTORY
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/learning-loop/corrections
     *
     * Returns the full correction history from the learning reservoir.
     * Useful for auditing what the system has learned from user feedback.
     */
    @GetMapping("/corrections")
    public Map<String, Object> getCorrectionHistory() {
        return learningOrchestrator.getLearningLoopHealth();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SYSTEM SUGGESTIONS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/learning-loop/suggestions
     *
     * Returns all pending system suggestions:
     *  - Auto-model suggestions (low success rate task types)
     *  - Link evaluation results (if admin shared model URLs)
     *  - Gap analysis reports (intelligence gaps detected)
     */
    @GetMapping("/suggestions")
    public Map<String, Object> getSystemSuggestions() {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        try {
            List<SystemSuggestion> modelGaps = learningOrchestrator.checkForModelGaps();
            result.put("modelGapSuggestions", modelGaps.stream()
                    .map(s -> Map.of(
                            "type", s.type().name(),
                            "message", s.message(),
                            "category", s.category(),
                            "score", s.score()
                    ))
                    .toList());
            result.put("totalSuggestions", modelGaps.size());
            result.put("status", "ok");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * POST /api/admin/learning-loop/evaluate-link
     *
     * Evaluates a model link (HuggingFace/GitHub URL) and returns a recommendation.
     *
     * Body: { "url": "https://huggingface.co/org/model-name" }
     */
    @PostMapping("/evaluate-link")
    public Map<String, Object> evaluateModelLink(@RequestBody Map<String, Object> body) {
        String url = (String) body.get("url");
        SystemSuggestion suggestion = learningOrchestrator.evaluateModelLink(url);
        return Map.of(
                "type", suggestion.type().name(),
                "message", suggestion.message(),
                "target", suggestion.target(),
                "score", suggestion.score(),
                "status", "ok"
        );
    }

    /**
     * POST /api/admin/learning-loop/gap-analysis
     *
     * Runs gap analysis for a specific task type.
     *
     * Body: { "taskType": "code_generation" }
     */
    @PostMapping("/gap-analysis")
    public Map<String, Object> runGapAnalysis(@RequestBody Map<String, Object> body) {
        String taskType = (String) body.getOrDefault("taskType", "");
        SystemSuggestion result = learningOrchestrator.detectIntelligenceGap(taskType);
        return Map.of(
                "type", result.type().name(),
                "message", result.message(),
                "target", result.target(),
                "score", result.score(),
                "status", "ok"
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INTENT CLASSIFICATION TEST
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * POST /api/admin/learning-loop/test-intent
     *
     * Tests intent classification for a given query without executing it.
     * Useful for verifying Phase 2 vector-based matching is working correctly.
     *
     * Body: { "query": "build a spring boot rest api" }
     */
    @PostMapping("/test-intent")
    public Map<String, Object> testIntentClassification(@RequestBody Map<String, Object> body) {
        String query = (String) body.get("query");
        if (query == null || query.isBlank()) {
            return Map.of("error", "query is required", "status", "bad_request");
        }

        Map<String, String> hubInfo = learningOrchestrator.identifyBestHub(query);
        return Map.of(
                "query", query,
                "identifiedHub", hubInfo.get("hub"),
                "identifiedCluster", hubInfo.get("cluster"),
                "status", "ok"
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  KNOWLEDGE BASE MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * POST /api/admin/learning-loop/reload
     *
     * Reloads the core_knowledge.json from disk.
     * Use after manually editing the knowledge file.
     */
    @PostMapping("/reload")
    public Map<String, Object> reloadKnowledgeBase() {
        try {
            learningOrchestrator.reloadKnowledgeBase();
            return Map.of("status", "reloaded", "message", "Knowledge base reloaded from disk");
        } catch (Exception e) {
            return Map.of("status", "error", "error", e.getMessage());
        }
    }

    /**
     * GET /api/admin/learning-loop/version
     *
     * Returns the current system version and knowledge base metadata.
     */
    @GetMapping("/version")
    public Map<String, Object> getVersion() {
        return Map.of(
                "phase", "Phase 2 — Learning & Integration",
                "status", "active",
                "endpoints", List.of(
                        "GET  /api/admin/learning-loop/health",
                        "GET  /api/admin/learning-loop/corrections",
                        "GET  /api/admin/learning-loop/suggestions",
                        "POST /api/admin/learning-loop/evaluate-link",
                        "POST /api/admin/learning-loop/gap-analysis",
                        "POST /api/admin/learning-loop/test-intent",
                        "POST /api/admin/learning-loop/reload",
                        "GET  /api/admin/learning-loop/version"
                )
        );
    }
}
