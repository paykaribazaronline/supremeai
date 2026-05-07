package com.supremeai.intelligence.profiling;

import com.supremeai.provider.AIProviderType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AIProfiler.
 * Tests AI performance recording, best AI selection, and fallback behavior.
 */
class AIProfilerTest {

    @Test
    void testRecordPerformance_newCategoryCreatesProfile() {
        AIProfiler profiler = new AIProfiler();

        profiler.recordPerformance("SQL_FIX", AIProviderType.GROQ_LLAMA3, true, 150);

        AIProviderType best = profiler.getBestAIForTask("SQL_FIX");

        assertEquals(AIProviderType.GROQ_LLAMA3, best);
    }

    @Test
    void testRecordPerformance_multipleProviders() {
        AIProfiler profiler = new AIProfiler();

        profiler.recordPerformance("JAVA_GENERATION", AIProviderType.GROQ_LLAMA3, true, 200);
        profiler.recordPerformance("JAVA_GENERATION", AIProviderType.GEMINI_PRO, true, 100);

        AIProviderType best = profiler.getBestAIForTask("JAVA_GENERATION");

        // Gemini is faster -> better score
        assertEquals(AIProviderType.GEMINI_PRO, best);
    }

    @Test
    void testRecordPerformance_bestAIChangesOverTime() {
        AIProfiler profiler = new AIProfiler();

        profiler.recordPerformance("TASK", AIProviderType.GROQ_LLAMA3, true, 300);
        profiler.recordPerformance("TASK", AIProviderType.GEMINI_PRO, true, 200);
        profiler.recordPerformance("TASK", AIProviderType.GROQ_LLAMA3, true, 100);  // Groq improves

        AIProviderType best = profiler.getBestAIForTask("TASK");

        assertNotNull(best);
    }

    @Test
    void testGetBestAIForTask_unknownCategoryReturnsDefault() {
        AIProfiler profiler = new AIProfiler();

        AIProviderType best = profiler.getBestAIForTask("UNKNOWN_TASK");

        assertEquals(AIProviderType.GROQ_LLAMA3, best);
    }

    @Test
    void testRecordPerformance_failureAffectsScore() {
        AIProfiler profiler = new AIProfiler();

        profiler.recordPerformance("TASK", AIProviderType.GROQ_LLAMA3, false, 100);  // failure
        profiler.recordPerformance("TASK", AIProviderType.GROQ_LLAMA3, true, 200);   // success

        AIProviderType best = profiler.getBestAIForTask("TASK");

        assertEquals(AIProviderType.GROQ_LLAMA3, best);
        // Even with 50% success, still the only provider
        assertNotNull(best);
    }

    @Test
    void testRecordPerformance_multipleTasksIndependent() {
        AIProfiler profiler = new AIProfiler();

        profiler.recordPerformance("SQL", AIProviderType.GROQ_LLAMA3, true, 100);
        profiler.recordPerformance("PYTHON", AIProviderType.GEMINI_PRO, true, 50);

        AIProviderType bestSQL = profiler.getBestAIForTask("SQL");
        AIProviderType bestPython = profiler.getBestAIForTask("PYTHON");

        assertEquals(AIProviderType.GROQ_LLAMA3, bestSQL);
        assertEquals(AIProviderType.GEMINI_PRO, bestPython);
    }

    @Test
    void testRecordPerformance_fasterSpeedIncreasesScore() {
        AIProfiler profiler = new AIProfiler();

        // Make Groq consistently fast
        for (int i = 0; i < 10; i++) {
            profiler.recordPerformance("TASK", AIProviderType.GROQ_LLAMA3, true, 50);
        }
        // Make Gemini slower
        for (int i = 0; i < 10; i++) {
            profiler.recordPerformance("TASK", AIProviderType.GEMINI_PRO, true, 500);
        }

        AIProviderType best = profiler.getBestAIForTask("TASK");

        assertEquals(AIProviderType.GROQ_LLAMA3, best);
    }

