package org.example.controller;

import org.example.service.PersistentAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Phase 5: Persistent Analytics Controller
 * REST API for historical metrics and trend analysis
 */
@RestController
@RequestMapping("/api/analytics")
public class PersistentAnalyticsController {

    @Autowired(required = false)
    private PersistentAnalyticsService analyticsService;

    /**
     * GET /api/analytics/historical?startTime=...&endTime=...
     * Get historical metrics for time range
     */
    @GetMapping("/historical")
    public ResponseEntity<?> getHistoricalMetrics(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        if (analyticsService == null) {
            return ResponseEntity.ok(Map.of("message", "Analytics service not available"));
        }

        try {
            LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime end = LocalDateTime.parse(endTime, DateTimeFormatter.ISO_DATE_TIME);
            return ResponseEntity.ok(analyticsService.getHistoricalMetrics(start, end));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/analytics/trend?metric=memory&hours=24
     * Get trend analysis for metric
     */
    @GetMapping("/trend")
    public ResponseEntity<?> getTrendAnalysis(
            @RequestParam String metric,
            @RequestParam(defaultValue = "24") int hours) {
        if (analyticsService == null) {
            return ResponseEntity.ok(Map.of("message", "Analytics service not available"));
        }

        return ResponseEntity.ok(analyticsService.getTrendAnalysis(metric, hours));
    }

    /**
     * GET /api/analytics/daily?date=2026-03-29
     * Get daily summary
     */
    @GetMapping("/daily")
    public ResponseEntity<?> getDailySummary(@RequestParam String date) {
        if (analyticsService == null) {
            return ResponseEntity.ok(Map.of("message", "Analytics service not available"));
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(date + "T00:00:00", DateTimeFormatter.ISO_DATE_TIME);
            return ResponseEntity.ok(analyticsService.getDailySummary(dateTime));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/analytics/monthly?year=2026&month=3
     * Get monthly summary
     */
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlySummary(
            @RequestParam int year,
            @RequestParam int month) {
        if (analyticsService == null) {
            return ResponseEntity.ok(Map.of("message", "Analytics service not available"));
        }

        return ResponseEntity.ok(analyticsService.getMonthlySummary(year, month));
    }

    /**
     * GET /api/analytics/export/json?startTime=...&endTime=...
     * Export metrics as JSON
     */
    @GetMapping("/export/json")
    public ResponseEntity<?> exportAsJson(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        if (analyticsService == null) {
            return ResponseEntity.ok(Map.of("message", "Analytics service not available"));
        }

        try {
            LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime end = LocalDateTime.parse(endTime, DateTimeFormatter.ISO_DATE_TIME);
            return ResponseEntity.ok(analyticsService.exportMetricsAsJson(start, end));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/analytics/export/csv?startTime=...&endTime=...
     * Export metrics as CSV
     */
    @GetMapping("/export/csv")
    public ResponseEntity<?> exportAsCsv(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        if (analyticsService == null) {
            return ResponseEntity.ok(Map.of("message", "Analytics service not available"));
        }

        try {
            LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime end = LocalDateTime.parse(endTime, DateTimeFormatter.ISO_DATE_TIME);
            String csv = analyticsService.exportMetricsAsCsv(start, end);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=metrics.csv")
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/analytics/record
     * Record metrics snapshot
     */
    @PostMapping("/record")
    public ResponseEntity<?> recordSnapshot(@RequestBody Map<String, Object> metrics) {
        if (analyticsService == null) {
            return ResponseEntity.ok(Map.of("message", "Analytics service not available"));
        }

        analyticsService.recordSnapshot(metrics);
        return ResponseEntity.ok(Map.of("status", "recorded"));
    }

    /**
     * GET /api/analytics/compare?p1Start=...&p1End=...&p2Start=...&p2End=...
     * Compare two time periods
     */
    @GetMapping("/compare")
    public ResponseEntity<?> comparePeriods(
            @RequestParam String p1Start,
            @RequestParam String p1End,
            @RequestParam String p2Start,
            @RequestParam String p2End) {
        if (analyticsService == null) {
            return ResponseEntity.ok(Map.of("message", "Analytics service not available"));
        }

        try {
            LocalDateTime period1Start = LocalDateTime.parse(p1Start, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime period1End = LocalDateTime.parse(p1End, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime period2Start = LocalDateTime.parse(p2Start, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime period2End = LocalDateTime.parse(p2End, DateTimeFormatter.ISO_DATE_TIME);

            return ResponseEntity.ok(analyticsService.comparePeriods(
                    period1Start, period1End, period2Start, period2End));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
