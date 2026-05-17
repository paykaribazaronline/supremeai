package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Map;

/**
 * Service to synchronize providers from Realtime Database to Firestore at startup.
 * Ensures that the admin dashboard (which uses Firestore) reflects the models
 * configured in the Firebase Realtime Database.
 */
@Service
public class ProviderInitializationService {

    private static final Logger log = LoggerFactory.getLogger(ProviderInitializationService.class);

    @Autowired
    private FirebaseRealtimeService realtimeService;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ProviderTypeRegistry providerTypeRegistry;

    @Value("${ai.providers.ollama.endpoint:http://localhost:11434}")
    private String ollamaEndpoint;

    private String determineType(String name) {
        if (name == null) return "GENERIC";
        String n = name.toUpperCase();
        if (n.contains("GEMINI")) return "GOOGLE";
        if (n.contains("OPENAI") || n.contains("GPT")) return "OPENAI";
        if (n.contains("ANTHROPIC") || n.contains("CLAUDE")) return "ANTHROPIC";
        if (n.contains("GROQ")) return "GROQ";
        if (n.contains("DEEPSEEK")) return "DEEPSEEK";
        if (n.contains("MISTRAL")) return "MISTRAL";
        if (n.contains("KIMI")) return "KIMI";
        if (n.contains("STEPFUN") || n.contains("STEP")) return "STEPFUN";
        if (n.contains("CODEGEEX")) return "CODEGEEX";
        if (n.contains("HUGGINGFACE")) return "HUGGINGFACE";
        if (n.contains("OLLAMA")) return "LOCAL";
        return "GENERIC";
    }

    /**
     * Enriches a provider with default metadata if not already present.
     */
    private void enrichProviderMetadata(APIProvider provider) {
        if (provider.getName() == null) return;
        if (provider.getBaseUrl() != null && !provider.getBaseUrl().isBlank()) return;

        String type = provider.getType();
        if (type != null && !type.isBlank()) {
            com.supremeai.model.ProviderTypeConfig typeConfig = providerTypeRegistry.getTypeConfig(type);
            if (typeConfig != null) {
                if (provider.getBaseUrl() == null || provider.getBaseUrl().isBlank()) {
                    provider.setBaseUrl(typeConfig.getDefaultBaseUrl());
                }
                if ((provider.getModels() == null || provider.getModels().isEmpty()) && typeConfig.getSupportedModels() != null) {
                    provider.setModels(typeConfig.getSupportedModels());
                }
                if (provider.getCapabilities() == null || provider.getCapabilities().isEmpty()) {
                    provider.setCapabilities(typeConfig.getCapabilities());
                }
                return;
            }
        }

        log.warn("No type config found for provider '{}' — admin must configure manually in dashboard", provider.getName());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncProvidersOnStartup() {
        log.info("[STARTUP] Initializing AI provider synchronization in background...");
        
        realtimeService.getData("config/api_keys")
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
            .flatMapMany(keysMap -> {
                if (keysMap == null || keysMap.isEmpty()) {
                    log.warn("[STARTUP] No API keys found in Realtime Database at config/api_keys. Skipping key sync.");
                    return Flux.empty();
                }
                return Flux.fromIterable(keysMap.entrySet());
            })
            .flatMap(entry -> {
                String name = entry.getKey();
                if (name == null) return Mono.empty();
                
                String key = String.valueOf(entry.getValue());
                
                return providerRepository.findById(name.toLowerCase())
                    .switchIfEmpty(Mono.just(new APIProvider(name.toLowerCase(), name, determineType(name), "ACTIVE")))
                    .flatMap(provider -> {
                        provider.setApiKey(key);
                        provider.setLastCheck(new Date());
                        
                        if (provider.getBaseUrl() == null || provider.getBaseUrl().isBlank()) {
                            enrichProviderMetadata(provider);
                        }
                        
                        log.debug("[STARTUP] Syncing provider key for: {}", name);
                        return providerRepository.save(provider);
                    })
                    .onErrorResume(e -> {
                        log.error("[STARTUP] Failed to sync provider {}: {}", name, e.getMessage());
                        return Mono.empty();
                    });
            })
            .collectList()
            .subscribe(
                list -> log.info("[STARTUP] Provider key sync complete. Total providers updated: {}", list.size()),
                err -> log.error("[STARTUP] Critical error during provider key synchronization: {}", err.getMessage())
            );
    }
}
