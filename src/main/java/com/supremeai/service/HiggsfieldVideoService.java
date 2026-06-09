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
public class HiggsfieldVideoService {
    private static final Logger logger = LoggerFactory.getLogger(HiggsfieldVideoService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${higgsfield.api.key:}")
    private String higgsfieldApiKey;
    @Value("${higgsfield.api.url:https://api.higgsfield.ai/v1}")
    private String higgsfieldApiUrl;

    public HiggsfieldVideoService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(120))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String generateVideo(String prompt, int durationSecs) throws IOException {
        if (higgsfieldApiKey == null || higgsfieldApiKey.isBlank()) {
            throw new IllegalStateException("Higgsfield API key not configured");
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("prompt", prompt);
        body.put("duration", Math.min(durationSecs, 60));
        body.put("quality", "standard");
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(higgsfieldApiUrl + "/videos/generate")
                .addHeader("Authorization", "Bearer " + higgsfieldApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Higgsfield HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "{}";
        }
    }

    public String getVideoStatus(String videoId) throws IOException {
        if (higgsfieldApiKey == null || higgsfieldApiKey.isBlank()) {
            return "{'error':'not_configured'}";
        }
        String url = higgsfieldApiUrl + "/videos/" + videoId + "/status";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + higgsfieldApiKey)
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "{}";
        }
    }

    public boolean isConfigured() {
        return higgsfieldApiKey != null && !higgsfieldApiKey.isBlank();
    }
}
