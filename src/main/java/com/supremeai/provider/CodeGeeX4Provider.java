package com.supremeai.provider;



import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.util.Map;

/**
 * CodeGeeX4 Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 * Cloud API from BigModel.cn (智谱AI) - Code-specialized 9B model with 128K context
 * Free tier available: https://bigmodel.cn
 * Uses OpenAI-compatible chat completions format
 */
@Component
public class CodeGeeX4Provider extends AbstractHttpProvider {
    private static final String API_URL = "https://open.bigmodel.cn/api/coding/paas/v4/chat/completions";
    private final String defaultModel;

    /**
     * Constructor with default model (codegeex-4)
     * Use this for cloud API access
     */
    public CodeGeeX4Provider() {
        this("", "codegeex-4");
    }

    public CodeGeeX4Provider(@Value("${codex4.api-key:}") String apiKey) {
        this(apiKey, "codegeex-4");
    }

    /**
     * Constructor with custom model
     *
     * @param apiKey  CodeGeeX4 API key from BigModel.cn
     * @param model   Model name: codegeex-4, codegeex-4-lite
     */
    public CodeGeeX4Provider(String apiKey, String model) {
        super(apiKey, API_URL, model);
        this.defaultModel = model;
    }

    @Override
    public String getName() {
        return "codegeex4";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "CodeGeeX4 (智谱AI)",
                "provider", "CodeGeeX4",
                "models", List.of("codegeex-4", "codegeex-4-lite"),
                "freeTier", "¥50-100 credit for new accounts (~$7-14)",
                "pricing", "¥0.002-0.01/1K tokens (~$0.0003-0.0014/1K)",
                "context", "128K tokens",
                "supports", List.of(
                    "code_generation", "code_completion", "code_infilling",
                    "function_calling", "repository_qa", "code_explanation",
                    "code_translation", "unit_test_generation", "code_review"
                ),
                "languages", "50+ programming languages",
                "baseUrl", API_URL
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "model", defaultModel,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "You are CodeGeeX, an intelligent programming assistant. You provide accurate, executable code with explanations when needed."
                        ),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7,
                "top_p", 0.7,
                "max_tokens", 4000,
                "stream", false
        );
    }

    @Override
    protected String extractResponse(String responseBody) throws Exception {
        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        
        // Check for error in response
        if (responseMap.containsKey("error")) {
            throw new RuntimeException("CodeGeeX4 Error: " + responseMap.get("error"));
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message.get("content");
            return content != null ? content.toString() : "No content in response";
        }
        
        return "No response from CodeGeeX4.";
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        builder.addHeader("Authorization", "Bearer " + apiKey);
    }
}
