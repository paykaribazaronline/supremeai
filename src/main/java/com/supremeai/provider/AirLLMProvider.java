package com.supremeai.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AirLLM Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 * Hosted LLM models via AirLLM side-car
 */
@Component
public class AirLLMProvider extends AbstractHttpProvider {
    private static final String DEFAULT_ENDPOINT = "https://airllm-default.endpoint/v1/chat/completions";
    private final String endpoint;
    private final String model;

    public AirLLMProvider(String endpoint, String apiKey, String model) {
        super(apiKey, endpoint != null && !endpoint.isEmpty() ? endpoint : DEFAULT_ENDPOINT,
                model != null && !model.isEmpty() ? model : "mistralai/Mistral-7B-Instruct-v0.3");
        this.endpoint = endpoint != null && !endpoint.isEmpty() ? endpoint : DEFAULT_ENDPOINT;
        this.model = model != null && !model.isEmpty() ? model : "mistralai/Mistral-7B-Instruct-v0.3";
    }

    @Override
    public String getName() {
        return "airllm";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "AirLLM",
                "model", model,
                "type", "remote",
                "endpoint", endpoint,
                "description", "AirLLM - Self-hosted/Cloud LLM with adaptive compression"
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", model,
                "temperature", 0.7,
                "max_tokens", 2048
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
        return "No response from AirLLM.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        // Add API key if provided
        if (apiKey != null && !apiKey.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + apiKey);
        }
    }
}
