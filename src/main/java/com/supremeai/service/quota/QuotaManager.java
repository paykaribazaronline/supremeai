package com.supremeai.service.quota;

import com.supremeai.model.UserTier;
import java.time.LocalDateTime;

/**
 * Unified Quota Manager Interface
 * Common contract for all quota systems in SupremeAI
 */
public interface QuotaManager<T> {

    /**
     * Check if entity has remaining quota
     */
    boolean hasQuotaRemaining(T entity);

    /**
     * Get current usage count
     */
    long getCurrentUsage(T entity);

    /**
     * Get maximum quota limit
     */
    long getQuotaLimit(T entity);

    /**
     * Get remaining available quota
     */
    default long getRemainingQuota(T entity) {
        if (isUnlimited(entity)) return Long.MAX_VALUE;
        return Math.max(0, getQuotaLimit(entity) - getCurrentUsage(entity));
    }

    /**
     * Check if entity has unlimited quota
     */
    boolean isUnlimited(T entity);

    /**
     * Get usage percentage (0-100)
     */
    default double getUsagePercentage(T entity) {
        long limit = getQuotaLimit(entity);
        if (limit <= 0 || isUnlimited(entity)) return 0.0;
        return (double) getCurrentUsage(entity) / limit * 100.0;
    }

    /**
     * Increment usage count
     * @return true if successful, false if quota exceeded
     */
    boolean incrementUsage(T entity);

    /**
     * Reset usage to zero
     */
    void resetUsage(T entity);

    /**
     * Validate and increment usage atomically
     * @throws QuotaExceededException if quota is exceeded
     */
    default void validateAndIncrement(T entity) throws QuotaExceededException {
        if (!hasQuotaRemaining(entity)) {
            throw new QuotaExceededException(getCurrentUsage(entity), getQuotaLimit(entity));
        }
        incrementUsage(entity);
    }

    /**
     * Sync entity quota with current tier configuration
     */
    void syncWithTierConfig(T entity, UserTier tier);

    /**
     * Get last used timestamp
     */
    LocalDateTime getLastUsedAt(T entity);

    /**
     * Quota usage statistics
     */
    record QuotaStats(
        long currentUsage,
        long quotaLimit,
        long remainingQuota,
        double usagePercentage,
        LocalDateTime lastUsedAt,
        boolean hasRemaining,
        boolean isUnlimited
    ) {}

    default QuotaStats getStats(T entity) {
        return new QuotaStats(
            getCurrentUsage(entity),
            getQuotaLimit(entity),
            getRemainingQuota(entity),
            getUsagePercentage(entity),
            getLastUsedAt(entity),
            hasQuotaRemaining(entity),
            isUnlimited(entity)
        );
    }
}
