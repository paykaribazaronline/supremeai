package com.supremeai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GeminiProvider implements AIProvider {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private final String apiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiProvider(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Gemini API key must be provided.");
        }
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "Google Gemini",
                "models", new String[]{"gemini-1.5-flash", "gemini-1.5-pro"}
        );
    }

    @Override
    public String generate(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            String urlWithKey = API_URL + "?key=" + apiKey;
            Request request = new Request.Builder()
                    .url(urlWithKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response + " - " + response.body().string());
                }

                Map<String, Object> responseMap = objectMapper.readValue(response.body().string(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    if (content != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
                return "No response from Gemini.";
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to call Gemini API", e);
        }
    }
}
