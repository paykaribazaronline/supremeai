package com.supremeai.provider;

import com.supremeai.service.ProviderModelRegistry;
import com.supremeai.service.ProviderTierService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OpenAI Provider implementation using shared HTTP client and ObjectMapper. Extends
 * AbstractHttpProvider for optimized performance.
 */
// @Component // Disabled: heavy cloud provider excluded from local-first runtime
public class OpenAIProvider extends AbstractHttpProvider {

  @Autowired(required = false)
  private ProviderModelRegistry providerModelRegistry;

  @Autowired(required = false)
  private ProviderTierService providerTierService;

  private static final String API_URL = "https://api.openai.com/v1/chat/completions";
  private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
  private static final List<String> SUPPORTED_MODELS =
      List.of("gpt-4", "gpt-4-turbo-preview", "gpt-3.5-turbo");

  public OpenAIProvider() {
    super("", API_URL, DEFAULT_MODEL);
  }

  public OpenAIProvider(String apiKey) {
    super(apiKey, API_URL, DEFAULT_MODEL);
  }

  public OpenAIProvider(String apiKey, String baseUrl, String model) {
    super(
        apiKey,
        baseUrl != null && !baseUrl.isEmpty() ? baseUrl : API_URL,
        model != null && !model.isEmpty() ? model : DEFAULT_MODEL);
  }

  @Override
  public String getName() {
    return "openai";
  }

  @Override
  public Map<String, Object> getCapabilities() {
    if (providerMetadataService != null) {
      return super.getCapabilities();
    }
    List<String> models =
        providerModelRegistry != null
            ? providerModelRegistry.getSupportedModels("openai")
            : SUPPORTED_MODELS;
    return Map.of("name", "OpenAI", "models", models, "type", "remote", "url", baseUrl);
  }

  @Override
  protected Map<String, Object> createRequestBody(String prompt) {
    String cleanPrompt = prompt;
    String mimeType = null;
    String base64Data = null;
    String dataUrl = null;

    try {
      java.util.regex.Pattern pattern =
          java.util.regex.Pattern.compile(
              "(!\\[.*?\\]\\(data:(image\\/[a-zA-Z*\\-+.]+);base64,([^)]+)\\))");
      java.util.regex.Matcher matcher = pattern.matcher(prompt);
      if (matcher.find()) {
        String fullMatch = matcher.group(1);
        dataUrl = "data:" + matcher.group(2) + ";base64," + matcher.group(3);
        mimeType = matcher.group(2);
        base64Data = matcher.group(3);
        cleanPrompt = prompt.replace(fullMatch, "[Attached Image]");
      }
    } catch (Exception e) {
      logger.error("Failed to parse image from prompt in OpenAIProvider", e);
    }

    if (dataUrl != null) {
      // Force multimodal model if image is present
      String model = getModel();
      String tier =
          providerTierService != null ? providerTierService.getTierForModel(model) : "basic";
      if (!"premium".equals(tier)) {
        // Dynamic model upgrade: search for a premium model in our registry
        List<String> registeredModels =
            providerModelRegistry != null
                ? providerModelRegistry.getSupportedModels("openai")
                : SUPPORTED_MODELS;
        model =
            registeredModels.stream()
                .filter(
                    m -> {
                      String t =
                          providerTierService != null
                              ? providerTierService.getTierForModel(m)
                              : "basic";
                      return "premium".equals(t);
                    })
                .findFirst()
                .orElse("gpt-4-turbo"); // Default fallback multimodal model
      }
      return Map.of(
          "messages",
          List.of(
              Map.of(
                  "role",
                  "user",
                  "content",
                  List.of(
                      Map.of("type", "text", "text", cleanPrompt),
                      Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))))),
          "model",
          model);
    }

    return Map.of(
        "messages", List.of(Map.of("role", "user", "content", prompt)),
        "model", getModel());
  }

  @Override
  protected String extractResponse(String responseBody) throws Exception {
    return extractOpenAICompatibleResponse(responseBody, "OpenAI");
  }

  @Override
  protected void addAuthHeaders(okhttp3.Request.Builder builder) {
    builder.addHeader("Authorization", "Bearer " + apiKey);
  }
}
