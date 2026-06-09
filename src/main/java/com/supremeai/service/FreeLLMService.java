package com.supremeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Service
public class FreeLLMService {
    private static final Logger logger = LoggerFactory.getLogger(FreeLLMService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${freellm.api.url:}")
    private String freeLLMApiUrl;
    @Value("${freellm.api.key:}")
    private String freeLLMApiKey;

    public FreeLLMService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String model, String prompt) throws IOException {
        if (freeLLMApiKey == null || freeLLMApiKey.isBlank()) {
            throw new IllegalStateException("FreeLLM API key not configured");
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("model", model);
        body.put("messages", java.util.List.of(Map.of("role", "user", "content", prompt)));
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(freeLLMApiUrl + "/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + freeLLMApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("FreeLLM HTTP " + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "{}";
            var root = objectMapper.readTree(respBody);
            var choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
            return respBody;
        }
    }

    public boolean isConfigured() {
        return freeLLMApiKey != null && !freeLLMApiKey.isBlank();
    }
}
