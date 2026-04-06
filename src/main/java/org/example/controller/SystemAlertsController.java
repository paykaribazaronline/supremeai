package org.example.controller;

import org.example.service.SystemAlertsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Exposes system alerts that require manual admin action.
 * GET /api/system/alerts — returns all current alerts with Bangla how-to guides.
 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class SystemAlertsController {

    @Autowired
    private SystemAlertsService systemAlertsService;

    @GetMapping("/alerts")
    public ResponseEntity<?> getAlerts() {
        try {
            return ResponseEntity.ok(Map.of(
                "alerts",   systemAlertsService.computeAlerts(),
                "total",    systemAlertsService.countTotalAlerts(),
                "critical", systemAlertsService.countCriticalAlerts()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
