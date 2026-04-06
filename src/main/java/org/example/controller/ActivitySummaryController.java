package org.example.controller;

import org.example.service.ActivitySummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoint for the last-24h activity summary dashboard tab.
 * GET /api/activity/summary
 */
@RestController
@RequestMapping("/api/activity")
@CrossOrigin(origins = "*")
public class ActivitySummaryController {

    @Autowired
    private ActivitySummaryService activitySummaryService;

    /**
     * GET /api/activity/summary
     * Returns aggregated last-24-hour activity across all system components.
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        try {
            return ResponseEntity.ok(activitySummaryService.buildSummary());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/activity/record
     * Body: { "category": "LEARNING", "title": "New pattern learned", "detail": "…" }
     * Allows other services/admin to push custom activity events.
     */
    @PostMapping("/record")
    public ResponseEntity<?> recordEvent(@RequestBody Map<String, String> body) {
        try {
            activitySummaryService.record(
                body.getOrDefault("category", "GENERAL"),
                body.getOrDefault("title", ""),
                body.getOrDefault("detail", "")
            );
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
