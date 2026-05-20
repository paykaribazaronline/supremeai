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
        lenient().when(auth.isAuthenticated()).thenReturn(true);
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void getConfiguredProviders_shouldReturnWrappedProviderList() {
        APIProvider p1 = new APIProvider("prov-1", "OpenAI", "llm", "active");
        APIProvider p2 = new APIProvider("prov-2", "Anthropic", "llm", "active");

        when(providerAdminService.getAllProviders()).thenReturn(Flux.just(p1, p2));

        ResponseEntity<ApiResponse<Map<String, Object>>> result =
                controller.getConfiguredProviders();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());
        Map<String, Object> data = (Map<String, Object>) result.getBody().getData();
        List<?> providers = (List<?>) data.get("providers");
        assertEquals(2, providers.size());

        verify(providerAdminService).getAllProviders();
    }

    @Test
    void addProvider_shouldDelegateToService() {
        setAuthentication("admin");
        APIProvider input = new APIProvider(null, "New Provider", "llm", "active");
        APIProvider saved = new APIProvider("prov-new", "New Provider", "llm", "active");

        when(providerAdminService.addProvider(eq(input), anyString())).thenReturn(Mono.just(saved));

        ResponseEntity<ApiResponse<Map<String, Object>>> result = controller.addProvider(input);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());

        verify(providerAdminService).addProvider(eq(input), anyString());
    }

    @Test
    void updateProviderById_shouldDelegateToService() {
        setAuthentication("admin");
        APIProvider input = new APIProvider(null, "Updated Provider", "llm", "active");
        APIProvider saved = new APIProvider("prov-1", "Updated Provider", "llm", "active");

        when(providerAdminService.updateProvider(eq("prov-1"), eq(input), anyString())).thenReturn(Mono.just(saved));

        ResponseEntity<ApiResponse<Map<String, Object>>> result = controller.updateProviderById("prov-1", input);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());

        verify(providerAdminService).updateProvider(eq("prov-1"), eq(input), anyString());
    }

    @Test
    void deleteProvider_shouldDelegateToService() {
        setAuthentication("admin");
        when(providerAdminService.deleteProvider(eq("prov-1"), anyString())).thenReturn(Mono.empty());

        ResponseEntity<ApiResponse<String>> result = controller.deleteProvider("prov-1");

        assertTrue(result.getStatusCode().is2xxSuccessful());

        verify(providerAdminService).deleteProvider(eq("prov-1"), anyString());
    }

    @Test
    void testProviderKey_shouldDelegateToService() {
        when(providerAdminService.validateKey("OpenAI", "sk-test")).thenReturn(Mono.just(true));

        ResponseEntity<ApiResponse<Map<String, Object>>> result =
                controller.testProviderKey(Map.of("name", "OpenAI", "apiKey", "sk-test"));

        assertEquals(HttpStatus.OK, result.getStatusCode());

        verify(providerAdminService).validateKey("OpenAI", "sk-test");
    }

    @Test
    void discoverModels_shouldDelegateToDiscoveryService() {
        when(discoveryService.discoverModels(any())).thenReturn(Flux.empty());

        ResponseEntity<ApiResponse<List<Map<String, Object>>>> result =
                controller.discoverModels("test");

        assertEquals(HttpStatus.OK, result.getStatusCode());

        verify(discoveryService).discoverModels("test");
    }
}
