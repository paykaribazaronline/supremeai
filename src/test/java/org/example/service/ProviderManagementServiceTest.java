package org.example.service;

import org.example.model.APIProvider;
import org.example.model.Quota;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderManagementServiceTest {

    @Mock
    private ProviderRegistryService providerRegistryService;

    @Mock
    private AIAPIService aiApiService;

    @Mock
    private QuotaService quotaService;

    @Mock
    private ProviderAuditService providerAuditService;

    @InjectMocks
    private ProviderManagementService providerManagementService;

    private APIProvider provider;

    @BeforeEach
    void setUp() {
        provider = new APIProvider();
        provider.setId("google-gemini");
        provider.setName("Google Gemini");
        provider.setBaseModel("GEMINI");
        provider.setApiKey("old-key");
        provider.setCreatedByEmail("admin@supremeai.com");
        provider.setMonthlyQuota(1000);
        provider.setFreeQuotaPercent(80);
        provider.setRateLimitPerMinute(100);
        provider.setCreatedAt(LocalDateTime.now());

    }

    @Test
    void rotateProviderReplacesStoredKeyAndLogsAuditEvent() throws IOException {
        when(aiApiService.getProviderDisplayName(anyString())).thenReturn("Google Gemini");
        when(aiApiService.hasNativeConnector(anyString())).thenReturn(true);
        when(aiApiService.isProviderConfigured(anyString())).thenReturn(true);
        when(providerRegistryService.getProvider("google-gemini")).thenReturn(provider);
        when(providerRegistryService.addOrUpdateProvider(any(APIProvider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = providerManagementService.rotateProvider(
            "google-gemini",
            Map.of("apiKey", "new-key", "reason", "security", "alertEmail", "admin@supremeai.com")
        );

        Map<?, ?> saved = (Map<?, ?>) response.get("provider");
        assertEquals("new-key", provider.getApiKey());
        assertEquals(Boolean.TRUE, response.get("success"));
        assertNotNull(saved);
        assertEquals("google-gemini", saved.get("id"));
        verify(providerAuditService).log(eq("ROTATE_KEY"), eq("google-gemini"), eq("admin@supremeai.com"), eq("SUCCESS"), anyMap());
        verify(quotaService).syncConfiguredProviders();
    }

    @Test
    void getConfiguredProvidersIncludesRealQuotaUsageFields() {
        when(aiApiService.getProviderDisplayName(anyString())).thenReturn("Google Gemini");
        when(aiApiService.hasNativeConnector(anyString())).thenReturn(true);
        when(aiApiService.isProviderConfigured(anyString())).thenReturn(true);
        Quota quota = new Quota("google-gemini", "Google Gemini", 100, 1000, 100);
        quota.setMonthlyLimit(1000);
        quota.setRequestsUsedThisMonth(125);

        when(providerRegistryService.getAllProviders()).thenReturn(List.of(provider));
        when(quotaService.getQuotaDetails("google-gemini")).thenReturn(quota);

        List<Map<String, Object>> configuredProviders = providerManagementService.getConfiguredProviders();

        assertEquals(1, configuredProviders.size());
        assertEquals(125L, configuredProviders.get(0).get("requestsUsedThisMonth"));
        assertEquals(875L, configuredProviders.get(0).get("remainingMonthlyRequests"));
    }

    @Test
    void probeProviderAllowsEndpointOnlyAirLlmConnector() throws IOException {
        APIProvider airllm = new APIProvider();
        airllm.setId("airllm-local");
        airllm.setName("AirLLM Local");
        airllm.setEndpoint("http://localhost:8081/v1/chat/completions");
        airllm.setHealthCheckUrl("http://localhost:8081/health");
        airllm.setCapabilities(List.of("complex-reasoning", "long-context", "analysis"));
        airllm.setComplexityTier("high");
        airllm.setCreatedByEmail("admin@supremeai.com");

        when(providerRegistryService.getProvider("airllm-local")).thenReturn(airllm);
        when(providerRegistryService.addOrUpdateProvider(any(APIProvider.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiApiService.requiresApiKey("airllm-local")).thenReturn(false);
        when(aiApiService.probeProviderConnection(eq("airllm-local"), isNull(), eq("http://localhost:8081/health")))
            .thenReturn("healthy");
        when(aiApiService.getProviderDisplayName(anyString())).thenReturn("AirLLM Local");
        when(aiApiService.hasNativeConnector(anyString())).thenReturn(true);
        when(aiApiService.isProviderConfigured(anyString())).thenReturn(false);

        Map<String, Object> result = providerManagementService.probeProvider("airllm-local");

        assertEquals(Boolean.TRUE, result.get("success"));
        verify(aiApiService).probeProviderConnection(eq("airllm-local"), isNull(), eq("http://localhost:8081/health"));
    }

    @Test
    void getConfiguredProvidersIncludesCapabilitiesAndHealthCheckFields() {
        provider.setCapabilities(List.of("complex-reasoning", "analysis"));
        provider.setComplexityTier("high");
        provider.setHealthCheckUrl("http://localhost:8081/health");

        when(aiApiService.getProviderDisplayName(anyString())).thenReturn("AirLLM Local");
        when(aiApiService.hasNativeConnector(anyString())).thenReturn(true);
        when(aiApiService.isProviderConfigured(anyString())).thenReturn(false);
        when(aiApiService.requiresApiKey(anyString())).thenReturn(false);
        when(providerRegistryService.getAllProviders()).thenReturn(List.of(provider));

        List<Map<String, Object>> configuredProviders = providerManagementService.getConfiguredProviders();

        assertEquals(List.of("complex-reasoning", "analysis"), configuredProviders.get(0).get("capabilities"));
        assertEquals("high", configuredProviders.get(0).get("complexityTier"));
        assertEquals("http://localhost:8081/health", configuredProviders.get(0).get("healthCheckUrl"));
    }

    @Test
    void removeProviderLogsAuditEvent() {
        when(providerRegistryService.removeProvider("google-gemini")).thenReturn(true);

        boolean removed = providerManagementService.removeProvider("google-gemini", "admin@supremeai.com");

        assertTrue(removed);
        verify(providerAuditService).log(eq("REMOVE_PROVIDER"), eq("google-gemini"), eq("admin@supremeai.com"), eq("SUCCESS"), anyMap());
    }
}