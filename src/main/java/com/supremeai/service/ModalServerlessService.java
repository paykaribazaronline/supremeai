package com.supremeai.service;

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
public class ModalServerlessService {
  private static final Logger logger = LoggerFactory.getLogger(ModalServerlessService.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${modal.api.url:https://api.modal.com/v1}")
  private String modalApiUrl;

  @Value("${modal.api.key:}")
  private String modalApiKey;

  public ModalServerlessService() {
    this.httpClient = new OkHttpClient.Builder().callTimeout(Duration.ofSeconds(120)).build();
    this.objectMapper = new ObjectMapper();
  }

  public String invoke(String functionName, Map<String, Object> payload) throws IOException {
    if (modalApiKey == null || modalApiKey.isBlank()) {
      throw new IllegalStateException("Modal API key not configured");
    }
    var body = new java.util.HashMap<String, Object>();
    body.put("function", functionName);
    body.put("input", payload);
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(modalApiUrl + "/functions/invoke")
            .addHeader("Authorization", "Bearer " + modalApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Modal HTTP " + response.code());
      }
      return response.body() != null ? response.body().string() : "{}";
    }
  }

  public boolean isConfigured() {
    return modalApiKey != null && !modalApiKey.isBlank();
  }
}
