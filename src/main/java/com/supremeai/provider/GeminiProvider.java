package com.supremeai.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Gemini Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 */
@Component
public class GeminiProvider extends AbstractHttpProvider {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    public GeminiProvider(String apiKey) {
        super(apiKey, API_URL, "gemini-1.5-flash");
    }

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "Google Gemini",
                "models", new String[]{"gemini-1.5-flash", "gemini-1.5-pro"}
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );
    }

    @Override
    protected String extractResponse(String responseBody) throws Exception {
        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        }
        return "No response from Gemini.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        // Gemini uses query parameter for API key
    }

    @Override
    protected String buildUrl() {
        return API_URL + "?key=" + apiKey;
    }
}
