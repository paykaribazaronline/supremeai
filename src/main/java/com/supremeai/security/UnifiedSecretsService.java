package com.supremeai.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UnifiedSecretsService {

  private static final Logger log = LoggerFactory.getLogger(UnifiedSecretsService.class);

  @Autowired private SecretManagerService secretManagerService;

  @Autowired private FirebaseSecretsService firebaseSecretsService;

  @Autowired private EncryptionService encryptionService;

  @Value("${secrets.cache.enabled:true}")
  private boolean cacheEnabled;

  @Value("${secrets.cache.ttl.minutes:30}")
  private int cacheTtlMinutes;

  @Value("${spring.profiles.active:local}")
  private String activeProfile;

  private final Cache<String, String> secretCache;

  public UnifiedSecretsService() {
    this.secretCache =
        Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats()
            .build();
  }

  public boolean isProduction() {
    return "prod".equals(activeProfile);
  }

  /**
   * Retrieve a secret value with automatic fallback between providers.
   *
   * @param secretKey The key of the secret to retrieve
   * @return The secret value as a Mono<String>
   */
  public Mono<String> getSecret(String secretKey) {
    if (cacheEnabled) {
      String cached = secretCache.getIfPresent(secretKey);
      if (cached != null) {
        log.debug("Returning cached secret for key: {}", secretKey);
        return Mono.just(cached);
      }
    }

    return Mono.fromCallable(() -> secretManagerService.getSecret(secretKey))
        .flatMap(
            value -> {
              if (value != null && !value.isEmpty()) {
                log.debug("Retrieved secret from GCP Secret Manager for key: {}", secretKey);
                return cacheAndReturn(secretKey, value);
              }
              return tryFirebase(secretKey);
            })
        .onErrorResume(e -> tryFirebase(secretKey))
        .switchIfEmpty(tryFirebase(secretKey));
  }

  /** Try to retrieve secret from Firebase. */
  private Mono<String> tryFirebase(String secretKey) {
    if (firebaseSecretsService != null) {
      String firebaseKey = mapToFirebaseKey(secretKey);
      return firebaseSecretsService
          .getSecret(firebaseKey)
          .flatMap(
              value -> {
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
   * Maps common secret keys to Firebase provider structure. e.g., GEMINI_API_KEY -> gemini.apiKey
   */
  private String mapToFirebaseKey(String secretKey) {
    String key = secretKey.toLowerCase();
    if (key.contains("gemini") && key.contains("key")) return "gemini.apiKey";
    if (isLocalFirstExcludedKey(key)) return null;

    if (secretKey.contains(".")) return secretKey;

    return secretKey.toLowerCase() + ".apiKey";
  }

  private boolean isLocalFirstExcludedKey(String key) {
    return key.contains("openai")
        || key.contains("groq")
        || key.contains("anthropic")
        || key.contains("deepseek");
  }

  /** Try to retrieve secret from environment variables. */
  private Mono<String> tryEnvironmentVariable(String secretKey) {
    if (isProduction()) {
      log.error(
          "CRITICAL SECURITY AUDIT FAILURE: Env var fallback for secret key '{}' is BLOCKED in production!",
          secretKey);
      return Mono.empty();
    }

    String envValue = System.getenv(secretKey.toUpperCase().replace(".", "_").replace("-", "_"));
    if (envValue != null && !envValue.isEmpty()) {
      log.debug("Retrieved secret from environment variable for key: {}", secretKey);
      return cacheAndReturn(secretKey, envValue);
    }

    log.warn("Secret not found in any provider for key: {}", secretKey);
    return Mono.empty();
  }

  /** Cache the secret value and return it. */
  private Mono<String> cacheAndReturn(String key, String value) {
    if (cacheEnabled) {
      secretCache.put(key, value);
    }
    return Mono.just(value);
  }

  /** Retrieve multiple secrets at once. */
  public Mono<Map<String, String>> getSecrets(String... secretKeys) {
    return reactor.core.publisher.Flux.fromArray(secretKeys)
        .flatMap(key -> getSecret(key).map(value -> reactor.util.function.Tuples.of(key, value)))
        .collectMap(tuple -> tuple.getT1(), tuple -> tuple.getT2());
  }

  /** Encrypt a value using the EncryptionService. */
  public String encrypt(String value) {
    return encryptionService.encrypt(value);
  }

  /** Decrypt a value using the EncryptionService. */
  public String decrypt(String encryptedValue) {
    return encryptionService.decrypt(encryptedValue);
  }

  /** Clear the secret cache. */
  public void clearCache() {
    log.info("Clearing secret cache");
    secretCache.invalidateAll();
  }
}
