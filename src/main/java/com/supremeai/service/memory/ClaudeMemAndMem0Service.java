package com.supremeai.service.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ClaudeMemAndMem0Service {
  private static final Logger logger = LoggerFactory.getLogger(ClaudeMemAndMem0Service.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${claude.mem.base_url:https://claude-mem.example.com}")
  private String claudeMemBaseUrl;

  @Value("${claude.mem.api.key:}")
  private String claudeMemApiKey;

  @Value("${mem0.api.key:}")
  private String mem0ApiKey;

  @Value("${mem0.api.url:https://api.mem0.ai/v1}")
  private String mem0ApiUrl;

  public ClaudeMemAndMem0Service() {
    this.httpClient =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    this.objectMapper = new ObjectMapper();
  }

  public String storeClaudeMemory(String sessionId, String content) throws IOException {
    if (claudeMemApiKey == null || claudeMemApiKey.isBlank()) {
      logger.warn(
          "CLAUDE_MEM_API_KEY not configured; skipping memory store for session {}", sessionId);
      return content;
    }
    var body = new java.util.HashMap<String, Object>();
    body.put("session_id", sessionId);
    body.put("content", content);
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(claudeMemBaseUrl + "/memory/store")
            .addHeader("Authorization", "Bearer " + claudeMemApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        logger.error("Claude-mem store failed: HTTP {}", response.code());
        return content;
      }
      return content;
    }
  }

  public List<Map<String, Object>> searchClaudeMemory(String sessionId, String query, int limit)
      throws IOException {
    if (claudeMemApiKey == null || claudeMemApiKey.isBlank()) {
      logger.warn("CLAUDE_MEM_API_KEY not configured; returning empty memories.");
      return List.of();
    }
    var body = new java.util.HashMap<String, Object>();
    body.put("session_id", sessionId);
    body.put("query", query);
    body.put("limit", limit);
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(claudeMemBaseUrl + "/memory/search")
            .addHeader("Authorization", "Bearer " + claudeMemApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        logger.error("Claude-mem search failed: HTTP {}", response.code());
        return List.of();
      }
      String respBody = response.body() != null ? response.body().string() : "[]";
      JsonNode root = objectMapper.readTree(respBody);
      if (root.isArray()) {
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (JsonNode node : root) {
          results.add(objectMapper.convertValue(node, Map.class));
        }
        return results;
      }
      JsonNode resultsNode = root.path("results");
      return objectMapper.convertValue(resultsNode, List.class);
    }
  }

  public String storeMem0(String userId, String content) throws IOException {
    if (mem0ApiKey == null || mem0ApiKey.isBlank()) {
      logger.warn("MEM0_API_KEY not configured; skipping Mem0 store for user {}", userId);
      return content;
    }
    var body = new java.util.HashMap<String, Object>();
    body.put("user_id", userId);
    body.put("messages", List.of(Map.of("role", "user", "content", content)));
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(mem0ApiUrl + "/memories")
            .addHeader("Authorization", "Bearer " + mem0ApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        logger.error("Mem0 store failed: HTTP {}", response.code());
        return content;
      }
      return content;
    }
  }

  public List<Map<String, Object>> searchMem0(String userId, String query, int limit)
      throws IOException {
    if (mem0ApiKey == null || mem0ApiKey.isBlank()) {
      logger.warn("MEM0_API_KEY not configured; returning empty memories.");
      return List.of();
    }
    String url =
        mem0ApiUrl
            + "/memories/search?user_id="
            + URLEncoder.encode(userId, StandardCharsets.UTF_8)
            + "&query="
            + URLEncoder.encode(query, StandardCharsets.UTF_8)
            + "&limit="
            + limit;
    Request request =
        new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + mem0ApiKey)
            .get()
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        logger.error("Mem0 search failed: HTTP {}", response.code());
        return List.of();
      }
      String respBody = response.body() != null ? response.body().string() : "[]";
      JsonNode root = objectMapper.readTree(respBody);
      if (root.isArray()) {
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (JsonNode node : root) {
          results.add(objectMapper.convertValue(node, Map.class));
        }
        return results;
      }
      return List.of();
    }
  }

  public boolean isClaudeMemConfigured() {
    return claudeMemApiKey != null && !claudeMemApiKey.isBlank();
  }

  public boolean isMem0Configured() {
    return mem0ApiKey != null && !mem0ApiKey.isBlank();
  }
}
