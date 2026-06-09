package com.supremeai.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class UltraplinianSecurityService {
    private static final Logger logger = LoggerFactory.getLogger(UltraplinianSecurityService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${ultraplinian.api.url:}")
    private String ultraplinianApiUrl;
    @Value("${ultraplinian.api.key:}")
    private String ultraplinianApiKey;

    public UltraplinianSecurityService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String executeJailbreakTest(String prompt, String boundary) throws IOException {
        if (ultraplinianApiKey == null || ultraplinianApiKey.isBlank()) {
            throw new IllegalStateException("Ultraplinian API key not configured");
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("prompt", prompt);
        body.put("boundary_filter", boundary);
        body.put("depth", 3);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(ultraplinianApiUrl + "/v1/jailbreak/test")
                .addHeader("Authorization", "Bearer " + ultraplinianApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ultraplinian HTTP " + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "{}";
            return respBody;
        }
    }

    public Map<String, Object> probeBoundary(String policy, String input) throws IOException {
        var body = new java.util.HashMap<String, Object>();
        body.put("policy", policy);
        body.put("input", input);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(ultraplinianApiUrl + "/v1/jailbreak/probe")
                .addHeader("Authorization", "Bearer " + ultraplinianApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ultraplinian probe HTTP " + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "{}";
            return objectMapper.readValue(respBody, Map.class);
        }
    }
}
