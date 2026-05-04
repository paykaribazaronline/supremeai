package com.supremeai.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Rate limiting configuration with token bucket algorithm.
 * Per-provider rate limits to prevent abuse and ensure fair usage.
 */
@Configuration
public class RateLimitConfig {

    /**
     * OpenAI rate limiter: 1000 requests per minute
     */
    @Bean(name = "openaiRateLimiter")
    public Bucket openaiRateLimiter() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(1000, Refill.greedy(1000, Duration.ofMinutes(1))))
            .build();
    }

    /**
     * Groq rate limiter: 2000 requests per minute (faster provider)
     */
    @Bean(name = "groqRateLimiter")
    public Bucket groqRateLimiter() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(2000, Refill.greedy(2000, Duration.ofMinutes(1))))
            .build();
    }

    /**
     * Local/Ollama rate limiter: 100 requests per minute (resource intensive)
     */
    @Bean(name = "localRateLimiter")
    public Bucket localRateLimiter() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
            .build();
    }

    /**
     * Anthropic rate limiter: 500 requests per minute
     */
    @Bean(name = "anthropicRateLimiter")
    public Bucket anthropicRateLimiter() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(500, Refill.greedy(500, Duration.ofMinutes(1))))
            .build();
    }

    /**
     * General API rate limiter: 100 requests per minute per API key
     */
    @Bean
    public Function<String, Bucket> apiKeyRateLimiter() {
        ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
        
        return apiKey -> buckets.computeIfAbsent(apiKey, k -> 
            Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
                .build()
        );
    }
}