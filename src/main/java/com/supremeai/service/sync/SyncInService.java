package com.supremeai.service.sync;

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
public class SyncInService {
    private static final Logger logger = LoggerFactory.getLogger(SyncInService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${syncin.api.url:}")
    private String syncinApiUrl;
    @Value("${syncin.api.key:}")
    private String syncinApiKey;

    public SyncInService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String sync(String datasetId, Map<String, Object> payload) throws IOException {
        if (syncinApiKey == null || syncinApiKey.isBlank()) {
            throw new IllegalStateException("Sync-in API key not configured");
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("dataset_id", datasetId);
        body.put("data", payload);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(syncinApiUrl + "/v1/sync")
                .addHeader("Authorization", "Bearer " + syncinApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Sync-in HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "ok";
        }
    }

    public boolean isConfigured() {
        return syncinApiKey != null && !syncinApiKey.isBlank();
    }
}
