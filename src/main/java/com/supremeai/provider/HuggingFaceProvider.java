package com.supremeai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HuggingFaceProvider implements AIProvider {
    private static final String API_URL = "https://api-inference.huggingface.co/models/HuggingFaceH4/zephyr-7b-beta";
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HuggingFaceProvider(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("HuggingFace API key must be provided.");
        }
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
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
    public String generate(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "inputs", prompt,
                    "parameters", Map.of("max_new_tokens", 512)
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
                    throw new IOException("Unexpected code " + response + " - " + response.body().string());
                }

                String responseBody = response.body().string();
                // HuggingFace returns an array of objects
                List<?> list = objectMapper.readValue(responseBody, List.class);
                if (list != null && !list.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> first = (Map<String, Object>) list.get(0);
                    String generated = (String) first.get("generated_text");
                    if (generated != null) {
                        // Often the generated text includes the prompt; strip it if so
                        if (generated.startsWith(prompt)) {
                            generated = generated.substring(prompt.length()).trim();
                        }
                        return generated;
                    }
                }
                return "No response from HuggingFace.";
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to call HuggingFace API", e);
        }
    }
}
