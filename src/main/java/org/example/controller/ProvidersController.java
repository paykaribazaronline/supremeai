package org.example.controller;

import org.example.model.APIProvider;
import org.example.service.ProviderManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private ProviderManagementService providerManagementService;

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableProviders() {
        try {
            return ResponseEntity.ok(providerManagementService.getAvailableProviders());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/configured")
    public ResponseEntity<?> getConfiguredProviders() {
        try {
            return ResponseEntity.ok(providerManagementService.getConfiguredProviders());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addProvider(@RequestBody APIProvider provider) {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "provider", providerManagementService.saveProvider(provider)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProvider(@PathVariable String id, @RequestBody APIProvider provider) {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "provider", providerManagementService.updateProvider(id, provider)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test/{id}")
    public ResponseEntity<?> testProvider(@PathVariable String id) {
        try {
            return ResponseEntity.ok(providerManagementService.probeProvider(id));
        } catch (Exception e) {
            String message = e.getMessage() == null ? "Provider probe failed" : e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.status(404).body(Map.of("error", message));
            }
            return ResponseEntity.status(500).body(Map.of("error", message));
        }
    }

    @PostMapping("/probe/{id}")
    public ResponseEntity<?> probeProvider(@PathVariable String id) {
        return testProvider(id);
    }

    @PostMapping("/rotate/{id}")
    public ResponseEntity<?> rotateProvider(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(providerManagementService.rotateProvider(id, request));
        } catch (Exception e) {
            String message = e.getMessage() == null ? "Provider rotation failed" : e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.status(404).body(Map.of("error", message));
            }
            return ResponseEntity.status(500).body(Map.of("error", message));
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeProvider(@RequestBody Map<String, String> request) {
        try {
            String id = request.get("id");
            boolean removed = providerManagementService.removeProvider(id);
            return ResponseEntity.ok(Map.of("success", removed));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProvider(@PathVariable String id) {
        try {
            return ResponseEntity.ok(Map.of("success", providerManagementService.removeProvider(id)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
