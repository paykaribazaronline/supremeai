package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.model.SystemConfig;
import com.supremeai.repository.SystemConfigRepository;
import com.supremeai.security.SecretManagerService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Date;

/**
 * SystemConfigSeeder — seeds default system configuration into Firestore on first startup.
 * Only seeds global_settings. No hardcoded providers — admin configures via dashboard.
 */
@Component
public class SystemConfigSeeder {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigSeeder.class);

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private SecretManagerService secretManagerService;

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void seedSystemConfig() {
        log.info("[CONFIG_SEED] Checking for global_settings in background...");

        systemConfigRepository.findById("global_settings")
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .hasElement()
            .flatMap(exists -> {
                if (!exists) {
                    log.info("[CONFIG_SEED] No global_settings found — seeding default system config...");
                    return systemConfigRepository.save(buildDefaultConfig());
                } else {
                    log.info("[CONFIG_SEED] global_settings already exists — ensuring Telegram config is updated from environment...");
                    return systemConfigRepository.findById("global_settings")
                        .flatMap(config -> {
                            boolean updated = false;
                            Map<String, Object> tgConfig = config.getTelegramConfig();
                            if (tgConfig == null || tgConfig.isEmpty() || !Boolean.TRUE.equals(tgConfig.get("enabled"))) {
                                String telegramBotToken = secretManagerService.getSecret("TELEGRAM_BOT_TOKEN");
                                if (telegramBotToken == null) telegramBotToken = secretManagerService.getSecret("TG_BOT_TOKEN");
                                if (!telegramBotToken.isEmpty()) {
                                    log.info("[CONFIG_SEED] Found Telegram token in environment — updating existing config");
                                    config.setTelegramConfig(buildTelegramConfig());
                                    updated = true;
                                }
                            }
                            return updated ? systemConfigRepository.save(config) : Mono.just(config);
                        });
                }
            })
            .subscribe(
                config -> log.info("[CONFIG_SEED] Default system config sync complete"),
                error -> log.error("[CONFIG_SEED] Failed to seed system config: {}", error.getMessage())
            );
    }

    private Map<String, Object> buildTelegramConfig() {
        String telegramEnabled = secretManagerService.getSecret("TELEGRAM_ENABLED");
        if (telegramEnabled == null) telegramEnabled = "true";
        String telegramApiId = secretManagerService.getSecret("TELEGRAM_API_ID");
        if (telegramApiId == null) telegramApiId = secretManagerService.getSecret("TG_APP_ID");
        if (telegramApiId == null) telegramApiId = "";
        String telegramApiHash = secretManagerService.getSecret("TELEGRAM_API_HASH");
        if (telegramApiHash == null) telegramApiHash = secretManagerService.getSecret("TG_APP_HASH");
        if (telegramApiHash == null) telegramApiHash = "";
        String telegramBotToken = secretManagerService.getSecret("TELEGRAM_BOT_TOKEN");
        if (telegramBotToken == null) telegramBotToken = secretManagerService.getSecret("TG_BOT_TOKEN");
        if (telegramBotToken == null) telegramBotToken = "";
        String telegramChannelId = secretManagerService.getSecret("TELEGRAM_CHANNEL_ID");
        if (telegramChannelId == null) telegramChannelId = secretManagerService.getSecret("TG_CHANNEL_ID");
        if (telegramChannelId == null) telegramChannelId = "";
        String teldriveUrl = secretManagerService.getSecret("TELDRIVE_URL");
        if (teldriveUrl == null) teldriveUrl = "https://teldrive-lhlwyikwlq-uc.a.run.app";

        return Map.of(
            "enabled", "true".equalsIgnoreCase(telegramEnabled),
            "teldriveUrl", teldriveUrl,
            "apiId", telegramApiId,
            "apiHash", telegramApiHash,
            "botToken", telegramBotToken,
            "channelId", telegramChannelId,
            "status", "CONNECTED",
            "storageUsed", "0 B",
            "lastSync", ""
        );
    }

    private SystemConfig buildDefaultConfig() {
        SystemConfig config = new SystemConfig();
        config.setId("global_settings");

        config.setActiveModel("google/gemini-1.5-flash");
        config.setSmallModel("google/gemini-1.5-flash");
        config.setVersion(1L);

        config.setMaintenanceMode(false);
        config.setEmergencyStop(false);
        config.setApiAccessLock(false);
        config.setApiRotationStrategy("quota-based");
        config.setAutoExecApprovalRequired(true);
        config.setFullAuthority(false);
        config.setShareMode("manual");
        config.setEnableExternalDirectory(false);
        config.setEmailNotifications(true);
        config.setSmsAlerts(false);

        config.setSystemMessage(
            "You are SupremeAI, an expert software architect and full-stack developer. " +
            "You help users build, deploy, and manage AI-powered applications. " +
            "You are precise, helpful, and always explain your reasoning. " +
            "Default language: English. Switch to Bengali (বাংলা) if user speaks Bengali."
        );

        config.setPermissions(Map.of(
            "read", "allow",
            "edit", "ask",
            "bash", "ask",
            "task", "allow",
            "websearch", "allow",
            "external_directory", "deny",
            "file_delete", "ask",
            "git_push", "ask",
            "deploy", "ask"
        ));

        config.setTelegramConfig(buildTelegramConfig());

        String supabaseUrl = secretManagerService.getSecret("SUPABASE_DB_URL");
        String supabaseKey = secretManagerService.getSecret("SUPABASE_API_KEY");
        if (supabaseUrl != null && !supabaseUrl.isEmpty()) {
            config.setSupabaseConfig(Map.of(
                "dbUrl", supabaseUrl,
                "apiKey", supabaseKey,
                "status", "CONNECTED"
            ));
        } else {
            config.setSupabaseConfig(Map.of("status", "DISCONNECTED"));
        }

        config.setProviders(Map.of());

        config.setSettings(Map.of(
            "learning_interval_minutes", 60,
            "learning_archive_interval_hours", 12,
            "learning_sync_interval_hours", 12,
            "learning_hot_retention_days", 7,
            "learning_hot_limit_count", 100,
            "max_retries", 3,
            "cache_ttl_minutes", 30
        ));
        config.setAutonomousLearningEnabled(true);
        config.setAutonomousAuditEnabled(true);

        log.debug("[CONFIG_SEED] Built default config: model={} maintenance={}",
            config.getActiveModel(), config.isMaintenanceMode());

        return config;
    }
}
