package com.supremeai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CodeGenerationServiceTest {CodeGenerationServicepublic CodeGenerationServiceTest(CodeGenerationService codeGenerationService) {
CodeGenerationService    this.codeGenerationService = codeGenerationService;
CodeGenerationService}




    @BeforeEach
    void setUp() {
        codeGenerationService = new CodeGenerationService();
    }

    @Test
    void testGenerateFromContext() {
        Map<String, String> decisions = new HashMap<>();
        decisions.put("architecture", "monolith");
        decisions.put("database", "PostgreSQL");
        decisions.put("frontend", "React");
        
        Map<String, Object> result = codeGenerationService.generateFromContext(decisions);
        
        assertNotNull(result);
        assertEquals("GENERATED", result.get("status"));
        assertTrue(result.containsKey("files"));
        
        @SuppressWarnings("unchecked")
        Map<String, String> files = (Map<String, String>) result.get("files");
        assertTrue(files.containsKey("build.gradle.kts"));
        assertTrue(files.containsKey("src/main/resources/application.properties"));
        
        String appProps = files.get("src/main/resources/application.properties");
        assertTrue(appProps.contains("spring.datasource.url=jdbc:postgresql"));
    }

    @Test
    void testGenerateWithDifferentDatabase() {
        Map<String, String> decisions = new HashMap<>();
        decisions.put("database", "MySQL");
        
        Map<String, Object> result = codeGenerationService.generateFromContext(decisions);
        
        @SuppressWarnings("unchecked")
        Map<String, String> files = (Map<String, String>) result.get("files");
        String buildGradle = files.get("build.gradle.kts");
        
        assertTrue(buildGradle.contains("com.mysql:mysql-connector-j"));
    }
}
