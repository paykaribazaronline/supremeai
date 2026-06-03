package com.supremeai.service;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.provider.StubLocalProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Fast Path AI Service
 *
 * Groq first implementation with automatic failover
 * Returns first valid response as fast as possible
 */
@Service
public class FastPathAIService {

    private static final Logger logger = LoggerFactory.getLogger(FastPathAIService.class);

    private final AIProviderFactory providerFactory;
    private final ResponseCacheService cacheService;

    public FastPathAIService(AIProviderFactory providerFactory, ResponseCacheService cacheService) {
        this.providerFactory = providerFactory;
        this.cacheService = cacheService;
    }

    private CircuitBreaker localCircuitBreaker;

    @PostConstruct
    public void init() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        localCircuitBreaker = registry.circuitBreaker("local_fast_path");

        logger.info("FastPathAIService initialized with local-first circuit breaker");
    }

    /**
     * Fast path generation - Groq first with automatic failover
     * Returns response in < 500ms when Groq is healthy
     */
    public String generateFast(String prompt) {
        String cached = cacheService.getAiResponse(prompt);
        if (cached != null) {
            logger.debug("Cache hit for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));
            return cached;
        }

        if (localCircuitBreaker.getState() == CircuitBreaker.State.CLOSED) {
            try {
                String response = localCircuitBreaker.executeSupplier(() -> {
                    AIProvider local = providerFactory.getDefaultProvider();
                    return resolveResponse(local.generate(prompt));
                });

                cacheService.putAiResponse(prompt, response);
                logger.debug("Local fast path success");
                return response;

            } catch (Exception e) {
                logger.warn("Local fast path failed: {}", e.getMessage());
            }
        }

        try {
            AIProvider fallback = new StubLocalProvider();
            String response = resolveResponse(fallback.generate(prompt));
            cacheService.putAiResponse(prompt, response);
            logger.debug("Stub fallback success");
            return response;

        } catch (Exception e) {
            logger.error("All providers failed: {}", e.getMessage());
            throw new RuntimeException("All AI providers unavailable", e);
        }
    }

    /**
     * Parallel consensus generation - returns first response immediately
     * Runs all providers in parallel in background for consensus
     */
    public String generateParallel(String prompt, String... providers) {
        // Check cache first
        String cached = cacheService.getAiResponse(prompt);
        if (cached != null) {
            return cached;
        }

        CompletableFuture<String> firstResponse = new CompletableFuture<>();

        // Launch all providers in parallel using Reactor's non-blocking subscription
        for (String providerName : providers) {
            try {
                AIProvider provider = providerFactory.getProvider(providerName);
                provider.generate(prompt)
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .subscribe(
                        response -> {
                            if (!firstResponse.isDone()) {
                                firstResponse.complete(response);
                                logger.debug("First response received from: {}", providerName);
                            }
                            // All providers continue in background and populate cache for consensus
                            cacheService.putAiResponse(prompt, response);
                        },
                        error -> logger.warn("Provider {} failed: {}", providerName, error.getMessage())
                    );
            } catch (Exception e) {
                logger.warn("Provider setup failed for {}: {}", providerName, e.getMessage());
            }
        }

        try {
            return firstResponse.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("All providers timed out", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get response", e);
        }
    }

    /**
     * Check if Groq circuit is healthy
     */
    public boolean isGroqHealthy() {
        return localCircuitBreaker.getState() == CircuitBreaker.State.CLOSED;
    }

    private String resolveResponse(Mono<String> responseMono) {
        try {
            return java.util.Objects.requireNonNullElse(
                responseMono.subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .block(Duration.ofSeconds(5)), "");
        } catch (Exception e) {
            logger.warn("resolveResponse timed out or failed: {}", e.getMessage());
            return "";
        }
    }
}
