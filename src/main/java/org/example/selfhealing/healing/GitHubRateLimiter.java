package org.example.selfhealing.healing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * GitHub Rate Limiter
 * 
 * Prevents hitting GitHub API rate limits by:
 * - Tracking requests per hour
 * - Token bucket algorithm (allows burst, but averages)
 * - Queuing excess requests
 * - Providing backpressure to callers
 * 
 * GitHub limits: 5,000 requests/hour for authenticated requests
 */
@Service
public class GitHubRateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(GitHubRateLimiter.class);
    
    private static final int MAX_REQUESTS_PER_HOUR = 4500; // Conservative (80% of limit)
    private static final int TOKENS_PER_HOUR = MAX_REQUESTS_PER_HOUR;
    private static final long HOUR_MILLIS = 3600000L;
    
    private volatile long tokensAvailable;
    private volatile Instant lastRefillTime;
    private final Object tokenLock = new Object();
    
    private final BlockingQueue<RateLimitedTask> taskQueue = new LinkedBlockingQueue<>(1000);
    private final ExecutorService asyncProcessor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "github-rate-limit-processor");
        t.setDaemon(true);
        return t;
    });
    
    // Track request metrics
    private volatile int requestsThisHour = 0;
    private volatile Instant hourStartTime = Instant.now();
    
    public GitHubRateLimiter() {
        this.tokensAvailable = TOKENS_PER_HOUR;
        this.lastRefillTime = Instant.now();
        
        // Start async task processor
        startAsyncProcessor();
        
        logger.info("✅ GitHub Rate Limiter initialized: {} requests/hour", MAX_REQUESTS_PER_HOUR);
    }
    
    /**
     * Try to execute a task immediately; if rate limit exceeded, queue it
     * 
     * @param taskName Identifier for logging
     * @param task The code to execute
     * @return true if executed immediately, false if queued
     */
    public boolean executeWithRateLimit(String taskName, Runnable task) {
        refillTokens();
        
        synchronized (tokenLock) {
            if (tokensAvailable > 0) {
                tokensAvailable--;
                requestsThisHour++;
                
                logger.debug("🟢 GitHub API call immediate: {} (tokens left: {}/{})",
                        taskName, tokensAvailable, TOKENS_PER_HOUR);
                
                try {
                    task.run();
                    return true;
                } catch (Exception e) {
                    logger.error("❌ Task {} failed", taskName, e);
                    return false;
                }
            }
        }
        
        // Rate limit exceeded, queue for later
        logger.warn("🟡 GitHub API call queued: {} (rate limit reached)", taskName);
        try {
            taskQueue.offer(new RateLimitedTask(taskName, task));
            return false;
        } catch (Exception e) {
            logger.error("❌ Failed to queue task: {}", taskName, e);
            return false;
        }
    }
    
    /**
     * Execute task with guaranteed limit (blocks if necessary)
     * Used for critical operations
     */
    public void executeBlocking(String taskName, Runnable task) throws InterruptedException {
        while (!executeWithRateLimit(taskName, task)) {
            // Wait for a token to become available
            synchronized (tokenLock) {
                if (tokensAvailable <= 0) {
                    long waitTime = getTimeUntilNextTokenMs();
                    logger.info("⏳ Waiting {} ms for rate limit token...", waitTime);
                    Thread.sleep(Math.min(waitTime, 5000)); // Max 5 sec sleep
                    refillTokens();
                }
            }
        }
    }
    
    /**
     * Refill tokens based on elapsed time
     * Token refill rate = TOKENS_PER_HOUR / 3600 seconds
     */
    private void refillTokens() {
        synchronized (tokenLock) {
            Instant now = Instant.now();
            long elapsedMs = Duration.between(lastRefillTime, now).toMillis();
            
            // Calculate refill amount (linear refill)
            double refillRate = (double) TOKENS_PER_HOUR / HOUR_MILLIS;
            int tokensToAdd = (int) (elapsedMs * refillRate);
            
            if (tokensToAdd > 0) {
                tokensAvailable = Math.min(
                        tokensAvailable + tokensToAdd,
                        TOKENS_PER_HOUR
                );
                lastRefillTime = now;
                
                // Reset hourly counter
                if (now.isAfter(hourStartTime.plus(Duration.ofHours(1)))) {
                    requestsThisHour = 0;
                    hourStartTime = now;
                }
            }
        }
    }
    
    /**
     * Get milliseconds until next token is available
     */
    private long getTimeUntilNextTokenMs() {
        synchronized (tokenLock) {
            if (tokensAvailable > 0) {
                return 0;
            }
            
            // Time to add 1 token
            double refillRate = (double) TOKENS_PER_HOUR / HOUR_MILLIS;
            double msPerToken = 1.0 / refillRate;
            return (long) msPerToken;
        }
    }
    
    /**
     * Start background processor for queued tasks
     */
    private void startAsyncProcessor() {
        asyncProcessor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    RateLimitedTask task = taskQueue.poll(5, TimeUnit.SECONDS);
                    
                    if (task != null) {
                        refillTokens();
                        
                        synchronized (tokenLock) {
                            if (tokensAvailable > 0) {
                                tokensAvailable--;
                                requestsThisHour++;
                                
                                logger.info("🟢 Executing queued GitHub API call: {}", task.name);
                                try {
                                    task.task.run();
                                } catch (Exception e) {
                                    logger.error("❌ Queued task {} failed", task.name, e);
                                }
                            } else {
                                // Re-queue if no tokens available
                                taskQueue.offer(task);
                                Thread.sleep(1000); // Backoff
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
    
    /**
     * Get current rate limit status
     */
    public Map<String, Object> getStatus() {
        refillTokens();
        
        synchronized (tokenLock) {
            return Map.of(
                    "tokensAvailable", tokensAvailable,
                    "maxTokens", TOKENS_PER_HOUR,
                    "requestsThisHour", requestsThisHour,
                    "queuedTasks", taskQueue.size(),
                    "utilizationPercent", (100.0 * requestsThisHour / TOKENS_PER_HOUR)
            );
        }
    }
    
    /**
     * Check if we're at risk of hitting rate limit
     */
    public boolean isAtRisk() {
        refillTokens();
        
        synchronized (tokenLock) {
            double utilizationPercent = (100.0 * requestsThisHour / TOKENS_PER_HOUR);
            return utilizationPercent > 90; // Warn at 90%
        }
    }
    
    /**
     * Get current queue size for monitoring
     */
    public int getQueueSize() {
        return taskQueue.size();
    }
    
    /**
     * Internal task wrapper
     */
    private static class RateLimitedTask {
        String name;
        Runnable task;
        
        RateLimitedTask(String name, Runnable task) {
            this.name = name;
            this.task = task;
        }
    }
    
    /**
     * Shutdown the processor
     */
    public void shutdown() {
        asyncProcessor.shutdownNow();
    }
}
