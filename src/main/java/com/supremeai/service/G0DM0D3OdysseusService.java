package com.supremeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** G0DM0D3 & Odysseus Security Logic service for boundary/probe + hardware vulnerability checks */
@Service
public class G0DM0D3OdysseusService {
  private static final Logger logger = LoggerFactory.getLogger(G0DM0D3OdysseusService.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${g0dm0d3.api.url:}")
  private String g0dm0d3ApiUrl;

  @Value("${g0dm0d3.api.key:}")
  private String g0dm0d3ApiKey;

  @Value("${odysseus.api.url:}")
  private String odysseusApiUrl;

  @Value("${odysseus.api.key:}")
  private String odysseusApiKey;

  public G0DM0D3OdysseusService() {
    this.httpClient =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    this.objectMapper = new ObjectMapper();
  }

  public String g0dm0d3BoundaryTest(String prompt) throws IOException {
    if (g0dm0d3ApiKey == null || g0dm0d3ApiKey.isBlank()) {
      throw new IllegalStateException("G0DM0D3 API key not configured");
    }
    var body = new java.util.HashMap<String, Object>();
    body.put("prompt", prompt);
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(g0dm0d3ApiUrl + "/v1/jailbreak/test")
            .addHeader("Authorization", "Bearer " + g0dm0d3ApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) throw new IOException("G0DM0D3 HTTP " + response.code());
      return response.body() != null ? response.body().string() : "{}";
    }
  }

  public String odysseusHardwareScan() throws IOException {
    if (odysseusApiKey == null || odysseusApiKey.isBlank()) {
      throw new IllegalStateException("Odysseus API key not configured");
    }
    Request request =
        new Request.Builder()
            .url(odysseusApiUrl + "/v1/scan/hardware")
            .addHeader("Authorization", "Bearer " + odysseusApiKey)
            .get()
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) throw new IOException("Odysseus HTTP " + response.code());
      return response.body() != null ? response.body().string() : "{}";
    }
  }

  public boolean isG0DM0D3Configured() {
    return g0dm0d3ApiKey != null && !g0dm0d3ApiKey.isBlank();
  }

  public boolean isOdysseusConfigured() {
    return odysseusApiKey != null && !odysseusApiKey.isBlank();
  }
}
