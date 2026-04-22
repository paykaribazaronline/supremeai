package com.supremeai.security;

import com.supremeai.model.UserApiKey;
import com.supremeai.repository.UserApiKeyRepository;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

        // Handle Google AI which uses query param auth
        String url = config.testEndpoint;
        if ("QueryParam".equals(config.authMethod)) {
            url = url + key.getApiKey();
        }

        try {
            Request.Builder requestBuilder = new Request.Builder().url(url).get();

            if ("Bearer".equals(config.authMethod)) {
                requestBuilder.header("Authorization", "Bearer " + key.getApiKey());
            } else if ("x-api-key".equals(config.authMethod)) {
                requestBuilder.header("x-api-key", key.getApiKey());
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
     * Find the best API key to use for a given provider and user.
     * Picks the active key with the lowest usage count.
     */
    public Optional<UserApiKey> selectBestKey(String userId, String provider) {
        List<UserApiKey> keys = userApiKeyRepository
                .findByUserIdAndStatus(userId, "active")
                .collectList().block();

        if (keys == null || keys.isEmpty()) return Optional.empty();

        return keys.stream()
                .filter(k -> provider.equalsIgnoreCase(k.getProvider()))
                .filter(k -> !"error".equals(k.getStatus()))
                .min(Comparator.comparingLong(k -> k.getRequestCount() != null ? k.getRequestCount() : 0L));
    }

    /**
     * Scheduled check (daily at 2 AM) for keys that need rotation.
     * Marks keys as needing attention but does NOT auto-rotate them
     * (users must manually replace keys for security).
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void checkRotationDueKeys() {
        log.info("Running scheduled API key rotation check...");

        try {
            List<UserApiKey> allKeys = userApiKeyRepository.findAll().collectList().block();
            if (allKeys == null || allKeys.isEmpty()) {
                log.info("No API keys found to check.");
                return;
            }

            int rotationDue = 0;
            int inactive = 0;
            int errors = 0;

            for (UserApiKey key : allKeys) {
                try {
                    // Check if key is already inactive
                    if (!"active".equals(key.getStatus())) {
                        inactive++;
                        continue;
                    }

                    // Check if key has exceeded provider max age
                    int maxDays = getRotationDaysForKey(key.getProvider());
                    LocalDateTime maxAgeDate = LocalDateTime.now().minusDays(maxDays);
                    if (key.getAddedAt() != null && key.getAddedAt().isBefore(maxAgeDate)) {
                        key.setStatus("rotation_due");
                        key.setRotationDueAt(LocalDateTime.now());
                        userApiKeyRepository.save(key).block();
                        log.warn("Key {} for provider '{}' (user {}) is past rotation age ({} days). Marked rotation_due.",
                                key.getMaskedKey(), key.getProvider(), key.getUserId(), maxDays);
                        rotationDue++;
                        continue;
                    }

                    // Check if rotation was explicitly set and is now due
                    if (key.needsRotation()) {
                        key.setStatus("rotation_due");
                        userApiKeyRepository.save(key).block();
                        log.warn("Key {} for provider '{}' (user {}) has explicit rotation_due date reached.",
                                key.getMaskedKey(), key.getProvider(), key.getUserId());
                        rotationDue++;
                    }
                } catch (Exception e) {
                    log.error("Error checking key {}: {}", key.getId(), e.getMessage());
                    errors++;
                }
            }

            log.info("Rotation check complete. Total keys: {}, rotation_due: {}, inactive: {}, errors: {}",
                    allKeys.size(), rotationDue, inactive, errors);
        } catch (Exception e) {
            log.error("Failed to run rotation check: {}", e.getMessage(), e);
        }
    }

    /**
     * Test all active keys and mark any that fail validation.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void validateAllActiveKeys() {
        log.info("Running scheduled API key validation...");

        try {
            List<UserApiKey> allKeys = userApiKeyRepository.findAll().collectList().block();
            if (allKeys == null || allKeys.isEmpty()) return;

            int valid = 0;
            int invalid = 0;
            int skipped = 0;

            for (UserApiKey key : allKeys) {
                if (!"active".equals(key.getStatus())) {
                    skipped++;
                    continue;
                }

                try {
                    Map<String, Object> result = testApiKey(key);
                    boolean isValid = Boolean.TRUE.equals(result.get("valid"));
                    key.setLastTested(LocalDateTime.now());
                    userApiKeyRepository.save(key).block();

                    if (isValid) {
                        valid++;
                    } else {
                        key.setStatus("error");
                        userApiKeyRepository.save(key).block();
                        log.warn("Key {} for provider '{}' failed validation: {}",
                                key.getMaskedKey(), key.getProvider(), result.get("message"));
                        invalid++;
                    }
                } catch (Exception e) {
                    log.error("Validation failed for key {}: {}", key.getId(), e.getMessage());
                    skipped++;
                }
            }

            log.info("Key validation complete. Valid: {}, Invalid: {}, Skipped: {}", valid, invalid, skipped);
        } catch (Exception e) {
            log.error("Failed to validate keys: {}", e.getMessage(), e);
        }
    }

    /**
     * Get rotation status summary for a user.
     */
    public Map<String, Object> getRotationStatus(String userId) {
        List<UserApiKey> keys = userApiKeyRepository.findByUserId(userId).collectList().block();
        if (keys == null) keys = Collections.emptyList();

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
