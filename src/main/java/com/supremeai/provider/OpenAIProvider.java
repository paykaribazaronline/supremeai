package com.supremeai.provider;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 */
@Component
public class OpenAIProvider extends AbstractHttpProvider {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final List<String> SUPPORTED_MODELS = List.of(
        "gpt-4",
        "gpt-4-turbo-preview",
        "gpt-3.5-turbo"
    );

    public OpenAIProvider() {
        super("", API_URL, DEFAULT_MODEL);
    }

    public OpenAIProvider(String apiKey) {
        super(apiKey, API_URL, DEFAULT_MODEL);
    }

    public OpenAIProvider(String apiKey, String baseUrl, String model) {
        super(apiKey, baseUrl != null && !baseUrl.isEmpty() ? baseUrl : API_URL, model != null && !model.isEmpty() ? model : DEFAULT_MODEL);
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
        return Map.of(
            "name", "OpenAI",
            "models", SUPPORTED_MODELS,
            "type", "remote",
            "url", baseUrl
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        String cleanPrompt = prompt;
        String mimeType = null;
        String base64Data = null;
        String dataUrl = null;

        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(!\\[.*?\\]\\(data:(image\\/[a-zA-Z*\\-+.]+);base64,([^)]+)\\))"
            );
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
            if (model.startsWith("gpt-3.5")) {
                model = "gpt-4-turbo"; // upgrade to multimodal model
            }
            return Map.of(
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "text", "text", cleanPrompt),
                                    Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                            )
                    )),
                    "model", model
            );
        }

        return Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", getModel()
        );
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
