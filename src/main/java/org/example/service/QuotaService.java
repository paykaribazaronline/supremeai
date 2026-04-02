package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import org.example.model.APIProvider;
import org.example.model.Quota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final String STORE_PATH = "provider-quotas.json";
    private static final int MINIMUM_QUOTA_PERCENT = 20; // Min 20% remaining to use an AI
    private static final long DEFAULT_DAILY_LIMIT = 1000;
    private static final long DEFAULT_DAILY_TOKEN_LIMIT = 100000;
    private static final double DEFAULT_RPM_LIMIT = 120.0;

    @Autowired
    private ProviderRegistryService providerRegistryService;

    @Autowired
    private LocalJsonStoreService localJsonStoreService;

    private Map<String, Quota> quotas = new ConcurrentHashMap<>();

    @PostConstruct
    void loadPersistedQuotas() {
        quotas = new ConcurrentHashMap<>(localJsonStoreService.read(
            STORE_PATH,
            new TypeReference<Map<String, Quota>>() {},
            new HashMap<>()
        ));
    }
    
    /**
     * Sync quota records with the admin-managed provider registry.
     */
    public synchronized void syncConfiguredProviders() {
        List<APIProvider> activeProviders = providerRegistryService.getActiveProviders();
        Set<String> activeProviderIds = new HashSet<>();
        activeProviders.forEach(provider -> activeProviderIds.add(provider.getId()));

        quotas.keySet().removeIf(providerId -> !activeProviderIds.contains(providerId));

        activeProviders.forEach(provider -> {
            Quota quota = quotas.computeIfAbsent(provider.getId(), ignored -> createQuota(provider));
            applyProviderLimits(quota, provider);
        });

        persistQuotas();

        logger.info("✅ Quota system synced with {} admin-configured AI providers", quotas.size());
    }
    
    /**
     * Check if an AI provider has available quota
     */
    public boolean canUseAI(String providerId) {
        syncConfiguredProviders();
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
        syncConfiguredProviders();
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
        syncConfiguredProviders();
        Quota quota = quotas.get(providerId);
        if (quota != null) {
            quota.incrementUsage(tokenCount);
            persistQuotas();
            logger.debug("📊 {} usage recorded: {} tokens ({}/{})",
                providerId, tokenCount, 
                quota.getRequestsUsedThisMonth(), quota.getMonthlyLimit());
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
        syncConfiguredProviders();
        return quotas.get(providerId);
    }
    
    /**
     * Get all quota details
     */
    public Map<String, Quota> getAllQuotas() {
        syncConfiguredProviders();
        return new HashMap<>(quotas);
    }
    
    /**
     * Reset daily quotas (called daily or on schedule)
     */
    public void resetDailyQuotas() {
        logger.info("🔄 Resetting daily quotas for all providers");
        syncConfiguredProviders();
        for (Quota quota : quotas.values()) {
            quota.setRequestsUsedToday(0);
            quota.setTokensUsedToday(0);
            quota.setLastResetTime(LocalDateTime.now());
            quota.setNextResetTime(LocalDateTime.now().plusDays(1));
            quota.updateStatus();
        }
        persistQuotas();
        logger.info("✅ Daily quotas reset complete");
    }

    public void resetMonthlyQuotas() {
        logger.info("🔄 Resetting monthly quotas for all providers");
        syncConfiguredProviders();
        for (Quota quota : quotas.values()) {
            quota.setRequestsUsedThisMonth(0);
            quota.setTokensUsedThisMonth(0);
            quota.setLastMonthlyResetTime(LocalDateTime.now());
            quota.setNextMonthlyResetTime(LocalDateTime.now().plusMonths(1));
            quota.updateStatus();
        }
        persistQuotas();
    }
    
    /**
     * Check if we need fallback (most AIs out of quota)
     */
    public boolean shouldUseFallback() {
        return getAvailableProviders().isEmpty();
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
        syncConfiguredProviders();
        Quota quota = quotas.get(providerId);
        if (quota != null) {
            quota.setDailyLimit(newDailyLimit);
            quota.setMonthlyLimit(Math.max(newDailyLimit, newDailyLimit * 30));
            quota.updateStatus();
            persistQuotas();
            logger.info("✏️ Updated quota limit for {}: new limit = {}", providerId, newDailyLimit);
        }
    }
    
    /**
     * Manual quota increment (for testing or admin override)
     */
    public void manuallyIncrement(String providerId, long amount) {
        syncConfiguredProviders();
        Quota quota = quotas.get(providerId);
        if (quota != null) {
            quota.setRequestsUsedToday(quota.getRequestsUsedToday() + amount);
            quota.setRequestsUsedThisMonth(quota.getRequestsUsedThisMonth() + amount);
            quota.updateStatus();
            persistQuotas();
            logger.info("⚙️ Manual increment for {}: +{} requests", providerId, amount);
        }
    }
    
    /**
     * Get summary statistics
     */
    public Map<String, Object> getQuotaSummary() {
        syncConfiguredProviders();
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

    public int getConfiguredProviderCount() {
        syncConfiguredProviders();
        return quotas.size();
    }

    private Quota createQuota(APIProvider provider) {
        String providerId = provider.getId();
        String providerName = provider.getName();
        String normalized = providerName == null ? "" : providerName.toLowerCase(Locale.ROOT);

        if (normalized.contains("openai")) {
            return applyProviderLimits(new Quota(providerId, providerName, 3500, 200000, 3500), provider);
        }
        if (normalized.contains("anthropic") || normalized.contains("claude")) {
            return applyProviderLimits(new Quota(providerId, providerName, 1000, 50000, 100), provider);
        }
        if (normalized.contains("gemini") || normalized.contains("google")) {
            return applyProviderLimits(new Quota(providerId, providerName, 6000, 150000, 6000), provider);
        }
        if (normalized.contains("meta") || normalized.contains("llama")) {
            return applyProviderLimits(new Quota(providerId, providerName, 5000, 100000, 500), provider);
        }
        if (normalized.contains("mistral")) {
            return applyProviderLimits(new Quota(providerId, providerName, 300, 50000, 300), provider);
        }
        if (normalized.contains("cohere")) {
            return applyProviderLimits(new Quota(providerId, providerName, 1000, 100000, 200), provider);
        }
        if (normalized.contains("huggingface")) {
            return applyProviderLimits(new Quota(providerId, providerName, 500, 50000, 100), provider);
        }
        if (normalized.contains("xai") || normalized.contains("grok")) {
            return applyProviderLimits(new Quota(providerId, providerName, 500, 30000, 100), provider);
        }
        if (normalized.contains("deepseek")) {
            return applyProviderLimits(new Quota(providerId, providerName, 2000, 80000, 200), provider);
        }
        if (normalized.contains("perplexity")) {
            return applyProviderLimits(new Quota(providerId, providerName, 1000, 60000, 150), provider);
        }

        return applyProviderLimits(new Quota(providerId, providerName, DEFAULT_DAILY_LIMIT, DEFAULT_DAILY_TOKEN_LIMIT, DEFAULT_RPM_LIMIT), provider);
    }

    private Quota applyProviderLimits(Quota quota, APIProvider provider) {
        if (provider.getRateLimitPerMinute() != null) {
            quota.setRpmLimit(provider.getRateLimitPerMinute());
        }
        if (provider.getMonthlyQuota() != null) {
            quota.setMonthlyLimit(provider.getMonthlyQuota());
            quota.setDailyLimit(Math.max(1, provider.getMonthlyQuota() / 30));
        }
        quota.getMetadata().put("freeQuotaPercent", provider.getFreeQuotaPercent());
        quota.getMetadata().put("alertThreshold", provider.getAlertThreshold());
        return quota;
    }

    private void persistQuotas() {
        localJsonStoreService.write(STORE_PATH, quotas);
    }
}
