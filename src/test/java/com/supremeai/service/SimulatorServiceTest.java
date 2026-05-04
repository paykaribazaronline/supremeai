package com.supremeai.service;

import com.supremeai.model.UserSimulatorProfile;
import com.supremeai.model.UserSimulatorProfile.InstalledApp;
import com.supremeai.repository.UserSimulatorProfileRepository;
import com.supremeai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SimulatorServiceTest {

    @Mock
    private UserSimulatorProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConfigService configService;

    @Mock
    private QuotaService quotaService;

    @Mock
    private SimulatorDeploymentService deploymentService;

    @InjectMocks
    private SimulatorService simulatorService;

    private UserSimulatorProfile testProfile;

    @BeforeEach
    void setUp() {
        testProfile = new UserSimulatorProfile("user-123");
        testProfile.setInstallQuota(5);
        testProfile.setActiveInstalls(0);
    }

    // ─── getProfile tests ─────────────────────────────────────────────────────

    @Test
    void getProfile_existingUser_returnsProfile() {
        when(profileRepository.findByUserId("user-123")).thenReturn(Mono.just(testProfile));

        StepVerifier.create(simulatorService.getProfile("user-123"))
            .assertNext(profile -> {
                assertEquals("user-123", profile.getUserId());
                assertEquals(5, profile.getInstallQuota());
            })
            .verifyComplete();
    }

    @Test
    void getProfile_newUser_createsAndReturnsProfile() {
        when(profileRepository.findByUserId("new-user")).thenReturn(Mono.empty());
        UserSimulatorProfile newProfile = new UserSimulatorProfile("new-user");
        when(profileRepository.save(any(UserSimulatorProfile.class))).thenReturn(Mono.just(newProfile));

        StepVerifier.create(simulatorService.getProfile("new-user"))
            .assertNext(profile -> assertEquals("new-user", profile.getUserId()))
            .verifyComplete();

        verify(profileRepository).save(any(UserSimulatorProfile.class));
    }

    // ─── installApp tests ─────────────────────────────────────────────────────

    @Test
    void installApp_quotaAvailable_installsSuccessfully() {
        when(profileRepository.findByUserId("user-123")).thenReturn(Mono.just(testProfile));
        when(userRepository.findByFirebaseUid("user-123")).thenReturn(Mono.empty());
        when(quotaService.incrementUsage("user-123")).thenReturn(Mono.just(true));
        when(deploymentService.deployToSimulator("app-abc", "PIXEL_6"))
            .thenReturn("http://localhost:8080/simulator/preview/app-abc?device=pixel_6");

        UserSimulatorProfile savedProfile = new UserSimulatorProfile("user-123");
        savedProfile.setActiveInstalls(1);
        savedProfile.setInstallQuota(5);
        InstalledApp app = new InstalledApp("app-abc", "App app-ab", "1.0.0",
            "http://localhost:8080/simulator/preview/app-abc?device=pixel_6");
        savedProfile.addInstalledApp(app);
        when(profileRepository.save(any(UserSimulatorProfile.class))).thenReturn(Mono.just(savedProfile));

        StepVerifier.create(simulatorService.installApp("user-123", "app-abc", "PIXEL_6"))
            .assertNext(result -> {
                assertNotNull(result);
                assertEquals("app-abc", result.getInstalledApp().getAppId());
                assertNotNull(result.getPreviewUrl());
            })
            .verifyComplete();
    }

    @Test
    void installApp_quotaExceeded_returnsError() {
        when(profileRepository.findByUserId("user-123")).thenReturn(Mono.just(testProfile));
        when(userRepository.findByFirebaseUid("user-123")).thenReturn(Mono.empty());
        when(quotaService.incrementUsage("user-123")).thenReturn(Mono.just(false));

        StepVerifier.create(simulatorService.installApp("user-123", "app-xyz", "PIXEL_6"))
            .expectErrorMessage("Simulator quota exceeded")
            .verify();
    }

    // ─── uninstallApp tests ───────────────────────────────────────────────────

    @Test
    void uninstallApp_appExists_removesSuccessfully() {
        InstalledApp app = new InstalledApp("app-abc", "Test App", "1.0.0", "http://preview/app-abc");
        testProfile.addInstalledApp(app);

        when(profileRepository.findByUserId("user-123")).thenReturn(Mono.just(testProfile));
        when(profileRepository.save(any(UserSimulatorProfile.class))).thenReturn(Mono.just(testProfile));
        doNothing().when(deploymentService).undeployFromSimulator("app-abc");

        StepVerifier.create(simulatorService.uninstallApp("user-123", "app-abc"))
            .verifyComplete();

        verify(deploymentService).undeployFromSimulator("app-abc");
    }

    @Test
    void uninstallApp_profileNotFound_returnsError() {
        when(profileRepository.findByUserId("user-unknown")).thenReturn(Mono.empty());

        StepVerifier.create(simulatorService.uninstallApp("user-unknown", "app-abc"))
            .expectError(com.supremeai.exception.SimulatorResourceNotFoundException.class)
            .verify();
    }

    // ─── startSession tests ───────────────────────────────────────────────────

    @Test
    void startSession_installedApp_startsSession() {
        InstalledApp app = new InstalledApp("app-abc", "Test App", "1.0.0", "http://preview/app-abc");
        testProfile.addInstalledApp(app);

        when(profileRepository.findByUserId("user-123")).thenReturn(Mono.just(testProfile));
        when(profileRepository.save(any(UserSimulatorProfile.class))).thenReturn(Mono.just(testProfile));

        StepVerifier.create(simulatorService.startSession("user-123", "app-abc"))
            .assertNext(result -> {
                assertNotNull(result.getSessionId());
                assertTrue(result.getSessionId().startsWith("sess_"));
                assertNotNull(result.getWebsocketUrl());
                assertEquals("ACTIVE", result.getState());
            })
            .verifyComplete();
    }

    @Test
    void startSession_profileNotFound_returnsError() {
        when(profileRepository.findByUserId("user-unknown")).thenReturn(Mono.empty());

        StepVerifier.create(simulatorService.startSession("user-unknown", "app-abc"))
            .expectError(com.supremeai.exception.SimulatorResourceNotFoundException.class)
            .verify();
    }

    // ─── stopSession tests ────────────────────────────────────────────────────

    @Test
    void stopSession_activeSession_clearsSession() {
        UserSimulatorProfile.ActiveSession session =
            new UserSimulatorProfile.ActiveSession("sess_123", "app-abc", "/ws/simulator/sess_123");
        testProfile.setCurrentSession(session);

        when(profileRepository.findByUserId("user-123")).thenReturn(Mono.just(testProfile));
        when(profileRepository.save(any(UserSimulatorProfile.class))).thenReturn(Mono.just(testProfile));

        StepVerifier.create(simulatorService.stopSession("user-123"))
            .verifyComplete();

        verify(profileRepository).save(argThat(p -> p.getCurrentSession() == null));
    }

    // ─── getSessionStatus tests ───────────────────────────────────────────────

    @Test
    void getSessionStatus_noSession_returnsNone() {
        testProfile.setCurrentSession(null);
        when(profileRepository.findByUserId("user-123")).thenReturn(Mono.just(testProfile));

        StepVerifier.create(simulatorService.getSessionStatus("user-123"))
            .assertNext(status -> assertEquals("NONE", status.getState()))
            .verifyComplete();
    }

    @Test
    void getSessionStatus_activeSession_returnsSessionInfo() {
        UserSimulatorProfile.ActiveSession session =
            new UserSimulatorProfile.ActiveSession("sess_abc", "app-abc", "/ws/simulator/sess_abc");
        session.setState(UserSimulatorProfile.SessionState.ACTIVE);
        testProfile.setCurrentSession(session);

        when(profileRepository.findByUserId("user-123")).thenReturn(Mono.just(testProfile));

        StepVerifier.create(simulatorService.getSessionStatus("user-123"))
            .assertNext(status -> {
                assertEquals("sess_abc", status.getSessionId());
                assertEquals("app-abc", status.getActiveAppId());
                assertEquals("ACTIVE", status.getState());
            })
            .verifyComplete();
    }
}
