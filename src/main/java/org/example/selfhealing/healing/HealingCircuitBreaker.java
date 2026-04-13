package org.example.selfhealing.healing;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Healing Circuit Breaker
 * 
 * Prevents infinite loops by limiting retry attempts and detecting repeated failures.
 * 
 * Pattern:
 * - CLOSED: Normal operation, allow retries
 * - OPEN: Too many consecutive failures, escalate to human
 * - HALF_OPEN: Waiting before trying again
 */
@Service
public class HealingCircuitBreaker {
    private static final Logger logger = LoggerFactory.getLogger(HealingCircuitBreaker.class);
    
    private final int MAX_CONSECUTIVE_FAILURES = 3;
    
    // Track failures per workflow
    private final Map<String, FailureTracker> failureTrackers = new ConcurrentHashMap<>();
    
    /**
     * Check if we should attempt a fix for this workflow
     * 
     * @param workflowId The workflow that failed
     * @param errorFingerprint Hash of the error (to detect repeated errors)
     * @return true if we should try to fix it, false if escalate
     */
    public boolean shouldAttemptFix(String workflowId, String errorFingerprint) {
        FailureTracker tracker = failureTrackers.computeIfAbsent(
                workflowId, 
                k -> new FailureTracker(workflowId)
        );
        
        // Check if we're in cooldown period
        if (tracker.isInCooldown()) {
            logger.warn("🔴 Circuit breaker OPEN for {}: In cooldown period until {}", 
                    workflowId, tracker.cooldownUntil);
            return false;
        }
        
        // Check if same error repeated (infinite loop detection)
        if (errorFingerprint != null && isSameErrorRepeated(tracker, errorFingerprint)) {
            tracker.consecutiveFailures++;
            logger.warn("🔄 Same error repeated for {}: {} consecutive failures",
                    workflowId, tracker.consecutiveFailures);
        } else if (errorFingerprint != null) {
            tracker.lastErrorFingerprint = errorFingerprint;
            tracker.consecutiveFailures = 0; // Reset for new error type
        }
        
        // Check if exceeded max retries
        if (tracker.consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            logger.error("❌ Circuit breaker OPENING for {}: Max retries ({}) exceeded",
                    workflowId, MAX_CONSECUTIVE_FAILURES);
            tracker.openCircuit(); // Enter cooldown
            return false;
        }
        
        return true;
    }
    
    /**
     * Record a successful fix attempt
     */
    public void recordSuccess(String workflowId) {
        FailureTracker tracker = failureTrackers.get(workflowId);
        if (tracker != null) {
            tracker.consecutiveFailures = 0;
            tracker.lastErrorFingerprint = null;
            logger.info("✅ Circuit breaker reset for {}: Fix was successful", workflowId);
        }
    }
    
    /**
     * Record a failed fix attempt
     */
    public void recordFailure(String workflowId) {
        FailureTracker tracker = failureTrackers.computeIfAbsent(
                workflowId,
                k -> new FailureTracker(workflowId)
        );
        tracker.consecutiveFailures++;
        logger.warn("⚠️ Circuit breaker: Failure #{} for {}", 
                tracker.consecutiveFailures, workflowId);
    }
    
    /**
     * Check if the same error is being repeated
     */
    private boolean isSameErrorRepeated(FailureTracker tracker, String errorFingerprint) {
        return tracker.lastErrorFingerprint != null && 
               tracker.lastErrorFingerprint.equals(errorFingerprint);
    }
    
    /**
     * Get circuit breaker status for monitoring
     */
    public Map<String, Object> getStatus(String workflowId) {
        FailureTracker tracker = failureTrackers.get(workflowId);
        if (tracker == null) {
            return Map.of("status", "UNKNOWN", "workflowId", workflowId);
        }
        
        String status = tracker.isInCooldown() ? "OPEN" : "CLOSED";
        return Map.of(
                "status", status,
                "workflowId", workflowId,
                "consecutiveFailures", tracker.consecutiveFailures,
                "maxConsecutiveFailures", MAX_CONSECUTIVE_FAILURES,
                "isCooldown", tracker.isInCooldown(),
                "cooldownUntil", tracker.cooldownUntil
        );
    }
    
    /**
     * Internal failure tracker for a single workflow
     */
    private static class FailureTracker {
        int consecutiveFailures = 0;
        String lastErrorFingerprint = null;
        Instant cooldownUntil = null;
        
        FailureTracker(String workflowId) {
            // workflowId parameter kept for backward compatibility
        }
        
        void openCircuit() {
            this.cooldownUntil = Instant.now().plus(Duration.ofMinutes(30));
        }
        
        boolean isInCooldown() {
            if (cooldownUntil == null) return false;
            if (Instant.now().isAfter(cooldownUntil)) {
                cooldownUntil = null; // Reset cooldown
                return false;
            }
            return true;
        }
    }
}
