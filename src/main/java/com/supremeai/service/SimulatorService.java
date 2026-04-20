package com.supremeai.service;

import com.supremeai.exception.SimulatorConflictException;
import com.supremeai.exception.SimulatorDeploymentException;
import com.supremeai.exception.SimulatorQuotaExceededException;
import com.supremeai.exception.SimulatorResourceNotFoundException;
import com.supremeai.exception.SimulatorSessionException;
import com.supremeai.model.User;
import com.supremeai.model.UserSimulatorProfile;
import com.supremeai.model.UserSimulatorProfile.InstalledApp;
import com.supremeai.repository.UserRepository;
import com.supremeai.repository.UserSimulatorProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * Main service for simulator operations: install, uninstall, session management.
 *
 * This service coordinates:
 * - Quota validation
 * - App deployment to preview environment
 * - Profile persistence (Firestore)
 * - Audit logging
 */
@Service
public class SimulatorService {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorService.class);

    @Autowired
    private UserSimulatorProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConfigService configService;

    @Autowired
    private SimulatorQuotaService quotaService;

    @Autowired
    private SimulatorDeploymentService deploymentService;

    // Audit logging optional - if not present, use simple logger
    // @Autowired(required = false)
    // private AuditLogService auditLogService;

    // TODO: Inject app repository to validate app ownership
    // @Autowired
    // private ProjectRepository projectRepository;

    /**
     * Installs a generated app to user's simulator.
     *
     * Flow:
     * 1. Load or create user profile
     * 2. Validate quota (quotaService)
     * 3. Deploy app to preview environment (deploymentService)
     * 4. Update profile atomically (Firestore transaction)
     * 5. Log audit event
     *
     * @param userId Firebase user ID
     * @param appId ID of generated app to install
     * @param deviceType Device profile (PIXEL_6, IPHONE_15, etc.)
     * @return Result containing installed app and updated profile
     */
    public SimulatorInstallResult installApp(String userId, String appId, String deviceType) {
        logger.info("Installing app {} to simulator for user {}", appId, userId);

        // 1. Load or create profile
        UserSimulatorProfile profile = profileRepository.findByUserId(userId)
            .doOnError(e -> logger.error("Failed to fetch profile for user " + userId, e))
            .onErrorResume(e -> Mono.just(new UserSimulatorProfile(userId)))
            .block();

        if (profile == null) {
            logger.error("Profile could not be loaded/created for user {}", userId);
            throw new IllegalStateException("Failed to load simulator profile");
        }

        // Sync quota from user tier
        User user = userRepository.findByFirebaseUid(userId).block();
        if (user != null) {
            int dynamicQuota = configService.getMaxSimulatorInstallsForTier(user.getTier());
            profile.setInstallQuota(dynamicQuota);
        }

        // 2. Validate quota and duplicates
        quotaService.validateCanInstall(profile, appId);

        // 3. Deploy to preview environment (Cloud Run)
        String previewUrl;
        try {
            previewUrl = deploymentService.deployToSimulator(appId, deviceType);
        } catch (Exception e) {
            logger.error("Deployment failed for app {} user {}", appId, userId, e);
            throw new SimulatorDeploymentException(
                "Failed to deploy app to simulator: " + e.getMessage(), e
            );
        }

        // 4. Create InstalledApp entry
        // TODO: Fetch actual app name/version from ProjectRepository
        String appName = "App " + appId.substring(0, Math.min(6, appId.length()));
        String version = "1.0.0";

        InstalledApp installedApp = new InstalledApp(appId, appName, version, previewUrl);
        installedApp.setStatus(UserSimulatorProfile.AppStatus.INSTALLED);
        profile.addInstalledApp(installedApp);

        // 5. Save updated profile to Firestore
        try {
            profileRepository.save(profile).block();
        } catch (Exception e) {
            logger.error("Failed to save profile for user {}", userId, e);
            throw new RuntimeException("Failed to save simulator profile", e);
        }

        // Audit log - placeholder (AuditLogService not yet implemented)
        logger.info("[AUDIT] APP_INSTALL user={} appId={} previewUrl={}", 
            userId, appId, previewUrl);

        // 7. Record quota history
        quotaService.recordQuotaHistory(profile);

        logger.info("Successfully installed app {} for user {}. Quota: {}/{}",
            appId, userId, profile.getActiveInstalls(), profile.getInstallQuota());

        return new SimulatorInstallResult(
            installedApp,
            profile.getActiveInstalls(),
            profile.getInstallQuota(),
            previewUrl
        );
    }

    /**
     * Uninstalls an app from user's simulator.
     *
     * @param userId Firebase user ID
     * @param appId ID of app to uninstall
     */
    public void uninstallApp(String userId, String appId) {
        logger.info("Uninstalling app {} from simulator for user {}", appId, userId);

        // Load profile
        UserSimulatorProfile profile = profileRepository.findByUserId(userId)
            .switchIfEmpty(Mono.error(
                new SimulatorResourceNotFoundException("Profile not found for user: " + userId)
            ))
            .block();

        if (profile == null) {
            throw new SimulatorResourceNotFoundException("Profile not found");
        }

        // Find app
        InstalledApp app = quotaService.findInstalledApp(profile, appId);
        if (app == null) {
            throw new SimulatorResourceNotFoundException(
                "App not installed: " + appId
            );
        }

        // Check if currently running - if so, terminate session first
        if (profile.getCurrentSession() != null &&
            appId.equals(profile.getCurrentSession().getActiveAppId())) {
            stopSession(userId);
        }

        // Remove from profile
        boolean removed = profile.removeInstalledApp(appId);
        if (!removed) {
            // Should not happen if findInstalledApp succeeded
            throw new SimulatorConflictException("Failed to remove app");
        }

        // Save updated profile
        profileRepository.save(profile).block();

        // Undeploy from Cloud Run (cleanup)
        try {
            deploymentService.undeployFromSimulator(appId);
        } catch (Exception e) {
            logger.warn("Failed to undeploy simulator for app {}: {}", appId, e.getMessage());
            // Non-fatal - continue
        }

        // Audit log
        logger.info("[AUDIT] APP_UNINSTALL user={} appId={}", userId, appId);

        // Record quota history
        quotaService.recordQuotaHistory(profile);

        logger.info("Uninstalled app {} for user {}. Remaining: {}/{}",
            appId, userId, profile.getActiveInstalls(), profile.getInstallQuota());
    }

    /**
     * Gets user's simulator profile with installed apps
     */
    public Mono<UserSimulatorProfile> getProfile(String userId) {
        return profileRepository.findByUserId(userId)
            .switchIfEmpty(Mono.defer(() -> {
                UserSimulatorProfile newProfile = new UserSimulatorProfile(userId);
                return profileRepository.save(newProfile);
            }));
    }

    /**
     * Updates user's simulator profile settings (quota override, device, etc.)
     */
    public Mono<UserSimulatorProfile> updateProfile(String userId, UpdateProfileRequest request) {
        return profileRepository.findByUserId(userId)
            .flatMap(profile -> {
                if (request.getInstallQuota() != null) {
                    profile.setInstallQuota(Math.max(1, Math.min(20, request.getInstallQuota())));
                }
                if (request.getDevice() != null) {
                    UserSimulatorProfile.DeviceProfile device = profile.getDevice();
                    if (device == null) device = new UserSimulatorProfile.DeviceProfile();
                    // Update device fields
                    if (request.getDevice().getType() != null) {
                        try {
                            device.setType(
                                UserSimulatorProfile.DeviceProfile.DeviceType.valueOf(
                                    request.getDevice().getType()
                                )
                            );
                        } catch (IllegalArgumentException e) {
                            logger.warn("Invalid device type: {}, using default", 
                                request.getDevice().getType());
                        }
                    }
                    if (request.getDevice().getOsVersion() != null) {
                        device.setOsVersion(request.getDevice().getOsVersion());
                    }
                    if (request.getDevice().getScreenResolution() != null) {
                        device.setScreenResolution(request.getDevice().getScreenResolution());
                    }
                    if (request.getDevice().getDensityDpi() != null) {
                        device.setDensityDpi(request.getDevice().getDensityDpi());
                    }
                    profile.setDevice(device);
                }
                profile.setLastActiveAt(java.time.LocalDateTime.now());
                return profileRepository.save(profile);
            });
    }

    /**
     * Starts a simulator session for an installed app.
     *
     * @param userId User ID
     * @param appId App ID (must be installed)
     * @return Session info including WebSocket URL
     */
    public SessionStartResult startSession(String userId, String appId) {
        logger.info("Starting simulator session for user {} app {}", userId, appId);

        UserSimulatorProfile profile = profileRepository.findByUserId(userId)
            .switchIfEmpty(Mono.error(
                new SimulatorResourceNotFoundException("Profile not found")
            ))
            .block();

        if (profile == null) {
            throw new SimulatorResourceNotFoundException("Profile not found");
        }

        // Validate can launch
        quotaService.validateCanLaunchSession(profile, appId);

        // Get installed app details
        InstalledApp app = quotaService.findInstalledApp(profile, appId);
        if (app == null) {
            throw new SimulatorResourceNotFoundException(
                "App not installed: " + appId
            );
        }

        // Create session
        String sessionId = "sess_" + UUID.randomUUID().toString().substring(0, 12);
        String websocketUrl = "/ws/simulator/" + sessionId;  // TBD

        UserSimulatorProfile.ActiveSession session = 
            new UserSimulatorProfile.ActiveSession(sessionId, appId, websocketUrl);
        session.setState(UserSimulatorProfile.SessionState.ACTIVE);

        profile.setCurrentSession(session);
        app.recordLaunch();
        profile.setLastActiveAt(java.time.LocalDateTime.now());

        // Save
        profileRepository.save(profile).block();

        // Audit
        logger.info("[AUDIT] SESSION_START userId={} appId={} sessionId={}", 
            userId, appId, sessionId);

        return new SessionStartResult(
            sessionId,
            websocketUrl,
            app.getDeployedUrl(),
            UserSimulatorProfile.SessionState.ACTIVE,
            java.time.LocalDateTime.now()
        );
    }

    /**
     * Stops the current simulator session
     */
    public void stopSession(String userId) {
        logger.info("Stopping simulator session for user {}", userId);

        UserSimulatorProfile profile = profileRepository.findByUserId(userId)
            .block();

        if (profile != null && profile.getCurrentSession() != null) {
            String sessionId = profile.getCurrentSession().getSessionId();
            String appId = profile.getCurrentSession().getActiveAppId();

            profile.setCurrentSession(null);
            profileRepository.save(profile).block();

            // Audit
            logger.info("[AUDIT] SESSION_STOP userId={} sessionId={} appId={}", 
                userId, sessionId, appId);
        }
    }

    /**
     * Gets current session status for a user
     */
    public Mono<SessionStatusResult> getSessionStatus(String userId) {
        return profileRepository.findByUserId(userId)
            .map(profile -> {
                UserSimulatorProfile.ActiveSession session = profile.getCurrentSession();
                if (session == null) {
                    return new SessionStatusResult(null, null, "NONE", null);
                }
                return new SessionStatusResult(
                    session.getSessionId(),
                    session.getActiveAppId(),
                    session.getState().name(),
                    session.getLastHeartbeat()
                );
            });
    }

    // ─── Public DTOs / Results ──────────────────────────────────────────────────

    public static class SimulatorInstallResult {
        private final InstalledApp installedApp;
        private final int activeInstalls;
        private final int installQuota;
        private final String previewUrl;

        public SimulatorInstallResult(InstalledApp app, int active, int quota, String url) {
            this.installedApp = app;
            this.activeInstalls = active;
            this.installQuota = quota;
            this.previewUrl = url;
        }

        public InstalledApp getInstalledApp() { return installedApp; }
        public int getActiveInstalls() { return activeInstalls; }
        public int getInstallQuota() { return installQuota; }
        public String getPreviewUrl() { return previewUrl; }
    }

    public static class SessionStartResult {
        private final String sessionId;
        private final String websocketUrl;
        private final String previewUrl;
        private final String state;
        private final java.time.LocalDateTime startedAt;

        public SessionStartResult(String sessionId, String websocketUrl, String previewUrl,
                                  UserSimulatorProfile.SessionState state,
                                  java.time.LocalDateTime startedAt) {
            this.sessionId = sessionId;
            this.websocketUrl = websocketUrl;
            this.previewUrl = previewUrl;
            this.state = state.name();
            this.startedAt = startedAt;
        }

        public String getSessionId() { return sessionId; }
        public String getWebsocketUrl() { return websocketUrl; }
        public String getPreviewUrl() { return previewUrl; }
        public String getState() { return state; }
        public java.time.LocalDateTime getStartedAt() { return startedAt; }
    }

    public static class SessionStatusResult {
        private final String sessionId;
        private final String activeAppId;
        private final String state;
        private final java.time.LocalDateTime lastHeartbeat;

        public SessionStatusResult(String sessionId, String activeAppId, String state,
                                   java.time.LocalDateTime lastHeartbeat) {
            this.sessionId = sessionId;
            this.activeAppId = activeAppId;
            this.state = state;
            this.lastHeartbeat = lastHeartbeat;
        }

        public String getSessionId() { return sessionId; }
        public String getActiveAppId() { return activeAppId; }
        public String getState() { return state; }
        public java.time.LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    }

    public static class UpdateProfileRequest {
        private Integer installQuota;
        private DeviceUpdateRequest device;

        public Integer getInstallQuota() { return installQuota; }
        public void setInstallQuota(Integer installQuota) { this.installQuota = installQuota; }
        public DeviceUpdateRequest getDevice() { return device; }
        public void setDevice(DeviceUpdateRequest device) { this.device = device; }
    }

    public static class DeviceUpdateRequest {
        private String type; // DeviceType enum name (PIXEL_6, IPHONE_15, etc.)
        private String osVersion;
        private String screenResolution;
        private Integer densityDpi;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
        public String getScreenResolution() { return screenResolution; }
        public void setScreenResolution(String screenResolution) { this.screenResolution = screenResolution; }
        public Integer getDensityDpi() { return densityDpi; }
        public void setDensityDpi(Integer densityDpi) { this.densityDpi = densityDpi; }
    }
}
