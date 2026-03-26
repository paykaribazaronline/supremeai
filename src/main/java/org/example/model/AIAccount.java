package org.example.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a single AI Account (API Key holder)
 * - One provider can have multiple accounts
 * - Each account has independent budget, rate limit, ban status
 * - Example: GPT-4 Account 1, GPT-4 Account 2, etc.
 */
public class AIAccount {
    private String accountId;              // "gpt-4-prod-1", "claude-dev-2"
    private String provider;               // "GPT-4", "Claude-3", "Gemini"
    private String accountName;            // User-friendly name
    private String apiKey;                 // Actual API key (encrypted in Firebase)
    private String createdBy;              // Admin email
    private LocalDateTime createdAt;       // When account was added
    
    // Budget & Usage Tracking
    private double budgetLimit;            // $10, $20, $0 (free tier)
    private double budgetUsed;             // $1.50 (current month)
    private double monthlyQuota;           // Requests per month
    private double monthlyUsed;            // Requests used this month
    private double freeTierQuota;          // % of budget before billing
    
    // Rate Limiting
    private int requestsPerMinute;         // RPM limit
    private int tokensPerMinute;           // TPM limit
    private LocalDateTime lastRequestTime; // For rate limiting
    private int currentMinuteRequests;     // Requests in current minute
    
    // Status & Health
    private AccountStatus status;          // ACTIVE, SUSPENDED, BANNED
    private String suspensionReason;       // Why suspended
    private LocalDateTime suspendedAt;     // When suspended
    private LocalDateTime unblockAt;       // When unblock (auto-rotate)
    private boolean isBanned;              // Hit quota limit
    private String banReason;              // $0 exceeded, rate limit hit, etc
    
    // Performance Metrics
    private int successfulRequests;        // Total success
    private int failedRequests;            // Total failures
    private double averageResponseTime;    // ms
    private LocalDateTime lastUsedAt;      // Last successful request
    
    // Fallback Chain
    private int priority;                  // 1 (primary), 2 (backup), 3 (last resort)
    private boolean isActive;              // Can this account be used?
    
    public enum AccountStatus {
        ACTIVE, SUSPENDED, BANNED, DISABLED
    }
    
    // Constructor
    public AIAccount(String accountId, String provider, String accountName, 
                     String apiKey, String createdBy, double budgetLimit) {
        this.accountId = accountId;
        this.provider = provider;
        this.accountName = accountName;
        this.apiKey = apiKey;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        
        this.budgetLimit = budgetLimit;
        this.budgetUsed = 0;
        this.monthlyQuota = 10000;  // Default 10k requests/month
        this.monthlyUsed = 0;
        this.freeTierQuota = 50;    // Use 50% of budget before billing
        
        this.requestsPerMinute = 90;
        this.tokensPerMinute = 500000;
        this.currentMinuteRequests = 0;
        
        this.status = AccountStatus.ACTIVE;
        this.isBanned = false;
        this.isActive = true;
        this.priority = 1;
        
        this.successfulRequests = 0;
        this.failedRequests = 0;
        this.averageResponseTime = 0;
    }
    
    // Budget Check
    public boolean isBudgetAvailable() {
        if (budgetLimit == 0) return true;  // Unlimited
        return budgetUsed < budgetLimit;
    }
    
    public boolean isFreeTierAvailable() {
        double freeLimit = (budgetLimit * freeTierQuota) / 100;
        return budgetUsed < freeLimit;
    }
    
    public double getBudgetRemaining() {
        if (budgetLimit == 0) return Double.MAX_VALUE;
        return budgetLimit - budgetUsed;
    }
    
    // Quota Check
    public boolean isQuotaAvailable() {
        return monthlyUsed < monthlyQuota;
    }
    
    public int getQuotaRemaining() {
        return (int) (monthlyQuota - monthlyUsed);
    }
    
