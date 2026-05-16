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

/**
 * Service for managing API key rotation within provider-allowed limits.
 *
 * Features:
 * - Configurable rotation periods per provider (respects each provider's terms)
 * - Automatic detection of keys that need rotation
 * - Key validation/testing against provider endpoints
 * - Smart key selection: picks the least-used active key for a provider
 */
@Service
public class ApiKeyRotationService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyRotationService.class);

    /**
     * Provider-specific rotation configuration.
     * These values respect each provider's documented terms of service.
     */
    private static final Map<String, ProviderConfig> PROVIDER_CONFIGS = Map.ofEntries(
            Map.entry("openai", new ProviderConfig("https://api.openai.com/v1/models", "Bearer", 90, 5)),
            Map.entry("google ai", new ProviderConfig("https://generativelanguage.googleapis.com/v1beta/models?key=", "QueryParam", 90, 10)),
            Map.entry("anthropic", new ProviderConfig("https://api.anthropic.com/v1/models", "x-api-key", 90, 5)),
            Map.entry("groq", new ProviderConfig("https://api.groq.com/openai/v1/models", "Bearer", 60, 3)),
            Map.entry("mistral", new ProviderConfig("https://api.mistral.ai/v1/models", "Bearer", 90, 5)),
            Map.entry("deepseek", new ProviderConfig("https://api.deepseek.com/v1/models", "Bearer", 90, 5)),
            Map.entry("xai", new ProviderConfig("https://api.x.ai/v1/models", "Bearer", 90, 3)),
            Map.entry("openrouter", new ProviderConfig("https://openrouter.ai/api/v1/models", "Bearer", 90, 10)),
            Map.entry("together ai", new ProviderConfig("https://api.together.xyz/v1/models", "Bearer", 90, 5)),
            Map.entry("fireworks ai", new ProviderConfig("https://api.fireworks.ai/inference/v1/models", "Bearer", 90, 5)),
            Map.entry("cohere", new ProviderConfig("https://api.cohere.ai/v1/models", "Bearer", 90, 5))
    );

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

    /**
     * Get the decrypted API key string from a UserApiKey object.
     */
    public String getDecryptedApiKey(UserApiKey key) {
        if (key == null || key.getApiKey() == null) return null;
        return encryptionService.decrypt(key.getApiKey());
    }

    /**
     * Get the recommended rotation period in days for a given provider.
     */
    public int getRotationDaysForKey(String provider) {
        if (provider == null) return DEFAULT_ROTATION_DAYS;
        ProviderConfig config = PROVIDER_CONFIGS.get(provider.toLowerCase());
        return config != null ? config.rotationDays : DEFAULT_ROTATION_DAYS;
    }

    /**
     * Get the max allowed keys per provider for a user.
     */
    public int getMaxKeysPerProvider(String provider) {
        if (provider == null) return DEFAULT_MAX_KEYS_PER_PROVIDER;
        ProviderConfig config = PROVIDER_CONFIGS.get(provider.toLowerCase());
        return config != null ? config.maxKeys : DEFAULT_MAX_KEYS_PER_PROVIDER;
    }

    /**
     * Test if an API key is valid by making a lightweight request to the provider.
     */
    public Map<String, Object> testApiKey(UserApiKey key) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", key.getId());
        result.put("provider", key.getProvider());

        String providerLower = key.getProvider() != null ? key.getProvider().toLowerCase() : "";
        ProviderConfig config = PROVIDER_CONFIGS.get(providerLower);

        if (config == null) {
            // Unknown provider - we can't test, assume valid
            result.put("valid", true);
            result.put("message", "Unknown provider - cannot validate automatically");
            return result;
        }

        // Decrypt API key before using
        String decryptedApiKey = encryptionService.decrypt(key.getApiKey());

        // Handle Google AI which uses query param auth
        String url = config.testEndpoint;
        if ("QueryParam".equals(config.authMethod)) {
            url = url + decryptedApiKey;
        }

        try {
            Request.Builder requestBuilder = new Request.Builder().url(url).get();

            if ("Bearer".equals(config.authMethod)) {
                requestBuilder.header("Authorization", "Bearer " + decryptedApiKey);
            } else if ("x-api-key".equals(config.authMethod)) {
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

    /**
     * Find the best API key to use for a given provider and user reactively.
     * Picks the active key with the lowest usage count.
     */
    public Mono<UserApiKey> selectBestKey(String userId, String provider) {
        return userApiKeyRepository
                .findByUserIdAndStatus(userId, "active")
                .filter(k -> provider.equalsIgnoreCase(k.getProvider()))
                .filter(k -> !"error".equals(k.getStatus()))
                .sort(Comparator.comparingLong(k -> k.getRequestCount() != null ? k.getRequestCount() : 0L))
                .next();
    }

    /**
     * Scheduled check (daily at 2 AM) for keys that need rotation.
     * Marks keys as needing attention without blocking the thread.
     */
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

    /**
     * Test all active keys and mark any that fail validation.
     * Refactored to use reactive parallel processing (concurrency 10) for performance.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void validateAllActiveKeys() {
        testAllKeysNow().subscribe();
    }

    /**
     * Test all active keys and generate a health report.
     * Returns a Mono that completes when the report is saved.
     */
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

    /**
     * Get rotation status summary for a user reactively.
     */
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
                    summary.put("providerConfigs", PROVIDER_CONFIGS.keySet());
                    return summary;
                })
                .defaultIfEmpty(Map.of("totalKeys", 0));
    }

    /**
     * Internal provider configuration.
     */
    private static class ProviderConfig {
        final String testEndpoint;
        final String authMethod;
        final int rotationDays;
        final int maxKeys;

        ProviderConfig(String testEndpoint, String authMethod, int rotationDays, int maxKeys) {
            this.testEndpoint = testEndpoint;
            this.authMethod = authMethod;
            this.rotationDays = rotationDays;
            this.maxKeys = maxKeys;
        }
    }
}
