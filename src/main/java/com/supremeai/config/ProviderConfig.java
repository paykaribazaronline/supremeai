package com.supremeai.config;

import com.supremeai.provider.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI provider beans.
 * Creates provider instances with proper dependencies for Spring injection.
 */
@Configuration
public class ProviderConfig {

    @Value("${ai.providers.ollama.api-key:ollama}")
    private String ollamaApiKey;

    @Value("${ai.providers.gemini.api-key:}")
    private String geminiApiKey;

    @Bean
    public OllamaProvider ollamaProvider() {
        return new OllamaProvider(ollamaApiKey);
    }

    @Bean
    public GeminiProvider geminiProvider() {
        return new GeminiProvider(geminiApiKey);
    }
}
