
package com.supremeai.service;

import com.supremeai.cost.QuotaDefinition;
import com.supremeai.cost.QuotaManager;
import com.supremeai.cost.QuotaPeriod;
import com.supremeai.model.User;
import com.supremeai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuotaServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuotaManager quotaManager;

    @InjectMocks
    private QuotaService quotaService;

    private User testUser;

    @BeforeEach
    public void setUp() {
        testUser = new User("test-uid", "test@example.com", "Test User");
        testUser.setTier(com.supremeai.model.UserTier.FREE);
    }

    @Test
    public void testCheckQuota_Success() {
        // Arrange
        QuotaDefinition quotaDefinition = new QuotaDefinition();
        quotaDefinition.setMaxRequests(100);
        quotaDefinition.setPeriod(QuotaPeriod.DAILY);
        quotaDefinition.setCurrentRequests(50);

        when(quotaManager.getUserQuota(anyString())).thenReturn(quotaDefinition);
        when(userRepository.findByFirebaseUid(anyString())).thenReturn(Mono.just(testUser));

        // Act
        boolean result = quotaService.checkQuota("test-uid").block();

        // Assert
        assertTrue(result);
        verify(quotaManager).getUserQuota(anyString());
    }

    @Test
    public void testCheckQuota_Exceeded() {
        // Arrange
        QuotaDefinition quotaDefinition = new QuotaDefinition();
        quotaDefinition.setMaxRequests(100);
        quotaDefinition.setPeriod(QuotaPeriod.DAILY);
        quotaDefinition.setCurrentRequests(100);

        when(quotaManager.getUserQuota(anyString())).thenReturn(quotaDefinition);
        when(userRepository.findByFirebaseUid(anyString())).thenReturn(Mono.just(testUser));

        // Act
        boolean result = quotaService.checkQuota("test-uid").block();

        // Assert
        assertFalse(result);
        verify(quotaManager).getUserQuota(anyString());
    }

    @Test
    public void testIncrementQuota_Success() {
        // Arrange
        QuotaDefinition quotaDefinition = new QuotaDefinition();
        quotaDefinition.setMaxRequests(100);
        quotaDefinition.setPeriod(QuotaPeriod.DAILY);
        quotaDefinition.setCurrentRequests(50);

        when(quotaManager.getUserQuota(anyString())).thenReturn(quotaDefinition);
        when(quotaManager.incrementUserQuota(anyString())).thenReturn(Mono.just(quotaDefinition));

        // Act
        QuotaDefinition result = quotaService.incrementQuota("test-uid").block();

        // Assert
        assertNotNull(result);
        assertEquals(51, result.getCurrentRequests());
        verify(quotaManager).incrementUserQuota(anyString());
    }

    @Test
    public void testResetQuota_Success() {
        // Arrange
        QuotaDefinition quotaDefinition = new QuotaDefinition();
        quotaDefinition.setMaxRequests(100);
        quotaDefinition.setPeriod(QuotaPeriod.DAILY);
        quotaDefinition.setCurrentRequests(100);

        when(quotaManager.getUserQuota(anyString())).thenReturn(quotaDefinition);
        when(quotaManager.resetUserQuota(anyString())).thenReturn(Mono.just(quotaDefinition));

        // Act
        QuotaDefinition result = quotaService.resetQuota("test-uid").block();

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getCurrentRequests());
        verify(quotaManager).resetUserQuota(anyString());
    }

    @Test
    public void testGetQuotaInfo_Success() {
        // Arrange
        QuotaDefinition quotaDefinition = new QuotaDefinition();
        quotaDefinition.setMaxRequests(100);
        quotaDefinition.setPeriod(QuotaPeriod.DAILY);
        quotaDefinition.setCurrentRequests(50);
        quotaDefinition.setLastReset(LocalDateTime.now());

        when(quotaManager.getUserQuota(anyString())).thenReturn(quotaDefinition);

        // Act
        Map<String, Object> result = quotaService.getQuotaInfo("test-uid").block();

        // Assert
        assertNotNull(result);
        assertEquals(100, result.get("maxRequests"));
        assertEquals(50, result.get("currentRequests"));
        assertEquals("DAILY", result.get("period"));
        verify(quotaManager).getUserQuota(anyString());
    }

    @Test
    public void testCheckQuotaForUser_NotFound() {
        // Arrange
        when(userRepository.findByFirebaseUid(anyString())).thenReturn(Mono.empty());

        // Act
        boolean result = quotaService.checkQuota("nonexistent").block();

        // Assert
        assertFalse(result);
        verify(userRepository).findByFirebaseUid(anyString());
    }

    @Test
    public void testUpdateQuotaLimit_Success() {
        // Arrange
        QuotaDefinition quotaDefinition = new QuotaDefinition();
        quotaDefinition.setMaxRequests(100);
        quotaDefinition.setPeriod(QuotaPeriod.DAILY);
        quotaDefinition.setCurrentRequests(50);

        when(quotaManager.getUserQuota(anyString())).thenReturn(quotaDefinition);
        when(quotaManager.updateQuotaLimit(anyString(), anyInt())).thenReturn(Mono.just(quotaDefinition));

        // Act
        QuotaDefinition result = quotaService.updateQuotaLimit("test-uid", 200).block();

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getMaxRequests());
        verify(quotaManager).updateQuotaLimit(anyString(), anyInt());
    }

    @Test
    public void testGetQuotaUsageStats_Success() {
        // Arrange
        Map<String, Object> usageStats = new HashMap<>();
        usageStats.put("totalRequests", 1000);
        usageStats.put("successfulRequests", 950);
        usageStats.put("failedRequests", 50);
        usageStats.put("averageResponseTime", 250);

        when(quotaManager.getQuotaUsageStats(anyString())).thenReturn(usageStats);

        // Act
        Map<String, Object> result = quotaService.getQuotaUsageStats("test-uid").block();

        // Assert
        assertNotNull(result);
        assertEquals(1000, result.get("totalRequests"));
        assertEquals(950, result.get("successfulRequests"));
        assertEquals(50, result.get("failedRequests"));
        assertEquals(250, result.get("averageResponseTime"));
        verify(quotaManager).getQuotaUsageStats(anyString());
    }
}
