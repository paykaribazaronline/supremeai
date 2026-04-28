package com.supremeai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * StepFun (阶跃星辰) Provider Integration
 * Free tier available: https://platform.stepfun.com
 *
 * API Documentation: https://platform.stepfun.com/docs
 * Uses OpenAI-compatible chat completions format
 *
 * Models available:
 * - step-3.5-flash (free tier, fast & efficient)
 * - step-3.5-pro (advanced reasoning, paid)
 * - step-1 (basic model)
 *
 * Free Tier: ~10,000-50,000 tokens/day (varies by account)
 * Rate Limit: ~10-30 RPM (requests per minute)
 */
public class StepFunProvider implements AIProvider {

    private static final String API_URL = "https://api.stepfun.com/v1/chat/completions";
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
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
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("StepFun API key must be provided.");
        }
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
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
    public Mono<String> generate(String prompt) {
        return Mono.fromCallable(() -> {
            try {
                // Build request in OpenAI-compatible format
                Map<String, Object> requestBody = Map.of(
                        "messages", List.of(Map.of("role", "user", "content", prompt)),
                        "model", defaultModel,
                        "temperature", 0.7,
                        "max_tokens", 4000,
                        "stream", false
                );

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errBody = response.body() != null ? response.body().string() : "";

                        // Handle specific error codes
                        if (response.code() == 429) {
                            throw new IOException("STEPFUN_RATE_LIMIT: " + errBody);
                        } else if (response.code() == 401) {
                            throw new IOException("STEPFUN_AUTH_ERROR: Invalid API key");
                        } else if (response.code() == 400) {
                            throw new IOException("STEPFUN_BAD_REQUEST: " + errBody);
                        }
                        throw new IOException("StepFun API Error " + response.code() + ": " + errBody);
                    }

                    String responseBody = response.body().string();
                    Map<String, Object> responseMap = objectMapper.readValue(
                            responseBody,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                    );

                    // Extract response text (standard OpenAI format)
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        Object content = message.get("content");
                        return content != null ? content.toString() : "No content in response";
                    }

                    return "No response from StepFun.";
                }
            } catch (IOException e) {
                throw new RuntimeException("StepFun API call failed: " + e.getMessage(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public String getDefaultModel() {
        return defaultModel;
    }
}
