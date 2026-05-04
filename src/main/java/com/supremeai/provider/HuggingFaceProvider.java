package com.supremeai.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * HuggingFace Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 */
@Component
public class HuggingFaceProvider extends AbstractHttpProvider {
    private static final String API_URL = "https://api-inference.huggingface.co/models/HuggingFaceH4/zephyr-7b-beta";

    public HuggingFaceProvider(String apiKey) {
        super(apiKey, API_URL, "zephyr-7b-beta");
    }

    @Override
    public String getName() {
        return "huggingface";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "HuggingFace",
                "models", new String[]{"zephyr-7b-beta"}
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "inputs", prompt,
                "parameters", Map.of("max_new_tokens", 512)
        );
    }

    @Override
    protected String extractResponse(String responseBody) throws Exception {
        // HuggingFace returns an array of objects
        List<?> list = objectMapper.readValue(responseBody, List.class);
        if (list != null && !list.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> first = (Map<String, Object>) list.get(0);
            String generated = (String) first.get("generated_text");
            if (generated != null) {
                return generated;
            }
        }
        return "No response from HuggingFace.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        builder.addHeader("Authorization", "Bearer " + apiKey);
    }
}
