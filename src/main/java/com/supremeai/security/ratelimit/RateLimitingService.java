package com.supremeai.security.ratelimit;

import com.supremeai.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Unified service that delegates to the appropriate RateLimiter implementation.
 */
@Service
@Primary
@RequiredArgsConstructor
public class RateLimitingService implements RateLimiter {

    private final RateLimitProperties properties;
    private final RedisRateLimiter redisRateLimiter;
    private final InMemoryRateLimiter inMemoryRateLimiter;

    private RateLimiter getImplementation() {
        return properties.isDistributed() ? redisRateLimiter : inMemoryRateLimiter;
    }

    @Override
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        return getImplementation().tryAcquire(key, limit, windowSeconds);
    }

    @Override
    public Map<String, Object> getStatus(String key) {
        return getImplementation().getStatus(key);
    }

    @Override
    public void reset(String key) {
        getImplementation().reset(key);
    }
}
