package com.supremeai.controller;

import com.supremeai.model.APIProvider;
import com.supremeai.admin.ProviderAdminService;
import com.supremeai.service.AIProviderDiscoveryService;
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
    private ProviderAdminService providerAdminService;

    @Mock
    private AIProviderDiscoveryService discoveryService;

    private ProvidersController controller;

    @BeforeEach
    void setUp() {
        controller = new ProvidersController(providerAdminService, discoveryService);
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String userId) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(userId);
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void getConfiguredProviders_shouldReturnWrappedProviderList() {
        APIProvider p1 = new APIProvider("prov-1", "OpenAI", "llm", "active");
        APIProvider p2 = new APIProvider("prov-2", "Anthropic", "llm", "active");

        when(providerAdminService.getAllProviders()).thenReturn(Flux.just(p1, p2));

        StepVerifier.create(controller.getConfiguredProviders())
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().success());
                    Map<String, Object> data = (Map<String, Object>) response.getBody().data();
                    List<?> providers = (List<?>) data.get("providers");
                    assertEquals(2, providers.size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void addProvider_shouldDelegateToService() {
        setAuthentication("admin");
        APIProvider input = new APIProvider(null, "New Provider", "llm", "active");
        APIProvider saved = new APIProvider("prov-new", "New Provider", "llm", "active");

        when(providerAdminService.addProvider(eq(input), anyString())).thenReturn(Mono.just(saved));

        StepVerifier.create(controller.addProvider(input))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().success());
                    return true;
                })
                .verifyComplete();
        
        verify(providerAdminService).addProvider(eq(input), anyString());
    }

    @Test
    void updateProviderById_shouldDelegateToService() {
        setAuthentication("admin");
        APIProvider input = new APIProvider(null, "Updated Provider", "llm", "active");
        APIProvider saved = new APIProvider("prov-1", "Updated Provider", "llm", "active");

        when(providerAdminService.updateProvider(eq("prov-1"), eq(input), anyString())).thenReturn(Mono.just(saved));

        StepVerifier.create(controller.updateProviderById("prov-1", input))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().success());
                    return true;
                })
                .verifyComplete();

        verify(providerAdminService).updateProvider(eq("prov-1"), eq(input), anyString());
    }

    @Test
    void removeProvider_shouldDelegateToService() {
        setAuthentication("admin");
        when(providerAdminService.deleteProvider(eq("prov-1"), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(controller.removeProvider(Map.of("providerId", "prov-1")))
                .expectNextMatches(response -> {
                    assertTrue(response.getStatusCode().is2xxSuccessful());
                    return true;
                })
                .verifyComplete();

        verify(providerAdminService).deleteProvider(eq("prov-1"), anyString());
    }

    @Test
    void testProviderKey_shouldDelegateToService() {
        when(providerAdminService.validateKey("OpenAI", "sk-test")).thenReturn(Mono.just(true));

        StepVerifier.create(controller.testProviderKey(Map.of("name", "OpenAI", "apiKey", "sk-test")))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    return true;
                })
                .verifyComplete();

        verify(providerAdminService).validateKey("OpenAI", "sk-test");
    }

    @Test
    void discoverModels_shouldDelegateToDiscoveryService() {
        when(discoveryService.discoverModels(any())).thenReturn(Flux.empty());

        StepVerifier.create(controller.discoverModels("test"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    return true;
                })
                .verifyComplete();

        verify(discoveryService).discoverModels("test");
    }
}