    @Test
    void testRecordPerformance_higherSuccessRatePreferred() {
        AIProfiler profiler = new AIProfiler();

        // Groq: 50% success, Gemini: 90% success
        for (int i = 0; i < 5; i++) {
            profiler.recordPerformance("TASK", AIProviderType.GROQ_LLAMA3, true, 100);
            profiler.recordPerformance("TASK", AIProviderType.GROQ_LLAMA3, false, 100);
        }
        for (int i = 0; i < 9; i++) {
            profiler.recordPerformance("TASK", AIProviderType.GEMINI_PRO, true, 100);
        }
        profiler.recordPerformance("TASK", AIProviderType.GEMINI_PRO, false, 100);

        AIProviderType best = profiler.getBestAIForTask("TASK");

        assertEquals(AIProviderType.GEMINI_PRO, best);
    }

    @Test
    void testRecordPerformance_nullProvider() {
        AIProfiler profiler = new AIProfiler();

        assertThrows(NullPointerException.class, () -> {
            profiler.recordPerformance("TASK", null, true, 100);
        });
    }

    @Test
    void testRecordPerformance_nullCategory() {
        AIProfiler profiler = new AIProfiler();

        assertThrows(NullPointerException.class, () -> {
            profiler.recordPerformance(null, AIProviderType.GROQ_LLAMA3, true, 100);
        });
    }

    @Test
    void testGetBestAIForTask_multipleBestCandidates() {
        AIProfiler profiler = new AIProfiler();

        // Both have equal score
        profiler.recordPerformance("TASK", AIProviderType.GROQ_LLAMA3, true, 100);
        profiler.recordPerformance("TASK", AIProviderType.GEMINI_PRO, true, 100);

        AIProviderType best = profiler.getBestAIForTask("TASK");

        assertNotNull(best);
        // Should pick one of them
        assertTrue(best == AIProviderType.GROQ_LLAMA3 || best == AIProviderType.GEMINI_PRO);
    }

    @Test
    void testGetBestAIForTask_allProvidersFailing() {
        AIProfiler profiler = new AIProfiler();

        // All providers have 0% success
        profiler.recordPerformance("TASK", AIProviderType.GROQ_LLAMA3, false, 100);
        profiler.recordPerformance("TASK", AIProviderType.GEMINI_PRO, false, 200);

        AIProviderType best = profiler.getBestAIForTask("TASK");

        // With scoring, even 0% success will have score based on speed
        // Should still return something (the fastest failure)
        assertNotNull(best);
    }

    @Test
    void testRecordPerformance_zeroExecutionTime() {
        AIProfiler profiler = new AIProfiler();

        profiler.recordPerformance("TASK", AIProviderType.GROQ_LLAMA3, true, 0);

        // Should not throw
        AIProviderType best = profiler.getBestAIForTask("TASK");

        assertEquals(AIProviderType.GROQ_LLAMA3, best);
    }

    @Test
    void testRecordPerformance_largeNumberOfRecords() {
        AIProfiler profiler = new AIProfiler();

        for (int i = 0; i < 1000; i++) {
            profiler.recordPerformance("BULK_TASK", AIProviderType.GROQ_LLAMA3, i % 2 == 0, 100 + i);
        }

        AIProviderType best = profiler.getBestAIForTask("BULK_TASK");

        assertEquals(AIProviderType.GROQ_LLAMA3, best);
    }

    @Test
    void testProfilePersistence_acrossMultipleRecords() {
        AIProfiler profiler = new AIProfiler();

        profiler.recordPerformance("TASK1", AIProviderType.GROQ_LLAMA3, true, 100);
        AIProviderType best1 = profiler.getBestAIForTask("TASK1");

        profiler.recordPerformance("TASK1", AIProviderType.GEMINI_PRO, true, 50);
        AIProviderType best2 = profiler.getBestAIForTask("TASK1");

        assertNotEquals(best1, best2);
        assertEquals(AIProviderType.GEMINI_PRO, best2);
    }

    @Test
    void testRecordPerformance_negativeExecutionTime() {
        AIProfiler profiler = new AIProfiler();

        assertDoesNotThrow(() -> {
            profiler.recordPerformance("TASK", AIProviderType.GROQ_LLAMA3, true, -100);
        });
    }
}
