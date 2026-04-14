package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import org.example.model.APIProvider;
import org.example.model.Quota;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
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
        return canUseAI(providerId, false);
    }

    /**
     * Check if an AI provider has available quota.
     * @param systemOperation when true, only checks the hard daily/monthly limit
     *                        (skips the 20% reserve buffer). Used by internal system
     *                        operations such as project improvement and idle research
     *                        so that SUPERADMIN / system tasks can use providers up
     *                        to their actual limit.
     */
    public boolean canUseAI(String providerId, boolean systemOperation) {
        syncConfiguredProviders();
        Quota quota = quotas.get(providerId);
        if (quota == null) {
            logger.warn("⚠️ No quota found for provider: {}", providerId);
            return false;
        }

        boolean available;
        if (systemOperation) {
            // System operations only respect the hard limit — no 20% reserve
            available = quota.hasQuotaAvailable();
        } else {
            available = quota.hasQuotaAvailable() &&
                       quota.getRemainingPercentage() >= MINIMUM_QUOTA_PERCENT;
        }

        if (!available) {
            logger.warn("❌ Quota exhausted for {}: {}% used, status = {} (system={})",
                providerId, quota.getUsagePercentage(), quota.getStatus(), systemOperation);
        }
        
        return available;
    }
    
    /**
     * Get all providers with available quota (for intelligent selection)
     */
    public List<String> getAvailableProviders() {
        return getAvailableProviders(false);
    }

    /**
     * Get all providers with available quota.
     * @param systemOperation when true, skips the 20% reserve buffer check.
     */
    public List<String> getAvailableProviders(boolean systemOperation) {
        syncConfiguredProviders();
        List<String> available = new ArrayList<>();
        for (String providerId : quotas.keySet()) {
            if (canUseAI(providerId, systemOperation)) {
                available.add(providerId);
            }
        }
        logger.info("📊 Available providers: {}/{} (system={})", available.size(), quotas.size(), systemOperation);
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
    @Scheduled(cron = "0 0 0 * * *") // Midnight every day
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

    @Scheduled(cron = "0 0 0 1 * *") // First day of each month at midnight
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
     * Check if we need fallback for system operations (relaxed threshold).
     */
    public boolean shouldUseFallbackForSystem() {
        return getAvailableProviders(true).isEmpty();
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
     * Get detailed status for each provider
     */
    public Map<String, Map<String, Object>> getQuotaStatus() {
        syncConfiguredProviders();
        Map<String, Map<String, Object>> status = new LinkedHashMap<>();
        for (Map.Entry<String, Quota> entry : quotas.entrySet()) {
            Quota q = entry.getValue();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("status", q.getStatus());
            details.put("usage_percentage", q.getUsagePercentage());
            details.put("remaining_percentage", q.getRemainingPercentage());
            details.put("requests_today", q.getRequestsUsedToday());
            details.put("daily_limit", q.getDailyLimit());
            details.put("requests_month", q.getRequestsUsedThisMonth());
            details.put("monthly_limit", q.getMonthlyLimit());
            status.put(entry.getKey(), details);
        }
        return status;
    }

    /**
     * Get total remaining quota across all providers
     */
    public int getTotalRemainingQuota() {
        syncConfiguredProviders();
        return (int) quotas.values().stream()
                .mapToLong(Quota::getRemainingRequests)
                .sum();
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

    /**
     * Create quota for ANY provider — fully dynamic, no hardcoded provider names.
     * Uses admin-configured limits from APIProvider model. Falls back to sensible defaults.
     * When admin adds a new AI provider, its quota auto-appears with these defaults.
     */
    private Quota createQuota(APIProvider provider) {
        String providerId = provider.getId();
        String providerName = provider.getName();

        // Use admin-configured values from APIProvider, or sensible defaults
        long dailyLimit = DEFAULT_DAILY_LIMIT;
        long dailyTokenLimit = DEFAULT_DAILY_TOKEN_LIMIT;
        double rpmLimit = DEFAULT_RPM_LIMIT;

        if (provider.getMonthlyQuota() != null && provider.getMonthlyQuota() > 0) {
            dailyLimit = Math.max(1, provider.getMonthlyQuota() / 30);
        }
        if (provider.getRateLimitPerMinute() != null && provider.getRateLimitPerMinute() > 0) {
            rpmLimit = provider.getRateLimitPerMinute();
        }

        return applyProviderLimits(new Quota(providerId, providerName, dailyLimit, dailyTokenLimit, rpmLimit), provider);
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
