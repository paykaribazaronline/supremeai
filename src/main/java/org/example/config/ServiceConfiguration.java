package org.example.config;

import org.example.model.SystemConfig;
import org.example.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 5: Spring Configuration for Services
 * 
 * Configures service beans and their dependencies
 * Enables dependency injection throughout REST layer
 * 
 * Initializes:
 * - Firebase (cloud connection)
 * - HybridDataCollector (Phase 3)
 * - DataCollectorService (Phase 4)
 * - WebhookListener (Phase 4)
 * - AdminMessagePusher (Phase 4)
 */
@Configuration
public class ServiceConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ServiceConfiguration.class);
    
    /**
     * Initialize Firebase Service
     */
    @Bean
    public FirebaseService firebaseService() {
        logger.info("🔧 Initializing Firebase Service...");
        return new FirebaseService();
    }
    
    /**
     * Initialize System Configuration
     * Provides default system settings for Agent Orchestration
     */
    @Bean
    public SystemConfig systemConfig() {
        logger.info("🔧 Initializing System Configuration...");
        SystemConfig config = new SystemConfig();
        config.setAgentCount(5);
        config.setConsensusThreshold(0.6);
        config.setRotationEnabled(true);
        config.setVpnEnabled(false);
        return config;
    }
    
    /**
     * Initialize API Keys Map.
     *
     * Loads provider API keys from environment variables so that AI calls work
     * without requiring the admin to re-enter keys after every Cloud Run cold
     * start. Keys saved via the Provider Coverage UI are merged on top of these
     * environment-level defaults by {@link org.example.service.ProviderRegistryService}
     * after it finishes loading.
     *
     * Supported environment variables (all optional – absent keys are silently
     * skipped so the app starts cleanly even when keys are not yet configured):
     *
     *   OPENAI_API_KEY        → GPT4
     *   ANTHROPIC_API_KEY     → CLAUDE
     *   GOOGLE_GEMINI_API_KEY → GEMINI
     *   DEEPSEEK_API_KEY      → DEEPSEEK
     *   GROQ_API_KEY          → GROQ
     *   COHERE_API_KEY        → COHERE
     *   PERPLEXITY_API_KEY    → PERPLEXITY
     *   HUGGINGFACE_API_KEY   → HUGGINGFACE
     *   XAI_GROK_API_KEY      → XAI
     *   META_API_KEY          → LLAMA
     *   TAVILY_API_KEY        → TAVILY  (web-search for active learning)
     */
    @Bean
    public Map<String, String> apiKeys() {
        logger.info("🔧 Loading API Keys from environment variables...");
        Map<String, String> keys = new HashMap<>();
        loadEnvKey(keys, "OPENAI_API_KEY",        "GPT4");
        loadEnvKey(keys, "ANTHROPIC_API_KEY",      "CLAUDE");
        loadEnvKey(keys, "GOOGLE_GEMINI_API_KEY",  "GEMINI");
        loadEnvKey(keys, "DEEPSEEK_API_KEY",       "DEEPSEEK");
        loadEnvKey(keys, "GROQ_API_KEY",           "GROQ");
        loadEnvKey(keys, "COHERE_API_KEY",         "COHERE");
        loadEnvKey(keys, "PERPLEXITY_API_KEY",     "PERPLEXITY");
        loadEnvKey(keys, "HUGGINGFACE_API_KEY",    "HUGGINGFACE");
        loadEnvKey(keys, "XAI_GROK_API_KEY",       "XAI");
        loadEnvKey(keys, "META_API_KEY",           "LLAMA");
        loadEnvKey(keys, "TAVILY_API_KEY",         "TAVILY");
        logger.info("✅ API Keys loaded: {} provider(s) configured from environment", keys.size());
        return keys;
    }
    
    /**
     * Initialize Quota Tracker
     */
    @Bean
    public QuotaTracker quotaTracker(FirebaseService firebase) {
        logger.info("🔧 Initializing Quota Tracker...");
        return new QuotaTracker(firebase);
    }
    
    /**
     * Initialize API Data Collector
     */
    @Bean
    public APIDataCollector apiDataCollector(QuotaTracker quota, FirebaseService firebase) {
        logger.info("🔧 Initializing API Data Collector...");
        return new APIDataCollector(quota, firebase);
    }
    
    /**
     * Initialize Browser Data Collector (Puppeteer fallback)
     */
    @Bean
    public BrowserDataCollector browserDataCollector(QuotaTracker quota, FirebaseService firebase) {
        logger.info("🔧 Initializing Browser Data Collector...");
        return new BrowserDataCollector(quota, firebase);
    }
    
    /**
     * Initialize Hybrid Data Collector
     * Combines API-first with browser fallback
     */
    @Bean
    public HybridDataCollector hybridDataCollector(
            APIDataCollector apiCollector,
            BrowserDataCollector browserCollector,
            QuotaTracker quota,
            FirebaseService firebase) {
        
        logger.info("🔧 Initializing Hybrid Data Collector...");
        return new HybridDataCollector(apiCollector, browserCollector, quota, firebase);
    }
    
    /**
     * Initialize Data Collector Service (REST API Layer)
     */
    @Bean
    public DataCollectorService dataCollectorService(HybridDataCollector hybridCollector) {
        logger.info("🔧 Initializing Data Collector Service...");
        return new DataCollectorService(hybridCollector);
    }
    
    /**
     * Initialize Webhook Listener (GitHub webhooks)
     */
    @Bean
    public WebhookListener webhookListener(DataCollectorService collectorService) {
        logger.info("🔧 Initializing Webhook Listener...");
        return new WebhookListener(collectorService);
    }
    
    /**
     * Initialize Admin Message Pusher (real-time updates to dashboard)
     */
    @Bean
    public AdminMessagePusher adminMessagePusher() {
        logger.info("🔧 Initializing Admin Message Pusher...");
        return new AdminMessagePusher();
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    /**
     * Load a single API key from an environment variable into the keys map.
     * Logs presence/absence without printing the actual key value.
     */
    private void loadEnvKey(Map<String, String> keys, String envVar, String provider) {
        String value = System.getenv(envVar);
        if (value != null && !value.isBlank()) {
            keys.put(provider, value.trim());
            logger.info("  ✅ {} → {} configured", envVar, provider);
        } else {
            logger.debug("  ⚠️ {} not set – {} will use registry key if available", envVar, provider);
        }
    }
}
