package com.supremeai.controller;

import com.supremeai.model.SystemLearning;
import com.supremeai.service.EnhancedLearningService;
import com.supremeai.service.SystemLearningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemLearningControllerTest {

    @Mock
    private SystemLearningService learningService;

    @Mock
    private EnhancedLearningService enhancedService;

    @Mock
    private com.supremeai.service.CyberSecuritySkillService cyberSkillService;

    private SystemLearningController controller;

    private SystemLearning testLearning;

    @BeforeEach
    void setUp() {
        controller = new SystemLearningController(learningService, enhancedService, cyberSkillService);
        testLearning = new SystemLearning("learn-1", "Test Topic", "natural_language", "Test content");
        testLearning.setLearningType("NLP");
        testLearning.setQualityScore(0.9);
        testLearning.setSuccess(true);
        testLearning.setTimesApplied(5);
    }

    // ==================== getAllLearning Tests ====================

    @Test
    void getAllLearning_ReturnsAllLearnings() {
        SystemLearning learning2 = new SystemLearning("learn-2", "Another Topic", "code_generation", "Code content");
        when(learningService.getAllLearning()).thenReturn(Flux.just(testLearning, learning2));

        Flux<SystemLearning> result = controller.getAllLearning();

        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();

        verify(learningService).getAllLearning();
    }

    @Test
    void getAllLearning_Empty_ReturnsEmptyFlux() {
        when(learningService.getAllLearning()).thenReturn(Flux.empty());

        Flux<SystemLearning> result = controller.getAllLearning();

        StepVerifier.create(result)
                .verifyComplete();
    }

    // ==================== getByCategory Tests ====================

    @Test
    void getByCategory_ValidCategory_ReturnsFilteredLearnings() {
        SystemLearning nlLearning = new SystemLearning("learn-3", "NLP Topic", "natural_language", "NLP content");
        when(learningService.getByCategory("natural_language")).thenReturn(Flux.just(testLearning, nlLearning));

        Flux<SystemLearning> result = controller.getByCategory("natural_language");

        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();

        verify(learningService).getByCategory("natural_language");
    }

    @Test
    void getByCategory_UnknownCategory_ReturnsEmptyFlux() {
        when(learningService.getByCategory("unknown")).thenReturn(Flux.empty());

        Flux<SystemLearning> result = controller.getByCategory("unknown");

        StepVerifier.create(result)
                .verifyComplete();
    }

    // ==================== addLearning Tests ====================

    @Test
    void addLearning_ValidLearning_ReturnsSavedLearning() {
        SystemLearning newLearning = new SystemLearning();
        newLearning.setTopic("New Topic");
        newLearning.setCategory("test");

        when(learningService.addLearning(any(SystemLearning.class))).thenReturn(Mono.just(newLearning));

        SystemLearning result = controller.addLearning(newLearning).block();

        assertEquals("New Topic", result.getTopic());

        verify(learningService).addLearning(any(SystemLearning.class));
    }

    // ==================== deleteLearning Tests ====================

    @Test
    void deleteLearning_ValidId_ReturnsMonoVoid() {
        when(learningService.deleteLearning("learn-1")).thenReturn(Mono.empty());

        controller.deleteLearning("learn-1").block();

        verify(learningService).deleteLearning("learn-1");
    }

    // ==================== getSystemWisdom Tests ====================

    @Test
    void getSystemWisdom_ReturnsHighConfidenceLearnings() {
        SystemLearning highConf1 = new SystemLearning("w1", "Wise Topic 1", "cat1", "Content 1");
        highConf1.setConfidenceScore(0.95);
        highConf1.setLearnedAt(new Date(1700000000000L));

        SystemLearning highConf2 = new SystemLearning("w2", "Wise Topic 2", "cat2", "Content 2");
        highConf2.setConfidenceScore(0.90);
        highConf2.setLearnedAt(new Date(1600000000000L));

        SystemLearning lowConf = new SystemLearning("w3", "Low Topic", "cat3", "Content 3");
        lowConf.setConfidenceScore(0.5); // Below threshold

        when(learningService.getAllLearning()).thenReturn(
                Flux.just(highConf1, highConf2, lowConf));

        Mono<List<SystemLearning>> result = controller.getSystemWisdom();

        StepVerifier.create(result)
                .expectNextMatches(list -> {
                    assertEquals(2, list.size());
                    assertEquals("w1", list.get(0).getId());
                    assertEquals("w2", list.get(1).getId());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getSystemWisdom_NoHighConfidence_ReturnsEmptyList() {
        SystemLearning lowConf = new SystemLearning("low-1", "Low Topic", "cat", "Content");
        lowConf.setConfidenceScore(0.5);

        when(learningService.getAllLearning()).thenReturn(Flux.just(lowConf));

        Mono<List<SystemLearning>> result = controller.getSystemWisdom();

        StepVerifier.create(result)
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    // ==================== getStats Tests ====================

    @Test
    void getStats_ReturnsLearningStatistics() {
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("total", 10);
        mockStats.put("byType", Map.of("NLP", 5L, "MULTIMODAL", 3L, "ECOSYSTEM", 2L));
        mockStats.put("successCount", 8L);
        mockStats.put("failureCount", 2L);
        mockStats.put("averageQuality", 0.82);
        mockStats.put("successRate", 0.8);

        when(enhancedService.getLearningStats()).thenReturn(Mono.just(mockStats));

        Map<String, Object> result = controller.getStats().block();

        assertEquals(10, result.get("total"));
        assertEquals(8L, result.get("successCount"));

        verify(enhancedService).getLearningStats();
    }

    @Test
    void getStats_EmptyLearning_ReturnsZeroStats() {
        Map<String, Object> emptyStats = Map.of("total", 0);
        when(enhancedService.getLearningStats()).thenReturn(Mono.just(emptyStats));

        Map<String, Object> result = controller.getStats().block();

        assertEquals(0, result.get("total"));
    }

    // ==================== getBestPractices Tests ====================

    @Test
    void getBestPractices_ValidCategory_ReturnsFilteredLearnings() {
        SystemLearning bp1 = new SystemLearning("bp1", "Best Practice 1", "code_generation", "Content 1");
        bp1.setQualityScore(0.95);
        bp1.setSuccess(true);

        SystemLearning bp2 = new SystemLearning("bp2", "Best Practice 2", "code_generation", "Content 2");
        bp2.setQualityScore(0.88);
        bp2.setSuccess(true);

        SystemLearning lowQuality = new SystemLearning("low-1", "Low Quality", "code_generation", "Content 3");
        lowQuality.setQualityScore(0.5);
        lowQuality.setSuccess(true);

        when(enhancedService.getBestPractices("code_generation", 0.8))
                .thenReturn(Flux.just(bp1, bp2));

        Flux<SystemLearning> result = controller.getBestPractices("code_generation", 0.8);

        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();

        verify(enhancedService).getBestPractices("code_generation", 0.8);
    }

    @Test
    void getBestPractices_HighThreshold_ReturnsEmpty() {
        when(enhancedService.getBestPractices("any_category", 0.99))
                .thenReturn(Flux.empty());

        Flux<SystemLearning> result = controller.getBestPractices("any_category", 0.99);

        StepVerifier.create(result)
                .verifyComplete();
    }

    // ==================== getRecommendations Tests ====================

    @Test
    void getRecommendations_ValidTaskType_ReturnsRecommendations() {
        SystemLearning rec1 = new SystemLearning("rec-1", "Rec 1", "predictive", "Recommendation 1");
        rec1.setConfidenceScore(0.92);
        rec1.setLearningType("PREDICTIVE");
        rec1.setTags(List.of("code_generation", "predictive", "pattern_matching"));

        when(enhancedService.getPredictiveRecommendations(eq("code_generation"), anyMap()))
                .thenReturn(Flux.just(rec1));

        Flux<SystemLearning> result = controller.getRecommendations("code_generation");

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(enhancedService).getPredictiveRecommendations(eq("code_generation"), anyMap());
    }

    @Test
    void getRecommendations_NoMatches_ReturnsEmptyFlux() {
        when(enhancedService.getPredictiveRecommendations(anyString(), anyMap()))
                .thenReturn(Flux.empty());

        Flux<SystemLearning> result = controller.getRecommendations("unknown_task");

        StepVerifier.create(result)
                .verifyComplete();
    }

    // ==================== triggerImprovement Tests ====================

    @Test
    void triggerImprovement_ReturnsImprovementResults() {
        Map<String, Object> mockResult = Map.of(
                "success", true,
                "totalLearningsAnalyzed", 50,
                "improvementsIdentified", 5,
                "optimizationsApplied", 3,
                "recommendationsGenerated", 4
        );

        when(enhancedService.improveSystemLearning()).thenReturn(Mono.just(mockResult));

        Map<String, Object> result = controller.triggerImprovement().block();

        assertTrue((Boolean) result.get("success"));
        assertEquals(50, result.get("totalLearningsAnalyzed"));

        verify(enhancedService).improveSystemLearning();
    }

    // ==================== improveLearning Tests ====================

    @Test
    void improveLearning_ReturnsImprovementResults() {
        Map<String, Object> mockResult = Map.of("success", true, "totalLearningsAnalyzed", 10);

        when(enhancedService.improveSystemLearning()).thenReturn(Mono.just(mockResult));

        Map<String, Object> result = controller.improveLearning().block();

        assertTrue((Boolean) result.get("success"));
    }

    // ==================== triggerCyberResearch Tests ====================

    @Test
    void triggerCyberResearch_ReturnsResearchResults() {
        Map<String, Object> mockInsight = Map.of(
                "techniqueId", "SQL_INJECTION_abc1",
                "source", "Autonomous Research",
                "defenseEfficiency", 0.95
        );

        when(cyberSkillService.initiateLearningCycle("SQL Injection")).thenReturn(Mono.just(mockInsight));

        Map<String, Object> result = controller.triggerCyberResearch("SQL Injection").block();

        assertEquals("SQL_INJECTION_abc1", result.get("techniqueId"));

        verify(cyberSkillService).initiateLearningCycle("SQL Injection");
    }

    // ==================== getCyberStatus Tests ====================

    @Test
    void getCyberStatus_ReturnsCombinedStatus() {
        Map<String, Object> skill1 = Map.of("id", "SQLI_V1", "severity", "CRITICAL");
        Map<String, Object> protection1 = Map.of("targetId", "SQLI_V1", "status", "ACTIVE");
        Map<String, Object> auditReport = Map.of(
                "auditId", "audit-123",
                "vulnerabilitiesFound", 0,
                "resilienceScore", 0.99
        );

        when(cyberSkillService.getLearnedSkills()).thenReturn(Flux.just(skill1));
        when(cyberSkillService.getActiveProtections()).thenReturn(Flux.just(protection1));
        when(cyberSkillService.runSelfAudit()).thenReturn(Mono.just(auditReport));

        Map<String, Object> result = controller.getCyberStatus().block();

        List<?> skills = (List<?>) result.get("skills");
        List<?> protections = (List<?>) result.get("protections");
        assertNotNull(result.get("lastAudit"));
        assertEquals(1, skills.size());
        assertEquals(1, protections.size());
    }
}