package com.supremeai.controller;

import com.supremeai.model.MonitoringLog;
import com.supremeai.repository.MonitoringLogRepository;
import com.supremeai.response.ApiResponse;
import com.supremeai.service.ProductionHealthMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Admin Monitoring Controller - Phase 4
 * Exposes production health metrics and system status.
 */
@RestController
@RequestMapping("/api/admin/monitoring")
@PreAuthorize("hasRole('ADMIN')")
public class MonitoringController {
    public MonitoringController(ProductionHealthMonitor healthMonitor, MonitoringLogRepository monitoringLogRepository) {
        this.healthMonitor = healthMonitor;
        this.monitoringLogRepository = monitoringLogRepository;
    }


    private static final Logger logger = LoggerFactory.getLogger(MonitoringController.class);



    /**
     * GET /api/admin/monitoring/logs
     * Fetch recent system logs from Firestore.
     */
    @GetMapping("/logs")
    public Mono<ApiResponse<List<MonitoringLog>>> getRecentLogs(@RequestParam(defaultValue = "100") int limit) {
        return monitoringLogRepository.findByOrderByTimestampDesc()
                .take(limit)
                .collectList()
                .map(logs -> ApiResponse.success("Monitoring logs retrieved successfully", logs));
    }

    /**
     * DELETE /api/admin/monitoring/logs
     * Clear all system logs.
     */
    @DeleteMapping("/logs")
    public Mono<ApiResponse<Void>> clearLogs() {
        return monitoringLogRepository.deleteAll()
                .then(Mono.just(ApiResponse.success("Monitoring logs cleared successfully", null)));
    }

    /**
     * GET /api/admin/monitoring/health
     * Comprehensive system health status.
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> getHealthStatus() {
        return Mono.fromCallable(() -> {
            Map<String, Object> health = healthMonitor.getHealthStatus();
            return ResponseEntity.ok(health);
        });
    }

    /**
     * GET /api/admin/monitoring/dashboard
     * Lightweight dashboard summary.
     */
    @GetMapping("/dashboard")
    public Mono<ResponseEntity<Map<String, Object>>> getDashboardSummary() {
        return Mono.fromCallable(() -> {
            Map<String, Object> summary = healthMonitor.getDashboardSummary();
            return ResponseEntity.ok(summary);
        });
    }

    /**
     * POST /api/admin/monitoring/reset
     * Reset metrics (admin only).
     */
    @PostMapping("/reset")
    public Mono<ResponseEntity<Map<String, Object>>> resetMetrics() {
        return Mono.fromCallable(() -> {
            healthMonitor.resetMetrics();
            Map<String, Object> resp = Map.of(
                "status", "OK",
                "message", "Metrics reset successfully"
            );
            return ResponseEntity.ok(resp);
        });
    }

    /**
     * GET /api/admin/monitoring/test-sentry
     * Triggers a test exception to verify Sentry integration.
     */
    @GetMapping("/test-sentry")
    public Mono<ResponseEntity<Map<String, Object>>> testSentry() {
        return Mono.fromCallable(() -> {
            logger.info("Triggering test Sentry exception...");
            try {
                throw new RuntimeException("SupremeAI Monitoring Test: Sentry integration check at " + java.time.LocalDateTime.now());
            } catch (Exception e) {
                io.sentry.Sentry.captureException(e);
                logger.error("Test error captured and sent to Sentry", e);
                return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", "Test exception captured and reported to Sentry",
                    "error", e.getMessage()
                ));
            }
        });
    }
}
