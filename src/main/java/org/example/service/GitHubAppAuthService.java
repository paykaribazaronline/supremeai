package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GitHub App Authentication Service
 * 
 * Uses GitHub App (supremeai-bot) for OAuth instead of personal tokens.
 * Benefits:
 * - 10,000 requests/hour (vs 5,000 for personal token)
 * - Better security (can revoke at app level)
 * - Can limit permissions per repo
 * - Better audit trail
 * 
 * Workflow:
 * 1. Load app private key from env
 * 2. Generate JWT (signed with private key)
 * 3. Exchange JWT for installation access token
 * 4. Use access token for GitHub API calls
 * 5. Cache token until expiry
 */
@Service
public class GitHubAppAuthService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubAppAuthService.class);
    
    @Value("${github.app.id:}")
    private String appId;
    
    @Value("${github.app.private-key:}")
    private String privateKeyPem;
    
    @Value("${github.app.installation-id:}")
    private String installationId;
    
    // Cache installation tokens (key: installationId, value: token + expiry)
    private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();
    
    private PrivateKey cachedPrivateKey;
    
    /**
     * Get GitHub API token for use in requests
     * 
     * Workflow:
     * 1. Check cache for valid token
     * 2. If expired or missing → generate new JWT
     * 3. Exchange JWT for installation token
     * 4. Cache token with expiry
     * 5. Return token
     */
    public String getAccessToken(String installationId) {
        try {
            // Check cache first
            CachedToken cached = tokenCache.get(installationId);
            if (cached != null && cached.isValid()) {
                logger.debug("✓ Using cached GitHub App token for installation: {}", installationId);
                return cached.token;
            }
            
            logger.info("🔄 Generating new GitHub App token for installation: {}", installationId);
            
            // Generate JWT (valid for 10 minutes max)
            String jwt = generateJWT();
            
            // Exchange JWT for installation access token
            // TODO: Call GitHub API to exchange
            // POST https://api.github.com/app/installations/{installation_id}/access_tokens
            // Returns: { "token": "...", "expires_at": "..." }
            
            String token = "ghs_dummy_token"; // Placeholder
            long expiresAtSeconds = System.currentTimeMillis() / 1000 + 3600; // 1 hour
            
            // Cache the token
            tokenCache.put(installationId, new CachedToken(token, expiresAtSeconds));
            
            logger.info("✓ GitHub App token generated and cached");
            return token;
            
        } catch (Exception e) {
            logger.error("❌ Failed to get GitHub App token", e);
            throw new RuntimeException("GitHub App authentication failed", e);
        }
    }
    
    /**
     * Generate JWT signed with app private key
     * JWT is used to authenticate the app with GitHub
     */
    private String generateJWT() throws Exception {
        if (cachedPrivateKey == null) {
            cachedPrivateKey = loadPrivateKey(privateKeyPem);
        }
        
        long issuedAtTime = System.currentTimeMillis() / 1000;
        long expirationTime = issuedAtTime + 600; // 10 minutes
        
        // JWT payload
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iat", issuedAtTime);
        payload.put("exp", expirationTime);
        payload.put("iss", appId);
        
        // TODO: Actually sign the JWT
        // This requires implementing JWT signing with RSA
        // For now, return placeholder
        
        String jwtToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.placeholder.signature";
        logger.debug("JWT generated: iss={}, exp={}", appId, expirationTime);
        
        return jwtToken;
    }
    
    /**
     * Load RSA private key from PEM format
     */
    private PrivateKey loadPrivateKey(String privateKeyPem) throws Exception {
        try {
            String key = privateKeyPem
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            
            byte[] decodedKey = Base64.getDecoder().decode(key);
            
            java.security.spec.PKCS8EncodedKeySpec keySpec = 
                    new java.security.spec.PKCS8EncodedKeySpec(decodedKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            
            logger.info("✓ RSA private key loaded");
            return kf.generatePrivate(keySpec);
            
        } catch (Exception e) {
            logger.error("❌ Failed to load private key", e);
            throw e;
        }
    }
    
    /**
     * Verify app is properly configured
     */
    public void verifyAppConfiguration() {
        if (appId == null || appId.isEmpty()) {
            throw new RuntimeException("GITHUB_APP_ID not configured");
        }
        if (privateKeyPem == null || privateKeyPem.isEmpty()) {
            throw new RuntimeException("GITHUB_APP_PRIVATE_KEY not configured");
        }
        if (installationId == null || installationId.isEmpty()) {
            throw new RuntimeException("GITHUB_APP_INSTALLATION_ID not configured");
        }
        
        logger.info("✓ GitHub App configuration verified (appId={}, installationId={})", 
                appId, installationId);
    }
    
    /**
     * Clear token cache (for testing or manual reset)
     */
    public void clearTokenCache() {
        tokenCache.clear();
        logger.info("GitHub App token cache cleared");
    }
    
    /**
     * Get token cache stats (for monitoring)
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
            "cachedTokens", tokenCache.size(),
            "validTokens", tokenCache.values().stream()
                    .filter(CachedToken::isValid)
                    .count()
        );
    }
    
    /**
     * Get GitHub API rate limit for the app
     */
    public Map<String, Object> getRateLimit() {
        // GitHub App has 10,000 requests/hour
        return Map.of(
            "limit", 10000,
            "remaining", 9999, // Update from actual API
            "resetEpochSeconds", System.currentTimeMillis() / 1000 + 3600
        );
    }
    
    /**
     * Internal cached token holder
     */
    private static class CachedToken {
        String token;
        long expiresAtSeconds;
        
        CachedToken(String token, long expiresAtSeconds) {
            this.token = token;
            this.expiresAtSeconds = expiresAtSeconds;
        }
        
        boolean isValid() {
            // Valid if not expired yet (with 60 second buffer)
            long currentTime = System.currentTimeMillis() / 1000;
            return currentTime < (expiresAtSeconds - 60);
        }
    }
}
