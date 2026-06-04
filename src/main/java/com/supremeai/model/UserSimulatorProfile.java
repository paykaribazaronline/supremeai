package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserSimulatorProfile implements the requirements of Plan 22. It manages quotas for installed
 * applications and tracks active simulator sessions.
 */
@Document(collectionName = "simulator_profiles")
public class UserSimulatorProfile {

  @DocumentId private String userId;

  private int installQuota = 5; // Default limit as per Plan 22

  private int activeInstalls = 0;

  private List<InstalledApp> installedApps = new ArrayList<>();

  private ActiveSession currentSession;

  private LocalDateTime lastActiveAt;

  private String activeSessionId;

  private Map<String, Object> deviceConfig = new HashMap<>(); // Store Pixel 6, iPhone 15, etc.

  private long lastUsedTimestamp;

  private Map<String, Long> appExpiryMap = new HashMap<>(); // For the 7-day auto-cleanup policy

  private LocalDateTime lastActiveTimestamp;

  private UserTier userTier = UserTier.BASIC;

  // Constructors
  public UserSimulatorProfile() {}

  public UserSimulatorProfile(
      String userId,
      int installQuota,
      int activeInstalls,
      List<InstalledApp> installedApps,
      ActiveSession currentSession,
      LocalDateTime lastActiveAt,
      String activeSessionId,
      Map<String, Object> deviceConfig,
      long lastUsedTimestamp,
      Map<String, Long> appExpiryMap,
      LocalDateTime lastActiveTimestamp,
      UserTier userTier) {
    this.userId = userId;
    this.installQuota = installQuota;
    this.activeInstalls = activeInstalls;
    this.installedApps = installedApps != null ? installedApps : new ArrayList<>();
    this.currentSession = currentSession;
    this.lastActiveAt = lastActiveAt;
    this.activeSessionId = activeSessionId;
    this.deviceConfig = deviceConfig != null ? deviceConfig : new HashMap<>();
    this.lastUsedTimestamp = lastUsedTimestamp;
    this.appExpiryMap = appExpiryMap != null ? appExpiryMap : new HashMap<>();
    this.lastActiveTimestamp = lastActiveTimestamp;
    this.userTier = userTier != null ? userTier : UserTier.BASIC;
  }

  public UserSimulatorProfile(String userId) {
    this.userId = userId;
    this.installQuota = 5;
    this.activeInstalls = 0;
    this.installedApps = new ArrayList<>();
    this.deviceConfig = new HashMap<>();
    this.appExpiryMap = new HashMap<>();
    this.userTier = UserTier.BASIC;
  }

  // Getters and Setters
  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public int getInstallQuota() {
    return installQuota;
  }

  public void setInstallQuota(int installQuota) {
    this.installQuota = installQuota;
  }

  public int getActiveInstalls() {
    return activeInstalls;
  }

  public void setActiveInstalls(int activeInstalls) {
    this.activeInstalls = activeInstalls;
  }

  public List<InstalledApp> getInstalledApps() {
    return installedApps;
  }

  public void setInstalledApps(List<InstalledApp> installedApps) {
    this.installedApps = installedApps != null ? installedApps : new ArrayList<>();
    this.activeInstalls = this.installedApps.size();
  }

  public ActiveSession getCurrentSession() {
    return currentSession;
  }

  public void setCurrentSession(ActiveSession currentSession) {
    this.currentSession = currentSession;
  }

  public LocalDateTime getLastActiveAt() {
    return lastActiveAt;
  }

  public void setLastActiveAt(LocalDateTime lastActiveAt) {
    this.lastActiveAt = lastActiveAt;
  }

  public String getActiveSessionId() {
    return activeSessionId;
  }

  public void setActiveSessionId(String activeSessionId) {
    this.activeSessionId = activeSessionId;
  }

  public Map<String, Object> getDeviceConfig() {
    return deviceConfig;
  }

  public void setDeviceConfig(Map<String, Object> deviceConfig) {
    this.deviceConfig = deviceConfig != null ? deviceConfig : new HashMap<>();
  }

  public long getLastUsedTimestamp() {
    return lastUsedTimestamp;
  }

  public void setLastUsedTimestamp(long lastUsedTimestamp) {
    this.lastUsedTimestamp = lastUsedTimestamp;
  }

  public Map<String, Long> getAppExpiryMap() {
    return appExpiryMap;
  }

  public void setAppExpiryMap(Map<String, Long> appExpiryMap) {
    this.appExpiryMap = appExpiryMap != null ? appExpiryMap : new HashMap<>();
  }

