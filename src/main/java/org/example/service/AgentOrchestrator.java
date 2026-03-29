package org.example.service;

import org.example.model.Agent;
import org.example.model.Requirement;
import org.example.model.Vote;
import org.example.model.SystemConfig;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;


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
    
    // Phase 5 Services (Multi-Account Support)
    private final AIAccountManager accountManager;
    private final BudgetManager budgetManager;
    private final PublicAIRouter publicRouter;
    
    // Phase 6 Services (Hybrid Data Collection)
    private final QuotaTracker quotaTracker;
    private final APIDataCollector apiDataCollector;
    private final BrowserDataCollector browserDataCollector;
    private final HybridDataCollector hybridDataCollector;
    
    // Phase 7 Services (Cloud Functions & Admin Integration)
    private final DataCollectorService dataCollectorService;
    private final WebhookListener webhookListener;
    private final AdminMessagePusher adminMessagePusher;

    // ≡ƒÜÇ Intelligence, Safety & Self-Analysis Services
    private final InternetSearchService searchService;
    private final SafeZoneManager safeZoneManager;
    private final AutoSuggestionService autoSuggestionService;
    private final SelfGitAnalyzer selfGitAnalyzer;
    
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
        
        // Initialize Intelligence & Safety
        this.searchService = new InternetSearchService(apiKeys.getOrDefault("TAVILY", ""));
        this.safeZoneManager = new SafeZoneManager();
        this.autoSuggestionService = new AutoSuggestionService(aiService);
        this.selfGitAnalyzer = new SelfGitAnalyzer(".");
        
        // Initialize Phase 3 (Generator)
        this.fileOrchestrator = new FileOrchestrator("projects", this.memoryManager);
        this.templateManager = new TemplateManager("templates", this.fileOrchestrator);
        
        // Initialize Phase 4 (Git + CI/CD + Deployment)
        this.gitService = new GitIntegrationService("projects", firebase);
        this.cicdService = new CICDService("projects", firebase);
        this.deploymentService = new CloudDeploymentService(firebase);
        this.projectTypeManager = new ProjectTypeManager(gitService, cicdService, deploymentService, fileOrchestrator, firebase);
        
        // Initialize Phase 5 (Multi-Account Support)
        this.accountManager = new AIAccountManager(firebase);
        this.budgetManager = new BudgetManager(accountManager, firebase);
        this.publicRouter = new PublicAIRouter(accountManager, budgetManager, aiService, firebase);
        
        // Initialize Phase 6 (Hybrid Data Collection)
        this.quotaTracker = new QuotaTracker(firebase);
        this.apiDataCollector = new APIDataCollector(quotaTracker, firebase);
        this.browserDataCollector = new BrowserDataCollector(quotaTracker, firebase);
        this.hybridDataCollector = new HybridDataCollector(apiDataCollector, browserDataCollector, quotaTracker, firebase);
        
        // Initialize Phase 7 (Cloud Functions & Admin Integration)
        this.dataCollectorService = new DataCollectorService(hybridDataCollector);
        this.webhookListener = new WebhookListener(dataCollectorService);
        this.adminMessagePusher = new AdminMessagePusher();
        
        this.memoryManager.setFirebaseService(firebase);
        initializeAgentPool();
    }
    
    private void initializeAgentPool() {
        agentPool.put("BUILDER", new Agent("builder-1", "X-Builder", Agent.Role.BUILDER, "deepseek"));  
        agentPool.put("REVIEWER", new Agent("reviewer-1", "Y-Reviewer", Agent.Role.REVIEWER, "claude"));
        agentPool.put("ARCHITECT", new Agent("architect-1", "Z-Architect", Agent.Role.ARCHITECT, "gpt-4"));
    }
    
    /**
     * ≡ƒò╡∩╕Å SELF-ANALYSIS: SupremeAI analyzes its own development evolution
     */
    public void runSelfDiagnostic() {
        System.out.println("\n≡ƒò╡∩╕Å [SELF-DIAGNOSTIC] Running Git evolution analysis...");
        
        // 1. Analyze recent history
        List<Map<String, String>> history = selfGitAnalyzer.analyzeSelfHistory(5);
        System.out.println("≡ƒô£ Recent Evolution (Commit History):");
        history.forEach(c -> System.out.println("  [" + c.get("hash") + "] " + c.get("message")));

        // 2. Identify Hotspots
        Map<String, Integer> hotspots = selfGitAnalyzer.identifyHotspots();
        System.out.println("≡ƒöÑ Top Development Hotspots (Most modified files):");
        hotspots.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(h -> System.out.println("  " + h.getKey() + ": " + h.getValue() + " changes"));

        // 3. Check status
        Map<String, String> status = selfGitAnalyzer.checkSelfStatus();
        if (Boolean.parseBoolean(status.get("is_dirty"))) {
            System.out.println("ΓÜá∩╕Å System state is 'Dirty' (Uncommitted work detected).");
        } else {
            System.out.println("Γ£à System state is 'Clean'. Ready for deployment.");
        }
    }

    /**
     * ΓÜí ENHANCED WORKFLOW: Internet Search + SafeZone + Suggestions
     */
    public void processProjectRequirement(String projectId, String requirementDesc) {
        executor.submit(() -> {
            try {
                System.out.println("\n≡ƒÜÇ [ORCHESTRATOR] Analyzing requirement: " + requirementDesc);
                
                // 1. Generate Auto-Suggestions
                List<String> suggestions = autoSuggestionService.suggest(requirementDesc);
                firebaseService.saveChatMessage(projectId, "SupremeAI", 
                    "≡ƒÆí Suggestions: " + String.join(", ", suggestions), "suggestion");

                // 2. Internet Search for context (e.g. latest API changes)
                List<InternetSearchService.SearchResult> research = searchService.search(requirementDesc);
                String enhancedContext = research.isEmpty() ? "" : 
                    "\n\nLatest research findings:\n" + research.get(0).snippet;

                Requirement.Size size = classifier.classify(requirementDesc);
                Requirement requirement = new Requirement(UUID.randomUUID().toString(), 
                    requirementDesc + enhancedContext, size);
                
                if (size == Requirement.Size.HUMAN_REQUIRED) {
                    handleHumanRequired(projectId, requirement);
                    return;
                }
                
                approvalManager.processRequirement(requirement);
                if (requirement.getStatus() == Requirement.Status.APPROVED) {
                    // 3. Execution in SafeZone
                    safeZoneManager.executeInSafeZone(projectId, () -> runWorkflow(projectId, requirement));
                }
            } catch (Exception e) {
                System.err.println("Error processing requirement: " + e.getMessage());
            }
        });
    }

    private void handleHumanRequired(String projectId, Requirement req) {
        firebaseService.saveChatMessage(projectId, "SupremeAI", 
            "≡ƒÜ¿ I need human action for: \"" + req.getDescription() + "\".", "human_required");
    }

    /**
     * Process Git-Based Development Project
     * Admin provides git URL, branch, and development task
     * SupremeAI handles: Clone ΓåÆ Develop ΓåÆ Test ΓåÆ Fix Failures ΓåÆ Commit ΓåÆ Deploy
     */
    public void processGitProject(String projectId, Map<String, String> gitConfig) {
        executor.submit(() -> {
            try {
                String gitUrl = gitConfig.get("gitUrl");
                String branch = gitConfig.get("branch");
                String task = gitConfig.get("task");
                
                System.out.println("\n≡ƒöä [GIT-BASED WORKFLOW] Starting development on: " + gitUrl);
                firebaseService.saveChatMessage(projectId, "System", "≡ƒÜÇ Starting Git-based development workflow...", "system");
                
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
                    "Γ¥î Error: " + e.getMessage(), "error");
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
            
            // ≡ƒÜ¿ SAFETY CHECK before writing
            if (!safeZoneManager.isSafe(finalCode)) {
                firebaseService.saveChatMessage(projectId, "SafeZone", "≡ƒÜ½ Blocked code write due to security risk.", "warning");
                return;
            }

            // Step 4: Write Final Code to Disk (Phase 3)
            fileOrchestrator.writeFile(projectId, "lib/main.dart", finalCode);
            
            System.out.println("≡ƒÄë [PHASE 3] Project files generated successfully at: projects/" + projectId);
            firebaseService.saveChatMessage(projectId, "System", "Γ£à Code generation complete. Files saved locally.", "system");
            
        } catch (IOException e) {
            System.err.println("File generation error: " + e.getMessage());
        }
    }
    
    private String planWithArchitect(String projectId, String requirement) {
        System.out.println("\n≡ƒÅù∩╕Å  [ARCHITECT] Generating plan...");
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
        System.out.println("\n≡ƒö¿ [BUILDER] Generating code...");
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
            try {
    future.get(30, TimeUnit.SECONDS);
} catch (java.util.concurrent.TimeoutException e) {
    // Handle timeout
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
} catch (ExecutionException e) {
    // Handle execution error
}

        }
        return votes;
    }

    public void shutdown() {
        executor.shutdown();
    }
    
    // Getters
    public String selectBestAccountForProvider(String provider) {
        org.example.model.AIAccount account = accountManager.selectBestAccount(provider);
        return account != null ? account.getAccountId() : null;
    }
    public void recordAccountUsage(String accountId, String provider, double costIncurred, int responseTimeMs) {
        accountManager.recordSuccess(accountId, costIncurred, responseTimeMs);
    }
    public void recordAccountFailure(String accountId, String reason) {
        accountManager.recordFailure(accountId, reason);
    }
    public AIAccountManager getAccountManager() { return accountManager; }
    public BudgetManager getBudgetManager() { return budgetManager; }
    public PublicAIRouter.RouterResponse routeAIRequest(String provider, String prompt, Map<String, String> metadata) {
        return publicRouter.routeRequest(provider, prompt, metadata);
    }
    public PublicAIRouter getPublicRouter() { return publicRouter; }
    public QuotaTracker getQuotaTracker() { return quotaTracker; }
    public HybridDataCollector getHybridDataCollector() { return hybridDataCollector; }
    public HybridDataCollector.HybridResult collectGitHubData(String owner, String repo) { return hybridDataCollector.collectGitHubData(owner, repo); }
    public HybridDataCollector.HybridResult collectVercelStatus(String projectId) { return hybridDataCollector.collectVercelStatus(projectId); }
    public HybridDataCollector.HybridResult collectFirebaseStatus() { return hybridDataCollector.collectFirebaseStatus(); }
    public DataCollectorService getDataCollectorService() { return dataCollectorService; }
    public WebhookListener getWebhookListener() { return webhookListener; }
    public AdminMessagePusher getAdminMessagePusher() { return adminMessagePusher; }
    public void pushDataUpdateToAdmin(String s, String i, Map<String, Object> d, long t) { adminMessagePusher.pushDataUpdate(s, i, d, t); }
    public void pushAlertToAdmin(String a, String t, String m, Map<String, Object> mt) { adminMessagePusher.pushAlert(a, t, m, mt); }
}
