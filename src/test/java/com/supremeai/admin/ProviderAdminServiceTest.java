package com.supremeai.admin;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProviderAdminServiceTest {

    @Mock
    private ProviderRepository providerRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private AIProviderDiscoveryService discoveryService;
    @Mock
    private ProviderRoleSuggestionService roleSuggestionService;
    @Mock
    private AdminProviderValidationService adminProviderValidationService;
    @Mock
    private ProviderTypeRegistry providerTypeRegistry;

    private ProviderAdminService providerAdminService;

    @BeforeEach
    void setUp() {
        providerAdminService = new ProviderAdminService(
                providerRepository,
                activityLogRepository,
                discoveryService,
                roleSuggestionService,
                adminProviderValidationService,
                providerTypeRegistry
        );
    }

    @Test
    void getAllProviders_ShouldReturnFlux() {
        APIProvider provider = new APIProvider();
        provider.setName("Test Provider");
        
        when(providerRepository.findAll()).thenReturn(Flux.just(provider));

        StepVerifier.create(providerAdminService.getAllProviders())
                .expectNext(provider)
                .verifyComplete();
    }

    @Test
    void addProvider_WithValidKey_ShouldSaveAndLog() {
        APIProvider provider = new APIProvider();
        provider.setName("New Provider");
        provider.setType("openai");
        provider.setApiKey("valid-key");

        when(discoveryService.validateKey(anyString(), anyString())).thenReturn(Mono.just(true));
        when(providerRepository.save(any(APIProvider.class))).thenReturn(Mono.just(provider));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(providerAdminService.addProvider(provider, "admin-1"))
                .expectNext(provider)
                .verifyComplete();

        verify(providerRepository).save(provider);
        verify(activityLogRepository).save(any(ActivityLog.class));
    }

    @Test
    void addProvider_WithInvalidKey_ShouldError() {
        APIProvider provider = new APIProvider();
        provider.setName("New Provider");
        provider.setType("openai");
        provider.setApiKey("invalid-key");

        when(discoveryService.validateKey(anyString(), anyString())).thenReturn(Mono.just(false));

        StepVerifier.create(providerAdminService.addProvider(provider, "admin-1"))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(providerRepository, never()).save(any());
    }

    @Test
    void updateProvider_ShouldUpdateAndLog() {
        String id = "provider-1";
        APIProvider existing = new APIProvider();
        existing.setId(id);
        existing.setName("Old Name");
        existing.setApiKey("old-key");

        APIProvider updated = new APIProvider();
        updated.setName("New Name");
        updated.setApiKey("old-key"); // Key didn't change

        when(providerRepository.findById(id)).thenReturn(Mono.just(existing));
        when(providerRepository.save(any(APIProvider.class))).thenReturn(Mono.just(updated));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(providerAdminService.updateProvider(id, updated, "admin-1"))
                .expectNext(updated)
                .verifyComplete();

        verify(providerRepository).save(updated);
    }

    @Test
    void reviveProvider_ShouldChangeStatus() {
        String id = "provider-1";
        APIProvider provider = new APIProvider();
        provider.setId(id);
        provider.setStatus("dead");

        when(providerRepository.findById(id)).thenReturn(Mono.just(provider));
        when(providerRepository.save(any(APIProvider.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(providerAdminService.reviveProvider(id, "admin-1"))
                .assertNext(p -> {
                    assert "active".equals(p.getStatus());
                })
                .verifyComplete();
    }

    @Test
    void deleteProvider_ShouldDeleteAndLog() {
        String id = "provider-1";
        APIProvider provider = new APIProvider();
        provider.setName("To Delete");

        when(providerRepository.findById(id)).thenReturn(Mono.just(provider));
        when(providerRepository.deleteById(id)).thenReturn(Mono.empty());
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(providerAdminService.deleteProvider(id, "admin-1"))
                .verifyComplete();

        verify(providerRepository).deleteById(id);
    }

    @Test
    void getHealthStats_ShouldCalculateCorrectly() {
        APIProvider p1 = new APIProvider(); p1.setStatus("active");
        APIProvider p2 = new APIProvider(); p2.setStatus("error");
        APIProvider p3 = new APIProvider(); p3.setStatus("dead");

        when(providerRepository.findAll()).thenReturn(Flux.just(p1, p2, p3));

        StepVerifier.create(providerAdminService.getHealthStats())
                .assertNext(stats -> {
                    assertEquals(3, stats.get("total")); // list.size() is int
                    assertEquals(1L, stats.get("active")); // count() is long
                    assertEquals(1L, stats.get("error"));
                    assertEquals(1L, stats.get("dead"));
                    assertEquals(33L, stats.get("healthScore"));
                })
                .verifyComplete();
    }

    @Test
    void removeDeadProviders_ShouldOnlyDeleteDeadOnes() {
        APIProvider p1 = new APIProvider(); p1.setId("1"); p1.setName("Active Provider"); p1.setStatus("active");
        APIProvider p2 = new APIProvider(); p2.setId("2"); p2.setName("Dead Provider"); p2.setStatus("dead");

        when(providerRepository.findAll()).thenReturn(Flux.just(p1, p2));
        when(providerRepository.deleteById("2")).thenReturn(Mono.empty());
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(providerAdminService.removeDeadProviders("admin-1"))
                .verifyComplete();

        verify(providerRepository, never()).deleteById("1");
        verify(providerRepository).deleteById("2");
    }
}
