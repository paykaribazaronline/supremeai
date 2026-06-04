package com.supremeai.security.ratelimit;

import com.supremeai.config.RateLimitProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Unified service that delegates to the appropriate RateLimiter implementation. Falls back to
 * in-memory if Redis is unavailable.
 */
@Service
@Primary
public class RateLimitingService implements RateLimiter {

  private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);

  private final RateLimitProperties properties;
  private final RedisRateLimiter redisRateLimiter;
  private final InMemoryRateLimiter inMemoryRateLimiter;

  public RateLimitingService(
      RateLimitProperties properties,
      RedisRateLimiter redisRateLimiter,
      InMemoryRateLimiter inMemoryRateLimiter) {
    this.properties = properties;
    this.redisRateLimiter = redisRateLimiter;
    this.inMemoryRateLimiter = inMemoryRateLimiter;
  }

  @Override
  public boolean tryAcquire(String key, int limit, int windowSeconds) {
    if (properties.isDistributed()) {
      try {
        return redisRateLimiter.tryAcquire(key, limit, windowSeconds);
      } catch (Exception e) {
        log.warn(
            "Redis rate limiter failed for key '{}', falling back to in-memory: {}",
            key,
            e.getMessage());
        return inMemoryRateLimiter.tryAcquire(key, limit, windowSeconds);
      }
    }
    return inMemoryRateLimiter.tryAcquire(key, limit, windowSeconds);
  }

  @Override
  public Map<String, Object> getStatus(String key) {
    if (properties.isDistributed()) {
      try {
        return redisRateLimiter.getStatus(key);
      } catch (Exception e) {
        log.warn("Redis getStatus failed for key '{}', falling back to in-memory", key);
        return inMemoryRateLimiter.getStatus(key);
      }
    }
    return inMemoryRateLimiter.getStatus(key);
  }

  @Override
  public void reset(String key) {
    if (properties.isDistributed()) {
      try {
        redisRateLimiter.reset(key);
      } catch (Exception e) {
        log.warn("Redis reset failed for key '{}', resetting in-memory", key);
        inMemoryRateLimiter.reset(key);
      }
    } else {
      inMemoryRateLimiter.reset(key);
    }
  }
}
