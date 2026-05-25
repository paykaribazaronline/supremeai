package com.supremeai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CodeValidationServiceTest {

    private CodeValidationService codeValidationService;

    @BeforeEach
    void setUp() {
        codeValidationService = new CodeValidationService();
    }

    @Test
    void validateReturnsValidForCorrectFiles() {
        Map<String, String> files = new HashMap<>();
        files.put("build.gradle.kts", """
            plugins {
                kotlin("jvm") version "1.9.0"
            }
            dependencies {
                implementation(kotlin("stdlib"))
            }
            """);
        files.put("src/main/java/com/example/generated/GeneratedAppApplication.java", """
            package com.example.generated;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            
            @SpringBootApplication
            public class GeneratedAppApplication {
                public static void main(String[] args) {
                    SpringApplication.run(GeneratedAppApplication.class, args);
                }
            }
            """);

        Map<String, Object> result = codeValidationService.validate(files);
        assertTrue((Boolean) result.get("valid"));
        assertEquals(0, ((java.util.List<String>) result.get("errors")).size());
    }

    @Test
    void validateDetectsMissingFiles() {
        Map<String, String> files = new HashMap<>();
        
        Map<String, Object> result = codeValidationService.validate(files);
        assertFalse((Boolean) result.get("valid"));
        assertTrue(((java.util.List<String>) result.get("errors")).size() > 0);
    }

    @Test
    void validateGradleSyntaxDetectsMissingPlugins() {
        String gradleContent = "dependencies {}";
        
        Map<String, Object> result = codeValidationService.validateGradleSyntax(gradleContent);
        assertFalse((Boolean) result.get("valid"));
    }

    @Test
    void validateJavaSyntaxDetectsBraceMismatch() {
        String javaContent = "public class Test {";
        Map<String, Object> result = codeValidationService.validateJavaSyntax(javaContent, "Test");
        assertFalse((Boolean) result.get("valid"));
    }

    @Test
    void validateJavaSyntaxReturnsValidForCorrectCode() {
        String javaContent = """
            public class Test {
                public static void main(String[] args) {}
            }
            """;
        Map<String, Object> result = codeValidationService.validateJavaSyntax(javaContent, "Test");
        assertTrue((Boolean) result.get("valid"));
    }

    @Test
    void validateJavaSyntaxHandlesEmptyContent() {
        Map<String, Object> result = codeValidationService.validateJavaSyntax("", "Test");
        assertFalse((Boolean) result.get("valid"));
    }

    @Test
    void writeToTempDirectoryCreatesFiles() throws IOException {
        Map<String, String> files = new HashMap<>();
        files.put("Test.java", "public class Test {}");
        files.put("config.properties", "key=value");

        String tempDir = codeValidationService.writeToTempDirectory(files, "test-app");
        
        assertNotNull(tempDir);
        assertTrue(tempDir.contains("supremeai-test-app-"));
        
        java.nio.file.Path testFile = java.nio.file.Path.of(tempDir, "Test.java");
        assertTrue(java.nio.file.Files.exists(testFile));
    }
}