    // Rate Limiting Check
    public boolean canMakeRequest() {
        LocalDateTime now = LocalDateTime.now();
        
        // Reset counter if minute has passed
        if (lastRequestTime != null) {
            long secondsPassed = java.time.temporal.ChronoUnit.SECONDS
                    .between(lastRequestTime, now);
            if (secondsPassed >= 60) {
                currentMinuteRequests = 0;
            }
        }
        
        return currentMinuteRequests < requestsPerMinute && status == AccountStatus.ACTIVE;
    }
    
    public void recordRequest() {
        this.currentMinuteRequests++;
        this.lastRequestTime = LocalDateTime.now();
        this.monthlyUsed++;
    }
    
    public void recordSuccess(double costIncurred, int responseTimeMs) {
        this.successfulRequests++;
        this.budgetUsed += costIncurred;
        this.lastUsedAt = LocalDateTime.now();
        
        // Update average response time
        this.averageResponseTime = (this.averageResponseTime * (this.successfulRequests - 1) 
                                   + responseTimeMs) / this.successfulRequests;
    }
    
    public void recordFailure() {
        this.failedRequests++;
    }
    
    // Account Management
    public void suspend(String reason) {
        this.status = AccountStatus.SUSPENDED;
        this.suspensionReason = reason;
        this.suspendedAt = LocalDateTime.now();
        this.isActive = false;
    }
    
    public void ban(String reason) {
        this.isBanned = true;
        this.status = AccountStatus.BANNED;
        this.banReason = reason;
        this.isActive = false;
        this.unblockAt = LocalDateTime.now().plusMinutes(30);  // Auto-unban after 30min
    }
    
    public void reactivate() {
        this.status = AccountStatus.ACTIVE;
        this.isActive = true;
        this.isBanned = false;
        this.suspensionReason = null;
        this.currentMinuteRequests = 0;
    }
    
    public boolean shouldAutoUnblock() {
        if (!isBanned || unblockAt == null) return false;
        return LocalDateTime.now().isAfter(unblockAt);
    }
    
    // Health Score (0-100)
    public int getHealthScore() {
        if (status == AccountStatus.BANNED) return 0;
        if (status == AccountStatus.SUSPENDED) return 25;
        
        double successRate = successfulRequests > 0 
            ? (double) successfulRequests / (successfulRequests + failedRequests) * 100 
            : 100;
        
        double budgetHealth = budgetLimit > 0 
            ? ((budgetLimit - budgetUsed) / budgetLimit) * 100 
            : 100;
        
        return (int) ((successRate * 0.6) + (budgetHealth * 0.4));
    }
    
    // Getters & Setters
    public String getAccountId() { return accountId; }
    public String getProvider() { return provider; }
    public String getAccountName() { return accountName; }
    public String getApiKey() { return apiKey; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    public double getBudgetLimit() { return budgetLimit; }
    public double getBudgetUsed() { return budgetUsed; }
    public void setBudgetUsed(double amount) { this.budgetUsed = amount; }
    
    public double getMonthlyQuota() { return monthlyQuota; }
    public double getMonthlyUsed() { return monthlyUsed; }
    public void setMonthlyUsed(double amount) { this.monthlyUsed = amount; }
    
    public double getFreeTierQuota() { return freeTierQuota; }
    public void setFreeTierQuota(double percent) { this.freeTierQuota = percent; }
    
    public int getRequestsPerMinute() { return requestsPerMinute; }
    public void setRequestsPerMinute(int rpm) { this.requestsPerMinute = rpm; }
    
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    
    public boolean isActive() { return isActive; }
    public boolean isBanned() { return isBanned; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public int getSuccessfulRequests() { return successfulRequests; }
    public int getFailedRequests() { return failedRequests; }
    public double getAverageResponseTime() { return averageResponseTime; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    
    @Override
    public String toString() {
        return String.format(
            "AIAccount{id='%s', provider='%s', name='%s', budget=%.2f/%.2f, status=%s, health=%d%%}",
            accountId, provider, accountName, budgetUsed, budgetLimit, status, getHealthScore()
        );
    }
}
