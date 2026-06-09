package com.supremeai.service.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Service
public class PerplexityResearchService {
    private static final Logger logger = LoggerFactory.getLogger(PerplexityResearchService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${perplexity.api.key:}")
    private String perplexityApiKey;

    @Value("${perplexity.api.url:https://api.perplexity.ai/chat/completions}")
    private String perplexityApiUrl;

    public PerplexityResearchService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String search(String query, int maxTokens) throws IOException {
        if (perplexityApiKey == null || perplexityApiKey.isBlank()) {
            return "Perplexity not configured: " + query;
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("model", "llama-3.1-sonar-large-128k-online");
        body.put("max_tokens", maxTokens);
        body.put("messages", List.of(Map.of("role", "user", "content", query)));
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(perplexityApiUrl)
                .addHeader("Authorization", "Bearer " + perplexityApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Perplexity search failed: HTTP {}", response.code());
                throw new IOException("Perplexity HTTP " + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "{}";
            JsonNode root = objectMapper.readTree(respBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
            return respBody;
        }
    }

    public boolean isConfigured() {
        return perplexityApiKey != null && !perplexityApiKey.isBlank();
    }
}
