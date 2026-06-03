package com.supremeai.service;

import com.supremeai.exception.SimulatorConflictException;
import com.supremeai.exception.SimulatorQuotaExceededException;
import com.supremeai.exception.SimulatorResourceNotFoundException;
import com.supremeai.exception.SimulatorSessionException;
import com.supremeai.model.UserSimulatorProfile;
import org.springframework.stereotype.Service;

/**
 * Service for validating simulator quota and session rules.
 */
@Service
public class SimulatorQuotaService {

    /**
     * Validates that the user can install a new app.
     * Throws if quota exceeded or app already installed.
     */
    public void validateCanInstall(UserSimulatorProfile profile, String appId) {
        if (!hasQuotaRemaining(profile)) {
            throw new SimulatorQuotaExceededException(profile.getActiveInstalls(), profile.getInstallQuota());
        }
        if (profile.hasAppInstalled(appId)) {
            throw new SimulatorConflictException("App already installed: " + appId);
        }
    }

    /**
     * Checks if the user has remaining install quota.
     */
    public boolean hasQuotaRemaining(UserSimulatorProfile profile) {
        return profile.getActiveInstalls() < profile.getInstallQuota();
    }

    /**
     * Returns remaining install slots.
     */
    public int getRemainingSlots(UserSimulatorProfile profile) {
        return Math.max(0, profile.getInstallQuota() - profile.getActiveInstalls());
    }

    /**
     * Validates that a session can be launched for the given app.
     * Throws if app not installed or another app is already running.
     */
    public void validateCanLaunchSession(UserSimulatorProfile profile, String appId) {
        if (!profile.hasAppInstalled(appId)) {
            throw new SimulatorResourceNotFoundException("App not installed: " + appId);
        }

        UserSimulatorProfile.ActiveSession current = profile.getCurrentSession();
        if (current != null && !current.getActiveAppId().equals(appId)) {
            throw new SimulatorSessionException(
                "Another app is currently running: " + current.getActiveAppId()
            );
        }
        // If same app is running, it's idempotent — do nothing
    }
}
