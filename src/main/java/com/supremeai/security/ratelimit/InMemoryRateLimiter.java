package com.supremeai.security.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for local development or single-node deployments.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStarts = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowStart = windowStarts.computeIfAbsent(key, k -> now);
        
        if (now - windowStart > (windowSeconds * 1000L)) {
            counts.put(key, new AtomicInteger(0));
            windowStarts.put(key, now);
        }

        AtomicInteger count = counts.computeIfAbsent(key, k -> new AtomicInteger(0));
        if (count.get() >= limit) {
            return false;
        }

        count.incrementAndGet();
        return true;
    }

    @Override
    public Map<String, Object> getStatus(String key) {
        return Map.of(
            "count", counts.getOrDefault(key, new AtomicInteger(0)).get(),
            "window_start", windowStarts.getOrDefault(key, 0L)
        );
    }

    @Override
    public void reset(String key) {
        counts.remove(key);
        windowStarts.remove(key);
    }
}
