package com.supremeai.controller;

import com.supremeai.model.SystemConfig;
import com.supremeai.model.UserTier;
import com.supremeai.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controller for administrators to manage system-wide settings and quotas.
 */
@RestController
@RequestMapping("/api/admin/config")
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigController {

    @Autowired
    private ConfigService configService;

    /**
     * Get the current global system configuration.
     */
    @GetMapping
    public SystemConfig getSystemConfig() {
        return configService.getConfig();
    }

    /**
     * Update the entire system configuration.
     */
    @PutMapping
    public Mono<SystemConfig> updateSystemConfig(
            @RequestBody SystemConfig config,
            Authentication authentication,
            HttpServletRequest request) {
        validateConfig(config);
        String actor = authentication != null ? authentication.getName() : "unknown";
        return configService.updateConfig(config, actor, request.getRemoteAddr());
    }

    private void validateConfig(SystemConfig config) {
        if (config.getThresholds() != null) {
            for (Map.Entry<String, Double> entry : config.getThresholds().entrySet()) {
                Double val = entry.getValue();
                if (val != null && (val < 0.0 || val > 1.0)) {
                    throw new IllegalArgumentException("Threshold '" + entry.getKey() + "' must be between 0.0 and 1.0");
                }
            }
        }
        if (config.getTimeouts() != null) {
            for (Map.Entry<String, Long> entry : config.getTimeouts().entrySet()) {
                Long val = entry.getValue();
                if (val != null && val < 0) {
                    throw new IllegalArgumentException("Timeout '" + entry.getKey() + "' cannot be negative");
                }
            }
        }
        if (config.getSettings() != null) {
            Object maxLogs = config.getSettings().get("max_recent_logs");
            if (maxLogs instanceof Number && ((Number) maxLogs).intValue() > 10000) {
                throw new IllegalArgumentException("max_recent_logs cannot exceed 10000");
            }
        }
    }

    /**
     * Get all tier quotas.
     */
    @GetMapping("/quotas")
    public Map<String, Long> getQuotas() {
        return configService.getConfig().getTierQuotas();
    }

    /**
     * Update a specific tier's quota limit.
     */
    @PatchMapping("/quotas/{tier}")
    public Mono<SystemConfig> updateTierQuota(
            @PathVariable UserTier tier,
            @RequestParam long limit) {
        return configService.updateTierQuota(tier, limit);
    }

    /**
     * Force refresh the local configuration cache from Firestore.
     */
    @PostMapping("/refresh")
    public Mono<SystemConfig> refreshCache() {
        return configService.refreshCache();
    }
}
