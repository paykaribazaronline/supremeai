package com.supremeai.cost;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class QuotaDefinition {
    private String serviceName;
    private String featureName;
    private double limit;
    private QuotaPeriod period;
    private LocalDateTime lastResetTime;
    private double currentUsage;

    public QuotaDefinition(String serviceName, String featureName, double limit, QuotaPeriod period) {
        this.serviceName = serviceName;
        this.featureName = featureName;
        this.limit = limit;
        this.period = period;
        this.lastResetTime = LocalDateTime.now(ZoneOffset.UTC);
        this.currentUsage = 0.0;
    }

    public void addUsage(double usage) {
        checkAndResetIfNeeded();
        this.currentUsage += usage;
    }

    public boolean isLimitReached() {
        checkAndResetIfNeeded();
        return this.currentUsage >= this.limit;
    }
    
    public double getRemainingQuota() {
        checkAndResetIfNeeded();
        return Math.max(0, this.limit - this.currentUsage);
    }
    
    public double getUsagePercentage() {
        checkAndResetIfNeeded();
        return (this.currentUsage / this.limit) * 100.0;
    }

    private void checkAndResetIfNeeded() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        boolean shouldReset = false;

        switch (period) {
            case HOURLY:
                if (ChronoUnit.HOURS.between(lastResetTime, now) >= 1) shouldReset = true;
                break;
            case DAILY:
                if (ChronoUnit.DAYS.between(lastResetTime, now) >= 1) shouldReset = true;
                break;
            case MONTHLY:
                if (ChronoUnit.MONTHS.between(lastResetTime, now) >= 1) shouldReset = true;
                break;
            case YEARLY:
                if (ChronoUnit.YEARS.between(lastResetTime, now) >= 1) shouldReset = true;
                break;
        }

        if (shouldReset) {
            this.currentUsage = 0.0;
            this.lastResetTime = now;
        }
    }

    // Getters and Setters
    public String getServiceName() { return serviceName; }
    public String getFeatureName() { return featureName; }
    public double getLimit() { return limit; }
    public QuotaPeriod getPeriod() { return period; }
    public double getCurrentUsage() { return currentUsage; }
}