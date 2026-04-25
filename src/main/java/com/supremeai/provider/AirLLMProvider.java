package com.supremeai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AirLLM Provider
 * Hosted LLM models via AirLLM side-car
 * Configured via application.properties: ai.providers.airllm.*
 */
public class AirLLMProvider implements AIProvider {
    private static final Logger logger = LoggerFactory.getLogger(AirLLMProvider.class);
    private static final MediaType JSON = MediaType.get("application/json");

    private final String endpoint;
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public AirLLMProvider(String endpoint, String apiKey, String model) {
        this.endpoint = endpoint != null && !endpoint.isEmpty() ? endpoint : "https://airllm-default.endpoint/v1/chat/completions";
        this.apiKey = apiKey;
        this.model = model != null && !model.isEmpty() ? model : "mistralai/Mistral-7B-Instruct-v0.3";
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "airllm";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "AirLLM",
                "model", model,
                "type", "remote",
                "endpoint", endpoint,
                "description", "AirLLM - Self-hosted/Cloud LLM with adaptive compression"
        );
    }

    @Override
    public Mono<String> generate(String prompt) {
        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> requestBody = Map.of(
                        "messages", List.of(Map.of("role", "user", "content", prompt)),
                        "model", model,
                        "temperature", 0.7,
                        "max_tokens", 2048
                );

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                Request.Builder requestBuilder = new Request.Builder()
                        .url(endpoint)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(jsonBody, JSON));

                // Add API key if provided
                if (apiKey != null && !apiKey.isEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
                }

                Request request = requestBuilder.build();

                try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        logger.error("AirLLM API error: {} - {}", response.code(), response.message());
                        throw new RuntimeException("AirLLM API error: " + response.code());
                    }

                    Map<String, Object> responseMap = objectMapper.readValue(
                            response.body().string(),
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                    );

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    }
                    return "No response from AirLLM.";
                }
            } catch (Exception e) {
                logger.error("Failed to call AirLLM API", e);
                throw new RuntimeException("Failed to call AirLLM API", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
