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
public class PineconeVectorService {
  private static final Logger logger = LoggerFactory.getLogger(PineconeVectorService.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${pinecone.api.key:}")
  private String pineconeApiKey;

  @Value("${pinecone.environment:}")
  private String pineconeEnvironment;

  @Value("${pinecone.index:supremeai-index}")
  private String pineconeIndex;

  public PineconeVectorService() {
    this.httpClient =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    this.objectMapper = new ObjectMapper();
  }

  public String upsert(String id, List<Float> vector, Map<String, Object> metadata)
      throws IOException {
    if (pineconeApiKey == null || pineconeApiKey.isBlank()) {
      throw new IllegalStateException("Pinecone API key not configured");
    }
    String url =
        String.format(
            "https://%s-%s.svc.aped-4627-b74a.pinecone.io/vectors/upsert",
            pineconeIndex, pineconeEnvironment);
    var body = new java.util.HashMap<String, Object>();
    body.put("vectors", List.of(Map.of("id", id, "values", vector, "metadata", metadata)));
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(url)
            .addHeader("Api-Key", pineconeApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Pinecone upsert failed HTTP " + response.code());
      }
      return response.body() != null ? response.body().string() : "ok";
    }
  }

  public List<Map<String, Object>> search(List<Float> queryVector, int topK) throws IOException {
    if (pineconeApiKey == null || pineconeApiKey.isBlank()) {
      throw new IllegalStateException("Pinecone API key not configured");
    }
    String url =
        String.format(
            "https://%s-%s.svc.aped-4627-b74a.pinecone.io/query",
            pineconeIndex, pineconeEnvironment);
    var body = new java.util.HashMap<String, Object>();
    body.put("vector", queryVector);
    body.put("topK", topK);
    body.put("includeMetadata", true);
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(url)
            .addHeader("Api-Key", pineconeApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Pinecone search failed HTTP " + response.code());
      }
      String respBody = response.body() != null ? response.body().string() : "{}";
      var root = objectMapper.readTree(respBody);
      var matches = root.path("matches");
      List<Map<String, Object>> results = new java.util.ArrayList<>();
      for (var node : matches) {
        results.add(objectMapper.convertValue(node, Map.class));
      }
      return results;
    }
  }

  public boolean isConfigured() {
    return pineconeApiKey != null && !pineconeApiKey.isBlank();
  }
}
