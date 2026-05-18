package com.supremeai.provider;

import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Generic SupremeCloudProvider for all GCP Cloud Run, HF Inference Endpoints, and Render deployments.
 * Supports both OpenAI-compatible and HF Inference API formats.
 */
public class SupremeCloudProvider extends AbstractHttpProvider {

    private final String providerName;
    private final boolean isHfInference;

    public SupremeCloudProvider(String apiKey, String providerName, String defaultModel, String baseUrl) {
        super(apiKey, baseUrl, defaultModel);
        this.providerName = providerName;
        this.isHfInference = providerName.startsWith("hf_") && baseUrl.contains("api-inference.huggingface.co");
    }

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    protected String getRequestUrl() {
        if (isHfInference) {
            return baseUrl; // HF inference uses URL directly
        }
        String url = baseUrl.endsWith("/") ? baseUrl + "v1/chat/completions" : baseUrl + "/v1/chat/completions";
        logger.info("[SupremeCloudProvider] getRequestUrl for {}: {}", providerName, url);
        return url;
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
            "name", providerName,
            "model", getModel(),
            "type", isHfInference ? "huggingface-inference" : "cloud-native"
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        if (isHfInference) {
            return Map.of(
                "inputs", prompt,
                "parameters", Map.of(
                    "max_new_tokens", 512,
                    "temperature", 0.7,
                    "return_full_text", false
                )
            );
        }
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
            return "No response from " + providerName;
        }

        if (isHfInference) {
            // HF Inference API can return a List or a Map
            Object rawResponse = responseBody.trim().startsWith("[")
                ? objectMapper.readValue(responseBody, new TypeReference<List<Map<String, Object>>>() {})
                : objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

            if (rawResponse instanceof List) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) rawResponse;
                if (!list.isEmpty() && list.get(0).containsKey("generated_text")) {
                    return (String) list.get(0).get("generated_text");
                }
            } else if (rawResponse instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) rawResponse;
                if (map.containsKey("generated_text")) {
                    return (String) map.get("generated_text");
                }
                if (map.containsKey("error")) {
                    return "HF Error: " + map.get("error");
                }
            }
            return "Empty HF response";
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
