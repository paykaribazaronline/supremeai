package com.supremeai.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supremeai.security.UnifiedSecretsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NvidiaKimiProvider {

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper;

    private final UnifiedSecretsService secretsService;

    private static final String INVOKE_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String MODEL_NAME = "moonshotai/kimi-k2.6";

    public Mono<String> askQuestion(String question) {
        return secretsService
                .getSecret("nvidia.api.key")
                .switchIfEmpty(Mono.defer(() -> Mono.error(new IllegalStateException("Missing NVIDIA API key in Firebase/system config"))))
                .flatMap(apiKey -> {
                    ObjectNode rootNode = objectMapper.createObjectNode();
                    rootNode.put("model", MODEL_NAME);
                    rootNode.put("max_tokens", 16384);
                    rootNode.put("temperature", 1.0);
                    rootNode.put("top_p", 1.0);
                    rootNode.put("stream", false);

                    ArrayNode messages = rootNode.putArray("messages");
                    ObjectNode userMessage = messages.addObject();
                    userMessage.put("role", "user");
                    userMessage.put("content", question);

                    String jsonPayload = objectMapper.writeValueAsString(rootNode);

                    Request request = new Request.Builder()
                            .url(INVOKE_URL)
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .addHeader("Accept", "application/json")
                            .post(RequestBody.create(jsonPayload, MediaType.get("application/json")))
                            .build();

                    return Mono.fromCallable(() -> {
                        try (Response response = okHttpClient.newCall(request).execute()) {
                            if (!response.isSuccessful()) {
                                log.error("NVIDIA API error: {} - {}", response.code(), response.message());
                                throw new IOException("Unexpected code " + response);
                            }

                            if (response.body() == null) {
                                return "";
                            }

                            String responseBody = response.body().string();
                            return parseResponse(responseBody);
                        }
                    });
                });
    }

    private String parseResponse(String json) throws IOException {
        var tree = objectMapper.readTree(json);
        return tree.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();
    }
}
