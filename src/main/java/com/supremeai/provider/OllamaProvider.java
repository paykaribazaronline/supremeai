package com.supremeai.provider;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.util.Map;

/**
 * Ollama Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 * Connects to local Ollama server for free, offline AI inference.
 * (অফলাইন এআই ইনফারেন্সের জন্য লোকাল ওলামা সার্ভারের সাথে সংযোগ স্থাপন করে)
 */
@Component
public class OllamaProvider extends AbstractHttpProvider {
    private final String apiUrl;
    private final String modelName;

    public OllamaProvider() {
        this("");
    }

    public OllamaProvider(@Value("${ai.providers.ollama.api-key:ollama}") String apiKey) {
        this(apiKey, "http://localhost:11434", "codegeex4");
    }

    public OllamaProvider(String apiKey, String endpoint, String modelName) {
        super(apiKey, endpoint + "/v1/chat/completions", modelName != null ? modelName : "codegeex4");
        this.apiUrl = endpoint;
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
                "endpoint", apiUrl,
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
