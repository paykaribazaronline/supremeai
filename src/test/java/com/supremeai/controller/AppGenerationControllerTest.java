package com.supremeai.controller;

import com.supremeai.dto.AppGenerationRequest;
import com.supremeai.generation.FullStackCodeGenerator;
import com.supremeai.generation.MultiPlatformGenerator;
import com.supremeai.service.CodeGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppGenerationControllerTest {

    @Mock
    private CodeGenerationService codeGenerationService;

    @Mock
    private FullStackCodeGenerator fullStackCodeGenerator;

    @Mock
    private MultiPlatformGenerator multiPlatformGenerator;

    private AppGenerationController controller;

    @BeforeEach
    void setUp() {
        controller = new AppGenerationController();
        // Use reflection to set private fields
        try {
            java.lang.reflect.Field codeGenField = AppGenerationController.class.getDeclaredField("codeGenerationService");
            codeGenField.setAccessible(true);
            codeGenField.set(controller, codeGenerationService);

            java.lang.reflect.Field fullStackField = AppGenerationController.class.getDeclaredField("fullStackCodeGenerator");
            fullStackField.setAccessible(true);
            fullStackField.set(controller, fullStackCodeGenerator);

            java.lang.reflect.Field multiPlatformField = AppGenerationController.class.getDeclaredField("multiPlatformGenerator");
            multiPlatformField.setAccessible(true);
            multiPlatformField.set(controller, multiPlatformGenerator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void generateApp_shouldUseAIServiceWhenUseAIEnabled() {
        // Given
        AppGenerationRequest request = new AppGenerationRequest();
        request.setName("TestApp");
        request.setDescription("A test application");
        request.setPlatform("fullstack");
        request.setDatabase("postgresql");
        request.setUseAI(true);

        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("appName", "TestApp");
        serviceResult.put("files", new HashMap<String, String>());
        serviceResult.put("fileCount", 10);
        serviceResult.put("entities", 1);

        when(codeGenerationService.generateAppWithAI(
                eq("TestApp"), eq("A test application"), anyList(), eq("postgresql"), eq("JWT")
        )).thenReturn(serviceResult);

        // When
        ResponseEntity<Map<String, Object>> response = controller.generateApp(request);

        // Then
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> result = response.getBody();
        assertNotNull(result);
        assertEquals("TestApp", result.get("name"));
        assertEquals("GENERATED", result.get("status"));
        assertEquals("A test application", result.get("description"));
        assertEquals("fullstack", result.get("platform"));

        verify(codeGenerationService).generateAppWithAI(
                eq("TestApp"), eq("A test application"), anyList(), eq("postgresql"), eq("JWT")
        );
        verify(codeGenerationService, never()).generateFromContext(any());
        verify(multiPlatformGenerator, never()).generateForPlatform(anyString(), anyString());
    }

    @Test
    void generateApp_shouldUseFullStackGeneratorForFullstackPlatform() {
        // Given
        AppGenerationRequest request = new AppGenerationRequest();
        request.setName("FullStackApp");
        request.setDescription("Full stack application");
        request.setPlatform("fullstack");
        request.setDatabase("mysql");
        request.setUseAI(false);

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("files", new HashMap<String, String>());
        expectedResult.put("decisions", new HashMap<String, String>());

        when(codeGenerationService.generateFromContext(anyMap())).thenReturn(expectedResult);

        // When
        ResponseEntity<Map<String, Object>> response = controller.generateApp(request);

        // Then
        assertEquals(200, response.getStatusCode().value());
        verify(codeGenerationService).generateFromContext(anyMap());
        verify(multiPlatformGenerator, never()).generateForPlatform(anyString(), anyString());
    }

    @Test
    void generateApp_shouldUseMultiPlatformGeneratorForAndroid() {
        // Given
        AppGenerationRequest request = new AppGenerationRequest();
        request.setName("AndroidApp");
        request.setDescription("Android application");
        request.setPlatform("android");
        request.setUseAI(false);

        Map<String, String> platformResult = new HashMap<>();
        platformResult.put("code", "Generated Android code");
        platformResult.put("structure", "Android project structure");

        when(multiPlatformGenerator.generateForPlatform("Android application", "android"))
                .thenReturn(platformResult);

        // When
        ResponseEntity<Map<String, Object>> response = controller.generateApp(request);

        // Then
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> result = response.getBody();
        assertNotNull(result);
        assertEquals("Generated Android code", result.get("code"));
        assertEquals("Android project structure", result.get("structure"));
        assertNotNull(result.get("decisions"));

        verify(multiPlatformGenerator).generateForPlatform("Android application", "android");
        verify(codeGenerationService, never()).generateFromContext(anyMap());
    }

    @Test
    void generateApp_shouldHandleExceptionsGracefully() {
        // Given
        AppGenerationRequest request = new AppGenerationRequest();
        request.setName("FailingApp");

        when(codeGenerationService.generateFromContext(anyMap()))
                .thenThrow(new RuntimeException("Generation failed"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.generateApp(request);

        // Then
        assertEquals(500, response.getStatusCode().value());
        Map<String, Object> result = response.getBody();
        assertNotNull(result);
        assertEquals("ERROR", result.get("status"));
        assertTrue(result.get("message").toString().contains("Generation failed"));
    }

    @Test
    void health_shouldReturnServiceHealth() {
        // When
        ResponseEntity<Map<String, String>> response = controller.health();

        // Then
        assertEquals(200, response.getStatusCode().value());
        Map<String, String> health = response.getBody();
        assertNotNull(health);
        assertEquals("UP", health.get("status"));
        assertEquals("AppGenerationService", health.get("service"));
    }

    @Test
    void previewGeneration_shouldReturnLimitedFilePreview() {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("platform", "fullstack");

        Map<String, String> fullFiles = new HashMap<>();
        fullFiles.put("pom.xml", "<xml>POM content</xml>");
        fullFiles.put("src/main/java/App.java", "public class App {}");
        fullFiles.put("src/main/resources/app.properties", "app.settings=value");
        fullFiles.put("Dockerfile", "FROM java:8");
        fullFiles.put("README.md", "# README");

        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("files", fullFiles);
        serviceResult.put("decisions", new HashMap<String, String>());

        when(codeGenerationService.generateFromContext(anyMap())).thenReturn(serviceResult);

        // When
        ResponseEntity<Map<String, Object>> response = controller.previewGeneration(request);

        // Then
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> result = response.getBody();
        assertNotNull(result);
        assertEquals(true, result.get("preview"));
        assertEquals(5, result.get("totalFiles"));

        @SuppressWarnings("unchecked")
        Map<String, String> previewFiles = (Map<String, String>) result.get("files");
        assertNotNull(previewFiles);
        assertTrue(previewFiles.size() <= 3); // Should be limited to 3 files
    }

    @Test
    void previewGeneration_shouldHandleExceptions() {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("platform", "fullstack");

        when(codeGenerationService.generateFromContext(anyMap()))
                .thenThrow(new RuntimeException("Preview failed"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.previewGeneration(request);

        // Then
        assertEquals(500, response.getStatusCode().value());
        Map<String, Object> result = response.getBody();
        assertNotNull(result);
        assertEquals("ERROR", result.get("status"));
        assertTrue(result.get("message").toString().contains("Preview failed"));
    }
}