package org.example.selfhealing;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Minimal RetryStrategy for SelfHealing
 */
public class RetryStrategy {
    public int maxAttempts = 3;
    public long initialDelayMs = 100;
    public long maxDelayMs = 5000;
    public double backoffMultiplier = 2.0;
    
    public RetryStrategy() {}
    
    public RetryStrategy(int maxAttempts, long initialDelayMs) {
        this.maxAttempts = maxAttempts;
        this.initialDelayMs = initialDelayMs;
    }
    
    public long getDelayForAttempt(int attemptNumber) {
        long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attemptNumber - 1));
        return Math.min(delay, maxDelayMs);
    }
}
