package org.example.service;

import org.example.model.Quota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quota Management Service
 * Tracks API usage per provider, prevents quota exhaustion
 * Ensures SupremeAI doesn't monopolize all quotas
 */
@Service
public class QuotaService {
    private static final Logger logger = LoggerFactory.getLogger(QuotaService.class);
    
    private Map<String, Quota> quotas = new ConcurrentHashMap<>();
    private static final int MINIMUM_QUOTA_PERCENT = 20; // Min 20% remaining to use an AI
    
    public QuotaService() {
        initializeDefaultQuotas();
    }
    
    /**
     * Initialize default quotas for 10 AI providers
     * These are conservative estimates based on typical API limits
     */
    private void initializeDefaultQuotas() {
        // OpenAI: 3,500 RPM, 200,000 tokens/min
        quotas.put("openai-gpt4", new Quota(
            "openai-gpt4", "OpenAI GPT-4",
            3500, 200000, 3500
        ));
        
        // Anthropic: 50,000 tokens/day limit
        quotas.put("anthropic-claude", new Quota(
            "anthropic-claude", "Anthropic Claude",
            1000, 50000, 100
        ));
        
        // Google Gemini: 100 requests/second
        quotas.put("google-gemini", new Quota(
            "google-gemini", "Google Gemini",
            6000, 150000, 6000
        ));
        
        // Meta LLaMA: 5,000 requests/day
        quotas.put("meta-llama", new Quota(
            "meta-llama", "Meta LLaMA",
            5000, 100000, 500
        ));
        
        // Mistral: 300 requests/minute
        quotas.put("mistral", new Quota(
            "mistral", "Mistral",
            300, 50000, 300
        ));
        
        // Cohere: Varies, conservative 1,000/day
        quotas.put("cohere", new Quota(
            "cohere", "Cohere",
            1000, 100000, 200
        ));
        
        // HuggingFace: Free tier 1,000/month
        quotas.put("huggingface", new Quota(
            "huggingface", "HuggingFace",
            500, 50000, 100
        ));
        
        // XAI Grok: Limited access ~500/day
        quotas.put("xai-grok", new Quota(
            "xai-grok", "XAI Grok",
            500, 30000, 100
        ));
        
        // DeepSeek: 2,000 requests/day
        quotas.put("deepseek", new Quota(
            "deepseek", "DeepSeek",
            2000, 80000, 200
        ));
        
        // Perplexity: 1,000 requests/day
        quotas.put("perplexity", new Quota(
            "perplexity", "Perplexity",
            1000, 60000, 150
        ));
        
        logger.info("✅ Quota system initialized with 10 AI providers");
    }
    
    /**
     * Check if an AI provider has available quota
     */
    public boolean canUseAI(String providerId) {
        Quota quota = quotas.get(providerId);
        if (quota == null) {
            logger.warn("⚠️ No quota found for provider: {}", providerId);
            return false;
        }
        
        boolean available = quota.hasQuotaAvailable() && 
                           quota.getRemainingPercentage() >= MINIMUM_QUOTA_PERCENT;
        
        if (!available) {
            logger.warn("❌ Quota exhausted for {}: {}% used, status = {}",
                providerId, quota.getUsagePercentage(), quota.getStatus());
        }
        
        return available;
    }
    
    /**
     * Get all providers with available quota (for intelligent selection)
     */
    public List<String> getAvailableProviders() {
        List<String> available = new ArrayList<>();
        for (String providerId : quotas.keySet()) {
            if (canUseAI(providerId)) {
                available.add(providerId);
            }
        }
        logger.info("📊 Available providers: {}/{}", available.size(), quotas.size());
        return available;
    }
    
    /**
     * Record API usage for a provider
     */
    public void recordUsage(String providerId, long tokenCount) {
        Quota quota = quotas.get(providerId);
        if (quota != null) {
            quota.incrementUsage(tokenCount);
            logger.debug("📊 {} usage recorded: {} tokens ({}/{})",
                providerId, tokenCount, 
                quota.getRequestsUsedToday(), quota.getDailyLimit());
        }
    }
    
    /**
     * Get remaining quota percentage for a provider
     */
    public double getRemainingQuotaPercent(String providerId) {
        Quota quota = quotas.get(providerId);
        return quota != null ? quota.getRemainingPercentage() : 0;
    }
    
    /**
     * Get quota details for a provider
     */
    public Quota getQuotaDetails(String providerId) {
        return quotas.get(providerId);
    }
    
    /**
     * Get all quota details
     */
    public Map<String, Quota> getAllQuotas() {
        return new HashMap<>(quotas);
    }
    
    /**
     * Reset daily quotas (called daily or on schedule)
     */
    public void resetDailyQuotas() {
        logger.info("🔄 Resetting daily quotas for all providers");
        for (Quota quota : quotas.values()) {
            quota.setRequestsUsedToday(0);
            quota.setTokensUsedToday(0);
            quota.setLastResetTime(LocalDateTime.now());
            quota.setNextResetTime(LocalDateTime.now().plusDays(1));
            quota.updateStatus();
        }
        logger.info("✅ Daily quotas reset complete");
    }
    
    /**
     * Check if we need fallback (most AIs out of quota)
     */
    public boolean shouldUseFallback() {
        int available = getAvailableProviders().size();
        return available < 5; // Less than 5 AIs have quota
    }
    
    /**
     * Get count of providers with healthy quota
     */
    public int getHealthyProviderCount() {
        return (int) quotas.values().stream()
            .filter(q -> q.getStatus().equals("HEALTHY"))
            .count();
    }
    
    /**
     * Get count of providers with critical quota
     */
    public int getCriticalProviderCount() {
        return (int) quotas.values().stream()
            .filter(q -> q.getStatus().equals("CRITICAL"))
            .count();
    }
    
    /**
     * Get count of providers out of quota
     */
    public int getOutOfQuotaProviderCount() {
        return (int) quotas.values().stream()
            .filter(q -> q.getStatus().equals("OUT_OF_QUOTA"))
            .count();
    }
    
    /**
     * Update quota limits (admin function to adjust API limits)
     */
    public void updateQuotaLimit(String providerId, long newDailyLimit) {
        Quota quota = quotas.get(providerId);
        if (quota != null) {
            quota.setDailyLimit(newDailyLimit);
            quota.updateStatus();
            logger.info("✏️ Updated quota limit for {}: new limit = {}", providerId, newDailyLimit);
        }
    }
    
    /**
     * Manual quota increment (for testing or admin override)
     */
    public void manuallyIncrement(String providerId, long amount) {
        Quota quota = quotas.get(providerId);
        if (quota != null) {
            quota.setRequestsUsedToday(quota.getRequestsUsedToday() + amount);
            quota.updateStatus();
            logger.info("⚙️ Manual increment for {}: +{} requests", providerId, amount);
        }
    }
    
    /**
     * Get summary statistics
     */
    public Map<String, Object> getQuotaSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalProviders", quotas.size());
        summary.put("healthyProviders", getHealthyProviderCount());
        summary.put("criticalProviders", getCriticalProviderCount());
        summary.put("outOfQuotaProviders", getOutOfQuotaProviderCount());
        summary.put("availableProviders", getAvailableProviders());
        summary.put("shouldUseFallback", shouldUseFallback());
        summary.put("timestamp", LocalDateTime.now());
        return summary;
    }
}
