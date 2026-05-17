package com.supremeai.provider;

import java.util.Map;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Standard provider for all Anthropic compatible APIs.
 * This class replaces model-specific providers like AnthropicProvider/ClaudeProvider.
 */
public class AnthropicChatProvider extends AbstractHttpProvider {
    private final String providerName;

    public AnthropicChatProvider(String apiKey, String baseUrl, String model, String providerName) {
        super(apiKey, baseUrl, model);
        this.providerName = providerName;
    }

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
            "model", getModel(),
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "max_tokens", 1024
        );
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        if (apiKey != null && !apiKey.isBlank()) {
            builder.addHeader("x-api-key", apiKey);
            builder.addHeader("anthropic-version", "2023-06-01");
            builder.addHeader("content-type", "application/json");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String extractResponse(String responseBody) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            return "No response from " + providerName;
        }
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
        if (content != null && !content.isEmpty()) {
            return (String) content.get(0).get("text");
        }
        return "No content in response from " + providerName;
    }
}
