package com.supremeai.agentorchestration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.supremeai.service.CodeGenerationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AgentOrchestrationControllerTest {

  @Mock private AdaptiveAgentOrchestrator orchestrator;

  @Mock private CodeGenerationService codeGenerationService;

  @Mock private com.supremeai.agent.GPublishAgent publishAgent;

  @Mock private com.supremeai.service.AppOrchestrationService orchestrationService;

  private AgentOrchestrationController controller;

  @BeforeEach
  void setUp() {
    // Set up the controller with both mocks
    controller = new AgentOrchestrationController();

    // Use reflection to inject mocks since controller uses @Autowired
    try {
      var field = AgentOrchestrationController.class.getDeclaredField("orchestrator");
      field.setAccessible(true);
      field.set(controller, orchestrator);

      field = AgentOrchestrationController.class.getDeclaredField("codeGenerationService");
      field.setAccessible(true);
      field.set(controller, codeGenerationService);

      field = AgentOrchestrationController.class.getDeclaredField("publishAgent");
      field.setAccessible(true);
      field.set(controller, publishAgent);

      field = AgentOrchestrationController.class.getDeclaredField("orchestrationService");
      field.setAccessible(true);
      field.set(controller, orchestrationService);
    } catch (Exception e) {
      throw new RuntimeException("Failed to inject mocks", e);
    }
  }

  @Test
  void orchestrate_shouldReturnBadRequest_whenRequirementIsNull() {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("requirement", null);

    // Act
    Mono<ResponseEntity<Object>> result = controller.orchestrate(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(400, response.getStatusCode().value());
              assertTrue(response.getBody().toString().contains("error"));
            })
        .verifyComplete();
  }

  @Test
  void orchestrate_shouldReturnBadRequest_whenRequirementIsEmpty() {
    // Arrange
    Map<String, Object> request = Map.of("requirement", "");

    // Act
    Mono<ResponseEntity<Object>> result = controller.orchestrate(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(400, response.getStatusCode().value());
            })
        .verifyComplete();
  }

  @Test
  void orchestrate_shouldReturn503_whenOrchestratorIsNull() {
    // Arrange - set orchestrator to null
    try {
      var field = AgentOrchestrationController.class.getDeclaredField("orchestrator");
      field.setAccessible(true);
      field.set(controller, null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Map<String, Object> request = Map.of("requirement", "Build an app");

    // Act
    Mono<ResponseEntity<Object>> result = controller.orchestrate(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(503, response.getStatusCode().value());
            })
        .verifyComplete();
  }

  @Test
  @SuppressWarnings("unchecked")
  void orchestrate_shouldReturnCompletedStatus_whenSuccessful() {
    // Arrange
    Map<String, Object> request = Map.of("requirement", "Build a web app");

    OrchesResultContext mockResult = new OrchesResultContext(new HashMap<>());
    mockResult.setStatus("COMPLETED");
    when(orchestrator.orchestrate(anyString())).thenReturn(mockResult);

    // Act
    Mono<ResponseEntity<Object>> result = controller.orchestrate(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(200, response.getStatusCode().value());
              @SuppressWarnings("unchecked")
              Map<String, Object> body = (Map<String, Object>) response.getBody();
              assertNotNull(body);
              assertEquals("COMPLETED", body.get("status"));
            })
        .verifyComplete();
  }

  @Test
  void orchestrate_shouldHandleException_andReturn500() {
    // Arrange
    Map<String, Object> request = Map.of("requirement", "Build an app");
    when(orchestrator.orchestrate(anyString()))
        .thenThrow(new RuntimeException("Orchestration failed"));

    // Act
    Mono<ResponseEntity<Object>> result = controller.orchestrate(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(500, response.getStatusCode().value());
              assertTrue(response.getBody().toString().contains("error"));
            })
        .verifyComplete();
  }

  @Test
  void orchestrateAndGenerate_shouldReturnBadRequest_whenRequirementIsNull() {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("requirement", null);

    // Act
    Mono<ResponseEntity<Object>> result = controller.orchestrateAndGenerate(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(400, response.getStatusCode().value());
            })
        .verifyComplete();
  }

  @Test
  void orchestrateAndGenerate_shouldReturnCombinedResult_whenSuccessful() {
    // Arrange
    Map<String, Object> request = Map.of("requirement", "Build a mobile app");

    Map<String, String> decisions = Map.of("platform", "mobile", "framework", "flutter");
    Map<String, Object> generationContext = new HashMap<>(decisions);
    Map<String, Object> codeResult = Map.of("code", "generated_code");

    Map<String, Object> mockPipelineResult = new HashMap<>();
    mockPipelineResult.put("status", "COMPLETED");
    mockPipelineResult.put("requirement", "Build a mobile app");
    mockPipelineResult.put("decisions", decisions);
    mockPipelineResult.put("generationContext", generationContext);
    mockPipelineResult.put("generatedApp", codeResult);

    when(orchestrationService.runFullPipeline(eq("Build a mobile app"), any()))
        .thenReturn(Mono.just(mockPipelineResult));

    // Act
    Mono<ResponseEntity<Object>> result = controller.orchestrateAndGenerate(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(200, response.getStatusCode().value());
              @SuppressWarnings("unchecked")
              Map<String, Object> body = (Map<String, Object>) response.getBody();
              assertEquals("COMPLETED", body.get("status"));
              assertEquals("Build a mobile app", body.get("requirement"));
              assertTrue(body.containsKey("decisions"));
              assertTrue(body.containsKey("generationContext"));
              assertTrue(body.containsKey("generatedApp"));
            })
        .verifyComplete();
  }

  @Test
  void orchestrateAndGenerate_shouldHandleOrchestrationException() {
    // Arrange
    Map<String, Object> request = Map.of("requirement", "Build an app");
    when(orchestrationService.runFullPipeline(eq("Build an app"), any()))
        .thenReturn(Mono.error(new RuntimeException("Orchestration error")));

    // Act
    Mono<ResponseEntity<Object>> result = controller.orchestrateAndGenerate(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(500, response.getStatusCode().value());
              assertTrue(response.getBody().toString().contains("error"));
            })
        .verifyComplete();
  }

  @Test
  void health_shouldReturnUpStatus() {
    // Act
    Mono<ResponseEntity<Object>> result = controller.health();

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(200, response.getStatusCode().value());
              @SuppressWarnings("unchecked")
              Map<String, Object> body = (Map<String, Object>) response.getBody();
              assertEquals("UP", body.get("status"));
              assertEquals("AdaptiveAgentOrchestrator", body.get("service"));
              assertTrue(body.containsKey("components"));
            })
        .verifyComplete();
  }

  @Test
  void generateWithContext_shouldReturnBadRequest_whenContextIsNull() {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("context", null);

    // Act
    Mono<ResponseEntity<Object>> result = controller.generateWithContext(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(400, response.getStatusCode().value());
            })
        .verifyComplete();
  }

  @Test
  void generateWithContext_shouldReturnGeneratedCode_whenSuccessful() {
    // Arrange
    Map<String, String> decisions = Map.of("platform", "web");
    Map<String, Object> context = Map.of("decisions", decisions);
    Map<String, Object> request = Map.of("context", context);

    Map<String, Object> generatedCode = Map.of("files", List.of("main.js", "style.css"));
    when(codeGenerationService.generateFromContext(anyMap())).thenReturn(generatedCode);

    // Act
    Mono<ResponseEntity<Object>> result = controller.generateWithContext(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(200, response.getStatusCode().value());
              assertEquals(generatedCode, response.getBody());
            })
        .verifyComplete();
  }

  @Test
  void createPublishingPlan_shouldReturnBadRequest_whenPlatformIsNull() {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("platform", null);

    // Act
    Mono<ResponseEntity<Object>> result = controller.createPublishingPlan(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(400, response.getStatusCode().value());
              assertTrue(response.getBody().toString().contains("error"));
            })
        .verifyComplete();
  }

  @Test
  void createPublishingPlan_shouldReturnBadRequest_whenPlatformIsEmpty() {
    // Arrange
    Map<String, Object> request = Map.of("platform", "");

    // Act
    Mono<ResponseEntity<Object>> result = controller.createPublishingPlan(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(400, response.getStatusCode().value());
            })
        .verifyComplete();
  }

  @Test
  void createPublishingPlan_shouldReturnSuccess_whenValidPlatform() {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("platform", "android");
    request.put("config", new HashMap<String, String>());

    Map<String, String> mockPlan = Map.of("platform", "Android", "store", "Google Play Store");
    when(publishAgent.createPublishingPlan(eq("android"), anyMap())).thenReturn(mockPlan);

    // Act
    Mono<ResponseEntity<Object>> result = controller.createPublishingPlan(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(200, response.getStatusCode().value());
              @SuppressWarnings("unchecked")
              Map<String, Object> body = (Map<String, Object>) response.getBody();
              assertEquals("SUCCESS", body.get("status"));
              assertEquals("android", body.get("platform"));
              assertEquals(mockPlan, body.get("publishingPlan"));
            })
        .verifyComplete();
  }

  @Test
  void createPublishingPlan_shouldHandleException_andReturn500() {
    // Arrange
    Map<String, Object> request = Map.of("platform", "web");
    when(publishAgent.createPublishingPlan(anyString(), anyMap()))
        .thenThrow(new RuntimeException("Agent failure"));

    // Act
    Mono<ResponseEntity<Object>> result = controller.createPublishingPlan(request);

    // Assert
    StepVerifier.create(result)
        .consumeNextWith(
            response -> {
              assertEquals(500, response.getStatusCode().value());
              assertTrue(response.getBody().toString().contains("Publishing plan creation failed"));
            })
        .verifyComplete();
  }
}
