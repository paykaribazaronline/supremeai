package com.supremeai.provider;

import com.supremeai.service.AIProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory for creating AI provider instances
 * Supports 10 AI models for voting system (S4)
 */
@Component
public class AIProviderFactory {

    @Autowired
    private AIProviderService aiProviderService;

    @Value("${ai.providers.airllm.endpoint:}")
    private String airllmEndpoint;

    @Value("${ai.providers.airllm.model:mistralai/Mistral-7B-Instruct-v0.3}")
    private String airllmModel;

    @Autowired(required = false)
    private OllamaProvider ollamaProvider;

    public AIProvider getProvider(String name) {
        return getProvider(name, null);
    }

    public AIProvider getProvider(String name, String overrideApiKey) {
        String key = (overrideApiKey != null && !overrideApiKey.isEmpty()) 
                     ? overrideApiKey 
                     : aiProviderService.getActiveKey(name.toLowerCase());

        switch (name.toLowerCase()) {
            // Core 10 AI Models for S4 Voting System
            case "gpt4":
            case "openai":
                return new OpenAIProvider(key);

            case "claude":
            case "anthropic":
                return new AnthropicProvider(key);

            case "gemini":
                return new GeminiProvider(key);

            case "groq":
                return new GroqProvider(key);

            case "deepseek":
                return new DeepSeekProvider(key);

            case "ollama":
                if (ollamaProvider == null) {
                    throw new IllegalStateException("Ollama provider is not available in cloud profile");
                }
                return ollamaProvider;

            case "huggingface":
                return new HuggingFaceProvider(key);

            case "airllm":
                return new AirLLMProvider(airllmEndpoint, key, airllmModel);

            case "kimi":
                return new KimiProvider(key);

            case "mistral":
                return new MistralProvider(key);

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
