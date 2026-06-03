package com.supremeai.provider;

import com.supremeai.security.SecretManagerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * Gemini Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 */
@Component
public class GeminiProvider extends AbstractHttpProvider {
    public GeminiProvider(SecretManagerService secretManagerService) {
        this.secretManagerService = secretManagerService;
    }


    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";


    public GeminiProvider() {
        super("", API_URL, "gemini-1.5-pro");
    }

    public GeminiProvider(String apiKey) {
        super(apiKey, API_URL, "gemini-1.5-pro");
    }

    @Override
    protected String getRequestUrl() {
        String key = apiKey;
        if (key == null || key.isBlank()) {
            key = secretManagerService.getSecret("GEMINI_API_KEY");
        }
        return super.getRequestUrl() + "?key=" + key;
    }

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "Google Gemini",
                "models", new String[]{"gemini-1.5-flash", "gemini-1.5-pro"}
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        String cleanPrompt = prompt;
        String mimeType = null;
        String base64Data = null;

        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(!\\[.*?\\]\\(data:(image\\/[a-zA-Z*\\-+.]+);base64,([^)]+)\\))"
            );
            java.util.regex.Matcher matcher = pattern.matcher(prompt);
            if (matcher.find()) {
                String fullMatch = matcher.group(1);
                mimeType = matcher.group(2);
                base64Data = matcher.group(3);
                cleanPrompt = prompt.replace(fullMatch, "[Attached Image]");
            }
        } catch (Exception e) {
            log.error("Failed to parse image from prompt in GeminiProvider", e);
        throw new RuntimeException("Swallowed exception: " + e.getMessage(), e);
    }

        if (base64Data != null && mimeType != null) {
            return Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", cleanPrompt),
                                    Map.of("inlineData", Map.of(
                                            "mimeType", mimeType,
                                            "data", base64Data
                                    ))
                            ))
                    )
            );
        }

        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );
    }

    @Override
    protected String extractResponse(String responseBody) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            return "No response from Gemini.";
        }
        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        }
        return "No response from Gemini.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        // Gemini uses query parameter for API key
    }
}
