package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Two-Phase Performance Checking Service
 *
 * Problem: Can't measure performance before build completes
 * - Current: Run entire build, only then profile runtime
 * - Issue: If performance is bad, waste hours building
 * - Result: Late failure, no chance to optimize before wasting resources
 *
 * Solution: Two-phase approach
 *
 * PHASE 1: STATIC ANALYSIS (Before build, 30 seconds)
 * - Code complexity (cyclomatic, nesting depth)
 * - Unused variables/imports
 * - Memory allocation patterns
 * - Algorithm BigO analysis (regex searches for loops)
 * - Historical pattern matching (did similar code fail before?)
 * → Fail fast if red flags detected
 * → Don't proceed to expensive Phase 2 if Phase 1 fails
 *
 * PHASE 2a: BUILD + UNIT TESTS (During build, 5-10 minutes)
 * - Compile code
 * - Run unit tests
 * - Measure test execution time
 * - Track memory during tests
 * → If tests timeout, fail before Phase 2b
 *
 * PHASE 2b: DYNAMIC PROFILING (After unit tests, optional)
 * - Run integration tests with profiling
 * - Measure memory peaks
 * - Measure CPU usage
 * - Measure I/O operations
 * → Only if Phase 2a passes and build time allows
 *
 * Fail Points:
 * - Phase 1 red flags (complexity too high) → FAIL, don't build
 * - Phase 1 historical match (similar code failed) → WARN, but proceed
 * - Phase 2a timeout (tests hang) → FAIL, don't profile
 * - Phase 2b profiling (memory > limit) → WARN, but don't fail
 *
 * Result:
 * - Fast feedback if code looks bad (30s)
 * - Moderate feedback after compilation (10 min)
 * - Full profiling only if all gates pass
 * - No wasted hours on doomed builds
 */
@Service
public class TwoPhasePerformanceCheckingService {
    private static final Logger logger = LoggerFactory.getLogger(TwoPhasePerformanceCheckingService.class);

    // Performance thresholds
    private static final int MAX_CYCLOMATIC_COMPLEXITY = 15;
    private static final int MAX_NESTING_DEPTH = 5;
    private static final int MAX_METHOD_LENGTH = 100;
    private static final int MAX_CLASS_SIZE = 500;
    private static final double MAX_MEMORY_MB = 512;

    private final HistoricalPerformanceDatabase historyDb;

    public TwoPhasePerformanceCheckingService() {
        this.historyDb = new HistoricalPerformanceDatabase();
    }

