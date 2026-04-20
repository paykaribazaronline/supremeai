package com.supremeai.service.quota;

import com.supremeai.model.UserSimulatorProfile;
import com.supremeai.model.UserTier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Simulator Quota Manager - Unified implementation for simulator install quotas
 */
@Service
public class SimulatorQuotaManager extends AbstractQuotaManager<UserSimulatorProfile> {

    @Override
    protected void initializeExtractors() {
        currentUsageExtractor = profile -> (long) profile.getActiveInstalls();
        quotaLimitExtractor = profile -> (long) profile.getInstallQuota();
        unlimitedChecker = profile -> profile.getUserTier() != null && profile.getUserTier().isUnlimited();
        quotaLimitSetter = (profile, limit) -> profile.setInstallQuota(limit.intValue());
        usageIncrementer = (profile, delta) -> {
            int newCount = Math.max(0, profile.getActiveInstalls() + delta.intValue());
            profile.setActiveInstalls(newCount);
        };
    }

    @Override
    protected long getTierQuota(UserTier tier) {
        return configService.getMaxSimulatorInstallsForTier(tier);
    }

    @Override
    public LocalDateTime getLastUsedAt(UserSimulatorProfile entity) {
        return entity != null ? entity.getLastActiveAt() : null;
    }

    /**
     * Check if app is already installed (simulator specific)
     */
    public boolean isAppInstalled(UserSimulatorProfile profile, String appId) {
        if (profile == null || profile.getInstalledApps() == null) return false;
        return profile.getInstalledApps().stream()
            .anyMatch(app -> app.getAppId().equals(appId));
    }
}
