package com.supremeai.provider;

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

    public AIProvider getProvider(String name) {
        switch (name.toLowerCase()) {
            case "groq":
                return new GroqProvider(groqApiKey);
            case "openai":
                return new OpenAIProvider(openaiApiKey);
            case "anthropic":
                return new AnthropicProvider(anthropicApiKey);
            default:
                throw new IllegalArgumentException("Unknown AI provider: " + name);
        }
    }
}
