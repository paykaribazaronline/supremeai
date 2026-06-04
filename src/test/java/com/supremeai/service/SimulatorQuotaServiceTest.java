package com.supremeai.service;

import com.supremeai.exception.SimulatorConflictException;
import com.supremeai.exception.SimulatorQuotaExceededException;
import com.supremeai.exception.SimulatorResourceNotFoundException;
import com.supremeai.exception.SimulatorSessionException;
import com.supremeai.model.UserSimulatorProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimulatorQuotaServiceTest {

    private SimulatorQuotaService quotaService;
    private UserSimulatorProfile profile;

    @BeforeEach
    void setUp() {
        quotaService = new SimulatorQuotaService();
        profile = new UserSimulatorProfile("test-user");
        profile.setInstallQuota(2); // Set a small quota for testing
    }

    @Test
    void testHasQuotaRemaining_True() {
        profile.setActiveInstalls(1);
        assertTrue(quotaService.hasQuotaRemaining(profile));
    }

    @Test
    void testHasQuotaRemaining_False() {
        profile.setActiveInstalls(2);
        assertFalse(quotaService.hasQuotaRemaining(profile));
    }

    @Test
    void testValidateCanInstall_Success() {
        profile.setActiveInstalls(1);
        assertDoesNotThrow(() -> quotaService.validateCanInstall(profile, "new-app"));
    }

    @Test
    void testValidateCanInstall_ThrowsQuotaExceeded() {
        profile.setActiveInstalls(2);
        assertThrows(SimulatorQuotaExceededException.class, 
            () -> quotaService.validateCanInstall(profile, "new-app"));
    }

    @Test
    void testValidateCanInstall_ThrowsConflict() {
        profile.setActiveInstalls(1);
        UserSimulatorProfile.InstalledApp app = new UserSimulatorProfile.InstalledApp("existing-app", "Test App", "1.0", "url");
        profile.addInstalledApp(app);
        
        assertThrows(SimulatorConflictException.class, 
            () -> quotaService.validateCanInstall(profile, "existing-app"));
    }

    @Test
    void testValidateCanLaunchSession_Success() {
        UserSimulatorProfile.InstalledApp app = new UserSimulatorProfile.InstalledApp("app-1", "Test App", "1.0", "url");
        profile.addInstalledApp(app);
        
        assertDoesNotThrow(() -> quotaService.validateCanLaunchSession(profile, "app-1"));
    }

    @Test
    void testValidateCanLaunchSession_ThrowsNotFound() {
        assertThrows(SimulatorResourceNotFoundException.class, 
            () -> quotaService.validateCanLaunchSession(profile, "non-existent-app"));
    }

    @Test
    void testValidateCanLaunchSession_ThrowsSessionException() {
        UserSimulatorProfile.InstalledApp app1 = new UserSimulatorProfile.InstalledApp("app-1", "App 1", "1.0", "url");
        UserSimulatorProfile.InstalledApp app2 = new UserSimulatorProfile.InstalledApp("app-2", "App 2", "1.0", "url");
        profile.addInstalledApp(app1);
        profile.addInstalledApp(app2);
        
        profile.setCurrentSession(new UserSimulatorProfile.ActiveSession("session-1", "app-1", "url"));
        
        assertThrows(SimulatorSessionException.class, 
            () -> quotaService.validateCanLaunchSession(profile, "app-2"));
    }
}
