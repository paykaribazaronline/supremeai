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
public class ReplicateModelService {
    private static final Logger logger = LoggerFactory.getLogger(ReplicateModelService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${replicate.api.key:}")
    private String replicateApiKey;
    @Value("${replicate.api.url:https://api.replicate.com/v1}")
    private String replicateApiUrl;

    public ReplicateModelService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(120))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String runModel(String modelVersion, Map<String, Object> input) throws IOException {
        if (replicateApiKey == null || replicateApiKey.isBlank()) {
            throw new IllegalStateException("Replicate API key not configured");
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("version", modelVersion);
        body.put("input", input);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(replicateApiUrl + "/predictions")
                .addHeader("Authorization", "Token " + replicateApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Replicate HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "{}";
        }
    }

    public boolean isConfigured() {
        return replicateApiKey != null && !replicateApiKey.isBlank();
    }
}
