package com.supremeai.controller;

import com.supremeai.learning.LearningModeControl;
import com.supremeai.learning.LearningQuotaService;
import com.supremeai.learning.FocusDetectorService;
import com.supremeai.model.LearningSource;
import com.supremeai.repository.LearningSourceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.supremeai.learning.active.ActiveInternetScraper;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.net.URL;

@RestController
@RequestMapping("/api/admin/learning")
@PreAuthorize("hasRole('ADMIN')")
public class LearningAdminController {

    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(15);

    private final LearningModeControl modeControl;
    private final LearningQuotaService quotaService;
    private final ActiveInternetScraper scraper;
    private final com.supremeai.service.ConfigService configService;
    private final com.supremeai.service.SelfImprovementService selfImprovementService;
    private final LearningSourceRepository sourceRepository;
    private final FocusDetectorService focusDetector;
public LearningAdminController(LearningModeControl modeControl,
                                    LearningQuotaService quotaService,
                                    ActiveInternetScraper scraper,
                                    com.supremeai.service.ConfigService configService,
                                    com.supremeai.service.SelfImprovementService selfImprovementService,
                                    LearningSourceRepository sourceRepository,
                                    FocusDetectorService focusDetector) {
        this.modeControl = modeControl;
        this.quotaService = quotaService;
        this.scraper = scraper;
        this.configService = configService;
        this.selfImprovementService = selfImprovementService;
        this.sourceRepository = sourceRepository;
        this.focusDetector = focusDetector;
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
        
        // Add dynamic interval from ConfigService
        response.put("learningIntervalMinutes", configService.getEffectiveSetting("learning_interval_minutes", 60L));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update the learning interval (minutes).
     */
    @PostMapping("/interval")
    public ResponseEntity<Map<String, Object>> updateInterval(@RequestBody Map<String, Object> body) {
        Object intervalObj = body.get("interval");
        if (intervalObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "interval is required"));
        }

        long interval;
        try {
            interval = Long.parseLong(intervalObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid interval value"));
        }

        configService.updateSetting("learning_interval_minutes", interval)
            .subscribe(); // Fire and forget update
        return ResponseEntity.ok(Map.of("status", "success", "interval", interval));
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
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            var issues = scraper.scrapeTrendingIssues();
            selfImprovementService.ingestScrapedIssues(issues);
        });
        return ResponseEntity.ok(Map.of(
            "status", "triggered",
            "message", "Learning cycle started successfully with active scraping"
        ));
    }

    // ===== LEARNING SOURCES MANAGEMENT =====

    /**
     * GET /api/admin/learning/sources
     * Returns all registered learning sources.
     */
    @GetMapping("/sources")
    public ResponseEntity<List<Map<String, Object>>> getAllSources() {
        List<Map<String, Object>> result = sourceRepository.findAll()
                .map(this::toSourceMap)
                .collectList()
                .block(BLOCK_TIMEOUT);
        return ResponseEntity.ok(result != null ? result : List.of());
    }

    /**
     * POST /api/admin/learning/sources
     * Add a new learning source. Auto-detects topic from URL.
     * Body: { "url": "https://example.com", "manualFocus": "marketing", "priority": 5, "notes": "..." }
     */
    @PostMapping("/sources")
    public ResponseEntity<Map<String, Object>> addSource(@RequestBody Map<String, Object> body) {
        String url = (String) body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error", "message", "URL is required"
            ));
        }

        // Validate URL format
        try {
            new URL(url);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error", "message", "Invalid URL format"
            ));
        }

        // Check if already exists
        boolean exists = sourceRepository.findAll()
                .filter(s -> s.getUrl() != null && s.getUrl().equalsIgnoreCase(url))
                .count()
                .map(cnt -> cnt > 0)
                .onErrorReturn(false)
                .block(BLOCK_TIMEOUT);
        if (exists) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error", "message", "This URL is already in the learning sources"
            ));
        }

        // Smart auto-detection
        String detectedFocus = focusDetector.detectFocus(url);
        String domain = focusDetector.extractDomain(url);
        String manualFocus = (String) body.get("manualFocus");

        LearningSource source = new LearningSource();
        source.setUrl(url);
        source.setDomain(domain);
        source.setDetectedFocus(detectedFocus);
        source.setManualFocus(manualFocus);
        source.setPriority(body.get("priority") != null ? ((Number) body.get("priority")).intValue() : 5);
        source.setEnabled(true);
        source.setNotes((String) body.get("notes"));

        LearningSource saved = sourceRepository.save(source).block(BLOCK_TIMEOUT);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Learning source added. Detected focus: " + saved.getEffectiveFocus(),
                "source", toSourceMap(saved)
        ));
    }

    /**
     * POST /api/admin/learning/sources/detect-focus
     * Preview what focus the system would detect for a URL (before adding).
     * Body: { "url": "https://example.com" }
     */
    @PostMapping("/sources/detect-focus")
    public ResponseEntity<Map<String, Object>> previewFocus(@RequestBody Map<String, Object> body) {
        String url = (String) body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error", "message", "URL is required"
            ));
        }

        String detectedFocus = focusDetector.detectFocus(url);
        String domain = focusDetector.extractDomain(url);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "url", url,
                "domain", domain,
                "detectedFocus", detectedFocus,
                "availableFocusAreas", focusDetector.getAllFocusAreas()
        ));
    }

    /**
     * DELETE /api/admin/learning/sources/{id}
     * Remove a learning source.
     */
    @DeleteMapping("/sources/{id}")
    public ResponseEntity<Map<String, String>> deleteSource(@PathVariable String id) {
        LearningSource existing = sourceRepository.findById(id).block(BLOCK_TIMEOUT);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        sourceRepository.delete(existing);
        return ResponseEntity.ok(Map.of(
                "status", "deleted",
                "id", id,
                "message", "Learning source removed"
        ));
    }

    /**
     * POST /api/admin/learning/sources/{id}/toggle
     * Enable or disable a learning source.
     */
    @PostMapping("/sources/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleSource(@PathVariable String id) {
        LearningSource existing = sourceRepository.findById(id).block(BLOCK_TIMEOUT);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        existing.setEnabled(!existing.isEnabled());
        existing.setUpdatedAt(new Date());
        LearningSource saved = sourceRepository.save(existing).block(BLOCK_TIMEOUT);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "enabled", saved.isEnabled(),
                "source", toSourceMap(saved)
        ));
    }

    private Map<String, Object> toSourceMap(LearningSource s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("url", s.getUrl());
        map.put("domain", s.getDomain());
        map.put("detectedFocus", s.getDetectedFocus());
        map.put("manualFocus", s.getManualFocus());
        map.put("effectiveFocus", s.getEffectiveFocus());
        map.put("enabled", s.isEnabled());
        map.put("priority", s.getPriority());
        map.put("successCount", s.getSuccessCount());
        map.put("failureCount", s.getFailureCount());
        map.put("lastScrapedAt", s.getLastScrapedAt());
        map.put("createdAt", s.getCreatedAt());
        map.put("notes", s.getNotes());
        return map;
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
