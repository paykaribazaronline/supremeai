package org.example.selfhealing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/**
 * Retry Strategy with Exponential Backoff and Jitter
 * 
 * Implements automated retry logic with:
 * 1. Exponential backoff: 100ms, 200ms, 400ms...
 * 2. Jitter: Add randomness to prevent thundering herd
 * 3. Max retry limit: Prevent infinite loops
 * 4. Selective retry: Only retry on transient failures
 */
public class RetryStrategy {
    private static final Logger logger = LoggerFactory.getLogger(RetryStrategy.class);
    
    private final String operationName;
    private final int maxAttempts;
    private final long initialDelayMs;
    private final double backoffMultiplier;
    private final long maxDelayMs;
    private final double jitterFactor;
    private final RandomGenerator random;
    
    public RetryStrategy(String operationName) {
        this(operationName,
             SelfHealingConfig.MAX_RETRY_ATTEMPTS,
             SelfHealingConfig.INITIAL_RETRY_DELAY_MS,
             SelfHealingConfig.RETRY_BACKOFF_MULTIPLIER,
             SelfHealingConfig.MAX_RETRY_DELAY_MS,
             SelfHealingConfig.RETRY_JITTER_FACTOR);
    }
    
    public RetryStrategy(String operationName, int maxAttempts, long initialDelayMs,
                        double backoffMultiplier, long maxDelayMs, double jitterFactor) {
        this.operationName = operationName;
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.maxDelayMs = maxDelayMs;
        this.jitterFactor = jitterFactor;
        this.random = RandomGeneratorFactory.getDefault().create(System.nanoTime());
    }
    
    /**
     * Execute operation with retry logic
     */
    public <T> T execute(Supplier<T> operation) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.debug("🔄 Retry attempt {}/{} for {}", attempt, maxAttempts, operationName);
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                
                // Check if we should retry this exception
                if (!isRetryable(e)) {
                    logger.warn("🚫 Non-retryable error for {}: {}", operationName, e.getMessage());
                    throw e;
                }
                
                // Calculate backoff delay
                if (attempt < maxAttempts) {
                    long delayMs = calculateBackoffDelay(attempt);
                    logger.info("⏳ {} failed (attempt {}/{}), retrying in {}ms: {}",
                        operationName, attempt, maxAttempts, delayMs, e.getMessage());
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new Exception("Retry interrupted for " + operationName, ie);
                    }
                }
            }
        }
        
        logger.error("❌ {} failed after {} attempts", operationName, maxAttempts);
        throw lastException;
    }
    
    /**
     * Determine if an exception is retryable (transient vs permanent)
     */
    private boolean isRetryable(Exception e) {
        // Always retry on timeout/connection errors
        if (e instanceof java.net.SocketTimeoutException ||
            e instanceof java.net.ConnectException ||
            e instanceof java.io.InterruptedIOException) {
            return true;
        }
        
        // Retry on specific HTTP status codes typical for transient errors
        if (e instanceof Exception && e.getMessage() != null) {
            String msg = e.getMessage().toLowerCase();
            return msg.contains("timeout") ||
                   msg.contains("connection refused") ||
                   msg.contains("connection reset") ||
                   msg.contains("temporarily unavailable") ||
                   msg.contains("service unavailable") ||
                   msg.contains("too many requests");
        }
        
        // Don't retry on IllegalArgumentException, auth errors, not found
        if (e instanceof IllegalArgumentException ||
            e instanceof SecurityException ||
            e instanceof UnsupportedOperationException) {
            return false;
        }
        
        // Default: retry other exceptions
        return true;
    }
    
    /**
     * Calculate backoff delay with exponential growth and jitter
     */
    private long calculateBackoffDelay(int attemptNumber) {
        // Exponential: delay = initialDelay * (multiplier ^ (attempt - 1))
        long exponentialDelay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attemptNumber - 1));
        
        // Cap at max delay
        long cappedDelay = Math.min(exponentialDelay, maxDelayMs);
        
        // Add jitter: randomness to prevent thundering herd
        long jitter = (long) (cappedDelay * jitterFactor * (random.nextDouble() * 2 - 1));
        long finalDelay = Math.max(1, cappedDelay + jitter);
        
        return finalDelay;
    }
    
    // ===== Getters =====
    
    public String getOperationName() {
        return operationName;
    }
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    @Override
    public String toString() {
        return String.format("RetryStrategy{operation='%s', maxAttempts=%d, initialDelay=%dms}",
            operationName, maxAttempts, initialDelayMs);
    }
}
