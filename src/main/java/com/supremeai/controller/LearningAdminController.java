package com.supremeai.controller;

import com.supremeai.learning.LearningModeControl;
import com.supremeai.learning.LearningQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Learning Management API - Admin only.
 * Controls quotas, modes, and provides operational metrics.
 */
@RestController
@RequestMapping("/api/admin/learning")
@PreAuthorize("hasRole('ADMIN')")
public class LearningAdminController {

    private final LearningModeControl modeControl;
    private final LearningQuotaService quotaService;

    @Autowired
    public LearningAdminController(LearningModeControl modeControl, LearningQuotaService quotaService) {
        this.modeControl = modeControl;
        this.quotaService = quotaService;
    }

    /**
     * Get current learning mode and quota stats.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("mode", modeControl.getCurrentMode().name());
        response.put("modeDescription", modeControl.getCurrentMode().getDescription());
        response.put("emergencyPaused", modeControl.isEmergencyPaused());
        response.put("scrapingAllowed", modeControl.isScrapingAllowed());
        response.put("autoApprovalAllowed", modeControl.isAutoApprovalAllowed());
        response.put("learningAllowed", modeControl.isLearningAllowed());
        response.put("quota", quotaService.getQuotaStats());
        return ResponseEntity.ok(response);
    }

    /**
     * Set learning mode (AGGRESSIVE, BALANCED, MANUAL, PAUSED).
     * POST { "mode": "AGGRESSIVE" }
     */
    @PostMapping("/mode")
    public ResponseEntity<Map<String, String>> setMode(@RequestBody Map<String, String> body) {
        String modeStr = body.get("mode");
        if (modeStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "mode is required"));
        }

        try {
            LearningModeControl.LearningMode mode = LearningModeControl.LearningMode.valueOf(modeStr.toUpperCase());
            modeControl.setMode(mode);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "mode", mode.name(),
                "description", mode.getDescription()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid mode. Use: AGGRESSIVE, BALANCED, MANUAL, PAUSED"
            ));
        }
    }

    /**
     * Emergency pause - immediately stop all learning.
     */
    @PostMapping("/emergency-pause")
    public ResponseEntity<Map<String, String>> emergencyPause() {
        modeControl.emergencyPause();
        return ResponseEntity.ok(Map.of(
            "status", "paused",
            "message", "Emergency pause activated. All learning halted."
        ));
    }

    /**
     * Resume from emergency pause.
     */
    @PostMapping("/resume")
    public ResponseEntity<Map<String, String>> resume() {
        modeControl.resumeFromPause();
        return ResponseEntity.ok(Map.of(
            "status", "resumed",
            "message", "Learning resumed to previous mode: " + modeControl.getCurrentMode()
        ));
    }

    /**
     * Update quota limits (admin override).
     */
    @PostMapping("/quota")
    public ResponseEntity<Map<String, Object>> updateQuota(@RequestBody QuotaUpdateRequest request) {
        if (request.getPerUserDailyMax() != null) {
            quotaService.setPerUserDailyMax(request.getPerUserDailyMax());
        }
        if (request.getGlobalDailyMax() != null) {
            quotaService.setGlobalDailyMax(request.getGlobalDailyMax());
        }
        return ResponseEntity.ok(Map.of(
            "status", "updated",
            "quota", quotaService.getQuotaStats()
        ));
    }

    /**
     * Get quota statistics.
     */
    @GetMapping("/quota")
    public ResponseEntity<Map<String, Object>> getQuotaStats() {
        return ResponseEntity.ok(quotaService.getQuotaStats());
    }

    /**
     * Trigger a manual learning cycle (for MANUAL mode or testing).
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> triggerLearning() {
        if (!modeControl.allowManualTrigger()) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Manual trigger not allowed in current mode: " + modeControl.getCurrentMode()
            ));
        }
        // TODO: Kick off ActiveInternetScraper and learning pipeline
        return ResponseEntity.ok(Map.of(
            "status", "triggered",
            "message", "Learning cycle started (implementation pending)"
        ));
    }

    // DTO for quota updates
    public static class QuotaUpdateRequest {
        private Integer perUserDailyMax;
        private Integer globalDailyMax;

        public Integer getPerUserDailyMax() { return perUserDailyMax; }
        public void setPerUserDailyMax(Integer perUserDailyMax) { this.perUserDailyMax = perUserDailyMax; }
        public Integer getGlobalDailyMax() { return globalDailyMax; }
        public void setGlobalDailyMax(Integer globalDailyMax) { this.globalDailyMax = globalDailyMax; }
    }
}
