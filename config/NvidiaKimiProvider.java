package com.supremeai.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supremeai.security.UnifiedSecretsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NvidiaKimiProvider {

    private final WebClient.Builder webClientBuilder = WebClient.builder();
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

                    return webClientBuilder.build()
                            .post()
                            .uri(INVOKE_URL)
                            .header("Authorization", "Bearer " + apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(rootNode)
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(responseBody -> {
                                try {
                                    return parseResponse(responseBody);
                                } catch (IOException e) {
                                    throw new RuntimeException("Error parsing Kimi response", e);
                                }
                            })
                            .onErrorResume(e -> {
                                log.error("NVIDIA API error for Kimi: {}", e.getMessage());
                                return Mono.just("");
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
