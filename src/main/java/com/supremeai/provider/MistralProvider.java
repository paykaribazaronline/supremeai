package com.supremeai.provider;



import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.util.Map;

/**
 * Mistral AI Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 */
@Component
public class MistralProvider extends AbstractHttpProvider {
    private static final String API_URL = "https://api.mistral.ai/v1/chat/completions";
    private final String model;

    public MistralProvider() {
        this("", "mistral-large-latest");
    }

    public MistralProvider(@Value("${mistral.api-key:}") String apiKey) {
        this(apiKey, "mistral-large-latest");
    }

    public MistralProvider(String apiKey, String model) {
        super(apiKey, API_URL, model);
        this.model = model;
    }

    @Override
    public String getName() {
        return "mistral";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "Mistral AI",
                "model", model,
                "type", "remote",
                "description", "Mistral AI - High-performance language models"
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", model,
                "temperature", 0.7
        );
    }

    // extractResponse: AbstractHttpProvider-এর OpenAI-compatible default ব্যবহৃত হচ্ছে

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        builder.addHeader("Authorization", "Bearer " + apiKey);
    }
}
