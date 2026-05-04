package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Retry service with exponential backoff and jitter.
 * Provides resilient retry logic for failed operations.
 */
@Service
public class RetryService {

    private static final Logger log = LoggerFactory.getLogger(RetryService.class);
    private static final Random random = new Random();

    /**
     * Execute with exponential backoff and jitter.
     */
    public <T> T executeWithRetry(Supplier<T> operation, 
                                   int maxRetries,
                                   long initialDelayMs,
                                   long maxDelayMs,
                                   double jitterFactor) {
        int attempts = 0;
        long delay = initialDelayMs;
        
        while (true) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempts++;
                if (attempts > maxRetries) {
                    log.error("Operation failed after {} attempts", maxRetries, e);
                    throw new RuntimeException("Operation failed after " + maxRetries + " attempts", e);
                }
                
                // Calculate delay with jitter
                long jitter = (long) (delay * jitterFactor * random.nextDouble());
                long actualDelay = Math.min(delay + jitter, maxDelayMs);
                
                log.warn("Attempt {} failed, retrying in {}ms", attempts, actualDelay, e);
                
                try {
                    TimeUnit.MILLISECONDS.sleep(actualDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
                
                // Exponential backoff
                delay = Math.min(delay * 2, maxDelayMs);
            }
        }
    }

    /**
     * Execute async operation with exponential backoff and jitter.
     */
    public <T> CompletableFuture<T> executeWithRetryAsync(
            Supplier<CompletableFuture<T>> operation,
            int maxRetries,
            long initialDelayMs,
            long maxDelayMs,
            double jitterFactor) {
        
        return CompletableFuture.supplyAsync(() -> {
            int attempts = 0;
            long delay = initialDelayMs;
            
            while (true) {
                try {
                    return operation.get().join();
                } catch (Exception e) {
                    attempts++;
                    if (attempts > maxRetries) {
                        log.error("Async operation failed after {} attempts", maxRetries, e);
                        throw new RuntimeException("Operation failed after " + maxRetries + " attempts", e);
                    }
                    
                    // Calculate delay with jitter
                    long jitter = (long) (delay * jitterFactor * random.nextDouble());
                    long actualDelay = Math.min(delay + jitter, maxDelayMs);
                    
                    log.warn("Async attempt {} failed, retrying in {}ms", attempts, actualDelay, e);
                    
                    try {
                        TimeUnit.MILLISECONDS.sleep(actualDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                    
                    // Exponential backoff
                    delay = Math.min(delay * 2, maxDelayMs);
                }
            }
        });
    }

    /**
     * Execute with full jitter strategy (random delay up to calculated backoff).
     * This is the most effective jitter strategy for avoiding thundering herd.
     */
    public <T> T executeWithFullJitter(Supplier<T> operation,
                                        int maxRetries,
                                        long initialDelayMs,
                                        long maxDelayMs) {
        int attempts = 0;
        long delay = initialDelayMs;
        
        while (true) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempts++;
                if (attempts > maxRetries) {
                    log.error("Operation failed after {} attempts", maxRetries, e);
                    throw new RuntimeException("Operation failed after " + maxRetries + " attempts", e);
                }
                
                // Full jitter: random delay between 0 and calculated delay
                long actualDelay = (long) (random.nextDouble() * delay);
                
                log.warn("Attempt {} failed, retrying in {}ms (full jitter)", attempts, actualDelay, e);
                
                try {
                    TimeUnit.MILLISECONDS.sleep(actualDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
                
                // Exponential backoff
                delay = Math.min(delay * 2, maxDelayMs);
            }
        }
    }
}