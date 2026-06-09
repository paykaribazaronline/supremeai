package com.supremeai.service.avatar;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenHumanService {
  private static final Logger logger = LoggerFactory.getLogger(OpenHumanService.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${openhuman.api.url:}")
  private String openHumanUrl;

  @Value("${openhuman.api.key:}")
  private String openHumanApiKey;

  public OpenHumanService() {
    this.httpClient =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    this.objectMapper = new ObjectMapper();
  }

  public String generateAvatar(Map<String, Object> input) throws IOException {
    if (openHumanApiKey == null || openHumanApiKey.isBlank()) {
      throw new IllegalStateException("OpenHuman API key not configured");
    }
    var body = new java.util.HashMap<>(input);
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(openHumanUrl + "/v1/avatar/generate")
            .addHeader("Authorization", "Bearer " + openHumanApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("OpenHuman HTTP " + response.code());
      }
      return response.body() != null ? response.body().string() : "{}";
    }
  }

  public boolean isConfigured() {
    return openHumanApiKey != null
        && !openHumanApiKey.isBlank()
        && openHumanUrl != null
        && !openHumanUrl.isBlank();
  }
}