  public LocalDateTime getLastActiveTimestamp() {
    return lastActiveTimestamp;
  }

  public void setLastActiveTimestamp(LocalDateTime lastActiveTimestamp) {
    this.lastActiveTimestamp = lastActiveTimestamp;
  }

  public UserTier getUserTier() {
    return userTier;
  }

  public void setUserTier(UserTier userTier) {
    this.userTier = userTier != null ? userTier : UserTier.BASIC;
  }

  /** Checks if the user has room for a new installation. */
  public boolean canInstall() {
    return activeInstalls < installQuota;
  }

  /** Adds an installed app to the profile. */
  public void addInstalledApp(InstalledApp app) {
    if (this.installedApps == null) {
      this.installedApps = new ArrayList<>();
    }
    this.installedApps.add(app);
    this.activeInstalls = this.installedApps.size();
  }

  /** Removes an installed app by ID. */
  public boolean removeInstalledApp(String appId) {
    if (this.installedApps == null) {
      return false;
    }
    boolean removed = this.installedApps.removeIf(app -> app.getAppId().equals(appId));
    this.activeInstalls = this.installedApps.size();
    return removed;
  }

  /** Checks if a specific app is installed in the profile. */
  public boolean hasAppInstalled(String appId) {
    if (this.installedApps == null) {
      return false;
    }
    return this.installedApps.stream().anyMatch(app -> app.getAppId().equals(appId));
  }

  // Builder
  public static UserSimulatorProfileBuilder builder() {
    return new UserSimulatorProfileBuilder();
  }

  public static class UserSimulatorProfileBuilder {
    private String userId;
    private int installQuota = 5;
    private int activeInstalls = 0;
    private List<InstalledApp> installedApps = new ArrayList<>();
    private ActiveSession currentSession;
    private LocalDateTime lastActiveAt;
    private String activeSessionId;
    private Map<String, Object> deviceConfig = new HashMap<>();
    private long lastUsedTimestamp;
    private Map<String, Long> appExpiryMap = new HashMap<>();
    private LocalDateTime lastActiveTimestamp;
    private UserTier userTier = UserTier.BASIC;

    public UserSimulatorProfileBuilder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public UserSimulatorProfileBuilder installQuota(int installQuota) {
      this.installQuota = installQuota;
      return this;
    }

    public UserSimulatorProfileBuilder activeInstalls(int activeInstalls) {
      this.activeInstalls = activeInstalls;
      return this;
    }

    public UserSimulatorProfileBuilder installedApps(List<InstalledApp> installedApps) {
      this.installedApps = installedApps;
      return this;
    }

    public UserSimulatorProfileBuilder currentSession(ActiveSession currentSession) {
      this.currentSession = currentSession;
      return this;
    }

    public UserSimulatorProfileBuilder lastActiveAt(LocalDateTime lastActiveAt) {
      this.lastActiveAt = lastActiveAt;
      return this;
    }

    public UserSimulatorProfileBuilder activeSessionId(String activeSessionId) {
      this.activeSessionId = activeSessionId;
      return this;
    }

    public UserSimulatorProfileBuilder deviceConfig(Map<String, Object> deviceConfig) {
      this.deviceConfig = deviceConfig;
      return this;
    }

    public UserSimulatorProfileBuilder lastUsedTimestamp(long lastUsedTimestamp) {
      this.lastUsedTimestamp = lastUsedTimestamp;
      return this;
    }

    public UserSimulatorProfileBuilder appExpiryMap(Map<String, Long> appExpiryMap) {
      this.appExpiryMap = appExpiryMap;
      return this;
    }

    public UserSimulatorProfileBuilder lastActiveTimestamp(LocalDateTime lastActiveTimestamp) {
      this.lastActiveTimestamp = lastActiveTimestamp;
      return this;
    }

    public UserSimulatorProfileBuilder userTier(UserTier userTier) {
      this.userTier = userTier;
      return this;
    }

    public UserSimulatorProfile build() {
      return new UserSimulatorProfile(
          userId,
          installQuota,
          activeInstalls,
          installedApps,
          currentSession,
          lastActiveAt,
          activeSessionId,
          deviceConfig,
          lastUsedTimestamp,
          appExpiryMap,
          lastActiveTimestamp,
          userTier);
    }
  }

  // Inner Models
  public enum AppStatus {
    INSTALLED,
    RUNNING,
    ERROR
  }

  public enum SessionState {
    ACTIVE,
    PAUSED,
    TERMINATED
  }

