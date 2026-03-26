package org.example.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.*;

/**
 * DEPRECATED: Use RateLimitingService instead
 * This is a backup implementation
 */
class RateLimitingServiceBackup {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);
    private final int tokensPerMinute;
    private final int tokensPerHour;
    private final Map<String, TokenBucket> userBuckets = new ConcurrentHashMap<>();
    private final Map<String, TokenBucket> projectBuckets = new ConcurrentHashMap<>();
    
    public static class TokenBucket {
        private int tokensRemaining;
        private final int maxTokens;
        private long lastResetTime;
        private final long resetIntervalMs;
        
        public TokenBucket(int maxTokens, long resetIntervalMs) {
            this.maxTokens = maxTokens;
            this.tokensRemaining = maxTokens;
            this.lastResetTime = System.currentTimeMillis();
            this.resetIntervalMs = resetIntervalMs;
        }
        
        public synchronized boolean tryConsume() {
            refillIfNeeded();
            if (tokensRemaining > 0) {
                tokensRemaining--;
                return true;
            }
            return false;
        }
        
        private void refillIfNeeded() {
            long now = System.currentTimeMillis();
            if (now - lastResetTime >= resetIntervalMs) {
                tokensRemaining = maxTokens;
                lastResetTime = now;
            }
        }
        
        public int getTokensRemaining() {
            refillIfNeeded();
            return tokensRemaining;
        }
    }
    
    public RateLimitingServiceBackup(int tokensPerMinute, int tokensPerHour) {
        this.tokensPerMinute = tokensPerMinute;
        this.tokensPerHour = tokensPerHour;
        logger.info("RateLimiter initialized: {}/min, {}/hour", tokensPerMinute, tokensPerHour);
    }
    
    /**
     * Check if user is within rate limits
     */
    public boolean allowUserRequest(String userId) {
        TokenBucket bucket = userBuckets.computeIfAbsent(userId, 
            id -> new TokenBucket(tokensPerMinute, 60000));
        
        if (bucket.tryConsume()) {
            return true;
        } else {
            logger.warn("User rate limited: {}", userId);
            return false;
        }
    }
    
    /**
     * Check if project is within rate limits
     */
    public boolean allowProjectRequest(String projectId) {
        TokenBucket bucket = projectBuckets.computeIfAbsent(projectId,
            id -> new TokenBucket(tokensPerHour, 3600000));
        
        if (bucket.tryConsume()) {
            return true;
        } else {
            logger.warn("Project rate limited: {}", projectId);
            return false;
        }
    }
    
    /**
     * Get remaining tokens for user
     */
    public int getUserTokensRemaining(String userId) {
        TokenBucket bucket = userBuckets.get(userId);
        return bucket != null ? bucket.getTokensRemaining() : tokensPerMinute;
    }
    
    /**
     * Get remaining tokens for project
     */
    public int getProjectTokensRemaining(String projectId) {
        TokenBucket bucket = projectBuckets.get(projectId);
        return bucket != null ? bucket.getTokensRemaining() : tokensPerHour;
    }
    
    /**
     * Reset user rate limit
     */
    public void resetUserLimit(String userId) {
        userBuckets.put(userId, new TokenBucket(tokensPerMinute, 60000));
        logger.info("Reset rate limit for user: {}", userId);
    }
    
    /**
     * Reset project rate limit
     */
    public void resetProjectLimit(String projectId) {
        projectBuckets.put(projectId, new TokenBucket(tokensPerHour, 3600000));
        logger.info("Reset rate limit for project: {}", projectId);
    }
}
