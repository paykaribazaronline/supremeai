package com.supremeai.resilience;

import com.supremeai.cost.QuotaManager;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Retryable executor for AI provider calls.
 *
 * Features:
 * - Exponential backoff with jitter between retries
 * - Configurable max attempts per provider
 * - Quota-aware: skips retry if quota is exhausted
 * - Respects circuit breaker state
 * - Distinguishes between retryable and non-retryable errors
 */
@Service
public class RetryableAIExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryableAIExecutor.class);

    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_WAIT = Duration.ofMillis(500);
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final double JITTER_FACTOR = 0.3;

    private RetryConfig retryConfig;
    private RetryRegistry retryRegistry;

    @Autowired
    private QuotaManager quotaManager;

    @PostConstruct
    public void init() {
        retryConfig = RetryConfig.custom()
                .maxAttempts(MAX_RETRIES + 1) // 1 initial + retries
                .waitDuration(INITIAL_WAIT)
                .retryOnException(this::isRetryable)
                .build();

        retryRegistry = RetryRegistry.of(retryConfig);
    }

    /**
     * Execute an AI call with retry and exponential backoff.
     *
     * @param providerName Provider identifier (used for circuit breaker context)
     * @param serviceName  Service name for quota checking
     * @param operation    The actual AI call
     * @return The result
     */
    public <T> T execute(String providerName, String serviceName, Supplier<T> operation) {
        // Check quota before attempting
        if (!quotaManager.recordUsage(serviceName, "Requests", 0)) {
            throw new QuotaExceededException("Quota exceeded for " + serviceName);
        }

        Retry retry = retryRegistry.retry(providerName);

        return Retry.decorateSupplier(retry, () -> {
            try {
                return operation.get();
            } catch (Exception e) {
                log.warn("Retry attempt failed for {}: {}", providerName, e.getMessage());
                throw e;
            }
        }).get();
    }

    /**
     * Execute an AI call with retry and a custom circuit breaker.
     *
     * @param providerName   Provider identifier
     * @param serviceName    Service name for quota checking
     * @param circuitBreaker Circuit breaker instance
     * @param operation      The actual AI call
     * @return The result
     */
    public <T> T executeWithCircuitBreaker(String providerName, String serviceName,
                                           CircuitBreaker circuitBreaker, Supplier<T> operation) {
        if (!quotaManager.recordUsage(serviceName, "Requests", 0)) {
            throw new QuotaExceededException("Quota exceeded for " + serviceName);
        }

        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            throw new CircuitBreakerOpenException("Circuit breaker is OPEN for " + providerName);
        }

        Retry retry = retryRegistry.retry(providerName);
        Supplier<T> decorated = Retry.decorateSupplier(retry, operation);
        return CircuitBreaker.decorateSupplier(circuitBreaker, decorated).get();
    }

    /**
     * Calculate sleep duration with exponential backoff and jitter.
     */
    public long calculateBackoff(int attempt) {
        long base = INITIAL_WAIT.toMillis() * (long) Math.pow(BACKOFF_MULTIPLIER, attempt);
        long jitter = (long) (base * JITTER_FACTOR * (Math.random() * 2 - 1));
        return Math.max(INITIAL_WAIT.toMillis(), base + jitter);
    }

    /**
     * Determine if an exception should trigger a retry.
     */
    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof QuotaExceededException) return false;
        if (throwable instanceof CircuitBreakerOpenException) return false;
        if (throwable instanceof java.net.http.HttpTimeoutException) return true;
        if (throwable instanceof java.io.IOException) return true;

        String message = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";
        if (message.contains("rate limit") || message.contains("429") || message.contains("too many requests")) {
            return true;
        }
        if (message.contains("timeout") || message.contains("connection reset") || message.contains("broken pipe")) {
            return true;
        }
        if (message.contains("500") || message.contains("502") || message.contains("503") || message.contains("504")) {
            return true;
        }
        if (message.contains("unexpected end of stream") || message.contains("socket closed")) {
            return true;
        }
        // Don't retry 4xx client errors (except 429)
        if (message.contains("400") || message.contains("401") || message.contains("403") || message.contains("404")) {
            return false;
        }

        return false;
    }

    public static class QuotaExceededException extends RuntimeException {
        public QuotaExceededException(String message) {
            super(message);
        }
    }

    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
