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
 * ClaudeMemoryService provides long-term context and memory for AI agents.
 * It interfaces with Claude-mem/Mem0 or a custom MCP server.
 */
@Service
public class ClaudeMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeMemoryService.class);
    private final WebClient webClient;

    @Value("${supremeai.claude-mem.api-key:}")
    private String apiKey;

    public ClaudeMemoryService(WebClient.Builder webClientBuilder, @Value("${supremeai.claude-mem.url:http://localhost:3000}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Mono<Void> storeMemory(String userId, String info) {
        log.info("[Memory] Storing new information for user: {}", userId);
        return webClient.post()
                .uri("/add")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(Map.of("user_id", userId, "text", info))
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<List<String>> searchMemory(String userId, String query) {
        log.debug("[Memory] Searching context for user: {} with query: {}", userId, query);
        return webClient.post()
                .uri("/search")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(Map.of("user_id", userId, "query", query))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                    return results.stream().map(r -> (String) r.get("memory")).toList();
                });
    }
}