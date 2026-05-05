package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User simulator profile storing quota, installed apps, device config, and session state.
 * Stored in Firestore collection: "simulator_profiles"
 * Document ID = Firebase user ID
 */
@Document(collectionName = "simulator_profiles")
public class UserSimulatorProfile {

    @DocumentId
    private String userId;

    /** User's subscription tier */
    private UserTier userTier = UserTier.FREE;

    /** Maximum number of apps user can have installed simultaneously */
    private int installQuota = 5;

    /** Current number of installed apps (derived from installedApps.size()) */
    private int activeInstalls = 0;

    /** List of installed applications */
    private List<InstalledApp> installedApps = new ArrayList<>();

    /** Device configuration for simulator */
    private DeviceProfile device;

    /** Current active session, if any */
    private ActiveSession currentSession;

    /** Timestamp of last user activity */
    private LocalDateTime lastActiveAt;

    /** Historical quota usage tracking */
    private List<QuotaHistoryEntry> quotaHistory = new ArrayList<>();

    /** Time profile was created */
    @ServerTimestamp
    private LocalDateTime createdAt;

    /** Time profile was last updated */
    @ServerTimestamp
    private LocalDateTime updatedAt;

    public UserSimulatorProfile() {}

    public UserSimulatorProfile(String userId) {
        this.userId = userId;
        this.installQuota = 5;
        this.activeInstalls = 0;
        this.installedApps = new ArrayList<>();
        this.device = DeviceProfile.defaultDevice();
        this.lastActiveAt = LocalDateTime.now();
    }

    // ─── Getters & Setters ──────────────────────────────────────────────────────

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public UserTier getUserTier() { return userTier; }
    public void setUserTier(UserTier userTier) { this.userTier = userTier; }

    public int getInstallQuota() { return installQuota; }
    public void setInstallQuota(int installQuota) { this.installQuota = installQuota; }

    public int getActiveInstalls() { return activeInstalls; }
    public void setActiveInstalls(int activeInstalls) { this.activeInstalls = activeInstalls; }

    public List<InstalledApp> getInstalledApps() { return installedApps; }
    public void setInstalledApps(List<InstalledApp> installedApps) { this.installedApps = installedApps; }

    public DeviceProfile getDevice() { return device; }
    public void setDevice(DeviceProfile device) { this.device = device; }

