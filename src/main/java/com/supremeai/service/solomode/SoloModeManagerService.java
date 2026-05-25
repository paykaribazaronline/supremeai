package com.supremeai.service.solomode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.service.FirebaseRealtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.ProviderRepository;

/**
 * Manages Solo Mode Intelligence Features (SL-01 to SL-04)
 * Ensures SupremeAI can operate even if all external APIs are completely down.
 */
@Service
public class SoloModeManagerService {

    private static final Logger log = LoggerFactory.getLogger(SoloModeManagerService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private AIProviderFactory providerFactory;

    @Value("${solo.fallback.model:phi-3-mini}")
    private String fallbackModelName;

    @Value("${supremeai.solo-mode.fallback-provider:airllm-sidecar}")
    private String fallbackProviderId;

    @Value("${airllm.healthcheck-url:${AIRLLM_HEALTHCHECK_URL:http://localhost:8081/health}}")
    private String airllmHealthcheckUrl;

    @Autowired
    private org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder;

    // SL-01: Local AI Model Auto-Download Tracker
    private boolean isLocalModelDownloading = false;
    private boolean isLocalModelAvailable = false;

    public Mono<Boolean> checkAirLLMHealth() {
        return webClientBuilder.build().get()
                .uri(airllmHealthcheckUrl)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false);
    }

