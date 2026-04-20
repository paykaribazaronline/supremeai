package com.supremeai.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Optimized Ollama provider implementation
 * Tuned for maximum performance with local LLaMA 3 70B
 */
@Component
@Profile("!cloud")
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
    private static final String DEFAULT_MODEL = "llama3:70b";
    private static final String BASE_URL = "http://localhost:11434/api/generate";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public OllamaProvider() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();
        this.objectMapper = new ObjectMapper();
        logger.info("OllamaProvider initialized with OkHttpClient");
    }

    @Override
    public String generate(String prompt) {
        OllamaRequest requestPayload = new OllamaRequest(
                DEFAULT_MODEL,
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
                    .url(BASE_URL)
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
            @JsonProperty("num_thread") int numThread
    ) {}

    record OllamaResponse(
            String response,
            boolean done,
            @JsonProperty("total_duration") long totalDuration,
            @JsonProperty("eval_count") long evalCount,
            @JsonProperty("eval_duration") long evalDuration
    ) {}
}
