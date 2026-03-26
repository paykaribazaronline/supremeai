package org.example.security;

import com.google.cloud.secretmanager.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Secure API key management using Google Cloud Secret Manager
 * Never store secrets in code or environment variables directly
 */
public class SecretManager {
    private static final Logger logger = LoggerFactory.getLogger(SecretManager.class);
    private final String projectId;
    private final Map<String, String> secretCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 3600000; // 1 hour
    private final Map<String, Long> cacheTimes = new ConcurrentHashMap<>();
    
    public SecretManager(String projectId) {
        this.projectId = projectId;
        logger.info("Initializing SecretManager for project: {}", projectId);
    }
    
    /**
     * Retrieve secret from Cloud Secret Manager with caching
     * @param secretId Secret name (e.g., "deepseek-api-key")
     * @return Secret value
     * @throws SecretNotFoundException if secret doesn't exist
     */
    public String getSecret(String secretId) throws SecretNotFoundException {
        // Check cache first
        if (isCacheValid(secretId)) {
            logger.debug("Returning cached secret: {}", secretId);
            return secretCache.get(secretId);
        }
        
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            SecretVersionName name = SecretVersionName.of(projectId, secretId, "latest");
            AccessSecretVersionResponse response = client.accessSecretVersion(name);
            String secret = response.getPayload().getData().toStringUtf8();
            
            // Cache the secret
            secretCache.put(secretId, secret);
            cacheTimes.put(secretId, System.currentTimeMillis());
            
            logger.info("Retrieved and cached secret: {}", secretId);
            return secret;
        } catch (Exception e) {
            logger.error("Failed to retrieve secret: {}", secretId, e);
            String envVar = System.getenv(secretId);
            if (envVar != null) {
                logger.info("Using environment variable for: {}", secretId);
                return envVar;
            }
            throw new RuntimeException("Failed to retrieve secret: " + secretId, e);
        }
    }
    
    /**
     * Get multiple secrets efficiently
     */
    public Map<String, String> getSecrets(String... secretIds) throws SecretNotFoundException {
        Map<String, String> result = new HashMap<>();
        for (String id : secretIds) {
            result.put(id, getSecret(id));
        }
        return result;
    }
    
    /**
     * Invalidate cache for a secret (e.g., after rotation)
     */
    public void invalidateCache(String secretId) {
        secretCache.remove(secretId);
        cacheTimes.remove(secretId);
        logger.info("Cache invalidated for secret: {}", secretId);
    }
    
    /**
     * Clear all cached secrets
     */
    public void clearCache() {
        secretCache.clear();
        cacheTimes.clear();
        logger.info("All secret caches cleared");
    }
    
    /**
     * Check if cached value is still valid
     */
    private boolean isCacheValid(String secretId) {
        if (!secretCache.containsKey(secretId)) {
            return false;
        }
        Long cacheTime = cacheTimes.get(secretId);
        if (cacheTime == null) {
            return false;
        }
        return (System.currentTimeMillis() - cacheTime) < CACHE_TTL_MS;
    }
    
    /**
     * Get fallback secret from environment if Cloud Secret Manager unavailable
     * WARNING: Only for local development, never in production
     */
    public String getSecretWithFallback(String secretId, boolean allowEnvFallback) 
            throws SecretNotFoundException {
        try {
            return getSecret(secretId);
        } catch (Exception e) {
            if (allowEnvFallback) {
                String envKey = secretId.toUpperCase().replace("-", "_");
                String envValue = System.getenv(envKey);
                if (envValue != null && !envValue.isEmpty()) {
                    logger.warn("Using environment variable fallback for: {}", secretId);
                    return envValue;
                }
            }
            throw new SecretNotFoundException("Secret not available: " + secretId);
        }
    }
    
    /**
     * Custom exception for missing secrets
     */
    public static class SecretNotFoundException extends Exception {
        public SecretNotFoundException(String message) {
            super(message);
        }
    }
}
