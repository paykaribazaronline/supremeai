package com.supremeai.security;

import com.supremeai.model.UserApiKey;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.security.EncryptionService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ApiKeyRotationService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyRotationService.class);

    private static final int DEFAULT_ROTATION_DAYS = 90;
    private static final int DEFAULT_MAX_KEYS_PER_PROVIDER = 5;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    @Autowired
    private UserApiKeyRepository userApiKeyRepository;

    @Autowired
    private com.supremeai.repository.APIHealthReportRepository healthReportRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private com.supremeai.service.ProviderTypeRegistry providerTypeRegistry;

    public String getDecryptedApiKey(UserApiKey key) {
        if (key == null || key.getApiKey() == null) return null;
        return encryptionService.decrypt(key.getApiKey());
    }

    public int getRotationDaysForKey(String provider) {
        if (provider == null) return DEFAULT_ROTATION_DAYS;
        try {
            com.supremeai.model.ProviderTypeConfig typeConfig = providerTypeRegistry.getTypeConfig(provider);
            if (typeConfig != null && typeConfig.getExtraConfig() != null) {
                Object days = typeConfig.getExtraConfig().get("rotationDays");
                if (days instanceof Number) return ((Number) days).intValue();
            }
        } catch (Exception e) {
            log.debug("Could not load rotation days from Firestore for {}: {}", provider, e.getMessage());
        }
        return DEFAULT_ROTATION_DAYS;
    }

    public int getMaxKeysPerProvider(String provider) {
        if (provider == null) return DEFAULT_MAX_KEYS_PER_PROVIDER;
        try {
            com.supremeai.model.ProviderTypeConfig typeConfig = providerTypeRegistry.getTypeConfig(provider);
            if (typeConfig != null && typeConfig.getExtraConfig() != null) {
                Object max = typeConfig.getExtraConfig().get("maxKeys");
                if (max instanceof Number) return ((Number) max).intValue();
            }
        } catch (Exception e) {
            log.debug("Could not load maxKeys from Firestore for {}: {}", provider, e.getMessage());
        }
        return DEFAULT_MAX_KEYS_PER_PROVIDER;
    }

    public Map<String, Object> testApiKey(UserApiKey key) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", key.getId());
        result.put("provider", key.getProvider());

        String providerLower = key.getProvider() != null ? key.getProvider().toLowerCase() : "";

        String testEndpoint = null;
        String authMethod = "Bearer";
        try {
            com.supremeai.model.ProviderTypeConfig typeConfig = providerTypeRegistry.getTypeConfig(providerLower);
            if (typeConfig != null && typeConfig.getExtraConfig() != null) {
                Object endpoint = typeConfig.getExtraConfig().get("testEndpoint");
                if (endpoint instanceof String) testEndpoint = (String) endpoint;
                Object auth = typeConfig.getExtraConfig().get("authMethod");
                if (auth instanceof String) authMethod = (String) auth;
            }
        } catch (Exception e) {
            log.debug("Could not load test config from Firestore for {}: {}", providerLower, e.getMessage());
        }

        if (testEndpoint == null) {
            result.put("valid", true);
            result.put("message", "No test endpoint configured — skipping validation");
            return result;
        }

        String decryptedApiKey = encryptionService.decrypt(key.getApiKey());
        String url = testEndpoint;
        if ("QueryParam".equals(authMethod)) {
            url = url + decryptedApiKey;
        }

        try {
            Request.Builder requestBuilder = new Request.Builder().url(url).get();
            if ("Bearer".equals(authMethod)) {
                requestBuilder.header("Authorization", "Bearer " + decryptedApiKey);
            } else if ("x-api-key".equals(authMethod)) {
                requestBuilder.header("x-api-key", decryptedApiKey);
                requestBuilder.header("anthropic-version", "2023-06-01");
            }

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                boolean valid = response.isSuccessful();
                result.put("valid", valid);
                result.put("statusCode", response.code());
                result.put("message", valid ? "API key is valid and working" : "API key test failed - status " + response.code());
            }
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "Connection error: " + e.getMessage());
        }

        return result;
    }

    public Mono<UserApiKey> selectBestKey(String userId, String provider) {
        return userApiKeyRepository
                .findByUserIdAndStatus(userId, "active")
                .filter(k -> provider.equalsIgnoreCase(k.getProvider()))
                .filter(k -> !"error".equals(k.getStatus()))
                .sort(Comparator.comparingLong(k -> k.getRequestCount() != null ? k.getRequestCount() : 0L))
                .next();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void checkRotationDueKeys() {
        log.info("Running scheduled API key rotation check...");

        userApiKeyRepository.findAll()
                .flatMap(key -> {
                    if (!"active".equals(key.getStatus())) {
                        return Mono.empty();
                    }

                    int maxDays = getRotationDaysForKey(key.getProvider());
                    LocalDateTime maxAgeDate = LocalDateTime.now().minusDays(maxDays);

                    if ((key.getAddedAt() != null && key.getAddedAt().isBefore(maxAgeDate)) || key.needsRotation()) {
                        key.setStatus("rotation_due");
                        key.setRotationDueAt(LocalDateTime.now());
                        log.warn("Key {} for provider '{}' (user {}) is due for rotation. Marked rotation_due.",
                                key.getMaskedKey(), key.getProvider(), key.getUserId());
                        return userApiKeyRepository.save(key);
                    }
                    return Mono.empty();
                })
                .doOnTerminate(() -> log.info("Rotation check completed."))
                .subscribe();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void validateAllActiveKeys() {
        testAllKeysNow().subscribe();
    }

    public Mono<Void> testAllKeysNow() {
        log.info("Running API key validation and report generation...");

        return userApiKeyRepository.findAll().collectList()
            .flatMap(allKeys -> {
                if (allKeys == null || allKeys.isEmpty()) return Mono.empty();

                List<UserApiKey> activeKeys = allKeys.stream()
                        .filter(k -> "active".equals(k.getStatus()))
                        .collect(Collectors.toList());

                if (activeKeys.isEmpty()) {
                    log.info("No active keys to validate.");
                    return Mono.empty();
                }

                int concurrency = Math.min(10, activeKeys.size());
                AtomicInteger validCount = new AtomicInteger();
                List<Map<String, Object>> deadDetails = Collections.synchronizedList(new ArrayList<>());
                List<UserApiKey> rotationDue = allKeys.stream().filter(k -> "rotation_due".equals(k.getStatus())).collect(Collectors.toList());

                return Flux.fromIterable(activeKeys)
                        .flatMap(key ->
                                Mono.fromCallable(() -> {
                                            Map<String, Object> result = testApiKey(key);
                                            boolean isValid = Boolean.TRUE.equals(result.get("valid"));
                                            key.setLastTested(LocalDateTime.now());

                                            if (isValid) {
                                                validCount.incrementAndGet();
                                            } else {
                                                key.setStatus("error");
                                                Map<String, Object> detail = new HashMap<>();
                                                detail.put("id", key.getId());
                                                detail.put("label", key.getLabel());
                                                detail.put("provider", key.getProvider());
                                                detail.put("error", result.get("message"));
                                                deadDetails.add(detail);
                                            }
                                            return key;
                                        })
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .flatMap(k -> userApiKeyRepository.save(k)),
                                concurrency)
                        .then(Mono.defer(() -> {
                            String reportId = "report_" + LocalDateTime.now().toString().replace(":", "-");
                            com.supremeai.model.APIHealthReport report = new com.supremeai.model.APIHealthReport(
                                reportId, allKeys.size(), validCount.get(), deadDetails.size(), rotationDue.size()
                            );
                            report.setDeadKeyDetails(deadDetails);
                            return healthReportRepository.save(report);
                        }))
                        .doOnSuccess(r -> log.info("API Health Report generated: {}", r.getId()))
                        .then();
            });
    }

    public Mono<Map<String, Object>> getRotationStatus(String userId) {
        return userApiKeyRepository.findByUserId(userId).collectList()
                .map(keys -> {
                    long active = keys.stream().filter(k -> "active".equals(k.getStatus())).count();
                    long rotationDue = keys.stream().filter(k -> "rotation_due".equals(k.getStatus())).count();
                    long error = keys.stream().filter(k -> "error".equals(k.getStatus())).count();

                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("totalKeys", keys.size());
                    summary.put("active", active);
                    summary.put("rotationDue", rotationDue);
                    summary.put("error", error);
                    summary.put("providerConfigs", providerTypeRegistry.getAllTypes().keySet());
                    return summary;
                })
                .defaultIfEmpty(Map.of("totalKeys", 0));
    }
}
