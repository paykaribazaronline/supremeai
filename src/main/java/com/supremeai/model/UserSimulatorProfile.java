package com.supremeai.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

/**
 * UserSimulatorProfile implements the requirements of Plan 22.
 * It manages quotas for installed applications and tracks active simulator sessions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collectionName = "simulator_profiles")
public class UserSimulatorProfile {

    @DocumentId
    private String userId;

    @Builder.Default
    private int installQuota = 5; // Default limit as per Plan 22

    @Builder.Default
    private int activeInstalls = 0;

    @Builder.Default
    private List<InstalledApp> installedApps = new ArrayList<>();

    private ActiveSession currentSession;

    private LocalDateTime lastActiveAt;

    private String activeSessionId;

    @Builder.Default
    private Map<String, Object> deviceConfig = new HashMap<>(); // Store Pixel 6, iPhone 15, etc.

    private long lastUsedTimestamp;

    @Builder.Default
    private Map<String, Long> appExpiryMap = new HashMap<>(); // For the 7-day auto-cleanup policy

    private LocalDateTime lastActiveTimestamp;

    @Builder.Default
    private UserTier userTier = UserTier.BASIC;

    public UserSimulatorProfile(String userId) {
        this.userId = userId;
        this.installQuota = 5;
        this.activeInstalls = 0;
        this.installedApps = new ArrayList<>();
        this.deviceConfig = new HashMap<>();
        this.appExpiryMap = new HashMap<>();
        this.userTier = UserTier.BASIC;
    }

    /**
     * Checks if the user has room for a new installation.
     */
    public boolean canInstall() {
        return activeInstalls < installQuota;
    }

    /**
     * Adds an installed app to the profile.
     */
    public void addInstalledApp(InstalledApp app) {
        if (this.installedApps == null) {
            this.installedApps = new ArrayList<>();
        }
        this.installedApps.add(app);
        this.activeInstalls = this.installedApps.size();
    }

    /**
     * Removes an installed app by ID.
     */
    public boolean removeInstalledApp(String appId) {
        if (this.installedApps == null) {
            return false;
        }
        boolean removed = this.installedApps.removeIf(app -> app.getAppId().equals(appId));
        this.activeInstalls = this.installedApps.size();
        return removed;
    }

    /**
     * Checks if a specific app is installed in the profile.
     */
    public boolean hasAppInstalled(String appId) {
        if (this.installedApps == null) {
            return false;
        }
        return this.installedApps.stream().anyMatch(app -> app.getAppId().equals(appId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner Models
    // ─────────────────────────────────────────────────────────────────────────

    public enum AppStatus {
        INSTALLED, RUNNING, ERROR
    }

    public enum SessionState {
        ACTIVE, PAUSED, TERMINATED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstalledApp {
        private String appId;
        private String appName;
        private String version;
        private String deployedUrl;
        private LocalDateTime installedAt = LocalDateTime.now();
        private AppStatus status = AppStatus.INSTALLED;
        private int launchCount = 0;
        private LocalDateTime lastLaunchedAt;

        public InstalledApp(String appId, String appName, String version, String deployedUrl) {
            this.appId = appId;
            this.appName = appName;
            this.version = version;
            this.deployedUrl = deployedUrl;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveSession {
        private String sessionId;
        private String activeAppId;
        private String sessionUrl;
        private LocalDateTime startedAt = LocalDateTime.now();
        private LocalDateTime lastHeartbeat = LocalDateTime.now();
        private SessionState state = SessionState.ACTIVE;

        public ActiveSession(String sessionId, String activeAppId, String sessionUrl) {
            this.sessionId = sessionId;
            this.activeAppId = activeAppId;
            this.sessionUrl = sessionUrl;
        }
    }

    public static class DeviceProfile {
        public enum DeviceType {
            PIXEL_6("Pixel 6 (Android)", "Android 14", "1080x2340", 440),
            IPHONE_15("iPhone 15 (iOS)", "iOS 17", "1179x2556", 460),
            GALAXY_S23("Galaxy S23 (Android)", "Android 13", "1080x2340", 425);

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
}