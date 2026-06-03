package com.supremeai.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for rate limiting.
 * Currently uses simple in-memory implementation via RateLimitingFilter.
 * Future: integrate with Redis or distributed store for cluster environments.
 */
@Configuration
public class RateLimiterConfiguration {
    // Configuration properties can be defined in application.yml under rate.limit
    // e.g., rate.limit.per-minute=100
}
