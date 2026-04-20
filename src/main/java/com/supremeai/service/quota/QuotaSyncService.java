package com.supremeai.service.quota;

import com.supremeai.model.SystemConfig;
import com.supremeai.model.UserTier;
import com.supremeai.repository.UserApiRepository;
import com.supremeai.repository.UserSimulatorProfileRepository;
import com.supremeai.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Central Quota Sync Service
 * Handles automatic sync of all user quotas when global config changes
 */
@Service
public class QuotaSyncService {

    private static final Logger logger = LoggerFactory.getLogger(QuotaSyncService.class);

    @Autowired
    private ConfigService configService;

    @Autowired
    private UserApiRepository userApiRepository;

    @Autowired
    private UserSimulatorProfileRepository simulatorProfileRepository;

    @Autowired
    private ApiQuotaManager apiQuotaManager;

    @Autowired
    private SimulatorQuotaManager simulatorQuotaManager;

    @PostConstruct
    public void init() {
        logger.info("Quota Sync Service initialized");
    }

    /**
     * Sync all user quotas with current tier configuration
     * Run this after admin changes global tier quotas
     */
    @Async
    public void syncAllUserQuotas() {
        logger.info("Starting full quota sync for all users...");
        
        // Sync all API keys
        userApiRepository.findAll()
            .doOnNext(api -> {
                apiQuotaManager.syncWithTierConfig(api, api.getUserTier());
                userApiRepository.save(api).subscribe();
            })
            .doOnComplete(() -> logger.info("All API key quotas synced successfully"))
            .subscribe();

        // Sync all simulator profiles
        simulatorProfileRepository.findAll()
            .doOnNext(profile -> {
                simulatorQuotaManager.syncWithTierConfig(profile, profile.getUserTier());
                simulatorProfileRepository.save(profile).subscribe();
            })
            .doOnComplete(() -> logger.info("All simulator quotas synced successfully"))
            .subscribe();
    }

    /**
     * Sync quotas for a specific user tier
     */
    @Async
    public void syncTierQuotas(UserTier tier) {
        logger.info("Syncing quotas for tier: {}", tier);
        
        long newApiQuota = configService.getQuotaForTier(tier);
        int newSimulatorQuota = configService.getMaxSimulatorInstallsForTier(tier);

        userApiRepository.findByUserTier(tier)
            .doOnNext(api -> {
                api.setMonthlyQuota(newApiQuota);
                userApiRepository.save(api).subscribe();
            })
            .subscribe();

        simulatorProfileRepository.findByUserTier(tier)
            .doOnNext(profile -> {
                profile.setInstallQuota(newSimulatorQuota);
                simulatorProfileRepository.save(profile).subscribe();
            })
            .subscribe();
    }

    /**
     * Event listener for config changes
     * Automatically syncs all quotas when admin updates system config
     */
    @EventListener
    public void onConfigUpdated(SystemConfig config) {
        logger.info("System config updated - triggering full quota sync");
        syncAllUserQuotas();
    }
}
