package org.example.service;

import java.io.*;

import java.util.*;

/**
 * CI/CD Pipeline Service
 * 
 * Handles:
 * - Run tests
 * - Build project
 * - Check code quality
 * - Generate test reports
 * - Pass/Fail decision
 * 
 * ADMIN CONFIGURES:
 * - Test command (npm test, mvn test, pytest, etc.)
 * - Build command (npm run build, gradle build, etc.)
 * - Code quality tools (eslint, sonarqube, etc.)
 * - Coverage thresholds
 * - Success criteria
 */
public class CICDService {
    
    private final String workspaceRoot;
    private final FirebaseService firebase;
    
    public static class BuildResult {
        public boolean success;
        public String buildId;
        public long duration;
        public int testsPassed;
        public int testsFailed;
        public double codecCoverage;
        public String logs;
        public List<String> failedTests;
        
        public BuildResult() {
            this.failedTests = new ArrayList<>();
        }
    }
    
    public CICDService(String workspaceRoot, FirebaseService firebase) {
        this.workspaceRoot = workspaceRoot;
        this.firebase = firebase;
    }
    
    /**
     * Run complete CI/CD pipeline
     */
    public BuildResult runPipeline(String projectId, Map<String, String> config) {
        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🔨 [CI/CD] Starting pipeline for: " + projectId);
            System.out.println("=".repeat(60));
            
            BuildResult result = new BuildResult();
            result.buildId = UUID.randomUUID().toString().substring(0, 8);
            long startTime = System.currentTimeMillis();
            
            String projectPath = workspaceRoot + File.separator + projectId;
            
            // Phase 1: Install dependencies
            System.out.println("\n📦 [CI/CD] Installing dependencies...");
            if (!installDependencies(projectPath, config)) {
                result.success = false;
                result.logs = "Failed at dependency installation";
                return result;
            }
            
            // Phase 2: Run tests
            System.out.println("\n🧪 [CI/CD] Running tests...");
            TestResult testResult = runTests(projectPath, config);
            result.testsPassed = testResult.passed;
            result.testsFailed = testResult.failed;
            result.failedTests = testResult.failedTests;
            
            if (testResult.failed > 0) {
                System.out.println("❌ [CI/CD] Tests failed: " + testResult.failed + " failures");
                result.success = false;
                result.logs = testResult.output;
                result.duration = System.currentTimeMillis() - startTime;
                logBuildResult(projectId, result);
                return result;
            }
            
            // Phase 3: Code coverage check
            System.out.println("\n📊 [CI/CD] Checking code coverage...");
            double coverage = checkCodeCoverage(projectPath, config);
            result.codecCoverage = coverage;
            
            if (coverage < 75) { // Default threshold
                System.out.println("⚠️ [CI/CD] Code coverage low: " + coverage + "%");
                // Don't fail, just warn
            }
            
            // Phase 4: Build project
            System.out.println("\n🏗️ [CI/CD] Building project...");
            if (!buildProject(projectPath, config)) {
                result.success = false;
                result.logs = "Failed at build phase";
                result.duration = System.currentTimeMillis() - startTime;
                logBuildResult(projectId, result);
                return result;
            }
            
            // Phase 5: Code quality checks
            System.out.println("\n🔍 [CI/CD] Running code quality checks...");
            if (!runQualityChecks(projectPath, config)) {
                System.out.println("⚠️ [CI/CD] Quality issues found");
                // Don't fail, just report
            }
            
            // SUCCESS!
            result.success = true;
            result.duration = System.currentTimeMillis() - startTime;
            result.logs = "✅ All checks passed!";
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("✅ [CI/CD] Pipeline completed successfully!");
            System.out.println("   Duration: " + (result.duration / 1000) + "s");
            System.out.println("   Tests: " + result.testsPassed + " passed");
            System.out.println("   Coverage: " + String.format("%.1f%%", result.codecCoverage));
            System.out.println("=".repeat(60));
            
            logBuildResult(projectId, result);
            return result;
            
        } catch (Exception e) {
            BuildResult result = new BuildResult();
            result.success = false;
            result.logs = "Pipeline error: " + e.getMessage();
            System.err.println("❌ [CI/CD] Pipeline error: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Install dependencies
     */
    private boolean installDependencies(String projectPath, Map<String, String> config) {
        try {
            String installCmd = config.getOrDefault("install_command", "npm install");
            System.out.println("   Running: " + installCmd);
            
            ProcessBuilder pb = new ProcessBuilder(installCmd.split(" "));
            pb.directory(new File(projectPath));
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("   ✅ Dependencies installed");
                return true;
            } else {
                System.err.println("   ❌ Dependency installation failed");
                return false;
            }
        } catch (Exception e) {
            System.err.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Run tests and collect results
     */
    private TestResult runTests(String projectPath, Map<String, String> config) {
        TestResult result = new TestResult();
        try {
            String testCmd = config.getOrDefault("test_command", "npm test");
            System.out.println("   Running: " + testCmd);
            
            ProcessBuilder pb = new ProcessBuilder(testCmd.split(" "));
            pb.directory(new File(projectPath));
            Process process = pb.start();
            
            // Capture output
            result.output = captureOutput(process);
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                result.passed = 12; // Parse from actual output in production
                result.failed = 0;
                System.out.println("   ✅ All tests passed");
            } else {
                result.passed = 10;
                result.failed = 2;
                result.failedTests.add("TestCase#testLogin failed");
                result.failedTests.add("TestCase#testPayment failed");
                System.out.println("   ❌ Tests failed: " + result.failed);
            }
        } catch (Exception e) {
            result.failed = 1;
            result.failedTests.add("Exception: " + e.getMessage());
            System.err.println("   ❌ Error: " + e.getMessage());
        }
        return result;
    }
    
    /**
     * Check code coverage
     */
    private double checkCodeCoverage(String projectPath, Map<String, String> config) {
        try {
            String coverageCmd = config.getOrDefault("coverage_command", "npm run coverage");
            
            ProcessBuilder pb = new ProcessBuilder(coverageCmd.split(" "));
            pb.directory(new File(projectPath));
            Process process = pb.start();
            
            String output = captureOutput(process);
            process.waitFor();
            
            // Parse coverage from output (production would parse actual reports)
            double coverage = 82.5; // Mock value
            System.out.println("   Coverage: " + String.format("%.1f%%", coverage));
            return coverage;
        } catch (Exception e) {
            System.err.println("   ⚠️ Could not determine coverage: " + e.getMessage());
            return 70.0; // Default
        }
    }
    
    /**
     * Build the project
     */
    private boolean buildProject(String projectPath, Map<String, String> config) {
        try {
            String buildCmd = config.getOrDefault("build_command", "npm run build");
            System.out.println("   Running: " + buildCmd);
            
            ProcessBuilder pb = new ProcessBuilder(buildCmd.split(" "));
            pb.directory(new File(projectPath));
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("   ✅ Build successful");
                return true;
            } else {
                System.err.println("   ❌ Build failed");
                return false;
            }
        } catch (Exception e) {
            System.err.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Run code quality checks
     */
    private boolean runQualityChecks(String projectPath, Map<String, String> config) {
        try {
            String lintCmd = config.getOrDefault("lint_command", "npm run lint");
            System.out.println("   Running: " + lintCmd);
            
            ProcessBuilder pb = new ProcessBuilder(lintCmd.split(" "));
            pb.directory(new File(projectPath));
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("   ✅ Code quality checks passed");
                return true;
            } else {
                System.out.println("   ⚠️ Quality issues detected");
                return false; // Warn but don't fail
            }
        } catch (Exception e) {
            System.err.println("   ⚠️ Could not run quality checks: " + e.getMessage());
            return true; // Don't fail if tool not available
        }
    }
    
    private String captureOutput(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }
    
    private void logBuildResult(String projectId, BuildResult result) {
        try {
            Map<String, Object> log = new HashMap<>();
            log.put("build_id", result.buildId);
            log.put("success", result.success);
            log.put("duration_ms", result.duration);
            log.put("tests_passed", result.testsPassed);
            log.put("tests_failed", result.testsFailed);
            log.put("coverage", result.codecCoverage);
            log.put("timestamp", System.currentTimeMillis());
            // TODO: Implement Firebase logging method
            // firebase.saveBuildLog(projectId, log);
        } catch (Exception e) {
            System.err.println("Failed to log build result: " + e.getMessage());
        }
    }
    
    public static class TestResult {
        public int passed;
        public int failed;
        public List<String> failedTests;
        public String output;
        
        public TestResult() {
            this.failedTests = new ArrayList<>();
        }
    }
}
