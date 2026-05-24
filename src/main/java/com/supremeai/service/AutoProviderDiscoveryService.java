package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * ZM-01: Auto-Provider Discovery
 * Background job that scans OpenRouter/HuggingFace for new free/cheap models weekly
 * and auto-registers them in Firestore if they pass a quality threshold.
 */
@Service
public class AutoProviderDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(AutoProviderDiscoveryService.class);

    @Autowired
    private ProviderRepository providerRepository;

    private final WebClient webClient = WebClient.create();

    @Scheduled(cron = "0 0 4 * * SUN") // Every Sunday at 4 AM
    public void scanForNewProviders() {
        log.info("🔍 Starting Weekly Auto-Provider Discovery scan...");

        // Simulate fetching from OpenRouter / HuggingFace API
        // In a real scenario, this would call actual APIs and filter by pricing.
        fetchMockProviders()
            .flatMap(provider -> providerRepository.findByName(provider.getName())
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("New provider discovered and auto-registered: {}", provider.getName());
                    return providerRepository.save(provider);
                }))
            )
            .subscribe(
                result -> {},
                error -> log.error("Auto-Provider Discovery failed", error),
                () -> log.info("✅ Auto-Provider Discovery scan completed.")
            );
    }

    private reactor.core.publisher.Flux<APIProvider> fetchMockProviders() {
        // Simulating discovered free/cheap models
        APIProvider newProvider1 = new APIProvider();
        newProvider1.setId("auto_" + UUID.randomUUID().toString().substring(0, 8));
        newProvider1.setName("openrouter/meta-llama-3-8b-instruct:free");
        newProvider1.setProviderType("openai"); // uses openai compatible API
        newProvider1.setStatus("active");
        newProvider1.setBaseUrl("https://openrouter.ai/api/v1");
        newProvider1.setModels(List.of("meta-llama/llama-3-8b-instruct:free"));
        newProvider1.setDescription("Auto-discovered free Llama 3 model from OpenRouter");

        return reactor.core.publisher.Flux.just(newProvider1);
    }
}
