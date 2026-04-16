package com.supremeai.service;

import com.supremeai.model.UserApi;
import com.supremeai.repository.UserApiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class QuotaService {

    @Autowired
    private UserApiRepository userApiRepository;

    /**
     * Check if an API key has quota remaining for the current month
     */
    public boolean hasQuotaRemaining(String apiKey) {
        Optional<UserApi> apiOpt = userApiRepository.findByApiKey(apiKey);
        if (apiOpt.isEmpty()) {
            return false;
        }

        UserApi api = apiOpt.get();
        if (!api.getIsActive()) {
            return false;
        }

        return api.hasQuotaRemaining();
    }

    /**
     * Increment usage for an API key
     * Returns true if successful, false if quota exceeded
     */
    @Transactional
    public boolean incrementUsage(String apiKey) {
        Optional<UserApi> apiOpt = userApiRepository.findByApiKey(apiKey);
        if (apiOpt.isEmpty()) {
            return false;
        }

        UserApi api = apiOpt.get();
        if (!api.getIsActive()) {
            return false;
        }

        if (!api.hasQuotaRemaining()) {
            return false;
        }

        api.incrementUsage();
        userApiRepository.save(api);
        return true;
    }

    /**
     * Get current usage for an API key
     */
    public Long getCurrentUsage(String apiKey) {
        Optional<UserApi> apiOpt = userApiRepository.findByApiKey(apiKey);
        return apiOpt.map(UserApi::getCurrentUsage).orElse(0L);
    }

    /**
     * Get monthly quota for an API key
     */
    public Long getMonthlyQuota(String apiKey) {
        Optional<UserApi> apiOpt = userApiRepository.findByApiKey(apiKey);
        return apiOpt.map(UserApi::getMonthlyQuota).orElse(0L);
    }

    /**
     * Reset monthly usage for all APIs - runs on the 1st of every month at midnight
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void resetMonthlyUsage() {
        userApiRepository.resetMonthlyUsage(LocalDateTime.now());
    }

    /**
     * Manually reset usage for a specific API (admin function)
     */
    @Transactional
    public boolean resetApiUsage(String apiKey) {
        Optional<UserApi> apiOpt = userApiRepository.findByApiKey(apiKey);
        if (apiOpt.isEmpty()) {
            return false;
        }

        UserApi api = apiOpt.get();
        api.resetMonthlyUsage();
        userApiRepository.save(api);
        return true;
    }

    /**
     * Get usage statistics for an API
     */
    public ApiUsageStats getUsageStats(String apiKey) {
        Optional<UserApi> apiOpt = userApiRepository.findByApiKey(apiKey);
        if (apiOpt.isEmpty()) {
            return null;
        }

        UserApi api = apiOpt.get();
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