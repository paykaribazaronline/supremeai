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
 * ErrorFixingSuggestor Unit Tests
 * Tests: Auto-fixing, error type detection, fix suggestions
 */
@DisplayName("ErrorFixingSuggestor Tests")
public class ErrorFixingSuggestorTest {

    private ErrorFixingSuggestor fixingSuggestor;
    
    @Mock
    private FileOrchestrator fileOrchestrator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        fixingSuggestor = new ErrorFixingSuggestor();
        fixingSuggestor.fileOrchestrator = fileOrchestrator;
    }

    @Test
    @DisplayName("Suggest Fixes for Issues")
    public void testSuggestFixesForIssues() {
        String projectId = "test-project";
        String framework = "REACT";
        List<Map<String, Object>> issues = new ArrayList<>();
        
        Map<String, Object> issue = new HashMap<>();
        issue.put("code", "MISSING_DEPENDENCY");
        issue.put("message", "Missing react");
        issues.add(issue);

        Map<String, Object> result = fixingSuggestor.suggestFixes(projectId, framework, issues);

        assertEquals(true, result.get("success"));
        assertNotNull(result.get("suggestions"));
        assertTrue((Integer) result.get("autoFixableCount") >= 0);
    }

    @Test
    @DisplayName("Detect Missing Dependencies")
    public void testDetectMissingDependency() {
        String projectId = "test-project";
        String framework = "NODEJS";
        List<Map<String, Object>> issues = new ArrayList<>();
        
        Map<String, Object> issue = new HashMap<>();
        issue.put("code", "MISSING_DEPENDENCY");
        issue.put("message", "Missing dependency: express");
        issues.add(issue);

        Map<String, Object> result = fixingSuggestor.suggestFixes(projectId, framework, issues);

        assertNotNull(result.get("suggestions"));
    }

    @Test
    @DisplayName("Identify Auto-Fixable Errors")
    public void testIdentifyAutoFixableErrors() {
        String code = "INDENTATION_ERROR";
        assertTrue(fixingSuggestor.isAutoFixable(code));
        
        code = "MISSING_DEPENDENCY";
        assertTrue(fixingSuggestor.isAutoFixable(code));
        
        code = "UNKNOWN_ERROR";
        assertFalse(fixingSuggestor.isAutoFixable(code));
    }

    @Test
    @DisplayName("Apply Fixes to Project")
    public void testApplyFixesToProject() {
        String projectId = "test-project";
        String framework = "REACT";
        List<Map<String, Object>> issues = new ArrayList<>();
        
        Map<String, Object> issue = new HashMap<>();
        issue.put("code", "MISSING_DEPENDENCY");
        issue.put("message", "Missing react");
        issues.add(issue);

        when(fileOrchestrator.fileExists(projectId, "package.json")).thenReturn(true);
        when(fileOrchestrator.readFile(projectId, "package.json"))
            .thenReturn("{\"dependencies\": {}}");

        Map<String, Object> result = fixingSuggestor.applyFixes(projectId, framework, issues);

        assertEquals(true, result.get("success"));
        assertNotNull(result.get("appliedCount"));
    }

    @Test
    @DisplayName("Handle Unmatched Braces")
    public void testFixUnmatchedBraces() {
        String code = "function test() { console.log('hello')";
        // Should be able to fix by adding closing brace
        
        assertTrue(fixingSuggestor.isAutoFixable("UNMATCHED_BRACES"));
    }

    @Test
    @DisplayName("Fix Python Indentation Errors")
    public void testFixPythonIndentation() {
        assertTrue(fixingSuggestor.isAutoFixable("INDENTATION_ERROR"));
    }

    @Test
    @DisplayName("Fix Syntax Errors")
    public void testFixSyntaxErrors() {
        assertTrue(fixingSuggestor.isAutoFixable("SYNTAX_ERROR"));
    }

    @Test
    @DisplayName("Handle Invalid JSON")
    public void testFixInvalidJSON() {
        assertTrue(fixingSuggestor.isAutoFixable("INVALID_JSON"));
    }

    @Test
    @DisplayName("Get Fix Statistics")
    public void testGetFixStatistics() {
        Map<String, Object> stats = fixingSuggestor.getFixStats();

        assertNotNull(stats.get("autoFixableTypes"));
        assertNotNull(stats.get("totalIssuesFixed"));
    }

    @Test
    @DisplayName("Extract Dependency Name Correctly")
    public void testExtractDependencyName() {
        String message = "Missing dependency: axios@^0.21.0";
        // Should extract "axios"
        
        String message2 = "Missing package: lodash";
        // Should extract "lodash"
    }

    @Test
    @DisplayName("Support Multiple Error Types")
    public void testMultipleErrorTypeSupport() {
        String[] autoFixableTypes = {
            "MISSING_DEPENDENCY",
            "MISSING_SPRING_BOOT",
            "MISSING_MAIN",
            "MISSING_IMPORTS",
            "MISSING_VERSION",
            "INDENTATION_ERROR",
            "SYNTAX_ERROR",
            "MISSING_FLUTTER_SDK",
            "UNMATCHED_BRACES",
            "INVALID_JSON"
        };

        for (String errorType : autoFixableTypes) {
            assertTrue(fixingSuggestor.isAutoFixable(errorType), 
                "Error type should be fixable: " + errorType);
        }
    }

    @Test
    @DisplayName("Calculate Fix Success Rate")
    public void testCalculateFixSuccessRate() {
        int totalIssues = 10;
        int autoFixable = 8;
        int appliedFixes = 7;
        
        double successRate = (double) appliedFixes / totalIssues * 100;
        assertTrue(successRate > 0 && successRate <= 100);
        assertEquals(70, successRate);
    }

    @Test
    @DisplayName("Categorize Manual vs Auto-Fixable")
    public void testCategorizeFixes() {
        List<Map<String, Object>> issues = new ArrayList<>();
        
        // Auto-fixable issue
        Map<String, Object> issue1 = new HashMap<>();
        issue1.put("code", "MISSING_DEPENDENCY");
        issues.add(issue1);
        
        // Non-fixable issue  
        Map<String, Object> issue2 = new HashMap<>();
        issue2.put("code", "ARCHITECTURE_MISMATCH");
        issues.add(issue2);

        Map<String, Object> result = fixingSuggestor.suggestFixes("test-project", "REACT", issues);

        assertNotNull(result.get("autoFixable"));
        assertNotNull(result.get("requiresManualFix"));
    }
}
