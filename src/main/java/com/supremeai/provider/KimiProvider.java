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
 * Kimi (Moonshot AI) Provider
 * API: https://api.moonshot.cn/v1/chat/completions
 */
public class KimiProvider implements AIProvider {
    private static final Logger logger = LoggerFactory.getLogger(KimiProvider.class);
    private static final String API_URL = "https://api.moonshot.cn/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json");

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public KimiProvider(String apiKey) {
        this(apiKey, "moonshot-v1-128k");
    }

    public KimiProvider(String apiKey, String model) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Kimi API key must be provided.");
        }
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "kimi";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "Kimi (Moonshot AI)",
                "model", model,
                "type", "remote",
                "description", "Kimi AI - Long context processing (128k tokens)"
        );
    }

    @Override
    public Mono<String> generate(String prompt) {
        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> requestBody = Map.of(
                        "messages", List.of(Map.of("role", "user", "content", prompt)),
                        "model", model,
                        "temperature", 0.7
                );

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(jsonBody, JSON))
                        .build();

                try (okhttp3.Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        logger.error("Kimi API error: {} - {}", response.code(), response.message());
                        throw new RuntimeException("Kimi API error: " + response.code());
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
                    return "No response from Kimi.";
                }
            } catch (Exception e) {
                logger.error("Failed to call Kimi API", e);
                throw new RuntimeException("Failed to call Kimi API", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
