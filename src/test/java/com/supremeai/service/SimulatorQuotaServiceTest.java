package com.supremeai.service;

import com.supremeai.exception.SimulatorConflictException;
import com.supremeai.exception.SimulatorQuotaExceededException;
import com.supremeai.exception.SimulatorResourceNotFoundException;
import com.supremeai.exception.SimulatorSessionException;
import com.supremeai.model.UserSimulatorProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimulatorQuotaService.
 *
 * Covers:
 * - Quota validation (pass/fail)
 * - Duplicate detection
 * - Remaining slots calculation
 * - Session launch validation
 */
@ExtendWith(MockitoExtension.class)
class SimulatorQuotaServiceTest {

    private SimulatorQuotaService quotaService;
    private UserSimulatorProfile profile;

    @BeforeEach
    void setUp() {
        quotaService = new SimulatorQuotaService();
        profile = new UserSimulatorProfile("test-user-123");
        profile.setInstallQuota(5);
        profile.setActiveInstalls(0);
    }

    @Test
    void testValidateCanInstall_WithinQuota_Passes() {
        // Act & Assert - no exception
        quotaService.validateCanInstall(profile, "app-001");
    }

    @Test
    void testValidateCanInstall_AtQuota_Throws() {
        // Arrange: user has 5/5 installed
        profile.setActiveInstalls(5);
        
        // Act & Assert
        SimulatorQuotaExceededException ex = assertThrows(
            SimulatorQuotaExceededException.class,
            () -> quotaService.validateCanInstall(profile, "app-001")
        );
        assertEquals(5, ex.getUsed());
        assertEquals(5, ex.getLimit());
    }

    @Test
    void testValidateCanInstall_DuplicateApp_Throws() {
        // Arrange: app already installed
        UserSimulatorProfile.InstalledApp existing = 
            new UserSimulatorProfile.InstalledApp("app-001", "TestApp", "1.0", "http://url");
        profile.addInstalledApp(existing);
        
        // Act & Assert
        assertThrows(SimulatorConflictException.class,
            () -> quotaService.validateCanInstall(profile, "app-001")
        );
    }

    @Test
    void testHasQuotaRemaining_WhenUnderLimit_ReturnsTrue() {
        profile.setActiveInstalls(3);
        assertTrue(quotaService.hasQuotaRemaining(profile));
    }

    @Test
    void testHasQuotaRemaining_AtLimit_ReturnsFalse() {
        profile.setActiveInstalls(5);
        assertFalse(quotaService.hasQuotaRemaining(profile));
    }

    @Test
    void testGetRemainingSlots_CalculatesCorrectly() {
        profile.setActiveInstalls(2);
        assertEquals(3, quotaService.getRemainingSlots(profile));
        
        profile.setActiveInstalls(5);
        assertEquals(0, quotaService.getRemainingSlots(profile));
    }

    @Test
    void testValidateCanLaunchSession_WhenNoSession_Allows() {
        // No session active
        UserSimulatorProfile.InstalledApp app = 
            new UserSimulatorProfile.InstalledApp("app-001", "TestApp", "1.0", "http://url");
        profile.addInstalledApp(app);
        
        // Should not throw
        quotaService.validateCanLaunchSession(profile, "app-001");
    }

    @Test
    void testValidateCanLaunchSession_WhenSameAppAlreadyRunning_Idempotent() {
        // App is already running - should be idempotent (no-op)
        UserSimulatorProfile.InstalledApp app = 
            new UserSimulatorProfile.InstalledApp("app-001", "TestApp", "1.0", "http://url");
        profile.addInstalledApp(app);
        
        UserSimulatorProfile.ActiveSession session = 
            new UserSimulatorProfile.ActiveSession("sess-123", "app-001", "ws://");
        profile.setCurrentSession(session);
        
        // Should not throw - same app is already running
        quotaService.validateCanLaunchSession(profile, "app-001");
    }

    @Test
    void testValidateCanLaunchSession_WhenDifferentAppRunning_Throws() {
        // App A running, try to launch App B
        UserSimulatorProfile.InstalledApp appA = 
            new UserSimulatorProfile.InstalledApp("app-A", "AppA", "1.0", "http://a");
        UserSimulatorProfile.InstalledApp appB = 
            new UserSimulatorProfile.InstalledApp("app-B", "AppB", "1.0", "http://b");
        profile.addInstalledApp(appA);
        profile.addInstalledApp(appB);
        
        UserSimulatorProfile.ActiveSession session = 
            new UserSimulatorProfile.ActiveSession("sess-123", "app-A", "ws://");
        profile.setCurrentSession(session);
        
        // Should throw because another app is running
        SimulatorSessionException ex = assertThrows(
            SimulatorSessionException.class,
            () -> quotaService.validateCanLaunchSession(profile, "app-B")
        );
        assertTrue(ex.getMessage().contains("Another app is currently running"));
    }

    @Test
    void testValidateCanLaunchSession_WhenAppNotInstalled_Throws() {
        // Try to launch app that's not installed
        SimulatorResourceNotFoundException ex = assertThrows(
            SimulatorResourceNotFoundException.class,
            () -> quotaService.validateCanLaunchSession(profile, "non-existent")
        );
        assertTrue(ex.getMessage().contains("App not installed"));
    }
}