    /**
     * SL-01: Local AI Model Auto-Download
     * Triggered if all external providers fail. Auto-pulls the smallest viable GGUF model (e.g., Phi-3-mini).
     */
    public Mono<String> triggerLocalModelFallback(String prompt) {
        return checkAirLLMHealth()
                .flatMap(isHealthy -> {
                    if (isHealthy) {
                        isLocalModelAvailable = true;
                        isLocalModelDownloading = false;
                        return callAirLLMSidecar(prompt);
                    }

                    if (!isLocalModelDownloading) {
                        log.warn("All external providers failed. Initiating auto-download of {} GGUF fallback model via AirLLM sidecar at {}.", 
                                fallbackModelName, airllmHealthcheckUrl);
                        isLocalModelDownloading = true;

                        // Periodically check health every 1 second, up to 10 times
                        return reactor.core.publisher.Flux.interval(java.time.Duration.ofSeconds(1))
                                .take(10)
                                .flatMap(i -> checkAirLLMHealth())
                                .filter(h -> h)
                                .next() // get the first true value, if any
                                .flatMap(h -> {
                                    log.info("{} successfully downloaded and loaded into memory via AirLLM sidecar.", fallbackModelName);
                                    isLocalModelDownloading = false;
                                    isLocalModelAvailable = true;
                                    return callAirLLMSidecar(prompt);
                                })
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.warn("AirLLM sidecar at {} is still initializing or downloading the model.", airllmHealthcheckUrl);
                                    return Mono.just("System is currently downloading and loading the offline fallback model (" + fallbackModelName + "). Please wait...");
                                }));
                    } else {
                        return Mono.just("System is currently downloading the offline fallback model (" + fallbackModelName + "). Please wait...");
                    }
                });
    }

    private Mono<String> callAirLLMSidecar(String prompt) {
        log.info("[SoloMode] Calling AirLLM sidecar for fallback model: {} with prompt: {}", fallbackModelName, prompt);
        return providerRepository.findById(fallbackProviderId)
                .switchIfEmpty(providerRepository.findById(fallbackProviderId.toLowerCase()))
                .flatMap(airllmConfig -> {
                    log.info("[SoloMode] Found fallback provider: baseUrl={}, model={}", airllmConfig.getBaseUrl(), airllmConfig.getModelName());
                    AIProvider provider = providerFactory.createProviderFromConfig(airllmConfig);
                    if (provider == null) {
                        return Mono.error(new RuntimeException("Could not construct provider for " + fallbackProviderId));
                    }
                    return provider.generate(prompt);
                })
                .onErrorResume(e -> {
                    log.error("[SoloMode] Error communicating with AirLLM sidecar: {}", e.getMessage());
                    return Mono.just("[Local Fallback " + fallbackModelName + "] (Offline Mode Fallback response) Result for: " + prompt);
                });
    }

    /**
     * SL-02: P2P Knowledge Sync
     * SupremeAI instances can share learned knowledge entries via signed Firestore writes.
     */
    public Mono<Void> broadcastP2PKnowledge(String topic, String learnedInsight) {
        log.info("Broadcasting P2P knowledge sync for topic: {}", topic);
        String nodeId = "node_" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> syncData = new HashMap<>();
        syncData.put("nodeId", nodeId);
        syncData.put("insight", learnedInsight);
        syncData.put("timestamp", System.currentTimeMillis());
        syncData.put("signature", "signed_" + UUID.randomUUID().toString()); // Placeholder for crypto signature

        return firebaseRealtimeService.setData("p2p_knowledge_sync/" + topic + "/" + nodeId, syncData);
    }

    /**
     * SL-03: Offline Knowledge Distillation
     * Periodic job compresses Firestore system_learning into updated core_knowledge.json offline snapshot.
     */
    @Scheduled(cron = "0 0 1 * * ?") // Every day at 1 AM
    public void distillOfflineKnowledge() {
        log.info("Starting Offline Knowledge Distillation...");
        firebaseRealtimeService.getData("system_learning")
                .subscribe(
                        data -> {
                            if (data == null || data.isEmpty()) {
                                log.info("No new learning data to distill.");
                                return;
                            }
                            try {
                                File targetFile = new File("core_knowledge.json");
                                Map<String, Object> distilled = new HashMap<>();
                                distilled.put("last_updated", System.currentTimeMillis());
                                distilled.put("entries", data.size());
                                distilled.put("knowledge", data);
                                
                                mapper.writerWithDefaultPrettyPrinter().writeValue(targetFile, distilled);
                                log.info("Successfully distilled {} entries into core_knowledge.json", data.size());
                            } catch (Exception e) {
                                log.error("Failed to write offline knowledge distillation", e);
                            }
                        },
                        error -> log.error("Failed to fetch system_learning for distillation", error)
                );
    }

    /**
     * SL-04: Emergency Code Generation
     * Template-based code scaffolding that works with zero AI.
     */
    public String generateEmergencyScaffold(String requirement) {
        log.info("Triggering SL-04 Emergency Code Generation (Zero-AI Template Matching) for: {}", requirement);
        String lowerReq = requirement.toLowerCase();
        
        StringBuilder code = new StringBuilder();
        code.append("/**\n * EMERGENCY SCAFFOLD GENERATED\n * No AI APIs were reachable.\n */\n\n");
        
        if (lowerReq.contains("spring") || lowerReq.contains("java") || lowerReq.contains("boot")) {
            code.append("@RestController\n")
                .append("public class EmergencyController {\n")
                .append("    @GetMapping(\"/api/health\")\n")
                .append("    public String health() {\n")
                .append("        return \"OK\";\n")
                .append("    }\n")
                .append("}\n");
        } else if (lowerReq.contains("react") || lowerReq.contains("frontend") || lowerReq.contains("web")) {
            code.append("import React from 'react';\n\n")
                .append("export default function EmergencyApp() {\n")
                .append("  return (\n")
                .append("    <div className=\"p-4\">\n")
                .append("      <h1>Emergency Web Scaffold</h1>\n")
                .append("      <p>App generated in offline mode.</p>\n")
                .append("    </div>\n")
                .append("  );\n")
                .append("}\n");
        } else {
            code.append("// Generic Offline Script\n")
                .append("console.log('Emergency scaffold initialized.');\n")
                .append("function start() {\n")
                .append("  // TODO: Add logic here\n")
                .append("}\n");
        }
        
        return code.toString();
    }

    /**
     * SL-02: Step Limit Guard
     * Checks if autonomous step execution should be allowed based on step count and timeout.
     */
    private static final int MAX_STEPS = 15;
    private static final long TIMEOUT_MINUTES = 5;
    private final java.util.concurrent.atomic.AtomicInteger stepCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    private volatile long stepStartTime = 0;

    public boolean canExecuteAutonomousStep() {
        if (stepCounter.get() >= MAX_STEPS) {
            log.warn("Step limit reached ({}) for this session", MAX_STEPS);
            return false;
        }
        
        if (stepStartTime > 0 && 
            (System.currentTimeMillis() - stepStartTime) > (TIMEOUT_MINUTES * 60 * 1000)) {
            log.warn("Timeout reached ({} min) for this session", TIMEOUT_MINUTES);
            return false;
        }
        
        return true;
    }

    public void incrementStepCounter() {
        stepCounter.incrementAndGet();
        stepStartTime = System.currentTimeMillis();
    }

    public void resetStepCounter() {
        stepCounter.set(0);
        stepStartTime = 0;
    }

    /**
     * SL-03: Provider Recovery
     * Attempts to recover failed providers by checking their health status.
     */
    public Mono<List<String>> recoverFailedProviders() {
        log.info("Attempting to recover failed providers...");
        return providerRepository.findAll()
                .filter(p -> p.getStatus() != null && "QUARANTINED".equalsIgnoreCase(p.getStatus()))
                .flatMap(provider -> 
                    providerFactory.checkProviderHealth(provider)
                            .flatMap(isHealthy -> {
                                if (Boolean.TRUE.equals(isHealthy)) {
                                    log.info("Provider {} recovered, reactivating", provider.getId());
                                    provider.setStatus("ACTIVE");
                                    return providerRepository.save(provider)
                                            .thenReturn(provider.getId());
                                }
                                return Mono.just("");
                            })
                            .onErrorResume(e -> {
                                log.error("Error during health check recovery for provider {}: {}", provider.getId(), e.getMessage());
                                return Mono.just("");
                            })
                )
                .filter(id -> id != null && !id.isEmpty())
                .collectList()
                .doOnNext(recovered -> log.info("Recovered {} providers: {}", recovered.size(), recovered));
    }

    /**
     * SL-04: Vision Service Graceful Degradation
     * Returns text-only fallback when VisionService is unavailable.
     */
    public Mono<String> getVisionFallback(String prompt, byte[] imageData) {
        log.info("VisionService unavailable, using text-only fallback for prompt");
        return Mono.fromCallable(() -> {
            StringBuilder fallback = new StringBuilder();
            fallback.append("[VISION_FALLBACK] ");
            
            if (imageData != null && imageData.length > 0) {
                fallback.append("Image analysis skipped (VisionService unavailable). ");
                fallback.append("Image size: ").append(imageData.length).append(" bytes. ");
            }
            
            fallback.append("Prompt analysis: ").append(prompt);
            return fallback.toString();
        });
    }
}
