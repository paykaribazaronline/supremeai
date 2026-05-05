package com.supremeai.service;

import com.supremeai.exception.SimulatorConflictException;
import com.supremeai.exception.SimulatorDeploymentException;
import com.supremeai.exception.SimulatorResourceNotFoundException;
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

import java.util.UUID;

/**
 * Main service for simulator operations: install, uninstall, session management.
 * Refactored to use UnifiedQuotaService instead of obsolete SimulatorQuotaService.
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
    private QuotaService quotaService;

    @Autowired
    private SimulatorDeploymentService deploymentService;

    public Mono<SimulatorInstallResult> installApp(String userId, String appId, String deviceType) {
        logger.info("Installing app {} to simulator for user {}", appId, userId);

        return profileRepository.findByUserId(userId)
            .doOnError(e -> logger.error("Failed to fetch profile for user " + userId, e))
            .onErrorResume(e -> Mono.just(new UserSimulatorProfile(userId)))
            .flatMap(profile -> userRepository.findByFirebaseUid(userId)
                .map(user -> {
                    int dynamicQuota = configService.getMaxSimulatorInstallsForTier(user.getTier());
                    profile.setInstallQuota(dynamicQuota);
                    return profile;
                })
                .defaultIfEmpty(profile)
                .flatMap(updatedProfile -> quotaService.incrementUsage(userId)
                    .flatMap(incremented -> {
                        if (!incremented) {
                            return Mono.error(new RuntimeException("Simulator quota exceeded"));
                        }

                        String previewUrl;
                        try {
                            previewUrl = deploymentService.deployToSimulator(appId, deviceType);
                        } catch (Exception e) {
                            logger.error("Deployment failed for app {} user {}", appId, userId, e);
                            return Mono.error(new SimulatorDeploymentException("Failed to deploy: " + e.getMessage(), e));
                        }

                        String appName = "App " + appId.substring(0, Math.min(6, appId.length()));
                        String version = "1.0.0";

                        InstalledApp installedApp = new InstalledApp(appId, appName, version, previewUrl);
                        installedApp.setStatus(UserSimulatorProfile.AppStatus.INSTALLED);
                        updatedProfile.addInstalledApp(installedApp);

                        return profileRepository.save(updatedProfile)
                            .map(savedProfile -> new SimulatorInstallResult(
                                installedApp,
                                savedProfile.getActiveInstalls(),
                                savedProfile.getInstallQuota(),
                                previewUrl
                            ));
                    })
                )
            );
    }

    public Mono<Void> uninstallApp(String userId, String appId) {
        return profileRepository.findByUserId(userId)
            .switchIfEmpty(Mono.error(new SimulatorResourceNotFoundException("Profile not found")))
            .flatMap(profile -> {
                if (profile.removeInstalledApp(appId)) {
                    return profileRepository.save(profile)
                        .then(Mono.fromRunnable(() -> {
                            try {
                                deploymentService.undeployFromSimulator(appId);
                            } catch (Exception e) {
                                logger.warn("Undeployment cleanup failed: {}", e.getMessage());
                            }
                        }));
                }
                return Mono.empty();
            });
    }

    public Mono<UserSimulatorProfile> getProfile(String userId) {
        return profileRepository.findByUserId(userId)
            .switchIfEmpty(Mono.defer(() -> {
                UserSimulatorProfile newProfile = new UserSimulatorProfile(userId);
                return profileRepository.save(newProfile);
            }));
    }

    public Mono<UserSimulatorProfile> updateProfile(String userId, UpdateProfileRequest request) {
        return profileRepository.findByUserId(userId)
            .flatMap(profile -> {
                if (request.getInstallQuota() != null) {
                    profile.setInstallQuota(request.getInstallQuota());
                }
                profile.setLastActiveAt(java.time.LocalDateTime.now());
                return profileRepository.save(profile);
            });
    }

    public Mono<SessionStartResult> startSession(String userId, String appId) {
        return profileRepository.findByUserId(userId)
            .switchIfEmpty(Mono.error(new SimulatorResourceNotFoundException("Profile not found")))
            .flatMap(profile -> {
                String sessionId = "sess_" + UUID.randomUUID().toString().substring(0, 12);
                String websocketUrl = "/ws/simulator/" + sessionId;

                UserSimulatorProfile.ActiveSession session = 
                    new UserSimulatorProfile.ActiveSession(sessionId, appId, websocketUrl);
                session.setState(UserSimulatorProfile.SessionState.ACTIVE);

                profile.setCurrentSession(session);
                // Get the actual preview URL for the active app
                String previewUrl = profile.getInstalledApps().stream()
                    .filter(a -> a.getAppId().equals(appId))
                    .map(InstalledApp::getDeployedUrl)
                    .findFirst()
                    .orElse(deploymentService.deployToSimulator(appId, "PIXEL_6"));

                return profileRepository.save(profile)
                    .map(savedProfile -> new SessionStartResult(sessionId, websocketUrl, previewUrl, UserSimulatorProfile.SessionState.ACTIVE, java.time.LocalDateTime.now()));
            });
    }

    public Mono<Void> stopSession(String userId) {
        return profileRepository.findByUserId(userId)
            .flatMap(profile -> {
                profile.setCurrentSession(null);
                return profileRepository.save(profile);
            })
            .then();
    }

    /**
     * Get all active simulator deployments for admin view.
     */
    public java.util.Map<String, SimulatorDeploymentService.DeploymentRecord> getAllDeployments() {
        return deploymentService.getAllDeployments();
    }

    public Mono<SessionStatusResult> getSessionStatus(String userId) {        return profileRepository.findByUserId(userId)
            .map(profile -> {
                UserSimulatorProfile.ActiveSession session = profile.getCurrentSession();
                return new SessionStatusResult(
                    session != null ? session.getSessionId() : null,
                    session != null ? session.getActiveAppId() : null,
                    session != null ? session.getState().name() : "NONE",
                    session != null ? session.getLastHeartbeat() : null
                );
            });
    }

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
        private String type;
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
