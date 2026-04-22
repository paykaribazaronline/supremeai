package com.supremeai.controller;

import com.supremeai.service.QuotaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quota")
@CrossOrigin(origins = "*")
public class QuotaController {

    private final QuotaService quotaService;

    public QuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getQuota(@PathVariable String userId) {
        QuotaService.UserUsageStats stats = quotaService.getUsageStats(userId);
        if (stats == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        return ResponseEntity.ok(Map.of(
                "currentUsage", stats.getCurrentUsage(),
                "monthlyQuota", stats.getMonthlyQuota(),
                "lastUsedAt", stats.getLastUsedAt() != null ? stats.getLastUsedAt().toString() : null,
                "hasQuotaRemaining", stats.isHasQuotaRemaining(),
                "usagePercentage", stats.getUsagePercentage()
        ));
    }

    @PostMapping("/{userId}/reset")
    public ResponseEntity<Map<String, Object>> resetQuota(@PathVariable String userId) {
        boolean success = quotaService.resetUserUsage(userId);
        if (success) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "Quota reset for user " + userId));
        }
        return ResponseEntity.status(404).body(Map.of("error", "User not found"));
    }
}
