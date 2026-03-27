package org.example.selfhealing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Health Monitor for Services
 * 
 * Continuously monitors service health by:
 * 1. Tracking error rates and response times
 * 2. Detecting degraded/critical states
 * 3. Recording health history for analysis
 * 4. Triggering recovery when thresholds exceeded
 */
public class HealthMonitor {
    private static final Logger logger = LoggerFactory.getLogger(HealthMonitor.class);
    
    public enum HealthState {
        HEALTHY,      // Normal operation
        DEGRADED,     // Issues detected, still functional
        CRITICAL,     // Severe issues, failing
        RECOVERING    // In recovery process
    }
    
    private final String serviceName;
    private volatile HealthState state = HealthState.HEALTHY;
    
    // Metrics tracking
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalResponseTimeMs = new AtomicLong(0);
    private final AtomicInteger highResponseTimeCount = new AtomicInteger(0);
    
    // State transition tracking
    private final AtomicLong lastHealthCheckTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastRecoveryAttemptTime = new AtomicLong(0);
    
    // Health history (last 100 events)
    private final Queue<HealthEvent> healthHistory = new LinkedList<>();
    private static final int MAX_HISTORY_SIZE = 100;
    
    public HealthMonitor(String serviceName) {
        this.serviceName = serviceName;
        logger.debug("{}Created health monitor for service: {}", SelfHealingConfig.DEBUG_PREFIX, serviceName);
    }
    
    /**
     * Record a successful operation
     */
    public void recordSuccess(long responseTimeMs) {
        totalRequests.incrementAndGet();
        totalResponseTimeMs.addAndGet(responseTimeMs);
        consecutiveFailures.set(0);
        
        // Check if response time is high
        if (responseTimeMs > SelfHealingConfig.RESPONSE_TIME_THRESHOLD_MS) {
            highResponseTimeCount.incrementAndGet();
        } else {
            highResponseTimeCount.set(Math.max(0, highResponseTimeCount.get() - 1));
        }
        
        updateHealthState();
    }
    
    /**
     * Record a failed operation
     */
    public void recordFailure(long responseTimeMs) {
        totalRequests.incrementAndGet();
        totalResponseTimeMs.addAndGet(responseTimeMs);
        consecutiveFailures.incrementAndGet();
        failedRequests.incrementAndGet();
        
        updateHealthState();
    }
    
    /**
     * Update health state based on current metrics
     */
    private synchronized void updateHealthState() {
        long total = totalRequests.get();
        if (total == 0) return;
        
        double errorRate = (double) failedRequests.get() / total;
        int failures = consecutiveFailures.get();
        
        HealthState newState = state;
        
        if (failures >= SelfHealingConfig.HEALTH_CHECK_CRITICAL_THRESHOLD ||
            errorRate > SelfHealingConfig.ERROR_RATE_THRESHOLD * 2) {
            newState = HealthState.CRITICAL;
        } else if (failures >= SelfHealingConfig.HEALTH_CHECK_FAILURE_THRESHOLD ||
                   errorRate > SelfHealingConfig.ERROR_RATE_THRESHOLD) {
            newState = HealthState.DEGRADED;
        } else if (state == HealthState.RECOVERING) {
            // Gradually recover
            if (failures == 0 && errorRate < SelfHealingConfig.ERROR_RATE_THRESHOLD / 2) {
                newState = HealthState.HEALTHY;
            }
        } else {
            newState = HealthState.HEALTHY;
        }
        
        // Record state change
        if (newState != state) {
            recordHealthEvent(String.format("State transition: %s → %s (errors: %d, rate: %.1f%%)",
                state, newState, failures, errorRate * 100));
            logger.warn("{}Health state changed for {}: {} (consecutive failures: {})",
                SelfHealingConfig.WARNING_PREFIX, serviceName, newState, failures);
        }
        
        state = newState;
        lastHealthCheckTime.set(System.currentTimeMillis());
    }
    
