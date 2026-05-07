package com.supremeai.intelligence.vision;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RippleEffectPredictor.
 * Tests ripple effect detection for entity changes, service modifications, and interface updates.
 */
class RippleEffectPredictorTest {

    private final RippleEffectPredictor predictor = new RippleEffectPredictor();

    @Test
    void testAnalyzeCodeChange_entityColumnModification() {
        String filePath = "src/main/java/com/example/UserEntity.java";
        String oldCode = "private String name;";
        String newCode = "@Column\nprivate String userName;";

        RippleWarning warning = predictor.analyzeCodeChange(filePath, oldCode, newCode);

        assertNotNull(warning);
        assertTrue(warning.hasWarnings());
        String alert = warning.generateAlertMessage();
        assertTrue(alert.contains("Ripple Effect"));
        assertTrue(alert.contains("Database Schema") || alert.contains("DTO") || alert.contains("Frontend"));
    }

    @Test
    void testAnalyzeCodeChange_entityFileWithoutColumnChange() {
        String filePath = "UserEntity.java";
        String code = "public class User {}";

        RippleWarning warning = predictor.analyzeCodeChange(filePath, code, code);

        assertNull(warning);
    }

    @Test
    void testAnalyzeCodeChange_serviceInterfaceModification() {
        String filePath = "UserService.java";
        String oldCode = "public interface UserService {}";
        String newCode = "public interface UserService { void newMethod(); }";

        RippleWarning warning = predictor.analyzeCodeChange(filePath, oldCode, newCode);

        assertNotNull(warning);
        assertTrue(warning.hasWarnings());
        String alert = warning.generateAlertMessage();
        assertTrue(alert.contains("Implementations") || alert.contains("Test"));
    }

    @Test
    void testAnalyzeCodeChange_noChanges() {
        String filePath = "SomeFile.java";
        String oldCode = "code";
        String newCode = "code";

        RippleWarning warning = predictor.analyzeCodeChange(filePath, oldCode, newCode);

        assertNull(warning);
    }

    @Test
    void testAnalyzeCodeChange_dtoChange() {
        String filePath = "UserDTO.java";
        String oldCode = "public class UserDTO { private String name; }";
        String newCode = "public class UserDTO { private String fullName; }";

        RippleWarning warning = predictor.analyzeCodeChange(filePath, oldCode, newCode);

        // DTO is not Entity or Service, so no warning from current logic
        assertNull(warning);
    }

    @Test
    void testAnalyzeCodeChange_serviceImplementationChange() {
        String filePath = "UserServiceImpl.java";
        String oldCode = "class Impl {}";
        String newCode = "class Impl { void newMethod() {} }";

        RippleWarning warning = predictor.analyzeCodeChange(filePath, oldCode, newCode);

        assertNull(warning); // Only interface changes trigger warning
    }

    @Test
    void testAnalyzeCodeChange_modelFileWithColumn() {
        String filePath = "ProductModel.java";
        String oldCode = "";
        String newCode = "@Column\nprivate String price;";

        RippleWarning warning = predictor.analyzeCodeChange(filePath, oldCode, newCode);

        assertNotNull(warning);
    }

    @Test
    void testRippleWarning_hasWarnings_empty() {
        RippleWarning warning = new RippleWarning("test.java");

        assertFalse(warning.hasWarnings());
    }

    @Test
    void testRippleWarning_hasWarnings_nonEmpty() {
        RippleWarning warning = new RippleWarning("test.java");
        warning.addAffectedArea("Database", "Migration needed");

        assertTrue(warning.hasWarnings());
    }

    @Test
    void testRippleWarning_addMultipleAreas() {
        RippleWarning warning = new RippleWarning("Entity.java");
        warning.addAffectedArea("Database", "Migrate");
        warning.addAffectedArea("DTO", "Update");
        warning.addAffectedArea("Frontend", "Change");

        assertTrue(warning.hasWarnings());
        assertTrue(warning.generateAlertMessage().contains("Database"));
        assertTrue(warning.generateAlertMessage().contains("DTO"));
        assertTrue(warning.generateAlertMessage().contains("Frontend"));
    }

    @Test
    void testRippleWarning_generateAlertMessage_format() {
        RippleWarning warning = new RippleWarning("UserEntity.java");
        warning.addAffectedArea("Database Schema", "Flyway migration needed");

        String alert = warning.generateAlertMessage();

        assertTrue(alert.startsWith("🚨 Supreme Vision Alert"));
        assertTrue(alert.contains("UserEntity.java"));
        assertTrue(alert.contains("⚠️ [Database Schema] Flyway migration needed"));
        assertTrue(alert.contains("Before you commit"));
    }

    @Test
    void testRippleWarning_messageIncludesCheckFiles() {
        RippleWarning warning = new RippleWarning("File.java");
        warning.addAffectedArea("Tests", "Update mocks");

        String alert = warning.generateAlertMessage();

        assertTrue(alert.contains("check these files"));
    }

    @Test
    void testAnalyzeCodeChange_caseInsensitiveExtension() {
        String filePath = "USERENTITY.JAVA";
        String oldCode = "";
        String newCode = "@Column\nprivate String field;";

        RippleWarning warning = predictor.analyzeCodeChange(filePath, oldCode, newCode);

        assertNotNull(warning);
    }

    @Test
    void testAnalyzeCodeChange_entityWithMultipleColumnChanges() {
        String filePath = "Entity.java";
        String oldCode = "private String a; private String b;";
        String newCode = "@Column private String a; @Column private String b;";

        RippleWarning warning = predictor.analyzeCodeChange(filePath, oldCode, newCode);

        assertNotNull(warning);
        String alert = warning.generateAlertMessage();
        assertTrue(alert.contains("Database Schema"));
        assertTrue(alert.contains("DTO"));
    }

    @Test
    void testAnalyzeCodeChange_nonEntityJavaFile() {
        String filePath = "Utils.java";
        String oldCode = "public class Utils {}";
        String newCode = "public class Utils { void helper() {} }";

        RippleWarning warning = predictor.analyzeCodeChange(filePath, oldCode, newCode);

        assertNull(warning);
    }

    @Test
    void testRippleWarning_nullChangedFile() {
        RippleWarning warning = new RippleWarning(null);
        warning.addAffectedArea("Test", "Issue");

        String alert = warning.generateAlertMessage();

        assertTrue(alert.contains("null"));
    }
}