  public static class InstalledApp {
    private String appId;
    private String appName;
    private String version;
    private String deployedUrl;
    private LocalDateTime installedAt = LocalDateTime.now();
    private AppStatus status = AppStatus.INSTALLED;
    private int launchCount = 0;
    private LocalDateTime lastLaunchedAt;

    public InstalledApp() {}

    public InstalledApp(String appId, String appName, String version, String deployedUrl) {
      this.appId = appId;
      this.appName = appName;
      this.version = version;
      this.deployedUrl = deployedUrl;
      this.installedAt = LocalDateTime.now();
      this.status = AppStatus.INSTALLED;
      this.launchCount = 0;
    }

    public InstalledApp(
        String appId,
        String appName,
        String version,
        String deployedUrl,
        LocalDateTime installedAt,
        AppStatus status,
        int launchCount,
        LocalDateTime lastLaunchedAt) {
      this.appId = appId;
      this.appName = appName;
      this.version = version;
      this.deployedUrl = deployedUrl;
      this.installedAt = installedAt != null ? installedAt : LocalDateTime.now();
      this.status = status != null ? status : AppStatus.INSTALLED;
      this.launchCount = launchCount;
      this.lastLaunchedAt = lastLaunchedAt;
    }

    // Getters and Setters
    public String getAppId() {
      return appId;
    }

    public void setAppId(String appId) {
      this.appId = appId;
    }

    public String getAppName() {
      return appName;
    }

    public void setAppName(String appName) {
      this.appName = appName;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public String getDeployedUrl() {
      return deployedUrl;
    }

    public void setDeployedUrl(String deployedUrl) {
      this.deployedUrl = deployedUrl;
    }

    public LocalDateTime getInstalledAt() {
      return installedAt;
    }

    public void setInstalledAt(LocalDateTime installedAt) {
      this.installedAt = installedAt;
    }

    public AppStatus getStatus() {
      return status;
    }

    public void setStatus(AppStatus status) {
      this.status = status;
    }

    public int getLaunchCount() {
      return launchCount;
    }

    public void setLaunchCount(int launchCount) {
      this.launchCount = launchCount;
    }

    public LocalDateTime getLastLaunchedAt() {
      return lastLaunchedAt;
    }

    public void setLastLaunchedAt(LocalDateTime lastLaunchedAt) {
      this.lastLaunchedAt = lastLaunchedAt;
    }
  }

  public static class ActiveSession {
    private String sessionId;
    private String activeAppId;
    private String sessionUrl;
    private LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime lastHeartbeat = LocalDateTime.now();
    private SessionState state = SessionState.ACTIVE;

    public ActiveSession() {}

    public ActiveSession(String sessionId, String activeAppId, String sessionUrl) {
      this.sessionId = sessionId;
      this.activeAppId = activeAppId;
      this.sessionUrl = sessionUrl;
      this.startedAt = LocalDateTime.now();
      this.lastHeartbeat = LocalDateTime.now();
      this.state = SessionState.ACTIVE;
    }

    public ActiveSession(
        String sessionId,
        String activeAppId,
        String sessionUrl,
        LocalDateTime startedAt,
        LocalDateTime lastHeartbeat,
        SessionState state) {
      this.sessionId = sessionId;
      this.activeAppId = activeAppId;
      this.sessionUrl = sessionUrl;
      this.startedAt = startedAt != null ? startedAt : LocalDateTime.now();
      this.lastHeartbeat = lastHeartbeat != null ? lastHeartbeat : LocalDateTime.now();
      this.state = state != null ? state : SessionState.ACTIVE;
    }

    // Getters and Setters
    public String getSessionId() {
      return sessionId;
    }

    public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
    }

    public String getActiveAppId() {
      return activeAppId;
    }

    public void setActiveAppId(String activeAppId) {
      this.activeAppId = activeAppId;
    }

    public String getSessionUrl() {
      return sessionUrl;
    }

    public void setSessionUrl(String sessionUrl) {
      this.sessionUrl = sessionUrl;
    }

    public LocalDateTime getStartedAt() {
      return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
      this.startedAt = startedAt;
    }

    public LocalDateTime getLastHeartbeat() {
      return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
      this.lastHeartbeat = lastHeartbeat;
    }

    public SessionState getState() {
      return state;
    }

    public void setState(SessionState state) {
      this.state = state;
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

      public String getDisplayName() {
        return displayName;
      }

      public String getOsVersion() {
        return osVersion;
      }

      public String getResolution() {
        return resolution;
      }

      public int getDensityDpi() {
        return densityDpi;
      }
    }
  }
}
