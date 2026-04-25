
package com.supremeai.service;

import com.supremeai.model.UserApiKey;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.cost.QuotaManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuotaServiceTest {

    @Mock
    private UserApiKeyRepository userApiKeyRepository;

    @Mock
    private QuotaManager quotaManager;

    @InjectMocks
    private QuotaService quotaService;

    private UserApiKey activeApiKey;
    private UserApiKey inactiveApiKey;
    private UserApiKey exhaustedApiKey;

    @BeforeEach
    public void setUp() {
        activeApiKey = new UserApiKey("user1", "OpenAI", "Test Key", "encrypted-key-1");
        activeApiKey.setId("key1");
        activeApiKey.setStatus("active");
        activeApiKey.setRequestCount(500L);

        inactiveApiKey = new UserApiKey("user1", "Google AI", "Inactive Key", "encrypted-key-2");
        inactiveApiKey.setId("key2");
        inactiveApiKey.setStatus("inactive");
        inactiveApiKey.setRequestCount(100L);

        exhaustedApiKey = new UserApiKey("user2", "Anthropic", "Exhausted Key", "encrypted-key-3");
        exhaustedApiKey.setId("key3");
        exhaustedApiKey.setStatus("active");
        exhaustedApiKey.setRequestCount(1000L);
    }

    @Test
    public void testHasQuotaRemaining_ActiveKeyWithQuota() {
        when(userApiKeyRepository.findByApiKey("valid-key")).thenReturn(Mono.just(activeApiKey));

        boolean result = quotaService.hasQuotaRemaining("valid-key");

        assertTrue(result);
        verify(userApiKeyRepository).findByApiKey("valid-key");
    }

    @Test
    public void testHasQuotaRemaining_InactiveKey() {
        when(userApiKeyRepository.findByApiKey("inactive-key")).thenReturn(Mono.just(inactiveApiKey));

        boolean result = quotaService.hasQuotaRemaining("inactive-key");

        assertFalse(result);
        verify(userApiKeyRepository).findByApiKey("inactive-key");
    }

    @Test
    public void testHasQuotaRemaining_ExhaustedKey() {
        when(userApiKeyRepository.findByApiKey("exhausted-key")).thenReturn(Mono.just(exhaustedApiKey));

        boolean result = quotaService.hasQuotaRemaining("exhausted-key");

        assertFalse(result);
        verify(userApiKeyRepository).findByApiKey("exhausted-key");
    }

    @Test
    public void testHasQuotaRemaining_KeyNotFound() {
        when(userApiKeyRepository.findByApiKey("nonexistent-key")).thenReturn(Mono.empty());

        boolean result = quotaService.hasQuotaRemaining("nonexistent-key");

        assertFalse(result);
        verify(userApiKeyRepository).findByApiKey("nonexistent-key");
    }

    @Test
    public void testIncrementUsage_Success() {
        activeApiKey.setRequestCount(500L);
        when(userApiKeyRepository.findByApiKey("valid-key")).thenReturn(Mono.just(activeApiKey));
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenReturn(Mono.just(activeApiKey));

        boolean result = quotaService.incrementUsage("valid-key");

        assertTrue(result);
        assertEquals(501L, activeApiKey.getRequestCount());
        verify(userApiKeyRepository).findByApiKey("valid-key");
        verify(userApiKeyRepository).save(any(UserApiKey.class));
    }

    @Test
    public void testIncrementUsage_QuotaExceeded() {
        when(userApiKeyRepository.findByApiKey("exhausted-key")).thenReturn(Mono.just(exhaustedApiKey));

        boolean result = quotaService.incrementUsage("exhausted-key");

        assertFalse(result);
        verify(userApiKeyRepository).findByApiKey("exhausted-key");
        verify(userApiKeyRepository, never()).save(any(UserApiKey.class));
    }

    @Test
    public void testIncrementUsage_InactiveKey() {
        when(userApiKeyRepository.findByApiKey("inactive-key")).thenReturn(Mono.just(inactiveApiKey));

        boolean result = quotaService.incrementUsage("inactive-key");

        assertFalse(result);
        verify(userApiKeyRepository).findByApiKey("inactive-key");
        verify(userApiKeyRepository, never()).save(any(UserApiKey.class));
    }

    @Test
    public void testIncrementUsage_KeyNotFound() {
        when(userApiKeyRepository.findByApiKey("nonexistent-key")).thenReturn(Mono.empty());

        boolean result = quotaService.incrementUsage("nonexistent-key");

        assertFalse(result);
        verify(userApiKeyRepository).findByApiKey("nonexistent-key");
        verify(userApiKeyRepository, never()).save(any(UserApiKey.class));
    }

    @Test
    public void testValidateAndIncrement_Success() {
        activeApiKey.setRequestCount(500L);
        when(userApiKeyRepository.findByApiKey("valid-key")).thenReturn(Mono.just(activeApiKey));
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenReturn(Mono.just(activeApiKey));

        assertDoesNotThrow(() -> quotaService.validateAndIncrement("valid-key"));
        assertEquals(501L, activeApiKey.getRequestCount());
        verify(userApiKeyRepository).findByApiKey("valid-key");
        verify(userApiKeyRepository).save(any(UserApiKey.class));
    }

    @Test
    public void testValidateAndIncrement_InvalidKey() {
        when(userApiKeyRepository.findByApiKey("invalid-key")).thenReturn(Mono.empty());

        assertThrows(IllegalArgumentException.class, () -> quotaService.validateAndIncrement("invalid-key"));
        verify(userApiKeyRepository).findByApiKey("invalid-key");
    }

    @Test
    public void testValidateAndIncrement_InactiveKey() {
        when(userApiKeyRepository.findByApiKey("inactive-key")).thenReturn(Mono.just(inactiveApiKey));

        assertThrows(IllegalArgumentException.class, () -> quotaService.validateAndIncrement("inactive-key"));
        verify(userApiKeyRepository).findByApiKey("inactive-key");
    }

    @Test
    public void testValidateAndIncrement_QuotaExceeded() {
        when(userApiKeyRepository.findByApiKey("exhausted-key")).thenReturn(Mono.just(exhaustedApiKey));

        assertThrows(RuntimeException.class, () -> quotaService.validateAndIncrement("exhausted-key"));
        verify(userApiKeyRepository).findByApiKey("exhausted-key");
    }

    @Test
    public void testGetCurrentUsage_Success() {
        when(userApiKeyRepository.findByApiKey("valid-key")).thenReturn(Mono.just(activeApiKey));

        Long result = quotaService.getCurrentUsage("valid-key");

        assertEquals(500L, result);
        verify(userApiKeyRepository).findByApiKey("valid-key");
    }

    @Test
    public void testGetCurrentUsage_KeyNotFound() {
        when(userApiKeyRepository.findByApiKey("nonexistent-key")).thenReturn(Mono.empty());

        Long result = quotaService.getCurrentUsage("nonexistent-key");

        assertEquals(0L, result);
        verify(userApiKeyRepository).findByApiKey("nonexistent-key");
    }

    @Test
    public void testGetMonthlyQuota() {
        Long result = quotaService.getMonthlyQuota("any-key");

        assertEquals(1000L, result);
    }

    @Test
    public void testResetApiUsage_Success() {
        when(userApiKeyRepository.findByApiKey("valid-key")).thenReturn(Mono.just(activeApiKey));
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenReturn(Mono.just(activeApiKey));

        boolean result = quotaService.resetApiUsage("valid-key");

        assertTrue(result);
        assertEquals(0L, activeApiKey.getRequestCount());
        verify(userApiKeyRepository).findByApiKey("valid-key");
        verify(userApiKeyRepository).save(any(UserApiKey.class));
    }

    @Test
    public void testResetApiUsage_KeyNotFound() {
        when(userApiKeyRepository.findByApiKey("nonexistent-key")).thenReturn(Mono.empty());

        boolean result = quotaService.resetApiUsage("nonexistent-key");

        assertFalse(result);
        verify(userApiKeyRepository).findByApiKey("nonexistent-key");
        verify(userApiKeyRepository, never()).save(any(UserApiKey.class));
    }

    @Test
    public void testGetUsageStats_Success() {
        activeApiKey.setRequestCount(500L);
        activeApiKey.setLastUsed(LocalDateTime.now());
        when(userApiKeyRepository.findByApiKey("valid-key")).thenReturn(Mono.just(activeApiKey));

        QuotaService.ApiUsageStats result = quotaService.getUsageStats("valid-key");

        assertNotNull(result);
        assertEquals(500L, result.getCurrentUsage());
        assertEquals(1000L, result.getMonthlyQuota());
        assertTrue(result.isHasQuotaRemaining());
        verify(userApiKeyRepository).findByApiKey("valid-key");
    }

    @Test
    public void testGetUsageStats_KeyNotFound() {
        when(userApiKeyRepository.findByApiKey("nonexistent-key")).thenReturn(Mono.empty());

        QuotaService.ApiUsageStats result = quotaService.getUsageStats("nonexistent-key");

        assertNull(result);
        verify(userApiKeyRepository).findByApiKey("nonexistent-key");
    }

    @Test
    public void testGetUsageStats_QuotaExhausted() {
        exhaustedApiKey.setRequestCount(1000L);
        when(userApiKeyRepository.findByApiKey("exhausted-key")).thenReturn(Mono.just(exhaustedApiKey));

        QuotaService.ApiUsageStats result = quotaService.getUsageStats("exhausted-key");

        assertNotNull(result);
        assertEquals(1000L, result.getCurrentUsage());
        assertFalse(result.isHasQuotaRemaining());
        verify(userApiKeyRepository).findByApiKey("exhausted-key");
    }
}
