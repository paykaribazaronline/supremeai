package com.supremeai.controller;

import com.supremeai.model.SystemConfig;
import com.supremeai.model.UserTier;
import com.supremeai.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Controller for administrators to manage system-wide settings and quotas.
 */
@RestController
@RequestMapping("/api/admin/config")
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
    public Mono<SystemConfig> updateSystemConfig(@RequestBody SystemConfig config) {
        return configService.updateConfig(config);
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
