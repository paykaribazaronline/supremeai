package com.supremeai.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Kimi (Moonshot AI) Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 */
@Component
public class KimiProvider extends AbstractHttpProvider {
    private static final String API_URL = "https://api.moonshot.cn/v1/chat/completions";
    private final String model;

    public KimiProvider(String apiKey) {
        this(apiKey, "moonshot-v1-128k");
    }

    public KimiProvider(String apiKey, String model) {
        super(apiKey, API_URL, model);
        this.model = model;
    }

    @Override
    public String getName() {
        return "kimi";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "Kimi (Moonshot AI)",
                "model", model,
                "type", "remote",
                "description", "Kimi AI - Long context processing (128k tokens)"
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
        return "No response from Kimi.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        builder.addHeader("Authorization", "Bearer " + apiKey);
    }
}
