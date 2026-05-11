package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Value("${ai.providers.ollama.endpoint:http://localhost:11434}")
    private String ollamaEndpoint;

    private String determineType(String name) {
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
        String name = provider.getName().toLowerCase();
        
        if (name.contains("gemini")) {
            provider.setBaseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent");
            provider.setModels(java.util.List.of("gemini-1.5-flash", "gemini-1.5-pro"));
            provider.setCapabilities(java.util.List.of("chat", "reasoning", "multimodal"));
        } else if (name.contains("openai")) {
            provider.setBaseUrl("https://api.openai.com/v1/chat/completions");
            provider.setModels(java.util.List.of("gpt-4", "gpt-3.5-turbo"));
            provider.setCapabilities(java.util.List.of("chat", "code"));
        } else if (name.contains("huggingface")) {
            provider.setBaseUrl("https://api-inference.huggingface.co/models/meta-llama/Llama-3.3-70B-Instruct/v1/chat/completions");
            provider.setCapabilities(java.util.List.of("chat"));
        } else if (name.contains("codegeex")) {
            provider.setBaseUrl("https://open.bigmodel.cn/api/coding/paas/v4/chat/completions");
            provider.setModels(java.util.List.of("codegeex-4", "codegeex-4-lite"));
            provider.setCapabilities(java.util.List.of("chat", "code", "reasoning", "multimodal"));
            provider.setLanguages(java.util.List.of("zh", "en", "multi"));
        } else if (name.contains("stepfun")) {
            provider.setBaseUrl("https://api.stepfun.com/v1/chat/completions");
            provider.setModels(java.util.List.of("step-3.5-flash", "step-3.5-pro", "step-1"));
        } else if (name.contains("kimi")) {
            provider.setBaseUrl("https://api.moonshot.cn/v1/chat/completions");
        } else if (name.contains("mistral")) {
            provider.setBaseUrl("https://api.mistral.ai/v1/chat/completions");
        } else if (name.contains("groq")) {
            provider.setBaseUrl("https://api.groq.com/openai/v1/chat/completions");
        } else if (name.contains("deepseek")) {
            provider.setBaseUrl("https://api.deepseek.com/v1/chat/completions");
        } else if (name.contains("anthropic")) {
            provider.setBaseUrl("https://api.anthropic.com/v1/messages");
        } else if (name.contains("ollama")) {
            provider.setBaseUrl(ollamaEndpoint + "/v1/chat/completions");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncProvidersOnStartup() {
        log.info("Syncing AI providers from Realtime Database to Firestore...");
        
        realtimeService.getData("config/api_keys")
            .flatMapMany(keysMap -> {
                if (keysMap == null || keysMap.isEmpty()) {
                    log.warn("No API keys found in Realtime Database at config/api_keys");
                    return Flux.empty();
                }
                
                return Flux.fromIterable(keysMap.entrySet());
            })
            .flatMap(entry -> {
                String name = entry.getKey();
                String key = String.valueOf(entry.getValue());
                
                return providerRepository.findById(name.toLowerCase())
                    .switchIfEmpty(Mono.just(new APIProvider(name.toLowerCase(), name, determineType(name), "ACTIVE")))
                    .flatMap(provider -> {
                        provider.setApiKey(key);
                        provider.setLastCheck(new Date());
                        
                        // If metadata is missing, enrich it
                        if (provider.getBaseUrl() == null || provider.getBaseUrl().isBlank()) {
                            enrichProviderMetadata(provider);
                        }
                        
                        log.info("Syncing provider: {} (Type: {})", name, provider.getType());
                        return providerRepository.save(provider);
                    });
            })
            .collectList()
            .doOnSuccess(list -> log.info("Successfully synced {} providers to Firestore.", list.size()))
            .doOnError(err -> log.error("Error syncing providers: {}", err.getMessage()))
            .subscribe();
    }
}
