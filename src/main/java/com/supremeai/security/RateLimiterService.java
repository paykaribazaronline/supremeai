package com.supremeai.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified Access Control.
 * Replaces API Key registration with IP-based rate limiting.
 */
@Service
public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);

    private final ConcurrentHashMap<String, Integer> requestCounts = new ConcurrentHashMap<>();

    private final Counter allowedRequestsCounter;
    private final Counter blockedRequestsCounter;

    public RateLimiterService(MeterRegistry meterRegistry) {
        this.allowedRequestsCounter = meterRegistry.counter("rate_limiter.allowed");
        this.blockedRequestsCounter = meterRegistry.counter("rate_limiter.blocked");
    }

    public boolean isAllowed(String clientIp) {
        int count = requestCounts.getOrDefault(clientIp, 0);
        if (count > 100) {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
            blockedRequestsCounter.increment();
            return false;
        }
        requestCounts.put(clientIp, count + 1);
        logger.debug("Request allowed for IP: {}", clientIp);
        allowedRequestsCounter.increment();
        return true;
    }
}
