package com.supremeai.intelligence.healing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BuildResult.
 * Tests build result status, error logs, and stage information.
 */
class BuildResultTest {

    @Test
    void testConstructor_success() {
        BuildResult result = new BuildResult(true, "BUILD SUCCESSFUL", "ALL_STAGES");

        assertTrue(result.isSuccess());
        assertEquals("BUILD SUCCESSFUL", result.getErrorLogs());
        assertEquals("ALL_STAGES", result.getFailedStage());
    }

    @Test
    void testConstructor_failure() {
        BuildResult result = new BuildResult(false, "Compilation error at line 10", "COMPILATION");

        assertFalse(result.isSuccess());
        assertEquals("Compilation error at line 10", result.getErrorLogs());
        assertEquals("COMPILATION", result.getFailedStage());
    }

    @Test
    void testConstructor_nullErrorLogs() {
        BuildResult result = new BuildResult(false, null, "UNIT_TESTS");

        assertFalse(result.isSuccess());
        assertNull(result.getErrorLogs());
        assertEquals("UNIT_TESTS", result.getFailedStage());
    }

    @Test
    void testConstructor_emptyErrorLogs() {
        BuildResult result = new BuildResult(false, "", "LINTING");

        assertFalse(result.isSuccess());
        assertEquals("", result.getErrorLogs());
        assertEquals("LINTING", result.getFailedStage());
    }

    @Test
    void testConstructor_nullFailedStage() {
        BuildResult result = new BuildResult(true, "Success", null);

        assertTrue(result.isSuccess());
        assertNull(result.getFailedStage());
    }

    @Test
    void testGetters() {
        BuildResult result = new BuildResult(false, "Error details", "COMPILATION");

        assertFalse(result.isSuccess());
        assertEquals("Error details", result.getErrorLogs());
        assertEquals("COMPILATION", result.getFailedStage());
    }

    @Test
    void testVariousFailureStages() {
        BuildResult compilationFail = new BuildResult(false, "syntax error", "COMPILATION");
        assertFalse(compilationFail.isSuccess());

        BuildResult testFail = new BuildResult(false, "test assertion failed", "UNIT_TESTS");
        assertFalse(testFail.isSuccess());

        BuildResult lintFail = new BuildResult(false, "code style violation", "LINTING");
        assertFalse(lintFail.isSuccess());
    }

    @Test
    void testBuildResultEquality() {
        BuildResult r1 = new BuildResult(true, "OK", "ALL");
        BuildResult r2 = new BuildResult(true, "OK", "ALL");
        BuildResult r3 = new BuildResult(false, "FAIL", "COMPILATION");

        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
    }

    @Test
    void testBuildResultHashCode() {
        BuildResult r1 = new BuildResult(true, "OK", "ALL");
        BuildResult r2 = new BuildResult(true, "OK", "ALL");

        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void testBuildResultToString() {
        BuildResult result = new BuildResult(false, "Test error", "UNIT_TESTS");

        String str = result.toString();

        assertTrue(str.contains("false"));
        assertTrue(str.contains("Test error"));
        assertTrue(str.contains("UNIT_TESTS"));
    }

    @Test
    void testSuccessfulBuild_hasNullFailedStage() {
        BuildResult success = new BuildResult(true, "Success", null);

        assertNull(success.getFailedStage());
        assertTrue(success.isSuccess());
    }

    @Test
    void testFailureWithLongErrorMessage() {
        String longError = "Error: line 1: syntax error\nError: line 2: unexpected token\n" +
                          "Error: line 3: missing semicolon\nError: line 4: undeclared variable";
        BuildResult result = new BuildResult(false, longError, "COMPILATION");

        assertEquals(longError, result.getErrorLogs());
        assertFalse(result.isSuccess());
    }
}
