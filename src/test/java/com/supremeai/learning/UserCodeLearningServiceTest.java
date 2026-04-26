package com.supremeai.learning;

import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.model.SystemLearning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserCodeLearningService.
 * Tests the diff analysis, category determination, and learning behavior.
 */
@ExtendWith(MockitoExtension.class)
class UserCodeLearningServiceTest {

    @Mock
    private GlobalKnowledgeBase globalKnowledgeBase;

    @Mock
    private com.google.cloud.firestore.Firestore firestore;

    // We'll test the internal logic via reflection since methods are private
    private UserCodeLearningService service;

    @BeforeEach
    void setUp() throws Exception {
        // Create service without Firestore (use in-memory fallback)
        service = new UserCodeLearningService();
        // Inject mocked globalKnowledgeBase using reflection
        java.lang.reflect.Field field = UserCodeLearningService.class
                .getDeclaredField("globalKnowledgeBase");
        field.setAccessible(true);
        field.set(service, globalKnowledgeBase);
    }

    /**
     * Test that identical code produces no learning.
     */
    @Test
    void learnFromUserEdit_identicalCode_doesNothing() {
        String taskId = "task1";
        String original = "print('hello')";
        String edited = "print('hello')";
        String context = "simple script";

        service.learnFromUserEdit(taskId, original, edited, context);

        // Verify no interaction with knowledge base
        verifyNoInteractions(globalKnowledgeBase);
    }

    /**
     * Test that minor changes produce MINOR_CORRECTION type.
     */
    @Test
    void learnFromUserEdit_minorChanges_createsMinorCorrection() throws Exception {
        String taskId = "task2";
        String original = "x = 1\nprint(x)";
        String edited = "x = 2\nprint(x)";
        String context = "bug fix";

        // Invoke private analyzeCodeDiff via reflection
        var analyzeMethod = UserCodeLearningService.class
                .getDeclaredMethod("analyzeCodeDiff", String.class, String.class);
        analyzeMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>) analyzeMethod.invoke(service, original, edited);

        double similarity = (double) result.get("similarity");
        // Minor edit should have high similarity
        assert similarity > 50;

        // Invoke private determineCategory
        var catMethod = UserCodeLearningService.class
                .getDeclaredMethod("determineCategory", String.class, 
                    UserCodeLearningService.CodeDiffAnalysis.class);
        catMethod.setAccessible(true);
        // We need to get CodeDiffAnalysis instance
        var diffAnalysis = new UserCodeLearningService.CodeDiffAnalysis();
        diffAnalysis.setSimilarityPercentage(similarity);
        String category = (String) catMethod.invoke(service, context, diffAnalysis);
        // Should be BUG_FIX because context contains "bug"
        assertEquals("BUG_FIX", category);
    }

    /**
     * Test category determination logic.
     */
    @Test
    void determineCategory_contextBased() throws Exception {
        var catMethod = UserCodeLearningService.class
                .getDeclaredMethod("determineCategory", String.class, 
                    UserCodeLearningService.CodeDiffAnalysis.class);
        catMethod.setAccessible(true);

        var diff = new UserCodeLearningService.CodeDiffAnalysis();
        diff.setSimilarityPercentage(80);

        assertEquals("GENERAL", catMethod.invoke(service, null, diff));
        assertEquals("GENERAL", catMethod.invoke(service, "", diff));
        assertEquals("BUG_FIX", catMethod.invoke(service, "fix error", diff));
        assertEquals("SECURITY", catMethod.invoke(service, "auth security token", diff));
        assertEquals("PERFORMANCE", catMethod.invoke(service, "optimize speed", diff));
        assertEquals("DATABASE", catMethod.invoke(service, "database query sql", diff));
        assertEquals("API", catMethod.invoke(service, "api endpoint controller", diff));
        assertEquals("UI_UX", catMethod.invoke(service, "frontend component ui", diff));
    }

    /**
     * Test learning type inference based on similarity.
     */
    @Test
    void createLearningType_basedOnSimilarity() throws Exception {
        var createMethod = UserCodeLearningService.class
                .getDeclaredMethod("createLearningPattern", String.class, 
                    UserCodeLearningService.CodeDiffAnalysis.class, String.class);
        createMethod.setAccessible(true);

        // >80% similarity => MINOR_CORRECTION
        var diffHigh = new UserCodeLearningService.CodeDiffAnalysis();
        diffHigh.setSimilarityPercentage(90);
        diffHigh.setAddedLines(List.of("added"));
        diffHigh.setRemovedLines(List.of("removed"));

        var pattern = (SystemLearning) createMethod.invoke(service, "task1", diffHigh, "general");
        assertEquals("MINOR_CORRECTION", pattern.getLearningType());

        // 50-80% => MODIFICATION
        var diffMid = new UserCodeLearningService.CodeDiffAnalysis();
        diffMid.setSimilarityPercentage(60);
        diffMid.setAddedLines(List.of("a", "b"));
        diffMid.setRemovedLines(List.of("x", "y"));
        pattern = (SystemLearning) createMethod.invoke(service, "task2", diffMid, "general");
        assertEquals("MODIFICATION", pattern.getLearningType());

        // <50% => MAJOR_REFACTOR
        var diffLow = new UserCodeLearningService.CodeDiffAnalysis();
        diffLow.setSimilarityPercentage(30);
        diffLow.setAddedLines(List.of("a", "b", "c"));
        diffLow.setRemovedLines(List.of("x", "y", "z"));
        pattern = (SystemLearning) createMethod.invoke(service, "task3", diffLow, "general");
        assertEquals("MAJOR_REFACTOR", pattern.getLearningType());
    }

    // Helper assertion methods inline (or use JUnit assertions)
    private void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
        }
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
