package com.supremeai.controller;

import com.supremeai.response.ApiResponse;
import com.supremeai.service.AIProviderDiscoveryService;

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

import java.util.Map;

@RestController
@RequestMapping("/api/admin/providers")
public class ProvidersController extends BaseAdminController<APIProvider, String> {

    @Autowired
    private ProviderRepository providerRepository;
    
    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private AIProviderDiscoveryService discoveryService;

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
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getConfiguredProviders() {
        return wrapList(providerRepository.findAll(), "providers");
    }

    @PostMapping("/add")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> addProvider(@RequestBody APIProvider provider) {
        return updateProvider(provider);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> updateProviderById(@PathVariable String id, @RequestBody APIProvider provider) {
        provider.setId(id);
        return updateProvider(provider);
    }

    @PostMapping("/remove")
    public Mono<ResponseEntity<ApiResponse<String>>> removeProvider(@RequestBody Map<String, String> payload) {
        String providerId = payload.get("providerId");
        if (providerId == null) {
            return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error("providerId is required")));
        }
        return deleteProvider(providerId).map(re -> {
            boolean success = re.getBody() != null && re.getBody().success();
            String error = (re.getBody() != null) ? re.getBody().error() : "Unknown error";
            return ResponseEntity.status(re.getStatusCode()).body(success ? ApiResponse.ok("Provider removed") : ApiResponse.error(error));
        });
    }

    @PostMapping("/test-key")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> testProviderKey(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String apiKey = payload.get("apiKey");

        if (name == null || apiKey == null) {
            return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error("name and apiKey are required")));
        }

        return discoveryService.validateKey(name, apiKey)
                .map(valid -> {
                    if (valid) {
                        return ResponseEntity.ok(ApiResponse.ok(Map.<String, Object>of("message", "Key validated successfully", "valid", true)));
                    } else {
                        return ResponseEntity.status(401).body(ApiResponse.<Map<String, Object>>error("Invalid key or provider error"));
                    }
                });
    }

    @GetMapping("/discover")
    public Mono<ResponseEntity<ApiResponse<java.util.List<Map<String, Object>>>>> discoverModels(@RequestParam(required = false) String query) {
        return discoveryService.discoverModels(query)
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponse.ok(list)));
    }

    @GetMapping("/scan")
    public Mono<ResponseEntity<ApiResponse<java.util.List<Map<String, Object>>>>> scanDeployments() {
        return discoveryService.scanDeployments()
                .collectList()
                .map(list -> ResponseEntity.ok(ApiResponse.ok(list)));
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> updateProvider(@RequestBody APIProvider provider) {
        String adminUserId = getCurrentAdminUserId();
        return providerRepository.save(provider)
                .flatMap(saved -> {
                    // Log admin action reactive way
                    ActivityLog log = new ActivityLog();
                    log.setUser(adminUserId);
                    log.setAction("UPDATE_PROVIDER");
                    log.setCategory("PROVIDER_MANAGEMENT");
                    log.setSeverity("INFO");
                    log.setOutcome("SUCCESS");
                    log.setDetails("Updated provider: " + saved.getId() + " (" + saved.getName() + ")");
                    
                    return activityLogRepository.save(log)
                            .thenReturn(ResponseEntity.ok(ApiResponse.ok(Map.of(
                                "message", "Provider updated successfully",
                                "provider", saved
                            ))));
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<String>>> deleteProvider(@PathVariable String id) {
        String adminUserId = getCurrentAdminUserId();
        return providerRepository.findById(id)
                .flatMap(provider -> {
                    String providerName = provider.getName();
                    return providerRepository.deleteById(id)
                            .then(Mono.fromCallable(() -> {
                                ActivityLog log = new ActivityLog();
                                log.setUser(adminUserId);
                                log.setAction("DELETE_PROVIDER");
                                log.setCategory("PROVIDER_MANAGEMENT");
                                log.setSeverity("WARN");
                                log.setOutcome("SUCCESS");
                                log.setDetails("Deleted provider: " + id + " (" + providerName + ")");
                                return log;
                            }))
                            .flatMap(activityLogRepository::save)
                            .thenReturn(ResponseEntity.ok(ApiResponse.ok("Provider deleted")));
                })
                .defaultIfEmpty(ResponseEntity.status(404).body(ApiResponse.<String>error("Provider not found")));
    }
}
