package org.example.config;

import org.example.service.AIAPIService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Bean Configuration for Test/Development
 * Provides stubs for dependencies needed for testing
 */
@Configuration
public class TestBeansConfiguration {
    
    /**
     * Provide AIAPIService bean for dependency injection
     */
    @Bean
    public AIAPIService aiAPIService() {
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
        return new AIAPIService(keys);
    }

    private void putIfPresent(Map<String, String> keys, String targetKey, String... envNames) {
        for (String envName : envNames) {
            String value = System.getenv(envName);
            if (value != null && !value.isBlank()) {
                keys.put(targetKey, value.trim());
                return;
            }
        }
    }
}
