package com.supremeai.learning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LearningQuotaService - Manages learning resource consumption quotas.
 * Prevents runaway costs and resource exhaustion from aggressive scraping/learning.
 *
 * Supports both global admin limits and per-user limits.
 * Quotas reset daily (midnight UTC) by default.
 *
 * Configuration (application.properties):
 * - learning.quota.global.dailyMax=1000      (max learning ops per day globally)
 * - learning.quota.perUser.dailyMax=50       (max per user per day)
 * - learning.quota.scraper.siteVisitMax=10   (max site visits per day)
 * - learning.quota.emergency.globalThreshold=0.9 (pause learning at 90% used)
 */
@Service
public class LearningQuotaService {

    private static final Logger log = LoggerFactory.getLogger(LearningQuotaService.class);

    @Value("${learning.quota.global.dailyMax:1000}")
    private int globalDailyMax;

    @Value("${learning.quota.perUser.dailyMax:50}")
    private int perUserDailyMax;

    @Value("${learning.quota.scraper.siteVisitMax:10}")
    private int siteVisitMaxPerUser;

    @Value("${learning.quota.emergency.globalThreshold:0.9}")
    private double emergencyThreshold;

    // Daily counters (reset at midnight UTC)
    private final Map<String, AtomicInteger> userDailyCounters = new ConcurrentHashMap<>();
    private final AtomicInteger globalDailyCounter = new AtomicInteger(0);
    private LocalDateTime lastResetDate = LocalDateTime.now();

    /**
     * Check if a learning operation is within quota.
     *
     * @param userId User ID (or "system" for cron jobs)
     * @param operationType Type of operation (SCRAPE, LEARN, VALIDATE)
     * @param quotaUnits Number of quota units to consume
     * @return true if allowed, false if quota exceeded
     */
    public boolean checkQuota(String userId, String operationType, int quotaUnits) {
        ensureDailyReset();

        String userKey = userId != null ? userId : "anonymous";

        // Check per-user quota
        AtomicInteger userCounter = userDailyCounters.computeIfAbsent(
            userKey, k -> new AtomicInteger(0)
        );
        int userCurrent = userCounter.get();
        int userAfter = userCurrent + quotaUnits;

        if (userAfter > perUserDailyMax) {
            log.warn("[QUOTA] DENIED user={} op={} requested={} current={} limit={}",
                userKey, operationType, quotaUnits, userCurrent, perUserDailyMax);
            return false;
        }

        // Check global quota
        int globalCurrent = globalDailyCounter.get();
        int globalAfter = globalCurrent + quotaUnits;
        if (globalAfter > globalDailyMax) {
            log.warn("[QUOTA] DENIED user={} op={} GLOBAL_LIMIT_REACHED global={}/{} limit={}",
                userKey, operationType, globalAfter, globalDailyMax, globalDailyMax);
            return false;
        }

        // Emergency threshold check
        if (((double) globalAfter / globalDailyMax) >= emergencyThreshold) {
            log.error("[QUOTA] EMERGENCY: {}% of global quota used. Pausing learning recommended.",
                Math.round((globalAfter / (double) globalDailyMax) * 100));
            // Could trigger automatic pause
        }

        // All checks passed - consume quota
        userCounter.addAndGet(quotaUnits);
        globalDailyCounter.addAndGet(quotaUnits);

        log.debug("[QUOTA] APPROVED user={} op={} units={} userTotal={} globalTotal={}",
            userKey, operationType, quotaUnits, userCounter.get(), globalDailyCounter.get());
        return true;
    }

    /**
     * Check if a specific user can visit more sites (browser access control).
     */
    public boolean canVisitSite(String userId) {
        ensureDailyReset();
        String userKey = userId != null ? userId : "anonymous";
        AtomicInteger counter = userDailyCounters.computeIfAbsent(
            userKey, k -> new AtomicInteger(0)
        );
        return counter.get() < siteVisitMaxPerUser;
    }

    /**
     * Record a site visit (consumes 1 quota unit).
     */
    public void recordSiteVisit(String userId) {
        checkQuota(userId, "SITE_VISIT", 1);
    }

    /**
     * Get current quota usage statistics for admin dashboard.
     */
    public Map<String, Object> getQuotaStats() {
        ensureDailyReset();
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("globalDailyUsed", globalDailyCounter.get());
        stats.put("globalDailyMax", globalDailyMax);
        stats.put("globalPercentUsed", Math.round((globalDailyCounter.get() / (double) globalDailyMax) * 100));
        stats.put("perUserDailyMax", perUserDailyMax);
        stats.put("siteVisitMaxPerUser", siteVisitMaxPerUser);
        stats.put("lastReset", lastResetDate.toString());
        return stats;
    }

    /**
     * Reset all daily counters at midnight UTC.
     */
    private void ensureDailyReset() {
        LocalDateTime now = LocalDateTime.now();
        if (now.toLocalDate().isAfter(lastResetDate.toLocalDate())) {
            // New day - reset counters
            synchronized (this) {
                if (now.toLocalDate().isAfter(lastResetDate.toLocalDate())) {
                    log.info("[QUOTA] Daily reset: clearing {} user counters and global counter",
                        userDailyCounters.size());
                    userDailyCounters.clear();
                    globalDailyCounter.set(0);
                    lastResetDate = now;
                }
            }
        }
    }

    /**
     * Set per-user daily max (admin override).
     */
    public void setPerUserDailyMax(int newLimit) {
        this.perUserDailyMax = newLimit;
        log.info("[QUOTA] Admin updated perUserDailyMax to {}", newLimit);
    }

    /**
     * Set global daily max (admin override).
     */
    public void setGlobalDailyMax(int newLimit) {
        this.globalDailyMax = newLimit;
        log.info("[QUOTA] Admin updated globalDailyMax to {}", newLimit);
    }

    /**
     * Check if learning should be paused (emergency threshold exceeded).
     */
    public boolean isEmergencyThresholdExceeded() {
        ensureDailyReset();
        double percent = globalDailyCounter.get() / (double) globalDailyMax;
        return percent >= emergencyThreshold;
    }
}
