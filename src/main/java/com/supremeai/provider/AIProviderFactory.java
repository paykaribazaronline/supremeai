package com.supremeai.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory for creating AI provider instances
 * Supports 10 AI models for voting system (S4)
 */
@Component
public class AIProviderFactory {

    @Value("${supremeai.provider.groq.api-key}")
    private String groqApiKey;

    @Value("${supremeai.provider.openai.api-key}")
    private String openaiApiKey;

    @Value("${supremeai.provider.anthropic.api-key}")
    private String anthropicApiKey;

    @Value("${supremeai.provider.gemini.api-key}")
    private String geminiApiKey;

    @Value("${supremeai.provider.huggingface.api-key}")
    private String huggingfaceApiKey;

    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${ai.kimi.api-key:}")
    private String kimiApiKey;

    @Value("${ai.providers.airllm.endpoint:}")
    private String airllmEndpoint;

    @Value("${ai.providers.airllm.api-key:}")
    private String airllmApiKey;

    @Value("${ai.providers.airllm.model:mistralai/Mistral-7B-Instruct-v0.3}")
    private String airllmModel;

    @Value("${ai.providers.mistral.api-key:}")
    private String mistralApiKey;

    @Autowired(required = false)
    private OllamaProvider ollamaProvider;

    public AIProvider getProvider(String name) {
        return getProvider(name, null);
    }

    public AIProvider getProvider(String name, String overrideApiKey) {
        switch (name.toLowerCase()) {
            // Core 10 AI Models for S4 Voting System
            case "gpt4":
            case "openai":
                return new OpenAIProvider(resolveKey(overrideApiKey, openaiApiKey));

            case "claude":
            case "anthropic":
                return new AnthropicProvider(resolveKey(overrideApiKey, anthropicApiKey));

            case "gemini":
                return new GeminiProvider(resolveKey(overrideApiKey, geminiApiKey));

            case "groq":
                return new GroqProvider(resolveKey(overrideApiKey, groqApiKey));

            case "deepseek":
                return new DeepSeekProvider(resolveKey(overrideApiKey, deepseekApiKey));

            case "ollama":
                if (ollamaProvider == null) {
                    throw new IllegalStateException("Ollama provider is not available in cloud profile");
                }
                return ollamaProvider;

            case "huggingface":
                return new HuggingFaceProvider(resolveKey(overrideApiKey, huggingfaceApiKey));

            case "airllm":
                return new AirLLMProvider(airllmEndpoint, resolveKey(overrideApiKey, airllmApiKey), airllmModel);

            case "kimi":
                return new KimiProvider(resolveKey(overrideApiKey, kimiApiKey));

            case "mistral":
                return new MistralProvider(resolveKey(overrideApiKey, mistralApiKey));

            default:
                throw new IllegalArgumentException("Unknown AI provider: " + name + ". Supported: gpt4, claude, gemini, groq, deepseek, ollama, huggingface, airllm, kimi, mistral");
        }
    }

    /**
     * Get list of all supported provider names
     */
    public String[] getSupportedProviders() {
        return new String[]{"gpt4", "claude", "gemini", "groq", "deepseek", "ollama", "huggingface", "airllm", "kimi", "mistral"};
    }

    /**
     * Get list of all supported provider names (alias for getSupportedProviders)
     */
    public String[] getAllProviderNames() {
        return getSupportedProviders();
    }

    private String resolveKey(String override, String fallback) {
        return (override != null && !override.isEmpty()) ? override : fallback;
    }
}
