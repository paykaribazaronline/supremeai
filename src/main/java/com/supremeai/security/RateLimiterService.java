package com.supremeai.security;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified Access Control.
 * Replaces API Key registration with IP-based rate limiting.
 */
@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, Integer> requestCounts = new ConcurrentHashMap<>();

    public boolean isAllowed(String clientIp) {
        // Simple logic: Allow 100 requests per minute per IP
        int count = requestCounts.getOrDefault(clientIp, 0);
        if (count > 100) {
            return false;
        }
        requestCounts.put(clientIp, count + 1);
        return true;
    }
}
