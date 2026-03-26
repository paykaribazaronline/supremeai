package org.example.exception;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Production-grade error handling with retry logic and circuit breaker
 * Handles transient vs permanent failures differently
 */
public class APIErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(APIErrorHandler.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 500;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    
    public APIErrorHandler(String apiName) {
        // Configure circuit breaker
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(30))
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(TransientException.class, TemporaryException.class)
                .ignoreExceptions(PermanentException.class)
                .build();
        
        this.circuitBreaker = CircuitBreaker.of(apiName, cbConfig);
        
        // Configure retry policy
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(MAX_RETRIES)
                .waitDuration(Duration.ofMillis(INITIAL_BACKOFF_MS))
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialBackoff(INITIAL_BACKOFF_MS, BACKOFF_MULTIPLIER))
                .retryOnException(throwable -> isRetryable(throwable))
                .build();
        
        this.retry = Retry.of(apiName, retryConfig);
    }
    
    /**
     * Execute API call with retry and circuit breaker protection
     * @param apiCall The API call supplier
     * @param apiName Name of API for logging
     * @return Result of API call
     * @throws TemporaryException if error cannot be recovered
     */
    public <T> T executeWithResilience(Supplier<T> apiCall, String apiName) 
            throws TemporaryException {
        try {
            // Apply circuit breaker then retry
            Supplier<T> resilientCall = CircuitBreaker.decorateSupplier(circuitBreaker,
                    Retry.decorateSupplier(retry, apiCall));
            
            T result = resilientCall.get();
            logger.info("API call succeeded: {}", apiName);
            return result;
            
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            logger.error("Circuit breaker OPEN for: {} - {}", apiName, e.getMessage());
            throw new TemporaryException("Circuit breaker open, service unavailable", e);
        } catch (Exception e) {
            logger.error("Unexpected error in API call: {} - {}", apiName, e.getMessage(), e);
            throw new TemporaryException("API call failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determine if error is retryable
     * Transient errors (timeout, 429) → retry
     * Permanent errors (401, 403) → don't retry
     */
    private static boolean isRetryable(Throwable throwable) {
        String message = throwable.getMessage() != null ? throwable.getMessage() : "";
        String cause = throwable.getCause() != null ? throwable.getCause().getMessage() : "";
        
        // Retry on these conditions
        if (message.contains("429") || message.contains("rate limit")) {
            return true; // Too Many Requests
        }
        if (message.contains("503") || message.contains("service unavailable")) {
            return true; // Service Unavailable
        }
        if (message.contains("timeout") || message.contains("deadline exceeded")) {
            return true; // Timeout
        }
        if (throwable instanceof java.net.ConnectException) {
            return true; // Connection error
        }
        if (throwable instanceof java.io.InterruptedIOException) {
            return true; // Interrupted
        }
        
        // Don't retry these
        if (message.contains("401") || message.contains("401 Unauthorized")) {
            return false; // Bad auth
        }
        if (message.contains("403") || message.contains("403 Forbidden")) {
            return false; // Forbidden
        }
        if (message.contains("400") || message.contains("Bad Request")) {
            return false; // Bad request
        }
        
        // Conservative: don't retry unknown errors
        return false;
    }
    
    // ========== CUSTOM EXCEPTIONS ==========
    
    /**
     * Temporary exception - can be retried
     */
    public static class TemporaryException extends Exception {
        public TemporaryException(String message) {
            super(message);
        }
        
        public TemporaryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Permanent exception - should NOT be retried
     */
    public static class PermanentException extends Exception {
        public PermanentException(String message) {
            super(message);
        }
        
        public PermanentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Transient exception - temporary failure, will retry
     */
    public static class TransientException extends TemporaryException {
        public TransientException(String message) {
            super(message);
        }
        
        public TransientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
