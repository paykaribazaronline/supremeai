package com.supremeai.service;

import com.supremeai.model.UserApi;
import com.supremeai.model.UserTier;
import com.supremeai.repository.UserApiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuotaServiceTest {

    @Mock
    private UserApiRepository userApiRepository;

    @InjectMocks
    private QuotaService quotaService;

    private UserApi testApi;
    private final String API_KEY = "test-api-key-123";

    @BeforeEach
    void setUp() {
        testApi = new UserApi("user-1", "Test API", API_KEY, "Description", UserTier.FREE, 100L);
        testApi.setIsActive(true);
        testApi.setCurrentUsage(10L);
    }

    @Test
    void testHasQuotaRemaining_Success() {
        when(userApiRepository.findByApiKey(API_KEY)).thenReturn(Optional.of(testApi));

        boolean hasQuota = quotaService.hasQuotaRemaining(API_KEY);

        assertTrue(hasQuota);
        verify(userApiRepository).findByApiKey(API_KEY);
    }

    @Test
    void testHasQuotaRemaining_Exceeded() {
        testApi.setCurrentUsage(100L); // Limit reached
        when(userApiRepository.findByApiKey(API_KEY)).thenReturn(Optional.of(testApi));

        boolean hasQuota = quotaService.hasQuotaRemaining(API_KEY);

        assertFalse(hasQuota);
    }

    @Test
    void testHasQuotaRemaining_InactiveApi() {
        testApi.setIsActive(false);
        when(userApiRepository.findByApiKey(API_KEY)).thenReturn(Optional.of(testApi));

        boolean hasQuota = quotaService.hasQuotaRemaining(API_KEY);

        assertFalse(hasQuota);
    }

    @Test
    void testIncrementUsage_Success() {
        when(userApiRepository.findByApiKey(API_KEY)).thenReturn(Optional.of(testApi));

        boolean result = quotaService.incrementUsage(API_KEY);

        assertTrue(result);
        assertEquals(11L, testApi.getCurrentUsage());
        verify(userApiRepository).save(testApi);
    }

    @Test
    void testIncrementUsage_Fail_WhenNoQuota() {
        testApi.setCurrentUsage(100L);
        when(userApiRepository.findByApiKey(API_KEY)).thenReturn(Optional.of(testApi));

        boolean result = quotaService.incrementUsage(API_KEY);

        assertFalse(result);
        assertEquals(100L, testApi.getCurrentUsage());
        verify(userApiRepository, never()).save(any());
    }

    @Test
    void testGetUsageStats_ValidKey() {
        when(userApiRepository.findByApiKey(API_KEY)).thenReturn(Optional.of(testApi));

        QuotaService.ApiUsageStats stats = quotaService.getUsageStats(API_KEY);

        assertNotNull(stats);
        assertEquals(10L, stats.getCurrentUsage());
        assertEquals(100L, stats.getMonthlyQuota());
        assertEquals(10.0, stats.getUsagePercentage());
    }

    @Test
    void testGetUsageStats_InvalidKey() {
        when(userApiRepository.findByApiKey(anyString())).thenReturn(Optional.empty());

        QuotaService.ApiUsageStats stats = quotaService.getUsageStats("invalid-key");

        assertNull(stats);
    }
}
