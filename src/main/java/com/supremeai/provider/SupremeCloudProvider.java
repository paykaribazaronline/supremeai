package com.supremeai.provider;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Generic SupremeCloudProvider for all GCP Cloud Run and HF Dedicated Endpoints.
 * Uses ProviderMetadataService to dynamically resolve URLs and Models.
 */
public class SupremeCloudProvider extends AbstractHttpProvider {

    private final String providerName;

    public SupremeCloudProvider(String apiKey, String providerName, String defaultModel, String baseUrl) {
        super(apiKey, baseUrl.endsWith("/") ? baseUrl + "api/generate" : baseUrl + "/api/generate", defaultModel);
        this.providerName = providerName;
    }

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
            "name", providerName,
            "model", getModel(),
            "type", "cloud-native"
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "model", getModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 1024,
                "temperature", 0.7
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String extractResponse(String responseBody) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            return "No response from cloud-native AI: " + providerName;
        }
        
        Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> first = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) first.get("message");
            if (message != null && message.get("content") != null) {
                return (String) message.get("content");
            }
        }
        return "Empty response from " + providerName;
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        if (apiKey != null && !apiKey.isBlank()) {
            builder.addHeader("Authorization", "Bearer " + apiKey);
        }
    }
}
