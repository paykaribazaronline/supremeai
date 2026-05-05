package com.supremeai.config;

import com.supremeai.provider.*;
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
        if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
            keys.put("openai", openaiApiKey);
            keys.put("gpt4", openaiApiKey);
        }
        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            keys.put("gemini", geminiApiKey);
        }
        if (groqApiKey != null && !groqApiKey.isEmpty()) {
            keys.put("groq", groqApiKey);
        }
        if (anthropicApiKey != null && !anthropicApiKey.isEmpty()) {
            keys.put("anthropic", anthropicApiKey);
            keys.put("claude", anthropicApiKey);
        }
        if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
            keys.put("deepseek", deepseekApiKey);
        }
        if (mistralApiKey != null && !mistralApiKey.isEmpty()) {
            keys.put("mistral", mistralApiKey);
        }
        if (kimiApiKey != null && !kimiApiKey.isEmpty()) {
            keys.put("kimi", kimiApiKey);
        }
        if (stepfunApiKey != null && !stepfunApiKey.isEmpty()) {
            keys.put("stepfun", stepfunApiKey);
        }
        if (codegeex4ApiKey != null && !codegeex4ApiKey.isEmpty()) {
            keys.put("codegeex4", codegeex4ApiKey);
        }
        return keys;
    }

    @Bean
    public CodeGeeX4Provider codegeex4Provider() {
        return new CodeGeeX4Provider(codegeex4ApiKey);
    }

    @Bean
    public OllamaProvider ollamaProvider() {
        return new OllamaProvider(ollamaApiKey);
    }

    @Bean
    public GeminiProvider geminiProvider() {
        return new GeminiProvider(geminiApiKey);
    }

    @Bean
    public OpenAIProvider openAIProvider() {
        return new OpenAIProvider(openaiApiKey);
    }

    @Bean
    public GroqProvider groqProvider() {
        return new GroqProvider(groqApiKey);
    }

    @Bean
    public AnthropicProvider anthropicProvider() {
        return new AnthropicProvider(anthropicApiKey);
    }

    @Bean
    public DeepSeekProvider deepSeekProvider() {
        return new DeepSeekProvider(deepseekApiKey);
    }

    @Bean
    public MistralProvider mistralProvider() {
        return new MistralProvider(mistralApiKey);
    }
}
