package com.supremeai.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.repository.UserRepository;
import com.supremeai.security.BruteForceProtectionService;
import com.supremeai.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private BruteForceProtectionService bruteForceProtectionService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ConfigService configService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFirebaseLogin_Success() {
        // Since FirebaseAuth.getInstance() is static, we'd need mockito-inline to mock it.
        // For now, let's assume we can't easily mock static Firebase in this environment 
        // without adding dependencies, but we can structure the test for logic if we mock the static call.
        
        try (MockedStatic<FirebaseAuth> firebaseAuthMock = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth authInstance = mock(FirebaseAuth.class);
            firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(authInstance);
            
            FirebaseToken token = mock(FirebaseToken.class);
            when(token.getUid()).thenReturn("test-uid");
            when(token.getEmail()).thenReturn("test@example.com");
            when(token.getClaims()).thenReturn(Map.of());
            
            try {
                when(authInstance.verifyIdToken(anyString())).thenReturn(token);
            } catch (Exception e) {}

            User user = new User("test-uid", "test@example.com", "test");
            user.setTier(UserTier.FREE);
            
            when(userRepository.findByFirebaseUid("test-uid")).thenReturn(Mono.just(user));
            when(userRepository.save(any())).thenReturn(Mono.just(user));
            when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("access-token");
            when(jwtUtil.generateRefreshToken(anyString(), anyString())).thenReturn("refresh-token");
            when(configService.getConfig()).thenReturn(null);

            Mono<Map<String, Object>> result = authenticationService.firebaseLogin("valid-token", "127.0.0.1");

            StepVerifier.create(result)
                .expectNextMatches(res -> res.get("token").equals("access-token") && res.get("status").equals("success"))
                .verifyComplete();
        }
    }

    @Test
    void testFirebaseLogin_InvalidToken() {
        Mono<Map<String, Object>> result = authenticationService.firebaseLogin("", "127.0.0.1");

        StepVerifier.create(result)
            .expectError(IllegalArgumentException.class)
            .verify();
    }
}
