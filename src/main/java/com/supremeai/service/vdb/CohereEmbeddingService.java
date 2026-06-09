package com.supremeai.service.vdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CohereEmbeddingService {
  private static final Logger logger = LoggerFactory.getLogger(CohereEmbeddingService.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${cohere.api.key:}")
  private String cohereApiKey;

  @Value("${cohere.api.url:https://api.cohere.ai/v1}")
  private String cohereApiUrl;

  public CohereEmbeddingService() {
    this.httpClient =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    this.objectMapper = new ObjectMapper();
  }

  public List<Double> embed(String text) throws IOException {
    if (cohereApiKey == null || cohereApiKey.isBlank()) {
      throw new IllegalStateException("Cohere API key not configured");
    }
    var body = new java.util.HashMap<String, Object>();
    body.put("model", "embed-english-v3.0");
    body.put("texts", List.of(text));
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(cohereApiUrl + "/embed")
            .addHeader("Authorization", "Bearer " + cohereApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Cohere HTTP " + response.code());
      }
      String respBody = response.body() != null ? response.body().string() : "{}";
      var root = objectMapper.readTree(respBody);
      var embeddings = root.path("embeddings");
      if (embeddings.isArray() && !embeddings.isEmpty()) {
        var arr = embeddings.get(0);
        List<Double> floats = new java.util.ArrayList<>();
        for (var v : arr) floats.add(v.asDouble());
        return floats;
      }
      return List.of();
    }
  }

  public List<String> rerank(String query, List<String> documents, int topN) throws IOException {
    if (cohereApiKey == null || cohereApiKey.isBlank()) {
      throw new IllegalStateException("Cohere API key not configured");
    }
    var body = new java.util.HashMap<String, Object>();
    body.put("model", "rerank-english-v3.0");
    body.put("query", query);
    body.put("documents", documents);
    if (topN > 0) body.put("top_n", topN);
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(cohereApiUrl + "/rerank")
            .addHeader("Authorization", "Bearer " + cohereApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Cohere rerank HTTP " + response.code());
      }
      String respBody = response.body() != null ? response.body().string() : "{}";
      var root = objectMapper.readTree(respBody);
      var results = root.path("results");
      List<String> out = new java.util.ArrayList<>();
      for (var r : results) {
        String doc = r.path("document").path("text").asText();
        out.add(doc);
      }
      return out;
    }
  }

  public boolean isConfigured() {
    return cohereApiKey != null && !cohereApiKey.isBlank();
  }
}
