package com.supremeai.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified secrets management service that integrates with GCP and Firebase.
 * Optimized for reduced build size and cloud-native stability.
 * 
 * Priority order:
 * 1. GCP Secret Manager (Native)
 * 2. Firebase Secrets (App context)
 * 3. Environment variables (Local/Container fallback)
 */
@Service
public class UnifiedSecretsService {

    private static final Logger log = LoggerFactory.getLogger(UnifiedSecretsService.class);

    @Autowired
    private SecretManagerService secretManagerService;

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

        // Try GCP Secret Manager first (Core Production Provider)
        return Mono.fromCallable(() -> secretManagerService.getSecret(secretKey))
            .flatMap(value -> {
                if (value != null && !value.isEmpty()) {
                    log.debug("Retrieved secret from GCP Secret Manager for key: {}", secretKey);
                    return cacheAndReturn(secretKey, value);
                }
                return tryFirebase(secretKey);
            })
            .onErrorResume(e -> tryFirebase(secretKey))
            .switchIfEmpty(tryFirebase(secretKey));
    }

    /**
     * Try to retrieve secret from Firebase.
     */
    private Mono<String> tryFirebase(String secretKey) {
        if (firebaseSecretsService != null) {
            String firebaseKey = mapToFirebaseKey(secretKey);
            return firebaseSecretsService.getSecret(firebaseKey)
                .flatMap(value -> {
                    if (value != null && !value.isEmpty()) {
                        log.debug("Retrieved secret from Firebase for key: {}", secretKey);
                        return cacheAndReturn(secretKey, value);
                    }
                    return tryEnvironmentVariable(secretKey);
                })
                .switchIfEmpty(tryEnvironmentVariable(secretKey));
        }
        return tryEnvironmentVariable(secretKey);
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
     * Try to retrieve secret from environment variables.
     */
    private Mono<String> tryEnvironmentVariable(String secretKey) {
        String envValue = System.getenv(secretKey.toUpperCase().replace(".", "_").replace("-", "_"));
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
     */
    public Mono<Map<String, String>> getSecrets(String... secretKeys) {
        return Mono.fromCallable(() -> {
            Map<String, String> secrets = new HashMap<>();
            for (String key : secretKeys) {
                // blocking for simplicity in the list retrieval context
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
     */
    public String encrypt(String value) {
        return encryptionService.encrypt(value);
    }

    /**
     * Decrypt a value using the EncryptionService.
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
     * Check the health of available secret providers.
     */
    public Mono<Map<String, Boolean>> healthCheck() {
        return Mono.fromCallable(() -> {
            Map<String, Boolean> health = new HashMap<>();

            // GCP Secret Manager Check
            health.put("gcp_secret_manager", secretManagerService != null);

            // Firebase Check
            if (firebaseSecretsService != null) {
                Boolean firebaseHealth = firebaseSecretsService.healthCheck().block();
                health.put("firebase", firebaseHealth != null && firebaseHealth);
            } else {
                health.put("firebase", false);
            }

            health.put("environment", true);
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

