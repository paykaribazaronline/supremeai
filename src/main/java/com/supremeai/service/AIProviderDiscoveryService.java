package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.google.cloud.run.v2.ServicesClient;
import com.google.cloud.run.v2.LocationName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 */
@Service
public class AIProviderDiscoveryService {
    public AIProviderDiscoveryService(String ollamaEndpoint, String gcpProjectId, String gcpRegion) {
        this.ollamaEndpoint = ollamaEndpoint;
        this.gcpProjectId = gcpProjectId;
        this.gcpRegion = gcpRegion;
    }

    public AIProviderDiscoveryService(AIProviderFactory providerFactory, WebClient webClient) {
        this.providerFactory = providerFactory;
        this.webClient = webClient;
    }


    private static final Logger logger = LoggerFactory.getLogger(AIProviderDiscoveryService.class);






    public Flux<Map<String, Object>> discoverModels(String query) {
        logger.info("Discovering AI models for query: {}", query);
        
        String searchQuery = (query == null || query.isEmpty()) ? "gpt" : query;
        
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

    public Mono<Boolean> validateKey(String providerName, String apiKey) {
        logger.info("[VALIDATION-DS] validateKey called: providerName={}, apiKeyLength={}", providerName, apiKey != null ? apiKey.length() : "null");
        try {
            com.supremeai.provider.AIProvider provider = providerFactory.getProvider(providerName, apiKey);
            return provider.generate("hi")
                    .map(resp -> {
                        logger.info("[VALIDATION-DS] generate response received for {}: length={}", providerName, resp != null ? resp.length() : "null");
                        return resp != null && !resp.isEmpty();
                    })
                    .doOnError(e -> logger.error("[VALIDATION-DS] generate() error for {}: {}", providerName, e.toString()))
                    .onErrorReturn(false);
        } catch (Exception e) {
            logger.error("[VALIDATION-DS] validateKey EXCEPTION for {}: {}", providerName, e.getClass().getSimpleName() + ": " + e.getMessage());
            return Mono.just(false);
        }
    }

    public Flux<Map<String, Object>> scanDeployments() {
        Flux<Map<String, Object>> ollamaModels = webClient.get()
                .uri(ollamaEndpoint + "/api/tags")
                .retrieve()
                .bodyToMono(Map.class)
                .flatMapMany(m -> {
                    List<Map<String, Object>> models = (List<Map<String, Object>>) m.get("models");
                    if (models == null) return Flux.empty();
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

        Flux<Map<String, Object>> cloudRunModels = Mono.fromCallable(() -> {
            List<Map<String, Object>> result = new ArrayList<>();
            try (ServicesClient client = ServicesClient.create()) {
                LocationName parent = LocationName.of(gcpProjectId, gcpRegion);
                for (com.google.cloud.run.v2.Service service : client.listServices(parent).iterateAll()) {
                    String name = service.getName();
                    String simpleName = name.contains("/") ? name.substring(name.lastIndexOf("/") + 1) : name;
                    String lower = simpleName.toLowerCase();

                    if (lower.contains("ai") || lower.contains("model") || lower.contains("llama") ||
                        lower.contains("mistral") || lower.contains("gpt") || lower.contains("embed") ||
                        lower.contains("vision")) {
                        result.add(Map.<String, Object>of(
                            "name", simpleName,
                            "provider", "google-cloud",
                            "type", "llm",
                            "baseUrl", service.getUri(),
                            "description", "Cloud Run Deployment: " + simpleName
                        ));
                    }
                }
            } catch (Exception e) {
                logger.warn("Cloud Run scan failed: {}", e.getMessage());
            }
            return result;
        }).flatMapMany(Flux::fromIterable);

        return Flux.merge(ollamaModels, cloudRunModels);
    }

    public com.supremeai.provider.AIProvider getBestProviderForTask(String task) {
        try {
            return providerFactory.getProvider("google-cloud", null);
        } catch (Exception e) {
            logger.error("Failed to get provider for task {}: {}", task, e.getMessage());
            return null;
        }
    }
}
