package com.supremeai.controller;

import com.supremeai.model.UserApiKey;
import com.supremeai.model.ActivityLog;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.security.ApiKeyRotationService;
import com.supremeai.security.EncryptionService;
import com.supremeai.service.AIRankingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

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
public class APIKeyController {

    @Autowired
    private UserApiKeyRepository userApiKeyRepository;

    @Autowired
    private ApiKeyRotationService rotationService;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private AIRankingService aiRankingService;

    @Autowired
    private EncryptionService encryptionService;

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
     * API key is encrypted before storing.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addKey(@RequestBody Map<String, Object> body) {
        String userId = getCurrentUserId();

        UserApiKey key = new UserApiKey();
        key.setUserId(userId);
        key.setProvider((String) body.get("provider"));
        key.setLabel((String) body.get("label"));

        // Encrypt API key before storing
        String plainApiKey = (String) body.get("apiKey");
        key.setApiKey(encryptionService.encrypt(plainApiKey));

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
        if (body.containsKey("apiKey")) {
            String plainApiKey = (String) body.get("apiKey");
            key.setApiKey(encryptionService.encrypt(plainApiKey));
        }
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

                // Log admin action
        ActivityLog log = new ActivityLog();
        log.setUser(userId);
        log.setAction("DELETE_API_KEY");
        log.setCategory("API_KEY_MANAGEMENT");
        log.setSeverity("INFO");
        log.setOutcome("SUCCESS");
        log.setDetails("Deleted API key: " + id);
        activityLogRepository.save(log).block();

        return ResponseEntity.ok(Map.of("status", "success", "message", "API key removed"));
    }

