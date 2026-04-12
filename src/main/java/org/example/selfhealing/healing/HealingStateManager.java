package org.example.selfhealing.healing;

import com.google.cloud.firestore.Firestore;
import org.example.selfhealing.domain.HealingAttempt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Healing State Manager
 * 
 * Persists and retrieves healing attempt history.
 * Used for:
 * - Detecting repeated failures
 * - Learning which fixes work best
 * - Audit trail
 * - Analytics
 */
@Service
public class HealingStateManager {
    private static final Logger logger = LoggerFactory.getLogger(HealingStateManager.class);
    
    @Autowired(required = false)
    private Firestore firestore;
    
    private final String COLLECTION_NAME = "healing_attempts";
    
    // In-memory cache for fast access
    private final Map<String, HealingAttempt> attemptCache = new ConcurrentHashMap<>();
    private final Map<String, List<HealingAttempt>> workflowAttempts = new ConcurrentHashMap<>();
    
    /**
     * Record a new healing attempt
     */
    public void recordAttempt(HealingAttempt attempt) {
        try {
            // Store in memory cache
            attemptCache.put(attempt.getAttemptId(), attempt);
            workflowAttempts.computeIfAbsent(attempt.getWorkflowId(), k -> new ArrayList<>())
                    .add(attempt);
            
            logger.info("📝 Recorded healing attempt: {} for workflow: {}", 
                    attempt.getAttemptId(), attempt.getWorkflowId());
            
            // Persist to Firestore if available
            if (firestore != null) {
                firestore.collection(COLLECTION_NAME)
                        .document(attempt.getAttemptId())
                        .set(attempt)
                        .get();
            }
            
        } catch (Exception e) {
            logger.error("❌ Error recording healing attempt", e);
        }
    }
    
    /**
     * Check if the same error has failed multiple times before
     */
    public boolean isRepeatedFailure(String workflowId, String errorFingerprint) {
        try {
            List<HealingAttempt> attempts = workflowAttempts.getOrDefault(workflowId, 
                    Collections.emptyList());
            
            long failedAttempts = attempts.stream()
                    .filter(a -> a.getStatus() == HealingAttempt.HealingStatus.FAILED)
                    .filter(a -> errorFingerprint.equals(a.getErrorFingerprint()))
                    .count();
            
            return failedAttempts >= 2; // Same error failed 2+ times?
            
        } catch (Exception e) {
            logger.error("Error checking repeated failure", e);
            return false;
        }
    }
    
    /**
     * Get all healing attempts for a workflow
     */
    public List<HealingAttempt> getWorkflowHistory(String workflowId) {
        return new ArrayList<>(workflowAttempts.getOrDefault(workflowId, Collections.emptyList()));
    }
    
    /**
     * Get healing attempts in the last N minutes
     */
    public List<HealingAttempt> getRecentAttempts(int minutesBack) {
        Instant cutoff = Instant.now().minusSeconds((long) minutesBack * 60);
        
        return attemptCache.values().stream()
                .filter(a -> a.getCreatedAt().isAfter(cutoff))
                .collect(Collectors.toList());
    }
    
    /**
     * Get successful vs failed attempts (for watchdog monitoring)
     */
    public Map<String, Long> getAttemptStats(int minutesBack) {
        List<HealingAttempt> recent = getRecentAttempts(minutesBack);
        
        long successful = recent.stream()
                .filter(a -> a.getStatus() == HealingAttempt.HealingStatus.SUCCESS)
                .count();
        
        long failed = recent.stream()
                .filter(a -> a.getStatus() == HealingAttempt.HealingStatus.FAILED)
                .count();
        
        long escalated = recent.stream()
                .filter(a -> a.getStatus() == HealingAttempt.HealingStatus.ESCALATED)
                .count();
        
        return Map.of(
                "successful", successful,
                "failed", failed,
                "escalated", escalated,
                "total", (long) recent.size()
        );
    }
    
    /**
     * Mark an attempt as resolved
     */
    public void markResolved(String attemptId, HealingAttempt.HealingStatus status) {
        HealingAttempt attempt = attemptCache.get(attemptId);
        if (attempt != null) {
            attempt.setStatus(status);
            attempt.setResolvedAt(Instant.now());
            
            logger.info("✅ Marked healing attempt {} as {}", attemptId, status);
            
            // Update Firestore
            if (firestore != null) {
                try {
                    firestore.collection(COLLECTION_NAME)
                            .document(attemptId)
                            .set(attempt)
                            .get();
                } catch (Exception e) {
                    logger.error("Error updating Firestore", e);
                }
            }
        }
    }
    
    /**
     * Get most common error types (for learning)
     */
    public Map<String, Long> getMostCommonErrors(int count) {
        return attemptCache.values().stream()
                .collect(Collectors.groupingByConcurrent(
                        HealingAttempt::getErrorType,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(count)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Get best performing fix strategies
     */
    public Map<String, Double> getBestFixStrategies() {
        Map<String, List<HealingAttempt>> byStrategy = attemptCache.values().stream()
                .collect(Collectors.groupingByConcurrent(HealingAttempt::getFixStrategy));
        
        return byStrategy.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            long successful = e.getValue().stream()
                                    .filter(a -> a.getStatus() == HealingAttempt.HealingStatus.SUCCESS)
                                    .count();
                            return (double) successful / e.getValue().size();
                        }
                ))
                .entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }
}
