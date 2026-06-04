package com.supremeai.provider;

import java.util.List;
import java.util.Map;

/**
 * Standard provider implementation for any OpenAI-compatible API. This class replaces
 * model-specific providers like OpenAI, Groq, etc.
 */
public class StandardChatProvider extends AbstractHttpProvider {
  private final String providerName;

  public StandardChatProvider(
      String apiKey, String endpoint, String modelName, String providerName) {
    super(apiKey, endpoint, modelName != null ? modelName : "default");
    this.providerName = providerName;
  }

  @Override
  public String getName() {
    return providerName;
  }

  @Override
  protected Map<String, Object> createRequestBody(String prompt) {
    return Map.of(
        "messages", List.of(Map.of("role", "user", "content", prompt)),
        "model", getModel(),
        "stream", false);
  }

  @Override
  protected String extractResponse(String responseBody) throws Exception {
    return extractOpenAICompatibleResponse(responseBody, getName());
  }

  @Override
  protected void addAuthHeaders(okhttp3.Request.Builder builder) {
    if (apiKey != null && !apiKey.isEmpty()) {
      builder.addHeader("Authorization", "Bearer " + apiKey);
    }
  }
}
