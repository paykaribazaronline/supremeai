package org.example.config;

import org.example.service.AIAPIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Bean Configuration for Test/Development
 * Provides stubs for dependencies needed for testing
 * 
 * ONLY LOADS IN 'test' PROFILE - not in 'local' or 'prod'
 */
@Configuration
@Profile("test")
public class TestBeansConfiguration {

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
     * Provide AIAPIService bean for dependency injection
     */
    @Bean
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
