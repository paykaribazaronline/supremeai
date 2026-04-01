package org.example.resilience;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Callable;

/**
 * Retry Strategy Service
 * Implements exponential backoff and adaptive retry logic
 */
@Service
public class RetryStrategy {
    private static final Logger logger = LoggerFactory.getLogger(RetryStrategy.class);
    
    private final RetryRegistry registry;
    private final Map<String, RetryStats> stats = new ConcurrentHashMap<>();
    
    public RetryStrategy() {
        RetryConfig defaultConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .intervalFunction(io.github.resilience4j.core.IntervalFunction
                .ofExponentialBackoff(500, 2))  // Exponential backoff: 500ms, 1000ms, 2000ms
            .retryOnException(e -> !(e instanceof IllegalArgumentException))
            .build();
        
        this.registry = RetryRegistry.of(defaultConfig);
    }
    
    /**
     * Create or get retry policy with custom config
     */
    public Retry getOrCreateRetry(String name, int maxAttempts, long initialDelayMs, double multiplier) {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(maxAttempts)
            .waitDuration(Duration.ofMillis(initialDelayMs))
            .intervalFunction(io.github.resilience4j.core.IntervalFunction
                .ofExponentialBackoff(initialDelayMs, multiplier))
            .retryOnException(Exception::new)
            .build();
        
        return registry.retry(name, config);
    }
    
    /**
     * Execute with adaptive retry
     */
    public <T> T executeWithRetry(String name, java.util.function.Callable<T> callable) throws Exception {
        Retry retry = getOrCreateRetry(name, 3, 500, 2.0);
        RetryStats stat = stats.computeIfAbsent(name, k -> new RetryStats(name));
        
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < 3) {
            attempt++;
            long startTime = System.currentTimeMillis();
            
            try {
                T result = retry.executeCallable(callable);
                long duration = System.currentTimeMillis() - startTime;
                stat.recordSuccess(attempt, duration);
                logger.info("✅ Retry succeeded on attempt {} for {}", attempt, name);
                return result;
            } catch (Exception e) {
                lastException = e;
                long duration = System.currentTimeMillis() - startTime;
                stat.recordFailure(attempt, duration);
                
                if (attempt < 3) {
                    long backoffMs = (long) (500 * Math.pow(2, attempt - 1));
                    logger.warn("⚠️ Attempt {} failed, retrying after {}ms: {}", attempt, backoffMs, e.getMessage());
                    Thread.sleep(backoffMs);
                } else {
                    logger.error("❌ All {} retry attempts failed for {}", attempt, name);
                }
            }
        }
        
        throw lastException;
    }
    
    /**
     * Get retry statistics
     */
    public Map<String, Object> getRetryStats(String name) {
        RetryStats stat = stats.get(name);
        if (stat == null) {
            return null;
        }
        
        return new LinkedHashMap<String, Object>() {{
            put("name", name);
            put("total_executions", stat.getTotalExecutions());
            put("successful_executions", stat.getSuccessfulExecutions());
            put("failed_executions", stat.getFailedExecutions());
            put("success_rate", stat.getSuccessRate());
            put("avg_attempts", stat.getAverageAttempts());
            put("avg_duration_ms", stat.getAverageDuration());
            put("first_attempt_success_rate", stat.getFirstAttemptSuccessRate());
        }};
    }
    
    /**
     * Get all retry statistics
     */
    public Map<String, Map<String, Object>> getAllRetryStats() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        stats.forEach((name, stat) -> {
            result.put(name, getRetryStats(name));
        });
        return result;
    }
    
    // Inner class for retry statistics
    private static class RetryStats {
        private final String name;
        private long totalExecutions = 0;
        private long successfulExecutions = 0;
        private long failedExecutions = 0;
        private long totalAttempts = 0;
        private long totalDuration = 0;
        private long firstAttemptSuccesses = 0;
        
        public RetryStats(String name) {
            this.name = name;
        }
        
        public synchronized void recordSuccess(int attempts, long duration) {
            totalExecutions++;
            successfulExecutions++;
            totalAttempts += attempts;
            totalDuration += duration;
            if (attempts == 1) {
                firstAttemptSuccesses++;
            }
        }
        
        public synchronized void recordFailure(int attempts, long duration) {
            totalExecutions++;
            failedExecutions++;
            totalAttempts += attempts;
            totalDuration += duration;
        }
        
        public synchronized double getSuccessRate() {
            return totalExecutions > 0 ? (successfulExecutions * 100.0) / totalExecutions : 0;
        }
        
        public synchronized double getFirstAttemptSuccessRate() {
            return successfulExecutions > 0 ? (firstAttemptSuccesses * 100.0) / successfulExecutions : 0;
        }
        
        public synchronized double getAverageAttempts() {
            return totalExecutions > 0 ? (double) totalAttempts / totalExecutions : 0;
        }
        
        public synchronized long getAverageDuration() {
            return totalExecutions > 0 ? totalDuration / totalExecutions : 0;
        }
        
        public long getTotalExecutions() { return totalExecutions; }
        public long getSuccessfulExecutions() { return successfulExecutions; }
        public long getFailedExecutions() { return failedExecutions; }
    }
}
