package org.example.service;

import org.example.model.Agent;
import org.example.model.Requirement;
import org.example.model.Vote;
import org.example.model.SystemConfig;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class AgentOrchestrator {
    private final AIAPIService aiService;
    private final ConsensusEngine consensusEngine;
    private final ApprovalManager approvalManager;
    private final RotationManager rotationManager;
    private final RequirementClassifier classifier;
    private final MemoryManager memoryManager;
    private final FirebaseService firebaseService;
    private final SystemConfig config;
    
    // Phase 3 Services
    private final FileOrchestrator fileOrchestrator;
    private final TemplateManager templateManager;
    
    // Phase 4 Services (Git + CI/CD + Deployment)
    private final GitIntegrationService gitService;
    private final CICDService cicdService;
    private final CloudDeploymentService deploymentService;
    private final ProjectTypeManager projectTypeManager;
    
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private final Map<String, Agent> agentPool = new HashMap<>();
    
    public AgentOrchestrator(Map<String, String> apiKeys, FirebaseService firebase, SystemConfig cfg) {
        this.aiService = new AIAPIService(apiKeys);
        this.consensusEngine = new ConsensusEngine(0.70);
        this.approvalManager = new ApprovalManager();
        this.rotationManager = new RotationManager();
        this.classifier = new RequirementClassifier();
        this.memoryManager = new MemoryManager();
        this.firebaseService = firebase;
        this.config = cfg;
        
        // Initialize Phase 3 (Generator)
        this.fileOrchestrator = new FileOrchestrator("projects", this.memoryManager);
        this.templateManager = new TemplateManager("templates", this.fileOrchestrator);
        
        // Initialize Phase 4 (Git + CI/CD + Deployment)
        this.gitService = new GitIntegrationService("projects", firebase);
        this.cicdService = new CICDService("projects", firebase);
        this.deploymentService = new CloudDeploymentService(firebase);
        this.projectTypeManager = new ProjectTypeManager(gitService, cicdService, deploymentService, fileOrchestrator, firebase);
        
        this.memoryManager.setFirebaseService(firebase);
        initializeAgentPool();
    }
    
    private void initializeAgentPool() {
        agentPool.put("BUILDER", new Agent("builder-1", "X-Builder", Agent.Role.BUILDER, "deepseek"));  
        agentPool.put("REVIEWER", new Agent("reviewer-1", "Y-Reviewer", Agent.Role.REVIEWER, "claude"));
        agentPool.put("ARCHITECT", new Agent("architect-1", "Z-Architect", Agent.Role.ARCHITECT, "gpt-4"));
    }
    
    public void processProjectRequirement(String projectId, String requirementDesc) {
        executor.submit(() -> {
            try {
                System.out.println("\n🚀 [ORCHESTRATOR] Analyzing requirement: " + requirementDesc);
                
                Requirement.Size size = classifier.classify(requirementDesc);
                Requirement requirement = new Requirement(UUID.randomUUID().toString(), requirementDesc, size);
                
                if (size == Requirement.Size.HUMAN_REQUIRED) {
                    handleHumanRequired(projectId, requirement);
                    return;
                }
                
                approvalManager.processRequirement(requirement);
                if (requirement.getStatus() == Requirement.Status.APPROVED) {
                    runWorkflow(projectId, requirement);
                }
            } catch (Exception e) {
                System.err.println("Error processing requirement: " + e.getMessage());
            }
        });
    }

    private void handleHumanRequired(String projectId, Requirement req) {
        firebaseService.saveChatMessage(projectId, "SupremeAI", 
            "🚨 I need human action for: \"" + req.getDescription() + "\".", "human_required");
    }

    /**
     * Process Git-Based Development Project
     * Admin provides git URL, branch, and development task
     * SupremeAI handles: Clone → Develop → Test → Fix Failures → Commit → Deploy
     */
    public void processGitProject(String projectId, Map<String, String> gitConfig) {
        executor.submit(() -> {
            try {
                String gitUrl = gitConfig.get("gitUrl");
                String branch = gitConfig.get("branch");
                String task = gitConfig.get("task");
                
                System.out.println("\n🔄 [GIT-BASED WORKFLOW] Starting development on: " + gitUrl);
                firebaseService.saveChatMessage(projectId, "System", "🚀 Starting Git-based development workflow...", "system");
                
                // Create project configuration
                ProjectTypeManager.ProjectConfig config = new ProjectTypeManager.ProjectConfig();
                config.type = ProjectTypeManager.ProjectType.GIT_BASED;
                config.projectId = projectId;
                config.gitUrl = gitUrl;
                config.gitBranch = branch;
                config.gitToken = gitConfig.getOrDefault("gitToken", "");
                
                // Build config
                config.buildConfig.put("install_command", gitConfig.getOrDefault("installCmd", "npm install"));
                config.buildConfig.put("test_command", gitConfig.get("testCmd"));
                config.buildConfig.put("build_command", gitConfig.getOrDefault("buildCmd", "npm run build"));
                config.buildConfig.put("lint_command", gitConfig.getOrDefault("lintCmd", ""));
                
                // Deployment config
                config.deploymentConfig.put("cloud_provider", gitConfig.getOrDefault("cloudProvider", ""));
                config.deploymentConfig.put("provider_credentials", gitConfig.getOrDefault("cloudToken", ""));
                config.deploymentConfig.put("auto_fix_tests", gitConfig.getOrDefault("autoFixTests", "true"));
                
                // Execute through ProjectTypeManager
                projectTypeManager.processProject(config, task);
                
            } catch (Exception e) {
                System.err.println("Error processing git project: " + e.getMessage());
                e.printStackTrace();
                firebaseService.saveChatMessage(projectId, "System", 
                    "❌ Error: " + e.getMessage(), "error");
            }
        });
    }

    private void runWorkflow(String projectId, Requirement requirement) {
        try {
            // Step 1: Planning with Voting Loop
            String finalPlan = planWithArchitect(projectId, requirement.getDescription());
            
            // Step 2: Initialize File Structure (Phase 3)
            templateManager.initializeProject(projectId, "FLUTTER"); // Defaulting to Flutter as example
            
            // Step 3: Code Generation with Voting Loop
            String finalCode = buildWithCodeGenerator(projectId, requirement.getDescription(), finalPlan);
            
            // Step 4: Write Final Code to Disk (Phase 3)
            fileOrchestrator.writeFile(projectId, "lib/main.dart", finalCode);
            
            System.out.println("🎉 [PHASE 3] Project files generated successfully at: projects/" + projectId);
            firebaseService.saveChatMessage(projectId, "System", "✅ Code generation complete. Files saved locally.", "system");
            
        } catch (IOException e) {
            System.err.println("File generation error: " + e.getMessage());
        }
    }
    
    private String planWithArchitect(String projectId, String requirement) {
        System.out.println("\n🏗️  [ARCHITECT] Generating plan...");
        String currentPlan = aiService.callAI("ARCHITECT", "Plan architecture for: " + requirement, rotationManager.getFallbackChain(Agent.Role.ARCHITECT));
        
        boolean consensusReached = false;
        int loopCount = 0;
        while (!consensusReached && loopCount < 3) {
            loopCount++;
            List<Vote> votes = requestConsensus("PLAN_VOTE_" + loopCount, currentPlan);
            if (consensusEngine.getApprovalRate(votes) >= 0.70) {
                consensusReached = true;
            } else {
                currentPlan = aiService.callAI("ARCHITECT", "Refine this plan based on suggestions.", rotationManager.getFallbackChain(Agent.Role.ARCHITECT));
            }
        }
        return currentPlan;
    }
    
    private String buildWithCodeGenerator(String projectId, String requirement, String plan) {
        System.out.println("\n🔨 [BUILDER] Generating code...");
        String currentCode = aiService.callAI("BUILDER", "Generate code based on plan: " + plan, rotationManager.getFallbackChain(Agent.Role.BUILDER));
        
        boolean consensusReached = false;
        int loopCount = 0;
        while (!consensusReached && loopCount < 3) {
            loopCount++;
            List<Vote> votes = requestConsensus("CODE_VOTE_" + loopCount, currentCode);
            if (consensusEngine.getApprovalRate(votes) >= 0.70) {
                consensusReached = true;
            } else {
                currentCode = aiService.callAI("BUILDER", "Fix code based on suggestions.", rotationManager.getFallbackChain(Agent.Role.BUILDER));
            }
        }
        return currentCode;
    }

    public List<Vote> requestConsensus(String taskId, String content) {
        List<Vote> votes = new CopyOnWriteArrayList<>();
        List<Future<?>> futures = new ArrayList<>();
        for (Agent agent : agentPool.values()) {
            futures.add(executor.submit(() -> {
                String response = aiService.callAI(agent.getRole().name(), "Evaluate: " + content, rotationManager.getFallbackChain(agent.getRole()));
                boolean approved = response != null && response.toUpperCase().contains("APPROVED");
                votes.add(new Vote(agent.getId(), approved, response));
            }));
        }
        for (Future<?> future : futures) {
            try { future.get(30, TimeUnit.SECONDS); } catch (Exception e) {}
        }
        return votes;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
