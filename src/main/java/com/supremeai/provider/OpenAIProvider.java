package com.supremeai.provider;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * OpenAI Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 */
@Component
public class OpenAIProvider extends AbstractHttpProvider {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    public OpenAIProvider(String apiKey) {
        super(apiKey, API_URL, "gpt-3.5-turbo");
    }

    @Override
    public String getName() {
        return "openai";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "OpenAI",
                "models", new String[]{"gpt-4", "gpt-3.5-turbo"}
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", "gpt-3.5-turbo"
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
        return "No response from OpenAI.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        builder.addHeader("Authorization", "Bearer " + apiKey);
    }
}
