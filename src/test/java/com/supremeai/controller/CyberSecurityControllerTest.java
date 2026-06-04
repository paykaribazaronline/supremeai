package com.supremeai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.supremeai.response.ApiResponse;
import com.supremeai.service.CyberSecuritySkillService;
import java.util.Map;
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

@ExtendWith(MockitoExtension.class)
class CyberSecurityControllerTest {

  @Mock private CyberSecuritySkillService cyberSecuritySkillService;

  private CyberSecurityController cyberSecurityController;

  @BeforeEach
  void setUp() {
    cyberSecurityController = new CyberSecurityController();
    setField(cyberSecurityController, "cyberSecuritySkillService", cyberSecuritySkillService);
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      java.lang.reflect.Field field = CyberSecurityController.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // ==================== getSkills Tests ====================

  @Test
  void getSkills_ReturnsLearnedSkills() {
    Map<String, Object> skill1 =
        Map.of("id", "SQLI_V1", "name", "SQL Injection Pattern Analysis", "severity", "CRITICAL");
    Map<String, Object> skill2 =
        Map.of("id", "XSS_V1", "name", "Cross-Site Scripting Mitigation", "severity", "HIGH");

    when(cyberSecuritySkillService.getLearnedSkills()).thenReturn(Flux.just(skill1, skill2));

    Flux<Map<String, Object>> result = cyberSecurityController.getSkills();

    StepVerifier.create(result)
        .expectNextMatches(s -> "SQLI_V1".equals(s.get("id")))
        .expectNextMatches(s -> "XSS_V1".equals(s.get("id")))
        .verifyComplete();
  }

  @Test
  void getSkills_EmptySkills_ReturnsEmptyFlux() {
    when(cyberSecuritySkillService.getLearnedSkills()).thenReturn(Flux.empty());

    Flux<Map<String, Object>> result = cyberSecurityController.getSkills();

    StepVerifier.create(result).verifyComplete();
  }

  // ==================== getProtections Tests ====================

  @Test
  void getProtections_ReturnsActiveProtections() {
    Map<String, Object> protection1 =
        Map.of(
            "targetId",
            "SQLI_V1",
            "protectionType",
            "DYNAMIC_VULNERABILITY_SHIELD",
            "status",
            "ACTIVE");

    when(cyberSecuritySkillService.getActiveProtections()).thenReturn(Flux.just(protection1));

    Flux<Map<String, Object>> result = cyberSecurityController.getProtections();

    StepVerifier.create(result).expectNextCount(1).verifyComplete();
  }

  @Test
  void getProtections_NoProtections_ReturnsEmptyFlux() {
    when(cyberSecuritySkillService.getActiveProtections()).thenReturn(Flux.empty());

    Flux<Map<String, Object>> result = cyberSecurityController.getProtections();

    StepVerifier.create(result).verifyComplete();
  }

  // ==================== initiateLearning Tests ====================

  @Test
  void initiateLearning_ValidTopic_ReturnsInsight() {
    Map<String, Object> mockInsight =
        Map.of(
            "techniqueId", "SQL_INJECTION_abc1",
            "source", "Autonomous Research",
            "learnedAt", "2026-05-14T10:00:00",
            "defenseEfficiency", 0.95);

    when(cyberSecuritySkillService.initiateLearningCycle(anyString()))
        .thenReturn(Mono.just(mockInsight));

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        cyberSecurityController.initiateLearning(Map.of("topic", "SQL Injection"));

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().isSuccess());
              assertEquals("SQL_INJECTION_abc1", response.getBody().getData().get("techniqueId"));
              return true;
            })
        .verifyComplete();

    verify(cyberSecuritySkillService).initiateLearningCycle("SQL Injection");
  }

  @Test
  void initiateLearning_EmptyTopic_ReturnsInsightWithDefaultTopic() {
    Map<String, Object> mockInsight =
        Map.of(
            "techniqueId", "GENERAL_VULNERABILITIES_abc1",
            "source", "Autonomous Research");

    when(cyberSecuritySkillService.initiateLearningCycle("General Vulnerabilities"))
        .thenReturn(Mono.just(mockInsight));

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        cyberSecurityController.initiateLearning(Map.of());

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              return true;
            })
        .verifyComplete();

    verify(cyberSecuritySkillService).initiateLearningCycle("General Vulnerabilities");
  }

  // ==================== runAudit Tests ====================

  @Test
  void runAudit_ReturnsAuditReport() {
    Map<String, Object> mockReport =
        Map.of(
            "auditId",
            "abc-123-def",
            "vulnerabilitiesFound",
            0,
            "resilienceScore",
            0.99,
            "summary",
            "System successfully resisted all internal exploitation attempts.");

    when(cyberSecuritySkillService.runSelfAudit()).thenReturn(Mono.just(mockReport));

    Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
        cyberSecurityController.runAudit();

    StepVerifier.create(result)
        .expectNextMatches(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              assertTrue(response.getBody().isSuccess());
              assertEquals(0, response.getBody().getData().get("vulnerabilitiesFound"));
              assertEquals(0.99, response.getBody().getData().get("resilienceScore"));
              return true;
            })
        .verifyComplete();

    verify(cyberSecuritySkillService).runSelfAudit();
  }

  // ==================== Verify Interactions ====================

  @Test
  void getAllEndpoints_VerifyServiceInteractions() {
    // Test all endpoints to ensure proper service method calls
    when(cyberSecuritySkillService.getLearnedSkills()).thenReturn(Flux.empty());
    when(cyberSecuritySkillService.getActiveProtections()).thenReturn(Flux.empty());
    when(cyberSecuritySkillService.initiateLearningCycle(anyString()))
        .thenReturn(Mono.just(Map.of()));
    when(cyberSecuritySkillService.runSelfAudit()).thenReturn(Mono.just(Map.of()));

    // Call each endpoint
    cyberSecurityController.getSkills().blockLast();
    cyberSecurityController.getProtections().blockLast();
    cyberSecurityController.initiateLearning(Map.of()).block();
    cyberSecurityController.runAudit().block();

    // Verify all methods were called exactly once
    verify(cyberSecuritySkillService, times(1)).getLearnedSkills();
    verify(cyberSecuritySkillService, times(1)).getActiveProtections();
    verify(cyberSecuritySkillService, times(1)).initiateLearningCycle(anyString());
    verify(cyberSecuritySkillService, times(1)).runSelfAudit();
  }
}
