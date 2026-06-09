package com.supremeai.provider;

import java.util.List;
import java.util.Map;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NvidiaNimProvider extends AbstractHttpProvider {
  private static final Logger log = LoggerFactory.getLogger(NvidiaNimProvider.class);
  private static final String API_URL = "https://integrate.api.nvidia.com/v1/chat/completions";

  public NvidiaNimProvider() {
    this("");
  }

  public NvidiaNimProvider(@Value("${nvidia.nim.api-key:}") String apiKey) {
    super(apiKey, API_URL, "meta/llama-3.1-70b-instruct");
  }

  @Override
  public String getName() {
    return "nvidia-nim";
  }

  @Override
  protected Map<String, Object> createRequestBody(String prompt) {
    return Map.of(
        "messages", List.of(Map.of("role", "user", "content", prompt)),
        "model", getModel(),
        "max_tokens", 2048);
  }

  @Override
  protected String extractResponse(String responseBody) throws Exception {
    return extractOpenAICompatibleResponse(responseBody, "NVIDIA NIM");
  }

  @Override
  protected void addAuthHeaders(okhttp3.Request.Builder builder) {
    if (apiKey != null && !apiKey.isBlank()) {
      builder.addHeader("Authorization", "Bearer " + apiKey);
    }
  }
}
