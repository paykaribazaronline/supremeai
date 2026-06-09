package com.supremeai.service.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UpstashRedisService {
  private static final Logger logger = LoggerFactory.getLogger(UpstashRedisService.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${upstash.api.url:}")
  private String upstashUrl;

  @Value("${upstash.api.key:}")
  private String upstashApiKey;

  public UpstashRedisService() {
    this.httpClient =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(15))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    this.objectMapper = new ObjectMapper();
  }

  public String get(String key) throws IOException {
    return request("GET", key, null);
  }

  public String set(String key, String value) throws IOException {
    return request("SET", key, value);
  }

  public String delete(String key) throws IOException {
    return request("DEL", key, null);
  }

  private String request(String command, String key, String value) throws IOException {
    if (upstashApiKey == null || upstashApiKey.isBlank() || upstashUrl == null) {
      throw new IllegalStateException("Upstash not configured");
    }
    var body = new java.util.HashMap<String, Object>();
    body.put("command", command);
    body.put("key", key);
    if (value != null) body.put("value", value);
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(upstashUrl + "/v1/execute")
            .addHeader("Authorization", "Bearer " + upstashApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Upstash HTTP " + response.code());
      }
      return response.body() != null ? response.body().string() : "ok";
    }
  }

  public boolean isConfigured() {
    return upstashApiKey != null
        && !upstashApiKey.isBlank()
        && upstashUrl != null
        && !upstashUrl.isBlank();
  }
}
