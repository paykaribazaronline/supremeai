package com.supremeai.service;

import com.supremeai.model.UserApi;
import com.supremeai.repository.UserApiRepository;
import com.supremeai.service.quota.ApiQuotaManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnifiedQuotaServiceTest {

    private UnifiedQuotaService unifiedQuotaService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter quotaCheckCounter;

    @Mock
    private Counter quotaIncrementCounter;

    @Mock
    private Counter quotaSpecialCounter;

    @Mock
    private UserApiRepository userApiRepository;

    @Mock
    private ApiQuotaManager quotaManager;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter("unified_quota_service.check")).thenReturn(quotaCheckCounter);
        when(meterRegistry.counter("unified_quota_service.increment")).thenReturn(quotaIncrementCounter);
        when(meterRegistry.counter("unified_quota_service.special")).thenReturn(quotaSpecialCounter);
        unifiedQuotaService = new UnifiedQuotaService(meterRegistry);
        // Manually inject the @Autowired fields
        try {
            var repoField = UnifiedQuotaService.class.getDeclaredField("userApiRepository");
            repoField.setAccessible(true);
            repoField.set(unifiedQuotaService, userApiRepository);

            var managerField = UnifiedQuotaService.class.getDeclaredField("quotaManager");
            managerField.setAccessible(true);
            managerField.set(unifiedQuotaService, quotaManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void checkAndIncrement_ShouldReturnFalse_WhenApiKeyNotFound() {
        // GIVEN
        String apiKey = "invalidKey";
        String type = "USER";
        when(userApiRepository.findByApiKey(apiKey)).thenReturn(Mono.empty());

        // WHEN
        boolean result = unifiedQuotaService.checkAndIncrement(apiKey, type);

        // THEN
        assertFalse(result);
        verify(userApiRepository).findByApiKey(apiKey);
        verifyNoInteractions(quotaManager);
    }

    @Test
    void checkAndIncrement_ShouldReturnFalse_WhenApiInactive() {
        // GIVEN
        String apiKey = "inactiveKey";
        String type = "USER";
        UserApi inactiveApi = new UserApi();
        inactiveApi.setIsActive(false);
        when(userApiRepository.findByApiKey(apiKey)).thenReturn(Mono.just(inactiveApi));

        // WHEN
        boolean result = unifiedQuotaService.checkAndIncrement(apiKey, type);

        // THEN
        assertFalse(result);
        verify(userApiRepository).findByApiKey(apiKey);
        verifyNoInteractions(quotaManager);
    }

    @Test
    void checkAndIncrement_ShouldReturnTrue_WhenQuotaIncremented() {
        // GIVEN
        String apiKey = "validKey";
        String type = "USER";
        UserApi activeApi = new UserApi();
        activeApi.setIsActive(true);
        when(userApiRepository.findByApiKey(apiKey)).thenReturn(Mono.just(activeApi));
        when(quotaManager.incrementUsage(activeApi)).thenReturn(true);
        when(userApiRepository.save(activeApi)).thenReturn(Mono.just(activeApi));

        // WHEN
        boolean result = unifiedQuotaService.checkAndIncrement(apiKey, type);

        // THEN
        assertTrue(result);
        verify(userApiRepository).findByApiKey(apiKey);
        verify(quotaManager).incrementUsage(activeApi);
        verify(userApiRepository).save(activeApi);
    }

    @Test
    void checkAndIncrement_ShouldReturnFalse_WhenQuotaNotIncremented() {
        // GIVEN
        String apiKey = "validKey";
        String type = "USER";
        UserApi activeApi = new UserApi();
        activeApi.setIsActive(true);
        when(userApiRepository.findByApiKey(apiKey)).thenReturn(Mono.just(activeApi));
        when(quotaManager.incrementUsage(activeApi)).thenReturn(false);

        // WHEN
        boolean result = unifiedQuotaService.checkAndIncrement(apiKey, type);

        // THEN
        assertFalse(result);
        verify(userApiRepository).findByApiKey(apiKey);
        verify(quotaManager).incrementUsage(activeApi);
        verify(userApiRepository, never()).save(any());
    }

    @Test
    void handleSpecialQuota_ShouldReturnTrue() {
        // GIVEN
        String entityId = "testId";
        String type = "SIMULATOR";

        // WHEN
        boolean result = unifiedQuotaService.handleSpecialQuota(entityId, type);

        // THEN
        assertTrue(result);
    }

    @Test
    void resetAll_ShouldNotThrowException() {
        // WHEN & THEN
        assertDoesNotThrow(() -> unifiedQuotaService.resetAll());
    }
}