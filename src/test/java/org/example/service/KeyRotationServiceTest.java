package org.example.service;

import org.example.security.SecretManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyRotationServiceTest {

    @Mock
    private SecretManager secretManager;

    @Mock
    private AlertingService alertingService;

    @Mock
    private AIAPIService aiApiService;

    @Test
    void forceRotationUpdatesSecretAndRuntimeKey() {
        KeyRotationService service = new KeyRotationService();
        ReflectionTestUtils.setField(service, "secretManager", secretManager);
        ReflectionTestUtils.setField(service, "alertingService", alertingService);
        ReflectionTestUtils.setField(service, "aiApiService", aiApiService);
        ReflectionTestUtils.setField(service, "gracePeriodHours", 24);

        ReflectionTestUtils.invokeMethod(service, "initializeProviderHandlers");

        when(secretManager.getSecret("GROQ_API_KEY")).thenReturn("old-groq-key");

        KeyRotationService.RotationResult result = service.forceRotation("GROQ");

        assertTrue(result.isSuccess());
        verify(secretManager).updateSecret(eq("GROQ_API_KEY"), anyString());
        verify(secretManager).invalidateCache("GROQ_API_KEY");
        verify(aiApiService).updateApiKey(eq("GROQ"), anyString());
    }

    @Test
    void forceRotationFailsWhenNoExistingKeyFound() {
        KeyRotationService service = new KeyRotationService();
        ReflectionTestUtils.setField(service, "secretManager", secretManager);
        ReflectionTestUtils.setField(service, "alertingService", alertingService);
        ReflectionTestUtils.setField(service, "aiApiService", aiApiService);

        ReflectionTestUtils.invokeMethod(service, "initializeProviderHandlers");

        when(secretManager.getSecret("GROQ_API_KEY")).thenReturn("");

        KeyRotationService.RotationResult result = service.forceRotation("GROQ");

        assertFalse(result.isSuccess());
        assertEquals("No existing key found", result.getError());
        verify(secretManager, never()).updateSecret(eq("GROQ_API_KEY"), anyString());
    }

    @Test
    void rotateAllKeysReturnsSummaryCounts() {
        KeyRotationService service = new KeyRotationService();
        ReflectionTestUtils.setField(service, "secretManager", secretManager);
        ReflectionTestUtils.setField(service, "alertingService", alertingService);
        ReflectionTestUtils.setField(service, "aiApiService", aiApiService);
        ReflectionTestUtils.setField(service, "gracePeriodHours", 24);

        ReflectionTestUtils.invokeMethod(service, "initializeProviderHandlers");

        when(secretManager.getSecret(anyString())).thenReturn("existing-key");

        KeyRotationService.RotationSummary summary = service.rotateAllKeys();

        assertTrue(summary.getTotal() >= 8);
        assertEquals(summary.getTotal(), summary.getSuccess() + summary.getFailed());
    }

    @Test
    void statisticsExposesSupportedProviders() {
        KeyRotationService service = new KeyRotationService();
        ReflectionTestUtils.setField(service, "secretManager", secretManager);
        ReflectionTestUtils.setField(service, "alertingService", alertingService);
        ReflectionTestUtils.setField(service, "aiApiService", aiApiService);

        ReflectionTestUtils.invokeMethod(service, "initializeProviderHandlers");

        Map<String, Object> stats = service.getStatistics();

        @SuppressWarnings("unchecked")
        java.util.Set<String> supported = (java.util.Set<String>) stats.get("supportedProviders");
        assertTrue(supported.contains("GROQ"));
        assertTrue(supported.contains("AWS_BEDROCK"));
        assertTrue(supported.contains("AZURE_OPENAI"));
        assertTrue(supported.contains("GCP_VERTEX_AI"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recent = (List<Map<String, Object>>) stats.get("recentRotations");
        assertEquals(0, recent.size());
    }

    @ParameterizedTest
    @MethodSource("cloudProviders")
    void forceRotationSupportsCloudProviders(String provider, String secretName) {
        KeyRotationService service = new KeyRotationService();
        ReflectionTestUtils.setField(service, "secretManager", secretManager);
        ReflectionTestUtils.setField(service, "alertingService", alertingService);
        ReflectionTestUtils.setField(service, "aiApiService", aiApiService);
        ReflectionTestUtils.setField(service, "gracePeriodHours", 24);

        ReflectionTestUtils.invokeMethod(service, "initializeProviderHandlers");

        when(secretManager.getSecret(secretName)).thenReturn("existing-key");

        KeyRotationService.RotationResult result = service.forceRotation(provider);

        assertTrue(result.isSuccess());
        verify(secretManager).updateSecret(eq(secretName), anyString());
        verify(secretManager).invalidateCache(secretName);
        verify(aiApiService).updateApiKey(eq(provider), anyString());
    }

    private static Stream<Arguments> cloudProviders() {
        return Stream.of(
            Arguments.of("AWS_BEDROCK", "AWS_BEDROCK_API_KEY"),
            Arguments.of("AZURE_OPENAI", "AZURE_OPENAI_API_KEY"),
            Arguments.of("GCP_VERTEX_AI", "GCP_VERTEX_AI_KEY")
        );
    }
}
