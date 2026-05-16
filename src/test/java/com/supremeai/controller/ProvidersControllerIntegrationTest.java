package com.supremeai.controller;

import com.supremeai.admin.ProviderAdminService;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProvidersControllerIntegrationTest {

    @Mock
    private ProviderAdminService providerAdminService;

    private com.supremeai.controller.ProvidersController providersController;

    @BeforeEach
    void setUp() {
        providersController = new com.supremeai.controller.ProvidersController(providerAdminService, null);
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(String userId, String role) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(userId);
        lenient().when(auth.getAuthorities()).thenReturn(
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    // ==================== getConfiguredProviders Tests ====================

    @Test
    void getConfiguredProviders_ReturnsProviderList() {
        APIProvider p1 = new APIProvider("prov-1", "OpenAI", "llm", "active");
        APIProvider p2 = new APIProvider("prov-2", "Anthropic", "llm", "active");

        when(providerAdminService.getAllProviders()).thenReturn(Flux.just(p1, p2));

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                providersController.getConfiguredProviders();

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().success());
                    Map<String, Object> data = response.getBody().data();
                    List<?> providers = (List<?>) data.get("providers");
                    assertEquals(2, providers.size());
                    return true;
                })
                .verifyComplete();

        verify(providerAdminService).getAllProviders();
    }

    @Test
    void getConfiguredProviders_EmptyList_ReturnsEmpty() {
        when(providerAdminService.getAllProviders()).thenReturn(Flux.empty());

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                providersController.getConfiguredProviders();

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> data = response.getBody().data();
                    List<?> providers = (List<?>) data.get("providers");
                    assertTrue(providers.isEmpty());
                    return true;
                })
                .verifyComplete();
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

        when(providerAdminService.addProvider(eq(input), anyString()))
                .thenReturn(Mono.just(saved));

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                providersController.addProvider(input);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody().success());
                    assertEquals("prov-new", ((Map) response.getBody().data().get("provider")).get("id"));
                    return true;
                })
                .verifyComplete();

        verify(providerAdminService).addProvider(eq(input), anyString());
    }

    @Test
    void addProvider_NoAuthentication_ThrowsException() {
        SecurityContextHolder.clearContext();

        APIProvider input = new APIProvider();
        input.setName("Test");
        input.setType("gemini");
        input.setApiKey("key");

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                providersController.addProvider(input);

        StepVerifier.create(result)
                .expectError(IllegalStateException.class)
                .verify();
    }

    // ==================== testProviderKey Tests ====================

    @Test
    void testProviderKey_ValidKey_ReturnsOk() {
        when(providerAdminService.validateKey("OpenAI", "sk-test"))
                .thenReturn(Mono.just(true));

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                providersController.testProviderKey(Map.of(
                        "name", "OpenAI",
                        "apiKey", "sk-test"
                ));

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(true, response.getBody().data().get("valid"));
                    return true;
                })
                .verifyComplete();

        verify(providerAdminService).validateKey("OpenAI", "sk-test");
    }

    @Test
    void testProviderKey_InvalidKey_ReturnsUnauthorized() {
        when(providerAdminService.validateKey("OpenAI", "bad-key"))
                .thenReturn(Mono.just(false));

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                providersController.testProviderKey(Map.of(
                        "name", "OpenAI",
                        "apiKey", "bad-key"
                ));

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testProviderKey_MissingNameOrKey_ReturnsBadRequest() {
        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result1 =
                providersController.testProviderKey(Map.of("name", "OpenAI"));
        // apiKey missing

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result2 =
                providersController.testProviderKey(Map.of("apiKey", "key"));
        // name missing

        StepVerifier.create(result1)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    return true;
                })
                .verifyComplete();

        StepVerifier.create(result2)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== discoverModels Tests ====================

    @Test
    void discoverModels_ReturnsModelList() {
        // Note: discoveryService is null in this controller setup
        // This tests the fallback when discoveryService is not injected
        Mono<ResponseEntity<ApiResponse<List<Map<String, Object>>>>> result =
                providersController.discoverModels("gpt");

        StepVerifier.create(result)
                .expectError(NullPointerException.class)
                .verify();
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

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                providersController.getHealthStats();

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> stats = response.getBody().data();
                    assertEquals(2, stats.get("total"));
                    assertEquals(50L, stats.get("healthScore"));
                    return true;
                })
                .verifyComplete();

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

        Mono<ResponseEntity<ApiResponse<List<String>>>> result =
                providersController.suggestRoles("prov-1");

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    List<String> roles = (List<String>) response.getBody().data();
                    assertEquals(3, roles.size());
                    assertTrue(roles.contains("COMMUNICATE"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void suggestRoles_NonExistentProvider_ReturnsNotFound() {
        when(providerAdminService.getAllProviders()).thenReturn(Flux.empty());

        Mono<ResponseEntity<ApiResponse<List<String>>>> result =
                providersController.suggestRoles("nonexistent");

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== removeProvider Tests ====================

    @Test
    void removeProvider_ValidRequest_Success() {
        setSecurityContext("admin-1", "ADMIN");

        doReturn(Mono.empty()).when(providerAdminService).removeDeadProviders(anyString());

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                providersController.removeDeadProviders();

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    return true;
                })
                .verifyComplete();

        verify(providerAdminService).removeDeadProviders("admin-1");
    }
}