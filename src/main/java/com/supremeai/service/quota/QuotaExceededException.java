package com.supremeai.service.quota;

/**
 * Unified exception thrown when quota limit is exceeded
 */
public class QuotaExceededException extends RuntimeException {

    private final long currentUsage;
    private final long quotaLimit;

    public QuotaExceededException(long currentUsage, long quotaLimit) {
        super(String.format("Quota exceeded: %d / %d", currentUsage, quotaLimit));
        this.currentUsage = currentUsage;
        this.quotaLimit = quotaLimit;
    }

    public QuotaExceededException(String message, long currentUsage, long quotaLimit) {
        super(message);
        this.currentUsage = currentUsage;
        this.quotaLimit = quotaLimit;
    }

    public long getCurrentUsage() {
        return currentUsage;
    }

    public long getQuotaLimit() {
        return quotaLimit;
    }
}
