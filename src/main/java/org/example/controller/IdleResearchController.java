package org.example.controller;

import org.example.service.IdleResearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Idle Research Controller - Admin visibility into SupremeAI's self-study
 */
@RestController
@RequestMapping("/api/research")
public class IdleResearchController {
    private static final Logger logger = LoggerFactory.getLogger(IdleResearchController.class);

    @Autowired
    private IdleResearchService researchService;

    /**
     * GET /api/research/stats - View research statistics & status
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(researchService.getResearchStats());
    }

    /**
     * POST /api/research/trigger - Admin force-trigger a research cycle
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerResearch() {
        logger.info("🔬 Admin triggered manual research cycle");
        Map<String, Object> report = researchService.triggerResearchNow();
        return ResponseEntity.ok(report);
    }

    /**
     * POST /api/research/queue - Queue a specific research topic
     */
    @PostMapping("/queue")
    public ResponseEntity<Map<String, Object>> queueTopic(@RequestBody Map<String, String> request) {
        String domain = request.getOrDefault("domain", "GENERAL");
        String question = request.get("question");

        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "question is required"
            ));
        }

        // Basic input validation
        if (question.length() > 1000) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "question too long (max 1000 chars)"
            ));
        }

        researchService.queueResearchTopic(domain, question, "ADMIN_QUEUE");
        return ResponseEntity.ok(Map.of(
            "status", "queued",
            "domain", domain,
            "question", question
        ));
    }

    /**
     * GET /api/research/history - View all research history
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(researchService.getResearchHistory());
    }

    /**
     * POST /api/research/enable - Admin enables the auto-learning system
     */
    @PostMapping("/enable")
    public ResponseEntity<Map<String, Object>> enableLearning() {
        researchService.enableLearning();
        logger.info("✅ Auto-learning system enabled via admin API");
        return ResponseEntity.ok(Map.of(
            "status", "enabled",
            "learningEnabled", true,
            "message", "Auto-learning system is now ACTIVE. It will begin when the system has been idle for 1 hour."
        ));
    }

    /**
     * POST /api/research/disable - Admin disables the auto-learning system
     */
    @PostMapping("/disable")
    public ResponseEntity<Map<String, Object>> disableLearning() {
        researchService.disableLearning();
        logger.info("🛑 Auto-learning system disabled via admin API");
        return ResponseEntity.ok(Map.of(
            "status", "disabled",
            "learningEnabled", false,
            "message", "Auto-learning system is now PAUSED. No automatic research will run."
        ));
    }

    /**
     * GET /api/research/quota - Firebase free-tier quota usage
     */
    @GetMapping("/quota")
    public ResponseEntity<Map<String, Object>> getFirebaseQuota() {
        return ResponseEntity.ok(researchService.getFirebaseQuotaStatus());
    }

    /**
     * GET /api/research/learning-limit - Get current learning limit per cycle
     */
    @GetMapping("/learning-limit")
    public ResponseEntity<Map<String, Object>> getLearningLimit() {
        return ResponseEntity.ok(Map.of(
            "maxTopicsPerCycle", researchService.getMaxTopicsPerCycle(),
            "min", 1,
            "max", 50
        ));
    }

    /**
     * POST /api/research/learning-limit - Admin sets learning limit per cycle
     * Body: { "limit": 5 }
     */
    @PostMapping("/learning-limit")
    public ResponseEntity<Map<String, Object>> setLearningLimit(@RequestBody Map<String, Object> request) {
        Object limitObj = request.get("limit");
        if (limitObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "limit is required"));
        }
        int limit;
        try {
            limit = ((Number) limitObj).intValue();
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "limit must be a number"));
        }
        if (limit < 1 || limit > 50) {
            return ResponseEntity.badRequest().body(Map.of("error", "limit must be between 1 and 50"));
        }
        researchService.setMaxTopicsPerCycle(limit);
        logger.info("📚 Admin set learning limit to {} topics per cycle", limit);
        return ResponseEntity.ok(Map.of(
            "status", "updated",
            "maxTopicsPerCycle", limit,
            "message", "Learning limit updated to " + limit + " topics per cycle"
        ));
    }

    /**
     * GET /api/research/settings - Get all admin-configurable learning settings
     */
    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new java.util.LinkedHashMap<>();
        settings.put("maxTopicsPerCycle", researchService.getMaxTopicsPerCycle());
        settings.put("cycleIntervalMinutes", researchService.getCycleIntervalMinutes());
        settings.put("dailyWriteLimit", researchService.getDailyWriteLimit());
        settings.put("dailyReadLimit", researchService.getDailyReadLimit());
        settings.put("learningEnabled", researchService.isLearningEnabled());
        return ResponseEntity.ok(settings);
    }

    /**
     * POST /api/research/settings - Update learning settings
     * Body: { "maxTopicsPerCycle": 5, "cycleIntervalMinutes": 10, "dailyWriteLimit": 20000, "dailyReadLimit": 60000 }
     * All fields optional — only provided fields are updated.
     */
    @PostMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> request) {
        Map<String, Object> updated = new java.util.LinkedHashMap<>();

        if (request.containsKey("maxTopicsPerCycle")) {
            int val = ((Number) request.get("maxTopicsPerCycle")).intValue();
            researchService.setMaxTopicsPerCycle(val);
            updated.put("maxTopicsPerCycle", researchService.getMaxTopicsPerCycle());
        }
        if (request.containsKey("cycleIntervalMinutes")) {
            long val = ((Number) request.get("cycleIntervalMinutes")).longValue();
            researchService.setCycleIntervalMinutes(val);
            updated.put("cycleIntervalMinutes", researchService.getCycleIntervalMinutes());
        }
        if (request.containsKey("dailyWriteLimit")) {
            long val = ((Number) request.get("dailyWriteLimit")).longValue();
            researchService.setDailyWriteLimit(val);
            updated.put("dailyWriteLimit", researchService.getDailyWriteLimit());
        }
        if (request.containsKey("dailyReadLimit")) {
            long val = ((Number) request.get("dailyReadLimit")).longValue();
            researchService.setDailyReadLimit(val);
            updated.put("dailyReadLimit", researchService.getDailyReadLimit());
        }

        if (updated.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No valid settings provided"));
        }

        logger.info("⚙️ Admin updated learning settings: {}", updated);
        updated.put("status", "updated");
        return ResponseEntity.ok(updated);
    }
}
