package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for discovering AI models from internet registries, cloud deployments, and local systems.
 * Implements the "Zero-Hardcoding" requirement for AI Provider Hub.
 * (ইন্টারনেট রেজিস্ট্রি, ক্লাউড ডিপ্লয়মেন্ট এবং লোকাল সিস্টেম থেকে এআই মডেল খুঁজে বের করার সার্ভিস)
 */
@Service
public class AIProviderDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(AIProviderDiscoveryService.class);

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private WebClient webClient;

    @org.springframework.beans.factory.annotation.Value("${ai.providers.ollama.endpoint:http://localhost:11434}")
    private String ollamaEndpoint;

    /**
     * Searches for AI models across various registries.
     * Calls actual APIs (HuggingFace, OpenRouter, etc.)
     */
    public Flux<Map<String, Object>> discoverModels(String query) {
        logger.info("Discovering AI models for query: {}", query);
        
        String searchQuery = (query == null || query.isEmpty()) ? "gpt" : query;
        
        // 1. HuggingFace Discovery
        Mono<List<Map<String, Object>>> hfModels = webClient.get()
                .uri("https://huggingface.co/api/models?search={query}&filter=text-generation&sort=downloads&direction=-1&limit=20", searchQuery)
                .retrieve()
                .bodyToFlux(Map.class)
                .map(m -> {
                    String modelId = (String) m.get("modelId");
                    return Map.<String, Object>of(
                        "name", modelId,
                        "provider", "huggingface",
                        "type", "llm",
                        "description", "HF Model: " + modelId,
                        "accountRequired", true
                    );
                })
                .collectList()
                .onErrorReturn(new ArrayList<>());

        // 2. OpenRouter (Static-ish but can be fetched)
        Mono<List<Map<String, Object>>> orModels = webClient.get()
                .uri("https://openrouter.ai/api/v1/models")
                .retrieve()
                .bodyToMono(Map.class)
                .map(m -> {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) m.get("data");
                    return data.stream()
                            .filter(model -> query == null || query.isEmpty() || model.get("id").toString().contains(query))
                            .map(model -> Map.<String, Object>of(
                                "name", model.get("id"),
                                "provider", "openrouter",
                                "type", "llm",
                                "description", model.get("description") != null ? model.get("description") : "OpenRouter Model",
                                "accountRequired", true
                            ))
                            .limit(20)
                            .collect(Collectors.toList());
                })
                .onErrorReturn(new ArrayList<>());

        return Flux.zip(hfModels, orModels)
                .flatMapIterable(tuple -> {
                    List<Map<String, Object>> combined = new ArrayList<>();
                    combined.addAll(tuple.getT1());
                    combined.addAll(tuple.getT2());
                    return combined;
                });
    }

    /**
     * Validates an API key for a given provider by performing a lightweight "hello world" request.
     */
    public Mono<Boolean> validateKey(String providerName, String apiKey) {
        try {
            com.supremeai.provider.AIProvider provider = providerFactory.getProvider(providerName, apiKey);
            return provider.generate("hi")
                    .map(resp -> resp != null && !resp.isEmpty())
                    .onErrorReturn(false);
        } catch (Exception e) {
            logger.error("Validation failed for {}: {}", providerName, e.getMessage());
            return Mono.just(false);
        }
    }

    /**
     * Scans for local or cloud-deployed models (e.g. Ollama, Local endpoints).
     */
    public Flux<Map<String, Object>> scanDeployments() {
        // Ping localhost:11434 for Ollama
        return webClient.get()
                .uri(ollamaEndpoint + "/api/tags")
                .retrieve()
                .bodyToMono(Map.class)
                .flatMapMany(m -> {
                    List<Map<String, Object>> models = (List<Map<String, Object>>) m.get("models");
                    return Flux.fromIterable(models.stream()
                            .map(model -> Map.<String, Object>of(
                                "name", model.get("name"),
                                "provider", "ollama",
                                "type", "llm",
                                "baseUrl", ollamaEndpoint
                            ))
                            .collect(Collectors.toList()));
                })
                .onErrorResume(e -> Flux.empty());
    }
}

