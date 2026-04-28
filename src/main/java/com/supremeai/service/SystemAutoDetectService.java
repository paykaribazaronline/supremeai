package com.supremeai.service;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service for system-level auto-detection of the best available AI provider.
 * Used by external integrations (e.g., Continue.dev) that need a single OpenAI-compatible endpoint.
 * Selects the first healthy provider from a prioritized list based on environment-configured API keys.
 */
@Service
public class SystemAutoDetectService {

    private static final Logger logger = LoggerFactory.getLogger(SystemAutoDetectService.class);

    // Provider keys from environment
    @Value("${supremeai.provider.groq.api-key:}")
    private String groqKey;
    @Value("${supremeai.provider.openai.api-key:}")
    private String openaiKey;
    @Value("${supremeai.provider.anthropic.api-key:}")
    private String anthropicKey;
    @Value("${supremeai.provider.gemini.api-key:}")
    private String geminiKey;
    @Value("${ai.deepseek.api-key:}")
    private String deepseekKey;
    @Value("${ai.mistral.api-key:}")
    private String mistralKey;

    @Autowired
    private AIProviderFactory providerFactory;

    // Provider selection order (highest priority first)
    private static final String[] PROVIDER_ORDER = {
        "groq",      // fast, good free tier
        "openai",    // reliable
        "anthropic", // strong reasoning
        "gemini",    // Google AI
        "deepseek",  // strong coding
        "mistral",   // efficient
        "ollama"     // local fallback
    };

    // Cache duration (5 minutes)
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000;

    // Cached healthy provider
    private volatile AIProvider cachedProvider = null;
    private volatile long cacheExpiry = 0;

    /**
     * Returns a healthy AIProvider, using cached if still valid, otherwise performs auto-detection.
     * Auto-detection iterates through PROVIDER_ORDER and returns the first provider that responds to a health check.
     */
    public AIProvider getProvider() {
        long now = System.currentTimeMillis();
        if (cachedProvider != null && now < cacheExpiry) {
            return cachedProvider;
        }
        synchronized (this) {
            if (cachedProvider != null && now < cacheExpiry) {
                return cachedProvider;
            }
            for (String name : PROVIDER_ORDER) {
                try {
                    AIProvider provider = createProvider(name);
                    if (provider != null && isHealthy(provider)) {
                        logger.info("Auto-detected provider: {}", name);
                        cachedProvider = provider;
                        cacheExpiry = now + CACHE_DURATION_MS;
                        return provider;
                    }
                } catch (Exception e) {
                    logger.debug("Provider {} unavailable: {}", name, e.getMessage());
                }
            }
            throw new RuntimeException("No healthy AI provider available. Configure at least one API key (e.g., GROQ_API_KEY, OPENAI_API_KEY).");
        }
    }

    /**
     * Creates a provider instance for the given provider name using environment keys.
     * Returns null if the provider requires an API key that is not configured.
     */
    private AIProvider createProvider(String name) {
        return switch (name) {
            case "groq" -> {
                if (groqKey == null || groqKey.isEmpty()) yield null;
                yield providerFactory.getProvider("groq", groqKey);
            }
            case "openai" -> {
                if (openaiKey == null || openaiKey.isEmpty()) yield null;
                yield providerFactory.getProvider("openai", openaiKey);
            }
            case "anthropic" -> {
                if (anthropicKey == null || anthropicKey.isEmpty()) yield null;
                yield providerFactory.getProvider("anthropic", anthropicKey);
            }
            case "gemini" -> {
                if (geminiKey == null || geminiKey.isEmpty()) yield null;
                yield providerFactory.getProvider("gemini", geminiKey);
            }
            case "deepseek" -> {
                if (deepseekKey == null || deepseekKey.isEmpty()) yield null;
                yield providerFactory.getProvider("deepseek", deepseekKey);
            }
            case "mistral" -> {
                if (mistralKey == null || mistralKey.isEmpty()) yield null;
                // MistralProvider supports custom model; use default
                yield providerFactory.getProvider("mistral", mistralKey);
            }
            case "ollama" -> providerFactory.getProvider("ollama"); // uses local config, no key needed
            default -> null;
        };
    }

    /**
     * Performs a quick health check on the provider.
     */
    private boolean isHealthy(AIProvider provider) {
        try {
            // Simple quick prompt; block with timeout
            String test = provider.generate("Hi").block(Duration.ofSeconds(2));
            return test != null && !test.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clears the cached provider, forcing re-evaluation on next request.
     */
    public void clearCache() {
        synchronized (this) {
            cachedProvider = null;
            cacheExpiry = 0;
        }
        logger.info("Auto-detect provider cache cleared");
    }
}
