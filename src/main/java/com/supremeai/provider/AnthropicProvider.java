package com.supremeai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AnthropicProvider implements AIProvider {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicProvider(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Anthropic API key must be provided.");
        }
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "anthropic";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "Anthropic",
                "models", new String[]{"claude-3-opus-20240229", "claude-3-sonnet-20240229"}
        );
    }

    @Override
    public String generate(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "model", "claude-3-sonnet-20240229",
                    "max_tokens", 1024
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response + " - " + response.body().string());
                }

                Map<String, Object> responseMap = objectMapper.readValue(response.body().string(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
                if (content != null && !content.isEmpty()) {
                    return (String) content.get(0).get("text");
                }
                return "No response from Anthropic.";
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to call Anthropic API", e);
        }
    }
}
