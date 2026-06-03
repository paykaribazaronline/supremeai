package com.supremeai.service;

import com.supremeai.admin.ProviderAdminService;
import com.supremeai.model.APIProvider;
import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.service.AIProviderDiscoveryService;
import com.supremeai.service.AdminProviderValidationService;
import com.supremeai.service.ProviderRoleSuggestionService;
import com.supremeai.service.ProviderTypeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderAdminServiceExtendedTest {ProviderRepositorypublic ProviderAdminServiceExtendedTest(ProviderRepository providerRepository, ActivityLogRepository activityLogRepository, AIProviderDiscoveryService discoveryService, ProviderRoleSuggestionService roleSuggestionService, AdminProviderValidationService adminProviderValidationService, ProviderTypeRegistry providerTypeRegistry, ProviderAdminService providerAdminService, APIProvider testProvider) {
ProviderRepository    this.providerRepository = providerRepository;
ProviderRepository    this.activityLogRepository = activityLogRepository;
ProviderRepository    this.discoveryService = discoveryService;
ProviderRepository    this.roleSuggestionService = roleSuggestionService;
ProviderRepository    this.adminProviderValidationService = adminProviderValidationService;
ProviderRepository    this.providerTypeRegistry = providerTypeRegistry;
ProviderRepository    this.providerAdminService = providerAdminService;
ProviderRepository    this.testProvider = testProvider;
ProviderRepository}


















    @BeforeEach
    void setUp() {
        providerAdminService = new ProviderAdminService(
                providerRepository, activityLogRepository, discoveryService,
                roleSuggestionService, adminProviderValidationService, providerTypeRegistry
        );

        testProvider = new APIProvider("prov-1", "Test Provider", "gemini", "active");
        testProvider.setApiKey("valid-key");
    }

    // ==================== addProvider Edge Cases ====================

    @ParameterizedTest
    @ValueSource(strings = {"gemini", "openai", "claude", "ollama", "deepseek"})
    void addProvider_WithVariousValidTypes_Succeeds(String providerType) {
        APIProvider provider = new APIProvider();
        provider.setName("Provider " + providerType);
        provider.setType(providerType);
        provider.setApiKey("valid-key-" + providerType);

        when(discoveryService.validateKey(providerType, "valid-key-" + providerType))
                .thenReturn(Mono.just(true));
        when(providerRepository.save(any(APIProvider.class)))
                .thenReturn(Mono.just(provider));
        when(activityLogRepository.save(any(ActivityLog.class)))
                .thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(providerAdminService.addProvider(provider, "admin-1"))
                .expectNextMatches(saved -> saved.getType().equals(providerType))
                .verifyComplete();

        verify(discoveryService).validateKey(providerType, "valid-key-" + providerType);
        verify(providerRepository).save(any(APIProvider.class));
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    void addProvider_NullApiKey_ThrowsException() {
        APIProvider provider = new APIProvider();
        provider.setName("Broken Provider");
        provider.setType("gemini");
        provider.setApiKey(null);

        StepVerifier.create(providerAdminService.addProvider(provider, "admin-1"))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(providerRepository, never()).save(any());
    }

    @Test
    void addProvider_EmptyApiKey_ThrowsException() {
        APIProvider provider = new APIProvider();
        provider.setName("Broken Provider");
        provider.setType("gemini");
        provider.setApiKey("");

        when(discoveryService.validateKey(anyString(), eq("")))
                .thenReturn(Mono.just(false));

        StepVerifier.create(providerAdminService.addProvider(provider, "admin-1"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ==================== updateProvider Edge Cases ====================

    @Test
    void updateProvider_ProviderNotFound_ReturnsEmpty() {
        when(providerRepository.findById("nonexistent")).thenReturn(Mono.empty());

        APIProvider update = new APIProvider();
        update.setName("Updated Name");

        StepVerifier.create(providerAdminService.updateProvider("nonexistent", update, "admin-1"))
                .verifyComplete();

        verify(providerRepository, never()).save(any());
    }

    @Test
    void updateProvider_ChangeFromActiveToInactive() {
        APIProvider existing = new APIProvider("prov-1", "Test", "gemini", "active");
        existing.setApiKey("key");

        APIProvider update = new APIProvider();
        update.setStatus("inactive");
        update.setName("Test");
        update.setApiKey("key"); // Key unchanged

        when(providerRepository.findById("prov-1")).thenReturn(Mono.just(existing));
        when(providerRepository.save(any(APIProvider.class))).thenReturn(Mono.just(update));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(providerAdminService.updateProvider("prov-1", update, "admin-1"))
                .expectNextMatches(saved -> "inactive".equals(saved.getStatus()))
                .verifyComplete();
    }

    @Test
    void updateProvider_KeyChangedFromDeadToActive() {
        APIProvider existing = new APIProvider("prov-1", "Test", "gemini", "dead");
        existing.setApiKey("old-key");
        existing.setDeadReason("Invalid key");

        APIProvider update = new APIProvider();
        update.setApiKey("new-valid-key");
        update.setName("Test");

        when(providerRepository.findById("prov-1")).thenReturn(Mono.just(existing));
        when(discoveryService.validateKey("gemini", "new-valid-key")).thenReturn(Mono.just(true));
        when(providerRepository.save(any(APIProvider.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(providerAdminService.updateProvider("prov-1", update, "admin-1"))
                .expectNextMatches(saved -> {
                    assertEquals("active", saved.getStatus());
                    assertNull(saved.getDeadReason());
                    assertNull(saved.getDeadAt());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== validateKey Tests ====================

    @Test
    void validateKey_NullType_ReturnsFalse() {
        StepVerifier.create(providerAdminService.validateKey(null, "some-key"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void validateKey_NullKey_ReturnsFalse() {
        StepVerifier.create(providerAdminService.validateKey("gemini", null))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void validateKey_BothNull_ReturnsFalse() {
        StepVerifier.create(providerAdminService.validateKey(null, null))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void validateKey_DiscoveryServiceError_ReturnsFalse() {
        when(discoveryService.validateKey("gemini", "key"))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        StepVerifier.create(providerAdminService.validateKey("gemini", "key"))
                .expectNext(false)
                .verifyComplete();
    }

    // ==================== getHealthStats Edge Cases ====================

    @Test
    void getHealthStats_NoProviders_ReturnsZeroHealth() {
        when(providerRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(providerAdminService.getHealthStats())
                .expectNextMatches(stats -> {
                    assertEquals(0, stats.get("total"));
                    assertEquals(0L, stats.get("active"));
                    assertEquals(100L, stats.get("healthScore"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getHealthStats_AllActive_ReturnsFullHealth() {
        APIProvider p1 = new APIProvider(); p1.setStatus("active");
        APIProvider p2 = new APIProvider(); p2.setStatus("active");
        APIProvider p3 = new APIProvider(); p3.setStatus("active");

        when(providerRepository.findAll()).thenReturn(Flux.just(p1, p2, p3));

        StepVerifier.create(providerAdminService.getHealthStats())
                .expectNextMatches(stats -> {
                    assertEquals(3, stats.get("total"));
                    assertEquals(3L, stats.get("active"));
                    assertEquals(100L, stats.get("healthScore"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getHealthStats_AllDead_ReturnsZeroHealth() {
        APIProvider p1 = new APIProvider(); p1.setStatus("dead");
        APIProvider p2 = new APIProvider(); p2.setStatus("dead");

        when(providerRepository.findAll()).thenReturn(Flux.just(p1, p2));

        StepVerifier.create(providerAdminService.getHealthStats())
                .expectNextMatches(stats -> {
                    assertEquals(2, stats.get("total"));
                    assertEquals(0L, stats.get("active"));
                    assertEquals(0L, stats.get("healthScore"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getHealthStats_MixedStatus_CalculatesCorrectly() {
        APIProvider p1 = new APIProvider(); p1.setStatus("active");
        APIProvider p2 = new APIProvider(); p2.setStatus("error");
        APIProvider p3 = new APIProvider(); p3.setStatus("dead");
        APIProvider p4 = new APIProvider(); p4.setStatus("inactive");

        when(providerRepository.findAll()).thenReturn(Flux.just(p1, p2, p3, p4));

        StepVerifier.create(providerAdminService.getHealthStats())
                .expectNextMatches(stats -> {
                    assertEquals(4, stats.get("total"));
                    assertEquals(1L, stats.get("active"));
                    assertEquals(1L, stats.get("error"));
                    assertEquals(1L, stats.get("dead"));
                    assertEquals(25L, stats.get("healthScore"));
                    return true;
                })
                .verifyComplete();
    }

    // ==================== suggestRoles Tests ====================

    @Test
    void suggestRoles_ReturnsListOfRoles() {
        APIProvider provider = new APIProvider();
        provider.setName("gemini");
        provider.setType("gemini");

        when(roleSuggestionService.suggestRoles(any(APIProvider.class)))
                .thenReturn(List.of("COMMUNICATE", "EXECUTE_TASKS", "VOTING"));

        List<String> roles = providerAdminService.suggestRoles(provider);

        assertFalse(roles.isEmpty());
        assertEquals("COMMUNICATE", roles.get(0));
        verify(roleSuggestionService).suggestRoles(provider);
    }

    @Test
    void suggestRoles_NullProvider_ReturnsEmptyOrHandlesGracefully() {
        when(roleSuggestionService.suggestRoles(null))
                .thenReturn(Collections.emptyList());

        List<String> roles = providerAdminService.suggestRoles(null);

        assertTrue(roles.isEmpty());
    }

    // ==================== patchCapability Tests ====================

    @Test
    void patchCapability_EnableCommunication() {
        APIProvider existing = new APIProvider("prov-1", "Test", "gemini", "active");
        existing.setCanCommunicate(false);

        Map<String, Object> updates = Map.of("canCommunicate", true);

        when(providerRepository.findById("prov-1")).thenReturn(Mono.just(existing));
        when(providerRepository.save(any(APIProvider.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(providerAdminService.patchCapability("prov-1", updates, "admin-1"))
                .expectNextMatches(saved -> saved.isCanCommunicate())
                .verifyComplete();
    }

    @Test
    void patchCapability_MultipleCapabilities() {
        APIProvider existing = new APIProvider("prov-1", "Test", "gemini", "active");
        existing.setCanCommunicate(false);
        existing.setCanExecuteTasks(false);
        existing.setCanParticipateInVoting(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("canCommunicate", true);
        updates.put("canExecuteTasks", true);
        updates.put("canParticipateInVoting", true);
        updates.put("assignedRoles", List.of("ADMIN", "EXECUTOR"));

        when(providerRepository.findById("prov-1")).thenReturn(Mono.just(existing));
        when(providerRepository.save(any(APIProvider.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(providerAdminService.patchCapability("prov-1", updates, "admin-1"))
                .expectNextMatches(saved -> {
                    assertTrue(saved.isCanCommunicate());
                    assertTrue(saved.isCanExecuteTasks());
                    assertTrue(saved.isCanParticipateInVoting());
                    assertEquals(List.of("ADMIN", "EXECUTOR"), saved.getAssignedRoles());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void patchCapability_NonExistentProvider_ReturnsEmpty() {
        when(providerRepository.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(providerAdminService.patchCapability("nonexistent", Map.of("canCommunicate", true), "admin-1"))
                .verifyComplete();
    }

    // ==================== removeDeadProviders Tests ====================

    @Test
    void removeDeadProviders_OnlyDeadRemoved() {
        APIProvider active = new APIProvider("prov-1", "Active", "gemini", "active");
        APIProvider dead1 = new APIProvider("prov-2", "Dead 1", "openai", "dead");
        APIProvider dead2 = new APIProvider("prov-3", "Dead 2", "claude", "dead");
        APIProvider error = new APIProvider("prov-4", "Error", "gemini", "error");

        when(providerRepository.findAll()).thenReturn(Flux.just(active, dead1, dead2, error));
        when(providerRepository.deleteById("prov-2")).thenReturn(Mono.empty());
        when(providerRepository.deleteById("prov-3")).thenReturn(Mono.empty());
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(providerAdminService.removeDeadProviders("admin-1"))
                .verifyComplete();

        verify(providerRepository).deleteById("prov-2");
        verify(providerRepository).deleteById("prov-3");
        verify(providerRepository, never()).deleteById("prov-1");
        verify(providerRepository, never()).deleteById("prov-4");
    }

    @Test
    void removeDeadProviders_NoDeadProviders_NoDeletions() {
        APIProvider active = new APIProvider("prov-1", "Active", "gemini", "active");
        APIProvider error = new APIProvider("prov-2", "Error", "openai", "error");

        when(providerRepository.findAll()).thenReturn(Flux.just(active, error));

        StepVerifier.create(providerAdminService.removeDeadProviders("admin-1"))
                .verifyComplete();

        verify(providerRepository, never()).deleteById(anyString());
    }

    // ==================== triggerValidation Tests ====================

    @Test
    void triggerValidation_InvokesValidationService() {
        doNothing().when(adminProviderValidationService).validateAllActiveProviders();

        providerAdminService.triggerValidation();

        // Give async operation time
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        verify(adminProviderValidationService, timeout(2000)).validateAllActiveProviders();
    }
}