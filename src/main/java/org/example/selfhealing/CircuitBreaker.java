package org.example.selfhealing;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Circuit Breaker Pattern Implementation
 * 
 * Prevents cascading failures by:
 * 1. CLOSED state: Allows requests, tracks failures
 * 2. OPEN state: Fails fast, prevents overwhelming failed service
 * 3. HALF_OPEN state: Tests if service has recovered
 * 
 * Transitions:
 * - CLOSED → OPEN: After N consecutive failures
 * - OPEN → HALF_OPEN: After timeout period
 * - HALF_OPEN → CLOSED: If test succeeds
 * - HALF_OPEN → OPEN: If test fails
 */
public class CircuitBreaker {
    
    public enum State {
        CLOSED,      // Normal operation
        OPEN,        // Fail fast
        HALF_OPEN    // Testing recovery
    }
    
    private final String name;
    private final int failureThreshold;
    private final long timeoutMs;
    private final int successThreshold;
    
    private volatile State state = State.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong openedAt = new AtomicLong(0);
    
    public CircuitBreaker(String name) {
        this(name, 
             SelfHealingConfig.CIRCUIT_BREAKER_FAILURE_THRESHOLD,
             SelfHealingConfig.CIRCUIT_BREAKER_TIMEOUT_MS,
             SelfHealingConfig.CIRCUIT_BREAKER_SUCCESS_THRESHOLD);
    }
    
    public CircuitBreaker(String name, int failureThreshold, long timeoutMs, int successThreshold) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.timeoutMs = timeoutMs;
        this.successThreshold = successThreshold;
    }
    
    /**
     * Execute operation with circuit breaker protection
     */
    public <T> T execute(Supplier<T> operation) throws Exception {
        State currentState = state;
        
        switch (currentState) {
            case CLOSED:
                return executeClosed(operation);
            case OPEN:
                return executeOpen();
            case HALF_OPEN:
                return executeHalfOpen(operation);
            default:
                throw new IllegalStateException("Unknown state: " + currentState);
        }
    }
    
    /**
     * Execute in CLOSED state - normal operation
     */
    private <T> T executeClosed(Supplier<T> operation) throws Exception {
        try {
            T result = operation.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }
    
    /**
     * Execute in OPEN state - fail fast
     */
    private <T> T executeOpen() throws Exception {
        long now = System.currentTimeMillis();
        long timeSinceOpened = now - openedAt.get();
        
        // If timeout has passed, try half-open state
        if (timeSinceOpened >= timeoutMs) {
            state = State.HALF_OPEN;
            successCount.set(0);
            return null; // Caller should retry
        }
        
        throw new CircuitBreakerOpenException(
            String.format("%s - Circuit breaker OPEN. Retry in %dms",
                name, timeoutMs - timeSinceOpened));
    }
    
    /**
     * Execute in HALF_OPEN state - test recovery
     */
    private <T> T executeHalfOpen(Supplier<T> operation) throws Exception {
        try {
            T result = operation.get();
            onHalfOpenSuccess();
            return result;
        } catch (Exception e) {
            onHalfOpenFailure();
            throw e;
        }
    }
    
    /**
     * Called on successful operation
     */
    private synchronized void onSuccess() {
        failureCount.set(0);
        
        if (state == State.HALF_OPEN) {
            successCount.incrementAndGet();
        }
    }
    
    /**
     * Called on failed operation
     */
    private synchronized void onFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        int failures = failureCount.incrementAndGet();
        
        if (failures >= failureThreshold && state == State.CLOSED) {
            state = State.OPEN;
            openedAt.set(System.currentTimeMillis());
        }
    }
    
    /**
     * Called on successful operation in HALF_OPEN state
     */
    private synchronized void onHalfOpenSuccess() {
        successCount.incrementAndGet();
        
        if (successCount.get() >= successThreshold) {
            state = State.CLOSED;
            failureCount.set(0);
        }
    }
    
    /**
     * Called on failed operation in HALF_OPEN state
     */
    private synchronized void onHalfOpenFailure() {
        state = State.OPEN;
        openedAt.set(System.currentTimeMillis());
        failureCount.set(0);
        successCount.set(0);
    }
    
    // ===== Getters =====
    
    public State getState() {
        return state;
    }
    
    public boolean isClosed() {
        return state == State.CLOSED;
    }
    
    public boolean isOpen() {
        return state == State.OPEN;
    }
    
    public boolean isHalfOpen() {
        return state == State.HALF_OPEN;
    }
    
    public int getFailureCount() {
        return failureCount.get();
    }
    
    public int getSuccessCount() {
        return successCount.get();
    }
    
    public long getLastFailureTime() {
        return lastFailureTime.get();
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return String.format("CircuitBreaker{name='%s', state=%s, failures=%d, successes=%d}",
            name, state, failureCount.get(), successCount.get());
    }
    
    /**
     * Exception thrown when circuit breaker is OPEN
     */
    public static class CircuitBreakerOpenException extends Exception {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
