package org.example.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Quota model tracking API usage per AI provider
 */
public class Quota {
    private String providerId;
    private String providerName;
    private long requestsUsedToday;
    private long dailyLimit;
    private long tokensUsedToday;
    private long dailyTokenLimit;
    private double requestsPerMinute;
    private double rpmLimit;
    private LocalDateTime lastResetTime;
    private LocalDateTime nextResetTime;
    private double usagePercentage;
    private String status; // HEALTHY, WARNING, CRITICAL, OUT_OF_QUOTA
    private Map<String, Object> metadata;
    
    public Quota() {
        this.metadata = new HashMap<>();
    }
    
    public Quota(String providerId, String providerName, long dailyLimit, long dailyTokenLimit, double rpmLimit) {
        this();
        this.providerId = providerId;
        this.providerName = providerName;
        this.dailyLimit = dailyLimit;
        this.dailyTokenLimit = dailyTokenLimit;
        this.rpmLimit = rpmLimit;
        this.requestsUsedToday = 0;
        this.tokensUsedToday = 0;
        this.requestsPerMinute = 0;
        this.lastResetTime = LocalDateTime.now();
        this.nextResetTime = LocalDateTime.now().plusDays(1);
        updateStatus();
    }
    
    public void incrementUsage(long tokenCount) {
        this.requestsUsedToday++;
        this.tokensUsedToday += tokenCount;
        updateStatus();
    }
    
    public void updateStatus() {
        this.usagePercentage = (double) requestsUsedToday / dailyLimit * 100;
        
        if (usagePercentage >= 100) {
            this.status = "OUT_OF_QUOTA";
        } else if (usagePercentage >= 85) {
            this.status = "CRITICAL";
        } else if (usagePercentage >= 70) {
            this.status = "WARNING";
        } else {
            this.status = "HEALTHY";
        }
    }
    
    public boolean hasQuotaAvailable() {
        return requestsUsedToday < dailyLimit && usagePercentage < 100;
    }
    
    public long getRemainingRequests() {
        return Math.max(0, dailyLimit - requestsUsedToday);
    }
    
    public long getRemainingTokens() {
        return Math.max(0, dailyTokenLimit - tokensUsedToday);
    }
    
    public double getRemainingPercentage() {
        return Math.max(0, 100.0 - usagePercentage);
    }
    
    // Getters and Setters
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    
    public long getRequestsUsedToday() { return requestsUsedToday; }
    public void setRequestsUsedToday(long requestsUsedToday) { 
        this.requestsUsedToday = requestsUsedToday;
        updateStatus();
    }
    
    public long getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(long dailyLimit) { this.dailyLimit = dailyLimit; }
    
    public long getTokensUsedToday() { return tokensUsedToday; }
    public void setTokensUsedToday(long tokensUsedToday) { 
        this.tokensUsedToday = tokensUsedToday;
        updateStatus();
    }
    
    public long getDailyTokenLimit() { return dailyTokenLimit; }
    public void setDailyTokenLimit(long dailyTokenLimit) { this.dailyTokenLimit = dailyTokenLimit; }
    
    public double getRequestsPerMinute() { return requestsPerMinute; }
    public void setRequestsPerMinute(double requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
    
    public double getRpmLimit() { return rpmLimit; }
    public void setRpmLimit(double rpmLimit) { this.rpmLimit = rpmLimit; }
    
    public LocalDateTime getLastResetTime() { return lastResetTime; }
    public void setLastResetTime(LocalDateTime lastResetTime) { this.lastResetTime = lastResetTime; }
    
    public LocalDateTime getNextResetTime() { return nextResetTime; }
    public void setNextResetTime(LocalDateTime nextResetTime) { this.nextResetTime = nextResetTime; }
    
    public double getUsagePercentage() { return usagePercentage; }
    public void setUsagePercentage(double usagePercentage) { this.usagePercentage = usagePercentage; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
