package com.supremeai.service.vdb;

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
public class TurbovecVectorService {
    private static final Logger logger = LoggerFactory.getLogger(TurbovecVectorService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${turbovec.api.url:}")
    private String turbovecApiUrl;
    @Value("${turbovec.api.key:}")
    private String turbovecApiKey;

    public TurbovecVectorService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String upsert(String collection, String id, List<Float> vector, Map<String, Object> metadata) throws IOException {
        if (turbovecApiKey == null || turbovecApiKey.isBlank()) {
            throw new IllegalStateException("Turbovec API key not configured");
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("collection", collection);
        body.put("id", id);
        body.put("vector", vector);
        body.put("metadata", metadata);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(turbovecApiUrl + "/v1/upsert")
                .addHeader("Authorization", "Bearer " + turbovecApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Turbovec upsert failed HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "ok";
        }
    }

    public List<Map<String, Object>> search(String collection, List<Float> queryVector, int topK) throws IOException {
        if (turbovecApiKey == null || turbovecApiKey.isBlank()) {
            throw new IllegalStateException("Turbovec API key not configured");
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("collection", collection);
        body.put("vector", queryVector);
        body.put("top_k", topK);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(turbovecApiUrl + "/v1/search")
                .addHeader("Authorization", "Bearer " + turbovecApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Turbovec search failed HTTP " + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "{}";
            var root = objectMapper.readTree(respBody);
            var resultsNode = root.path("results");
            List<Map<String, Object>> results = new java.util.ArrayList<>();
            for (var node : resultsNode) {
                results.add(objectMapper.convertValue(node, Map.class));
            }
            return results;
        }
    }

    public boolean isConfigured() {
        return turbovecApiKey != null && !turbovecApiKey.isBlank();
    }
}
