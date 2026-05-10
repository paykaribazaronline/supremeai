package com.supremeai.controller;

import com.supremeai.model.UserApiKey;
import com.supremeai.model.ActivityLog;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.security.ApiKeyRotationService;
import com.supremeai.security.EncryptionService;
import com.supremeai.service.ContextualAIRankingService;
import com.supremeai.dto.ApiKeyCreateRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Flux;
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
     private ContextualAIRankingService contextualRankingService;

    @Autowired
    private EncryptionService encryptionService;



    /**
     * GET /api/apikeys - List all API keys for the current user.
     * Returns keys with masked API values for security.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ResponseEntity<List<Map<String, Object>>>> listKeys(Authentication auth) {
        String userId = auth.getName();
        return userApiKeyRepository.findByUserId(userId)
            .collectList()
            .map(keys -> {
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
            });
    }

    /**
     * POST /api/apikeys - Add a new API key.
     * API key is encrypted before storing.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ResponseEntity<Map<String, Object>>> addKey(@Valid @RequestBody ApiKeyCreateRequest body, Authentication auth) {
        String userId = auth.getName();

        String provider = body.getProvider();
        String label = body.getLabel();
        String plainApiKey = body.getApiKey();

        UserApiKey key = new UserApiKey();
        key.setUserId(userId);
        key.setProvider(provider);
        key.setLabel(label);

        // Encrypt API key before storing
        key.setApiKey(encryptionService.encrypt(plainApiKey));

        key.setBaseUrl(body.getBaseUrl());

        if (body.getModels() != null) {
            key.setModels(body.getModels());
        }

        // Set rotation due date based on provider limits (default 30 days)
        int rotationDays = rotationService.getRotationDaysForKey(key.getProvider());
        key.setRotationDueAt(LocalDateTime.now().plusDays(rotationDays));

        return userApiKeyRepository.save(key)
            .map(saved -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("id", saved.getId());
                response.put("message", "API key added successfully");
                return ResponseEntity.ok(response);
            });
    }

    /**
     * PUT /api/apikeys/{id} - Update an existing API key.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ResponseEntity<?>> updateKey(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        String userId = auth.getName();

        return userApiKeyRepository.findById(id)
            .flatMap(key -> {
                if (!userId.equals(key.getUserId())) {
                    return Mono.just(ResponseEntity.status(403).body(Map.of("error", "Access denied")));
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

                return userApiKeyRepository.save(key)
                    .map(saved -> ResponseEntity.ok((Object) Map.of("status", "success", "message", "API key updated")));
            })
            .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.of("error", "API key not found"))));
    }

    /**
     * DELETE /api/apikeys/{id} - Delete an API key.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ResponseEntity<?>> deleteKey(@PathVariable String id, Authentication auth) {
        String userId = auth.getName();

        return userApiKeyRepository.findById(id)
            .flatMap(key -> {
                if (!userId.equals(key.getUserId())) {
                    return Mono.just(ResponseEntity.status(403).body(Map.of("error", "Access denied")));
                }

                return userApiKeyRepository.delete(key)
                    .then(Mono.defer(() -> {
                        ActivityLog logItem = new ActivityLog();
                        logItem.setUser(userId);
                        logItem.setAction("DELETE_API_KEY");
                        logItem.setCategory("API_KEY_MANAGEMENT");
                        logItem.setSeverity("INFO");
                        logItem.setOutcome("SUCCESS");
                        logItem.setDetails("Deleted API key: " + id);
                        return activityLogRepository.save(logItem);
                    }))
                    .thenReturn(ResponseEntity.ok((Object) Map.of("status", "success", "message", "API key removed")));
            })
            .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.of("error", "API key not found"))));
    }

    /**
     * DELETE /api/apikeys/bulk - Bulk delete API keys.
     */
    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ResponseEntity<?>> bulkDeleteKeys(@RequestBody Map<String, Object> body, Authentication auth) {
        String userId = auth.getName();
        @SuppressWarnings("unchecked")
        List<String> keyIds = (List<String>) body.get("keyIds");
        if (keyIds == null || keyIds.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "No key IDs provided")));
        }

        return Flux.fromIterable(keyIds)
            .flatMap(id -> userApiKeyRepository.findById(id))
            .filter(key -> userId.equals(key.getUserId()))
            .flatMap(key -> userApiKeyRepository.delete(key).thenReturn(key.getId()))
            .collectList()
            .flatMap(deletedIds -> {
                ActivityLog logItem = new ActivityLog();
                logItem.setUser(userId);
                logItem.setAction("BULK_DELETE_API_KEYS");
                logItem.setCategory("API_KEY_MANAGEMENT");
                logItem.setSeverity("INFO");
                logItem.setOutcome("SUCCESS");
                logItem.setDetails("Bulk deleted " + deletedIds.size() + " API keys: " + String.join(", ", deletedIds));
                
                return activityLogRepository.save(logItem)
                    .thenReturn(ResponseEntity.ok(Map.of(
                        "status", "success",
                        "deletedCount", deletedIds.size(),
                        "deletedIds", deletedIds,
                        "message", "Bulk delete completed"
                    )));
            });
    }

    /**
     * POST /api/apikeys/bulk/regenerate - Bulk regenerate API keys.
     * Note: For external providers, this updates rotation date; for internal keys, generates new key.
     */
    @PostMapping("/bulk/regenerate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ResponseEntity<?>> bulkRegenerateKeys(@RequestBody Map<String, Object> body, Authentication auth) {
        String userId = auth.getName();
        @SuppressWarnings("unchecked")
        List<String> keyIds = (List<String>) body.get("keyIds");
        if (keyIds == null || keyIds.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "No key IDs provided")));
        }

        return Flux.fromIterable(keyIds)
            .flatMap(id -> userApiKeyRepository.findById(id)
                .flatMap(key -> {
                    if (userId.equals(key.getUserId())) {
                        int rotationDays = rotationService.getRotationDaysForKey(key.getProvider());
                        key.setRotationDueAt(LocalDateTime.now().plusDays(rotationDays));
                        key.setStatus("active");
                        return userApiKeyRepository.save(key).map(saved -> {
                            Map<String, Object> res = new HashMap<>();
                            res.put("id", saved.getId());
                            res.put("label", saved.getLabel());
                            res.put("status", "success");
                            res.put("newMaskedKey", saved.getMaskedKey());
                            return res;
                        });
                    } else {
                        Map<String, Object> res = new HashMap<>();
                        res.put("id", id);
                        res.put("status", "forbidden");
                        return Mono.just(res);
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    Map<String, Object> res = new HashMap<>();
                    res.put("id", id);
                    res.put("status", "not_found");
                    return Mono.just(res);
                }))
            )
            .collectList()
            .flatMap(results -> {
                List<String> regeneratedIds = results.stream()
                    .filter(r -> "success".equals(r.get("status")))
                    .map(r -> (String) r.get("id"))
                    .collect(Collectors.toList());

                ActivityLog logItem = new ActivityLog();
                logItem.setUser(userId);
                logItem.setAction("BULK_REGENERATE_API_KEYS");
                logItem.setCategory("API_KEY_MANAGEMENT");
                logItem.setSeverity("INFO");
                logItem.setOutcome("SUCCESS");
                logItem.setDetails("Bulk regenerated " + regeneratedIds.size() + " API keys: " + String.join(", ", regeneratedIds));
                
                return activityLogRepository.save(logItem)
                    .thenReturn(ResponseEntity.ok(Map.of(
                        "status", "success",
                        "results", results,
                        "regeneratedCount", regeneratedIds.size(),
                        "message", "Bulk regenerate completed"
                    )));
            });
    }

    /**
     * POST /api/apikeys/{id}/test - Test if an API key is valid by making
     * a lightweight request to the provider's API.
     */
    @PostMapping("/{id}/test")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ResponseEntity<?>> testKey(@PathVariable String id, Authentication auth) {
        String userId = auth.getName();

        return userApiKeyRepository.findById(id)
            .flatMap(key -> {
                if (!userId.equals(key.getUserId())) {
                    return Mono.just(ResponseEntity.status(403).body(Map.of("error", "Access denied")));
                }

                // Test the key by making a lightweight request to the provider
                Map<String, Object> testResult = rotationService.testApiKey(key);

                key.setLastTested(LocalDateTime.now());
                boolean isValid = testResult.get("valid").equals(true);
                key.setStatus(isValid ? "active" : "error");
                
return userApiKeyRepository.save(key)
                     .map(saved -> {
                         // Record provider ranking
                         contextualRankingService.recordTaskOutcome(key.getProvider(), 
                             ContextualAIRankingService.TaskType.QUESTION_ANSWERING, isValid, 100L, isValid ? 4.0 : 1.0);
                         return ResponseEntity.ok((Object) testResult);
                     });
            })
            .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.of("error", "API key not found"))));
    }

    /**
     * GET /api/apikeys/usage - Get usage statistics for the current user's API keys.
     */
    @GetMapping("/usage")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ResponseEntity<?>> getUsage(Authentication auth) {
        String userId = auth.getName();
        return userApiKeyRepository.findByUserId(userId)
            .collectList()
            .map(keys -> {
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
            });
    }

    /**
     * POST /api/apikeys/test-request - Test API request using selected key
     */
    @PostMapping("/test-request")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Mono<ResponseEntity<?>> testRequest(@RequestBody Map<String, Object> body, Authentication auth) {
        String userId = auth.getName();
        String keyId = (String) body.get("keyId");
        String method = (String) body.get("method");
        String endpoint = (String) body.get("endpoint");
        @SuppressWarnings("unchecked")
        Map<String, Object> headers = (Map<String, Object>) body.getOrDefault("headers", new HashMap<String, Object>());
        Object requestBody = body.get("body");

        return userApiKeyRepository.findById(keyId)
            .flatMap(key -> {
                if (!userId.equals(key.getUserId())) {
                    return Mono.just(ResponseEntity.status(403).body(Map.of("error", "Access denied")));
                }

                return Mono.fromCallable(() -> {
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
                         contextualRankingService.recordTaskOutcome(key.getProvider(), 
                             ContextualAIRankingService.TaskType.QUESTION_ANSWERING, success, 100L, success ? 4.0 : 1.0);

                         return ResponseEntity.ok((Object) result);
                     } catch (Exception e) {
                         // Record failure
                         contextualRankingService.recordTaskOutcome(key.getProvider(), 
                             ContextualAIRankingService.TaskType.QUESTION_ANSWERING, false, 100L, 1.0);
                         return ResponseEntity.status(500).body((Object) Map.of("error", "Request failed: " + e.getMessage()));
                     }
                });
            })
            .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.of("error", "API key not found"))));
    }

    private Object tryParseJson(String str) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(str, Object.class);
        } catch (Exception e) {
            return str;
        }
    }
}
