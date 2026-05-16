package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.model.SystemConfig;
import com.supremeai.repository.SystemConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * SystemConfigSeeder — seeds default system configuration into Firestore on first startup.
 *
 * Seeds the "system_configs" collection with document id "global_settings".
 * Uses idempotent check: only writes if document does not already exist.
 *
 * Configuration seeded:
 * - Tier quotas (GUEST/FREE/BASIC/PRO/ENTERPRISE/ADMIN)
 * - Max API keys per tier
 * - Simulator install limits per tier
 * - Default AI model settings
 * - Permission defaults
 * - Lifecycle & learning defaults (stored as provider config map)
 */
@Component
public class SystemConfigSeeder {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigSeeder.class);

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void seedSystemConfig() {
        log.info("[CONFIG_SEED] Checking for global_settings and providers in background...");
        
        // Seed System Config
        systemConfigRepository.findById("global_settings")
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .hasElement()
            .flatMap(exists -> {
                if (!exists) {
                    log.info("[CONFIG_SEED] No global_settings found — seeding default system config...");
                    return systemConfigRepository.save(buildDefaultConfig());
                } else {
                    log.info("[CONFIG_SEED] global_settings already exists — skipping seed");
                    return Mono.empty();
                }
            })
            .subscribe(
                config -> log.info("[CONFIG_SEED] Default system config sync complete"),
                error -> log.error("[CONFIG_SEED] Failed to seed system config: {}", error.getMessage())
            );

        // Seed AI Providers (Idempotent Merge)
        Flux.fromIterable(buildDefaultProviders())
            .flatMap(p -> providerRepository.findById(p.getId())
                .hasElement()
                .flatMap(exists -> {
                    if (!exists) {
                        log.info("[CONFIG_SEED] Seeding missing provider: {}", p.getId());
                        return providerRepository.save(p);
                    }
                    return Mono.empty();
                }))
            .collectList()
            .subscribe(
                p -> log.info("[CONFIG_SEED] AI Providers sync complete"),
                error -> log.error("[CONFIG_SEED] Failed to sync AI providers: {}", error.getMessage())
            );
    }

    private List<APIProvider> buildDefaultProviders() {
        return List.of(
            createProvider("google_gemini", "Gemini 1.5 Flash", "google", "gemini-1.5-flash", "Primary Orchestrator & Multimodal Specialist", 1, true),
            createProvider("openai_gpt4", "GPT-4o Mini", "openai", "gpt-4o-mini", "Backup - Structured Data & Logic Verification", 2, true),
            createProvider("deepseek_v4", "DeepSeek V4 Pro", "deepseek", "deepseek-v4pro", "Professional Coding & Technical Architect", 3, true),
            createProvider("hf_mistral", "Mistral 7B", "huggingface", "mistralai/Mistral-7B-v0.3", "Conversational Instruction Tuning", 4, true),
            createProvider("hf_llama3", "Llama 3 8B", "huggingface", "meta-llama/Meta-Llama-3-8B-Instruct", "Bengali Support & Nuance Specialist", 5, true)
        );
    }

    private APIProvider createProvider(String id, String name, String type, String model, String desc, int priority, boolean active) {
        APIProvider p = new APIProvider();
        p.setId(id);
        p.setName(name);
        p.setProviderType(type);
        p.setModelName(model);
        p.setDescription(desc);
        p.setPriority(priority);
        p.setActive(active);
        p.setValidated(false); // Admin needs to provide API key in dashboard
        p.setConfig(Map.of(
            "baseUrl", getDefaultBaseUrl(type),
            "maxTokens", 128000
        ));
        return p;
    }

    private String getDefaultBaseUrl(String type) {
        return switch (type) {
            case "google" -> "https://generativelanguage.googleapis.com";
            case "openai" -> "https://api.openai.com/v1";
            case "deepseek" -> "https://api.deepseek.com";
            case "huggingface" -> "https://api-inference.huggingface.co/models";
            default -> "";
        };
    }

    private SystemConfig buildDefaultConfig() {
        SystemConfig config = new SystemConfig();
        config.setId("global_settings");

        // Active AI models (primary + fallback)
        config.setActiveModel("google/gemini-1.5-flash");
        config.setSmallModel("google/gemini-1.5-flash");
        config.setVersion(1L);

        // Operational flags
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

        // System message / AI persona
        config.setSystemMessage(
            "You are SupremeAI, an expert software architect and full-stack developer. " +
            "You help users build, deploy, and manage AI-powered applications. " +
            "You are precise, helpful, and always explain your reasoning. " +
            "Default language: English. Switch to Bengali (বাংলা) if user speaks Bengali."
        );

        // Permission defaults
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

        // Telegram Storage Configuration
        config.setTelegramConfig(Map.of(
            "enabled", true,
            "teldriveUrl", "https://teldrive-lhlwyikwlq-uc.a.run.app",
            "apiId", "26740148",
            "apiHash", "ea9925a232d96171e07cf7db8b1af172",
            "botToken", "8858245545:AAEljV_-N6QVCCe6FWeA9-fRw-cS1kcIm_A",
            "channelId", "8312651262",
            "status", "CONNECTED",
            "storageUsed", "0 B",
            "lastSync", ""
        ));

        // Supabase Configuration (Used by legacy services or as additional DB)
        config.setSupabaseConfig(Map.of(
            "dbUrl", "postgres://postgres:njel.com.bd123@db.knqoqjnwpgmrsezgqgpg.supabase.co:5432/postgres?sslmode=require",
            "apiKey", "",
            "password", "njel.com.bd123",
            "status", "CONNECTED"
        ));

        // AI provider configurations (Comprehensive All-in-One Landscape)
        config.setProviders(Map.ofEntries(
            Map.entry("gemini", Map.of(
                "enabled", true,
                "model", "gemini-1.5-flash",
                "description", "Primary Orchestrator & Multimodal Specialist (1M Context)",
                "maxTokens", 1000000,
                "rotationThreshold", 0.85,
                "priority", 1
            )),
            Map.entry("hf_codellama", Map.of(
                "enabled", true,
                "model", "CodeLlama-34b-Instruct-hf",
                "description", "HF - Primary Code Generation (Serverless)",
                "maxTokens", 16000,
                "rotationThreshold", 0.80,
                "priority", 2
            )),
            Map.entry("hf_mistral", Map.of(
                "enabled", true,
                "model", "Mistral-7B-Instruct-v0.3",
                "description", "HF - Major Chat & Conversation (Instruct)",
                "maxTokens", 32000,
                "rotationThreshold", 0.80,
                "priority", 3
            )),
            Map.entry("hf_llama3", Map.of(
                "enabled", true,
                "model", "Meta-Llama-3-8B-Instruct",
                "description", "HF - Google Alternative / Bengali Support",
                "maxTokens", 8192,
                "rotationThreshold", 0.70,
                "priority", 4
            )),
            Map.entry("hf_phi_vision", Map.of(
                "enabled", true,
                "model", "Phi-3-vision-128k-instruct",
                "description", "HF - Specialized Vision & Image Analysis",
                "maxTokens", 128000,
                "rotationThreshold", 0.80,
                "priority", 5
            )),
            Map.entry("hf_e5_large", Map.of(
                "enabled", true,
                "model", "multilingual-e5-large",
                "description", "HF - Multilingual Embeddings for RAG",
                "maxTokens", 512,
                "rotationThreshold", 0.90,
                "priority", 6
            )),
            Map.entry("render_phi2", Map.of(
                "enabled", true,
                "model", "phi-2",
                "description", "Render - Fast Response / Free Tier Docker",
                "maxTokens", 2048,
                "rotationThreshold", 0.60,
                "priority", 7
            )),
            Map.entry("render_tinyllama", Map.of(
                "enabled", true,
                "model", "tinyllama-1.1b",
                "description", "Render - Emergency Fallback (Always Free)",
                "maxTokens", 1024,
                "rotationThreshold", 0.50,
                "priority", 8
            )),
            Map.entry("render_phi3", Map.of(
                "enabled", true,
                "model", "phi-3-mini",
                "description", "Render - Balanced Quality (Docker)",
                "maxTokens", 4096,
                "rotationThreshold", 0.70,
                "priority", 9
            )),
            Map.entry("render_qwen", Map.of(
                "enabled", true,
                "model", "qwen-0.5b",
                "description", "Render - Ultra-Lightweight (Best for Free Tier)",
                "maxTokens", 2048,
                "rotationThreshold", 0.40,
                "priority", 10
            )),
            Map.entry("openai", Map.of(
                "enabled", true,
                "model", "gpt-4o-mini",
                "description", "Backup - Structured Data & Logic Verification",
                "maxTokens", 128000,
                "rotationThreshold", 0.80,
                "priority", 11
            )),
            Map.entry("deepseek", Map.of(
                "enabled", true,
                "model", "deepseek-v4pro",
                "description", "Professional Coding & Technical Architect",
                "maxTokens", 64000,
                "rotationThreshold", 0.80,
                "priority", 12
            ))
        ));

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
