package com.supremeai.service.scraper;

import com.fasterxml.jackson.databind.JsonNode;
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
public class FirecrawlScraperService {
  private static final Logger logger = LoggerFactory.getLogger(FirecrawlScraperService.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${firecrawl.api.key:}")
  private String firecrawlApiKey;

  @Value("${firecrawl.api.url:https://api.firecrawl.dev/v1}")
  private String firecrawlApiUrl;

  public FirecrawlScraperService() {
    this.httpClient =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    this.objectMapper = new ObjectMapper();
  }

  public ScrapeResult scrape(String url) throws IOException {
    return scrape(url, Map.of());
  }

  public ScrapeResult scrape(String url, Map<String, Object> options) throws IOException {
    if (firecrawlApiKey == null || firecrawlApiKey.isBlank()) {
      logger.warn("FIRECRAWL_API_KEY not configured; returning empty result for {}", url);
      return new ScrapeResult(url, "", Map.of(), false);
    }

    var bodyMap = new java.util.HashMap<>(options);
    bodyMap.put("url", url);
    bodyMap.putIfAbsent("formats", List.of("markdown"));
    bodyMap.putIfAbsent("onlyMainContent", true);
    bodyMap.putIfAbsent("removeBase64Images", true);
    bodyMap.putIfAbsent("blockAds", true);
    String bodyStr = objectMapper.writeValueAsString(bodyMap);

    Request request =
        new Request.Builder()
            .url(firecrawlApiUrl + "/scrape")
            .addHeader("Authorization", "Bearer " + firecrawlApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        logger.error("Firecrawl scrape failed for {}: HTTP {}", url, response.code());
        throw new IOException("Firecrawl HTTP " + response.code() + ": " + response.message());
      }
      String respBody = response.body() != null ? response.body().string() : "{}";
      JsonNode root = objectMapper.readTree(respBody);
      JsonNode data = root.path("data");
      String content = data.path("markdown").asText("");
      JsonNode metaNode = data.path("metadata");
      Map<String, Object> metadata = objectMapper.convertValue(metaNode, Map.class);
      boolean success = root.path("success").asBoolean(false);
      return new ScrapeResult(url, content, metadata, success);
    }
  }

  public Map<String, Object> crawl(String url, Map<String, Object> options) throws IOException {
    if (firecrawlApiKey == null || firecrawlApiKey.isBlank()) {
      throw new IllegalStateException("FIRECRAWL_API_KEY not configured");
    }

    var bodyMap = new java.util.HashMap<>(options);
    bodyMap.put("url", url);
    bodyMap.putIfAbsent("limit", 1);
    bodyMap.putIfAbsent("maxDepth", 1);
    bodyMap.putIfAbsent("allowExternalLinks", false);
    String bodyStr = objectMapper.writeValueAsString(bodyMap);

    Request request =
        new Request.Builder()
            .url(firecrawlApiUrl + "/crawl")
            .addHeader("Authorization", "Bearer " + firecrawlApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();

    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Firecrawl crawl HTTP " + response.code());
      }
      String respBody = response.body() != null ? response.body().string() : "{}";
      return objectMapper.readValue(respBody, Map.class);
    }
  }

  public boolean isConfigured() {
    return firecrawlApiKey != null && !firecrawlApiKey.isBlank();
  }

  public record ScrapeResult(
      String url, String content, Map<String, Object> metadata, boolean success) {}
}
