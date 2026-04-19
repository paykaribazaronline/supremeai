package com.supremeai.service;

import com.supremeai.model.UserApi;
import com.supremeai.repository.UserApiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class QuotaService {

    @Autowired
    private UserApiRepository userApiRepository;

    /**
     * Check if an API key has quota remaining for the current month
     */
    public boolean hasQuotaRemaining(String apiKey) {
        UserApi api = userApiRepository.findByApiKey(apiKey).block();
        if (api == null) {
            return false;
        }

        if (!api.getIsActive()) {
            return false;
        }

        return api.hasQuotaRemaining();
    }

    /**
     * Increment usage for an API key
     * Returns true if successful, false if quota exceeded
     */
    public boolean incrementUsage(String apiKey) {
        UserApi api = userApiRepository.findByApiKey(apiKey).block();
        if (api == null) {
            return false;
        }

        if (!api.getIsActive()) {
            return false;
        }

        if (!api.hasQuotaRemaining()) {
            return false;
        }

        api.incrementUsage();
        userApiRepository.save(api).block();
        return true;
    }

    /**
     * Get current usage for an API key
     */
    public Long getCurrentUsage(String apiKey) {
        UserApi api = userApiRepository.findByApiKey(apiKey).block();
        return api != null ? api.getCurrentUsage() : 0L;
    }

    /**
     * Get monthly quota for an API key
     */
    public Long getMonthlyQuota(String apiKey) {
        UserApi api = userApiRepository.findByApiKey(apiKey).block();
        return api != null ? api.getMonthlyQuota() : 0L;
    }

    /**
     * Reset monthly usage for all APIs - runs on the 1st of every month at midnight
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void resetMonthlyUsage() {
        userApiRepository.findAll()
            .doOnNext(api -> {
                api.resetMonthlyUsage();
                userApiRepository.save(api).subscribe();
            })
            .subscribe();
    }

    /**
     * Manually reset usage for a specific API (admin function)
     */
    public boolean resetApiUsage(String apiKey) {
        UserApi api = userApiRepository.findByApiKey(apiKey).block();
        if (api == null) {
            return false;
        }

        api.resetMonthlyUsage();
        userApiRepository.save(api).block();
        return true;
    }

    /**
     * Get usage statistics for an API
     */
    public ApiUsageStats getUsageStats(String apiKey) {
        UserApi api = userApiRepository.findByApiKey(apiKey).block();
        if (api == null) {
            return null;
        }

        return new ApiUsageStats(
            api.getCurrentUsage(),
            api.getMonthlyQuota(),
            api.getLastUsedAt(),
            api.hasQuotaRemaining()
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