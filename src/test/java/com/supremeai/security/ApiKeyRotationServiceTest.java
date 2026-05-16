package com.supremeai.service;

import com.supremeai.model.APIHealthReport;
import com.supremeai.model.UserApiKey;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.security.ApiKeyRotationService;
import com.supremeai.security.EncryptionService;
import com.supremeai.repository.APIHealthReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
class ApiKeyRotationServiceTest {

    @Mock
    private UserApiKeyRepository userApiKeyRepository;

    @Mock
    private APIHealthReportRepository healthReportRepository;

    @Mock
    private EncryptionService encryptionService;

    private ApiKeyRotationService rotationService;

    @BeforeEach
    void setUp() {
        rotationService = new ApiKeyRotationService();
        // Inject mocks manually since @Autowired is used in the class
        setField(rotationService, "userApiKeyRepository", userApiKeyRepository);
        setField(rotationService, "healthReportRepository", healthReportRepository);
        setField(rotationService, "encryptionService", encryptionService);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = ApiKeyRotationService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== getDecryptedApiKey Tests ====================

    @Test
    void getDecryptedApiKey_ValidKey_ReturnsDecrypted() {
        UserApiKey key = new UserApiKey();
        key.setApiKey("encrypted-key-123");

        when(encryptionService.decrypt("encrypted-key-123")).thenReturn("decrypted-key");

        String result = rotationService.getDecryptedApiKey(key);

        assertEquals("decrypted-key", result);
        verify(encryptionService).decrypt("encrypted-key-123");
    }

    @Test
    void getDecryptedApiKey_NullKey_ReturnsNull() {
        String result = rotationService.getDecryptedApiKey(null);
        assertNull(result);
    }

    @Test
    void getDecryptedApiKey_KeyWithNullApiKey_ReturnsNull() {
        UserApiKey key = new UserApiKey();
        key.setApiKey(null);

        String result = rotationService.getDecryptedApiKey(key);
        assertNull(result);
    }

    // ==================== getRotationDaysForKey Tests ====================

    @Test
    void getRotationDaysForKey_KnownProvider_ReturnsConfiguredDays() {
        int days = rotationService.getRotationDaysForKey("openai");
        assertEquals(90, days);
    }

    @Test
    void getRotationDaysForKey_UnknownProvider_ReturnsDefault() {
        int days = rotationService.getRotationDaysForKey("unknown-provider");
        assertEquals(90, days);
    }

    @Test
    void getRotationDaysForKey_Null_ReturnsDefault() {
        int days = rotationService.getRotationDaysForKey(null);
        assertEquals(90, days);
    }

    // ==================== getMaxKeysPerProvider Tests ====================

    @Test
    void getMaxKeysPerProvider_KnownProvider_ReturnsConfiguredMax() {
        int max = rotationService.getMaxKeysPerProvider("openai");
        assertEquals(5, max);
    }

    @Test
    void getMaxKeysPerProvider_UnknownProvider_ReturnsDefault() {
        int max = rotationService.getMaxKeysPerProvider("unknown");
        assertEquals(5, max);
    }

    // ==================== testApiKey Tests ====================

    @Test
    void testApiKey_UnknownProvider_ReturnsAssumedValid() {
        UserApiKey key = new UserApiKey();
        key.setId("key-1");
        key.setProvider("unknown-provider");
        key.setApiKey("some-key");

        when(encryptionService.decrypt("some-key")).thenReturn("decrypted");

        Map<String, Object> result = rotationService.testApiKey(key);

        assertEquals("key-1", result.get("id"));
        assertEquals("unknown-provider", result.get("provider"));
        assertTrue((Boolean) result.get("valid"));
        assertEquals("Unknown provider - cannot validate automatically", result.get("message"));
    }

    @Test
    void testApiKey_KnownProvider_DecryptsAndTests() {
        UserApiKey key = new UserApiKey();
        key.setId("key-2");
        key.setProvider("openai");
        key.setApiKey("encrypted-openai-key");

        when(encryptionService.decrypt("encrypted-openai-key")).thenReturn("sk-openai-test");

        Map<String, Object> result = rotationService.testApiKey(key);

        assertNotNull(result);
        assertEquals("key-2", result.get("id"));
        verify(encryptionService).decrypt("encrypted-openai-key");
    }

    // ==================== selectBestKey Tests ====================

    @Test
    void selectBestKey_ActiveKeysExist_ReturnsBestKey() {
        UserApiKey key1 = createKey("key-1", "openai", "active", 5L);
        UserApiKey key2 = createKey("key-2", "openai", "active", 2L);
        UserApiKey key3 = createKey("key-3", "openai", "active", 10L);

        when(userApiKeyRepository.findByUserIdAndStatus("user-1", "active"))
                .thenReturn(Flux.just(key1, key2, key3));

        Mono<UserApiKey> result = rotationService.selectBestKey("user-1", "openai");

        StepVerifier.create(result)
                .expectNextMatches(k -> "key-2".equals(k.getId()))
                .verifyComplete();
    }

    @Test
    void selectBestKey_NoActiveKeys_ReturnsEmpty() {
        when(userApiKeyRepository.findByUserIdAndStatus("user-1", "active"))
                .thenReturn(Flux.empty());

        Mono<UserApiKey> result = rotationService.selectBestKey("user-1", "openai");

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void selectBestKey_NoMatchingProvider_ReturnsEmpty() {
        UserApiKey key = createKey("key-1", "anthropic", "active", 1L);
        when(userApiKeyRepository.findByUserIdAndStatus("user-1", "active"))
                .thenReturn(Flux.just(key));

        Mono<UserApiKey> result = rotationService.selectBestKey("user-1", "openai");

        StepVerifier.create(result)
                .verifyComplete();
    }

    // ==================== getRotationStatus Tests ====================

    @Test
    void getRotationStatus_HasKeys_ReturnsSummary() {
        UserApiKey key1 = createKey("key-1", "openai", "active", 5L);
        UserApiKey key2 = createKey("key-2", "openai", "rotation_due", 3L);
        UserApiKey key3 = createKey("key-3", "openai", "error", 1L);

        when(userApiKeyRepository.findByUserId("user-1")).thenReturn(Flux.just(key1, key2, key3));

        Mono<Map<String, Object>> result = rotationService.getRotationStatus("user-1");

        StepVerifier.create(result)
                .expectNextMatches(summary -> {
                    assertEquals(3, summary.get("totalKeys"));
                    assertEquals(1L, summary.get("active"));
                    assertEquals(1L, summary.get("rotationDue"));
                    assertEquals(1L, summary.get("error"));
                    assertNotNull(summary.get("providerConfigs"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getRotationStatus_NoKeys_ReturnsEmptySummary() {
        when(userApiKeyRepository.findByUserId("user-1")).thenReturn(Flux.empty());

        Mono<Map<String, Object>> result = rotationService.getRotationStatus("user-1");

        StepVerifier.create(result)
                .expectNextMatches(summary -> {
                    assertEquals(0, summary.get("totalKeys"));
                    return true;
                })
                .verifyComplete();
    }

    // ==================== testAllKeysNow Tests ====================

    @Test
    void testAllKeysNow_NoActiveKeys_ReturnsEmpty() {
        when(userApiKeyRepository.findAll()).thenReturn(Flux.empty());

        Mono<Void> result = rotationService.testAllKeysNow();

        StepVerifier.create(result)
                .verifyComplete();

        verify(userApiKeyRepository).findAll();
    }

    @Test
    void testAllKeysNow_ActiveKeysValidated() {
        UserApiKey key1 = createKey("key-1", "openai", "active", 5L);
        UserApiKey key2 = createKey("key-2", "openai", "active", 3L);

        when(userApiKeyRepository.findAll()).thenReturn(Flux.just(key1, key2));
        when(encryptionService.decrypt(anyString())).thenReturn("decrypted-key");
        when(userApiKeyRepository.save(any(UserApiKey.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(healthReportRepository.save(any(APIHealthReport.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<Void> result = rotationService.testAllKeysNow();

        StepVerifier.create(result)
                .verifyComplete();

        verify(userApiKeyRepository, atLeastOnce()).save(any(UserApiKey.class));
        verify(healthReportRepository).save(any(APIHealthReport.class));
    }

    // ==================== Helper ====================

    private UserApiKey createKey(String id, String provider, String status, Long requestCount) {
        UserApiKey key = new UserApiKey();
        key.setId(id);
        key.setProvider(provider);
        key.setStatus(status);
        key.setRequestCount(requestCount);
        key.setAddedAt(LocalDateTime.now().minusDays(30));
        key.setLastTested(LocalDateTime.now());
        return key;
    }
}