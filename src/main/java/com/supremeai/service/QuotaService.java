package com.supremeai.service;

import com.supremeai.model.UserApiKey;
import com.supremeai.model.UserTier;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.cost.QuotaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Service for managing API key quotas and usage tracking.
 */
@Service
public class QuotaService {

    private static final Logger logger = LoggerFactory.getLogger(QuotaService.class);

    @Autowired
    private UserApiKeyRepository userApiKeyRepository;

    @Autowired
    private QuotaManager quotaManager;

    @Autowired
    private ConfigService configService;

    private long getMonthlyQuota() {
        return configService.getConfig().getTierQuotas().getOrDefault(UserTier.FREE.name(), 1000L);
    }

    private long getGuestMonthlyQuota() {
        return configService.getConfig().getTierQuotas().getOrDefault("GUEST", 50L);
    }

    /**
     * Check if an API key has quota remaining for the current month
     */
    public Mono<Boolean> hasQuotaRemaining(String apiKey) {
        String targetKey = (apiKey == null || "GUEST_MODE".equals(apiKey)) ? "GUEST_MODE" : apiKey;
        long limit = "GUEST_MODE".equals(targetKey) ? getGuestMonthlyQuota() : getMonthlyQuota();

        return userApiKeyRepository.findByApiKey(targetKey)
            .map(api -> "active".equalsIgnoreCase(api.getStatus()) && api.getRequestCount() < limit)
            .switchIfEmpty(Mono.defer(() -> {
                if ("GUEST_MODE".equals(targetKey)) {
                    // Create guest entry if it doesn't exist
                    UserApiKey guestApi = new UserApiKey();
                    guestApi.setApiKey("GUEST_MODE");
                    guestApi.setUserId("guest_user");
                    guestApi.setStatus("active");
                    guestApi.setRequestCount(0L);
                    guestApi.setAddedAt(LocalDateTime.now());
                    return userApiKeyRepository.save(guestApi).map(api -> true);
                }
                return Mono.just(false);
            }));
    }


    /**
     * Increment usage for an API key
     * Returns true if successful, false if quota exceeded
     */
    public Mono<Boolean> incrementUsage(String apiKey) {
        String targetKey = (apiKey == null || "GUEST_MODE".equals(apiKey)) ? "GUEST_MODE" : apiKey;
        long limit = "GUEST_MODE".equals(targetKey) ? getGuestMonthlyQuota() : getMonthlyQuota();

        return userApiKeyRepository.findByApiKey(targetKey)
            .flatMap(api -> {
                if (!"active".equalsIgnoreCase(api.getStatus())) {
                    return Mono.just(false);
                }
                if (api.getRequestCount() < limit) {
                    api.setRequestCount(api.getRequestCount() + 1);
                    api.setLastUsed(LocalDateTime.now());
                    return userApiKeyRepository.save(api).map(saved -> true);
                }
                return Mono.just(false);
            })
            .switchIfEmpty(Mono.defer(() -> {
                if ("GUEST_MODE".equals(targetKey)) {
                    UserApiKey guestApi = new UserApiKey();
                    guestApi.setApiKey("GUEST_MODE");
                    guestApi.setUserId("guest_user");
                    guestApi.setStatus("active");
                    guestApi.setRequestCount(1L);
                    guestApi.setLastUsed(LocalDateTime.now());
                    guestApi.setAddedAt(LocalDateTime.now());
                    return userApiKeyRepository.save(guestApi).map(api -> true);
                }
                return Mono.just(false);
            }));
    }


    /**
     * Validate and increment usage atomically with optimistic locking
     */
    public Mono<Void> validateAndIncrement(String apiKey) {
        return userApiKeyRepository.findByApiKey(apiKey)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid API key")))
            .flatMap(api -> {
                if (!"active".equalsIgnoreCase(api.getStatus())) {
                    return Mono.error(new IllegalArgumentException("Inactive API key"));
                }
                long currentCount = api.getRequestCount();
                if (currentCount >= getMonthlyQuota()) {
                    return Mono.error(new RuntimeException("Quota exceeded"));
                }

                // Atomic increment with version check for optimistic locking
                api.setRequestCount(currentCount + 1);
                api.setLastUsed(LocalDateTime.now());

                return userApiKeyRepository.save(api)
                    .onErrorResume(throwable -> {
                        // If save fails due to concurrent modification, retry once
                        logger.warn("Concurrent modification detected for API key {}, retrying", apiKey);
                        return userApiKeyRepository.findByApiKey(apiKey)
                            .flatMap(retryApi -> {
                                if (!"active".equalsIgnoreCase(retryApi.getStatus())) {
                                    return Mono.error(new IllegalArgumentException("Inactive API key"));
                                }
                                if (retryApi.getRequestCount() >= getMonthlyQuota()) {
                                    return Mono.error(new RuntimeException("Quota exceeded"));
                                }
                                retryApi.setRequestCount(retryApi.getRequestCount() + 1);
                                retryApi.setLastUsed(LocalDateTime.now());
                                return userApiKeyRepository.save(retryApi);
                            });
                    })
                    .then();
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
        return getMonthlyQuota();
    }

    /**
     * Reset monthly usage for all APIs - runs on the 1st of every month at midnight
     * Optimized for performance with batch processing
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void resetMonthlyUsage() {
        logger.info("Starting monthly usage reset for all API keys...");
        userApiKeyRepository.findAll()
            .map(api -> {
                api.setRequestCount(0L);
                return api;
            })
            .as(userApiKeyRepository::saveAll)
            .doOnError(error -> logger.error("Failed to reset monthly usage", error))
            .doOnComplete(() -> logger.info("Successfully reset monthly usage for all API keys"))
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
                getMonthlyQuota(),
                api.getLastUsed(),
                api.getRequestCount() < getMonthlyQuota()
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
