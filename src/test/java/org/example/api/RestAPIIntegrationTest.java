package org.example.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.example.service.CodeGenerationOrchestrator;
import org.example.service.ExecutionLogManager;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * REST API Integration Tests
 * Tests: Endpoint functionality, request/response handling
 */
@DisplayName("REST API Integration Tests")
public class RestAPIIntegrationTest {

    private CodeGenerationController generationController;
    private ExecutionLogController logController;
    
    @Mock
    private CodeGenerationOrchestrator orchestrator;
    
    @Mock
    private ExecutionLogManager logManager;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        generationController = new CodeGenerationController(orchestrator);
        logController = new ExecutionLogController(logManager);
    }

    @Test
    @DisplayName("POST /api/generation/react-component")
    public void testGenerateReactComponent() {
        String projectId = "test-project";
        String componentName = "Button";
        String description = "Button component";
        String features = "responsive,accessible";

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("status", "generated");
        mockResult.put("filesGenerated", 3);

        when(orchestrator.generateReactComponent(projectId, componentName, description, anyList()))
            .thenReturn(mockResult);

        Map<String, Object> response = generationController.generateReactComponent(
            projectId, componentName, description, features
        );

        assertEquals(true, response.get("success"));
        assertNotNull(response.get("data"));
    }

    @Test
    @DisplayName("POST /api/generation/node-service")
    public void testGenerateNodeService() {
        String projectId = "test-project";
        String serviceName = "UserService";
        String description = "User management";
        String methods = "create,read,update,delete";

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("status", "generated");
        mockResult.put("filesGenerated", 3);

        when(orchestrator.generateNodeService(projectId, serviceName, description, anyList()))
            .thenReturn(mockResult);

        Map<String, Object> response = generationController.generateNodeService(
            projectId, serviceName, description, methods
        );

        assertEquals(true, response.get("success"));
    }

    @Test
    @DisplayName("POST /api/generation/batch")
    public void testBatchGeneration() {
        String projectId = "test-project";
        String framework = "REACT";
        List<Map<String, String>> components = new ArrayList<>();
        
        Map<String, String> comp = new HashMap<>();
        comp.put("name", "Button");
        components.add(comp);

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("successCount", 1);
        mockResult.put("failureCount", 0);

        when(orchestrator.generateBatch(projectId, framework, components))
            .thenReturn(mockResult);

        Map<String, Object> response = generationController.generateBatch(
            projectId, framework, components
        );

        assertEquals(true, response.get("success"));
    }

    @Test
    @DisplayName("GET /api/generation/stats")
    public void testGetGenerationStats() {
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("supportedFrameworks", Arrays.asList("REACT", "NODEJS"));

        when(orchestrator.getGenerationStats())
            .thenReturn(mockStats);

        Map<String, Object> response = generationController.getStats();

        assertEquals(true, response.get("success"));
        assertNotNull(response.get("stats"));
    }

    @Test
    @DisplayName("GET /api/generation/history/{projectId}")
    public void testGetGenerationHistory() {
        String projectId = "test-project";
        Map<String, Object> mockHistory = new HashMap<>();
        mockHistory.put("projectId", projectId);
        mockHistory.put("totalGenerations", 5);

        when(orchestrator.getGenerationHistory(projectId))
            .thenReturn(mockHistory);

        Map<String, Object> response = generationController.getGenerationHistory(projectId);

        assertEquals(true, response.get("success"));
        assertNotNull(response.get("data"));
    }

    @Test
    @DisplayName("GET /api/generation/analytics")
    public void testGetGenerationAnalytics() {
        Map<String, Object> mockAnalytics = new HashMap<>();
        mockAnalytics.put("totalGenerations", 150);

        when(orchestrator.getAllGenerationStats())
            .thenReturn(mockAnalytics);

        Map<String, Object> response = generationController.getGenerationAnalytics();

        assertEquals(true, response.get("success"));
    }

    @Test
    @DisplayName("GET /api/generation/frameworks")
    public void testGetFrameworks() {
        Map<String, Object> response = generationController.getFrameworks();

        assertEquals(true, response.get("frameworks") != null);
        assertTrue(((List<?>) response.get("frameworks")).contains("REACT"));
    }

    @Test
    @DisplayName("GET /api/generation/health")
    public void testGenerationHealth() {
        Map<String, Object> response = generationController.health();

        assertEquals("healthy", response.get("status"));
        assertEquals("CodeGenerationOrchestrator", response.get("service"));
    }

    // ExecutionLogController tests

    @Test
    @DisplayName("GET /api/execution-logs/project/{projectId}")
    public void testGetProjectMetrics() {
        String projectId = "test-project";
        Map<String, Object> mockMetrics = new HashMap<>();
        mockMetrics.put("projectId", projectId);

        when(logManager.getProjectMetrics(projectId))
            .thenReturn(mockMetrics);

        Map<String, Object> response = logController.getProjectMetrics(projectId);

        assertEquals(true, response.get("success"));
        assertNotNull(response.get("data"));
    }

    @Test
    @DisplayName("GET /api/execution-logs/system")
    public void testGetSystemMetrics() {
        Map<String, Object> mockMetrics = new HashMap<>();
        mockMetrics.put("totalEvents", 500);

        when(logManager.getSystemMetrics())
            .thenReturn(mockMetrics);

        Map<String, Object> response = logController.getSystemMetrics();

        assertEquals(true, response.get("success"));
    }

    @Test
    @DisplayName("GET /api/execution-logs/daily/{date}")
    public void testGetDailyMetrics() {
        String date = "2024-03-29";
        Map<String, Object> mockMetrics = new HashMap<>();
        mockMetrics.put("date", date);

        when(logManager.getDailyMetrics(date))
            .thenReturn(mockMetrics);

        Map<String, Object> response = logController.getDailyMetrics(date);

        assertEquals(true, response.get("success"));
    }

    @Test
    @DisplayName("GET /api/execution-logs/trends/{days}")
    public void testGetPerformanceTrends() {
        int days = 7;
        Map<String, Object> mockTrends = new HashMap<>();
        mockTrends.put("days", days);

        when(logManager.getPerformanceTrends(days))
            .thenReturn(mockTrends);

        Map<String, Object> response = logController.getPerformanceTrends(days);

        assertEquals(true, response.get("success"));
    }

    @Test
    @DisplayName("GET /api/execution-logs/health")
    public void testExecutionLogsHealth() {
        Map<String, Object> response = logController.health();

        assertEquals("healthy", response.get("status"));
        assertEquals("ExecutionLogManager", response.get("service"));
    }

    @Test
    @DisplayName("Response Structure Consistency")
    public void testResponseStructureConsistency() {
        Map<String, Object> mockData = new HashMap<>();
        when(orchestrator.getGenerationStats()).thenReturn(mockData);

        Map<String, Object> response = generationController.getStats();

        assertTrue(response.containsKey("success"));
        assertTrue(response.containsKey("timestamp"));
    }

    @Test
    @DisplayName("Error Handling in Endpoints")
    public void testErrorHandling() {
        when(orchestrator.generateReactComponent(anyString(), anyString(), anyString(), anyList()))
            .thenThrow(new RuntimeException("API error"));

        Map<String, Object> response = generationController.generateReactComponent(
            "test", "comp", "desc", ""
        );

        assertEquals(false, response.get("success"));
        assertNotNull(response.get("error"));
    }
}
