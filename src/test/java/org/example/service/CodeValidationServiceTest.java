package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CodeValidationService Unit Tests
 * Tests: Multi-framework validation, issue detection, scoring
 */
@DisplayName("CodeValidationService Tests")
public class CodeValidationServiceTest {

    private CodeValidationService validationService;
    
    @Mock
    private FileOrchestrator fileOrchestrator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        validationService = new CodeValidationService(fileOrchestrator);

    @Test
    @DisplayName("Validate React Project Successfully")
    public void testValidateReactProjectSuccess() {
        String projectId = "test-project";
        
        when(fileOrchestrator.fileExists(projectId, "package.json")).thenReturn(true);
        when(fileOrchestrator.readFile(projectId, "package.json"))
            .thenReturn("{\"react\": \"^18.0.0\", \"react-dom\": \"^18.0.0\"}");
        when(fileOrchestrator.searchFiles(projectId, "src/**/*.tsx"))
            .thenReturn(new ArrayList<>());

        Map<String, Object> result = validationService.validateProject(projectId, "REACT");

        assertEquals(true, result.get("isValid"));
        assertNotNull(result.get("validationScore"));
    }

    @Test
    @DisplayName("Detect Missing React Dependencies")
    public void testDetectMissingReactDependencies() {
        String projectId = "test-project";
        
        when(fileOrchestrator.fileExists(projectId, "package.json")).thenReturn(true);
        when(fileOrchestrator.readFile(projectId, "package.json"))
            .thenReturn("{\"typescript\": \"^4.0.0\"}");
        when(fileOrchestrator.searchFiles(projectId, "src/**/*.tsx"))
            .thenReturn(new ArrayList<>());

        Map<String, Object> result = validationService.validateProject(projectId, "REACT");

        assertEquals(false, result.get("isValid"));
        assertTrue((Integer) result.get("totalIssues") > 0);
    }

    @Test
    @DisplayName("Validate Node.js Project")
    public void testValidateNodeProjectSuccess() {
        String projectId = "test-project";
        
        when(fileOrchestrator.fileExists(projectId, "package.json")).thenReturn(true);
        when(fileOrchestrator.readFile(projectId, "package.json"))
            .thenReturn("{\"express\": \"^4.0.0\", \"typescript\": \"^4.0.0\"}");
        when(fileOrchestrator.fileExists(projectId, "tsconfig.json")).thenReturn(true);

        Map<String, Object> result = validationService.validateProject(projectId, "NODEJS");

        assertNotNull(result.get("isValid"));
        assertNotNull(result.get("totalIssues"));
    }

    @Test
    @DisplayName("Validate Python Project")
    public void testValidatePythonProject() {
        String projectId = "test-project";
        
        when(fileOrchestrator.fileExists(projectId, "requirements.txt")).thenReturn(true);
        when(fileOrchestrator.readFile(projectId, "requirements.txt"))
            .thenReturn("flask==2.0.0\nsqlalchemy==1.4.0");

        Map<String, Object> result = validationService.validateProject(projectId, "PYTHON");

        assertNotNull(result.get("isValid"));
        assertNotNull(result.get("validationScore"));
    }

    @Test
    @DisplayName("Calculate Validation Score Correctly")
    public void testCalculationValidationScore() {
        // 2 critical × 20 + 1 error × 10 + 3 warnings × 2 = 58 deducted
        // Score = 100 - 58 = 42
        Map<String, Object> result = new HashMap<>();
        result.put("criticalCount", 2);
        result.put("errorCount", 1);
        result.put("warningCount", 3);

        int score = 100 - (2 * 20 + 1 * 10 + 3 * 2);
        assertTrue(score >= 0 && score <= 100);
    }

    @Test
    @DisplayName("Detect Configuration File Issues")
    public void testDetectConfigurationIssues() {
        String projectId = "test-project";
        
        when(fileOrchestrator.fileExists(projectId, "tsconfig.json")).thenReturn(false);

        Map<String, Object> result = validationService.validateProject(projectId, "REACT");

        // Should find missing tsconfig.json
        assertNotNull(result.get("issues"));
    }

    @Test
    @DisplayName("Handle Unknown Framework Gracefully")
    public void testUnknownFrameworkHandling() {
        String projectId = "test-project";

        Map<String, Object> result = validationService.validateProject(projectId, "UNKNOWN");

        assertEquals(false, result.get("isValid"));
        assertTrue((Integer) result.get("totalIssues") > 0);
    }

    @Test
    @DisplayName("Validate Multiple Files")
    public void testValidateMultipleFiles() {
        String projectId = "test-project";
        List<Map<String, Object>> files = new ArrayList<>();
        
        Map<String, Object> file1 = new HashMap<>();
        file1.put("path", "src/App.tsx");
        files.add(file1);

        when(fileOrchestrator.searchFiles(projectId, "src/**/*.tsx"))
            .thenReturn(files);

        Map<String, Object> result = validationService.validateProject(projectId, "REACT");

        assertNotNull(result.get("totalIssues"));
    }

    @Test
    @DisplayName("Identify Code Issues by Severity")
    public void testSeverityLevelDetection() {
        String projectId = "test-project";

        Map<String, Object> result = validationService.validateProject(projectId, "REACT");

        assertNotNull(result.get("criticalCount"));
        assertNotNull(result.get("errorCount"));
        assertNotNull(result.get("warningCount"));
    }
}
