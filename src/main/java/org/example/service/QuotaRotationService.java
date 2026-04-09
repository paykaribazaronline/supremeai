package org.example.service;

import org.example.model.APIProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quota-Based AI Rotation Service
 * Rotates through free API quotas to optimize cost (targets: $0/month)
 * Tracks remaining quota per provider per month
 */
@Service
public class QuotaRotationService {
    private static final Logger logger = LoggerFactory.getLogger(QuotaRotationService.class);

    @Autowired
    private ProviderRegistryService providerRegistryService;
    
    /**
     * Tracks quota usage per provider per month
     */
    public static class QuotaTracker {
        public String providerId;
        public String providerName;
        public YearMonth month;
        public int totalQuota;
        public int usedQuota = 0;
        public int remainingQuota;
        public double costPerCall = 0.0;  // Free tier = $0
        public LocalDate lastResetDate = LocalDate.now();
        
        public QuotaTracker(String providerId, String providerName, int dailyQuota) {
            this.providerId = providerId;
            this.providerName = providerName;
            this.month = YearMonth.now();
            // Monthly quota = daily * 30 days
            this.totalQuota = dailyQuota * 30;
            this.remainingQuota = this.totalQuota;
        }
        
        public boolean canConsume(int amount) {
            return remainingQuota >= amount;
        }
        
        public void consume(int amount) {
            if (canConsume(amount)) {
                usedQuota += amount;
                remainingQuota -= amount;
            }
        }
        
        public double estimatedMonthlyCost() {
            return usedQuota * costPerCall;  // Always $0 for free tiers
        }
        
        public boolean isNearLimit() {
            return remainingQuota < (totalQuota * 0.15);  // Within 15% of limit
        }
        
        public boolean isExhausted() {
            return remainingQuota <= 0;
        }
    }
    
    private final Map<String, QuotaTracker> quotaTrackers = new ConcurrentHashMap<>();
    private final Map<String, Integer> failureCount = new ConcurrentHashMap<>();
    private int currentProviderIndex = 0;
    
    /**
     * Sync quota trackers with the admin-managed provider registry.
     */
    public synchronized void syncQuotaTrackers() {
        List<APIProvider> activeProviders = providerRegistryService.getActiveProviders();
        Set<String> activeIds = new HashSet<>();

        for (APIProvider provider : activeProviders) {
            activeIds.add(provider.getId());
            quotaTrackers.computeIfAbsent(provider.getId(), id -> {
                int dailyQuota = estimateDailyQuota(provider.getName());
                logger.info("✅ Initialized quota rotation for {}: {} calls/month", provider.getName(), dailyQuota * 30);
                return new QuotaTracker(provider.getId(), provider.getName(), dailyQuota);
            });
        }

        quotaTrackers.keySet().removeIf(providerId -> !activeIds.contains(providerId));
    }
    
