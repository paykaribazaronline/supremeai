package com.supremeai.service.quota;

import com.supremeai.model.UserApi;
import com.supremeai.model.UserTier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * API Quota Manager - Unified implementation for API call quotas
 */
@Service
public class ApiQuotaManager extends AbstractQuotaManager<UserApi> {

    @Override
    protected void initializeExtractors() {
        currentUsageExtractor = UserApi::getCurrentUsage;
        quotaLimitExtractor = UserApi::getMonthlyQuota;
        unlimitedChecker = api -> api.getUserTier() != null && api.getUserTier().isUnlimited();
        quotaLimitSetter = UserApi::setMonthlyQuota;
        usageIncrementer = (api, delta) -> {
            long newUsage = Math.max(0, api.getCurrentUsage() + delta);
            api.setCurrentUsage(newUsage);
            api.setLastUsedAt(LocalDateTime.now());
        };
    }

    @Override
    protected long getTierQuota(UserTier tier) {
        return configService.getQuotaForTier(tier);
    }

    @Override
    public LocalDateTime getLastUsedAt(UserApi entity) {
        return entity != null ? entity.getLastUsedAt() : null;
    }
}
