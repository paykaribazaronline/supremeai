package com.supremeai.controller;

import com.supremeai.admin.ProviderAdminService;
import com.supremeai.service.AIProviderDiscoveryService;
import com.supremeai.model.APIProvider;
import com.supremeai.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvidersControllerIntegrationTest {

    @Mock
    private ProviderAdminService providerAdminService;
    @Mock
    private AIProviderDiscoveryService discoveryService;

    private com.supremeai.controller.ProvidersController providersController;

    @BeforeEach
    void setUp() {
        providersController = new com.supremeai.controller.ProvidersController(providerAdminService, discoveryService);
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(String userId, String role) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(userId);
        lenient().when(auth.isAuthenticated()).thenReturn(true);
        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
        lenient().doReturn(authorities).when(auth).getAuthorities();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    // ==================== getConfiguredProviders Tests ====================

    @Test
    void getConfiguredProviders_ReturnsProviderList() {
        APIProvider p1 = new APIProvider("prov-1", "OpenAI", "llm", "active");
        APIProvider p2 = new APIProvider("prov-2", "Anthropic", "llm", "active");

        when(providerAdminService.getAllProviders()).thenReturn(Flux.just(p1, p2));

        ResponseEntity<ApiResponse<Map<String, Object>>> result =
                providersController.getConfiguredProviders();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());
        Map<String, Object> data = result.getBody().getData();
        List<?> providers = (List<?>) data.get("providers");
        assertEquals(2, providers.size());

        verify(providerAdminService).getAllProviders();
    }

    @Test
    void getConfiguredProviders_EmptyList_ReturnsEmpty() {
        when(providerAdminService.getAllProviders()).thenReturn(Flux.empty());

        ResponseEntity<ApiResponse<Map<String, Object>>> result =
                providersController.getConfiguredProviders();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> data = result.getBody().getData();
        List<?> providers = (List<?>) data.get("providers");
        assertTrue(providers.isEmpty());
    }

    // ==================== addProvider Tests ====================

    @Test
    void addProvider_ValidProvider_Success() {
        setSecurityContext("admin-1", "ADMIN");

        APIProvider input = new APIProvider();
        input.setName("New Provider");
        input.setType("gemini");
        input.setApiKey("valid-key");

        APIProvider saved = new APIProvider("prov-new", "New Provider", "gemini", "inactive");

        when(providerAdminService.addProvider(any(APIProvider.class), anyString()))
                .thenReturn(Mono.just(saved));

        ResponseEntity<ApiResponse<Map<String, Object>>> result =
                providersController.addProvider(input);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isSuccess());
        APIProvider resProv = (APIProvider) result.getBody().getData().get("provider");
        assertEquals("prov-new", resProv.getId());

        verify(providerAdminService).addProvider(any(APIProvider.class), anyString());
    }

    @Test
    void addProvider_NoAuthentication_ThrowsException() {
        SecurityContextHolder.clearContext();

        APIProvider input = new APIProvider();
        input.setName("Test");
        input.setType("gemini");
        input.setApiKey("key");

        ResponseEntity<ApiResponse<Map<String, Object>>> result =
                providersController.addProvider(input);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertFalse(result.getBody().isSuccess());
    }

    // ==================== testProviderKey Tests ====================

    @Test
    void testProviderKey_ValidKey_ReturnsOk() {
        when(providerAdminService.validateKey("OpenAI", "sk-test"))
                .thenReturn(Mono.just(true));

        ResponseEntity<ApiResponse<Map<String, Object>>> result =
                providersController.testProviderKey(Map.of(
                        "name", "OpenAI",
                        "apiKey", "sk-test"
                ));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(true, result.getBody().getData().get("valid"));

        verify(providerAdminService).validateKey("OpenAI", "sk-test");
    }

    @Test
    void testProviderKey_InvalidKey_ReturnsUnauthorized() {
        when(providerAdminService.validateKey("OpenAI", "bad-key"))
                .thenReturn(Mono.just(false));

        ResponseEntity<ApiResponse<Map<String, Object>>> result =
                providersController.testProviderKey(Map.of(
                        "name", "OpenAI",
                        "apiKey", "bad-key"
                ));

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void testProviderKey_MissingNameOrKey_ReturnsBadRequest() {
        ResponseEntity<ApiResponse<Map<String, Object>>> result1 =
                providersController.testProviderKey(Map.of("name", "OpenAI"));
        // apiKey missing

        ResponseEntity<ApiResponse<Map<String, Object>>> result2 =
                providersController.testProviderKey(Map.of("apiKey", "key"));
        // name missing

        assertEquals(HttpStatus.BAD_REQUEST, result1.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, result2.getStatusCode());
    }

    // ==================== discoverModels Tests ====================

    @Test
    void discoverModels_ReturnsModelList() {
        when(discoveryService.discoverModels(anyString())).thenReturn(Flux.just(Map.of("model", "gpt-4")));
        ResponseEntity<ApiResponse<List<Map<String, Object>>>> result =
                providersController.discoverModels("gpt");

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getData().size());
    }

    // ==================== getHealthStats Tests ====================

    @Test
    void getHealthStats_ReturnsHealthData() {
        APIProvider p1 = new APIProvider(); p1.setStatus("active");
        APIProvider p2 = new APIProvider(); p2.setStatus("error");

        when(providerAdminService.getHealthStats()).thenReturn(Mono.just(Map.of(
                "total", 2,
                "active", 1L,
                "error", 1L,
                "dead", 0L,
                "healthScore", 50L
        )));

        ResponseEntity<ApiResponse<Map<String, Object>>> result =
                providersController.getHealthStats();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> stats = result.getBody().getData();
        assertEquals(2, stats.get("total"));
        assertEquals(50L, stats.get("healthScore"));

        verify(providerAdminService).getHealthStats();
    }

    // ==================== suggestRoles Tests ====================

    @Test
    void suggestRoles_ValidProvider_ReturnsRoles() {
        APIProvider provider = new APIProvider("prov-1", "OpenAI", "openai", "active");

        when(providerAdminService.getAllProviders()).thenReturn(Flux.just(provider));
        when(providerAdminService.suggestRoles(provider)).thenReturn(
                List.of("COMMUNICATE", "EXECUTE_TASKS", "VOTING")
        );

        ResponseEntity<ApiResponse<List<String>>> result =
                providersController.getConfiguredRoles("prov-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        List<String> roles = result.getBody().getData();
        assertEquals(3, roles.size());
        assertTrue(roles.contains("COMMUNICATE"));

        verify(providerAdminService).getAllProviders();
    }

    @Test
    void suggestRoles_NonExistentProvider_ReturnsNotFound() {
        when(providerAdminService.getAllProviders()).thenReturn(Flux.empty());

        ResponseEntity<ApiResponse<List<String>>> result =
                providersController.getConfiguredRoles("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    // ==================== removeProvider Tests ====================

    @Test
    void removeProvider_ValidRequest_Success() {
        setSecurityContext("admin-1", "ADMIN");

        when(providerAdminService.removeDeadProviders(anyString())).thenReturn(Mono.empty());

        ResponseEntity<ApiResponse<String>> result =
                providersController.cleanupDeadProviders();

        assertEquals(HttpStatus.OK, result.getStatusCode());

        verify(providerAdminService).removeDeadProviders("admin-1");
    }
}
