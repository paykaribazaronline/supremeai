package com.supremeai.controller;

import com.supremeai.model.APIProvider;
import com.supremeai.model.ActivityLog;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ProvidersControllerTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private AIProviderFactory aiProviderFactory;

    @Mock
    private com.supremeai.service.AIProviderDiscoveryService discoveryService;

    private ProvidersController controller;

    @BeforeEach
    void setUp() {
        controller = new ProvidersController(providerRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "activityLogRepository", activityLogRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "aiProviderFactory", aiProviderFactory);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "discoveryService", discoveryService);
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String userId, boolean isAdmin) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(userId);
        if (isAdmin) {
            lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();
        } else {
            lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(auth).getAuthorities();
        }
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }



    private void setAdminAuthentication(String adminId) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(adminId);
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void getConfiguredProviders_shouldReturnWrappedProviderList() {
        APIProvider p1 = new APIProvider("prov-1", "OpenAI", "llm", "active");
        APIProvider p2 = new APIProvider("prov-2", "Anthropic", "llm", "active");

        when(providerRepository.findAll()).thenReturn(Flux.just(p1, p2));

        StepVerifier.create(controller.getConfiguredProviders())
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().success());
                    Map<String, Object> data = response.getBody().data();
                    List<?> providers = (List<?>) data.get("providers");
                    assertEquals(2, providers.size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getConfiguredProviders_shouldReturnEmptyList_whenNone() {
        when(providerRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(controller.getConfiguredProviders())
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> data = response.getBody().data();
                    List<?> providers = (List<?>) data.get("providers");
                    assertTrue(providers.isEmpty());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void addProvider_shouldSaveAndReturnProvider() {
        setAuthentication("admin", true);

        APIProvider input = new APIProvider(null, "New Provider", "llm", "active");
        APIProvider saved = new APIProvider("prov-new", "New Provider", "llm", "active");

        when(providerRepository.save(any(APIProvider.class))).thenReturn(Mono.just(saved));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(controller.addProvider(input))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().success());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void updateProviderById_shouldSetIdAndSave() {
        setAuthentication("admin", true);

        APIProvider input = new APIProvider(null, "Updated Provider", "llm", "active");
        APIProvider saved = new APIProvider("prov-1", "Updated Provider", "llm", "active");

        when(providerRepository.save(any(APIProvider.class))).thenReturn(Mono.just(saved));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(controller.updateProviderById("prov-1", input))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().success());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void removeProvider_shouldDeleteAndReturnSuccess() {
        setAuthentication("admin", true);

        APIProvider existing = new APIProvider("prov-1", "OpenAI", "llm", "active");

        when(providerRepository.findById("prov-1")).thenReturn(Mono.just(existing));
        when(providerRepository.deleteById("prov-1")).thenReturn(Mono.empty());
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));

        StepVerifier.create(controller.removeProvider(Map.of("providerId", "prov-1")))
                .expectNextMatches(response -> {
                    assertTrue(response.getStatusCode().is2xxSuccessful());
                    assertTrue(response.getBody().success());
                    assertEquals("Provider removed", response.getBody().data());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void removeProvider_shouldReturnBadRequest_whenProviderIdMissing() {
        StepVerifier.create(controller.removeProvider(Map.of()))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void removeProvider_shouldHandleNotFound() {
        setAuthentication("admin", true);
        when(providerRepository.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(controller.removeProvider(Map.of("providerId", "missing")))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testProviderKey_shouldReturnBadRequest_whenNameMissing() {
        StepVerifier.create(controller.testProviderKey(Map.of("apiKey", "sk-test")))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testProviderKey_shouldReturnBadRequest_whenApiKeyMissing() {
        StepVerifier.create(controller.testProviderKey(Map.of("name", "OpenAI")))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testProviderKey_shouldReturnUnauthorized_whenUnsupportedProvider() {
        when(discoveryService.validateKey("UnknownAI", "sk-test"))
                .thenReturn(Mono.just(false));

        StepVerifier.create(controller.testProviderKey(Map.of("name", "UnknownAI", "apiKey", "sk-test")))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }
}
