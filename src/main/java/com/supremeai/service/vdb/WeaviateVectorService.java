package com.supremeai.service.vdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WeaviateVectorService {
  private static final Logger logger = LoggerFactory.getLogger(WeaviateVectorService.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${weaviate.api.url:http://localhost:8080}")
  private String weaviateUrl;

  @Value("${weaviate.api.key:}")
  private String weaviateApiKey;

  public WeaviateVectorService() {
    this.httpClient =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    this.objectMapper = new ObjectMapper();
  }

  public String upsert(String className, Map<String, Object> object, String id) throws IOException {
    var body = new java.util.HashMap<String, Object>();
    body.put("class", className);
    body.put("id", id);
    body.put("properties", object);
    String bodyStr = objectMapper.writeValueAsString(body);
    Request.Builder builder =
        new Request.Builder()
            .url(weaviateUrl + "/v1/objects")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON));
    if (weaviateApiKey != null && !weaviateApiKey.isBlank()) {
      builder.addHeader("Authorization", "Bearer " + weaviateApiKey);
    }
    try (Response response = httpClient.newCall(builder.build()).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Weaviate upsert failed HTTP " + response.code());
      }
      return response.body() != null ? response.body().string() : "ok";
    }
  }

  public List<Map<String, Object>> semanticSearch(String className, String query, int limit)
      throws IOException {
    var body = new java.util.HashMap<String, Object>();
    body.put("class", className);
    body.put("query", query);
    body.put("limit", limit);
    String bodyStr = objectMapper.writeValueAsString(body);
    Request.Builder builder =
        new Request.Builder()
            .url(weaviateUrl + "/v1/graphql")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON));
    if (weaviateApiKey != null && !weaviateApiKey.isBlank()) {
      builder.addHeader("Authorization", "Bearer " + weaviateApiKey);
    }
    try (Response response = httpClient.newCall(builder.build()).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Weaviate search failed HTTP " + response.code());
      }
      String respBody = response.body() != null ? response.body().string() : "{}";
      return List.of(Map.of("raw", respBody));
    }
  }

  public boolean isConfigured() {
    return weaviateUrl != null && !weaviateUrl.isBlank();
  }
}
