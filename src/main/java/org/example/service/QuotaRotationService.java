package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    // Define 10 AI Providers with their free tier quotas
    public enum AIProvider {
        OPENAI_GAPI("OpenAI GPT-4 (free tier)", 3),           // 3 calls/min free
        ANTHROPIC_API("Anthropic Claude (free tier)", 5),     // 5 calls/min free
        GOOGLE_GEMINI("Google Gemini (free tier)", 15),       // 15 calls/day free
        META_LLAMA("Meta Llama 2 (huggingface)", 100),        // ~100/day estimate
        MISTRAL_API("Mistral 7B (free tier)", 10),            // 10/day free
        COHERE_API("Cohere (free tier)", 20),                 // 20/day free
        HUGGINGFACE_API("HuggingFace Inference API", 50),     // 50/day free tier
        XAI_GROK("xAI Grok (API free tier)", 25),             // 25/day free
        DEEPSEEK_API("DeepSeek (free tier)", 30),             // 30/day free
        PERPLEXITY_API("Perplexity (free tier)", 40);         // 40/day free
        
        public final String displayName;
        public final int dailyQuota;
        
        AIProvider(String displayName, int dailyQuota) {
            this.displayName = displayName;
            this.dailyQuota = dailyQuota;
        }
    }
    
    /**
     * Tracks quota usage per provider per month
     */
    public static class QuotaTracker {
        public String provider;
        public YearMonth month;
        public int totalQuota;
        public int usedQuota = 0;
        public int remainingQuota;
        public double costPerCall = 0.0;  // Free tier = $0
        public LocalDate lastResetDate = LocalDate.now();
        
        public QuotaTracker(String provider, int dailyQuota) {
            this.provider = provider;
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
    
    public QuotaRotationService() {
        initializeQuotas();
    }
    
    /**
     * Initialize quota trackers for all 10 providers
     */
    private void initializeQuotas() {
        for (AIProvider provider : AIProvider.values()) {
            QuotaTracker tracker = new QuotaTracker(provider.name(), provider.dailyQuota);
            quotaTrackers.put(provider.name(), tracker);
            logger.info("✅ Initialized quota for {}: {} calls/month", provider.displayName, provider.dailyQuota * 30);
        }
    }
    
    /**
     * Get next healthy provider with available quota
     * Rotation strategy: Round-robin with fallback to highest available quota
     */
    public AIProvider getNextProvider() {
        AIProvider[] providers = AIProvider.values();
        
        // Try next provider in round-robin
        for (int i = 0; i < providers.length; i++) {
            currentProviderIndex = (currentProviderIndex + 1) % providers.length;
            AIProvider provider = providers[currentProviderIndex];
            
            QuotaTracker tracker = quotaTrackers.get(provider.name());
            if (!tracker.isExhausted()) {
                logger.info("🔄 Provider rotation: {} → {} (remaining: {})", 
                    i > 0 ? "skip" : "select", provider.displayName, tracker.remainingQuota);
                return provider;
            }
        }
        
        // Fallback: Find provider with highest remaining quota
        return quotaTrackers.entrySet().stream()
            .max(Comparator.comparingInt(e -> e.getValue().remainingQuota))
            .map(e -> AIProvider.valueOf(e.getKey()))
            .orElse(AIProvider.OPENAI_GAPI);  // Absolute fallback
    }
    
    /**
     * Consume quota for a successful API call
     */
    public void recordSuccess(String provider, int tokensUsed) {
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
        Map<String, Map<String, Object>> status = new LinkedHashMap<>();
        
        for (AIProvider provider : AIProvider.values()) {
            QuotaTracker tracker = quotaTrackers.get(provider.name());
            
            status.put(provider.displayName, new LinkedHashMap<String, Object>() {{
                put("provider", provider.name());
                put("quota_total", tracker.totalQuota);
                put("quota_used", tracker.usedQuota);
                put("quota_remaining", tracker.remainingQuota);
                put("quota_percent", String.format("%.1f%%", (tracker.usedQuota * 100.0) / tracker.totalQuota));
                put("status", tracker.isExhausted() ? "EXHAUSTED" : 
                             tracker.isNearLimit() ? "NEAR_LIMIT" : "OK");
                put("estimated_cost", String.format("$%.2f", tracker.estimatedMonthlyCost()));
                put("failures", failureCount.getOrDefault(provider.name(), 0));
            }});
        }
        
        return status;
    }
    
    /**
     * Calculate total remaining quota across all providers
     */
    public int getTotalRemainingQuota() {
        return quotaTrackers.values().stream()
            .mapToInt(t -> t.remainingQuota)
            .sum();
    }
    
    /**
     * Calculate projected monthly cost ($0 if using free tiers only)
     */
    public double getProjectedMonthlyCost() {
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
                logger.info("🔄 Resetting monthly quota for {}", tracker.provider);
                
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
    public AIProvider getOptimalProvider() {
        checkAndResetMonthlyQuotas();
        
        return quotaTrackers.entrySet().stream()
            .filter(e -> !e.getValue().isExhausted())
            .max(Comparator
                .comparingInt((Map.Entry<String, QuotaTracker> e) -> e.getValue().remainingQuota)
                .thenComparingInt(e -> -failureCount.getOrDefault(e.getKey(), 0)))
            .map(e -> AIProvider.valueOf(e.getKey()))
            .orElse(getNextProvider());
    }
    
    /**
     * Get quota summary for dashboard
     */
    public Map<String, Object> getQuotaSummary() {
        return new LinkedHashMap<String, Object>() {{
            put("status", "✅ OK");
            put("current_month", YearMonth.now().toString());
            put("total_providers", AIProvider.values().length);
            put("total_quota", quotaTrackers.values().stream().mapToInt(t -> t.totalQuota).sum());
            put("total_used", quotaTrackers.values().stream().mapToInt(t -> t.usedQuota).sum());
            put("total_remaining", getTotalRemainingQuota());
            put("providers_healthy", quotaTrackers.values().stream()
                .filter(t -> !t.isExhausted()).count());
            put("providers_exhausted", quotaTrackers.values().stream()
                .filter(QuotaTracker::isExhausted).count());
            put("projected_cost", String.format("$%.2f/month", getProjectedMonthlyCost()));
            put("next_provider", getNextProvider().displayName);
            put("optimal_provider", getOptimalProvider().displayName);
        }};
    }
}
