package com.supremeai.controller;

import com.supremeai.response.ApiResponse;
import com.supremeai.model.APIProvider;
import com.supremeai.admin.ProviderAdminService;
import com.supremeai.service.AIProviderDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/providers")
public class ProvidersController extends BaseAdminController<APIProvider, String> {

    private static final Logger log = LoggerFactory.getLogger(ProvidersController.class);
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(10);

    private final ProviderAdminService providerAdminService;
    private final AIProviderDiscoveryService discoveryService;

    @Autowired
    public ProvidersController(ProviderAdminService providerAdminService,
                               AIProviderDiscoveryService discoveryService) {
        this.providerAdminService = providerAdminService;
        this.discoveryService = discoveryService;
    }

    private Mono<String> getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Mono.error(new IllegalStateException("Not authenticated"));
        }
        return Mono.just(auth.getName());
    }

    @GetMapping("/configured")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getConfiguredProviders() {
        return providerAdminService.getAllProviders()
            .collectList()
            .map(providers -> wrapListSync(providers, "providers"))
            .onErrorResume(e -> handleError("Failed to get configured providers", e));
    }

    @PostMapping("/add")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> addProvider(@RequestBody APIProvider provider) {
        return getCurrentAdminUserId()
            .flatMap(adminUserId -> providerAdminService.addProvider(provider, adminUserId)
                .map(saved -> ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Provider added", "provider", (Object)saved)))))
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>error(e.getMessage()))));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> updateProviderById(@PathVariable String id, @RequestBody APIProvider provider) {
        return getCurrentAdminUserId()
            .flatMap(adminUserId -> providerAdminService.updateProvider(id, provider, adminUserId)
                .map(saved -> {
                    if (saved == null) return ResponseEntity.status(404).body(ApiResponse.<Map<String, Object>>error("Provider not found"));
                    return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Provider updated", "provider", (Object)saved)));
                }))
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>error(e.getMessage()))));
    }

    @PostMapping("/{id}/revive")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> reviveProvider(@PathVariable String id) {
        return getCurrentAdminUserId()
            .flatMap(adminUserId -> providerAdminService.reviveProvider(id, adminUserId)
                .map(saved -> {
                    if (saved == null) return ResponseEntity.status(404).body(ApiResponse.<Map<String, Object>>error("Provider not found"));
                    return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Provider revived successfully", "provider", (Object)saved)));
                }))
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>error(e.getMessage()))));
    }

    @PostMapping("/{id}/activate")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> activateProvider(@PathVariable String id) {
        return getCurrentAdminUserId()
            .flatMap(adminUserId -> providerAdminService.activateProvider(id, adminUserId)
                .map(saved -> {
                    if (saved == null) return ResponseEntity.status(404).body(ApiResponse.<Map<String, Object>>error("Provider not found"));
                    log.info("[API] Provider {} activated by admin {}", id, adminUserId);
                    return ResponseEntity.ok(ApiResponse.ok(Map.of(
                        "message", "Provider activated successfully",
                        "provider", (Object)saved,
                        "status", "ACTIVE"
                    )));
                }))
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>error(e.getMessage()))));
    }

    @PostMapping("/{id}/deactivate")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> deactivateProvider(
            @PathVariable String id,
            @RequestParam(defaultValue = "Admin requested deactivation") String reason) {
        return getCurrentAdminUserId()
            .flatMap(adminUserId -> providerAdminService.deactivateProvider(id, reason, adminUserId)
                .map(saved -> {
                    if (saved == null) return ResponseEntity.status(404).body(ApiResponse.<Map<String, Object>>error("Provider not found"));
                    log.info("[API] Provider {} deactivated by admin {} (reason: {})", id, adminUserId, reason);
                    return ResponseEntity.ok(ApiResponse.ok(Map.of(
                        "message", "Provider deactivated successfully",
                        "provider", (Object)saved,
                        "status", "INACTIVE",
                        "reason", reason
                    )));
                }))
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>error(e.getMessage()))));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<String>>> deleteProvider(@PathVariable String id) {
        return getCurrentAdminUserId()
            .flatMap(adminUserId -> providerAdminService.deleteProvider(id, adminUserId)
                .thenReturn(ResponseEntity.ok(ApiResponse.ok("Provider deleted"))))
            .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.<String>error(e.getMessage()))));
    }

    @PostMapping("/test-key")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> testProviderKey(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String apiKey = payload.get("apiKey");

        log.info("[TEST-KEY] Received test-key request: name={}, apiKeyLength={}", name, apiKey != null ? apiKey.length() : "null");

        if (name == null || apiKey == null) {
            log.warn("[TEST-KEY] Rejected: missing parameters. name={}, apiKey present={}", name, apiKey != null);
            return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error("name and apiKey are required")));
        }

        return providerAdminService.validateKey(name, apiKey)
            .timeout(BLOCK_TIMEOUT)
            .doOnNext(valid -> log.info("[TEST-KEY] Validation result for name={}: {}", name, valid))
            .map(valid -> Boolean.TRUE.equals(valid)
                ? ResponseEntity.ok(ApiResponse.<Map<String, Object>>ok(Map.of("message", "Key validated successfully", "valid", true)))
                : ResponseEntity.status(401).body(ApiResponse.<Map<String, Object>>error("Invalid key or provider unreachable")))
            .onErrorResume(e -> {
                log.error("[TEST-KEY] Unexpected error during validation for name={}: {}", name, e.toString());
                return Mono.just(ResponseEntity.status(500).body(ApiResponse.<Map<String, Object>>error("Validation error: " + e.getMessage())));
            });
    }

    @GetMapping("/discover")
    public Mono<ResponseEntity<ApiResponse<List<Map<String, Object>>>>> discoverModels(@RequestParam(required = false) String query) {
        return discoveryService.discoverModels(query)
            .collectList()
            .timeout(BLOCK_TIMEOUT)
            .map(list -> ResponseEntity.ok(ApiResponse.ok(list)))
            .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.<List<Map<String, Object>>>error("Discovery failed: " + e.getMessage()))));
    }

    @GetMapping("/health-stats")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getHealthStats() {
        return providerAdminService.getHealthStats()
            .timeout(BLOCK_TIMEOUT)
            .map(stats -> ResponseEntity.ok(ApiResponse.ok(stats)));
    }

    @GetMapping("/{id}/roles")
    public Mono<ResponseEntity<ApiResponse<List<String>>>> getConfiguredRoles(@PathVariable String id) {
        return providerAdminService.getAllProviders()
            .filter(p -> p.getId().equals(id))
            .next()
            .flatMap(provider -> Mono.just(providerAdminService.suggestRoles(provider)))
            .map(roles -> ResponseEntity.ok(ApiResponse.ok(roles)))
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Provider not found: " + id)))
            .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.<List<String>>error("Failed to fetch roles: " + e.getMessage()))));
    }

    @PostMapping("/{id}/capability")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> updateCapability(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates) {
        return getCurrentAdminUserId()
            .flatMap(adminUserId -> providerAdminService.patchCapability(id, updates, adminUserId)
                .map(saved -> {
                    if (saved == null) return ResponseEntity.status(404).body(ApiResponse.<Map<String, Object>>error("Provider not found"));
                    return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Capability updated", "provider", (Object)saved)));
                }))
            .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(ApiResponse.<Map<String, Object>>error(e.getMessage()))));
    }

    @PostMapping("/validate-all")
    public Mono<ResponseEntity<ApiResponse<String>>> triggerValidation() {
        providerAdminService.triggerValidation();
        return Mono.just(ResponseEntity.ok(ApiResponse.ok("Validation triggered")));
    }

    @PostMapping("/sanitize")
    public Mono<ResponseEntity<ApiResponse<String>>> sanitizeProviders() {
        return getCurrentAdminUserId()
            .flatMap(adminUserId -> providerAdminService.sanitizeProviders(adminUserId)
                .then(Mono.just(ResponseEntity.ok(ApiResponse.ok("Provider sanitization completed")))))
            .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.<String>error(e.getMessage()))));
    }

    @DeleteMapping("/dead")
    public Mono<ResponseEntity<ApiResponse<String>>> cleanupDeadProviders() {
        return getCurrentAdminUserId()
            .flatMap(adminUserId -> providerAdminService.removeDeadProviders(adminUserId)
                .then(Mono.just(ResponseEntity.ok(ApiResponse.ok("Dead providers removed")))))
            .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.<String>error(e.getMessage()))));
    }
}