    /**
     * DELETE /api/apikeys/bulk - Bulk delete API keys.
     */
    @DeleteMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkDeleteKeys(@RequestBody Map<String, Object> body) {
        String userId = getCurrentUserId();
        List<String> keyIds = (List<String>) body.get("keyIds");
        if (keyIds == null || keyIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No key IDs provided"));
        }

        int deletedCount = 0;
        List<String> deletedIds = new ArrayList<>();
        for (String id : keyIds) {
            UserApiKey key = userApiKeyRepository.findById(id).block();
            if (key != null && userId.equals(key.getUserId())) {
                userApiKeyRepository.delete(key).block();
                deletedIds.add(id);
                deletedCount++;
            }
        }

                // Log bulk admin action
        ActivityLog log = new ActivityLog();
        log.setUser(userId);
        log.setAction("BULK_DELETE_API_KEYS");
        log.setCategory("API_KEY_MANAGEMENT");
        log.setSeverity("INFO");
        log.setOutcome("SUCCESS");
        log.setDetails("Bulk deleted " + deletedCount + " API keys: " + String.join(", ", deletedIds));
        activityLogRepository.save(log).block();

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "deletedCount", deletedCount,
            "deletedIds", deletedIds,
            "message", "Bulk delete completed"
        ));
    }

    /**
     * POST /api/apikeys/bulk/regenerate - Bulk regenerate API keys.
     * Note: For external providers, this updates rotation date; for internal keys, generates new key.
     */
    @PostMapping("/bulk/regenerate")
    public ResponseEntity<Map<String, Object>> bulkRegenerateKeys(@RequestBody Map<String, Object> body) {
        String userId = getCurrentUserId();
        List<String> keyIds = (List<String>) body.get("keyIds");
        if (keyIds == null || keyIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No key IDs provided"));
        }

        List<Map<String, Object>> results = new ArrayList<>();
        List<String> regeneratedIds = new ArrayList<>();
        for (String id : keyIds) {
            UserApiKey key = userApiKeyRepository.findById(id).block();
            if (key != null && userId.equals(key.getUserId())) {
                // Regenerate key (for external providers, update rotation date; for internal, generate new key)
                int rotationDays = rotationService.getRotationDaysForKey(key.getProvider());
                key.setRotationDueAt(LocalDateTime.now().plusDays(rotationDays));
                key.setStatus("active");
                userApiKeyRepository.save(key).block();

                Map<String, Object> result = new HashMap<>();
                result.put("id", key.getId());
                result.put("label", key.getLabel());
                result.put("status", "success");
                result.put("newMaskedKey", key.getMaskedKey());
                results.add(result);
                regeneratedIds.add(id);
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("id", id);
                result.put("status", "not_found");
                results.add(result);
            }
        }

                // Log bulk admin action
        ActivityLog log = new ActivityLog();
        log.setUser(userId);
        log.setAction("BULK_REGENERATE_API_KEYS");
        log.setCategory("API_KEY_MANAGEMENT");
        log.setSeverity("INFO");
        log.setOutcome("SUCCESS");
        log.setDetails("Bulk regenerated " + regeneratedIds.size() + " API keys: " + String.join(", ", regeneratedIds));
        activityLogRepository.save(log).block();

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "results", results,
            "regeneratedCount", regeneratedIds.size(),
            "message", "Bulk regenerate completed"
        ));
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
        boolean isValid = testResult.get("valid").equals(true);
        key.setStatus(isValid ? "active" : "error");
        userApiKeyRepository.save(key).block();

        // Record provider ranking
        aiRankingService.recordRequest(key.getProvider(), isValid);

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

    /**
     * POST /api/apikeys/test-request - Test API request using selected key
     */
    @PostMapping("/test-request")
    public ResponseEntity<Map<String, Object>> testRequest(@RequestBody Map<String, Object> body) {
        String userId = getCurrentUserId();
        String keyId = (String) body.get("keyId");
        String method = (String) body.get("method");
        String endpoint = (String) body.get("endpoint");
        @SuppressWarnings("unchecked")
        Map<String, Object> headers = (Map<String, Object>) body.getOrDefault("headers", new HashMap<String, Object>());
        Object requestBody = body.get("body");

        UserApiKey key = userApiKeyRepository.findById(keyId).block();
        if (key == null || !userId.equals(key.getUserId())) {
            return ResponseEntity.status(404).body(Map.of("error", "API key not found"));
        }

        try {
            // Build target URL
            String targetUrl = endpoint.startsWith("http") ? endpoint : key.getBaseUrl() + endpoint;

                        // Create request
                        HttpHeaders httpHeaders = new HttpHeaders();
                        headers.forEach((k, v) -> httpHeaders.add(k, v.toString()));

                        // Decrypt API key before using
                        String decryptedApiKey = encryptionService.decrypt(key.getApiKey());
                        httpHeaders.add("Authorization", "Bearer " + decryptedApiKey);
            
                        HttpEntity<Object> requestEntity = new HttpEntity<>(
                                (!"GET".equals(method) && !"DELETE".equals(method)) ? requestBody : null, 
                                httpHeaders);
            
                        RestTemplate restTemplate = new RestTemplate();
                        ResponseEntity<String> responseEntity = restTemplate.exchange(
                                targetUrl,
                                HttpMethod.valueOf(method),
                                requestEntity,
                                String.class);

                        Map<String, Object> responseHeaders = new HashMap<>();
                        responseEntity.getHeaders().forEach((k, v) -> 
                                responseHeaders.put(k, String.join(", ", v)));
            
                        String responseBody = responseEntity.getBody();

                        Map<String, Object> result = new HashMap<>();
                        int statusCode = responseEntity.getStatusCode().value();
                        result.put("status", statusCode);
                        result.put("statusText", responseEntity.getStatusCode().toString());
                        result.put("headers", responseHeaders);
                        result.put("body", responseBody != null ? tryParseJson(responseBody) : null);

                        // Record provider ranking
                        boolean success = statusCode >= 200 && statusCode < 300;
                        aiRankingService.recordRequest(key.getProvider(), success);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Record failure
            aiRankingService.recordRequest(key.getProvider(), false);
            return ResponseEntity.status(500).body(Map.of("error", "Request failed: " + e.getMessage()));
        }
    }

    private Object tryParseJson(String str) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(str, Object.class);
        } catch (Exception e) {
            return str;
        }
    }
}
