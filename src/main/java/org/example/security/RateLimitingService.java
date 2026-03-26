package org.example.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting service to prevent abuse and quota exhaustion
 * Uses simple token bucket algorithm
 */
public class RateLimitingService {
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
    
    public RateLimitingService(int tokensPerMinute, int tokensPerHour) {
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
    public long getUserRemainingTokens(String userId) {
        TokenBucket bucket = userBuckets.get(userId);
        return bucket != null ? bucket.getTokensRemaining() : tokensPerMinute;
    }
    
    /**
     * Get remaining tokens for project
     */
    public long getProjectRemainingTokens(String projectId) {
        TokenBucket bucket = projectBuckets.get(projectId);
        return bucket != null ? bucket.getTokensRemaining() : tokensPerHour;
    }
    
    /**
     * Reset rate limit for user
     */
    public void resetUserLimit(String userId) {
        userBuckets.remove(userId);
        logger.info("Reset rate limit for user: {}", userId);
    }
    
    /**
     * Reset rate limit for project
     */
    public void resetProjectLimit(String projectId) {
        projectBuckets.remove(projectId);
        logger.info("Reset rate limit for project: {}", projectId);
    }
    
    /**
     * Clear all rate limits
     */
    public void clearAllLimits() {
        userBuckets.clear();
        projectBuckets.clear();
        logger.info("Cleared all rate limit buckets");
    }
    
    /**
     * Get statistics
     */
    public Map<String, Object> getStats() {
        return Map.ofEntries(
            Map.entry("active_users", userBuckets.size()),
            Map.entry("active_projects", projectBuckets.size()),
            Map.entry("tokens_per_minute", tokensPerMinute),
            Map.entry("tokens_per_hour", tokensPerHour)
        );
    }
}
