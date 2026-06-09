package com.supremeai.service.logic;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FabricLogicEngineService {
  private static final Logger logger = LoggerFactory.getLogger(FabricLogicEngineService.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient httpClient;

  @Value("${fabric.cli.path:fabric}")
  private String fabricCliPath;

  @Value("${fabric.patterns.dir:./fabric_patterns}")
  private String patternsDir;

  public FabricLogicEngineService() {
    this.httpClient =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(60))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
  }

  public String applyPattern(String patternName, String input) throws IOException {
    var body = new java.util.HashMap<String, Object>();
    body.put("pattern", patternName);
    body.put("input", input);
    String bodyStr = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url("http://localhost:3000/api/fabric/apply")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        logger.warn("Fabric HTTP {} for pattern {}", response.code(), patternName);
        return String.format("[Fabric Pattern: %s]\nInput: %s", patternName, input);
      }
      return response.body() != null ? response.body().string() : input;
    }
  }

  public java.util.List<String> listPatterns() {
    return List.of(
        "code_review", "security_audit", "summarize", "explain", "refactor", "unit_test");
  }

  public java.util.Map<String, Object> listPatternsMetadata() {
    java.util.Map<String, Object> meta = new java.util.LinkedHashMap<>();
    for (String p : listPatterns()) {
      meta.put(p, java.util.Map.of("type", "logical", "status", "active"));
    }
    return meta;
  }

  public boolean isConfigured() {
    return patternsDir != null && !patternsDir.isBlank();
  }
}
