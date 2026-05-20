package com.supremeai.provider;



import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 */
@Component
public class AnthropicProvider extends AbstractHttpProvider {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String DEFAULT_MODEL = "claude-3-sonnet-20240229";
    private static final List<String> SUPPORTED_MODELS = List.of(
        "claude-3-opus-20240229",
        "claude-3-sonnet-20240229",
        "claude-3-haiku-20240307"
    );

    public AnthropicProvider() {
        this("");
    }

    public AnthropicProvider(@Value("${anthropic.api-key:}") String apiKey) {
        super(apiKey, API_URL, DEFAULT_MODEL);
    }

    public AnthropicProvider(String apiKey, String baseUrl, String model) {
        super(apiKey, baseUrl != null && !baseUrl.isEmpty() ? baseUrl : API_URL, model != null && !model.isEmpty() ? model : DEFAULT_MODEL);
    }

    @Override
    public String getName() {
        return "anthropic";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        if (providerMetadataService != null) {
            return super.getCapabilities();
        }
        return Map.of(
            "name", "Anthropic",
            "models", SUPPORTED_MODELS,
            "type", "remote",
            "url", baseUrl
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", getModel(),
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
