package com.supremeai.service.agent;

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
public class ProximaAgentService {
    private static final Logger logger = LoggerFactory.getLogger(ProximaAgentService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${proxima.api.url:}")
    private String proximaApiUrl;
    @Value("${proxima.api.key:}")
    private String proximaApiKey;

    public ProximaAgentService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String launchAgent(String task, String context) throws IOException {
        if (proximaApiKey == null || proximaApiKey.isBlank()) {
            throw new IllegalStateException("Proxima API key not configured");
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("task", task);
        body.put("context", context);
        body.put("mode", "autonomous");
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(proximaApiUrl + "/v1/agent/launch")
                .addHeader("Authorization", "Bearer " + proximaApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Proxima HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "{\"status\":\"ok\"}";
        }
    }

    public boolean isConfigured() {
        return proximaApiKey != null && !proximaApiKey.isBlank();
    }
}
