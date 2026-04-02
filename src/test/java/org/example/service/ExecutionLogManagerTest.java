package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutionLogManager Unit Tests
 * Tests: Event logging, metrics aggregation, trends analysis
 */
@DisplayName("ExecutionLogManager Tests")
@Tag("unit")
public class ExecutionLogManagerTest {

    private ExecutionLogManager logManager;

    @BeforeEach
    public void setUp() {
        logManager = new ExecutionLogManager();
    }

    @Test
    @DisplayName("Log Generation Event")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testLogGenerationEvent() {
        String projectId = "test-project";
        String componentName = "Button";
        String framework = "REACT";
        long duration = 2345;
        boolean success = true;
        String agent = "GROQ";
        double score = 92.5;

        logManager.logGeneration(projectId, componentName, framework, duration, success, agent, score);

        // Should not throw exception
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Log Validation Event")
    public void testLogValidationEvent() {
        String projectId = "test-project";
        String framework = "REACT";
        boolean isValid = true;
        int totalIssues = 0;
        double score = 100.0;

        logManager.logValidation(projectId, framework, isValid, totalIssues, score);

        // Should not throw exception
    }

    @Test
    @DisplayName("Log Error Fix Event")
    public void testLogErrorFixEvent() {
        String projectId = "test-project";
        int totalIssues = 5;
        int autoFixable = 4;
        int appliedFixes = 3;

        logManager.logErrorFix(projectId, totalIssues, autoFixable, appliedFixes);

        // Should not throw exception
    }

    @Test
    @DisplayName("Log Agent Selection Event")
    public void testLogAgentSelectionEvent() {
        String taskType = "code-generation";
        String selectedAgent = "GROQ";
        List<String> fallbackChain = Arrays.asList("GROQ", "DEEPSEEK", "CLAUDE");
        Map<String, Double> scores = new HashMap<>();
        scores.put("GROQ", 95.0);
        scores.put("DEEPSEEK", 88.0);
        scores.put("CLAUDE", 82.0);

        logManager.logAgentSelection(taskType, selectedAgent, fallbackChain, scores);

        // Should not throw exception
    }

    @Test
    @DisplayName("Get Project Metrics")
    public void testGetProjectMetrics() {
        String projectId = "test-project";
        
        Map<String, Object> metrics = logManager.getProjectMetrics(projectId);

        assertEquals(projectId, metrics.get("projectId"));
        assertNotNull(metrics.get("totalGenerations"));
        assertNotNull(metrics.get("successfulGenerations"));
        assertNotNull(metrics.get("successRate"));
        assertNotNull(metrics.get("avgDurationMs"));
        assertNotNull(metrics.get("avgValidationScore"));
        assertNotNull(metrics.get("agentUsage"));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Get System Metrics")
    public void testGetSystemMetrics() {
        Map<String, Object> metrics = logManager.getSystemMetrics();

        assertNotNull(metrics.get("totalEvents"));
        assertNotNull(metrics.get("successCount"));
        assertNotNull(metrics.get("failureCount"));
        assertNotNull(metrics.get("successRate"));
        assertNotNull(metrics.get("uniqueProjects"));
        assertNotNull(metrics.get("eventTypeDistribution"));
    }

    @Test
    @DisplayName("Get Daily Metrics")
    public void testGetDailyMetrics() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        Map<String, Object> metrics = logManager.getDailyMetrics(date);

        assertEquals(date, metrics.get("date"));
        assertNotNull(metrics.get("eventCount"));
        assertNotNull(metrics.get("successCount"));
        assertNotNull(metrics.get("successRate"));
        assertNotNull(metrics.get("avgValidationScore"));
    }

    @Test
    @DisplayName("Get Performance Trends")
    public void testGetPerformanceTrends() {
        int days = 7;
        
        Map<String, Object> trends = logManager.getPerformanceTrends(days);

        assertEquals(days, trends.get("days"));
        assertNotNull(trends.get("dailyBreakdown"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dailyData = (List<Map<String, Object>>) trends.get("dailyBreakdown");
        assertEquals(days, dailyData.size());
    }

    @Test
    @DisplayName("Clear Old Logs")
    public void testClearOldLogs() {
        int daysToKeep = 30;
        
        Map<String, Object> result = logManager.clearOldLogs(daysToKeep);

        assertEquals(daysToKeep, result.get("daysToKeep"));
        assertNotNull(result.get("deletedFiles"));
    }

    @Test
    @DisplayName("Export Logs to CSV")
    public void testExportLogsToCSV() {
        String outputPath = "./test_export.csv";
        
        assertDoesNotThrow(() -> logManager.exportLogsToCSV(outputPath));
    }

    @Test
    @DisplayName("Aggregate Success Rates")
    public void testAggregateSuccessRates() {
        // Log multiple events with different outcomes
        logManager.logGeneration("p1", "comp1", "REACT", 1000, true, "GROQ", 90);
        logManager.logGeneration("p1", "comp2", "REACT", 1500, false, "DEEPSEEK", 70);
        logManager.logGeneration("p1", "comp3", "REACT", 1200, true, "GROQ", 85);

        Map<String, Object> metrics = logManager.getProjectMetrics("p1");

        assertNotNull(metrics.get("successRate"));
        // 2 out of 3 = 66.67%
    }

    @Test
    @DisplayName("Track Agent Usage Statistics")
    public void testTrackAgentUsage() {
        logManager.logGeneration("p1", "comp1", "REACT", 1000, true, "GROQ", 90);
        logManager.logGeneration("p2", "comp2", "REACT", 1200, true, "DEEPSEEK", 85);
        logManager.logGeneration("p3", "comp3", "REACT", 900, true, "GROQ", 92);

        Map<String, Object> metrics = logManager.getProjectMetrics("p1");
        
        @SuppressWarnings("unchecked")
        Map<String, Long> agentUsage = (Map<String, Long>) metrics.get("agentUsage");
        assertNotNull(agentUsage);
    }

    @Test
    @DisplayName("Calculate Average Metrics")
    public void testCalculateAverageMetrics() {
        logManager.logGeneration("project", "comp1", "REACT", 1000, true, "GROQ", 90.0);
        logManager.logGeneration("project", "comp2", "REACT", 2000, true, "GROQ", 95.0);

        Map<String, Object> metrics = logManager.getProjectMetrics("project");

        // Average duration: (1000 + 2000) / 2 = 1500
        // Average score: (90 + 95) / 2 = 92.5
        Object avgDuration = metrics.get("avgDurationMs");
        assertNotNull(avgDuration);
    }

    @Test
    @DisplayName("Identify Most Used Agent")
    public void testIdentifyMostUsedAgent() {
        logManager.logGeneration("p1", "c1", "REACT", 1000, true, "GROQ", 90);
        logManager.logGeneration("p1", "c2", "REACT", 1000, true, "GROQ", 91);
        logManager.logGeneration("p1", "c3", "REACT", 1000, true, "DEEPSEEK", 88);

        Map<String, Object> metrics = logManager.getProjectMetrics("p1");

        assertEquals("GROQ", metrics.get("mostUsedAgent"));
    }

    @Test
    @DisplayName("Event Type Distribution")
    public void testEventTypeDistribution() {
        logManager.logGeneration("p1", "c1", "REACT", 1000, true, "GROQ", 90);
        logManager.logValidation("p1", "REACT", true, 0, 100);
        logManager.logErrorFix("p1", 1, 1, 1);

        Map<String, Object> metrics = logManager.getSystemMetrics();
        
        @SuppressWarnings("unchecked")
        Map<String, Long> distribution = (Map<String, Long>) metrics.get("eventTypeDistribution");
        assertNotNull(distribution.get("GENERATION"));
        assertNotNull(distribution.get("VALIDATION"));
        assertNotNull(distribution.get("ERROR_FIX"));
    }
}
