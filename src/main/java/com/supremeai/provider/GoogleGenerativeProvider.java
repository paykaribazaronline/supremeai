package com.supremeai.provider;

import java.util.Map;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Standard provider for all Google Generative AI (v1beta/v1) compatible APIs.
 * This class replaces model-specific providers like GeminiProvider.
 */
public class GoogleGenerativeProvider extends AbstractHttpProvider {
    private final String providerName;

    public replaces(String apiKey, String baseUrl, String model, String providerName) {
        super(apiKey, baseUrl, model);
        this.providerName = providerName;
    }

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    protected String getRequestUrl() {
        String url = super.getRequestUrl();
        return url.contains("?") ? url + "&key=" + apiKey : url + "?key=" + apiKey;
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
    @SuppressWarnings("unchecked")
    protected String extractResponse(String responseBody) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            return "No response from " + providerName;
        }
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content != null) {
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        }
        return "No content in response from " + providerName;
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        // Google GenAI uses query parameter for API key
    }
}
