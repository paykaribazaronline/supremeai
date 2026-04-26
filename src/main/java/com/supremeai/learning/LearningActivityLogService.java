package com.supremeai.learning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * LearningActivityLogService - Centralized logging for all learning-related activities.
 * Critical for Phase 1 debugging and Phase 3 error analysis.
 *
 * Logs:
 * - Site access attempts (success/failure)
 * - Scraping operations (sources, item counts, errors)
 * - Learning proposals (submitted, approved, rejected)
 * - Solution versioning (create, update, obsolete)
 * - Quota consumption
 *
 * These logs feed into:
 * - Error analysis (Phase 3)
 * - Pattern recognition (Phase 2)
 * - Admin dashboard metrics
 */
@Service
public class LearningActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(LearningActivityLogService.class);

    /**
     * Log a site access attempt.
     */
    public void logSiteAccess(String url, boolean granted, String reason, String adminUser) {
        String logEntry = String.format(
            "[SITE_ACCESS] url=%s granted=%b reason=%s admin=%s timestamp=%s",
            url, granted, reason, adminUser, LocalDateTime.now()
        );
        if (granted) {
            log.info(logEntry);
        } else {
            log.warn(logEntry);
        }
        // Could also persist to Firestore 'learning_activity_log' collection
    }

    /**
     * Log a scraping session result.
     */
    public void logScrapingSession(String source, int itemsCollected, long durationMs, boolean success) {
        log.info("[SCRAPING] source={} items={} durationMs={} success={} timestamp={}",
            source, itemsCollected, durationMs, success, LocalDateTime.now());
    }

    /**
     * Log a learning proposal lifecycle event.
     */
    public void logProposalEvent(String proposalId, String eventType, String details, String adminUser) {
        log.info("[PROPOSAL] id={} event={} details={} admin={} timestamp={}",
            proposalId, eventType, details, adminUser, LocalDateTime.now());
    }

    /**
     * Log solution lifecycle event (create, update, obsolete, rollback).
     */
    public void logSolutionEvent(String solutionId, String eventType, String details) {
        log.info("[SOLUTION] id={} event={} details={} timestamp={}",
            solutionId, eventType, details, LocalDateTime.now());
    }

    /**
     * Log quota consumption for learning operations.
     */
    public void logQuotaUsage(String userId, String operation, int unitsConsumed, int remainingQuota) {
        log.debug("[QUOTA] userId={} operation={} consumed={} remaining={}",
            userId, operation, unitsConsumed, remainingQuota);
    }

    /**
     * Log content sanitization decision.
     */
    public void logSanitization(String source, String contentHash, boolean approved, String reason) {
        if (approved) {
            log.info("[SANITIZE] source={} hash={} result=APPROVED", source, contentHash);
        } else {
            log.warn("[SANITIZE] source={} hash={} result=REJECTED reason={}", source, contentHash, reason);
        }
    }

    /**
     * Generate a unique tracking ID for a learning session.
     */
    public String generateSessionId() {
        return "learn_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
