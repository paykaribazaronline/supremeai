package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;
import java.util.List;

/**
 * Mem0Service provides personalized long-term memory for AI agents.
 * It interfaces with the Mem0 API to store and retrieve user-specific preferences and context.
 */
@Service
public class Mem0Service {

    private static final Logger log = LoggerFactory.getLogger(Mem0Service.class);
    private final WebClient webClient;

    @Value("${supremeai.mem0.api-key:}")
    private String apiKey;

    public Mem0Service(WebClient.Builder webClientBuilder, @Value("${supremeai.mem0.url:http://localhost:8000}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * Stores a piece of personalized memory for a given user.
     * @param userId The ID of the user.
     * @param info The information to store.
     * @return A Mono<Void> indicating completion.
     */
    public Mono<Void> storeMemory(String userId, String info) {
        log.info("[Mem0] Storing personalized information for user: {}", userId);
        return webClient.post()
                .uri("/memories")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(Map.of("user_id", userId, "text", info))
                .retrieve()
                .bodyToMono(Void.class);
    }

    /**
     * Searches for personalized memories for a given user based on a query.
     * @param userId The ID of the user.
     * @param query The search query.
     * @return A Mono containing a list of relevant memory strings.
     */
    public Mono<List<String>> searchMemory(String userId, String query) {
        log.debug("[Mem0] Searching personalized context for user: {} with query: {}", userId, query);
        return webClient.post()
                .uri("/memories/search")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(Map.of("user_id", userId, "query", query))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                    return results.stream().map(r -> (String) r.get("text")).toList();
                });
    }
}
