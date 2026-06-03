package com.supremeai.provider;

import java.util.Map;
import java.util.List;

/**
 * Standard provider for local inference engines like Ollama.
 * Connects to local server for offline AI capabilities.
 */
public class LocalInferenceProvider extends AbstractHttpProvider {
    private final String providerName;

    public LocalInferenceProvider(String apiKey, String endpoint, String modelName, String providerName) {
        super(apiKey, (endpoint != null ? endpoint : "http://localhost:11434") + "/v1/chat/completions", modelName != null ? modelName : "default");
        this.providerName = providerName;
    }

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "model", getModel(),
            "stream", false
        );
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        // Local servers typically don't require auth headers
    }
}
