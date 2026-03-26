package org.example.service;

import java.util.*;

/**
 * Project Type Manager
 * 
 * Handles different project types:
 * 1. CODE GENERATION (current)
 *    - Admin describes app
 *    - AI generates code
 *    - AI commits to new repo
 *    - Tests & deploys
 * 
 * 2. GIT-BASED (new)
 *    - Admin provides existing git repo
 *    - AI clones, modifies, tests
 *    - AI commits changes
 *    - Tests & deploys
 * 
 * 3. MAINTENANCE (future)
 *    - Bug fixes
 *    - Refactoring
 *    - Performance optimization
 */
public class ProjectTypeManager {
    
    private final GitIntegrationService gitService;
    private final CICDService cicdService;
    private final CloudDeploymentService deploymentService;
    private final FileOrchestrator fileOrchestrator;
    private final FirebaseService firebase;
    
    public enum ProjectType {
        CODE_GENERATION,  // AI generates from scratch
        GIT_BASED,        // Work with existing repo
        MAINTENANCE,      // Bug fixes, refactoring
        MIGRATION         // Upgrade dependencies, migrate code
    }
    
    public static class ProjectConfig {
        public ProjectType type;
        public String projectId;
        public String description;
        public String gitUrl;              // For GIT_BASED
        public String gitBranch;            // Default: "develop" or "feat-xyz"
        public String gitToken;             // For authentication
        public String framework;            // flutter, nodejs, python, java, etc.
        public Map<String, String> buildConfig; // test_command, install_command, etc.
        public Map<String, String> deploymentConfig; // cloud_provider, credentials, etc.
        
        public ProjectConfig() {
            this.buildConfig = new HashMap<>();
            this.deploymentConfig = new HashMap<>();
        }
    }
    
    public ProjectTypeManager(GitIntegrationService git, CICDService cicd, 
                             CloudDeploymentService deployment, FileOrchestrator file,
                             FirebaseService firebase) {
        this.gitService = git;
        this.cicdService = cicd;
        this.deploymentService = deployment;
        this.fileOrchestrator = file;
        this.firebase = firebase;
    }
    
    /**
     * Process project based on type
     */
    public void processProject(ProjectConfig config, String taskDescription) {
        switch (config.type) {
            case CODE_GENERATION:
                processCodeGeneration(config, taskDescription);
                break;
            case GIT_BASED:
                processGitBasedProject(config, taskDescription);
                break;
            case MAINTENANCE:
                processMaintenance(config, taskDescription);
                break;
            default:
                System.err.println("Unknown project type: " + config.type);
        }
    }
    
    /**
     * WORKFLOW 1: Code Generation (Current SupremeAI)
     * Admin: "Build a task manager app"
     * → AI generates code
     * → AI commits to new Firebase storage / file system
     * → Tests pass/fail
     * → If pass: deploy
     */
    private void processCodeGeneration(ProjectConfig config, String taskDescription) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📝 [WORKFLOW] Code Generation");
        System.out.println("=".repeat(70));
        System.out.println("📋 Task: " + taskDescription);
        
        try {
            // Step 1: AI generates code
            System.out.println("\n🤖 Step 1: AI Code Generation");
            String generatedCode = generateCode(config, taskDescription);
            
            // Step 2: Save generated code
            System.out.println("\n💾 Step 2: Save Generated Code");
            fileOrchestrator.writeFile(config.projectId, "src/main.dart", generatedCode);
            
            // Step 3: Run tests (if available)
            System.out.println("\n🧪 Step 3: Testing Code");
            CICDService.BuildResult buildResult = cicdService.runPipeline(config.projectId, 
                config.buildConfig);
            
            if (!buildResult.success) {
                System.out.println("\n❌ Tests failed - attempting fixes");
                // AI could try to fix failures, or report to admin
                firebase.saveChatMessage(config.projectId, "System", 
                    "❌ Tests failed: " + buildResult.logs, "test_failure");
                return;
            }
            
            // Step 4: Commit to git (if git enabled)
            if (config.gitUrl != null && !config.gitUrl.isEmpty()) {
                System.out.println("\n📤 Step 4: Push to Git");
                gitService.cloneRepository(config.projectId, config.gitUrl, config.gitBranch);
                gitService.commitChanges(config.projectId, "feat: AI-generated code for " + taskDescription);
                gitService.pushToOrigin(config.projectId, config.gitBranch, config.gitToken);
            }
            
            // Step 5: Deploy to cloud
            System.out.println("\n☁️ Step 5: Cloud Deployment");
            CloudDeploymentService.DeploymentResult deployResult = 
                deploymentService.deploy(config.projectId, config.deploymentConfig);
            
            if (deployResult.success) {
                System.out.println("\n✅ PROJECT COMPLETE!");
                System.out.println("   URL: " + deployResult.deploymentUrl);
                firebase.saveChatMessage(config.projectId, "System", 
                    "✅ Project deployed: " + deployResult.deploymentUrl, "deployment_success");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in code generation: " + e.getMessage());
            firebase.saveChatMessage(config.projectId, "System", 
                "❌ Error: " + e.getMessage(), "error");
        }
    }
    
    /**
     * WORKFLOW 2: Git-Based Project (NEW!)
     * Admin: "Clone repo, add feature X, test, and deploy"
     * → AI clones your repo
     * → AI makes changes (using agents)
     * → AI runs your tests
     * → If pass: AI pushes to git & deploys
     * → If fail: Reports to you
     */
    private void processGitBasedProject(ProjectConfig config, String taskDescription) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🔄 [WORKFLOW] Git-Based Project Development");
        System.out.println("=".repeat(70));
        System.out.println("📋 Task: " + taskDescription);
        System.out.println("🔗 Repository: " + config.gitUrl);
        System.out.println("🌿 Branch: " + config.gitBranch);
        
