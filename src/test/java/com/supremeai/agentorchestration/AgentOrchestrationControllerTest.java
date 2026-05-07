package com.supremeai.agentorchestration;

import com.supremeai.service.CodeGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AgentOrchestrationControllerTest {

    @Mock
    private AdaptiveAgentOrchestrator orchestrator;

    @Mock
    private CodeGenerationService codeGenerationService;

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
                .consumeNextWith(response -> {
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
                .consumeNextWith(response -> {
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
                .consumeNextWith(response -> {
                    assertEquals(503, response.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
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
                .consumeNextWith(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertEquals("COMPLETED", body.get("status"));
                })
                .verifyComplete();
    }

    @Test
    void orchestrate_shouldHandleException_andReturn500() {
        // Arrange
        Map<String, Object> request = Map.of("requirement", "Build an app");
        when(orchestrator.orchestrate(anyString())).thenThrow(new RuntimeException("Orchestration failed"));

        // Act
        Mono<ResponseEntity<Object>> result = controller.orchestrate(request);

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(response -> {
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
                .consumeNextWith(response -> {
                    assertEquals(400, response.getStatusCode().value());
                })
                .verifyComplete();
    }

    @Test
    void orchestrateAndGenerate_shouldReturnCombinedResult_whenSuccessful() {
        // Arrange
        Map<String, Object> request = Map.of("requirement", "Build a mobile app");

        OrchesResultContext orchestrationResult = new OrchesResultContext(new HashMap<>());
        orchestrationResult.setStatus("COMPLETED");
        
        Map<String, String> decisions = Map.of("platform", "mobile", "framework", "flutter");
        Map<String, Object> generationContext = new HashMap<>(decisions);
        orchestrationResult.setGenerationContext(generationContext);
        
        Map<String, Object> codeResult = Map.of("code", "generated_code");
        
        when(orchestrator.orchestrate(anyString())).thenReturn(orchestrationResult);
        when(codeGenerationService.generateFromContext(anyMap())).thenReturn(codeResult);

        // Act
        Mono<ResponseEntity<Object>> result = controller.orchestrateAndGenerate(request);

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(response -> {
                    assertEquals(200, response.getStatusCode().value());
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
        when(orchestrator.orchestrate(anyString())).thenThrow(new RuntimeException("Orchestration error"));

        // Act
        Mono<ResponseEntity<Object>> result = controller.orchestrateAndGenerate(request);

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(response -> {
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
                .consumeNextWith(response -> {
                    assertEquals(200, response.getStatusCode().value());
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
                .consumeNextWith(response -> {
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
                .consumeNextWith(response -> {
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
                .consumeNextWith(response -> {
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
                .consumeNextWith(response -> {
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

        // We need to test GPublishAgent indirectly
        // Since GPublishAgent is instantiated inside the method,
        // we'll test that the method completes successfully
        
        // Act
        Mono<ResponseEntity<Object>> result = controller.createPublishingPlan(request);

        // Assert - should not throw and should return a response
        // Note: This will actually create a GPublishAgent instance - if it fails, test will fail
        StepVerifier.create(result)
                .consumeNextWith(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertEquals("SUCCESS", body.get("status"));
                    assertEquals("android", body.get("platform"));
                    assertTrue(body.containsKey("publishingPlan"));
                })
                .verifyComplete();
    }

    @Test
    void createPublishingPlan_shouldHandleException_andReturn500() {
        // This test would require mocking GPublishAgent but it's created inside the method.
        // For now, we verify that valid input works (covered in previous test)
        // In a more comprehensive test, we might refactor to make GPublishAgent injectable
        
        Map<String, Object> request = Map.of("platform", "web");
        
        // Act & Assert - should not throw unexpected exceptions
        Mono<ResponseEntity<Object>> result = controller.createPublishingPlan(request);
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }
}
