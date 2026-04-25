package com.supremeai.service;

import com.supremeai.model.UserApiKey;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.cost.QuotaManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
    public Mono<Boolean> hasQuotaRemaining(String apiKey) {
        return userApiKeyRepository.findByApiKey(apiKey)
            .map(api -> "active".equals(api.getStatus()) && api.getRequestCount() < DEFAULT_MONTHLY_QUOTA)
            .defaultIfEmpty(false);
    }

    /**
     * Increment usage for an API key
     * Returns true if successful, false if quota exceeded
     */
    public Mono<Boolean> incrementUsage(String apiKey) {
        return userApiKeyRepository.findByApiKey(apiKey)
            .flatMap(api -> {
                if (!"active".equals(api.getStatus())) {
                    return Mono.just(false);
                }
                if (api.getRequestCount() < DEFAULT_MONTHLY_QUOTA) {
                    api.setRequestCount(api.getRequestCount() + 1);
                    api.setLastUsed(LocalDateTime.now());
                    return userApiKeyRepository.save(api).map(saved -> true);
                }
                return Mono.just(false);
            })
            .defaultIfEmpty(false);
    }

    /**
     * Validate and increment usage atomically
     */
    public Mono<Void> validateAndIncrement(String apiKey) {
        return userApiKeyRepository.findByApiKey(apiKey)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid API key")))
            .flatMap(api -> {
                if (!"active".equals(api.getStatus())) {
                    return Mono.error(new IllegalArgumentException("Inactive API key"));
                }
                if (api.getRequestCount() >= DEFAULT_MONTHLY_QUOTA) {
                    return Mono.error(new RuntimeException("Quota exceeded"));
                }
                api.setRequestCount(api.getRequestCount() + 1);
                api.setLastUsed(LocalDateTime.now());
                return userApiKeyRepository.save(api).then();
            });
    }

    /**
     * Get current usage for an API key
     */
    public Mono<Long> getCurrentUsage(String apiKey) {
        return userApiKeyRepository.findByApiKey(apiKey)
            .map(UserApiKey::getRequestCount)
            .defaultIfEmpty(0L);
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
    public Mono<Boolean> resetApiUsage(String apiKey) {
        return userApiKeyRepository.findByApiKey(apiKey)
            .flatMap(api -> {
                api.setRequestCount(0L);
                return userApiKeyRepository.save(api).map(saved -> true);
            })
            .defaultIfEmpty(false);
    }

    /**
     * Manually reset usage for a specific user (admin function)
     */
    public Mono<Void> resetUserUsage(String userId) {
        return userApiKeyRepository.findByUserId(userId)
            .flatMap(api -> {
                api.setRequestCount(0L);
                return userApiKeyRepository.save(api);
            })
            .then();
    }

    /**
     * Get usage statistics for an API
     */
    public Mono<ApiUsageStats> getUsageStats(String apiKey) {
        return userApiKeyRepository.findByApiKey(apiKey)
            .map(api -> new ApiUsageStats(
                api.getRequestCount(),
                DEFAULT_MONTHLY_QUOTA,
                api.getLastUsed(),
                api.getRequestCount() < DEFAULT_MONTHLY_QUOTA
            ));
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
