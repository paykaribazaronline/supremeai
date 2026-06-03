package com.supremeai.provider;



import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.util.Map;

/**
 * Groq Provider implementation using shared HTTP client and ObjectMapper.
 * Extends AbstractHttpProvider for optimized performance.
 */
// @Component // Disabled: heavy cloud provider excluded from local-first runtime
public class GroqProvider extends AbstractHttpProvider {
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public GroqProvider() {
        this("");
    }

    public GroqProvider(@Value("${groq.api-key:}") String apiKey) {
        super(apiKey, API_URL, "mixtral-8x7b-32768");
    }

    public GroqProvider(String apiKey, String baseUrl, String model) {
        super(apiKey, baseUrl != null && !baseUrl.isEmpty() ? baseUrl : API_URL, model != null && !model.isEmpty() ? model : "mixtral-8x7b-32768");
    }

    @Override
    public String getName() {
        return "groq";
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "name", "Groq",
                "models", new String[]{"llama2-70b-4096", "mixtral-8x7b-32768", "gemma-7b-it"}
        );
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", getModel()
        );
    }

    @Override
    protected String extractResponse(String responseBody) throws Exception {
        return extractOpenAICompatibleResponse(responseBody, "Groq");
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        builder.addHeader("Authorization", "Bearer " + apiKey);
    }
}
