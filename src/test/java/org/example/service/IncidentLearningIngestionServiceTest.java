package org.example.service;

import org.example.model.SystemLearning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentLearningIngestionServiceTest {

    @Mock
    private SystemLearningService learningService;

    @InjectMocks
    private IncidentLearningIngestionService ingestionService;

    @TempDir
    Path tempDir;

    @Test
    void ingestExecutionLogs_convertsFailedAndPartialEventsIntoIncidents() throws Exception {
        String json = """
            [
              {
                "eventType": "GENERATION",
                "projectId": "p1",
                "componentName": "AuthScreen",
                "status": "FAILED",
                "timestamp": "2026-04-05 10:00:00",
                "metadata": {"totalIssues": 3, "appliedFixes": 1}
              },
              {
                "eventType": "VALIDATION",
                "projectId": "p1",
                "componentName": "",
                "status": "PARTIAL",
                "timestamp": "2026-04-05 10:05:00",
                "metadata": {"totalIssues": 2, "appliedFixes": 0}
              },
              {
                "eventType": "GENERATION",
                "projectId": "p2",
                "componentName": "Home",
                "status": "SUCCESS",
                "timestamp": "2026-04-05 10:10:00",
                "metadata": {}
              }
            ]
            """;

        Path logFile = tempDir.resolve("generation_2026-04-05.json");
        Files.writeString(logFile, json);

        when(learningService.learnFromIncident(anyString(), anyString(), anyString(), anyString(), anyList(), anyDouble(), anyMap()))
            .thenReturn(Map.of("status", "success"));

        Map<String, Object> result = ingestionService.ingestExecutionLogs(tempDir, 10);

        assertEquals("success", result.get("status"));
        assertEquals(3, result.get("eventsScanned"));
        assertEquals(2, result.get("incidentsLearned"));
        verify(learningService, times(2))
            .learnFromIncident(anyString(), anyString(), anyString(), anyString(), anyList(), anyDouble(), anyMap());
    }

    @Test
    void getIncidentLearningInsights_returnsCategoryAndConfidenceAggregates() {
        SystemLearning security = new SystemLearning();
        security.setCategory("SECURITY");
        security.setConfidenceScore(0.9);
        security.setTimestamp(System.currentTimeMillis());
        security.setContext(Map.of("kind", "INCIDENT_PLAYBOOK"));

        SystemLearning validation = new SystemLearning();
        validation.setCategory("VALIDATION");
        validation.setConfidenceScore(0.8);
        validation.setTimestamp(System.currentTimeMillis());
        validation.setContext(Map.of("kind", "INCIDENT_PLAYBOOK"));

        when(learningService.getIncidentPlaybooks(null)).thenReturn(List.of(security, validation));

        Map<String, Object> insights = ingestionService.getIncidentLearningInsights();

        assertEquals("success", insights.get("status"));
        assertEquals(2, insights.get("totalIncidents"));
        double avgConfidence = (double) insights.get("averageConfidence");
        assertTrue(avgConfidence >= 0.84 && avgConfidence <= 0.86);
    }
}
