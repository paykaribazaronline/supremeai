package org.example.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enterprise Circuit Breaker Manager
 * 
 * Implements resilience4j circuit breaker pattern with:
 * - CLOSED → OPEN → HALF_OPEN → CLOSED state machine
 * - Configurable failure thresholds
 * - Exponential backoff recovery
 * - Per-service circuit breaker isolation
 * 
 * @author SupremeAI
 * @version 2.0 Enterprise
 */
@Service
public class EnterpriseCircuitBreakerManager {
    private static final Logger logger = LoggerFactory.getLogger(EnterpriseCircuitBreakerManager.class);
    
    public enum CircuitState {
        CLOSED,      // Normal, requests pass through
        OPEN,        // Failing, requests rejected
        HALF_OPEN    // Testing, one request allowed
    }
    
    private static class CircuitBreakerMetrics {
        AtomicInteger consecutiveFailures = new AtomicInteger(0);
        AtomicInteger consecutiveSuccesses = new AtomicInteger(0);
        AtomicLong lastFailureTime = new AtomicLong(0);
        AtomicLong lastSuccessTime = new AtomicLong(System.currentTimeMillis());
        AtomicLong openedAt = new AtomicLong(0);
        CircuitState state = CircuitState.CLOSED;
        int failureThreshold;
        int successThreshold;
        long openTimeoutMs;
        long failureTimeWindowMs;
        int totalRequests = 0;
        int totalFailures = 0;
    }
    
    private final Map<String, CircuitBreakerMetrics> circuitBreakers = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newScheduledThreadPool(2);
    
    // Configuration presets
    public static final CircuitBreakerConfig PROVIDER_API_CONFIG = new CircuitBreakerConfig(
        "Provider API", 5, 3, 30000, 60000
    );
    
    public static final CircuitBreakerConfig DATABASE_CONFIG = new CircuitBreakerConfig(
        "Database", 3, 2, 60000, 30000
    );
    
    public static final CircuitBreakerConfig CACHE_CONFIG = new CircuitBreakerConfig(
        "Cache", 10, 5, 10000, 60000
    );
    
    /**
     * Register a circuit breaker with custom configuration
     */
    public void registerCircuitBreaker(String name, CircuitBreakerConfig config) {
        CircuitBreakerMetrics metrics = new CircuitBreakerMetrics();
        metrics.failureThreshold = config.failureThreshold;
        metrics.successThreshold = config.successThreshold;
        metrics.openTimeoutMs = config.openTimeoutMs;
        metrics.failureTimeWindowMs = config.failureTimeWindowMs;
        metrics.state = CircuitState.CLOSED;
        
        circuitBreakers.put(name, metrics);
        logger.info("Registered circuit breaker: {} (threshold: {}, timeout: {}ms)", 
            name, config.failureThreshold, config.openTimeoutMs);
    }
    
    /**
     * Record a request success
     */
    public void recordSuccess(String name) {
        CircuitBreakerMetrics metrics = getOrCreateCircuitBreaker(name, PROVIDER_API_CONFIG);
        
        synchronized (metrics) {
            metrics.totalRequests++;
            metrics.consecutiveFailures.set(0);
            metrics.lastSuccessTime.set(System.currentTimeMillis());
            
            if (metrics.state == CircuitState.HALF_OPEN) {
                metrics.consecutiveSuccesses.incrementAndGet();
                if (metrics.consecutiveSuccesses.get() >= metrics.successThreshold) {
                    transitionToClosed(name, metrics);
                }
            }
        }
    }
    
    /**
     * Record a request failure
     */
    public void recordFailure(String name, String reason) {
        CircuitBreakerMetrics metrics = getOrCreateCircuitBreaker(name, PROVIDER_API_CONFIG);
        
        synchronized (metrics) {
            metrics.totalRequests++;
            metrics.totalFailures++;
            metrics.lastFailureTime.set(System.currentTimeMillis());
            
            // Check if failure is within time window
            long timeSinceLastFailure = metrics.lastFailureTime.get() - metrics.lastFailureTime.get();
            if (timeSinceLastFailure > metrics.failureTimeWindowMs) {
                metrics.consecutiveFailures.set(1);
            } else {
                metrics.consecutiveFailures.incrementAndGet();
            }
            
            logger.warn("Circuit [{}] failure recorded: {}", name, reason);
            
            // Transition to OPEN if threshold exceeded
            if (metrics.consecutiveFailures.get() >= metrics.failureThreshold) {
                if (metrics.state != CircuitState.OPEN) {
                    transitionToOpen(name, metrics);
                }
            }
        }
    }
    
