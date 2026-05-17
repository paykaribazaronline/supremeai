package com.supremeai.service;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * SuperHubOrchestrator - Phase 1 Implementation
 * 
 * Routes complex multi-step tasks to specialized MoE Super-Hubs.
 * Implements the Hub-Based Architecture from supremeai_ecosystem_plan.md.
 */
@Service
public class SuperHubOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SuperHubOrchestrator.class);

    private final AIProviderFactory providerFactory;
    private final SupremeAIBrain supremeAIBrain;

    @Autowired
    public SuperHubOrchestrator(AIProviderFactory providerFactory, SupremeAIBrain supremeAIBrain) {
        this.providerFactory = providerFactory;
        this.supremeAIBrain = supremeAIBrain;
    }

    /**
     * Orchestrates a task by delegating it to the most appropriate Super-Hub.
     * Includes Phase 3 optimization: Hub failover and fallback.
     */
    public Mono<String> orchestrate(String taskDescription, Map<String, Object> context) {
        log.info("[SUPER_HUB] Orchestrating task: {} (Context: {})", taskDescription, context != null ? context.size() : 0);

        // Step 1: Identify the hub using SupremeAIBrain (which uses core_knowledge.json)
        return supremeAIBrain.identifyHub(taskDescription)
            .flatMap(hubId -> {
                log.info("[SUPER_HUB] Delegating task to primary hub: {}", hubId);
                
                // Step 2: Get the specialized provider for this hub
                AIProvider provider = providerFactory.getProvider(hubId);
                
                // Step 3: Execute the task via the hub with failover
                return provider.generate(taskDescription)
                    .onErrorResume(e -> {
                        log.warn("[SUPER_HUB] Primary hub {} failed: {}. Trying fallback.", hubId, e.getMessage());
                        return providerFactory.getDefaultProvider().generate(taskDescription);
                    });
            })
            .onErrorResume(e -> {
                log.error("[SUPER_HUB] Orchestration failed: {}", e.getMessage());
                return Mono.just("Orchestration Error: " + e.getMessage());
            });
    }

    /**
     * Specialized routing for Logic Core (Development & Data Intelligence)
     */
    public Mono<String> executeDevelopmentTask(String codeRequest) {
        AIProvider devHub = providerFactory.getProvider("dev_hub");
        return devHub.generate(codeRequest);
    }

    /**
     * Specialized routing for Linguistic Core (Language & Marketing)
     */
    public Mono<String> executeMarketingTask(String contentRequest) {
        AIProvider langHub = providerFactory.getProvider("lang_hub");
        return langHub.generate(contentRequest);
    }
}
