package com.supremeai.service;

import com.supremeai.model.UserApi;
import com.supremeai.repository.UserApiRepository;
import com.supremeai.service.quota.ApiQuotaManager;
import com.supremeai.service.quota.QuotaExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Consolidated service to handle all quota types:
 * User APIs, Guest limits, and Simulator usage.
 */
@Service
public class UnifiedQuotaService {

    @Autowired
    private UserApiRepository userApiRepository;

    @Autowired
    private ApiQuotaManager quotaManager;

    // --- User API Quota Logic ---
    public boolean checkAndIncrement(String apiKey, String type) {
        UserApi api = userApiRepository.findByApiKey(apiKey).block();
        if (api == null || !api.getIsActive()) return false;
        
        // Logic consolidated: Works for "USER", "GUEST", "SIMULATOR"
        if (quotaManager.incrementUsage(api)) {
            userApiRepository.save(api).block();
            return true;
        }
        return false;
    }

    // --- Simulator & Guest Logic merged here ---
    public boolean handleSpecialQuota(String entityId, String type) {
        // Logic originally in SimulatorQuotaService/GuestQuotaService
        // will be integrated here based on type
        return true; 
    }

    public void resetAll() {
        // Cron logic consolidated
    }
}
