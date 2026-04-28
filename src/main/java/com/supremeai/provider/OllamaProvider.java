package com.supremeai.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Optimized Ollama provider implementation
 * Works as local fallback when cloud providers are unavailable
 */
@Component
public class OllamaProvider implements AIProvider {

    @Override
    public String getName() {
        return "ollama";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "model", "llama3:70b",
                "local", true,
                "maxContext", 8192
        );
    }

    private static final Logger logger = LoggerFactory.getLogger(OllamaProvider.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String baseUrl;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public OllamaProvider(
            @Value("${ai.providers.ollama.model:llama3.1:8b}") String model,
            @Value("${ai.providers.ollama.endpoint:http://localhost:11434/api/generate}") String baseUrl) {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(180))
                .readTimeout(Duration.ofSeconds(180))
                .writeTimeout(Duration.ofSeconds(180))
                .build();
        this.objectMapper = new ObjectMapper();
        this.model = model;
        this.baseUrl = baseUrl;
        logger.info("OllamaProvider initialized with model: {} at {}", model, baseUrl);
    }

    @Override
    public Mono<String> generate(String prompt) {
        return Mono.fromCallable(() -> {
            OllamaRequest requestPayload = new OllamaRequest(
                    model,
                    prompt,
                    false,
                    new OllamaOptions(
                            8192,
                            0.7f,
                            0.95f,
                            64,
                            Runtime.getRuntime().availableProcessors()
                    )
            );

            try {
                String requestBody = objectMapper.writeValueAsString(requestPayload);
                RequestBody body = RequestBody.create(requestBody, JSON);
                Request request = new Request.Builder()
                        .url(baseUrl)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        throw new IOException("Unexpected code " + response);
                    }
                    String responseBody = response.body().string();
                    OllamaResponse ollamaResponse = objectMapper.readValue(responseBody, OllamaResponse.class);
                    return ollamaResponse.response();
                }
            } catch (Exception e) {
                logger.error("Ollama generation failed: {}", e.getMessage());
                throw new RuntimeException("Ollama unavailable", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Optimized Ollama request with performance tuning parameters
     */
    record OllamaRequest(
            String model,
            String prompt,
            boolean stream,
            OllamaOptions options
    ) {}

     record OllamaOptions(
             @JsonProperty("num_ctx") int numCtx,
             float temperature,
             @JsonProperty("top_p") float topP,
             @JsonProperty("num_batch") int numBatch,
             @JsonProperty("num_thread") int numThread,
             @JsonProperty("num_gpu") int numGpu
     ) {}

    record OllamaResponse(
            String response,
            boolean done,
            @JsonProperty("total_duration") long totalDuration,
            @JsonProperty("eval_count") long evalCount,
            @JsonProperty("eval_duration") long evalDuration
    ) {}
}
