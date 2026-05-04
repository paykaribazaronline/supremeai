package com.supremeai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.model.ActivityLog;
import com.supremeai.model.SystemConfig;
import com.supremeai.model.UserTier;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;

/**
 * Service to manage global system configuration with local caching for performance.
 */
@Profile("!local")
@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private static final String CONFIG_DOC_ID = "global_settings";

    @Autowired
    private SystemConfigRepository configRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // In-memory cache to avoid constant Firestore reads on every API call
    private SystemConfig cachedConfig;

    @PostConstruct
    public void init() {
        refreshCache().subscribe();
    }

    /**
     * Refreshes the local cache from Firestore.
     */
    public Mono<SystemConfig> refreshCache() {
        return configRepository.findById(CONFIG_DOC_ID)
            .switchIfEmpty(Mono.defer(() -> {
                logger.info("System config not found, initializing with defaults...");
                SystemConfig defaults = new SystemConfig();
                return configRepository.save(defaults);
            }))
            .doOnNext(config -> {
                this.cachedConfig = config;
                logger.info("System configuration cache updated.");
            });
    }

    /**
     * Returns the current configuration (from cache).
     */
    public SystemConfig getConfig() {
        if (cachedConfig == null) {
            // Fallback if cache isn't ready
            return new SystemConfig();
        }
        return cachedConfig;
    }

    /**
     * Get quota for a specific tier.
     */
    public long getQuotaForTier(UserTier tier) {
        if (tier == UserTier.ADMIN) return -1L;
        return getConfig().getQuotaForTier(tier);
    }

    /**
     * Get max APIs for a specific tier.
     */
    public int getMaxApisForTier(UserTier tier) {
        return getConfig().getMaxApisForTier(tier);
    }

    /**
     * Get max simulator installs for a specific tier.
     */
    public int getMaxSimulatorInstallsForTier(UserTier tier) {
        return getConfig().getMaxSimulatorInstallsForTier(tier);
    }

    /**
     * Update configuration and save to Firestore.
     */
    public Mono<SystemConfig> updateConfig(SystemConfig newConfig) {
        return updateConfig(newConfig, "system", "unknown");
    }

    /**
     * Update configuration and save to Firestore, including audit metadata.
     */
    public Mono<SystemConfig> updateConfig(SystemConfig newConfig, String actorUserId, String ipAddress) {
        SystemConfig previous = getConfig();
        newConfig.setId(CONFIG_DOC_ID); // Ensure ID is correct
        Long currentVersion = previous.getVersion() == null ? 1L : previous.getVersion();
        newConfig.setVersion(currentVersion + 1L);
        normalizeAuthoritySettings(newConfig);
        return configRepository.save(newConfig)
            .doOnNext(saved -> {
                this.cachedConfig = saved;
                logger.info("System configuration updated by admin.");
                eventPublisher.publishEvent(saved); // Publish config change event
                writeAuditLog(previous, saved, actorUserId, ipAddress);
            });
    }

    /**
     * Shortcut to update a specific tier quota.
     */
    public Mono<SystemConfig> updateTierQuota(UserTier tier, long limit) {
        SystemConfig current = getConfig();
        current.getTierQuotas().put(tier.name(), limit);
        return updateConfig(current);
    }

    private void normalizeAuthoritySettings(SystemConfig config) {
        if (config.getPermissions() == null) {
            return;
        }
        if (config.isFullAuthority()) {
            config.getPermissions().put("read", "allow");
            config.getPermissions().put("edit", "allow");
            config.getPermissions().put("bash", "allow");
            config.getPermissions().put("task", "allow");
            config.getPermissions().put("websearch", "allow");
            config.getPermissions().put(
                "external_directory",
                config.isEnableExternalDirectory() ? "allow" : "deny"
            );
        }
    }

    private void writeAuditLog(SystemConfig before, SystemConfig after, String actorUserId, String ipAddress) {
        String details = "CONFIG_UPDATE before=" + toJsonSafely(before) + " after=" + toJsonSafely(after);
        ActivityLog log = new ActivityLog(
            "CONFIG_UPDATED",
            actorUserId,
            "SYSTEM_CONFIG",
            "LOW",
            details,
            "SUCCESS",
            ipAddress
        );
        activityLogRepository.save(log).subscribe();
    }

    private String toJsonSafely(SystemConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"serialization_failed\"}";
        }
    }
}
