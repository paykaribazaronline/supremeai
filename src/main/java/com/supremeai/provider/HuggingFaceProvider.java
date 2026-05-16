package com.supremeai.provider;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * Refactored HuggingFace Provider.
 * Now dynamic and follows MetadataService patterns.
 */
@Component
public class HuggingFaceProvider extends AbstractHttpProvider {

    public HuggingFaceProvider() {
        this("");
    }

    public HuggingFaceProvider(String apiKey) {
        super(apiKey, "https://api-inference.huggingface.co/v1/chat/completions", "meta-llama/Llama-3.3-70B-Instruct");
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
                "max_tokens", 512
        );
    }

    // extractResponse: AbstractHttpProvider-এর OpenAI-compatible default ব্যবহৃত হচ্ছে

    @Override
    protected void addAuthHeaders(okhttp3.Request.Builder builder) {
        if (apiKey != null && !apiKey.isBlank()) {
            builder.addHeader("Authorization", "Bearer " + apiKey);
        }
    }
}
