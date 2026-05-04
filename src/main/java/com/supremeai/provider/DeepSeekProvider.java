package com.supremeai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 */
@Component
public class DeepSeekProvider extends AbstractHttpProvider {
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekProvider.class);
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private final String model;

    public DeepSeekProvider(String apiKey) {
        this(apiKey, "deepseek-coder");
    }

    public DeepSeekProvider(String apiKey, String model) {
        super(apiKey, API_URL, model);
        this.model = model;
    }

    @Override
    public String getName() {
        return "deepseek";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "DeepSeek",
                "model", model,
                "type", "remote",
                "description", "DeepSeek Coder - Specialized for code generation"
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", model,
                "temperature", 0.7,
                "max_tokens", 4000
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
        return "No response from DeepSeek.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        builder.addHeader("Authorization", "Bearer " + apiKey);
    }
}
