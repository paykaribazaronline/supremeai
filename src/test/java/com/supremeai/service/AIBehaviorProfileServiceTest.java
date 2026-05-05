package com.supremeai.service;

import com.supremeai.model.AIBehaviorProfile;
import com.supremeai.repository.AIBehaviorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

class AIBehaviorProfileServiceTest {
    private AIBehaviorProfileService profileService;
    private AIBehaviorProfileRepository mockRepository;

    @BeforeEach
    void setUp() {
        mockRepository = Mockito.mock(AIBehaviorProfileRepository.class);
        profileService = new AIBehaviorProfileService();
        try {
            java.lang.reflect.Field field = AIBehaviorProfileService.class.getDeclaredField("profileRepository");
            field.setAccessible(true);
            field.set(profileService, mockRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetDefaultProfile() {
        Mono<AIBehaviorProfile> defaultMono = profileService.getDefaultProfile();
        StepVerifier.create(defaultMono)
                .assertNext(profile -> {
                    assertEquals("default", profile.getProjectId());
                    assertEquals(AIBehaviorProfile.SecurityStrictness.MEDIUM, profile.getSecurityStrictness());
                    assertEquals(AIBehaviorProfile.PerformanceTradeoff.BALANCED, profile.getPerformanceTradeoff());
                })
                .verifyComplete();
    }

    @Test
    void testGetProfileForProjectWhenExists() {
        AIBehaviorProfile mockProfile = new AIBehaviorProfile();
        mockProfile.setId("test-id");
        mockProfile.setProjectId("project-123");
        mockProfile.setSecurityStrictness(AIBehaviorProfile.SecurityStrictness.HIGH);
        when(mockRepository.findFirstByProjectId("project-123")).thenReturn(Mono.just(mockProfile));

        Mono<AIBehaviorProfile> profileMono = profileService.getProfileForProject("project-123");
        StepVerifier.create(profileMono)
                .assertNext(p -> {
                    assertEquals("project-123", p.getProjectId());
                    assertEquals(AIBehaviorProfile.SecurityStrictness.HIGH, p.getSecurityStrictness());
                })
                .verifyComplete();
    }

    @Test
    void testGetProfileForProjectWhenNotExists() {
        when(mockRepository.findFirstByProjectId("nonexistent")).thenReturn(Mono.empty());
        Mono<AIBehaviorProfile> profileMono = profileService.getProfileForProject("nonexistent");
        StepVerifier.create(profileMono)
                .assertNext(profile -> assertEquals("default", profile.getProjectId()))
                .verifyComplete();
    }

    @Test
    void testSaveProfile() {
        AIBehaviorProfile profile = new AIBehaviorProfile();
        profile.setProjectId("project-456");
        profile.setSecurityStrictness(AIBehaviorProfile.SecurityStrictness.LOW);
        when(mockRepository.save(any(AIBehaviorProfile.class))).thenReturn(Mono.just(profile));

        Mono<AIBehaviorProfile> savedMono = profileService.saveProfile(profile);
        StepVerifier.create(savedMono)
                .assertNext(saved -> assertEquals("project-456", saved.getProjectId()))
                .verifyComplete();
    }

    @Test
    void testApplyProfileToContext() {
        AIBehaviorProfile profile = new AIBehaviorProfile();
        profile.setSecurityStrictness(AIBehaviorProfile.SecurityStrictness.HIGH);
        profile.setPerformanceTradeoff(AIBehaviorProfile.PerformanceTradeoff.QUALITY_OPTIMIZED);
        profile.setAdditionalPreferences(Map.of("includeTests", true));

        Map<String, Object> context = new java.util.LinkedHashMap<>();
        profileService.applyProfileToContext(profile, context);

        assertEquals("high", context.get("securityLevel"));
        assertTrue(Boolean.TRUE.equals(context.get("enableEncryption")));
        assertTrue(Boolean.TRUE.equals(context.get("enableAuditLogging")));
        assertEquals("quality", context.get("optimizeFor"));
        assertTrue(Boolean.TRUE.equals(context.get("includeTests")));
    }
}
