package com.supremeai.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.security.UnifiedSecretsService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class OpenAIClientImpl implements OpenAIClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final UnifiedSecretsService secretsService;

    public OpenAIClientImpl(
            UnifiedSecretsService secretsService,
            @Value("${openai.model:gpt-3.5-turbo}") String model) {
        this.secretsService = secretsService;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    private String getApiKey() {
        String key = secretsService.getSecret("OPENAI_API_KEY").block();
        return key != null ? key : "";
    }

    @Override
    public String generate(String prompt) {
        try {
            String jsonBody = String.format(
                    "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                    model,
                    prompt.replace("\"", "\\\""));

            RequestBody body = RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonNode root = objectMapper.readTree(response.body().string());
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        return choices.get(0)
                                .path("message")
                                .path("content")
                                .asText();
                    }
                }
                return "Failed to generate: " + response.message();
            }
        } catch (IOException e) {
            return "Error generating: " + e.getMessage();
        }
    }
}
