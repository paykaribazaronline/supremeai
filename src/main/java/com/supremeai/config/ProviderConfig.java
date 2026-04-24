package com.supremeai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ProviderConfig {
    
    @Bean
    public Map<String, String> providerApiKeys(
            @Value("${OPENAI_API_KEY:}") String openai,
            @Value("${GEMINI_API_KEY:}") String gemini,
            @Value("${ANTHROPIC_API_KEY:}") String anthropic,
            @Value("${COHERE_API_KEY:}") String cohere,
            @Value("${PERPLEXITY_API_KEY:}") String perplexity,
            @Value("${MISTRAL_API_KEY:}") String mistral,
            @Value("${LLAMA_API_KEY:}") String llama,
            @Value("${DEEPSEEK_API_KEY:}") String deepseek,
            @Value("${GROK_API_KEY:}") String grok,
            @Value("${AZURE_OPENAI_API_KEY:}") String azure,
            @Value("${CUSTOM_AI_API_KEY:}") String custom) {
        
        Map<String, String> keys = new HashMap<>();
        if (!openai.isBlank()) keys.put("openai", openai);
        if (!gemini.isBlank()) keys.put("gemini", gemini);
        if (!anthropic.isBlank()) keys.put("anthropic", anthropic);
        if (!cohere.isBlank()) keys.put("cohere", cohere);
        if (!perplexity.isBlank()) keys.put("perplexity", perplexity);
        if (!mistral.isBlank()) keys.put("mistral", mistral);
        if (!llama.isBlank()) keys.put("llama", llama);
        if (!deepseek.isBlank()) keys.put("deepseek", deepseek);
        if (!grok.isBlank()) keys.put("grok", grok);
        if (!azure.isBlank()) keys.put("azure", azure);
        if (!custom.isBlank()) keys.put("custom", custom);
        
        return keys;
    }
}