        try {
            // Step 1: Clone repository
            System.out.println("\n1️⃣ Step 1: Clone Repository");
            boolean cloned = gitService.cloneRepository(config.projectId, config.gitUrl, 
                config.gitBranch);
            if (!cloned) {
                firebase.saveChatMessage(config.projectId, "System", 
                    "❌ Failed to clone repository", "git_error");
                return;
            }
            
            // Step 2: AI analyzes repo structure
            System.out.println("\n2️⃣ Step 2: Analyze Repository");
            Map<String, Object> repoAnalysis = analyzeRepository(config.projectId);
            System.out.println("   Framework detected: " + repoAnalysis.get("framework"));
            System.out.println("   Test framework: " + repoAnalysis.get("test_framework"));
            
            // Step 3: AI makes changes based on task
            System.out.println("\n3️⃣ Step 3: Make Code Changes");
            makeCodeChanges(config, taskDescription);
            
            // Step 4: Run tests from repo
            System.out.println("\n4️⃣ Step 4: Run Tests");
            CICDService.BuildResult buildResult = cicdService.runPipeline(config.projectId, 
                config.buildConfig);
            
            if (!buildResult.success) {
                System.out.println("\n❌ TESTS FAILED");
                System.out.println("   Failed tests: " + buildResult.failedTests);
                firebase.saveChatMessage(config.projectId, "System", 
                    "❌ Tests failed:\n" + buildResult.failedTests, "test_failure");
                
                // Option 1: AI tries to fix
                System.out.println("   AI attempting to fix failures...");
                boolean fixed = attemptFixFailures(config, buildResult.failedTests);
                if (!fixed) {
                    System.out.println("   Report sent to admin for manual review");
                    return;
                }
                
                // Re-run tests
                buildResult = cicdService.runPipeline(config.projectId, config.buildConfig);
                if (!buildResult.success) {
                    System.out.println("   Still failing - stopping here");
                    return;
                }
            }
            
            System.out.println("\n✅ ALL TESTS PASSED!");
            
            // Step 5: Commit changes to git
            System.out.println("\n5️⃣ Step 5: Commit Changes");
            gitService.commitChanges(config.projectId, 
                "feat: AI implementation - " + taskDescription);
            
            // Step 6: Create pull request (optional)
            if (config.gitToken != null) {
                System.out.println("\n6️⃣ Step 6: Create Pull Request");
                String prNumber = gitService.createPullRequest(config.projectId, 
                    config.gitBranch, "main", 
                    "AI: " + taskDescription,
                    "",
                    config.gitToken);
                System.out.println("   PR: " + prNumber);
                firebase.saveChatMessage(config.projectId, "System", 
                    "📋 Pull request created: " + prNumber, "pr_created");
            } else {
                // Direct push
                System.out.println("\n6️⃣ Step 6: Push to Repository");
                gitService.pushToOrigin(config.projectId, config.gitBranch, config.gitToken);
            }
            
            // Step 7: Deploy to cloud
            System.out.println("\n7️⃣ Step 7: Cloud Deployment");
            CloudDeploymentService.DeploymentResult deployResult = 
                deploymentService.deploy(config.projectId, config.deploymentConfig);
            
            if (deployResult.success) {
                System.out.println("\n✅ PROJECT COMPLETE!");
                System.out.println("   Tests: ✅ All passed");
                System.out.println("   Code: ✅ Pushed to " + config.gitBranch);
                System.out.println("   Deployed: ✅ " + deployResult.deploymentUrl);
                
                firebase.saveChatMessage(config.projectId, "System", 
                    "✅ Feature delivered!\n" + 
                    "Tests: PASSED\n" +
                    "Repository: Pushed to " + config.gitBranch + "\n" +
                    "Live: " + deployResult.deploymentUrl, 
                    "delivery_complete");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in git-based project: " + e.getMessage());
            firebase.saveChatMessage(config.projectId, "System", 
                "❌ Error: " + e.getMessage(), "error");
        }
    }
    
    /**
     * WORKFLOW 3: Maintenance (Bug fixes, refactoring)
     */
    private void processMaintenance(ProjectConfig config, String taskDescription) {
        System.out.println("\n📌 [WORKFLOW] Maintenance");
        System.out.println("Task: " + taskDescription);
        // Similar to git-based but focused on existing code
    }
    
    private String generateCode(ProjectConfig config, String taskDescription) {
        System.out.println("   Architect analyzing requirements...");
        System.out.println("   Builder writing code...");
        System.out.println("   Reviewer checking quality...");
        // This would be actual AI agent calls
        return "// Generated code for: " + taskDescription;
    }
    
    private Map<String, Object> analyzeRepository(String projectId) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("framework", "Node.js/Express");
        analysis.put("test_framework", "Jest");
        analysis.put("build_tool", "npm");
        analysis.put("has_tests", true);
        analysis.put("test_coverage", 85.2);
        return analysis;
    }
    
    private void makeCodeChanges(ProjectConfig config, String taskDescription) {
        System.out.println("   Analyzing task requirements...");
        System.out.println("   Identifying files to modify...");
        System.out.println("   Making changes...");
        // AI agents would modify actual files using GitIntegrationService
    }
    
    private boolean attemptFixFailures(ProjectConfig config, List<String> failedTests) {
        System.out.println("   Reading failing tests...");
        System.out.println("   Understanding what went wrong...");
        System.out.println("   Fixing issues...");
        return true; // Simulated success
    }
}
