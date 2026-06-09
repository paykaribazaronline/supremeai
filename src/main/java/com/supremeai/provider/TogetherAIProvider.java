package com.supremeai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TogetherAIProvider extends AbstractHttpProvider {
    private static final Logger log = LoggerFactory.getLogger(TogetherAIProvider.class);
    private static final String API_URL = "https://api.together.xyz/v1/chat/completions";

    public TogetherAIProvider() {
        this("");
    }

    public TogetherAIProvider(@Value("${together.ai.api-key:}") String apiKey) {
        super(apiKey, API_URL, "meta-llama/Llama-3.3-70B-Instruct-Turbo");
    }

    @Override
    public String getName() {
        return "together.ai";
    }

    @Override
    protected Map<String, Object> createRequestBody(String prompt) {
        return Map.of(
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "model", getModel(),
                "max_tokens", 1024);
    }

    @Override
    protected String extractResponse(String responseBody) throws Exception {
        return extractOpenAICompatibleResponse(responseBody, "Together.ai");
    }

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        if (apiKey != null && !apiKey.isBlank()) {
            builder.addHeader("Authorization", "Bearer " + apiKey);
        }
    }
}
