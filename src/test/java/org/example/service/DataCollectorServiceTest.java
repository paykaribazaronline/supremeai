package org.example.service;


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
        HybridDataCollector.HybridResult mockResult = new HybridDataCollector.HybridResult();
        mockResult.data = createGitHubData();
        mockResult.success = true;
        mockResult.dataSource = "API";

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
        HybridDataCollector.HybridResult mockResult = new HybridDataCollector.HybridResult();
        mockResult.data = createGitHubData();
        mockResult.success = true;
        mockResult.dataSource = "CACHE";

        when(hybridDataCollector.collectGitHubData("supremeai", "core"))
                .thenReturn(mockResult);

        // When
        Map<String, Object> result = dataCollectorService.getGitHubData("supremeai", "core");

        // Then
        assertNotNull(result);
        assertNotNull(result.get("owner"));
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
        HybridDataCollector.HybridResult mockResult = new HybridDataCollector.HybridResult();
        mockResult.data = createVercelData();
        mockResult.success = true;
        mockResult.dataSource = "API";

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
    void testGetFirebaseStatusSuccess() {
        // Given
        HybridDataCollector.HybridResult mockResult = new HybridDataCollector.HybridResult();
        mockResult.data = createFirebaseData();
        mockResult.success = true;
        mockResult.dataSource = "API";

        when(hybridDataCollector.collectFirebaseStatus())
                .thenReturn(mockResult);

        // When
        Map<String, Object> result = dataCollectorService.getFirebaseStatus();

        // Then
        assertNotNull(result);
        assertEquals(12, result.get("collections"));
    }

    @Test
    void testResponseCachingWorksCorrectly() {
        // Given
        HybridDataCollector.HybridResult mockResult = new HybridDataCollector.HybridResult();
        mockResult.data = createGitHubData();
        mockResult.success = true;
        mockResult.dataSource = "API";

        when(hybridDataCollector.collectGitHubData("supremeai", "core"))
                .thenReturn(mockResult);

        // When - call twice
        dataCollectorService.getGitHubData("supremeai", "core");
        dataCollectorService.getGitHubData("supremeai", "core");

        // Then - second call should use cache, so collector called only once
        verify(hybridDataCollector, times(1)).collectGitHubData("supremeai", "core");
    }

    @Test
    void testCacheClearWorksCorrectly() {
        // Given
        HybridDataCollector.HybridResult mockResult = new HybridDataCollector.HybridResult();
        mockResult.data = createGitHubData();
        mockResult.success = true;
        mockResult.dataSource = "API";

        when(hybridDataCollector.collectGitHubData("supremeai", "core"))
                .thenReturn(mockResult);

        // When - get data, clear cache
        Map<String, Object> result1 = dataCollectorService.getGitHubData("supremeai", "core");
        dataCollectorService.clearCache("github:supremeai/core");
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
        HybridDataCollector.HybridResult mockResult = new HybridDataCollector.HybridResult();
        mockResult.data = createGitHubData();
        mockResult.success = true;
        mockResult.dataSource = "API";

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
    void testResponseStructureIncludesRequiredFields() {
        // Given
        HybridDataCollector.HybridResult mockResult = new HybridDataCollector.HybridResult();
        mockResult.data = createGitHubData();
        mockResult.success = true;
        mockResult.dataSource = "API";

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
