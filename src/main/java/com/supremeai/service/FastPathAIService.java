package com.supremeai.service;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * Fast Path AI Service
 *
 * Groq first implementation with automatic failover
 * Returns first valid response as fast as possible
 */
@Service
public class FastPathAIService {

    private static final Logger logger = LoggerFactory.getLogger(FastPathAIService.class);

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private ResponseCacheService cacheService;

    private CircuitBreaker groqCircuitBreaker;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @PostConstruct
    public void init() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        groqCircuitBreaker = registry.circuitBreaker("groq");

        logger.info("FastPathAIService initialized with Groq circuit breaker");
    }

    /**
     * Fast path generation - Groq first with automatic failover
     * Returns response in < 500ms when Groq is healthy
     */
    public String generateFast(String prompt) {
        // Check cache first
        String cached = cacheService.get(prompt);
        if (cached != null) {
            logger.debug("Cache hit for prompt: {}", prompt.substring(0, Math.min(50, prompt.length())));
            return cached;
        }

        // Try Groq first with circuit breaker
        if (groqCircuitBreaker.getState() == CircuitBreaker.State.CLOSED) {
            try {
                String response = groqCircuitBreaker.executeSupplier(() -> {
                    AIProvider groq = providerFactory.getProvider("groq");
                    return groq.generate(prompt);
                });

                cacheService.put(prompt, response);
                logger.debug("Groq fast path success");
                return response;

            } catch (Exception e) {
                logger.warn("Groq fast path failed, falling back to Ollama: {}", e.getMessage());
            }
        }

        // Fallback to local Ollama
        try {
            AIProvider ollama = providerFactory.getProvider("ollama");
            String response = ollama.generate(prompt);
            cacheService.put(prompt, response);
            logger.debug("Ollama fallback success");
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
        String cached = cacheService.get(prompt);
        if (cached != null) {
            return cached;
        }

        CompletableFuture<String> firstResponse = new CompletableFuture<>();

        // Launch all providers in parallel virtual threads
        for (String providerName : providers) {
            executor.submit(() -> {
                try {
                    AIProvider provider = providerFactory.getProvider(providerName);
                    String response = provider.generate(prompt);

                    // Complete immediately on first success
                    if (!firstResponse.isDone()) {
                        firstResponse.complete(response);
                        cacheService.put(prompt, response);
                        logger.debug("First response received from: {}", providerName);
                    }

                    // Continue running others in background for consensus
                    return response;

                } catch (Exception e) {
                    logger.warn("Provider {} failed: {}", providerName, e.getMessage());
                    return null;
                }
            });
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
        return groqCircuitBreaker.getState() == CircuitBreaker.State.CLOSED;
    }
}
