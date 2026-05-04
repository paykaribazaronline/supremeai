package com.supremeai.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Ollama Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 * Connects to local Ollama server (localhost:11434) for free, offline AI inference.
 */
@Component
public class OllamaProvider extends AbstractHttpProvider {
    private static final String API_URL = "http://localhost:11434/v1/chat/completions";
    private final String modelName;

    public OllamaProvider(String apiKey) {
        this(apiKey, "codegeex4");
    }

    public OllamaProvider(String apiKey, String modelName) {
        super(apiKey, API_URL, modelName != null ? modelName : "codegeex4");
        this.modelName = modelName != null ? modelName : "codegeex4";
    }

    @Override
    public String getName() {
        return "ollama";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "Ollama (Local)",
                "models", new String[]{modelName},
                "endpoint", "http://localhost:11434",
                "offline", true,
                "free", true
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", modelName,
                "stream", false
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
        return "No response from Ollama.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        // Ollama local server doesn't require authentication
    }
}
