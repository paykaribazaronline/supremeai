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

        if (name == null || apiKey == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("name and apiKey are required"));
        }

        Boolean valid = providerAdminService.validateKey(name, apiKey).block();
        if (Boolean.TRUE.equals(valid)) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Key validated successfully", "valid", true)));
        } else {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid key or provider error"));
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
