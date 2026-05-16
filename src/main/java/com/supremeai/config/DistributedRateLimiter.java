package com.supremeai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Distributed rate limiter using Redis for cluster-wide rate limiting.
 * Uses Redis Lua script for atomic operations to ensure accuracy in distributed environments.
 */
@Component
public class DistributedRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(DistributedRateLimiter.class);

    private final RedisTemplate<String, Object> redisTemplate;

    // Lua script for atomic rate limiting using token bucket algorithm
    private static final String RATE_LIMIT_SCRIPT =
            "local key = KEYS[1]\n" +
            "local limit = tonumber(ARGV[1])\n" +
            "local window = tonumber(ARGV[2])\n" +
            "local now = tonumber(ARGV[3])\n" +
            "local requested = tonumber(ARGV[4])\n" +
            "\n" +
            "local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')\n" +
            "local tokens = tonumber(bucket[1])\n" +
            "local last_refill = tonumber(bucket[2])\n" +
            "\n" +
            "if tokens == nil then\n" +
            "    tokens = limit\n" +
            "    last_refill = now\n" +
            "end\n" +
            "\n" +
            "local time_passed = math.max(0, now - last_refill)\n" +
            "local refill_amount = (time_passed / window) * limit\n" +
            "tokens = math.min(limit, tokens + refill_amount)\n" +
            "\n" +
            "if tokens >= requested then\n" +
            "    tokens = tokens - requested\n" +
            "    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)\n" +
            "    redis.call('EXPIRE', key, window * 2)\n" +
            "    return 1\n" +
            "else\n" +
            "    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)\n" +
            "    redis.call('EXPIRE', key, window * 2)\n" +
            "    return 0\n" +
            "end";

    public DistributedRateLimiter(@org.springframework.beans.factory.annotation.Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Try to acquire a permit for the given key.
     * Uses token bucket algorithm for smooth rate limiting.
     *
     * @param key Unique identifier for the rate limit bucket (e.g., user ID, IP address)
     * @param limit Maximum number of requests allowed
     * @param window Time window in seconds
     * @return true if permit acquired, false if rate limit exceeded
     */
    public boolean tryAcquire(String key, int limit, int window) {
        return tryAcquire(key, limit, window, 1);
    }

    /**
     * Try to acquire multiple permits for the given key.
     *
     * @param key Unique identifier for the rate limit bucket
     * @param limit Maximum number of requests allowed
     * @param window Time window in seconds
     * @param permits Number of permits to acquire
     * @return true if permits acquired, false if rate limit exceeded
     */
    public boolean tryAcquire(String key, int limit, int window, int permits) {
        if (redisTemplate == null) {
            // Fail open if Redis is disabled
            return true;
        }
        try {
            RedisScript<Long> script = RedisScript.of(RATE_LIMIT_SCRIPT, Long.class);
            List<String> keys = Collections.singletonList("rate_limit:" + key);
            List<String> args = List.of(
                    String.valueOf(limit),
                    String.valueOf(window),
                    String.valueOf(Instant.now().getEpochSecond()),
                    String.valueOf(permits)
            );

            Long result = redisTemplate.execute(script, keys, (Object[]) args.toArray(new String[0]));
            boolean allowed = result != null && result == 1L;

            if (!allowed) {
                log.debug("Rate limit exceeded for key: {}", key);
            }

            return allowed;
        } catch (Exception e) {
            log.error("Error checking rate limit for key {}: {}", key, e.getMessage());
            // Fail open - allow request if Redis is unavailable
            return true;
        }
    }

    /**
     * Get current rate limit status for a key.
     *
     * @param key Unique identifier for the rate limit bucket
     * @return Map containing tokens remaining and reset time
     */
    public Map<String, Object> getStatus(String key) {
        if (redisTemplate == null) {
            return Map.of("status", "disabled");
        }
        try {
            String redisKey = "rate_limit:" + key;
            Map<Object, Object> bucket = redisTemplate.opsForHash().entries(redisKey);

            Map<String, Object> status = new HashMap<>();
            status.put("tokens", bucket.getOrDefault("tokens", 0));
            status.put("last_refill", bucket.getOrDefault("last_refill", 0));
            status.put("ttl", redisTemplate.getExpire(redisKey));

            return status;
        } catch (Exception e) {
            log.error("Error getting rate limit status for key {}: {}", key, e.getMessage());
            return Map.of("error", "Unable to retrieve rate limit status");
        }
    }

    /**
     * Reset rate limit for a key.
     *
     * @param key Unique identifier for the rate limit bucket
     */
    public void reset(String key) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("rate_limit:" + key);
                log.debug("Rate limit reset for key: {}", key);
            } catch (Exception e) {
                log.error("Error resetting rate limit for key {}: {}", key, e.getMessage());
            }
        }
    }
}
