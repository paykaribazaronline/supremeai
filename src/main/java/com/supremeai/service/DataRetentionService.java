package com.supremeai.service;

import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.repository.ChatHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Data Retention & Cleanup Service
 * 
 * PERMANENT DATA (NEVER DELETED):
 * - System Learning patterns
 * - Admin configuration changes
 * - User tier and quota settings
 * - Audit logs for security events
 * 
 * TEMPORARY DATA (AUTO-CLEANED):
 * - Guest session data (after 24h)
 * - Chat history (after 7-30 days)
 * - Activity logs (after 90 days)
 * - Temporary session tokens
 * - Performance metrics (after 30 days)
 */
@Service
public class DataRetentionService {

    private static final Logger logger = LoggerFactory.getLogger(DataRetentionService.class);

    @Autowired
    private SystemLearningRepository systemLearningRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    /**
     * 🔴 NEVER DELETE SYSTEM LEARNING DATA
     * This is the permanent knowledge base that makes the system smarter over time
     */
    public boolean isPermanentData(String collection) {
        return switch (collection) {
            case "system_learning", "config", "tiers", "quotas", "audit", "providers" -> true;
            default -> false;
        };
    }

    /**
     * Cleanup temporary guest session data - runs every hour
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupGuestSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        logger.info("Cleaning up guest sessions older than: {}", cutoff);
        
        chatHistoryRepository.deleteByIsGuestTrueAndCreatedAtBefore(cutoff)
            .doOnSuccess(count -> logger.info("Cleaned up {} guest chat sessions", count))
            .subscribe();
    }

    /**
     * Cleanup activity logs - runs daily at 3AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupActivityLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        logger.info("Cleaning up activity logs older than: {}", cutoff);
        
        activityLogRepository.deleteByCreatedAtBefore(cutoff)
            .doOnSuccess(count -> logger.info("Cleaned up {} old activity logs", count))
            .subscribe();
    }

    /**
     * Cleanup old chat history - runs weekly
     */
    @Scheduled(cron = "0 0 4 * * SUN")
    public void cleanupOldChatHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        logger.info("Cleaning up chat history older than: {}", cutoff);
        
        chatHistoryRepository.deleteByCreatedAtBefore(cutoff)
            .doOnSuccess(count -> logger.info("Cleaned up {} old chat entries", count))
            .subscribe();
    }

    /**
     * Explicitly mark data for permanent retention
     */
    public void markAsPermanent(String id) {
        systemLearningRepository.findById(id)
            .doOnNext(entry -> {
                entry.setPermanent(true);
                systemLearningRepository.save(entry).subscribe();
            })
            .subscribe();
    }

    /**
     * System learning data is always permanent
     */
    public void ensureLearningPermanence() {
        systemLearningRepository.findAll()
            .filter(entry -> !entry.isPermanent())
            .doOnNext(entry -> {
                entry.setPermanent(true);
                systemLearningRepository.save(entry).subscribe();
            })
            .count()
            .doOnSuccess(count -> logger.info("Marked {} learning entries as permanent", count))
            .subscribe();
    }
}