    /**
     * Check if service needs manual intervention
     */
    public boolean needsIntervention() {
        return state == HealthState.CRITICAL;
    }
    
    /**
     * Check if service is in recovery mode
     */
    public boolean isRecovering() {
        return state == HealthState.RECOVERING;
    }
    
    /**
     * Mark service as recovering
     */
    public void markRecovering() {
        recordHealthEvent("Recovery initiated");
        state = HealthState.RECOVERING;
        lastRecoveryAttemptTime.set(System.currentTimeMillis());
    }
    
    /**
     * Record a health event
     */
    private void recordHealthEvent(String message) {
        synchronized (healthHistory) {
            healthHistory.offer(new HealthEvent(System.currentTimeMillis(), message));
            while (healthHistory.size() > MAX_HISTORY_SIZE) {
                healthHistory.poll();
            }
        }
    }
    
    /**
     * Get current health metrics
     */
    public HealthMetrics getMetrics() {
        long total = totalRequests.get();
        long failed = failedRequests.get();
        double errorRate = total > 0 ? (double) failed / total : 0;
        double avgResponseTime = total > 0 ? (double) totalResponseTimeMs.get() / total : 0;
        
        return new HealthMetrics(
            serviceName,
            state,
            total,
            failed,
            errorRate,
            avgResponseTime,
            highResponseTimeCount.get(),
            consecutiveFailures.get(),
            System.currentTimeMillis() - lastHealthCheckTime.get()
        );
    }
    
    /**
     * Get health history
     */
    public List<HealthEvent> getHistory() {
        synchronized (healthHistory) {
            return new ArrayList<>(healthHistory);
        }
    }
    
    /**
     * Reset metrics (for testing or after recovery)
     */
    public void reset() {
        totalRequests.set(0);
        failedRequests.set(0);
        totalResponseTimeMs.set(0);
        highResponseTimeCount.set(0);
        consecutiveFailures.set(0);
        state = HealthState.HEALTHY;
        recordHealthEvent("Metrics reset");
    }
    
    // ===== Getters =====
    
    public HealthState getState() {
        return state;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    public long getFailedRequests() {
        return failedRequests.get();
    }
    
    // ===== Inner Classes =====
    
    /**
     * Health metrics snapshot
     */
    public static class HealthMetrics {
        public final String serviceName;
        public final HealthState state;
        public final long totalRequests;
        public final long failedRequests;
        public final double errorRate;
        public final double avgResponseTimeMs;
        public final int highResponseTimeCount;
        public final int consecutiveFailures;
        public final long timeSinceLastCheckMs;
        
        public HealthMetrics(String serviceName, HealthState state, long totalRequests,
                           long failedRequests, double errorRate, double avgResponseTimeMs,
                           int highResponseTimeCount, int consecutiveFailures, long timeSinceLastCheckMs) {
            this.serviceName = serviceName;
            this.state = state;
            this.totalRequests = totalRequests;
            this.failedRequests = failedRequests;
            this.errorRate = errorRate;
            this.avgResponseTimeMs = avgResponseTimeMs;
            this.highResponseTimeCount = highResponseTimeCount;
            this.consecutiveFailures = consecutiveFailures;
            this.timeSinceLastCheckMs = timeSinceLastCheckMs;
        }
        
        @Override
        public String toString() {
            return String.format(
                "HealthMetrics{service='%s', state=%s, requests=%d, failed=%d, errorRate=%.2f%%, avgTime=%.0fms}",
                serviceName, state, totalRequests, failedRequests, errorRate * 100, avgResponseTimeMs);
        }
    }
    
    /**
     * Single health event in history
     */
    public static class HealthEvent {
        public final long timestamp;
        public final String message;
        
        public HealthEvent(long timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }
        
        @Override
        public String toString() {
            return String.format("[%d] %s", timestamp, message);
        }
    }
}
