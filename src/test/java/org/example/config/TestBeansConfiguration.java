package org.example.config;

import org.example.model.SystemConfig;
import org.example.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Spring Bean Configuration for Test/Development
 * Provides TEST implementations of all dependencies needed for testing
 * 
 * ONLY LOADS IN 'test' PROFILE - not in 'local' or 'prod'
 * Overrides ServiceConfiguration which is disabled for test profile
 */
@Configuration
@Profile("test")
public class TestBeansConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TestBeansConfiguration.class);

    @Value("${ai.timeout.ms:7000}")
    private int aiTimeoutMs;

    @Value("${ai.retry.max:2}")
    private int aiMaxRetries;

    @Value("${ai.retry.base.backoff.ms:250}")
    private long aiRetryBackoffMs;

    @Value("${ai.prompt.max.tokens:2000}")
    private int aiPromptMaxTokens;

    @Value("${ai.output.max.tokens:1500}")
    private int aiOutputMaxTokens;

    @Value("${ai.rate.limit.per.minute:60}")
    private int aiRateLimitPerMinute;

    @Value("${ai.circuit.failure.threshold:3}")
    private int aiCircuitFailureThreshold;

    @Value("${ai.circuit.open.ms:30000}")
    private long aiCircuitOpenMs;

    @Value("${ai.queue.capacity:500}")
    private int aiQueueCapacity;

    @Value("${ai.cache.ttl.minutes:10}")
    private int aiCacheTtlMinutes;

    @Value("${ai.cache.max.size:1000}")
    private int aiCacheMaxSize;
    
    /**
     * Mock Firebase Service to prevent actual Firebase calls in tests
     */
    @Bean
    @Primary
    public FirebaseService firebaseService() {
        logger.info("🧪 Loading mock FirebaseService for tests");
        return mock(FirebaseService.class);
    }
    
    /**
     * Mock SystemConfig for tests
     */
    @Bean
    @Primary
    public SystemConfig systemConfig() {
        logger.info("🧪 Loading mock SystemConfig for tests");
        SystemConfig config = new SystemConfig();
        config.setAgentCount(2);
        config.setConsensusThreshold(0.5);
        config.setRotationEnabled(false);
        config.setVpnEnabled(false);
        return config;
    }
    
    /**
     * Mock API Keys for tests
     */
    @Bean
    @Primary
    public Map<String, String> apiKeys() {
        logger.info("🧪 Loading empty API keys for tests");
        return new HashMap<>();
    }
    
    /**
     * Mock Quota Tracker
     */
    @Bean
    @Primary
    public QuotaTracker quotaTracker(FirebaseService firebase, LocalJsonStoreService jsonStore) {
        logger.info("🧪 Loading mock QuotaTracker for tests");
        return mock(QuotaTracker.class);
    }
    
    /**
     * Mock API Data Collector
     */
    @Bean
    @Primary
    public APIDataCollector apiDataCollector(QuotaTracker quota, FirebaseService firebase) {
        logger.info("🧪 Loading mock APIDataCollector for tests");
        return mock(APIDataCollector.class);
    }
    
    /**
     * Mock Browser Data Collector
     */
    @Bean
    @Primary
    public BrowserDataCollector browserDataCollector(QuotaTracker quota, FirebaseService firebase) {
        logger.info("🧪 Loading mock BrowserDataCollector for tests");
        return mock(BrowserDataCollector.class);
    }
    
    /**
     * Mock Hybrid Data Collector
     */
    @Bean
    @Primary
    public HybridDataCollector hybridDataCollector(
            APIDataCollector apiCollector,
            BrowserDataCollector browserCollector,
            QuotaTracker quota,
            FirebaseService firebase) {
        logger.info("🧪 Loading mock HybridDataCollector for tests");
        return mock(HybridDataCollector.class);
    }
    
    /**
     * Mock Data Collector Service
     */
    @Bean
    @Primary
    public DataCollectorService dataCollectorService(HybridDataCollector hybridCollector) {
        logger.info("🧪 Loading mock DataCollectorService for tests");
        return mock(DataCollectorService.class);
    }
    
    /**
     * Mock Webhook Listener
     */
    @Bean
    @Primary
    public WebhookListener webhookListener(DataCollectorService collectorService) {
        logger.info("🧪 Loading mock WebhookListener for tests");
        return mock(WebhookListener.class);
    }
    
    /**
     * Mock Admin Message Pusher
     */
    @Bean
    @Primary
    public AdminMessagePusher adminMessagePusher() {
        logger.info("🧪 Loading mock AdminMessagePusher for tests");
        return mock(AdminMessagePusher.class);
    }
    
    /**
     * Provide AIAPIService bean for dependency injection
     */
    @Bean
    @Primary
    public AIAPIService aiAPIService(org.example.service.ProviderRegistryService providerRegistryService,
                                    org.example.service.FallbackConfigService fallbackConfigService) {
        Map<String, String> keys = new HashMap<>();
        putIfPresent(keys, "GPT4", "OPENAI_API_KEY", "GPT4_API_KEY");
        putIfPresent(keys, "CLAUDE", "ANTHROPIC_API_KEY", "CLAUDE_API_KEY");
        putIfPresent(keys, "GROQ", "GROQ_API_KEY");
        putIfPresent(keys, "DEEPSEEK", "DEEPSEEK_API_KEY");
        putIfPresent(keys, "GEMINI", "GEMINI_API_KEY", "GOOGLE_GEMINI_API_KEY");
        putIfPresent(keys, "COHERE", "COHERE_API_KEY");
        putIfPresent(keys, "PERPLEXITY", "PERPLEXITY_API_KEY");
        putIfPresent(keys, "LLAMA", "LLAMA_API_KEY", "META_LLAMA_API_KEY");
        putIfPresent(keys, "HUGGINGFACE", "HUGGINGFACE_API_KEY", "HF_API_KEY");
        putIfPresent(keys, "XAI", "XAI_API_KEY", "GROK_API_KEY");
        putIfPresent(keys, "AIRLLM", "AIRLLM_API_KEY");
        AIAPIService service = new AIAPIService(
            keys,
            aiTimeoutMs,
            aiMaxRetries,
            aiRetryBackoffMs,
            aiPromptMaxTokens,
            aiOutputMaxTokens,
            aiRateLimitPerMinute,
            aiCircuitFailureThreshold,
            aiCircuitOpenMs,
            aiQueueCapacity,
            aiCacheTtlMinutes,
            aiCacheMaxSize
        );
        service.setProviderRegistryService(providerRegistryService);
        service.setFallbackConfigService(fallbackConfigService);

        String airllmEndpoint = getConfigValue("AIRLLM_ENDPOINT");
        if (airllmEndpoint != null && !airllmEndpoint.isBlank()) {
            service.updateProviderEndpoint("AIRLLM", airllmEndpoint);
        }

        return service;
    }

    private String getConfigValue(String key) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = System.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return null;
    }

    private void putIfPresent(Map<String, String> keys, String targetKey, String... envNames) {
        for (String envName : envNames) {
            String value = getConfigValue(envName);
            if (value != null && !value.isBlank()) {
                keys.put(targetKey, value.trim());
                return;
            }
        }
    }
}
