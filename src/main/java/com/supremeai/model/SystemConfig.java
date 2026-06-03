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

    // Generic configuration maps to replace hardcoded values
    private Map<String, Object> settings = new HashMap<>();
    private Map<String, String> collections = new HashMap<>();
    private Map<String, Long> timeouts = new HashMap<>();
    private Map<String, Double> thresholds = new HashMap<>();
    private Map<String, Object> uiMetadata = new HashMap<>();

    // General AI Settings
    private String activeModel = "default";   // resolved from provider registry at runtime
    private String smallModel = "default";    // resolved from provider registry at runtime
    private Long version = 1L;
    private boolean maintenanceMode = false;
    private boolean emergencyStop = false;
    private boolean apiAccessLock = false;
    private String apiRotationStrategy = "quota-based";
    private Object autoExecApprovalRequired = true;
    private boolean fullAuthority = false;
    private String shareMode = "manual";
    private boolean enableExternalDirectory = false;
    private boolean emailNotifications = true;
    private boolean smsAlerts = true;
    private String systemMessage = "You are SupremeAI, an expert software architect and assistant.";
    private Map<String, String> permissions = new HashMap<>();
    private Map<String, Map<String, Object>> providers = new HashMap<>();
    private java.util.List<String> adminEmails = new java.util.ArrayList<>();
    private boolean autonomousLearningEnabled = true;
    private boolean autonomousAuditEnabled = true;

    private Map<String, Object> telegramConfig = new HashMap<>();
    private Map<String, Object> supabaseConfig = new HashMap<>();

    public SystemConfig() {
        // Default Telegram configuration
        telegramConfig.put("enabled", false);
        telegramConfig.put("teldriveUrl", "http://localhost:8080");
        telegramConfig.put("apiToken", "");
        telegramConfig.put("channelId", "");
        telegramConfig.put("apiId", "");
        telegramConfig.put("apiHash", "");
        telegramConfig.put("botToken", "");
        telegramConfig.put("status", "DISCONNECTED");
        telegramConfig.put("storageUsed", "0 B");
        telegramConfig.put("lastSync", "");

        // Default Supabase configuration
        supabaseConfig.put("dbUrl", "");
        supabaseConfig.put("password", "");
        supabaseConfig.put("status", "PENDING");
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

        // Default tool permissions
        permissions.put("read", "allow");
        permissions.put("edit", "ask");
        permissions.put("bash", "ask");
        permissions.put("task", "allow");
        permissions.put("websearch", "allow");
        permissions.put("external_directory", "deny");

        // Initialize collections
        collections.put("repositories", "codeflow/repositories");
        collections.put("configs", "system_configs");
        collections.put("memories", "solution-memories");
        collections.put("learning", "system-learning");
        collections.put("knowledge", "database-knowledge");

        // Initialize timeouts (in milliseconds)
        timeouts.put("voting_timeout", 15000L);
        timeouts.put("cache_duration", 300000L);
        timeouts.put("api_timeout", 30000L);
        timeouts.put("io_timeout", 30000L);
        timeouts.put("simulator_deployment", 120000L);
        timeouts.put("simulator_session", 1800000L);

        // Initialize thresholds
        thresholds.put("consensus", 0.60);
        thresholds.put("min_clarity", 0.6);
        thresholds.put("idea_detection", 20.0);
        thresholds.put("retrain_threshold", 0.05);

        // Initialize settings
        settings.put("max_retries", 3);
        settings.put("initial_backoff_ms", 500);
        settings.put("backoff_multiplier", 2.0);
        settings.put("max_recent_logs", 1000);
        settings.put("cache_ttl_minutes", 30);
        this.autonomousLearningEnabled = true;
        this.autonomousAuditEnabled = true;
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

    public Map<String, Object> getSettings() { return settings; }
    public void setSettings(Map<String, Object> settings) { this.settings = settings; }

    public Map<String, String> getCollections() { return collections; }
    public void setCollections(Map<String, String> collections) { this.collections = collections; }

    public Map<String, Long> getTimeouts() { return timeouts; }
    public void setTimeouts(Map<String, Long> timeouts) { this.timeouts = timeouts; }

    public Map<String, Double> getThresholds() { return thresholds; }
    public void setThresholds(Map<String, Double> thresholds) { this.thresholds = thresholds; }

    public Map<String, Object> getUiMetadata() { return uiMetadata; }
    public void setUiMetadata(Map<String, Object> uiMetadata) { this.uiMetadata = uiMetadata; }

    public String getActiveModel() { return activeModel; }
    public void setActiveModel(String activeModel) { this.activeModel = activeModel; }

    public String getSmallModel() { return smallModel; }
    public void setSmallModel(String smallModel) { this.smallModel = smallModel; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public boolean isMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }

    public boolean isEmergencyStop() { return emergencyStop; }
    public void setEmergencyStop(boolean emergencyStop) { this.emergencyStop = emergencyStop; }

    public boolean isApiAccessLock() { return apiAccessLock; }
    public void setApiAccessLock(boolean apiAccessLock) { this.apiAccessLock = apiAccessLock; }

    public String getApiRotationStrategy() { return apiRotationStrategy; }
    public void setApiRotationStrategy(String apiRotationStrategy) { this.apiRotationStrategy = apiRotationStrategy; }

    public boolean isAutoExecApprovalRequired() { 
        if (autoExecApprovalRequired instanceof Boolean) {
            return (Boolean) autoExecApprovalRequired;
        } else if (autoExecApprovalRequired instanceof String) {
            return Boolean.parseBoolean((String) autoExecApprovalRequired);
        }
        return false;
    }
    public void setAutoExecApprovalRequired(Object autoExecApprovalRequired) { this.autoExecApprovalRequired = autoExecApprovalRequired; }

    public boolean isFullAuthority() { return fullAuthority; }
    public void setFullAuthority(boolean fullAuthority) { this.fullAuthority = fullAuthority; }

    public String getShareMode() { return shareMode; }
    public void setShareMode(String shareMode) { this.shareMode = shareMode; }

    public boolean isEnableExternalDirectory() { return enableExternalDirectory; }
    public void setEnableExternalDirectory(boolean enableExternalDirectory) { this.enableExternalDirectory = enableExternalDirectory; }

    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }

    public boolean isSmsAlerts() { return smsAlerts; }
    public void setSmsAlerts(boolean smsAlerts) { this.smsAlerts = smsAlerts; }

    public String getSystemMessage() { return systemMessage; }
    public void setSystemMessage(String systemMessage) { this.systemMessage = systemMessage; }

    public Map<String, String> getPermissions() { return permissions; }
    public void setPermissions(Map<String, String> permissions) { this.permissions = permissions; }

    public Map<String, Map<String, Object>> getProviders() { return providers; }
    public void setProviders(Map<String, Map<String, Object>> providers) { this.providers = providers; }

    public java.util.List<String> getAdminEmails() { return adminEmails; }
    public void setAdminEmails(java.util.List<String> adminEmails) { this.adminEmails = adminEmails; }

    public boolean isAutonomousLearningEnabled() { return autonomousLearningEnabled; }
    public void setAutonomousLearningEnabled(boolean autonomousLearningEnabled) { this.autonomousLearningEnabled = autonomousLearningEnabled; }

    public boolean isAutonomousAuditEnabled() { return autonomousAuditEnabled; }
    public void setAutonomousAuditEnabled(boolean autonomousAuditEnabled) { this.autonomousAuditEnabled = autonomousAuditEnabled; }

    public Map<String, Object> getTelegramConfig() { return telegramConfig; }
    public void setTelegramConfig(Map<String, Object> telegramConfig) { this.telegramConfig = telegramConfig; }

    public Map<String, Object> getSupabaseConfig() { return supabaseConfig; }
    public void setSupabaseConfig(Map<String, Object> supabaseConfig) { this.supabaseConfig = supabaseConfig; }

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

    /**
     * Helper to get a secret value for a specific provider.
     * Expects the secret to be stored in the 'providers' map under providerName -> secretKey.
     * 
     * @param providerName The name of the AI provider (e.g., "openai", "gemini")
     * @param secretKey The key for the secret (e.g., "apiKey")
     * @return The secret value, or null if not found
     */
    public String getProviderSecret(String providerName, String secretKey) {
        if (providers == null || !providers.containsKey(providerName)) {
            return null;
        }
        Map<String, Object> providerData = providers.get(providerName);
        if (providerData != null && providerData.containsKey(secretKey)) {
            Object value = providerData.get(secretKey);
            return value != null ? value.toString() : null;
        }
        return null;
    }
}
