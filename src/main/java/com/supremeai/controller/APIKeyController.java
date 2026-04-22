package com.supremeai.controller;

import com.supremeai.model.UserApiKey;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.security.ApiKeyRotationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for managing per-user API keys.
 * Connects the frontend APIKeysManager.tsx to the backend.
 *
 * Endpoints:
 *   GET    /api/apikeys           - List current user's keys
 *   POST   /api/apikeys           - Add a new key
 *   PUT    /api/apikeys/{id}      - Update a key
 *   DELETE /api/apikeys/{id}      - Delete a key
 *   POST   /api/apikeys/{id}/test - Test if a key is valid
 *   GET    /api/apikeys/usage     - Get usage stats
 */
@RestController
@RequestMapping("/api/apikeys")
@CrossOrigin(origins = "*", maxAge = 3600)
public class APIKeyController {

    @Autowired
    private UserApiKeyRepository userApiKeyRepository;

    @Autowired
    private ApiKeyRotationService rotationService;

    /**
     * Get the current authenticated user's Firebase UID.
     */
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Not authenticated");
        }
        return auth.getName();
    }

    /**
     * GET /api/apikeys - List all API keys for the current user.
     * Returns keys with masked API values for security.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listKeys() {
        String userId = getCurrentUserId();
        List<UserApiKey> keys = userApiKeyRepository.findByUserId(userId).collectList().block();

        if (keys == null) keys = Collections.emptyList();

        List<Map<String, Object>> result = keys.stream().map(key -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", key.getId());
            map.put("provider", key.getProvider());
            map.put("label", key.getLabel());
            map.put("apiKey", key.getMaskedKey());
            map.put("baseUrl", key.getBaseUrl());
            map.put("models", key.getModels());
            map.put("status", key.getStatus());
            map.put("addedAt", key.getAddedAt() != null ? key.getAddedAt().toString() : null);
            map.put("lastTested", key.getLastTested() != null ? key.getLastTested().toString() : null);
            map.put("requestCount", key.getRequestCount());
            map.put("needsRotation", key.needsRotation());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/apikeys - Add a new API key.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addKey(@RequestBody Map<String, Object> body) {
        String userId = getCurrentUserId();

        UserApiKey key = new UserApiKey();
        key.setUserId(userId);
        key.setProvider((String) body.get("provider"));
        key.setLabel((String) body.get("label"));
        key.setApiKey((String) body.get("apiKey"));
        key.setBaseUrl((String) body.get("baseUrl"));

        if (body.get("models") instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> models = (List<String>) body.get("models");
            key.setModels(models);
        }

        // Set rotation due date based on provider limits (default 30 days)
        int rotationDays = rotationService.getRotationDaysForKey(key.getProvider());
        key.setRotationDueAt(LocalDateTime.now().plusDays(rotationDays));

        UserApiKey saved = userApiKeyRepository.save(key).block();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("id", saved != null ? saved.getId() : null);
        response.put("message", "API key added successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/apikeys/{id} - Update an existing API key.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateKey(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        String userId = getCurrentUserId();

        UserApiKey key = userApiKeyRepository.findById(id).block();
        if (key == null || !userId.equals(key.getUserId())) {
            return ResponseEntity.status(404).body(Map.of("error", "API key not found"));
        }

        if (body.containsKey("provider")) key.setProvider((String) body.get("provider"));
        if (body.containsKey("label")) key.setLabel((String) body.get("label"));
        if (body.containsKey("apiKey")) key.setApiKey((String) body.get("apiKey"));
        if (body.containsKey("baseUrl")) key.setBaseUrl((String) body.get("baseUrl"));
        if (body.containsKey("status")) key.setStatus((String) body.get("status"));
        if (body.get("models") instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> models = (List<String>) body.get("models");
            key.setModels(models);
        }

        userApiKeyRepository.save(key).block();

        return ResponseEntity.ok(Map.of("status", "success", "message", "API key updated"));
    }

    /**
     * DELETE /api/apikeys/{id} - Delete an API key.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteKey(@PathVariable String id) {
        String userId = getCurrentUserId();

        UserApiKey key = userApiKeyRepository.findById(id).block();
        if (key == null || !userId.equals(key.getUserId())) {
            return ResponseEntity.status(404).body(Map.of("error", "API key not found"));
        }

        userApiKeyRepository.delete(key).block();

        return ResponseEntity.ok(Map.of("status", "success", "message", "API key removed"));
    }

    /**
     * POST /api/apikeys/{id}/test - Test if an API key is valid by making
     * a lightweight request to the provider's API.
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testKey(@PathVariable String id) {
        String userId = getCurrentUserId();

        UserApiKey key = userApiKeyRepository.findById(id).block();
        if (key == null || !userId.equals(key.getUserId())) {
            return ResponseEntity.status(404).body(Map.of("error", "API key not found"));
        }

        // Test the key by making a lightweight request to the provider
        Map<String, Object> testResult = rotationService.testApiKey(key);

        key.setLastTested(LocalDateTime.now());
        key.setStatus(testResult.get("valid").equals(true) ? "active" : "error");
        userApiKeyRepository.save(key).block();

        return ResponseEntity.ok(testResult);
    }

    /**
     * GET /api/apikeys/usage - Get usage statistics for the current user's API keys.
     */
    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> getUsage() {
        String userId = getCurrentUserId();
        List<UserApiKey> keys = userApiKeyRepository.findByUserId(userId).collectList().block();

        if (keys == null) keys = Collections.emptyList();

        long totalRequests = keys.stream()
                .mapToLong(k -> k.getRequestCount() != null ? k.getRequestCount() : 0L)
                .sum();

        double totalCost = keys.stream()
                .mapToDouble(k -> k.getEstimatedCost() != null ? k.getEstimatedCost() : 0.0)
                .sum();

        long activeKeys = keys.stream()
                .filter(k -> "active".equals(k.getStatus()))
                .count();

        long providerCount = keys.stream()
                .map(UserApiKey::getProvider)
                .distinct()
                .count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRequests", totalRequests);
        stats.put("activeKeys", activeKeys);
        stats.put("totalCost", Math.round(totalCost * 100.0) / 100.0);
        stats.put("providers", providerCount);
        stats.put("totalKeys", keys.size());

        // Per-provider breakdown
        Map<String, Map<String, Object>> byProvider = new LinkedHashMap<>();
        for (UserApiKey k : keys) {
            String prov = k.getProvider();
            if (!byProvider.containsKey(prov)) {
                byProvider.put(prov, new LinkedHashMap<>());
            }
            Map<String, Object> provStats = byProvider.get(prov);
            provStats.merge("requests", k.getRequestCount() != null ? k.getRequestCount() : 0L, (a, b) -> (Long) a + (Long) b);
            provStats.merge("cost", k.getEstimatedCost() != null ? k.getEstimatedCost() : 0.0, (a, b) -> (Double) a + (Double) b);
            provStats.merge("keyCount", 1L, (a, b) -> (Long) a + (Long) b);
        }
        stats.put("byProvider", byProvider);

        return ResponseEntity.ok(stats);
    }
}
