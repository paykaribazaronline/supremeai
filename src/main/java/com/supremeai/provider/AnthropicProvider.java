package com.supremeai.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 */
@Component
public class AnthropicProvider extends AbstractHttpProvider {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";

    public AnthropicProvider(String apiKey) {
        super(apiKey, API_URL, "claude-3-sonnet-20240229");
    }

    @Override
    public String getName() {
        return "anthropic";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "Anthropic",
                "models", new String[]{"claude-3-opus-20240229", "claude-3-sonnet-20240229"}
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", "claude-3-sonnet-20240229",
                "max_tokens", 1024
        );
    }

    @Override
    protected String extractResponse(String responseBody) throws Exception {
        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
        if (content != null && !content.isEmpty()) {
            return (String) content.get(0).get("text");
        }
        return "No response from Anthropic.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        builder.addHeader("x-api-key", apiKey)
               .addHeader("anthropic-version", "2023-06-01");
    }

    @Override
    protected void addExtraHeaders(okhttp3.Request.Builder builder) {
        builder.addHeader("Content-Type", "application/json");
    }
}