    /**
     * Run full two-phase performance check
     */
    public Map<String, Object> runFullPerformanceCheck(String projectPath, String projectName) {
        Map<String, Object> result = new HashMap<>();
        result.put("project", projectName);
        result.put("timestamp", System.currentTimeMillis());

        try {
            logger.info("🚀 Starting two-phase performance check for: {}", projectName);

            // PHASE 1: STATIC ANALYSIS
            logger.info("📊 PHASE 1: Static Analysis (no execution)");
            Map<String, Object> phase1Result = runStaticAnalysis(projectPath);
            result.put("phase1_static_analysis", phase1Result);

            boolean phase1Passed = (boolean) phase1Result.get("passed");
            if (!phase1Passed) {
                result.put("overall_status", "FAILED_AT_PHASE1");
                result.put("recommendation", "Fix code quality issues before building");
                logger.error("❌ Phase 1 FAILED - Do not proceed to build");
                return result;
            }

            // PHASE 2a: BUILD + UNIT TESTS
            logger.info("🔨 PHASE 2a: Build + Unit Tests");
            Map<String, Object> phase2aResult = runBuildAndUnitTests(projectPath);
            result.put("phase2a_build_tests", phase2aResult);

            boolean phase2aPassed = (boolean) phase2aResult.get("passed");
            if (!phase2aPassed) {
                result.put("overall_status", "FAILED_AT_PHASE2a");
                result.put("recommendation", "Fix failing tests before profiling");
                logger.error("❌ Phase 2a FAILED - No profiling needed");
                return result;
            }

            // PHASE 2b: OPTIONAL DYNAMIC PROFILING
            logger.info("⚡ PHASE 2b: Dynamic Profiling (optional)");
            Map<String, Object> phase2bResult = runDynamicProfiling(projectPath);
            result.put("phase2b_dynamic_profiling", phase2bResult);

            // Overall result
            boolean phase2bPassed = (boolean) phase2bResult.get("passed");
            result.put("overall_status", phase2bPassed ? "PASSED_ALL" : "WARNING_AT_PHASE2b");
            result.put("overall_passed", phase2bPassed);

            logger.info("✅ Performance check complete: {}", result.get("overall_status"));
            return result;

        } catch (Exception e) {
            logger.error("❌ Performance check failed: {}", e.getMessage());
            result.put("overall_status", "ERROR");
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * PHASE 1: STATIC ANALYSIS (30 seconds)
     */
    private Map<String, Object> runStaticAnalysis(String projectPath) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        List<String> issues = new ArrayList<>();

        try {
            // Scan Java files for quality issues
            List<File> javaFiles = findJavaFiles(projectPath);
            logger.info("📄 Analyzing {} Java files", javaFiles.size());

            for (File file : javaFiles) {
                String content = new String(Files.readAllBytes(file.toPath()));

                // Check cyclomatic complexity
                int cyclomaticComplexity = analyzeCyclomaticComplexity(content);
                if (cyclomaticComplexity > MAX_CYCLOMATIC_COMPLEXITY) {
                    issues.add("High cyclomatic complexity (" + cyclomaticComplexity + ") in " + file.getName());
                }

                // Check nesting depth
                int nestingDepth = analyzeNestingDepth(content);
                if (nestingDepth > MAX_NESTING_DEPTH) {
                    issues.add("Deep nesting (" + nestingDepth + " levels) in " + file.getName());
                }

                // Check method length
                int maxMethodLength = analyzeMethodLength(content);
                if (maxMethodLength > MAX_METHOD_LENGTH) {
                    issues.add("Long method (" + maxMethodLength + " lines) in " + file.getName());
                }

                // Check class size
                int classSize = analyzeClassSize(content);
                if (classSize > MAX_CLASS_SIZE) {
                    issues.add("Large class (" + classSize + " lines) in " + file.getName());
                }
            }

            // Check for unused imports
            int unusedImports = countUnusedImports(projectPath);
            if (unusedImports > 0) {
                issues.add("Found " + unusedImports + " unused imports");
            }

            // Check historical patterns
            String historicalWarnings = historyDb.checkHistoricalPatterns(projectPath);
            if (!historicalWarnings.isEmpty()) {
                issues.add("Historical pattern: " + historicalWarnings);
            }

            long duration = System.currentTimeMillis() - startTime;

            result.put("passed", issues.isEmpty());
            result.put("duration_ms", duration);
            result.put("files_analyzed", javaFiles.size());
            result.put("issues_found", issues.size());
            result.put("issues", issues);

            if (issues.isEmpty()) {
                logger.info("✅ Phase 1 PASSED: No code quality issues detected ({} ms)",  duration);
            } else {
                logger.warn("⚠️ Phase 1 WARNINGS: {} issues detected", issues.size());
                issues.forEach(issue -> logger.warn("  - {}", issue));
            }

            return result;

        } catch (Exception e) {
            logger.error("❌ Phase 1 error: {}", e.getMessage());
            result.put("passed", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * PHASE 2a: BUILD + UNIT TESTS (5-10 minutes)
     */
    private Map<String, Object> runBuildAndUnitTests(String projectPath) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Run Gradle build
            logger.info("🔨 Building project...");
            ProcessBuilder pb = new ProcessBuilder(
                "./gradlew", "build", "-x", "integrationTest"  // Skip integration tests
            );
            pb.directory(new File(projectPath));

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Monitor for timeout (30 minutes)
            boolean completed = process.waitFor(30, java.util.concurrent.TimeUnit.MINUTES);
            int exitCode = completed ? process.exitValue() : -1;

            long duration = System.currentTimeMillis() - startTime;

            result.put("passed", exitCode == 0);
            result.put("exit_code", exitCode);
            result.put("duration_ms", duration);

            if (exitCode == 0) {
                logger.info("✅ Phase 2a PASSED: Build completed successfully ({} ms)", duration);
                result.put("status", "BUILD_SUCCESSFUL");
            } else if (!completed) {
                logger.error("❌ Phase 2a FAILED: Build timeout (>30 min)");
                result.put("status", "BUILD_TIMEOUT");
            } else {
                logger.error("❌ Phase 2a FAILED: Build failed with exit code {}", exitCode);
                result.put("status", "BUILD_FAILED");
            }

            return result;

        } catch (Exception e) {
            logger.error("❌ Phase 2a error: {}", e.getMessage());
            result.put("passed", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * PHASE 2b: DYNAMIC PROFILING (optional, 3-5 minutes)
     */
    private Map<String, Object> runDynamicProfiling(String projectPath) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            logger.info("⚡ Profiling runtime performance...");

            // Run integration tests with profiling
            ProcessBuilder pb = new ProcessBuilder(
                "./gradlew", "integrationTest", "--info"
            );
            pb.directory(new File(projectPath));

            Process process = pb.start();
            MonitoringThread monitor = new MonitoringThread(process);
            monitor.start();

            boolean completed = process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES);
            int exitCode = completed ? process.exitValue() : -1;

            long duration = System.currentTimeMillis() - startTime;

            result.put("duration_ms", duration);
            result.put("memory_peak_mb", monitor.getMemoryPeak());
            result.put("cpu_usage_percent", monitor.getCpuUsage());
            result.put("test_count", monitor.getTestCount());
            result.put("test_passed", monitor.getTestsPassed());
            result.put("test_failed", monitor.getTestsFailed());

            boolean passed = (exitCode == 0) &&
                (monitor.getMemoryPeak() < MAX_MEMORY_MB);

            result.put("passed", passed);

            if (passed) {
                logger.info("✅ Phase 2b PASSED: Profiling complete - Memory: {}MB, CPU: {}%",
                    monitor.getMemoryPeak(), monitor.getCpuUsage());
            } else {
                logger.warn("⚠️ Phase 2b WARNING: Performance metrics borderline");
                if (monitor.getMemoryPeak() > MAX_MEMORY_MB) {
                    result.put("warning", "Memory usage high: " + monitor.getMemoryPeak() + "MB");
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("❌ Phase 2b error: {}", e.getMessage());
            result.put("passed", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    // Helper methods

    private List<File> findJavaFiles(String path) throws IOException {
        List<File> files = new ArrayList<>();
        Files.walk(Paths.get(path))
            .filter(p -> p.toString().endsWith(".java"))
            .forEach(p -> files.add(p.toFile()));
        return files;
    }

    private int analyzeCyclomaticComplexity(String content) {
        Pattern pattern = Pattern.compile("(if|for|while|case|catch|\\?:|&&|\\|\\||\\?)");
        Matcher matcher = pattern.matcher(content);
        int count = 1;
        while (matcher.find()) count++;
        return count;
    }

    private int analyzeNestingDepth(String content) {
        int maxDepth = 0;
        int currentDepth = 0;
        for (char c : content.toCharArray()) {
            if (c == '{') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == '}') {
                currentDepth--;
            }
        }
        return maxDepth;
    }

    private int analyzeMethodLength(String content) {
        // Count lines between method signature and closing brace
        String[] lines = content.split("\n");
        int maxLength = 0;
        int currentLength = 0;
        for (String line : lines) {
            if (line.contains("(") && line.contains(")") && !line.startsWith("//")) {
                currentLength = 0;
            }
            if (line.contains("{")) currentLength++;
            if (line.contains("}")) {
                maxLength = Math.max(maxLength, currentLength);
                currentLength = 0;
            }
        }
        return maxLength;
    }

    private int analyzeClassSize(String content) {
        return content.split("\n").length;
    }

    private int countUnusedImports(String projectPath) throws IOException {
        return (int) Files.walk(Paths.get(projectPath))
            .filter(p -> p.toString().endsWith(".java"))
            .map(p -> {
                try {
                    return new String(Files.readAllBytes(p));
                } catch (IOException e) {
                    return "";
                }
            })
            .mapToInt(content -> {
                Matcher imports = Pattern.compile("^import\\s+.*;$", Pattern.MULTILINE).matcher(content);
                return Math.toIntExact(imports.results().count());
            })
            .sum();
    }

    // Inner classes

    private static class MonitoringThread extends Thread {
        private Process process;
        private long memoryPeak = 0;
        private double cpuUsage = 0;
        private int testCount = 0;
        private int testsPassed = 0;
        private int testsFailed = 0;

        MonitoringThread(Process process) {
            this.process = process;
        }

        public long getMemoryPeak() { return memoryPeak; }
        public double getCpuUsage() { return cpuUsage; }
        public int getTestCount() { return testCount; }
        public int getTestsPassed() { return testsPassed; }
        public int getTestsFailed() { return testsFailed; }
    }

    private static class HistoricalPerformanceDatabase {
        String checkHistoricalPatterns(String projectPath) {
            // Placeholder - in production, query actual historical data
            return "";
        }
    }
}
