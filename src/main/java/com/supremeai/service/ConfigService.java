package com.supremeai.service;

import com.supremeai.model.SystemConfig;
import com.supremeai.model.UserTier;
import com.supremeai.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to manage global system configuration with Firestore persistence and local caching.
 */
@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private static final String DOCUMENT_ID = "global_settings";

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private Firestore firestore;

    private SystemConfig cachedConfig;
    private ListenerRegistration listenerRegistration;
    private final Executor listenerExecutor = Executors.newSingleThreadExecutor();

    public ConfigService() {
        // Initial fallback, will be refreshed from Firestore
        this.cachedConfig = createDefaultConfig();
    }

    @PostConstruct
    public void init() {
        // 1. Initial refresh
        refreshCache().subscribe(
            config -> logger.info("System configuration initialized: v{}", config.getVersion()),
            error -> logger.error("Failed to initialize system configuration", error)
        );

        // 2. Setup real-time listener
        try {
            this.listenerRegistration = firestore.collection("system_configs")
                    .document(DOCUMENT_ID)
                    .addSnapshotListener(listenerExecutor, (snapshot, error) -> {
                        if (error != null) {
                            logger.error("Firestore listener error for system_configs", error);
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            SystemConfig newConfig = snapshot.toObject(SystemConfig.class);
                            if (newConfig != null) {
                                this.cachedConfig = newConfig;
                                logger.info("System configuration updated in real-time: v{}", newConfig.getVersion());
                            }
                        }
                    });
            logger.info("Real-time configuration listener attached to document: {}", DOCUMENT_ID);
        } catch (Exception e) {
            logger.error("Failed to setup real-time configuration listener", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
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

        // Seed dynamic timeouts
        config.getTimeouts().put("voting_timeout", 15000L);
        config.getTimeouts().put("cache_duration", 300000L);
        config.getTimeouts().put("scraper_cache_ttl", 30L);

        // Seed dynamic thresholds
        config.getThresholds().put("consensus", 0.6);
        config.getThresholds().put("min_clarity", 0.6);
        config.getThresholds().put("auth_github.com", 0.90);
        config.getThresholds().put("auth_stackoverflow.com", 0.85);

        // Seed generic settings
        config.getSettings().put("max_retries", 2);
        config.getSettings().put("min_prompt_length", 10);
        config.getSettings().put("min_code_requirement_length", 20);
        config.getSettings().put("max_recent_logs", 1000);
        config.getSettings().put("scraper_rate_limit_requests", 10);
        config.getSettings().put("scraper_rate_limit_window", 60);

        return config;
    }

    /**
     * Refreshes the local cache from Firestore.
     */
    public Mono<SystemConfig> refreshCache() {
        // Diagnostic sync read
        try {
            logger.info("Diagnostic: Attempting sync read from Firestore...");
            var future = firestore.collection("system_configs").document("global_settings").get();
            var snapshot = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
            if (snapshot.exists()) {
                logger.info("Diagnostic: Sync read SUCCEEDED. Document exists.");
            } else {
                logger.warn("Diagnostic: Sync read SUCCEEDED but document is missing.");
            }
        } catch (Exception e) {
            logger.error("Diagnostic: Sync read FAILED", e);
        }

        return systemConfigRepository.findById("global_settings")
                .map(config -> {
                    this.cachedConfig = config;
                    return config;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    SystemConfig def = createDefaultConfig();
                    return systemConfigRepository.save(def).map(saved -> {
                        this.cachedConfig = saved;
                        return saved;
                    });
                }));
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
     * Update configuration in Firestore.
     */
    public Mono<SystemConfig> updateConfig(SystemConfig newConfig, String actorUserId, String ipAddress) {
        SystemConfig previous = getConfig();
        newConfig.setId("global_settings");
        Long currentVersion = previous.getVersion() == null ? 1L : previous.getVersion();
        newConfig.setVersion(currentVersion + 1L);
        
        return systemConfigRepository.save(newConfig)
                .map(saved -> {
                    this.cachedConfig = saved;
                    return saved;
                });
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

    // --- Helper methods for dynamic settings ---

    public String getCollectionName(String key, String defaultVal) {
        return getConfig().getCollections().getOrDefault(key, defaultVal);
    }

    public long getTimeout(String key, long defaultVal) {
        return getConfig().getTimeouts().getOrDefault(key, defaultVal);
    }

    public double getThreshold(String key, double defaultVal) {
        return getConfig().getThresholds().getOrDefault(key, defaultVal);
    }

    @SuppressWarnings("unchecked")
    public <T> T getSetting(String key, T defaultVal) {
        Object val = getConfig().getSettings().get(key);
        if (val == null) return defaultVal;
        try {
            return (T) val;
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
