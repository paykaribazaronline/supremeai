package com.supremeai.service;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@Service
public class SuperHubOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SuperHubOrchestrator.class);

    private final AIProviderFactory providerFactory;
    private final SupremeAIBrain supremeAIBrain;
public SuperHubOrchestrator(AIProviderFactory providerFactory, SupremeAIBrain supremeAIBrain) {
        this.providerFactory = providerFactory;
        this.supremeAIBrain = supremeAIBrain;
    }

    public Mono<String> orchestrate(String taskDescription, Map<String, Object> context) {
        log.info("[SUPER_HUB] Orchestrating task: {} (Context: {})", taskDescription, context != null ? context.size() : 0);

        return supremeAIBrain.identifyHub(taskDescription)
            .flatMap(hubId -> {
                log.info("[SUPER_HUB] Delegating task to hub: {}", hubId);
                try {
                    AIProvider provider = providerFactory.getProvider(hubId);
                    return provider.generate(taskDescription)
                        .onErrorResume(e -> {
                            log.warn("[SUPER_HUB] Hub {} failed: {}. Trying next available cloud provider.", hubId, e.getMessage());
                            return tryNextAvailableProvider(hubId, taskDescription);
                        });
                } catch (Exception e) {
                    log.warn("[SUPER_HUB] Could not create provider for hub {}: {}. Trying next available.", hubId, e.getMessage());
                    return tryNextAvailableProvider(hubId, taskDescription);
                }
            })
            .onErrorResume(e -> {
                log.error("[SUPER_HUB] Orchestration failed: {}", e.getMessage());
                return Mono.just("Orchestration Error: " + e.getMessage());
            });
    }

    private Mono<String> tryNextAvailableProvider(String excludeHubId, String taskDescription) {
        List<String> allProviders = providerFactory.getAvailableProviderIds();
        for (String providerName : allProviders) {
            if (providerName.equalsIgnoreCase(excludeHubId)) continue;
            try {
                AIProvider fallback = providerFactory.getProvider(providerName);
                log.info("[SUPER_HUB] Trying fallback cloud provider: {}", providerName);
                return fallback.generate(taskDescription)
                    .onErrorResume(e -> {
                        log.warn("[SUPER_HUB] Fallback provider {} also failed: {}", providerName, e.getMessage());
                        return Mono.empty();
                    });
            } catch (Exception e) {
                log.debug("[SUPER_HUB] Could not create fallback provider {}: {}", providerName, e.getMessage());
            }
        }
        return Mono.just("[SUPER_HUB] All cloud providers failed. Please check provider status in admin dashboard.");
    }

    public Mono<String> executeDevelopmentTask(String codeRequest) {
        return providerFactory.getBestProviderForTask("CODE_GENERATION")
                .flatMap(provider -> provider.generate(codeRequest)
                    .onErrorResume(e -> {
                        log.warn("[SUPER_HUB] Dev task failed on best provider: {}. Trying any available.", e.getMessage());
                        return tryNextAvailableProvider(null, codeRequest);
                    }))
                .onErrorResume(e -> {
                    log.warn("[SUPER_HUB] No best provider found for CODE_GENERATION: {}. Trying any available.", e.getMessage());
                    return tryNextAvailableProvider(null, codeRequest);
                });
    }

    public Mono<String> executeMarketingTask(String contentRequest) {
        return providerFactory.getBestProviderForTask("CREATIVE_WRITING")
                .flatMap(provider -> provider.generate(contentRequest)
                    .onErrorResume(e -> {
                        log.warn("[SUPER_HUB] Marketing task failed on best provider: {}. Trying any available.", e.getMessage());
                        return tryNextAvailableProvider(null, contentRequest);
                    }))
                .onErrorResume(e -> {
                    log.warn("[SUPER_HUB] No best provider found for CREATIVE_WRITING: {}. Trying any available.", e.getMessage());
                    return tryNextAvailableProvider(null, contentRequest);
                });
    }
}
