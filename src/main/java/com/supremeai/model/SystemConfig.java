package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.util.HashMap;
import java.util.Map;

/**
 * Global system configuration stored in Firestore.
 * This allows administrators to change quotas and AI settings in real-time.
 * Collection: "system_configs", Document ID: "global_settings"
 */
@Document(collectionName = "system_configs")
public class SystemConfig {

    @DocumentId
    private String id = "global_settings";

    // Quota limits for different tiers (Number of installs/AI calls)
    // Firestore does not support enum keys, so we use String keys
    private Map<String, Long> tierQuotas = new HashMap<>();

    // Maximum number of API Keys a user can create based on tier
    private Map<String, Integer> tierMaxApis = new HashMap<>();

    // Maximum number of apps user can have installed in simulator simultaneously
    private Map<String, Integer> tierMaxSimulatorInstalls = new HashMap<>();

    // General AI Settings
    private String activeModel = "gpt-4o";
    private boolean maintenanceMode = false;
    private String systemMessage = "You are SupremeAI, an expert software architect and assistant.";

    public SystemConfig() {
        // Default quotas as a fallback
        tierQuotas.put(UserTier.GUEST.name(), 5L);
        tierQuotas.put(UserTier.FREE.name(), 20L);
        tierQuotas.put(UserTier.BASIC.name(), 100L);
        tierQuotas.put(UserTier.PRO.name(), 500L);
        tierQuotas.put(UserTier.ENTERPRISE.name(), 2000L);
        tierQuotas.put(UserTier.ADMIN.name(), -1L); // Unlimited

        // Default API limits
        tierMaxApis.put(UserTier.GUEST.name(), 0);
        tierMaxApis.put(UserTier.FREE.name(), 1);
        tierMaxApis.put(UserTier.BASIC.name(), 3);
        tierMaxApis.put(UserTier.PRO.name(), 10);
        tierMaxApis.put(UserTier.ENTERPRISE.name(), 50);
        tierMaxApis.put(UserTier.ADMIN.name(), 100);

        // Default Simulator Limits
        tierMaxSimulatorInstalls.put(UserTier.GUEST.name(), 1);
        tierMaxSimulatorInstalls.put(UserTier.FREE.name(), 3);
        tierMaxSimulatorInstalls.put(UserTier.BASIC.name(), 5);
        tierMaxSimulatorInstalls.put(UserTier.PRO.name(), 10);
        tierMaxSimulatorInstalls.put(UserTier.ENTERPRISE.name(), 20);
        tierMaxSimulatorInstalls.put(UserTier.ADMIN.name(), 50);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Map<String, Long> getTierQuotas() { return tierQuotas; }
    public void setTierQuotas(Map<String, Long> tierQuotas) { this.tierQuotas = tierQuotas; }

    public Map<String, Integer> getTierMaxApis() { return tierMaxApis; }
    public void setTierMaxApis(Map<String, Integer> tierMaxApis) { this.tierMaxApis = tierMaxApis; }

    public Map<String, Integer> getTierMaxSimulatorInstalls() { return tierMaxSimulatorInstalls; }
    public void setTierMaxSimulatorInstalls(Map<String, Integer> tierMaxSimulatorInstalls) { this.tierMaxSimulatorInstalls = tierMaxSimulatorInstalls; }

    public String getActiveModel() { return activeModel; }
    public void setActiveModel(String activeModel) { this.activeModel = activeModel; }

    public boolean isMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }

    public String getSystemMessage() { return systemMessage; }
    public void setSystemMessage(String systemMessage) { this.systemMessage = systemMessage; }

    /**
     * Helper to get quota for a specific tier
     */
    public long getQuotaForTier(UserTier tier) {
        return tierQuotas.getOrDefault(tier.name(), 0L);
    }

    /**
     * Helper to get max APIs for a specific tier
     */
    public int getMaxApisForTier(UserTier tier) {
        return tierMaxApis.getOrDefault(tier.name(), 0);
    }

    /**
     * Helper to get max simulator installs for a specific tier
     */
    public int getMaxSimulatorInstallsForTier(UserTier tier) {
        return tierMaxSimulatorInstalls.getOrDefault(tier.name(), 0);
    }
}
