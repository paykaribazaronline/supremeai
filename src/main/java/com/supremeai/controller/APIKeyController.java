package com.supremeai.controller;

import com.supremeai.dto.ApiKeyCreateRequest;
import com.supremeai.model.APIHealthReport;
import com.supremeai.service.UserApiKeyService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST API for managing per-user API keys. Connects the frontend APIKeysManager.tsx to the backend.
 * Logic is delegated to UserApiKeyService.
 */
@RestController
@RequestMapping("/api/apikeys")
public class APIKeyController {

  @Autowired private UserApiKeyService apiKeyService;

  private static ResponseEntity<Map<String, Object>> forbidden(String msg) {
    Map<String, Object> err = new HashMap<>();
    err.put("error", msg);
    return ResponseEntity.status(403).body(err);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public Mono<ResponseEntity<List<Map<String, Object>>>> listKeys(Authentication auth) {
    return apiKeyService.listKeys(auth.getName()).map(ResponseEntity::ok);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public Mono<ResponseEntity<Map<String, Object>>> addKey(
      @Valid @RequestBody ApiKeyCreateRequest body, Authentication auth) {
    return apiKeyService
        .addKey(auth.getName(), body)
        .map(
            saved ->
                ResponseEntity.ok(
                    Map.of(
                        "status", "success",
                        "id", saved.getId(),
                        "message", "API key added successfully")));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public Mono<ResponseEntity<Map<String, Object>>> updateKey(
      @PathVariable String id, @RequestBody Map<String, Object> body, Authentication auth) {
    return apiKeyService
        .updateKey(auth.getName(), id, body)
        .map(
            saved ->
                ResponseEntity.ok(
                    Map.<String, Object>of("status", "success", "message", "API key updated")))
        .onErrorResume(SecurityException.class, e -> Mono.just(forbidden(e.getMessage())));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public Mono<ResponseEntity<Map<String, Object>>> deleteKey(
      @PathVariable String id, Authentication auth) {
    return apiKeyService
        .deleteKey(auth.getName(), id)
        .then(
            Mono.just(
                ResponseEntity.ok(
                    Map.<String, Object>of("status", "success", "message", "API key removed"))))
        .onErrorResume(SecurityException.class, e -> Mono.just(forbidden(e.getMessage())));
  }

  @DeleteMapping("/bulk")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public Mono<ResponseEntity<Map<String, Object>>> bulkDeleteKeys(
      @RequestBody Map<String, Object> body, Authentication auth) {
    @SuppressWarnings("unchecked")
    List<String> keyIds = (List<String>) body.get("keyIds");
    if (keyIds == null || keyIds.isEmpty()) {
      return Mono.just(
          ResponseEntity.badRequest().body(Map.<String, Object>of("error", "No key IDs provided")));
    }

    return apiKeyService
        .bulkDelete(auth.getName(), keyIds)
        .map(
            deletedIds ->
                ResponseEntity.ok(
                    Map.<String, Object>of(
                        "status",
                        "success",
                        "deletedCount",
                        deletedIds.size(),
                        "deletedIds",
                        deletedIds,
                        "message",
                        "Bulk delete completed")));
  }

  @PostMapping("/{id}/test")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public Mono<ResponseEntity<Map<String, Object>>> testKey(
      @PathVariable String id, Authentication auth) {
    return apiKeyService
        .testKey(auth.getName(), id)
        .map(ResponseEntity::ok)
        .onErrorResume(SecurityException.class, e -> Mono.just(forbidden(e.getMessage())));
  }

  @GetMapping("/usage")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public Mono<ResponseEntity<Map<String, Object>>> getUsage(Authentication auth) {
    return apiKeyService.getUsageStats(auth.getName()).map(ResponseEntity::ok);
  }

  @PostMapping("/test-request")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public Mono<ResponseEntity<Map<String, Object>>> testRequest(
      @RequestBody Map<String, Object> body, Authentication auth) {
    String keyId = (String) body.get("keyId");
    String method = (String) body.get("method");
    String endpoint = (String) body.get("endpoint");
    @SuppressWarnings("unchecked")
    Map<String, Object> headers = (Map<String, Object>) body.getOrDefault("headers", Map.of());
    Object requestBody = body.get("body");

    return apiKeyService
        .testRequest(auth.getName(), keyId, method, endpoint, headers, requestBody)
        .map(ResponseEntity::ok)
        .onErrorResume(SecurityException.class, e -> Mono.just(forbidden(e.getMessage())))
        .onErrorResume(
            Exception.class,
            e ->
                Mono.just(
                    ResponseEntity.status(500)
                        .body(Map.<String, Object>of("error", e.getMessage()))));
  }

  @PostMapping("/test-all")
  @PreAuthorize("hasRole('ADMIN')")
  public Mono<ResponseEntity<Map<String, Object>>> testAllKeys(Authentication auth) {
    return apiKeyService
        .testAllKeys()
        .then(
            Mono.just(
                ResponseEntity.ok(
                    Map.<String, Object>of(
                        "status", "success", "message", "Full API key validation triggered"))));
  }

  @GetMapping("/reports")
  @PreAuthorize("hasRole('ADMIN')")
  public Mono<ResponseEntity<List<APIHealthReport>>> getReports() {
    return apiKeyService.getHealthReports().map(ResponseEntity::ok);
  }

  @GetMapping("/reports/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Mono<ResponseEntity<APIHealthReport>> getReport(@PathVariable String id) {
    return apiKeyService
        .getHealthReport(id)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }
}
