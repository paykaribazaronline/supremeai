package com.supremeai.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Groq Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 */
@Component
public class GroqProvider extends AbstractHttpProvider {
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public GroqProvider(String apiKey) {
        super(apiKey, API_URL, "mixtral-8x7b-32768");
    }

    @Override
    public String getName() {
        return "groq";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "Groq",
                "models", new String[]{"llama2-70b-4096", "mixtral-8x7b-32768", "gemma-7b-it"}
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", "mixtral-8x7b-32768"
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
        return "No response from Groq.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        builder.addHeader("Authorization", "Bearer " + apiKey);
    }
}
