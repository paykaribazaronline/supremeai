package com.supremeai.config;

import com.supremeai.provider.*;
import com.supremeai.security.UnifiedSecretsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for AI provider beans.
 * Creates provider instances with proper dependencies for Spring injection.
 */
@Configuration
public class ProviderConfig {

    @Autowired
    private UnifiedSecretsService secretsService;

    @Value("${ai.providers.ollama.api-key:ollama}")
    private String ollamaApiKey;

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @Value("${OPENAI_API_KEY:}")
    private String openaiApiKey;

    @Value("${GROQ_API_KEY:}")
    private String groqApiKey;

    @Value("${ANTHROPIC_API_KEY:}")
    private String anthropicApiKey;

    @Value("${DEEPSEEK_API_KEY:}")
    private String deepseekApiKey;

    @Value("${MISTRAL_API_KEY:}")
    private String mistralApiKey;

    @Value("${KIMI_API_KEY:}")
    private String kimiApiKey;

    @Value("${STEPFUN_API_KEY:}")
    private String stepfunApiKey;

    @Value("${supremeai.provider.codegeex4.api-key:}")
    private String codegeex4ApiKey;

    @Bean(name = "aiInitialKeys")
    public Map<String, String> initialKeys() {
        Map<String, String> keys = new HashMap<>();
        
        // Fetch keys from UnifiedSecretsService (which checks Firebase, Vault, AWS, ENV)
        addKeyIfPresent(keys, "openai", "OPENAI_API_KEY", openaiApiKey);
        addKeyIfPresent(keys, "gpt4", "OPENAI_API_KEY", openaiApiKey);
        addKeyIfPresent(keys, "gemini", "GEMINI_API_KEY", geminiApiKey);
        addKeyIfPresent(keys, "groq", "GROQ_API_KEY", groqApiKey);
        addKeyIfPresent(keys, "anthropic", "ANTHROPIC_API_KEY", anthropicApiKey);
        addKeyIfPresent(keys, "claude", "ANTHROPIC_API_KEY", anthropicApiKey);
        addKeyIfPresent(keys, "deepseek", "DEEPSEEK_API_KEY", deepseekApiKey);
        addKeyIfPresent(keys, "mistral", "MISTRAL_API_KEY", mistralApiKey);
        addKeyIfPresent(keys, "kimi", "KIMI_API_KEY", kimiApiKey);
        addKeyIfPresent(keys, "stepfun", "STEPFUN_API_KEY", stepfunApiKey);
        addKeyIfPresent(keys, "codegeex4", "supremeai.provider.codegeex4.api-key", codegeex4ApiKey);
        
        return keys;
    }

    private void addKeyIfPresent(Map<String, String> keys, String provider, String secretKey, String defaultValue) {
        String key = defaultValue;
        if (key == null || key.isEmpty()) {
            key = defaultValue;
        }
        if (key != null && !key.isEmpty()) {
            keys.put(provider, key);
        }
    }

    private String getEffectiveKey(String secretKey, String defaultValue) {
        return defaultValue;
    }

    @Bean
    public CodeGeeX4Provider codegeex4Provider() {
        return new CodeGeeX4Provider(getEffectiveKey("supremeai.provider.codegeex4.api-key", codegeex4ApiKey));
    }

    @Bean
    public OllamaProvider ollamaProvider() {
        return new OllamaProvider(getEffectiveKey("ai.providers.ollama.api-key", ollamaApiKey));
    }

    @Bean
    public GeminiProvider geminiProvider() {
        return new GeminiProvider(getEffectiveKey("GEMINI_API_KEY", geminiApiKey));
    }

    @Bean
    public OpenAIProvider openAIProvider() {
        return new OpenAIProvider(getEffectiveKey("OPENAI_API_KEY", openaiApiKey));
    }

    @Bean
    public GroqProvider groqProvider() {
        return new GroqProvider(getEffectiveKey("GROQ_API_KEY", groqApiKey));
    }

    @Bean
    public AnthropicProvider anthropicProvider() {
        return new AnthropicProvider(getEffectiveKey("ANTHROPIC_API_KEY", anthropicApiKey));
    }

    @Bean
    public DeepSeekProvider deepSeekProvider() {
        return new DeepSeekProvider(getEffectiveKey("DEEPSEEK_API_KEY", deepseekApiKey));
    }

    @Bean
    public MistralProvider mistralProvider() {
        return new MistralProvider(getEffectiveKey("MISTRAL_API_KEY", mistralApiKey));
    }
}
