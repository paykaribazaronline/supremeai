package com.supremeai.security.ratelimit;

import java.util.Map;

/**
 * Unified interface for rate limiting implementations.
 */
public interface RateLimiter {
    /**
     * Try to acquire a permit.
     */
    boolean tryAcquire(String key, int limit, int windowSeconds);

    /**
     * Get current status (tokens remaining, etc).
     */
    Map<String, Object> getStatus(String key);

    /**
     * Reset limits for a key.
     */
    void reset(String key);
}
