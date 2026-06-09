package com.supremeai.service.rag;

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
public class LlamaIndexRagService {
    private static final Logger logger = LoggerFactory.getLogger(LlamaIndexRagService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${llamaindex.api.url:}")
    private String llamaindexApiUrl;

    @Value("${llamaindex.api.key:}")
    private String llamaindexApiKey;

    public LlamaIndexRagService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String ingestDocument(String indexName, String text, Map<String, Object> metadata) throws IOException {
        if (llamaindexApiKey == null || llamaindexApiKey.isBlank() || llamaindexApiUrl == null || llamaindexApiUrl.isBlank()) {
            logger.warn("LlamaIndex not configured; skipping ingestion for index {}", indexName);
            return "skipped-unconfigured";
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("index_name", indexName);
        body.put("text", text);
        body.put("metadata", metadata);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(llamaindexApiUrl + "/ingest")
                .addHeader("Authorization", "Bearer " + llamaindexApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("LlamaIndex ingest failed: HTTP {}", response.code());
                throw new IOException("LlamaIndex ingest HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "ok";
        }
    }

    public List<Map<String, Object>> queryIndex(String indexName, String query, int topK) throws IOException {
        if (llamaindexApiKey == null || llamaindexApiKey.isBlank() || llamaindexApiUrl == null || llamaindexApiUrl.isBlank()) {
            logger.warn("LlamaIndex not configured; returning empty results.");
            return List.of();
        }
        var body = new java.util.HashMap<String, Object>();
        body.put("index_name", indexName);
        body.put("query", query);
        body.put("top_k", topK);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request request = new Request.Builder()
                .url(llamaindexApiUrl + "/query")
                .addHeader("Authorization", "Bearer " + llamaindexApiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("LlamaIndex query failed: HTTP {}", response.code());
                return List.of();
            }
            String respBody = response.body() != null ? response.body().string() : "{}";
            JsonNode root = objectMapper.readTree(respBody);
            JsonNode resultsNode = root.path("results");
            List<Map<String, Object>> results = new ArrayList<>();
            for (JsonNode node : resultsNode) {
                results.add(objectMapper.convertValue(node, Map.class));
            }
            return results;
        }
    }

    public boolean isConfigured() {
        return llamaindexApiKey != null && !llamaindexApiKey.isBlank()
                && llamaindexApiUrl != null && !llamaindexApiUrl.isBlank();
    }
}
