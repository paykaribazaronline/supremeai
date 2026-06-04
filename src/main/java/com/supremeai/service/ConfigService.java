package com.supremeai.service;

import com.supremeai.model.SystemConfig;
import com.supremeai.model.SystemWorkRule;
import com.supremeai.model.UserTier;
import com.supremeai.repository.SystemConfigRepository;
import com.supremeai.service.SystemWorkRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service to manage global system configuration with Firestore persistence and local caching.
 */
@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
    private static final String DOCUMENT_ID = "global_settings";
    private static final String REDIS_CONFIG_KEY = "supremeai:config";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private Firestore firestore;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private SystemWorkRuleService systemWorkRuleService;

    @org.springframework.beans.factory.annotation.Value("${supremeai.redis.mock-online:false}")
    private boolean mockOnline;

    private SystemConfig cachedConfig;
    private ListenerRegistration listenerRegistration;
    private final ExecutorService listenerExecutor = Executors.newSingleThreadExecutor();

    public ConfigService() {
        this.cachedConfig = createDefaultConfig();
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void init() {
        logger.info("[CONFIG] Initializing system configuration...");

        refreshCache()
            .flatMap(config -> {
                boolean needsUpdate = false;
                if (!config.getSettings().containsKey("agentic.web.targets")) {
                    config.getSettings().put("agentic.web.targets", 
                        "- https://stackoverflow.com/search?q=%s (For programming errors and bugs)\n" +
                        "- https://pub.dev/packages?q=%s (For Flutter and Dart packages)\n" +
                        "- https://gemini.google.com/app?q=%s (For creative writing, songs, translations, and poems)\n" +
                        "- https://kimi.moonshot.cn/chat?q=%s (For complex analysis, math, and deep research)\n" +
                        "- https://huggingface.co/chat?q=%s (For free open-source AI models and coding help)\n" +
                        "- https://github.com/search?q=%s&type=code (For open-source code examples and repositories)\n" +
                        "- https://developer.mozilla.org/en-US/search?q=%s (For JavaScript, HTML, and CSS web documentation)\n" +
                        "- https://en.wikipedia.org/w/index.php?search=%s (For general factual knowledge and history)\n" +
                        "- https://html.duckduckgo.com/html/?q=%s (For latest news, current events, and general info)"
                    );
                    needsUpdate = true;
                }
                if (needsUpdate) {
                    return updateConfig(config);
                }
                return Mono.just(config);
            })
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .subscribe(
                config -> logger.info("[CONFIG] System configuration initialized: v{}", config.getVersion()),
                error -> logger.error("[CONFIG] Failed to initialize system configuration: {}", error.getMessage())
            );

        if (systemWorkRuleService != null) {
            systemWorkRuleService.ensureDefaults()
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .subscribe(
                        results -> logger.info("[CONFIG] SystemWorkRule defaults ensured: {}", results),
                        error -> logger.warn("[CONFIG] ensureDefaults failed: {}", error.getMessage())
                    );
        }

        try {
            this.listenerRegistration = firestore.collection("system_configs")
                    .document(DOCUMENT_ID)
                    .addSnapshotListener(listenerExecutor, (snapshot, error) -> {
                        if (error != null) {
                            logger.error("[CONFIG] Firestore listener error", error);
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            SystemConfig newConfig = snapshot.toObject(SystemConfig.class);
                            if (newConfig != null) {
                                this.cachedConfig = newConfig;
                                logger.info("[CONFIG] Live update received: v{}", newConfig.getVersion());
                            }
                        }
                    });
            logger.info("[CONFIG] Real-time listener attached to: {}", DOCUMENT_ID);
        } catch (Exception e) {
            logger.error("[CONFIG] Failed to setup real-time listener", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (listenerRegistration != null) listenerRegistration.remove();
        // Shutdown the dedicated listener executor to avoid thread leaks on application stop
        try {
            listenerExecutor.shutdown();
            if (!listenerExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                logger.warn("[CONFIG] Listener executor did not terminate, forcing shutdown");
                listenerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("[CONFIG] Interrupted during executor shutdown", e);
            Thread.currentThread().interrupt();
        }
    }

    // ─── Defaults ──────────────────────────────────────────────────────────

    private SystemConfig createDefaultConfig() {
        SystemConfig config = new SystemConfig();
        config.setId("global_settings");
        config.setVersion(1L);
        config.getAdminEmails().add("admin@supreme.ai");
        config.getAdminEmails().add("admin@supremeai.com");
        config.getAdminEmails().add("admin@supremeai.dev");
        config.getAdminEmails().add("niloyjoy7@gmail.com");
        config.getTierQuotas().put(UserTier.FREE.name(), 1000L);
        config.getTierQuotas().put(UserTier.BASIC.name(), 10000L);
        config.getTierQuotas().put(UserTier.PRO.name(), 100000L);
        config.getTierQuotas().put("GUEST", 50L);
        config.getTierQuotas().put(UserTier.ADMIN.name(), -1L);
        config.getTierMaxApis().put(UserTier.FREE.name(), 5);
        config.getTierMaxApis().put(UserTier.BASIC.name(), 20);
        config.getTierMaxApis().put(UserTier.PRO.name(), 100);
        config.getTierMaxApis().put(UserTier.ADMIN.name(), -1);
        config.getTierMaxSimulatorInstalls().put(UserTier.FREE.name(), 3);
        config.getTierMaxSimulatorInstalls().put(UserTier.BASIC.name(), 10);
        config.getTierMaxSimulatorInstalls().put(UserTier.PRO.name(), 50);
        config.getTierMaxSimulatorInstalls().put(UserTier.ADMIN.name(), 50);
        config.getTimeouts().put("voting_timeout", 15000L);
        config.getTimeouts().put("cache_duration", 300000L);
        config.getTimeouts().put("scraper_cache_ttl", 30L);
        config.getThresholds().put("consensus", 0.6);
        config.getThresholds().put("min_clarity", 0.6);
        config.getThresholds().put("auth_github.com", 0.90);
        config.getThresholds().put("auth_stackoverflow.com", 0.85);
        config.getSettings().put("max_retries", 2);
        config.getSettings().put("min_prompt_length", 10);
        config.getSettings().put("min_code_requirement_length", 20);
        config.getSettings().put("max_recent_logs", 1000);
        config.getSettings().put("scraper_rate_limit_requests", 10);
        config.getSettings().put("scraper_rate_limit_window", 60);
        config.getSettings().put("agentic.web.targets", 
            "- https://stackoverflow.com/search?q=%s (For programming errors and bugs)\n" +
            "- https://pub.dev/packages?q=%s (For Flutter and Dart packages)\n" +
            "- https://gemini.google.com/app?q=%s (For creative writing, songs, translations, and poems)\n" +
            "- https://kimi.moonshot.cn/chat?q=%s (For complex analysis, math, and deep research)\n" +
            "- https://huggingface.co/chat?q=%s (For free open-source AI models and coding help)\n" +
            "- https://github.com/search?q=%s&type=code (For open-source code examples and repositories)\n" +
            "- https://developer.mozilla.org/en-US/search?q=%s (For JavaScript, HTML, and CSS web documentation)\n" +
            "- https://en.wikipedia.org/w/index.php?search=%s (For general factual knowledge and history)\n" +
            "- https://html.duckduckgo.com/html/?q=%s (For latest news, current events, and general info)"
        );
        return config;
    }

    // ─── Cache management ─────────────────────────────────────────────────

    public Mono<SystemConfig> refreshCache() {
        if (redisTemplate != null && !mockOnline) {
            try {
                SystemConfig redisConfig = (SystemConfig) redisTemplate.opsForValue().get(REDIS_CONFIG_KEY);
                if (redisConfig != null) {
                    this.cachedConfig = redisConfig;
                    return Mono.just(redisConfig);
                }
            } catch (Exception e) {
                logger.warn("Redis miss: {}", e.getMessage());
            }
        }
        return systemConfigRepository.findById("global_settings")
                .doOnNext(cfg -> {
                    this.cachedConfig = cfg;
                    if (redisTemplate != null) {
                        try { redisTemplate.opsForValue().set(REDIS_CONFIG_KEY, cfg, CACHE_TTL); }
                        catch (Exception e) { logger.warn("Redis set failed: {}", e.getMessage()); }
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    SystemConfig def = createDefaultConfig();
                    return systemConfigRepository.save(def).doOnNext(saved -> this.cachedConfig = saved);
                }));
    }

    public SystemConfig getConfig() {
        return cachedConfig;
    }

    // ─── SystemWorkRule overlays ──────────────────────────────────────────
    //
    // SystemWorkRules are the authoritative source of truth.  When an admin
    // sets a rule these helpers always return the rule value first, then fall
    // back to SystemConfig.settings, then defaultVal.

    /**
     * Sync helper — resolves a setting value honouring SystemWorkRule overrides.
     * Only call from non-reactive (blocking) threads.
     */
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(10);

    @SuppressWarnings("unchecked")
    public <T> T getEffectiveSetting(String key, T defaultVal) {
        if (systemWorkRuleService != null) {
            try {
                String ruleVal = systemWorkRuleService.getRuleByKey(key)
                        .map(SystemWorkRule::getValue)
                        .onErrorReturn(null)
                        .block(BLOCK_TIMEOUT);
                if (ruleVal != null) {
                    try { return (T) ruleVal; } catch (ClassCastException e) { /* fall through */ }
                }
            } catch (Exception ignored) { }
        }
        Object val = getConfig().getSettings().get(key);
        if (val != null) { try { return (T) val; } catch (ClassCastException e) { /* fall through */ } }
        return defaultVal;
    }

    /**
     * Mono-based string override — SystemWorkRules are checked before SystemConfig.
     */
    public Mono<String> getEffectiveString(String key, String defaultVal) {
        if (systemWorkRuleService == null) {
            Object raw = getConfig().getSettings().getOrDefault(key, defaultVal);
            return Mono.just(raw == null ? defaultVal : (String) raw);
        }
        return systemWorkRuleService.getRuleByKey(key)
                .map(SystemWorkRule::getValue)
                .cast(String.class)
                .onErrorReturn((String) getConfig().getSettings().getOrDefault(key, defaultVal));
    }

    public Mono<Boolean> getEffectiveBoolean(String key, boolean defaultVal) {
        return getEffectiveString(key, String.valueOf(defaultVal))
                .map(v -> { try { return Boolean.parseBoolean(v); } catch (Exception e) { return defaultVal; } });
    }

    public Mono<Long> getEffectiveLong(String key, long defaultVal) {
        return getEffectiveString(key, String.valueOf(defaultVal))
                .map(v -> { try { return Long.parseLong(v); } catch (Exception e) { return defaultVal; } });
    }

    // ─── Quota helpers ─────────────────────────────────────────────────────

    public long getQuotaForTier(UserTier tier) {
        if (tier == UserTier.ADMIN) return -1L;
        return getConfig().getTierQuotas().getOrDefault(tier.name(), 1000L);
    }

    public int getMaxApisForTier(UserTier tier) {
        return getConfig().getTierMaxApis().getOrDefault(tier.name(), 0);
    }

    public int getMaxSimulatorInstallsForTier(UserTier tier) {
        return getConfig().getTierMaxSimulatorInstalls().getOrDefault(tier.name(), 0);
    }

    // ─── Config write ──────────────────────────────────────────────────────

    public Mono<SystemConfig> updateConfig(SystemConfig newConfig) {
        return updateConfig(newConfig, "system", "unknown");
    }

    public Mono<SystemConfig> updateConfig(SystemConfig newConfig, String actorUserId, String ipAddress) {
        logger.info("[CONFIG] Update requested by {} from {}. Version: {}", actorUserId, ipAddress, newConfig.getVersion());

        SystemConfig previous = getConfig();
        newConfig.setId("global_settings");
        Long currentVersion = previous.getVersion() == null ? 1L : previous.getVersion();
        newConfig.setVersion(currentVersion + 1L);

        logger.debug("[CONFIG] Saving to Firestore: collections={}, thresholds={}, timeouts={}",
                newConfig.getCollections().size(), newConfig.getThresholds().size(), newConfig.getTimeouts().size());

        return systemConfigRepository.save(newConfig)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(saved -> logger.info("[CONFIG] Saved config v{}", saved.getVersion()))
                .doOnError(e -> logger.error("[CONFIG] Save failed: {}", e.getMessage()))
                .map(saved -> {
                    this.cachedConfig = saved;
                    if (redisTemplate != null) {
                        try { redisTemplate.delete(REDIS_CONFIG_KEY); }
                        catch (Exception e) { logger.warn("Redis invalidate failed: {}", e.getMessage()); }
                    }
                    return saved;
                })
                .onErrorResume(e -> {
                    logger.warn("[CONFIG] Persistence failed, writing local cache: {}", e.getMessage());
                    this.cachedConfig = newConfig;
                    return Mono.just(newConfig);
                });
    }

    public Mono<SystemConfig> updateTierQuota(UserTier tier, long limit) {
        SystemConfig current = getConfig();
        Map<String, Long> quotas = new HashMap<>(current.getTierQuotas());
        quotas.put(tier.name(), limit);
        current.setTierQuotas(quotas);
        return updateConfig(current, "system", "unknown");
    }

    // ─── Legacy helpers ────────────────────────────────────────────────────

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
        try { return (T) val; } catch (ClassCastException e) { return defaultVal; }
    }

    /**
     * Updates a single setting in the global configuration.
     */
    public Mono<SystemConfig> updateSetting(String key, Object value) {
        SystemConfig current = getConfig();
        current.getSettings().put(key, value);
        return updateConfig(current);
    }


}
