package com.supremeai.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified secrets management service that integrates with multiple secret providers.
 * Provides a fallback mechanism and caching for improved performance.
 * 
 * Priority order:
 * 1. HashiCorp Vault (if enabled)
 * 2. AWS Secrets Manager (if enabled)
 * 3. Environment variables (fallback)
 * 4. EncryptionService (for encrypted values)
 * 
 * Configuration:
 * - secrets.cache.enabled: Enable secret caching (default: true)
 * - secrets.cache.ttl.minutes: Cache TTL in minutes (default: 30)
 */
@Service
public class UnifiedSecretsService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedSecretsService.class);

    @Autowired(required = false)
    private VaultSecretsService vaultSecretsService;

    @Autowired(required = false)
    private AwsSecretsService awsSecretsService;

    @Autowired
    private FirebaseSecretsService firebaseSecretsService;

    @Autowired
    private EncryptionService encryptionService;

    @Value("${secrets.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${secrets.cache.ttl.minutes:30}")
    private int cacheTtlMinutes;

    private final Map<String, CacheEntry> secretCache = new ConcurrentHashMap<>();

    /**
     * Retrieve a secret value with automatic fallback between providers.
     * 
     * @param secretKey The key of the secret to retrieve
     * @return The secret value as a Mono<String>
     */
    public Mono<String> getSecret(String secretKey) {
        // Check cache first
        if (cacheEnabled) {
            CacheEntry cached = secretCache.get(secretKey);
            if (cached != null && !cached.isExpired()) {
                log.debug("Returning cached secret for key: {}", secretKey);
                return Mono.just(cached.value);
            }
        }

        // Try Firebase first (as requested by user)
        if (firebaseSecretsService != null) {
            // Map common keys to Firebase structure if needed
            String firebaseKey = mapToFirebaseKey(secretKey);
            return firebaseSecretsService.getSecret(firebaseKey)
                .flatMap(value -> {
                    if (value != null && !value.isEmpty()) {
                        log.debug("Retrieved secret from Firebase for key: {}", secretKey);
                        return cacheAndReturn(secretKey, value);
                    }
                    return tryVault(secretKey);
                })
                .switchIfEmpty(tryVault(secretKey));
        }

        return tryVault(secretKey);
    }

    /**
     * Try to retrieve secret from HashiCorp Vault.
     */
    private Mono<String> tryVault(String secretKey) {
        if (vaultSecretsService != null) {
            return vaultSecretsService.getSecret(secretKey)
                .flatMap(value -> {
                    if (value != null && !value.isEmpty()) {
                        log.debug("Retrieved secret from Vault for key: {}", secretKey);
                        return cacheAndReturn(secretKey, value);
                    }
                    return tryAwsSecrets(secretKey);
                })
                .switchIfEmpty(tryAwsSecrets(secretKey));
        }
        return tryAwsSecrets(secretKey);
    }

    /**
     * Maps common secret keys to Firebase provider structure.
     * e.g., GEMINI_API_KEY -> gemini.apiKey
     */
    private String mapToFirebaseKey(String secretKey) {
        String key = secretKey.toLowerCase();
        if (key.contains("gemini") && key.contains("key")) return "gemini.apiKey";
        if (key.contains("openai") && key.contains("key")) return "openai.apiKey";
        if (key.contains("groq") && key.contains("key")) return "groq.apiKey";
        if (key.contains("anthropic") && key.contains("key")) return "anthropic.apiKey";
        if (key.contains("deepseek") && key.contains("key")) return "deepseek.apiKey";
        
        // Fallback: if it contains a dot, assume it's already in provider.key format
        if (secretKey.contains(".")) return secretKey;
        
        // Otherwise try to guess (e.g. GEMINI -> gemini.apiKey)
        return secretKey.toLowerCase() + ".apiKey";
    }

    /**
     * Try to retrieve secret from AWS Secrets Manager.
     */
    private Mono<String> tryAwsSecrets(String secretKey) {
        if (awsSecretsService != null) {
            return awsSecretsService.getSecret(secretKey)
                .flatMap(value -> {
                    if (value != null && !value.isEmpty()) {
                        log.debug("Retrieved secret from AWS Secrets Manager for key: {}", secretKey);
                        return cacheAndReturn(secretKey, value);
                    }
                    return tryEnvironmentVariable(secretKey);
                })
                .switchIfEmpty(tryEnvironmentVariable(secretKey));
        }

        // Fall back to environment variables
        return tryEnvironmentVariable(secretKey);
    }

    /**
     * Try to retrieve secret from environment variables.
     */
    private Mono<String> tryEnvironmentVariable(String secretKey) {
        String envValue = System.getenv(secretKey.toUpperCase());
        if (envValue != null && !envValue.isEmpty()) {
            log.debug("Retrieved secret from environment variable for key: {}", secretKey);
            return cacheAndReturn(secretKey, envValue);
        }

        log.warn("Secret not found in any provider for key: {}", secretKey);
        return Mono.empty();
    }

    /**
     * Cache the secret value and return it.
     */
    private Mono<String> cacheAndReturn(String key, String value) {
        if (cacheEnabled) {
            secretCache.put(key, new CacheEntry(value, cacheTtlMinutes));
        }
        return Mono.just(value);
    }

    /**
     * Retrieve multiple secrets at once.
     * 
     * @param secretKeys Array of secret keys to retrieve
     * @return Map of secret keys to values
     */
    public Mono<Map<String, String>> getSecrets(String... secretKeys) {
        return Mono.fromCallable(() -> {
            Map<String, String> secrets = new HashMap<>();
            for (String key : secretKeys) {
                String value = getSecret(key).block();
                if (value != null) {
                    secrets.put(key, value);
                }
            }
            return secrets;
        });
    }

    /**
     * Encrypt a value using the EncryptionService.
     * 
     * @param value The value to encrypt
     * @return The encrypted value
     */
    public String encrypt(String value) {
        return encryptionService.encrypt(value);
    }

    /**
     * Decrypt a value using the EncryptionService.
     * 
     * @param encryptedValue The encrypted value
     * @return The decrypted value
     */
    public String decrypt(String encryptedValue) {
        return encryptionService.decrypt(encryptedValue);
    }

    /**
     * Clear the secret cache.
     */
    public void clearCache() {
        log.info("Clearing secret cache");
        secretCache.clear();
    }

    /**
     * Check the health of all secret providers.
     * 
     * @return Map of provider names to health status
     */
    public Mono<Map<String, Boolean>> healthCheck() {
        return Mono.fromCallable(() -> {
            Map<String, Boolean> health = new HashMap<>();

            if (vaultSecretsService != null) {
                Boolean vaultHealth = vaultSecretsService.healthCheck().block();
                health.put("vault", vaultHealth != null && vaultHealth);
            } else {
                health.put("vault", false);
            }

            if (awsSecretsService != null) {
                Boolean awsHealth = awsSecretsService.healthCheck().block();
                health.put("aws", awsHealth != null && awsHealth);
            } else {
                health.put("aws", false);
            }

            if (firebaseSecretsService != null) {
                Boolean firebaseHealth = firebaseSecretsService.healthCheck().block();
                health.put("firebase", firebaseHealth != null && firebaseHealth);
            } else {
                health.put("firebase", false);
            }

            health.put("environment", true); // Environment variables always available

            return health;
        });
    }

    /**
     * Cache entry with TTL support.
     */
    private static class CacheEntry {
        final String value;
        final long expiryTime;

        CacheEntry(String value, int ttlMinutes) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + (ttlMinutes * 60 * 1000L);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
