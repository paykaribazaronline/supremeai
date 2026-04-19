package com.supremeai.selfhealing;

import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;

/**
 * SelfHealingService - Provides simple retry logic with exponential backoff.
 * Taste phase implementation: minimal blocking retry for synchronous operations.
 */
@Service
public class SelfHealingService {

    /**
     * Execute a task with retry.
     *
     * @param task          the task to execute
     * @param maxAttempts   maximum number of attempts (>=1)
     * @param initialBackoff initial backoff in milliseconds
     * @param <T>           return type
     * @return task result
     * @throws Exception if all attempts fail
     */
    public <T> T executeWithRetry(Callable<T> task, int maxAttempts, long initialBackoff) throws Exception {
        if (maxAttempts < 1) maxAttempts = 1;
        long backoff = initialBackoff;
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return task.call();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                    backoff *= 2; // exponential backoff
                }
            }
        }
        throw lastException != null ? lastException : new IllegalStateException("Retry failed");
    }

    /**
     * Run a task with retry (void return).
     */
    public void runWithRetry(Runnable task, int maxAttempts, long backoffMs) throws Exception {
        executeWithRetry(() -> { task.run(); return null; }, maxAttempts, backoffMs);
    }
}
