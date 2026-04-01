package org.example.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Quota Allocation - Track per-user quota usage
 */
public class UserQuotaAllocation {
    private String id = UUID.randomUUID().toString();
    private String userId;
    private UserTier tier;
    private long requestsUsedToday;
    private long requestsUsedThisMonth;
    private int appsCreatedToday;
    private LocalDate lastResetDate;
    private LocalDateTime createdAt;
    
    public UserQuotaAllocation() {
        this.lastResetDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }
    
    public UserQuotaAllocation(String userId, UserTier tier) {
        this();
        this.userId = userId;
        this.tier = tier;
    }
    
    /**
     * Check if user can make another API request
     */
    public boolean canMakeAPIRequest() {
        if (tier.isUnlimited()) return true;
        
        // Check daily limit
        if (tier.dailyLimit != -1 && requestsUsedToday >= tier.dailyLimit) {
            return false;
        }
        
        // Check monthly limit
        if (tier.monthlyLimit != -1 && requestsUsedThisMonth >= tier.monthlyLimit) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if user can create another app today
     */
    public boolean canCreateApp() {
        if (tier.isUnlimited()) return true;
        
        if (tier.appCreationsPerDay != -1 && appsCreatedToday >= tier.appCreationsPerDay) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Record an API request
     */
    public void recordAPIRequest() {
        resetIfNewDay();
        requestsUsedToday++;
        requestsUsedThisMonth++;
    }
    
    /**
     * Record an app creation
     */
    public void recordAppCreation() {
        resetIfNewDay();
        appsCreatedToday++;
    }
    
    /**
     * Reset daily counters if new day
     */
    private void resetIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!lastResetDate.equals(today)) {
            requestsUsedToday = 0;
            appsCreatedToday = 0;
            lastResetDate = today;
        }
    }
    
    /**
     * Get remaining daily requests
     */
    public long getRemainingDailyRequests() {
        if (tier.isUnlimited()) return Long.MAX_VALUE;
        if (tier.dailyLimit == -1) return Long.MAX_VALUE;
        return Math.max(0, tier.dailyLimit - requestsUsedToday);
    }
    
    /**
     * Get remaining monthly requests
     */
    public long getRemainingMonthlyRequests() {
        if (tier.isUnlimited()) return Long.MAX_VALUE;
        if (tier.monthlyLimit == -1) return Long.MAX_VALUE;
        return Math.max(0, tier.monthlyLimit - requestsUsedThisMonth);
    }
    
    /**
     * Get remaining app creations for today
     */
    public int getRemainingAppCreations() {
        if (tier.isUnlimited()) return Integer.MAX_VALUE;
        if (tier.appCreationsPerDay == -1) return Integer.MAX_VALUE;
        return Math.max(0, tier.appCreationsPerDay - appsCreatedToday);
    }
    
    // Getters & Setters
    public String getId() { return id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public UserTier getTier() { return tier; }
    public void setTier(UserTier tier) { this.tier = tier; }
    
    public long getRequestsUsedToday() { return requestsUsedToday; }
    public void setRequestsUsedToday(long requestsUsedToday) { this.requestsUsedToday = requestsUsedToday; }
    
    public long getRequestsUsedThisMonth() { return requestsUsedThisMonth; }
    public void setRequestsUsedThisMonth(long requestsUsedThisMonth) { this.requestsUsedThisMonth = requestsUsedThisMonth; }
    
    public int getAppsCreatedToday() { return appsCreatedToday; }
    public void setAppsCreatedToday(int appsCreatedToday) { this.appsCreatedToday = appsCreatedToday; }
    
    public LocalDate getLastResetDate() { return lastResetDate; }
    public void setLastResetDate(LocalDate lastResetDate) { this.lastResetDate = lastResetDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
