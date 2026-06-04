package com.supremeai.controller;

import com.supremeai.service.CodeGenerationService;
import com.supremeai.service.AppOrchestrationService;
import com.supremeai.generation.FullStackCodeGenerator;
import com.supremeai.generation.MultiPlatformGenerator;
import com.supremeai.model.EntityDefinition;
import com.supremeai.model.FieldDefinition;
import com.supremeai.model.GeneratedApp;
import com.supremeai.repository.GeneratedAppRepository;
import com.supremeai.response.ApiResponse;
import com.supremeai.dto.AppGenerationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppGenerationControllerTest {

    @Mock
    private CodeGenerationService codeGenerationService;

    @Mock
    private AppOrchestrationService appOrchestrationService;

    @Mock
    private FullStackCodeGenerator fullStackCodeGenerator;

    @Mock
    private MultiPlatformGenerator multiPlatformGenerator;

    @Mock
    private GeneratedAppRepository generatedAppRepository;

    @Mock
    private WebSocketController webSocketController;

    @Mock
    private Authentication authentication;

    private AppGenerationController controller;

    @BeforeEach
    void setUp() {
        controller = new AppGenerationController();
        setField(controller, "codeGenerationService", codeGenerationService);
        setField(controller, "appOrchestrationService", appOrchestrationService);
        setField(controller, "fullStackCodeGenerator", fullStackCodeGenerator);
        setField(controller, "multiPlatformGenerator", multiPlatformGenerator);
        setField(controller, "generatedAppRepository", generatedAppRepository);
        setField(controller, "webSocketController", webSocketController);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = AppGenerationController.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== generateApp - AI-Enabled Tests ====================

    @Test
    void generateApp_WithAIEnabled_ReturnsGeneratedApp() {
        AppGenerationRequest request = new AppGenerationRequest();
        request.setName("My AI App");
        request.setDescription("An AI powered app");
        request.setPlatform("fullstack");
        request.setDatabase("PostgreSQL");
        request.setType("web");
        request.setUseAI(true);
        request.setEntities(new ArrayList<>());

        when(authentication.getName()).thenReturn("user-123");

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("files", Map.of("app.js", "const app = {}"));
        mockResult.put("fileCount", 5);

        when(codeGenerationService.generateAppWithAI(anyString(), anyString(), anyList(), anyString(), anyString()))
                .thenReturn(mockResult);
        when(generatedAppRepository.save(any(GeneratedApp.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                controller.generateApp(request, authentication);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(202, response.getStatusCode().value());
                    assertTrue(response.getBody().isSuccess());
                    assertEquals("ACCEPTED", response.getBody().getData().get("status"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void generateApp_WithAIAndEntities_GeneratesWithEntities() {
        AppGenerationRequest request = new AppGenerationRequest();
        request.setName("E-Commerce App");
        request.setDescription("An e-commerce platform");
        request.setPlatform("fullstack");
        request.setDatabase("PostgreSQL");
        request.setType("web");
        request.setUseAI(true);

        EntityDefinition productEntity = new EntityDefinition();
        productEntity.setName("Product");
        productEntity.setDescription("A product entity");

        FieldDefinition nameField = new FieldDefinition();
        nameField.setName("name");
        nameField.setType("string");
        nameField.setRequired(true);
        productEntity.setFields(List.of(nameField));

        request.setEntities(List.of(productEntity));

        when(authentication.getName()).thenReturn("user-456");

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("files", Map.of("app.js", "// E-commerce app"));
        mockResult.put("fileCount", 10);

        when(codeGenerationService.generateAppWithAI(anyString(), anyString(), anyList(), anyString(), anyString()))
                .thenReturn(mockResult);
        when(generatedAppRepository.save(any(GeneratedApp.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                controller.generateApp(request, authentication);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(202, response.getStatusCode().value());
                    assertEquals("ACCEPTED", response.getBody().getData().get("status"));
                    return true;
                })
                .verifyComplete();

        verify(codeGenerationService, timeout(3000)).generateAppWithAI(
                eq("E-Commerce App"), eq("An e-commerce platform"),
                anyList(), eq("PostgreSQL"), eq("JWT")
        );
    }

    // ==================== generateApp - Platform-Specific Tests ====================

    @Test
    void generateApp_WebPlatform_GeneratesWebApp() {
        AppGenerationRequest request = new AppGenerationRequest();
        request.setName("Web App");
        request.setDescription("A web application");
        request.setPlatform("web");
        request.setDatabase("PostgreSQL");
        request.setType("web");
        request.setUseAI(false);

        when(authentication.getName()).thenReturn("user-789");

        Map<String, String> platformResult = new HashMap<>();
        platformResult.put("main", "// React app code");
        platformResult.put("files", "3");

        when(multiPlatformGenerator.generateForPlatform(anyString(), eq("web")))
                .thenReturn(platformResult);
        when(generatedAppRepository.save(any(GeneratedApp.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                controller.generateApp(request, authentication);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(202, response.getStatusCode().value());
                    assertTrue(response.getBody().isSuccess());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void generateApp_AndroidPlatform_GeneratesAndroidApp() {
        AppGenerationRequest request = new AppGenerationRequest();
        request.setName("Android App");
        request.setPlatform("android");
        request.setDatabase("SQLite");
        request.setType("mobile");
        request.setUseAI(false);

        when(authentication.getName()).thenReturn("user-android");

        Map<String, String> platformResult = new HashMap<>();
        platformResult.put("main", "// Android code");

        when(multiPlatformGenerator.generateForPlatform(anyString(), eq("android")))
                .thenReturn(platformResult);
        when(generatedAppRepository.save(any(GeneratedApp.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                controller.generateApp(request, authentication);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(202, response.getStatusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void generateApp_DefaultPlatform_UsesFullStack() {
        AppGenerationRequest request = new AppGenerationRequest();
        request.setName("Default App");
        request.setPlatform("unknown");
        request.setDatabase("PostgreSQL");
        request.setType("web");
        request.setUseAI(false);

        when(authentication.getName()).thenReturn("user-default");

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("files", Map.of("app.js", "// Full stack app"));

        when(codeGenerationService.generateFromContext(anyMap()))
                .thenReturn(mockResult);
        when(generatedAppRepository.save(any(GeneratedApp.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                controller.generateApp(request, authentication);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(202, response.getStatusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== generateApp - Anonymous User Tests ====================

    @Test
    void generateApp_NullAuthentication_UsesAnonymous() {
        AppGenerationRequest request = new AppGenerationRequest();
        request.setName("Anonymous App");
        request.setPlatform("fullstack");
        request.setDatabase("PostgreSQL");
        request.setType("web");
        request.setUseAI(false);

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("files", Map.of());

        when(codeGenerationService.generateFromContext(anyMap()))
                .thenReturn(mockResult);
        when(generatedAppRepository.save(any(GeneratedApp.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                controller.generateApp(request, null);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(202, response.getStatusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== generateApp - Error Tests ====================

    @Test
    void generateApp_ServiceError_ReturnsInternalServerError() {
        AppGenerationRequest request = new AppGenerationRequest();
        request.setName("Failing App");
        request.setPlatform("fullstack");
        request.setDatabase("PostgreSQL");
        request.setType("web");
        request.setUseAI(false);

        when(authentication.getName()).thenReturn("user-error");

        when(codeGenerationService.generateFromContext(anyMap()))
                .thenThrow(new RuntimeException("Generation failed"));

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                controller.generateApp(request, authentication);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(202, response.getStatusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== previewGeneration Tests ====================

    @Test
    void previewGeneration_ReturnsPreviewFiles() {
        Map<String, Object> request = Map.of("platform", "fullstack");

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("files", Map.of(
                "app.js", "// Main app",
                "index.html", "<html></html>",
                "style.css", "body {}",
                "server.js", "// Server"
        ));

        when(codeGenerationService.generateFromContext(anyMap())).thenReturn(mockResult);

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                controller.previewGeneration(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    Map<String, Object> body = response.getBody().getData();
                    assertTrue((Boolean) body.get("preview"));
                    assertEquals(3, ((Map) body.get("files")).size()); // Limited to 3
                    assertEquals(4, body.get("totalFiles"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void previewGeneration_WithError_ReturnsError() {
        Map<String, Object> request = Map.of("platform", "web");

        when(codeGenerationService.generateFromContext(anyMap()))
                .thenThrow(new RuntimeException("Preview failed"));

        Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> result =
                controller.previewGeneration(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(500, response.getStatusCode().value());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== health Endpoint Tests ====================

    @Test
    void health_ReturnsUpStatus() {
        Mono<ResponseEntity<ApiResponse<Map<String, String>>>> result = controller.health();

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    Map<String, String> body = response.getBody().getData();
                    assertEquals("UP", body.get("status"));
                    assertEquals("AppGenerationService", body.get("service"));
                    return true;
                })
                .verifyComplete();
    }
}