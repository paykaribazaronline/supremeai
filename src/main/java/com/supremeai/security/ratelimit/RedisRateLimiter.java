package com.supremeai.security.ratelimit;

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
 * Redis-based distributed rate limiter implementation.
 */
@Component
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_SCRIPT =
            "local key = KEYS[1]\n" +
            "local limit = tonumber(ARGV[1])\n" +
            "local window = tonumber(ARGV[2])\n" +
            "local now = tonumber(ARGV[3])\n" +
            "local requested = 1\n" +
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

    public RedisRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        try {
            RedisScript<Long> script = RedisScript.of(RATE_LIMIT_SCRIPT, Long.class);
            List<String> keys = Collections.singletonList("rate_limit:" + key);
            List<String> args = List.of(
                    String.valueOf(limit),
                    String.valueOf(windowSeconds),
                    String.valueOf(Instant.now().getEpochSecond())
            );

            Long result = redisTemplate.execute(script, keys, args.toArray(new String[0]));
            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("Redis rate limit error for {}: {}", key, e.getMessage());
            return true; // Fail open
        }
    }

    @Override
    public Map<String, Object> getStatus(String key) {
        try {
            String redisKey = "rate_limit:" + key;
            Map<Object, Object> bucket = redisTemplate.opsForHash().entries(redisKey);

            Map<String, Object> status = new HashMap<>();
            status.put("tokens", bucket.getOrDefault("tokens", 0));
            status.put("last_refill", bucket.getOrDefault("last_refill", 0));
            status.put("ttl", redisTemplate.getExpire(redisKey));

            return status;
        } catch (Exception e) {
            return Map.of("error", "Status unavailable");
        }
    }

    @Override
    public void reset(String key) {
        redisTemplate.delete("rate_limit:" + key);
    }
}
