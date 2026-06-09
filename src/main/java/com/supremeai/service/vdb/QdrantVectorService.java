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
public class QdrantVectorService {
    private static final Logger logger = LoggerFactory.getLogger(QdrantVectorService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${qdrant.api.url:http://localhost:6333}")
    private String qdrantUrl;
    @Value("${qdrant.api.key:}")
    private String qdrantApiKey;

    public QdrantVectorService() {
        this.httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String upsert(String collection, String id, List<Float> vector, Map<String, Object> metadata) throws IOException {
        var body = new java.util.HashMap<String, Object>();
        body.put("points", List.of(Map.of("id", id, "vector", vector, "payload", metadata)));
        String bodyStr = objectMapper.writeValueAsString(body);
        Request.Builder builder = new Request.Builder()
                .url(qdrantUrl + "/collections/" + collection + "/points")
                .addHeader("Content-Type", "application/json")
                .put(RequestBody.create(bodyStr, JSON));
        if (qdrantApiKey != null && !qdrantApiKey.isBlank()) {
            builder.addHeader("api-key", qdrantApiKey);
        }
        try (Response response = httpClient.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Qdrant upsert failed HTTP " + response.code());
            }
            return response.body() != null ? response.body().string() : "ok";
        }
    }

    public List<Map<String, Object>> search(String collection, List<Float> queryVector, int topK) throws IOException {
        var body = new java.util.HashMap<String, Object>();
        body.put("vector", queryVector);
        body.put("limit", topK);
        body.put("with_payload", true);
        String bodyStr = objectMapper.writeValueAsString(body);
        Request.Builder builder = new Request.Builder()
                .url(qdrantUrl + "/collections/" + collection + "/points/search")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON));
        if (qdrantApiKey != null && !qdrantApiKey.isBlank()) {
            builder.addHeader("api-key", qdrantApiKey);
        }
        try (Response response = httpClient.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Qdrant search failed HTTP " + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "{}";
            var root = objectMapper.readTree(respBody);
            var results = root.path("result");
            List<Map<String, Object>> out = new java.util.ArrayList<>();
            for (var r : results.path("points")) {
                out.add(objectMapper.convertValue(r, Map.class));
            }
            return out;
        }
    }

    public boolean isConfigured() {
        return qdrantUrl != null && !qdrantUrl.isBlank();
    }
}
