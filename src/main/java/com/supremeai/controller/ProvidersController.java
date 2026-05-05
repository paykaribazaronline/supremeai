package com.supremeai.controller;

import com.supremeai.model.APIProvider;
import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/providers")
public class ProvidersController extends BaseAdminController<APIProvider, String> {

    @Autowired
    private ProviderRepository providerRepository;
    
    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private com.supremeai.provider.AIProviderFactory aiProviderFactory;

    public ProvidersController(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    private String getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Not authenticated");
        }
        return auth.getName();
    }

        @GetMapping("/configured")
    public Mono<ResponseEntity<Object>> getConfiguredProviders() {
        return wrapList(providerRepository.findAll(), "providers");
    }

    @PostMapping("/add")
    public Mono<ResponseEntity<Object>> addProvider(@RequestBody APIProvider provider) {
        return updateProvider(provider);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Object>> updateProviderById(@PathVariable String id, @RequestBody APIProvider provider) {
        provider.setId(id);
        return updateProvider(provider);
    }

    @PostMapping("/remove")
    public Mono<ResponseEntity<Object>> removeProvider(@RequestBody Map<String, String> payload) {
        String providerId = payload.get("providerId");
        if (providerId == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "providerId is required")));
        }
        return deleteProvider(providerId);
    }

    @PostMapping("/test-key")
    public Mono<ResponseEntity<Object>> testProviderKey(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String apiKey = payload.get("apiKey");

        if (name == null || apiKey == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "name and apiKey are required")));
        }

        try {
            com.supremeai.provider.AIProvider provider = aiProviderFactory.getProvider(name, apiKey);
            return provider.generate("test")
                    .map(response -> ResponseEntity.ok((Object) Map.of(
                            "success", true,
                            "message", "Key validated successfully",
                            "response", response
                    )))
                    .onErrorResume(e -> Mono.just(ResponseEntity.status(401).body(Map.of(
                            "success", false,
                            "error", "Invalid key or provider error: " + e.getMessage()
                    ))));
        } catch (Exception e) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Unsupported provider or configuration error: " + e.getMessage()
            )));
        }
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> updateProvider(@RequestBody APIProvider provider) {
        return providerRepository.save(provider)
                .map(saved -> {
// Log admin action
                                ActivityLog log = new ActivityLog();
                                log.setUser(getCurrentAdminUserId());
                                log.setAction("UPDATE_PROVIDER");
                    log.setCategory("PROVIDER_MANAGEMENT");
                    log.setSeverity("INFO");
                    log.setOutcome("SUCCESS");
                    log.setDetails("Updated provider: " + saved.getId() + " (" + saved.getName() + ")");
                    activityLogRepository.save(log).block();
                    
                    return (ResponseEntity<Object>) ResponseEntity.ok((Object) Map.of(
                        "message", "Provider updated successfully",
                        "provider", saved
                    ));
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> deleteProvider(@PathVariable String id) {
        return providerRepository.findById(id)
                .flatMap(provider -> {
                    String providerName = provider.getName();
                    return wrapDelete(providerRepository.deleteById(id), "Provider deleted")
                            .doOnSuccess(aVoid -> {
                                // Log admin action
                                ActivityLog log = new ActivityLog();
                                log.setUser(getCurrentAdminUserId());
                                log.setAction("DELETE_PROVIDER");
                                log.setCategory("PROVIDER_MANAGEMENT");
                                log.setSeverity("WARN"); // Deleting providers is notable
                                log.setOutcome("SUCCESS");
                                log.setDetails("Deleted provider: " + id + " (" + providerName + ")");
                                activityLogRepository.save(log).block();
                            });
                })
                .defaultIfEmpty(ResponseEntity.status(404).body((Object) Map.of("error", "Provider not found")));
    }
}
