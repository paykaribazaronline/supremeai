package com.supremeai.service.analytics;

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
public class PostHogService {
    private static final Logger logger = LoggerFactory.getLogger(PostHogService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${posthog.api.url:}")
    private String posthogUrl;
    @Value("${posthog.api.key:}")
    private String posthogApiKey;

    public PostHogService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(15))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String capture(String event, Map<String, Object> properties) throws IOException {
        if (posthogApiKey == null || posthogApiKey.isBlank() || posthogUrl == null) {
            throw new IllegalStateException("PostHog not configured");
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("event", event);
        body.put("properties", properties);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(posthogUrl + "/batch")
                .addHeader("Authorization", "Bearer " + posthogApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("PostHog HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "ok";
        }
    }

    public boolean isConfigured() {
        return posthogApiKey != null && !posthogApiKey.isBlank() && posthogUrl != null && !posthogUrl.isBlank();
    }
}
