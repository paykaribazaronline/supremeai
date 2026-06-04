package com.supremeai.service;

import com.supremeai.dto.ApiKeyCreateRequest;
import com.supremeai.model.ActivityLog;
import com.supremeai.model.UserApiKey;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.security.ApiKeyRotationService;
import com.supremeai.security.EncryptionService;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserApiKeyService {

  private static final Logger logger = LoggerFactory.getLogger(UserApiKeyService.class);

  @Autowired private UserApiKeyRepository userApiKeyRepository;

  @Autowired private ApiKeyRotationService rotationService;

  @Autowired private ActivityLogRepository activityLogRepository;

  @Autowired private EncryptionService encryptionService;

  @Autowired private ContextualAIRankingService contextualRankingService;

  public Mono<List<Map<String, Object>>> listKeys(String userId) {
    return userApiKeyRepository
        .findByUserId(userId)
        .collectList()
        .map(
            keys ->
                keys.stream()
                    .map(
                        key -> {
                          Map<String, Object> map = new LinkedHashMap<>();
                          map.put("id", key.getId());
                          map.put("provider", key.getProvider());
                          map.put("label", key.getLabel());
                          map.put("apiKey", key.getMaskedKey());
                          map.put("baseUrl", key.getBaseUrl());
                          map.put("models", key.getModels());
                          map.put("status", key.getStatus());
                          map.put(
                              "addedAt",
                              key.getAddedAt() != null ? key.getAddedAt().toString() : null);
                          map.put(
                              "lastTested",
                              key.getLastTested() != null ? key.getLastTested().toString() : null);
                          map.put("requestCount", key.getRequestCount());
                          map.put("needsRotation", key.needsRotation());
                          return map;
                        })
                    .collect(Collectors.toList()));
  }

  public Mono<UserApiKey> addKey(String userId, ApiKeyCreateRequest body) {
    UserApiKey key = new UserApiKey();
    key.setUserId(userId);
    key.setProvider(body.getProvider());
    key.setLabel(body.getLabel());
    key.setApiKey(encryptionService.encrypt(body.getApiKey()));
    key.setBaseUrl(body.getBaseUrl());

    if (body.getModels() != null) {
      key.setModels(body.getModels());
    }

    int rotationDays = rotationService.getRotationDaysForKey(key.getProvider());
    key.setRotationDueAt(LocalDateTime.now().plusDays(rotationDays));

    return userApiKeyRepository.save(key);
  }

  public Mono<UserApiKey> updateKey(String userId, String keyId, Map<String, Object> updates) {
    return userApiKeyRepository
        .findById(keyId)
        .flatMap(
            key -> {
              if (!userId.equals(key.getUserId())) {
                return Mono.error(new SecurityException("Access denied to API key"));
              }

              if (updates.containsKey("provider"))
                key.setProvider((String) updates.get("provider"));
              if (updates.containsKey("label")) key.setLabel((String) updates.get("label"));
              if (updates.containsKey("apiKey")) {
                key.setApiKey(encryptionService.encrypt((String) updates.get("apiKey")));
              }
              if (updates.containsKey("baseUrl")) key.setBaseUrl((String) updates.get("baseUrl"));
              if (updates.containsKey("status")) key.setStatus((String) updates.get("status"));
              if (updates.get("models") instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> models = (List<String>) updates.get("models");
                key.setModels(models);
              }

              return userApiKeyRepository.save(key);
            });
  }

  public Mono<Void> deleteKey(String userId, String keyId) {
    return userApiKeyRepository
        .findById(keyId)
        .flatMap(
            key -> {
              if (!userId.equals(key.getUserId())) {
                return Mono.error(new SecurityException("Access denied to API key"));
              }

              return userApiKeyRepository
                  .delete(key)
                  .then(recordActivity(userId, "DELETE_API_KEY", "Deleted API key: " + keyId));
            });
  }

  public Mono<List<String>> bulkDelete(String userId, List<String> keyIds) {
    return Flux.fromIterable(keyIds)
        .flatMap(id -> userApiKeyRepository.findById(id))
        .filter(key -> userId.equals(key.getUserId()))
        .flatMap(key -> userApiKeyRepository.delete(key).thenReturn(key.getId()))
        .collectList()
        .flatMap(
            deletedIds ->
                recordActivity(
                        userId,
                        "BULK_DELETE_API_KEYS",
                        "Bulk deleted " + deletedIds.size() + " API keys")
                    .thenReturn(deletedIds));
  }

  public Mono<Map<String, Object>> testKey(String userId, String keyId) {
    return userApiKeyRepository
        .findById(keyId)
        .flatMap(
            key -> {
              if (!userId.equals(key.getUserId())) {
                return Mono.error(new SecurityException("Access denied"));
              }

              Map<String, Object> testResult = rotationService.testApiKey(key);
              key.setLastTested(LocalDateTime.now());
              boolean isValid = Boolean.TRUE.equals(testResult.get("valid"));
              key.setStatus(isValid ? "active" : "error");

              return userApiKeyRepository
                  .save(key)
                  .map(
                      saved -> {
                        contextualRankingService.recordTaskOutcome(
                            key.getProvider(),
                            ContextualAIRankingService.TaskType.QUESTION_ANSWERING,
                            isValid,
                            100L,
                            isValid ? 4.0 : 1.0);
                        return testResult;
                      });
            });
  }

  public Mono<Map<String, Object>> getUsageStats(String userId) {
    return userApiKeyRepository
        .findByUserId(userId)
        .collectList()
        .map(
            keys -> {
              long totalRequests =
                  keys.stream()
                      .mapToLong(k -> k.getRequestCount() != null ? k.getRequestCount() : 0L)
                      .sum();
              double totalCost =
                  keys.stream()
                      .mapToDouble(k -> k.getEstimatedCost() != null ? k.getEstimatedCost() : 0.0)
                      .sum();
              long activeKeys = keys.stream().filter(k -> "active".equals(k.getStatus())).count();
              long providerCount = keys.stream().map(UserApiKey::getProvider).distinct().count();

              Map<String, Object> stats = new LinkedHashMap<>();
              stats.put("totalRequests", totalRequests);
              stats.put("activeKeys", activeKeys);
              stats.put("totalCost", Math.round(totalCost * 100.0) / 100.0);
              stats.put("providers", providerCount);
              stats.put("totalKeys", keys.size());

              Map<String, Map<String, Object>> byProvider = new LinkedHashMap<>();
              for (UserApiKey k : keys) {
                String prov = k.getProvider();
                byProvider.computeIfAbsent(prov, p -> new LinkedHashMap<>());
                Map<String, Object> provStats = byProvider.get(prov);
                provStats.merge(
                    "requests",
                    k.getRequestCount() != null ? k.getRequestCount() : 0L,
                    (a, b) -> (Long) a + (Long) b);
                provStats.merge(
                    "cost",
                    k.getEstimatedCost() != null ? k.getEstimatedCost() : 0.0,
                    (a, b) -> (Double) a + (Double) b);
                provStats.merge("keyCount", 1L, (a, b) -> (Long) a + (Long) b);
              }
              stats.put("byProvider", byProvider);
              return stats;
            });
  }

  public Mono<Map<String, Object>> testRequest(
      String userId,
      String keyId,
      String method,
      String endpoint,
      Map<String, Object> headers,
      Object body) {
    return userApiKeyRepository
        .findById(keyId)
        .flatMap(
            key -> {
              if (!userId.equals(key.getUserId())) {
                return Mono.error(new SecurityException("Access denied"));
              }

              return Mono.fromCallable(
                  () -> {
                    try {
                      String targetUrl =
                          endpoint.startsWith("http") ? endpoint : key.getBaseUrl() + endpoint;
                      HttpHeaders httpHeaders = new HttpHeaders();
                      if (headers != null) {
                        headers.forEach((k, v) -> httpHeaders.add(k, v.toString()));
                      }

                      String decryptedApiKey = encryptionService.decrypt(key.getApiKey());
                      httpHeaders.add("Authorization", "Bearer " + decryptedApiKey);

                      HttpEntity<Object> requestEntity =
                          new HttpEntity<>(
                              (!"GET".equals(method) && !"DELETE".equals(method)) ? body : null,
                              httpHeaders);

                      RestTemplate restTemplate = new RestTemplate();
                      ResponseEntity<String> responseEntity =
                          restTemplate.exchange(
                              targetUrl, HttpMethod.valueOf(method), requestEntity, String.class);

                      int statusCode = responseEntity.getStatusCode().value();
                      boolean success = statusCode >= 200 && statusCode < 300;
                      contextualRankingService.recordTaskOutcome(
                          key.getProvider(),
                          ContextualAIRankingService.TaskType.QUESTION_ANSWERING,
                          success,
                          100L,
                          success ? 4.0 : 1.0);

                      Map<String, Object> result = new HashMap<>();
                      result.put("status", statusCode);
                      result.put("statusText", responseEntity.getStatusCode().toString());
                      result.put("body", tryParseJson(responseEntity.getBody()));
                      return result;
                    } catch (Exception e) {
                      contextualRankingService.recordTaskOutcome(
                          key.getProvider(),
                          ContextualAIRankingService.TaskType.QUESTION_ANSWERING,
                          false,
                          100L,
                          1.0);
                      throw new RuntimeException("Request failed: " + e.getMessage());
                    }
                  });
            });
  }

  public Mono<Void> testAllKeys() {
    return rotationService.testAllKeysNow().then();
  }

  @Autowired private com.supremeai.repository.APIHealthReportRepository healthReportRepository;

  public Mono<List<com.supremeai.model.APIHealthReport>> getHealthReports() {
    return healthReportRepository
        .findAll()
        .collectList()
        .map(
            reports -> {
              reports.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
              return reports;
            });
  }

  public Mono<com.supremeai.model.APIHealthReport> getHealthReport(String id) {
    return healthReportRepository.findById(id);
  }

  private Object tryParseJson(String str) {
    if (str == null) return null;
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().readValue(str, Object.class);
    } catch (Exception e) {
      return str;
    }
  }

  private Mono<Void> recordActivity(String userId, String action, String details) {
    ActivityLog logItem = new ActivityLog();
    logItem.setUser(userId);
    logItem.setAction(action);
    logItem.setCategory("API_KEY_MANAGEMENT");
    logItem.setSeverity("INFO");
    logItem.setOutcome("SUCCESS");
    logItem.setDetails(details);
    return activityLogRepository.save(logItem).then();
  }
}
