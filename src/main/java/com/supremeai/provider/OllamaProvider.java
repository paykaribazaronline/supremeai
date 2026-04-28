package com.supremeai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Ollama Provider for SupremeAI
 * Connects to local Ollama server (localhost:11434) for free, offline AI inference.
 * Supports any model installed in Ollama (codegeex4, llama2, mistral, etc.)
 */
public class OllamaProvider implements AIProvider {

    private static final String API_URL = "http://localhost:11434/v1/chat/completions";
    private final String apiKey; // Not used for local Ollama, but kept for interface compatibility
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String modelName;

    public OllamaProvider(String apiKey) {
        this(apiKey, "codegeex4"); // Default to CodeGeeX4 model
    }

    public OllamaProvider(String apiKey, String modelName) {
        this.apiKey = apiKey; // Can be null for local Ollama
        this.modelName = modelName != null ? modelName : "codegeex4";
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
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
    public Mono<String> generate(String prompt) {
        return Mono.fromCallable(() -> {
            Map<String, Object> requestBody = Map.of(
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "model", modelName,
                    "stream", false
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                Map<String, Object> responseMap = objectMapper.readValue(response.body().string(),
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
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
