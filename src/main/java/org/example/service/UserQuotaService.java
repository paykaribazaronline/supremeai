package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.model.UserTier;
import org.example.model.UserQuotaAllocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User Quota Service
 * Manages per-user quotas based on their tier
 * SUPERADMIN (you) = unlimited access
 */
@Service
public class UserQuotaService {
    private static final Logger logger = LoggerFactory.getLogger(UserQuotaService.class);
    
    @Autowired
    private LocalJsonStoreService jsonStore;

    private static final String QUOTA_STORE_PATH = "user-quotas/quotas.json";

    private Map<String, UserQuotaAllocation> userQuotas = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        Map<String, UserQuotaAllocation> saved = jsonStore.read(
                QUOTA_STORE_PATH,
                new TypeReference<Map<String, UserQuotaAllocation>>() {},
                Map.of());
        userQuotas.putAll(saved);
        logger.info("✅ UserQuotaService ready — restored {} user quotas from disk", saved.size());
    }

    private void persistQuotas() {
        jsonStore.write(QUOTA_STORE_PATH, userQuotas);
    }
    
    /**
     * Get or create user quota allocation
     * SECURITY (Phase 11): No default tier assignments. All users start as FREE.
     * Admins must explicitly promote users via /api/tier endpoints.
     * ⚠️ CRITICAL: No special handling for "admin" or "supremeai" usernames!
     */
    public UserQuotaAllocation getUserQuota(String userId) {
        return userQuotas.computeIfAbsent(userId, key -> {
            // Default: FREE tier for ALL users (no exceptions, no special cases)
            UserTier tier = UserTier.FREE;
            logger.info("📊 User {} quota initialized with {} tier", key, tier.name);
            
            return new UserQuotaAllocation(key, tier);
        });
    }
    
    /**
     * Check if user can make an API request
     */
    public boolean canMakeRequest(String userId) {
        UserQuotaAllocation quota = getUserQuota(userId);
        boolean allowed = quota.canMakeAPIRequest();
        
        if (!allowed) {
            logger.warn("❌ User {} quota exceeded: {}/{} requests today",
                userId, quota.getRequestsUsedToday(), quota.getTier().dailyLimit);
        }
        
        return allowed;
    }
    
    /**
     * Check if user can create an app
     */
    public boolean canCreateApp(String userId) {
        UserQuotaAllocation quota = getUserQuota(userId);
        boolean allowed = quota.canCreateApp();
        
        if (!allowed) {
            logger.warn("❌ User {} app creation limit exceeded: {}/{} apps today",
                userId, quota.getAppsCreatedToday(), quota.getTier().appCreationsPerDay);
        }
        
        return allowed;
    }
    
    /**
     * Record an API request for user
     */
    public void recordRequest(String userId) {
        UserQuotaAllocation quota = getUserQuota(userId);
        quota.recordAPIRequest();
        persistQuotas();
        logger.debug("📊 User {} API request recorded: {}/{} requests today",
            userId, quota.getRequestsUsedToday(), quota.getTier().dailyLimit);
    }
    
    /**
     * Record an app creation for user
     */
    public void recordAppCreation(String userId) {
        UserQuotaAllocation quota = getUserQuota(userId);
        quota.recordAppCreation();
        persistQuotas();
        logger.debug("📝 User {} app creation recorded: {}/{} apps today",
            userId, quota.getAppsCreatedToday(), quota.getTier().appCreationsPerDay);
    }
    
    /**
     * Set user tier (admin function)
     */
    public void setUserTier(String userId, UserTier tier) {
        UserQuotaAllocation quota = getUserQuota(userId);
        quota.setTier(tier);
        persistQuotas();
        logger.info("⚙️ User {} tier changed to: {}", userId, tier.name);
    }
    
    /**
     * Get user's remaining quota
     */
    public Map<String, Object> getQuotaStatus(String userId) {
        UserQuotaAllocation quota = getUserQuota(userId);
        
        Map<String, Object> status = new HashMap<>();
        status.put("userId", userId);
        status.put("tier", quota.getTier().name);
        status.put("tierPrice", "$" + quota.getTier().monthlyPrice + "/month");
        status.put("requestsUsedToday", quota.getRequestsUsedToday());
        status.put("dailyLimit", quota.getTier().dailyLimit == -1 ? "UNLIMITED" : quota.getTier().dailyLimit);
        status.put("remainingDaily", quota.getRemainingDailyRequests());
        status.put("requestsUsedThisMonth", quota.getRequestsUsedThisMonth());
        status.put("monthlyLimit", quota.getTier().monthlyLimit == -1 ? "UNLIMITED" : quota.getTier().monthlyLimit);
        status.put("remainingMonthly", quota.getRemainingMonthlyRequests());
        status.put("appsCreatedToday", quota.getAppsCreatedToday());
        status.put("appCreationLimit", quota.getTier().appCreationsPerDay == -1 ? "UNLIMITED" : quota.getTier().appCreationsPerDay);
        status.put("remainingAppCreations", quota.getRemainingAppCreations());
        status.put("isUnlimited", quota.getTier().isUnlimited());
        
        return status;
    }
    
    /**
     * Reset monthly quotas (call on 1st of month)
     */
    public void resetMonthlyQuotas() {
        logger.info("🔄 Resetting monthly quotas for all users");
        for (UserQuotaAllocation quota : userQuotas.values()) {
            quota.setRequestsUsedThisMonth(0);
        }
        persistQuotas();
    }
    
    /**
     * Get all user quotas (admin view)
     */
    public Map<String, UserQuotaAllocation> getAllUserQuotas() {
        return new HashMap<>(userQuotas);
    }
    
    /**
     * Check if multiple users can all make requests
     */
    public List<String> checkBatchRequests(List<String> userIds) {
        List<String> usersWithoutQuota = new ArrayList<>();
        for (String userId : userIds) {
            if (!canMakeRequest(userId)) {
                usersWithoutQuota.add(userId);
            }
        }
        return usersWithoutQuota;
    }
}
