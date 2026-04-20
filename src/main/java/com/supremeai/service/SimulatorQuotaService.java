package com.supremeai.service;

import com.supremeai.exception.SimulatorConflictException;
import com.supremeai.exception.SimulatorQuotaExceededException;
import com.supremeai.exception.SimulatorResourceNotFoundException;
import com.supremeai.model.UserSimulatorProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for quota validation and management in simulator
 */
@Service
public class SimulatorQuotaService {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorQuotaService.class);

    /**
     * Validates whether a user can install an app based on quota and duplicates.
     *
     * @param profile User's simulator profile
     * @param appId ID of the app to install
     * @throws SimulatorQuotaExceededException if quota limit reached
     * @throws SimulatorConflictException if app already installed
     */
    public void validateCanInstall(UserSimulatorProfile profile, String appId) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }
        if (appId == null || appId.trim().isEmpty()) {
            throw new IllegalArgumentException("appId cannot be null or empty");
        }

        int used = profile.getActiveInstalls();
        int quota = profile.getInstallQuota();

        // Check quota
        if (used >= quota) {
            logger.warn("User {} exceeded simulator quota: {}/{}", 
                profile.getUserId(), used, quota);
            throw new SimulatorQuotaExceededException(used, quota);
        }

        // Check duplicate
        if (profile.hasAppInstalled(appId)) {
            logger.info("User {} attempted to reinstall already-installed app {}", 
                profile.getUserId(), appId);
            throw new SimulatorConflictException(
                "App is already installed in your simulator"
            );
        }

        logger.debug("Quota validation passed for user {} ({} / {})", 
            profile.getUserId(), used, quota);
    }

    /**
     * Checks if user has remaining quota slots (non-throwing)
     */
    public boolean hasQuotaRemaining(UserSimulatorProfile profile) {
        return profile != null && profile.getActiveInstalls() < profile.getInstallQuota();
    }

    /**
     * Returns number of remaining install slots
     */
    public int getRemainingSlots(UserSimulatorProfile profile) {
        if (profile == null) return 0;
        return Math.max(0, profile.getInstallQuota() - profile.getActiveInstalls());
    }

    /**
     * Checks if a specific app is already installed
     */
    public boolean isAppInstalled(UserSimulatorProfile profile, String appId) {
        if (profile == null || profile.getInstalledApps() == null) return false;
        return profile.getInstalledApps().stream()
            .anyMatch(app -> app.getAppId().equals(appId));
    }

    /**
     * Gets a specific installed app entry
     */
    public UserSimulatorProfile.InstalledApp findInstalledApp(
            UserSimulatorProfile profile, String appId) {
        if (profile == null || profile.getInstalledApps() == null) return null;
        return profile.getInstalledApps().stream()
            .filter(app -> app.getAppId().equals(appId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Validates if user can launch a session (at most 1 concurrent session)
     */
    public void validateCanLaunchSession(UserSimulatorProfile profile, String appId) {
        if (profile.getCurrentSession() != null) {
            // Check if same app already running
            String runningAppId = profile.getCurrentSession().getActiveAppId();
            if (runningAppId != null && runningAppId.equals(appId)) {
                logger.debug("App {} already running for user {}", appId, profile.getUserId());
                return; // Already running - OK (idempotent launch)
            }
            // Different app already running - stop it first
            throw new SimulatorSessionException(
                "Another app is currently running. Stop it before launching a different app."
            );
        }

        // Verify app is actually installed
        if (!profile.hasAppInstalled(appId)) {
            throw new SimulatorResourceNotFoundException(
                "App not installed: " + appId
            );
        }
    }

    /**
     * Records quota history entry
     */
    public void recordQuotaHistory(UserSimulatorProfile profile) {
        List<UserSimulatorProfile.QuotaHistoryEntry> history = profile.getQuotaHistory();
        if (history == null) {
            history = new java.util.ArrayList<>();
            profile.setQuotaHistory(history);
        }
        history.add(new UserSimulatorProfile.QuotaHistoryEntry(
            java.time.LocalDateTime.now(),
            profile.getActiveInstalls()
        ));

        // Keep only last 30 entries (30 days if daily)
        if (history.size() > 30) {
            history.remove(0);
        }
    }
}
