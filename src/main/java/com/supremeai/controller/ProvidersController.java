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

import java.util.List;
import java.util.Map;

/**
 * Controller for managing AI providers.
 * Refactored to delegate business logic to ProviderAdminService.
 */
@RestController
@RequestMapping("/api/admin/providers")
public class ProvidersController extends BaseAdminController<APIProvider, String> {

    private static final Logger log = LoggerFactory.getLogger(ProvidersController.class);

    private final ProviderAdminService providerAdminService;
    private final AIProviderDiscoveryService discoveryService;

    @Autowired
    public ProvidersController(ProviderAdminService providerAdminService,
                               AIProviderDiscoveryService discoveryService) {
        this.providerAdminService = providerAdminService;
        this.discoveryService = discoveryService;
    }

    private String getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }
        return auth.getName();
    }

    @GetMapping("/configured")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfiguredProviders() {
        try {
            List<APIProvider> providers = providerAdminService.getAllProviders().collectList().block();
            return wrapListSync(providers, "providers");
        } catch (Exception e) {
            return handleErrorSync("Failed to fetch providers", e);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addProvider(@RequestBody APIProvider provider) {
        try {
            String adminUserId = getCurrentAdminUserId();
            APIProvider saved = providerAdminService.addProvider(provider, adminUserId).block();
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Provider added", "provider", (Object)saved)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>>
            updateProviderById(@PathVariable String id, @RequestBody APIProvider provider) {
        try {
            String adminUserId = getCurrentAdminUserId();
            APIProvider saved = providerAdminService.updateProvider(id, provider, adminUserId).block();
            if (saved == null) return ResponseEntity.status(404).body(ApiResponse.error("Provider not found"));
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Provider updated", "provider", (Object)saved)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/revive")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reviveProvider(@PathVariable String id) {
        try {
            String adminUserId = getCurrentAdminUserId();
            APIProvider saved = providerAdminService.reviveProvider(id, adminUserId).block();
            if (saved == null) return ResponseEntity.status(404).body(ApiResponse.error("Provider not found"));
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Provider revived successfully", "provider", (Object)saved)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Manually activate a provider (set status to ACTIVE)
     * Use this to activate GCloud/real API providers that have valid keys
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> activateProvider(@PathVariable String id) {
        try {
            String adminUserId = getCurrentAdminUserId();
            APIProvider saved = providerAdminService.activateProvider(id, adminUserId).block();
            if (saved == null) return ResponseEntity.status(404).body(ApiResponse.error("Provider not found"));
            log.info("[API] Provider {} activated by admin {}", id, adminUserId);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "Provider activated successfully",
                "provider", (Object)saved,
                "status", "ACTIVE"
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Manually deactivate a provider (set status to INACTIVE)
     * Use this to mark fake/test API keys as inactive
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deactivateProvider(
            @PathVariable String id,
            @RequestParam(defaultValue = "Admin requested deactivation") String reason) {
        try {
            String adminUserId = getCurrentAdminUserId();
            APIProvider saved = providerAdminService.deactivateProvider(id, reason, adminUserId).block();
            if (saved == null) return ResponseEntity.status(404).body(ApiResponse.error("Provider not found"));
            log.info("[API] Provider {} deactivated by admin {} (reason: {})", id, adminUserId, reason);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "Provider deactivated successfully",
                "provider", (Object)saved,
                "status", "INACTIVE",
                "reason", reason
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProvider(@PathVariable String id) {
        try {
            String adminUserId = getCurrentAdminUserId();
            providerAdminService.deleteProvider(id, adminUserId).block();
            return ResponseEntity.ok(ApiResponse.ok("Provider deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/test-key")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testProviderKey(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String apiKey = payload.get("apiKey");

        log.info("[TEST-KEY] Received test-key request: name={}, apiKeyLength={}", name, apiKey != null ? apiKey.length() : "null");

        if (name == null || apiKey == null) {
            log.warn("[TEST-KEY] Rejected: missing parameters. name={}, apiKey present={}", name, apiKey != null);
            return ResponseEntity.badRequest().body(ApiResponse.error("name and apiKey are required"));
        }

        try {
            Boolean valid = providerAdminService.validateKey(name, apiKey).block();
            log.info("[TEST-KEY] Validation result for name={}: {}", name, valid);
            if (Boolean.TRUE.equals(valid)) {
                return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Key validated successfully", "valid", true)));
            } else {
                return ResponseEntity.status(401).body(ApiResponse.error("Invalid key or provider unreachable"));
            }
        } catch (Exception e) {
            log.error("[TEST-KEY] Unexpected error during validation for name={}: {}", name, e.toString());
            return ResponseEntity.status(500).body(ApiResponse.error("Validation error: " + e.getMessage()));
        }
    }

    @GetMapping("/discover")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> discoverModels(@RequestParam(required = false) String query) {
        List<Map<String, Object>> list = discoveryService.discoverModels(query).collectList().block();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/health-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStats() {
        Map<String, Object> stats = providerAdminService.getHealthStats().block();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<List<String>>> getConfiguredRoles(@PathVariable String id) {
        try {
            Mono<List<APIProvider>> providersMono = providerAdminService.getAllProviders()
                    .filter(p -> p.getId().equals(id))
                    .collectList();
            List<APIProvider> list = providersMono.block(java.time.Duration.ofSeconds(5));
            if (list == null || list.isEmpty()) {
                return ResponseEntity.status(404).body(ApiResponse.error("Provider not found"));
            }
            List<String> roles = providerAdminService.suggestRoles(list.get(0));
            return ResponseEntity.ok(ApiResponse.ok(roles));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch roles: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/capability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCapability(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates) {
        try {
            String adminUserId = getCurrentAdminUserId();
            APIProvider saved = providerAdminService.patchCapability(id, updates, adminUserId).block();
            if (saved == null) return ResponseEntity.status(404).body(ApiResponse.error("Provider not found"));
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Capability updated", "provider", (Object)saved)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/validate-all")
    public ResponseEntity<ApiResponse<String>> triggerValidation() {
        providerAdminService.triggerValidation();
        return ResponseEntity.ok(ApiResponse.ok("Validation triggered"));
    }

    @PostMapping("/sanitize")
    public ResponseEntity<ApiResponse<String>> sanitizeProviders() {
        try {
            String adminUserId = getCurrentAdminUserId();
            providerAdminService.sanitizeProviders(adminUserId).block();
            return ResponseEntity.ok(ApiResponse.ok("Provider sanitization completed"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/dead")
    public ResponseEntity<ApiResponse<String>> cleanupDeadProviders() {
        try {
            String adminUserId = getCurrentAdminUserId();
            providerAdminService.removeDeadProviders(adminUserId).block();
            return ResponseEntity.ok(ApiResponse.ok("Dead providers removed"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }
}
