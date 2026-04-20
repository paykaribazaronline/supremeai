package com.supremeai.service;

import com.supremeai.model.UserTier;
import com.supremeai.service.quota.QuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Guest Quota Service
 * Manages anonymous guest usage limits without requiring API keys
 * Quota limits are configurable by admin via SystemConfig
 */
@Service
public class GuestQuotaService {

    private static final Logger logger = LoggerFactory.getLogger(GuestQuotaService.class);

    @Autowired
    private ConfigService configService;

    private final ConcurrentHashMap<String, AtomicLong> guestUsage = new ConcurrentHashMap<>();
    private LocalDate currentUsageDay = LocalDate.now();

    /**
     * Check if guest has remaining quota
     * @param guestIdentifier IP address or session identifier
     */
    public boolean hasQuotaRemaining(String guestIdentifier) {
        resetDailyIfNeeded();
        
        long usage = guestUsage.getOrDefault(guestIdentifier, new AtomicLong(0)).get();
        long guestQuota = configService.getQuotaForTier(UserTier.GUEST);
        
        return usage < guestQuota;
    }

    /**
     * Increment guest usage count
     * @return true if successful, false if quota exceeded
     */
    public boolean incrementUsage(String guestIdentifier) {
        resetDailyIfNeeded();
        
        AtomicLong counter = guestUsage.computeIfAbsent(guestIdentifier, k -> new AtomicLong(0));
        long current = counter.incrementAndGet();
        long guestQuota = configService.getQuotaForTier(UserTier.GUEST);
        
        if (current > guestQuota) {
            counter.decrementAndGet();
            logger.warn("Guest quota exceeded for: {}", guestIdentifier);
            return false;
        }
        
        logger.debug("Guest usage incremented: {} → {}/{}", guestIdentifier, current, guestQuota);
        return true;
    }

    /**
     * Validate and increment usage atomically
     * @throws QuotaExceededException if quota is exceeded
     */
    public void validateAndIncrement(String guestIdentifier) throws QuotaExceededException {
        if (!hasQuotaRemaining(guestIdentifier)) {
            long usage = guestUsage.getOrDefault(guestIdentifier, new AtomicLong(0)).get();
            long quota = configService.getQuotaForTier(UserTier.GUEST);
            throw new QuotaExceededException(usage, quota);
        }
        incrementUsage(guestIdentifier);
    }

    /**
     * Get current usage for guest
     */
    public long getCurrentUsage(String guestIdentifier) {
        resetDailyIfNeeded();
        return guestUsage.getOrDefault(guestIdentifier, new AtomicLong(0)).get();
    }

    /**
     * Get current guest quota limit (admin configurable)
     */
    public long getGuestQuotaLimit() {
        return configService.getQuotaForTier(UserTier.GUEST);
    }

    /**
     * Get remaining quota for guest
     */
    public long getRemainingQuota(String guestIdentifier) {
        return Math.max(0, getGuestQuotaLimit() - getCurrentUsage(guestIdentifier));
    }

    /**
     * Reset usage if day has changed
     */
    private void resetDailyIfNeeded() {
        LocalDate today = LocalDate.now();
        if (!currentUsageDay.equals(today)) {
            synchronized (this) {
                if (!currentUsageDay.equals(today)) {
                    guestUsage.clear();
                    currentUsageDay = today;
                    logger.info("Guest daily quotas reset for new day: {}", today);
                }
            }
        }
    }

    /**
     * Reset all guest usage manually (admin function)
     */
    public void resetAllGuestUsage() {
        guestUsage.clear();
        logger.info("All guest quotas reset manually by admin");
    }

    /**
     * Automatic daily reset at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyReset() {
        guestUsage.clear();
        currentUsageDay = LocalDate.now();
        logger.info("Automatic daily guest quota reset completed");
    }

    /**
     * Extract guest identifier from request
     * Priority: X-Forwarded-For → Remote Address → Anonymous
     */
    public String extractGuestIdentifier(jakarta.servlet.http.HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "anonymous";
    }
}