    public ActiveSession getCurrentSession() { return currentSession; }
    public void setCurrentSession(ActiveSession currentSession) { this.currentSession = currentSession; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public List<QuotaHistoryEntry> getQuotaHistory() { return quotaHistory; }
    public void setQuotaHistory(List<QuotaHistoryEntry> quotaHistory) { this.quotaHistory = quotaHistory; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ─── Business Logic Helpers ─────────────────────────────────────────────────

    /**
     * Adds an installed app entry and increments active installs
     */
    public void addInstalledApp(InstalledApp app) {
        this.installedApps.add(app);
        this.activeInstalls = this.installedApps.size();
        this.lastActiveAt = LocalDateTime.now();
    }

    /**
     * Removes an installed app by appId
     * @return true if app was removed, false if not found
     */
    public boolean removeInstalledApp(String appId) {
        boolean removed = this.installedApps.removeIf(app -> app.getAppId().equals(appId));
        if (removed) {
            this.activeInstalls = this.installedApps.size();
            this.lastActiveAt = LocalDateTime.now();
        }
        return removed;
    }

    /**
     * Checks if an app is already installed
     */
    public boolean hasAppInstalled(String appId) {
        return this.installedApps.stream()
            .anyMatch(app -> app.getAppId().equals(appId));
    }

    /**
     * Gets an installed app by appId
     */
    public InstalledApp getInstalledApp(String appId) {
        return this.installedApps.stream()
            .filter(app -> app.getAppId().equals(appId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if user has remaining quota
     */
    public boolean hasQuotaRemaining() {
        return this.activeInstalls < this.installQuota;
    }

    /**
     * Returns how many install slots remain
     */
    public int getRemainingSlots() {
        return Math.max(0, this.installQuota - this.activeInstalls);
    }

    /**
     * Resets all installed apps and active count (admin operation)
     */
    public void resetAllInstalls() {
        this.installedApps.clear();
        this.activeInstalls = 0;
        this.currentSession = null;
        this.lastActiveAt = LocalDateTime.now();
    }

    // ─── Nested Model Classes ────────────────────────────────────────────────────

    /**
     * Represents a single installed application in the simulator
     */
    public static class InstalledApp {
        private String appId;              // Reference to generated app ID
        private String appName;            // User-friendly name
        private String version;            // Semantic version
        private String deployedUrl;        // Preview URL (Cloud Run)
        private LocalDateTime installedAt;
        private int launchCount = 0;       // How many times launched
        private LocalDateTime lastLaunchedAt;
        private AppStatus status = AppStatus.INSTALLED;
        private String failureReason;      // If status=ERROR

        public InstalledApp() {}

        public InstalledApp(String appId, String appName, String version, String deployedUrl) {
            this.appId = appId;
            this.appName = appName;
            this.version = version;
            this.deployedUrl = deployedUrl;
            this.installedAt = LocalDateTime.now();
        }

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }

        public String getAppName() { return appName; }
        public void setAppName(String appName) { this.appName = appName; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getDeployedUrl() { return deployedUrl; }
        public void setDeployedUrl(String deployedUrl) { this.deployedUrl = deployedUrl; }

        public LocalDateTime getInstalledAt() { return installedAt; }
        public void setInstalledAt(LocalDateTime installedAt) { this.installedAt = installedAt; }

        public int getLaunchCount() { return launchCount; }
        public void setLaunchCount(int launchCount) { this.launchCount = launchCount; }

        public LocalDateTime getLastLaunchedAt() { return lastLaunchedAt; }
        public void setLastLaunchedAt(LocalDateTime lastLaunchedAt) { this.lastLaunchedAt = lastLaunchedAt; }

        public AppStatus getStatus() { return status; }
        public void setStatus(AppStatus status) { this.status = status; }

        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

        /** Increment launch counter and update timestamp */
        public void recordLaunch() {
            this.launchCount++;
            this.lastLaunchedAt = LocalDateTime.now();
            this.status = AppStatus.RUNNING;
        }

        /** Mark app as errored during deployment/launch */
        public void markAsFailed(String reason) {
            this.status = AppStatus.ERROR;
            this.failureReason = reason;
        }
    }

    /**
     * Simulator device profile (emulated device)
     */
    public static class DeviceProfile {
        private DeviceType type = DeviceType.PIXEL_6;
        private String osVersion = "Android 14";
        private String screenResolution = "1080x2340";
        private int densityDpi = 440;
        private boolean hasGooglePlayServices = false;
        private java.util.Map<String, String> customProperties = new java.util.HashMap<>();

        public DeviceProfile() {}

        public static DeviceProfile defaultDevice() {
            DeviceProfile dp = new DeviceProfile();
            dp.type = DeviceType.PIXEL_6;
            dp.osVersion = "Android 14";
            dp.screenResolution = "1080x2340";
            dp.densityDpi = 440;
            return dp;
        }

        // Getters & Setters
        public DeviceType getType() { return type; }
        public void setType(DeviceType type) { this.type = type; }

        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

        public String getScreenResolution() { return screenResolution; }
        public void setScreenResolution(String screenResolution) { this.screenResolution = screenResolution; }

        public int getDensityDpi() { return densityDpi; }
        public void setDensityDpi(int densityDpi) { this.densityDpi = densityDpi; }

        public boolean isHasGooglePlayServices() { return hasGooglePlayServices; }
        public void setHasGooglePlayServices(boolean hasGooglePlayServices) { this.hasGooglePlayServices = hasGooglePlayServices; }

        public java.util.Map<String, String> getCustomProperties() { return customProperties; }
        public void setCustomProperties(java.util.Map<String, String> customProperties) { this.customProperties = customProperties; }

        /** Available device profiles (extensible) */
        public enum DeviceType {
            PIXEL_6("Google Pixel 6", "Android 14", "1080x2340", 440),
            PIXEL_7("Google Pixel 7", "Android 14", "1080x2400", 460),
            SAMSUNG_S24("Samsung Galaxy S24", "Android 14", "1080x2340", 416),
            IPHONE_15("iPhone 15", "iOS 17.4", "1179x2556", 460),
            IPHONE_15_PRO("iPhone 15 Pro", "iOS 17.4", "1179x2556", 460),
            TABLET_10("10-inch Tablet", "Android 13", "1920x1200", 224);

            private final String displayName;
            private final String osVersion;
            private final String resolution;
            private final int densityDpi;

            DeviceType(String displayName, String osVersion, String resolution, int densityDpi) {
                this.displayName = displayName;
                this.osVersion = osVersion;
                this.resolution = resolution;
                this.densityDpi = densityDpi;
            }

            public String getDisplayName() { return displayName; }
            public String getOsVersion() { return osVersion; }
            public String getResolution() { return resolution; }
            public int getDensityDpi() { return densityDpi; }
        }
    }

    /**
     * Currently active simulator session
     */
    public static class ActiveSession {
        private String sessionId;
        private String activeAppId;
        private String sessionUrl;          // WebSocket or preview URL
        private LocalDateTime startedAt;
        private LocalDateTime lastHeartbeat;
        private SessionState state = SessionState.ACTIVE;
        private java.util.Map<String, String> metadata = new java.util.HashMap<>();

        public ActiveSession() {}

        public ActiveSession(String sessionId, String activeAppId, String sessionUrl) {
            this.sessionId = sessionId;
            this.activeAppId = activeAppId;
            this.sessionUrl = sessionUrl;
            this.startedAt = LocalDateTime.now();
            this.lastHeartbeat = LocalDateTime.now();
        }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getActiveAppId() { return activeAppId; }
        public void setActiveAppId(String activeAppId) { this.activeAppId = activeAppId; }

        public String getSessionUrl() { return sessionUrl; }
        public void setSessionUrl(String sessionUrl) { this.sessionUrl = sessionUrl; }

        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

        public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
        public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

        public SessionState getState() { return state; }
        public void setState(SessionState state) { this.state = state; }

        public java.util.Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(java.util.Map<String, String> metadata) { this.metadata = metadata; }

        public void refreshHeartbeat() {
            this.lastHeartbeat = LocalDateTime.now();
        }
    }

    /**
     * Quota usage history entry
     */
    public static class QuotaHistoryEntry {
        private LocalDateTime date;
        private int installCount;

        public QuotaHistoryEntry() {}

        public QuotaHistoryEntry(LocalDateTime date, int installCount) {
            this.date = date;
            this.installCount = installCount;
        }

        public static QuotaHistoryEntry of(LocalDateTime date, int count) {
            return new QuotaHistoryEntry(date, count);
        }

        public LocalDateTime getDate() { return date; }
        public void setDate(LocalDateTime date) { this.date = date; }

        public int getInstallCount() { return installCount; }
        public void setInstallCount(int installCount) { this.installCount = installCount; }
    }

    /**
     * Status of an installed application
     */
    public enum AppStatus {
        INSTALLED,    // Successfully installed, not yet running
        RUNNING,      // Currently active in simulator
        ERROR,        // Deployment or runtime error
        EXPIRED       // Removed due to TTL expiry
    }

    /**
     * Session lifecycle states
     */
    public enum SessionState {
        ACTIVE,   // Simulator running
        PAUSED,   // Temporarily suspended
        TERMINATED, // User ended session
        EXPIRED   // Auto-terminated due to timeout
    }
}
