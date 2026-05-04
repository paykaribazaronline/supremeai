package com.supremeai.service;

import com.supremeai.model.SystemConfig;
import com.supremeai.model.UserTier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to manage global system configuration with local caching for performance.
 * This version is simplified for local development without Firestore dependency.
 */
@Service
public class ConfigService {

    private SystemConfig cachedConfig;

    public ConfigService() {
        // Initialize with default configuration
        this.cachedConfig = createDefaultConfig();
    }

    private SystemConfig createDefaultConfig() {
        SystemConfig config = new SystemConfig();
        config.setId("global_settings");
        config.setVersion(1L);
        
        // Set default quotas
        config.getTierQuotas().put(UserTier.FREE.name(), 1000L);
        config.getTierQuotas().put(UserTier.BASIC.name(), 10000L);
        config.getTierQuotas().put(UserTier.PRO.name(), 100000L);
        config.getTierQuotas().put(UserTier.ADMIN.name(), -1L);
        
        // Set max APIs
        config.getTierMaxApis().put(UserTier.FREE.name(), 5);
        config.getTierMaxApis().put(UserTier.BASIC.name(), 20);
        config.getTierMaxApis().put(UserTier.PRO.name(), 100);
        config.getTierMaxApis().put(UserTier.ADMIN.name(), -1);
        
        // Set max simulator installs
        config.getTierMaxSimulatorInstalls().put(UserTier.FREE.name(), 3);
        config.getTierMaxSimulatorInstalls().put(UserTier.BASIC.name(), 10);
        config.getTierMaxSimulatorInstalls().put(UserTier.PRO.name(), 50);
        config.getTierMaxSimulatorInstalls().put(UserTier.ADMIN.name(), 50);
        
        return config;
    }

    /**
     * Refreshes the local cache (no-op for local, returns current config).
     */
    public Mono<SystemConfig> refreshCache() {
        return Mono.just(cachedConfig);
    }

    /**
     * Returns the current configuration (from cache).
     */
    public SystemConfig getConfig() {
        return cachedConfig;
    }

    /**
     * Get quota for a specific tier.
     */
    public long getQuotaForTier(UserTier tier) {
        if (tier == UserTier.ADMIN) return -1L;
        return getConfig().getTierQuotas().getOrDefault(tier.name(), 1000L);
    }

    /**
     * Get max APIs for a specific tier.
     */
    public int getMaxApisForTier(UserTier tier) {
        return getConfig().getTierMaxApis().getOrDefault(tier.name(), 0);
    }

    /**
     * Get max simulator installs for a specific tier.
     */
    public int getMaxSimulatorInstallsForTier(UserTier tier) {
        return getConfig().getTierMaxSimulatorInstalls().getOrDefault(tier.name(), 0);
    }

    /**
     * Update configuration (in-memory for local).
     */
    public Mono<SystemConfig> updateConfig(SystemConfig newConfig) {
        return updateConfig(newConfig, "system", "unknown");
    }

    /**
     * Update configuration (in-memory for local).
     */
    public Mono<SystemConfig> updateConfig(SystemConfig newConfig, String actorUserId, String ipAddress) {
        SystemConfig previous = getConfig();
        newConfig.setId("global_settings");
        Long currentVersion = previous.getVersion() == null ? 1L : previous.getVersion();
        newConfig.setVersion(currentVersion + 1L);
        this.cachedConfig = newConfig;
        return Mono.just(newConfig);
    }

    /**
     * Shortcut to update a specific tier quota.
     */
    public Mono<SystemConfig> updateTierQuota(UserTier tier, long limit) {
        SystemConfig current = getConfig();
        Map<String, Long> quotas = new HashMap<>(current.getTierQuotas());
        quotas.put(tier.name(), limit);
        current.setTierQuotas(quotas);
        return updateConfig(current, "system", "unknown");
    }
}
