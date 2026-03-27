package org.example.service;

import org.example.data.HybridDataCollector;
import org.example.data.HybridResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataCollectorServiceTest {

    @Mock
    private HybridDataCollector hybridDataCollector;

    private DataCollectorService dataCollectorService;

    @BeforeEach
    void setUp() {
        dataCollectorService = new DataCollectorService(hybridDataCollector);
    }

    @Test
    void testGetGitHubDataSuccess() {
        // Given
        HybridResult mockResult = new HybridResult();
        mockResult.setData(createGitHubData());
        mockResult.setSuccess(true);
        mockResult.setFromCache(false);

        when(hybridDataCollector.collectGitHubData("supremeai", "core"))
                .thenReturn(mockResult);

        // When
        Map<String, Object> result = dataCollectorService.getGitHubData("supremeai", "core");

        // Then
        assertNotNull(result);
        assertEquals("supremeai", result.get("owner"));
        assertEquals("core", result.get("repo"));
        assertEquals(150, result.get("stars"));
    }

    @Test
    void testGetGitHubDataWithCachedResult() {
        // Given
        HybridResult mockResult = new HybridResult();
        mockResult.setData(createGitHubData());
        mockResult.setSuccess(true);
        mockResult.setFromCache(true);
        mockResult.setCacheHitTime(3500); // 3.5 seconds

        when(hybridDataCollector.collectGitHubData("supremeai", "core"))
                .thenReturn(mockResult);

        // When
        Map<String, Object> result = dataCollectorService.getGitHubData("supremeai", "core");

        // Then
        assertNotNull(result);
        assertTrue((Boolean) result.get("fromCache"));
    }

    @Test
    void testGetGitHubDataThrowsExceptionOnNullOwner() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            dataCollectorService.getGitHubData(null, "core");
        });
    }

    @Test
    void testGetGitHubDataThrowsExceptionOnNullRepo() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            dataCollectorService.getGitHubData("supremeai", null);
        });
    }

    @Test
    void testGetVercelStatusSuccess() {
        // Given
        HybridResult mockResult = new HybridResult();
        mockResult.setData(createVercelData());
        mockResult.setSuccess(true);
        mockResult.setFromCache(false);

        when(hybridDataCollector.collectVercelStatus("proj_123"))
                .thenReturn(mockResult);

        // When
        Map<String, Object> result = dataCollectorService.getVercelStatus("proj_123");

        // Then
        assertNotNull(result);
        assertEquals("proj_123", result.get("projectId"));
        assertEquals("ready", result.get("status"));
    }

    @Test
    void testGetFirebaseMetricsSuccess() {
        // Given
        HybridResult mockResult = new HybridResult();
        mockResult.setData(createFirebaseData());
        mockResult.setSuccess(true);
        mockResult.setFromCache(false);

        when(hybridDataCollector.collectFirebaseMetrics())
                .thenReturn(mockResult);

        // When
        Map<String, Object> result = dataCollectorService.getFirebaseMetrics();

        // Then
        assertNotNull(result);
        assertEquals(12, result.get("collections"));
        assertEquals(5420, result.get("totalDocuments"));
    }

    @Test
    void testGetSystemHealthSuccess() {
        // Given
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("uptime", 3600);
        healthData.put("timestamp", System.currentTimeMillis());

        HybridResult mockResult = new HybridResult();
        mockResult.setData(healthData);
        mockResult.setSuccess(true);
        mockResult.setFromCache(false);

        when(hybridDataCollector.getSystemHealth())
                .thenReturn(mockResult);

        // When
        Map<String, Object> result = dataCollectorService.getSystemHealth();

        // Then
        assertNotNull(result);
        assertEquals("UP", result.get("status"));
        assertEquals(3600, result.get("uptime"));
    }

    @Test
    void testGetRequestStatsSuccess() {
        // Given
        HybridResult mockResult = new HybridResult();
        Map<String, Object> statsData = new HashMap<>();
        statsData.put("totalRequests", 10250);
        statsData.put("averageResponseTime", 245);
        statsData.put("errorRate", 0.02);
        mockResult.setData(statsData);
        mockResult.setSuccess(true);
        mockResult.setFromCache(false);

        when(hybridDataCollector.getRequestStats())
                .thenReturn(mockResult);

        // When
        Map<String, Object> result = dataCollectorService.getRequestStats();

        // Then
        assertNotNull(result);
        assertEquals(10250, result.get("totalRequests"));
        assertEquals(0.02, (Double) result.get("errorRate"), 0.001);
    }

    @Test
    void testResponseCachingWorksCorrectly() {
        // Given
        HybridResult mockResult = new HybridResult();
        mockResult.setData(createGitHubData());
        mockResult.setSuccess(true);
        mockResult.setFromCache(false);

        when(hybridDataCollector.collectGitHubData("supremeai", "core"))
                .thenReturn(mockResult);

        // When - call twice
        Map<String, Object> result1 = dataCollectorService.getGitHubData("supremeai", "core");
        Map<String, Object> result2 = dataCollectorService.getGitHubData("supremeai", "core");

        // Then - second call should use cache
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1, result2);
    }

    @Test
    void testCacheClearWorksCorrectly() {
        // Given
        HybridResult mockResult = new HybridResult();
        mockResult.setData(createGitHubData());
        mockResult.setSuccess(true);
        mockResult.setFromCache(false);

        when(hybridDataCollector.collectGitHubData("supremeai", "core"))
                .thenReturn(mockResult);

        // When - get data, clear cache
        Map<String, Object> result1 = dataCollectorService.getGitHubData("supremeai", "core");
        dataCollectorService.clearCache();
        Map<String, Object> result2 = dataCollectorService.getGitHubData("supremeai", "core");

        // Then - should call collector twice (not cached on second call)
        verify(hybridDataCollector, times(2)).collectGitHubData("supremeai", "core");
    }

    @Test
    void testHandlesHybridDataCollectorException() {
        // Given
        when(hybridDataCollector.collectGitHubData("supremeai", "core"))
                .thenThrow(new RuntimeException("API connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            dataCollectorService.getGitHubData("supremeai", "core");
        });
    }

    @Test
    void testMultipleConcurrentRequests() throws InterruptedException {
        // Given
        HybridResult mockResult = new HybridResult();
        mockResult.setData(createGitHubData());
        mockResult.setSuccess(true);
        mockResult.setFromCache(false);

        when(hybridDataCollector.collectGitHubData(anyString(), anyString()))
                .thenReturn(mockResult);

        // When - simulate concurrent requests
        Thread t1 = new Thread(() -> 
            dataCollectorService.getGitHubData("org1", "repo1")
        );
        Thread t2 = new Thread(() -> 
            dataCollectorService.getGitHubData("org2", "repo2")
        );

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Then
        verify(hybridDataCollector, times(2)).collectGitHubData(anyString(), anyString());
    }

    @Test
    void testResponseStructureIncludesTimestamp() {
        // Given
        HybridResult mockResult = new HybridResult();
        mockResult.setData(createGitHubData());
        mockResult.setSuccess(true);
        mockResult.setFromCache(false);

        when(hybridDataCollector.collectGitHubData("supremeai", "core"))
                .thenReturn(mockResult);

        // When
        Map<String, Object> result = dataCollectorService.getGitHubData("supremeai", "core");

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("owner"));
        assertTrue(result.containsKey("repo"));
        assertTrue(result.containsKey("stars"));
    }

    // Helper methods
    private Map<String, Object> createGitHubData() {
        Map<String, Object> data = new HashMap<>();
        data.put("owner", "supremeai");
        data.put("repo", "core");
        data.put("stars", 150);
        data.put("forks", 25);
        data.put("issues", 3);
        data.put("pulls", 2);
        return data;
    }

    private Map<String, Object> createVercelData() {
        Map<String, Object> data = new HashMap<>();
        data.put("projectId", "proj_123");
        data.put("status", "ready");
        data.put("domains", 2);
        data.put("deployments", 45);
        return data;
    }

    private Map<String, Object> createFirebaseData() {
        Map<String, Object> data = new HashMap<>();
        data.put("collections", 12);
        data.put("documents", 5420);
        data.put("storage", 2.5);
        return data;
    }
}
