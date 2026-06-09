package com.supremeai.service.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LongCatAIService {
  private static final Logger logger = LoggerFactory.getLogger(LongCatAIService.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${longcatai.api.url:}")
  private String longcataiApiUrl;

  @Value("${longcatai.api.key:}")
  private String longcataiApiKey;

  public LongCatAIService() {
    this.httpClient =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(60))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    this.objectMapper = new ObjectMapper();
  }

  public String queryLongContext(String prompt) throws IOException {
    if (longcataiApiKey == null || longcataiApiKey.isBlank()) {
      throw new IllegalStateException("LongCatAI API key not configured");
    }
    var body = new java.util.HashMap<String, Object>();
    body.put("prompt", prompt);
    body.put("max_tokens", 4096);
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(longcataiApiUrl + "/v1/completions")
            .addHeader("Authorization", "Bearer " + longcataiApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("LongCatAI HTTP " + response.code());
      }
      String respBody = response.body() != null ? response.body().string() : "{}";
      return respBody;
    }
  }

  public boolean isConfigured() {
    return longcataiApiKey != null && !longcataiApiKey.isBlank();
  }
}
