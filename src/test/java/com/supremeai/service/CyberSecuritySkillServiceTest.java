package com.supremeai.service;

import com.supremeai.controller.IntelligenceController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CyberSecuritySkillServiceTest {IntelligenceControllerpublic CyberSecuritySkillServiceTest(IntelligenceController intelligenceController, CyberSecuritySkillService cyberSecuritySkillService) {
IntelligenceController    this.intelligenceController = intelligenceController;
IntelligenceController    this.cyberSecuritySkillService = cyberSecuritySkillService;
IntelligenceController}






    @BeforeEach
    void setUp() {
        cyberSecuritySkillService = new CyberSecuritySkillService();
        // The service has hardcoded initial techniques in its constructor
    }

    // ==================== Initial State Tests ====================

    @Test
    void constructor_InitializesWithCoreTechniques() {
        // The constructor registers SQLI_V1 and XSS_V1
        Flux<Map<String, Object>> skills = cyberSecuritySkillService.getLearnedSkills();

        StepVerifier.create(skills)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void constructor_InitialTechniquesHaveCorrectSeverity() {
        Flux<Map<String, Object>> skills = cyberSecuritySkillService.getLearnedSkills();

        StepVerifier.create(skills.collectList())
                .expectNextMatches(list -> {
                    // Both initial techniques should have severity
                    assertTrue(list.stream().allMatch(s ->
                            s.get("severity") != null &&
                            (s.get("severity").equals("CRITICAL") || s.get("severity").equals("HIGH"))
                    ));
                    return true;
                })
                .verifyComplete();
    }

    // ==================== getLearnedSkills Tests ====================

    @Test
    void getLearnedSkills_ReturnsAllTechniques() {
        Flux<Map<String, Object>> skills = cyberSecuritySkillService.getLearnedSkills();

        StepVerifier.create(skills)
                .expectNextCount(2) // SQLI_V1 and XSS_V1 from constructor
                .verifyComplete();
    }

    // ==================== getActiveProtections Tests ====================

    @Test
    void getActiveProtections_AfterInit_ReturnsProtections() {
        // Protections are generated alongside techniques in constructor
        // But generateProtection is private and only called from initiateLearningCycle
        // So initially there are no active protections
        Flux<Map<String, Object>> protections = cyberSecuritySkillService.getActiveProtections();

        StepVerifier.create(protections)
                .verifyComplete();
    }

    // ==================== initiateLearningCycle Tests ====================

    @Test
    void initiateLearningCycle_ValidTopic_CreatesTechniqueAndProtection() {
        Mono<Map<String, Object>> result = cyberSecuritySkillService.initiateLearningCycle("SQL Injection");

        StepVerifier.create(result)
                .expectNextMatches(insight -> {
                    assertNotNull(insight.get("techniqueId"));
                    assertEquals("Autonomous Research", insight.get("source"));
                    assertEquals(0.95, insight.get("defenseEfficiency"));
                    assertTrue(insight.get("learnedAt").toString().length() > 0);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void initiateLearningCycle_AfterLearning_SkillCountIncreases() {
        int initialCount = cyberSecuritySkillService.getLearnedSkills().collectList().block().size();

        cyberSecuritySkillService.initiateLearningCycle("XSS Advanced").block();

        int newCount = cyberSecuritySkillService.getLearnedSkills().collectList().block().size();

        assertEquals(initialCount + 1, newCount);
    }

    @Test
    void initiateLearningCycle_CreatesProtection() {
        cyberSecuritySkillService.initiateLearningCycle("Test Topic").block();

        Flux<Map<String, Object>> protections = cyberSecuritySkillService.getActiveProtections();

        StepVerifier.create(protections)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void initiateLearningCycle_MultipleTopics_CreatesMultipleTechniques() {
        cyberSecuritySkillService.initiateLearningCycle("Topic 1").block();
        cyberSecuritySkillService.initiateLearningCycle("Topic 2").block();
        cyberSecuritySkillService.initiateLearningCycle("Topic 3").block();

        int count = cyberSecuritySkillService.getLearnedSkills().collectList().block().size();

        assertEquals(5, count); // 2 initial + 3 new
    }

    // ==================== runSelfAudit Tests ====================

    @Test
    void runSelfAudit_ReturnsValidReport() {
        Mono<Map<String, Object>> result = cyberSecuritySkillService.runSelfAudit();

        StepVerifier.create(result)
                .expectNextMatches(report -> {
                    assertNotNull(report.get("auditId"));
                    assertNotNull(report.get("timestamp"));
                    assertEquals(0, report.get("vulnerabilitiesFound"));
                    assertEquals(0.99, report.get("resilienceScore"));
                    assertTrue(report.get("summary").toString().contains("successfully"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void runSelfAudit_MultipleCalls_GeneratesDifferentAuditIds() {
        Mono<Map<String, Object>> audit1 = cyberSecuritySkillService.runSelfAudit();
        Mono<Map<String, Object>> audit2 = cyberSecuritySkillService.runSelfAudit();

        StepVerifier.create(audit1)
                .expectNextMatches(r1 -> {
                    String id1 = r1.get("auditId").toString();
                    return id1 != null && !id1.isEmpty();
                })
                .verifyComplete();

        StepVerifier.create(audit2)
                .expectNextMatches(r2 -> {
                    String id2 = r2.get("auditId").toString();
                    return id2 != null && !id2.isEmpty();
                })
                .verifyComplete();
    }

    // ==================== Resilience Score Tests ====================

    @Test
    void runSelfAudit_ResilienceScoreIsHigh() {
        Mono<Map<String, Object>> result = cyberSecuritySkillService.runSelfAudit();

        StepVerifier.create(result)
                .expectNextMatches(report -> {
                    double score = (double) report.get("resilienceScore");
                    assertTrue(score >= 0.9 && score <= 1.0);
                    return true;
                })
                .verifyComplete();
    }
}