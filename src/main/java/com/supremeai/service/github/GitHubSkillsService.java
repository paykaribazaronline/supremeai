package com.supremeai.service.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GitHubSkillsService {
  private static final Logger logger = LoggerFactory.getLogger(GitHubSkillsService.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${github.token:}")
  private String githubToken;

  @Value("${github.api.url:https://api.github.com}")
  private String githubApiUrl;

  public GitHubSkillsService() {
    this.httpClient =
        new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(30))
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    this.objectMapper = new ObjectMapper();
  }

  public String createPullRequest(String owner, String repo, String title, String head, String base)
      throws IOException {
    if (githubToken == null || githubToken.isBlank()) {
      throw new IllegalStateException("GitHub token not configured");
    }
    var body = new java.util.HashMap<String, Object>();
    body.put("title", title);
    body.put("head", head);
    body.put("base", base);
    String bodyStr = objectMapper.writeValueAsString(body);
    Request request =
        new Request.Builder()
            .url(githubApiUrl + "/repos/" + owner + "/" + repo + "/pulls")
            .addHeader("Authorization", "Bearer " + githubToken)
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(bodyStr, JSON))
            .build();
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("GitHub HTTP " + response.code());
      }
      return response.body() != null ? response.body().string() : "{}";
    }
  }

  public boolean isConfigured() {
    return githubToken != null && !githubToken.isBlank();
  }
}
