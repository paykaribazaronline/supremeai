package com.supremeai.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * StepFun (阶跃星辰) Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 * Free tier available: https://platform.stepfun.com
 * Uses OpenAI-compatible chat completions format
 */
@Component
public class StepFunProvider extends AbstractHttpProvider {
    private static final String API_URL = "https://api.stepfun.com/v1/chat/completions";
    private final String defaultModel;

    /**
     * Constructor with default model (step-3.5-flash)
     * Use this for free tier access
     */
    public StepFunProvider(String apiKey) {
        this(apiKey, "step-3.5-flash");
    }

    /**
     * Constructor with custom model
     *
     * @param apiKey  StepFun API key (format: sf-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx)
     * @param model   Model name: step-3.5-flash, step-3.5-pro, or step-1
     */
    public StepFunProvider(String apiKey, String model) {
        super(apiKey, API_URL, model);
        this.defaultModel = model;
    }

    @Override
    public String getName() {
        return "stepfun";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "StepFun (阶跃星辰)",
                "provider", "StepFun",
                "models", List.of("step-3.5-flash", "step-3.5-pro", "step-1"),
                "freeTier", "10k-50k tokens/day",
                "rateLimit", "10-30 RPM",
                "supports", List.of("chat", "code", "reasoning", "multimodal"),
                "languages", List.of("zh", "en", "multi"),
                "baseUrl", API_URL
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", defaultModel
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
        return "No response from StepFun.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        builder.addHeader("Authorization", "Bearer " + apiKey);
    }
}
