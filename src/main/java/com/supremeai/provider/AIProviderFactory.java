package com.supremeai.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    @Autowired(required = false)
    private OllamaProvider ollamaProvider;

    public AIProvider getProvider(String name) {
        return getProvider(name, null);
    }

    public AIProvider getProvider(String name, String overrideApiKey) {
        switch (name.toLowerCase()) {
            case "groq":
                return new GroqProvider(resolveKey(overrideApiKey, groqApiKey));
            case "openai":
                return new OpenAIProvider(resolveKey(overrideApiKey, openaiApiKey));
            case "anthropic":
                return new AnthropicProvider(resolveKey(overrideApiKey, anthropicApiKey));
            case "gemini":
                return new GeminiProvider(resolveKey(overrideApiKey, geminiApiKey));
            case "huggingface":
                return new HuggingFaceProvider(resolveKey(overrideApiKey, huggingfaceApiKey));
            case "ollama":
                if (ollamaProvider == null) {
                    throw new IllegalStateException("Ollama provider is not available in cloud profile");
                }
                return ollamaProvider;
            default:
                throw new IllegalArgumentException("Unknown AI provider: " + name);
        }
    }

    private String resolveKey(String override, String fallback) {
        return (override != null && !override.isEmpty()) ? override : fallback;
    }
}
