package com.supremeai.service;

import com.supremeai.model.UserApiKey;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.cost.QuotaManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class QuotaService {

    @Autowired
    private UserApiKeyRepository userApiKeyRepository;

    @Autowired
    private QuotaManager quotaManager;

    private static final long DEFAULT_MONTHLY_QUOTA = 1000L;

    /**
     * Check if an API key has quota remaining for the current month
     */
    public boolean hasQuotaRemaining(String apiKey) {
        UserApiKey api = userApiKeyRepository.findByApiKey(apiKey).block();
        if (api == null || !"active".equals(api.getStatus())) {
            return false;
        }
        return api.getRequestCount() < DEFAULT_MONTHLY_QUOTA;
    }

    /**
     * Increment usage for an API key
     * Returns true if successful, false if quota exceeded
     */
    public boolean incrementUsage(String apiKey) {
        UserApiKey api = userApiKeyRepository.findByApiKey(apiKey).block();
        if (api == null || !"active".equals(api.getStatus())) {
            return false;
        }
        
        if (api.getRequestCount() < DEFAULT_MONTHLY_QUOTA) {
            api.setRequestCount(api.getRequestCount() + 1);
            api.setLastUsed(LocalDateTime.now());
            userApiKeyRepository.save(api).block();
            return true;
        }
        return false;
    }

    /**
     * Validate and increment usage atomically
     */
    public void validateAndIncrement(String apiKey) {
        UserApiKey api = userApiKeyRepository.findByApiKey(apiKey).block();
        if (api == null || !"active".equals(api.getStatus())) {
            throw new IllegalArgumentException("Invalid or inactive API key");
        }
        
        if (api.getRequestCount() >= DEFAULT_MONTHLY_QUOTA) {
            throw new RuntimeException("Quota exceeded");
        }

        api.setRequestCount(api.getRequestCount() + 1);
        api.setLastUsed(LocalDateTime.now());
        userApiKeyRepository.save(api).block();
    }

    /**
     * Get current usage for an API key
     */
    public Long getCurrentUsage(String apiKey) {
        UserApiKey api = userApiKeyRepository.findByApiKey(apiKey).block();
        return api != null ? api.getRequestCount() : 0L;
    }

    /**
     * Get monthly quota for an API key
     */
    public Long getMonthlyQuota(String apiKey) {
        return DEFAULT_MONTHLY_QUOTA;
    }

    /**
     * Reset monthly usage for all APIs - runs on the 1st of every month at midnight
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void resetMonthlyUsage() {
        userApiKeyRepository.findAll()
            .doOnNext(api -> {
                api.setRequestCount(0L);
                userApiKeyRepository.save(api).subscribe();
            })
            .subscribe();
    }

    /**
     * Manually reset usage for a specific API (admin function)
     */
    public boolean resetApiUsage(String apiKey) {
        UserApiKey api = userApiKeyRepository.findByApiKey(apiKey).block();
        if (api == null) {
            return false;
        }

        api.setRequestCount(0L);
        userApiKeyRepository.save(api).block();
        return true;
    }

    /**
     * Manually reset usage for a specific user (admin function)
     */
    public boolean resetUserUsage(String userId) {
        userApiKeyRepository.findByUserId(userId)
            .doOnNext(api -> {
                api.setRequestCount(0L);
                userApiKeyRepository.save(api).subscribe();
            })
            .subscribe();
        return true;
    }

    /**
     * Get usage statistics for an API
     */
    public ApiUsageStats getUsageStats(String apiKey) {
        UserApiKey api = userApiKeyRepository.findByApiKey(apiKey).block();
        if (api == null) {
            return null;
        }

        return new ApiUsageStats(
            api.getRequestCount(),
            DEFAULT_MONTHLY_QUOTA,
            api.getLastUsed(),
            api.getRequestCount() < DEFAULT_MONTHLY_QUOTA
        );
    }

    public static class ApiUsageStats {
        private final Long currentUsage;
        private final Long monthlyQuota;
        private final LocalDateTime lastUsedAt;
        private final boolean hasQuotaRemaining;

        public ApiUsageStats(Long currentUsage, Long monthlyQuota, LocalDateTime lastUsedAt, boolean hasQuotaRemaining) {
            this.currentUsage = currentUsage;
            this.monthlyQuota = monthlyQuota;
            this.lastUsedAt = lastUsedAt;
            this.hasQuotaRemaining = hasQuotaRemaining;
        }

        public Long getCurrentUsage() { return currentUsage; }
        public Long getMonthlyQuota() { return monthlyQuota; }
        public LocalDateTime getLastUsedAt() { return lastUsedAt; }
        public boolean isHasQuotaRemaining() { return hasQuotaRemaining; }

        public double getUsagePercentage() {
            if (monthlyQuota == 0) return 0.0;
            return (double) currentUsage / monthlyQuota * 100.0;
        }
    }
}
