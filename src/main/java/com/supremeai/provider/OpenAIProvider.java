package com.supremeai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class OpenAIProvider implements AIProvider {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIProvider(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key must be provided.");
        }
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "openai";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "OpenAI",
                "models", new String[]{"gpt-4", "gpt-3.5-turbo"}
        );
    }

    @Override
    public String generate(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "model", "gpt-3.5-turbo"
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
                    throw new IOException("Unexpected code " + response);
                }

                Map<String, Object> responseMap = objectMapper.readValue(response.body().string(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
                return "No response from OpenAI.";
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to call OpenAI API", e);
        }
    }
}