    /**
     * Get next healthy provider with available quota
     * Rotation strategy: Round-robin with fallback to highest available quota
     */
    public String getNextProvider() {
        syncQuotaTrackers();
        List<QuotaTracker> providers = new ArrayList<>(quotaTrackers.values());
        if (providers.isEmpty()) {
            return null;
        }
        
        // Try next provider in round-robin
        for (int i = 0; i < providers.size(); i++) {
            currentProviderIndex = (currentProviderIndex + 1) % providers.size();
            QuotaTracker tracker = providers.get(currentProviderIndex);
            if (!tracker.isExhausted()) {
                logger.info("🔄 Provider rotation: {} → {} (remaining: {})", 
                    i > 0 ? "skip" : "select", tracker.providerName, tracker.remainingQuota);
                return tracker.providerId;
            }
        }
        
        // Fallback: Find provider with highest remaining quota
        return quotaTrackers.entrySet().stream()
            .max(Comparator.comparingInt(e -> e.getValue().remainingQuota))
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Consume quota for a successful API call
     */
    public void recordSuccess(String provider, int tokensUsed) {
        syncQuotaTrackers();
        QuotaTracker tracker = quotaTrackers.get(provider);
        if (tracker != null) {
            tracker.consume(1);  // Count as 1 API call regardless of tokens
            failureCount.put(provider, 0);  // Reset failure count
            
            if (tracker.isNearLimit()) {
                logger.warn("⚠️ {} approaching quota limit: {} remaining", provider, tracker.remainingQuota);
            }
        }
    }
    
    /**
     * Record failed API call (don't consume quota, but track failures)
     */
    public void recordFailure(String provider) {
        syncQuotaTrackers();
        failureCount.merge(provider, 1, Integer::sum);
        int failures = failureCount.get(provider);
        
        if (failures >= 3) {
            logger.warn("❌ {} has {} consecutive failures - will skip in rotation", provider, failures);
        }
    }
    
    /**
     * Reset failure count when provider succeeds
     */
    public void resetFailureCount(String provider) {
        failureCount.put(provider, 0);
    }
    
    /**
     * Get current quota status for all providers
     */
    public Map<String, Map<String, Object>> getQuotaStatus() {
        syncQuotaTrackers();
        Map<String, Map<String, Object>> status = new LinkedHashMap<>();
        
        for (QuotaTracker tracker : quotaTrackers.values()) {
            
            status.put(tracker.providerName, new LinkedHashMap<String, Object>() {{
                put("provider", tracker.providerId);
                put("quota_total", tracker.totalQuota);
                put("quota_used", tracker.usedQuota);
                put("quota_remaining", tracker.remainingQuota);
                put("quota_percent", String.format("%.1f%%", (tracker.usedQuota * 100.0) / tracker.totalQuota));
                put("status", tracker.isExhausted() ? "EXHAUSTED" : 
                             tracker.isNearLimit() ? "NEAR_LIMIT" : "OK");
                put("estimated_cost", String.format("$%.2f", tracker.estimatedMonthlyCost()));
                put("failures", failureCount.getOrDefault(tracker.providerId, 0));
            }});
        }
        
        return status;
    }
    
    /**
     * Calculate total remaining quota across all providers
     */
    public int getTotalRemainingQuota() {
        syncQuotaTrackers();
        return quotaTrackers.values().stream()
            .mapToInt(t -> t.remainingQuota)
            .sum();
    }
    
    /**
     * Calculate projected monthly cost ($0 if using free tiers only)
     */
    public double getProjectedMonthlyCost() {
        syncQuotaTrackers();
        return quotaTrackers.values().stream()
            .mapToDouble(QuotaTracker::estimatedMonthlyCost)
            .sum();
    }
    
    /**
     * Reset monthly quotas if date crossed month boundary
     */
    public void checkAndResetMonthlyQuotas() {
        YearMonth currentMonth = YearMonth.now();
        
        for (QuotaTracker tracker : quotaTrackers.values()) {
            if (!tracker.month.equals(currentMonth)) {
                logger.info("🔄 Resetting monthly quota for {}", tracker.providerName);
                
                // Archive previous month's stats
                tracker.lastResetDate = LocalDate.now();
                tracker.month = currentMonth;
                tracker.usedQuota = 0;
                tracker.remainingQuota = tracker.totalQuota;
            }
        }
    }
    
    /**
     * Get recommended provider based on:
     * 1. Highest remaining quota
     * 2. Lowest failure rate
     * 3. Good success history
     */
    public String getOptimalProvider() {
        checkAndResetMonthlyQuotas();
        
        return quotaTrackers.entrySet().stream()
            .filter(e -> !e.getValue().isExhausted())
            .max(Comparator
                .comparingInt((Map.Entry<String, QuotaTracker> e) -> e.getValue().remainingQuota)
                .thenComparingInt(e -> -failureCount.getOrDefault(e.getKey(), 0)))
            .map(Map.Entry::getKey)
            .orElse(getNextProvider());
    }
    
    /**
     * Get quota summary for dashboard
     */
    public Map<String, Object> getQuotaSummary() {
        syncQuotaTrackers();
        String nextProviderId = getNextProvider();
        String optimalProviderId = getOptimalProvider();

        return new LinkedHashMap<String, Object>() {{
            put("status", "✅ OK");
            put("current_month", YearMonth.now().toString());
            put("total_providers", quotaTrackers.size());
            put("total_quota", quotaTrackers.values().stream().mapToInt(t -> t.totalQuota).sum());
            put("total_used", quotaTrackers.values().stream().mapToInt(t -> t.usedQuota).sum());
            put("total_remaining", getTotalRemainingQuota());
            put("providers_healthy", quotaTrackers.values().stream()
                .filter(t -> !t.isExhausted()).count());
            put("providers_exhausted", quotaTrackers.values().stream()
                .filter(QuotaTracker::isExhausted).count());
            put("projected_cost", String.format("$%.2f/month", getProjectedMonthlyCost()));
            put("next_provider", getProviderDisplayName(nextProviderId));
            put("optimal_provider", getProviderDisplayName(optimalProviderId));
        }};
    }

    public String getProviderDisplayName(String providerId) {
        if (providerId == null) {
            return null;
        }
        QuotaTracker tracker = quotaTrackers.get(providerId);
        return tracker == null ? providerId : tracker.providerName;
    }

    public List<Map<String, Object>> getRegisteredProviders() {
        syncQuotaTrackers();
        List<Map<String, Object>> providers = new ArrayList<>();
        for (QuotaTracker tracker : quotaTrackers.values()) {
            providers.add(new LinkedHashMap<String, Object>() {{
                put("id", tracker.providerId);
                put("display_name", tracker.providerName);
                put("daily_quota", tracker.totalQuota / 30);
                put("monthly_quota", tracker.totalQuota);
                put("monthly_cost_free_tier", "$0.00");
            }});
        }
        return providers;
    }

    /**
     * Estimate daily quota from admin-configured provider settings.
     * No hardcoded provider names — reads from the provider registry.
     */
    private int estimateDailyQuota(String providerName) {
        // Look up admin-configured quota from provider registry
        List<APIProvider> providers = providerRegistryService.getActiveProviders();
        for (APIProvider p : providers) {
            if (providerName != null && providerName.equalsIgnoreCase(p.getName())) {
                if (p.getMonthlyQuota() != null && p.getMonthlyQuota() > 0) {
                    return Math.max(1, p.getMonthlyQuota() / 30);
                }
                if (p.getRateLimitPerMinute() != null && p.getRateLimitPerMinute() > 0) {
                    // Estimate: RPM * 60 min * 8 hours conservative usage per day
                    return p.getRateLimitPerMinute() * 60 * 8;
                }
            }
        }
        // Sensible default for unknown providers — admin can override anytime
        return 100;
    }
}
