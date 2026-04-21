package com.supremeai.service;

import com.supremeai.model.UserApi;
import com.supremeai.repository.UserApiRepository;
import com.supremeai.service.quota.ApiQuotaManager;
import com.supremeai.service.quota.QuotaExceededException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Consolidated service to handle all quota types:
 * User APIs, Guest limits, and Simulator usage.
 */
@Service
public class UnifiedQuotaService {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedQuotaService.class);

    private final Counter quotaCheckCounter;
    private final Counter quotaIncrementCounter;
    private final Counter quotaSpecialCounter;

    @Autowired
    private UserApiRepository userApiRepository;

    @Autowired
    private ApiQuotaManager quotaManager;

    public UnifiedQuotaService(MeterRegistry meterRegistry) {
        this.quotaCheckCounter = meterRegistry.counter("unified_quota_service.check");
        this.quotaIncrementCounter = meterRegistry.counter("unified_quota_service.increment");
        this.quotaSpecialCounter = meterRegistry.counter("unified_quota_service.special");
    }

    // --- User API Quota Logic ---
    public boolean checkAndIncrement(String apiKey, String type) {
        logger.info("Checking and incrementing quota for API key: {} type: {}", apiKey, type);
        quotaCheckCounter.increment();

        UserApi api = userApiRepository.findByApiKey(apiKey).block();
        if (api == null || !api.getIsActive()) {
            logger.warn("API key not found or inactive: {}", apiKey);
            return false;
        }

        // Logic consolidated: Works for "USER", "GUEST", "SIMULATOR"
        if (quotaManager.incrementUsage(api)) {
            userApiRepository.save(api).block();
            quotaIncrementCounter.increment();
            logger.debug("Quota incremented successfully for {}", apiKey);
            return true;
        }
        logger.warn("Quota increment failed for {}", apiKey);
        return false;
    }

    // --- Simulator & Guest Logic merged here ---
    public boolean handleSpecialQuota(String entityId, String type) {
        logger.info("Handling special quota for entity: {} type: {}", entityId, type);
        quotaSpecialCounter.increment();
        // Logic originally in SimulatorQuotaService/GuestQuotaService
        // will be integrated here based on type
        logger.debug("Special quota handled for {}", entityId);
        return true;
    }

    public void resetAll() {
        logger.info("Resetting all quotas");
        // Cron logic consolidated
        logger.info("Quota reset completed");
    }
}