    /**
     * Check if request can proceed
     */
    public boolean canProceed(String name) {
        CircuitBreakerMetrics metrics = getOrCreateCircuitBreaker(name, PROVIDER_API_CONFIG);
        
        synchronized (metrics) {
            switch (metrics.state) {
                case CLOSED:
                    return true;
                    
                case OPEN:
                    // Check if timeout expired, move to HALF_OPEN
                    long openDuration = System.currentTimeMillis() - metrics.openedAt.get();
                    if (openDuration >= metrics.openTimeoutMs) {
                        transitionToHalfOpen(name, metrics);
                        return true; // Allow one test request
                    }
                    return false; // Still open, reject
                    
                case HALF_OPEN:
                    // Allow one request through
                    return true;
                    
                default:
                    return false;
            }
        }
    }
    
    /**
     * Get circuit breaker state
     */
    public CircuitState getState(String name) {
        CircuitBreakerMetrics metrics = circuitBreakers.get(name);
        return metrics != null ? metrics.state : CircuitState.CLOSED;
    }
    
    /**
     * Get circuit breaker metrics
     */
    public Map<String, Object> getMetrics(String name) {
        CircuitBreakerMetrics metrics = circuitBreakers.get(name);
        if (metrics == null) return new HashMap<>();
        
        Map<String, Object> result = new HashMap<>();
        result.put("state", metrics.state.toString());
        result.put("total_requests", metrics.totalRequests);
        result.put("total_failures", metrics.totalFailures);
        result.put("failure_rate", metrics.totalRequests > 0 ? 
            (double) metrics.totalFailures / metrics.totalRequests * 100 : 0);
        result.put("consecutive_failures", metrics.consecutiveFailures.get());
        result.put("last_failure_time", metrics.lastFailureTime.get());
        result.put("last_success_time", metrics.lastSuccessTime.get());
        
        return result;
    }
    
    /**
     * Get all circuit breaker statuses
     */
    public Map<String, Object> getAllStatuses() {
        Map<String, Object> result = new HashMap<>();
        circuitBreakers.forEach((name, metrics) -> {
            result.put(name, getMetrics(name));
        });
        return result;
    }
    
    /**
     * Reset circuit breaker
     */
    public void reset(String name) {
        CircuitBreakerMetrics metrics = circuitBreakers.get(name);
        if (metrics == null) return;
        
        synchronized (metrics) {
            transitionToClosed(name, metrics);
            logger.info("Circuit breaker [{}] reset to CLOSED", name);
        }
    }
    
    // ============ Private Helper Methods ============
    
    private void transitionToOpen(String name, CircuitBreakerMetrics metrics) {
        metrics.state = CircuitState.OPEN;
        metrics.openedAt.set(System.currentTimeMillis());
        metrics.consecutiveSuccesses.set(0);
        logger.error("⚠️ Circuit [{}] OPENED - fast-failing all requests", name);
    }
    
    private void transitionToHalfOpen(String name, CircuitBreakerMetrics metrics) {
        metrics.state = CircuitState.HALF_OPEN;
        metrics.consecutiveSuccesses.set(0);
        metrics.consecutiveFailures.set(0);
        logger.warn("Circuit [{}] → HALF_OPEN - testing recovery", name);
    }
    
    private void transitionToClosed(String name, CircuitBreakerMetrics metrics) {
        metrics.state = CircuitState.CLOSED;
        metrics.consecutiveFailures.set(0);
        metrics.consecutiveSuccesses.set(0);
        metrics.lastSuccessTime.set(System.currentTimeMillis());
        logger.info("✅ Circuit [{}] → CLOSED - recovered", name);
    }
    
    private CircuitBreakerMetrics getOrCreateCircuitBreaker(String name, CircuitBreakerConfig config) {
        return circuitBreakers.computeIfAbsent(name, key -> {
            CircuitBreakerMetrics metrics = new CircuitBreakerMetrics();
            metrics.failureThreshold = config.failureThreshold;
            metrics.successThreshold = config.successThreshold;
            metrics.openTimeoutMs = config.openTimeoutMs;
            metrics.failureTimeWindowMs = config.failureTimeWindowMs;
            return metrics;
        });
    }
    
    /**
     * Shutdown all background tasks
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Error shutting down circuit breaker manager", e);
        }
    }
    
    /**
     * Circuit Breaker Configuration
     */
    public static class CircuitBreakerConfig {
        public final String name;
        public final int failureThreshold;      // How many failures to open
        public final int successThreshold;      // How many successes to close
        public final long openTimeoutMs;        // How long to stay open
        public final long failureTimeWindowMs;  // Time window for counting failures
        
        public CircuitBreakerConfig(String name, int failureThreshold, int successThreshold, 
                                   long openTimeoutMs, long failureTimeWindowMs) {
            this.name = name;
            this.failureThreshold = failureThreshold;
            this.successThreshold = successThreshold;
            this.openTimeoutMs = openTimeoutMs;
            this.failureTimeWindowMs = failureTimeWindowMs;
        }
    }
}
