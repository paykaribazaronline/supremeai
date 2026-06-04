package com.supremeai.provider;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Refactored HuggingFace Provider. Now dynamic and follows MetadataService patterns. */
@Component
public class HuggingFaceProvider extends AbstractHttpProvider {

  public HuggingFaceProvider() {
    this("");
  }

  public HuggingFaceProvider(String apiKey) {
    super(
        apiKey,
        "https://api-inference.huggingface.co/v1/chat/completions",
        "meta-llama/Llama-3.3-70B-Instruct");
  }

  @Override
  public String getName() {
    return "huggingface";
  }

  @Override
  protected Map<String, Object> createRequestBody(String prompt) {
    return Map.of(
        "model", getModel(),
        "messages", List.of(Map.of("role", "user", "content", prompt)),
        "max_tokens", 512);
  }

  @Override
  protected String extractResponse(String responseBody) throws Exception {
    return extractOpenAICompatibleResponse(responseBody, "HuggingFace");
  }

  @Override
  protected void addAuthHeaders(okhttp3.Request.Builder builder) {
    if (apiKey != null && !apiKey.isBlank()) {
      builder.addHeader("Authorization", "Bearer " + apiKey);
    }
  }
}
