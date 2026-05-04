package com.supremeai.provider;

import org.springframework.stereotype.Component;

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

    public MistralProvider(String apiKey) {
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

    @Override
    protected String extractResponse(String responseBody) throws Exception {
        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        }
        return "No response from Mistral.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        builder.addHeader("Authorization", "Bearer " + apiKey);
    }
}
