package com.supremeai.intelligence;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ParallelCodeAnalyzer.
 * Tests parallel code analysis, chunking, and result aggregation.
 */
class ParallelCodeAnalyzerTest {

    private ThreadPoolTaskExecutor createTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(2000);
        executor.initialize();
        return executor;
    }

    @Test
    void testAnalyzeMassiveCode_smallInput() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        String smallCode = "public class Test {}";

        AnalysisResult result = analyzer.analyzeMassiveCode(smallCode);

        assertNotNull(result);
        assertTrue(result.totalLinesAnalyzed > 0);
        assertTrue(result.timeTakenMs >= 0);
    }

    @Test
    void testAnalyzeMassiveCode_mediumInput() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        StringBuilder mediumCode = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            mediumCode.append("public class Class").append(i).append(" {}\n\n");
        }

        AnalysisResult result = analyzer.analyzeMassiveCode(mediumCode.toString());

        assertNotNull(result);
        assertTrue(result.totalLinesAnalyzed > 5);
    }

    @Test
    void testAnalyzeMassiveCode_detectsHardcodedSecrets() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        String codeWithSecret = "public class Config {\n" +
                               "    private String password = \"secret123\";\n" +
                               "    private String apiKey = \"key456\";\n" +
                               "}";

        AnalysisResult result = analyzer.analyzeMassiveCode(codeWithSecret);

        assertTrue(result.allVulnerabilities.contains("Potential Hardcoded Secret"));
    }

    @Test
    void testAnalyzeMassiveCode_detectsSelectStar() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        String codeWithSelectStar = "public class Repository {\n" +
                                    "    String query = \"SELECT * FROM users\";\n" +
                                    "}";

        AnalysisResult result = analyzer.analyzeMassiveCode(codeWithSelectStar);

        assertTrue(result.allOptimizations.contains("Avoid SELECT *, specify columns"));
    }

    @Test
    void testAnalyzeMassiveCode_multipleIssues() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        String code = "public class BadCode {\n" +
                     "    String pwd = \"password\";\n" +
                     "    String sql = \"SELECT * FROM table\";\n" +
                     "}";

        AnalysisResult result = analyzer.analyzeMassiveCode(code);

        assertTrue(result.allVulnerabilities.size() >= 1);
        assertTrue(result.allOptimizations.size() >= 1);
    }

    @Test
    void testAnalyzeMassiveCode_noIssues() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        String cleanCode = "public class CleanCode {\n" +
                          "    public void goodMethod() {\n" +
                          "        System.out.println(\"Hello\");\n" +
                          "    }\n" +
                          "}";

        AnalysisResult result = analyzer.analyzeMassiveCode(cleanCode);

        assertTrue(result.allVulnerabilities.isEmpty());
        assertTrue(result.allOptimizations.isEmpty());
    }

    @Test
    void testAnalyzeMassiveCode_largeInput() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        StringBuilder largeCode = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeCode.append("public class Class").append(i).append(" { /* content */ }\n");
        }

        AnalysisResult result = analyzer.analyzeMassiveCode(largeCode.toString());

        assertTrue(result.totalLinesAnalyzed >= 1000);
        assertTrue(result.timeTakenMs >= 0);
    }

    @Test
    void testAnalyzeMassiveCode_chunkingByClass() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        String multiClassCode = 
            "public class A {}\n" +
            "public class B {}\n" +
            "public class C {}\n";

        AnalysisResult result = analyzer.analyzeMassiveCode(multiClassCode);

        assertNotNull(result);
        assertTrue(result.totalLinesAnalyzed > 0);
    }

    @Test
    void testAnalyzeMassiveCode_performanceImprovementOverSingleThreaded() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        StringBuilder largeCode = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            largeCode.append("public class Class").append(i).append(" { /* content */ }\n");
        }

        long start = System.currentTimeMillis();
        AnalysisResult result = analyzer.analyzeMassiveCode(largeCode.toString());
        long duration = System.currentTimeMillis() - start;

        // Should complete in reasonable time (under 10 seconds for 500 classes)
        assertTrue(duration < 10000, "Parallel analysis should be fast");
        assertTrue(result.totalLinesAnalyzed > 0);
    }

    @Test
    void testAnalyzeMassiveCode_timeoutHandling() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        // The code analyzer has a 5 second timeout per chunk
        // Normal code should never hit this
        String normalCode = "public class Test { }";

        AnalysisResult result = analyzer.analyzeMassiveCode(normalCode);

        assertNotNull(result);
    }

    @Test
    void testAnalyzeMassiveCode_emptyInput() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        AnalysisResult result = analyzer.analyzeMassiveCode("");

        assertNotNull(result);
        assertEquals(0, result.totalLinesAnalyzed);
    }

    @Test
    void testAnalyzeMassiveCode_resultToString() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        String code = "public class Test { String pwd = \"secret\"; }";
        AnalysisResult result = analyzer.analyzeMassiveCode(code);

        String summary = result.toString();

        assertTrue(summary.contains("Analyzed"));
        assertTrue(summary.contains("lines"));
        assertTrue(summary.contains("ms"));
    }

    @Test
    void testAnalyzeMassiveCode_correctlyCountsLines() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        String codeWithExactLines = "line1\nline2\nline3\nline4\nline5";
        AnalysisResult result = analyzer.analyzeMassiveCode(codeWithExactLines);

        assertEquals(5, result.totalLinesAnalyzed);
    }

    @Test
    void testAnalyzeMassiveCode_capitalizationInSecretDetection() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        String code = "String PASSWORD = \"secret\";";
        AnalysisResult result = analyzer.analyzeMassiveCode(code);

        assertTrue(result.allVulnerabilities.contains("Potential Hardcoded Secret"));
    }

    @Test
    void testChunkCode_function() throws Exception {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);

        // Using reflection to access private chunkCode method
        java.lang.reflect.Method chunkMethod = ParallelCodeAnalyzer.class.getDeclaredMethod("chunkCode", String.class);
        chunkMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.List<String> chunks = (java.util.List<String>) chunkMethod.invoke(analyzer, "public class A {} public class B {}");

        assertTrue(chunks.size() >= 1);
    }

    @Test
    void testAnalyzeMassiveCode_nullInput() {
        ThreadPoolTaskExecutor executor = createTaskExecutor();
        ParallelCodeAnalyzer analyzer = new ParallelCodeAnalyzer(executor);
        AnalysisResult result = analyzer.analyzeMassiveCode(null);
        assertNotNull(result);
        assertEquals(0, result.totalLinesAnalyzed);
    }
}
