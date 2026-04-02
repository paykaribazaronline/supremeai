package org.example.controller;

import org.example.model.APIProvider;
import org.example.service.ProviderRegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * API Providers Controller
 * Manages AI API provider configurations
 */
@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProvidersController {
    @Autowired
    private ProviderRegistryService providerRegistryService;

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableProviders() {
        try {
            return ResponseEntity.ok(providerRegistryService.getActiveProviders());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/configured")
    public ResponseEntity<?> getConfiguredProviders() {
        try {
            return ResponseEntity.ok(providerRegistryService.getAllProviders());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addProvider(@RequestBody APIProvider provider) {
        try {
            APIProvider savedProvider = providerRegistryService.addOrUpdateProvider(provider);
            return ResponseEntity.ok(Map.of("success", true, "id", savedProvider.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProvider(@PathVariable String id, @RequestBody APIProvider provider) {
        try {
            provider.setId(id);
            providerRegistryService.addOrUpdateProvider(provider);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test/{id}")
    public ResponseEntity<?> testProvider(@PathVariable String id) {
        try {
            APIProvider provider = providerRegistryService.getProvider(id);
            
            if (provider == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Provider not found"));
            }

            provider.setLastTested(LocalDateTime.now());
            provider.setStatus("active");
            provider.setSuccessCount(provider.getSuccessCount() + 1);
            providerRegistryService.addOrUpdateProvider(provider);
            
            return ResponseEntity.ok(Map.of("success", true, "status", "active"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeProvider(@RequestBody Map<String, String> request) {
        try {
            String id = request.get("id");
            boolean removed = providerRegistryService.removeProvider(id);
            return ResponseEntity.ok(Map.of("success", removed));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
