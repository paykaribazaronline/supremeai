package org.example.service;

import org.example.model.Agent;
import org.example.model.Requirement;
import org.example.model.Vote;
import org.example.model.SystemConfig;

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
    
    private final ExecutorService executor = Executors.newFixedThreadPool(8); // Increased threads for parallel tasks
    private final Map<String, Agent> agentPool = new HashMap<>();
    private final Set<String> blockedRequirements = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    public AgentOrchestrator(Map<String, String> apiKeys, FirebaseService firebase, SystemConfig cfg) {
        this.aiService = new AIAPIService(apiKeys);
        this.consensusEngine = new ConsensusEngine(0.70);
        this.approvalManager = new ApprovalManager();
        this.rotationManager = new RotationManager();
        this.classifier = new RequirementClassifier();
        this.memoryManager = new MemoryManager();
        this.firebaseService = firebase;
        this.config = cfg;
        
        this.memoryManager.setFirebaseService(firebase);
        initializeAgentPool();
    }
    
    private void initializeAgentPool() {
        agentPool.put("BUILDER", new Agent("builder-1", "X-Builder", Agent.Role.BUILDER, "deepseek"));  
        agentPool.put("REVIEWER", new Agent("reviewer-1", "Y-Reviewer", Agent.Role.REVIEWER, "claude"));
        agentPool.put("ARCHITECT", new Agent("architect-1", "Z-Architect", Agent.Role.ARCHITECT, "gpt-4"));
    }
    
    /**
     * Entry point for a new project requirement.
     * Operates in a non-blocking way so other requirements can proceed.
     */
    public void processProjectRequirement(String projectId, String requirementDesc) {
        executor.submit(() -> {
            try {
                System.out.println("\n🚀 [ORCHESTRATOR] Analyzing requirement: " + requirementDesc);
                
                Requirement.Size size = classifier.classify(requirementDesc);
                Requirement requirement = new Requirement(UUID.randomUUID().toString(), requirementDesc, size);
                
                // Check if this specific requirement needs human input
                if (size == Requirement.Size.HUMAN_REQUIRED) {
                    handleHumanRequired(projectId, requirement);
                    return; // Stop this thread, but others continue
                }
                
                approvalManager.processRequirement(requirement);
                firebaseService.saveChatMessage(projectId, "system", 
                    "Requirement classified as: " + size, "system");
                
                if (requirement.getStatus() == Requirement.Status.APPROVED) {
                    runWorkflow(projectId, requirement);
                } else if (size == Requirement.Size.BIG) {
                    System.out.println("⏳ [WORKFLOW] Blocked: Waiting for Admin Approval for BIG task: " + requirementDesc);
                    firebaseService.sendNotification("admin", "Approval Needed", requirementDesc, "approval");
                }
            } catch (Exception e) {
                System.err.println("Error processing requirement: " + e.getMessage());
            }
        });
    }

    private void handleHumanRequired(String projectId, Requirement req) {
        System.out.println("🚨 [HUMAN_ACTION_REQUIRED] Blocked Task: " + req.getDescription());
        blockedRequirements.add(req.getId());
        req.setStatus(Requirement.Status.WAITING_FOR_HUMAN);
        
        firebaseService.saveChatMessage(projectId, "SupremeAI", 
            "🚨 I cannot proceed with this task: \"" + req.getDescription() + "\". I need your help (King Mode/Manual Action).", 
            "human_required");
        
        firebaseService.sendNotification("admin", "🚨 Action Required", 
            "SupremeAI is stuck on: " + req.getDescription(), "human_action");
        
        System.out.println("ℹ️  [ORCHESTRATOR] Other tasks will continue in parallel...");
    }

    private void runWorkflow(String projectId, Requirement requirement) {
        // This is a simplified version of the loop logic we discussed
        planWithArchitect(projectId, requirement.getDescription());
    }
    
    private void planWithArchitect(String projectId, String requirement) {
        System.out.println("\n🏗️  [ARCHITECT] Generating plan...");
        
        String currentPlan = aiService.callAI("ARCHITECT", "Plan architecture for: " + requirement, rotationManager.getFallbackChain(Agent.Role.ARCHITECT));
        
        boolean consensusReached = false;
        int loopCount = 0;
        
        while (!consensusReached && loopCount < 5) {
            loopCount++;
            List<Vote> votes = requestConsensus("PLAN_VOTE_" + loopCount, currentPlan);
            double approvalRate = consensusEngine.getApprovalRate(votes);
            String improvements = consensusEngine.collectImprovements(votes);
            
            if (approvalRate >= 0.70 && improvements.isEmpty()) {
                consensusReached = true;
            } else {
                currentPlan = aiService.callAI("ARCHITECT", "Improve this plan: " + currentPlan + "\nSuggestions: " + improvements, rotationManager.getFallbackChain(Agent.Role.ARCHITECT));
            }
        }
        
        firebaseService.saveChatMessage(projectId, "Z-Architect", currentPlan, "ai");
        buildWithCodeGenerator(projectId, requirement, currentPlan);
    }
    
    private void buildWithCodeGenerator(String projectId, String requirement, String plan) {
        System.out.println("\n🔨 [BUILDER] Generating code...");
        String currentCode = aiService.callAI("BUILDER", "Generate code for: " + requirement + " based on: " + plan, rotationManager.getFallbackChain(Agent.Role.BUILDER));
        
        boolean consensusReached = false;
        int loopCount = 0;
        
        while (!consensusReached && loopCount < 5) {
            loopCount++;
            List<Vote> votes = requestConsensus("CODE_VOTE_" + loopCount, currentCode);
            double approvalRate = consensusEngine.getApprovalRate(votes);
            String improvements = consensusEngine.collectImprovements(votes);
            
            if (approvalRate >= 0.70 && improvements.isEmpty()) {
                consensusReached = true;
            } else {
                currentCode = aiService.callAI("BUILDER", "Fix this code: " + currentCode + "\nIssues: " + improvements, rotationManager.getFallbackChain(Agent.Role.BUILDER));
            }
        }
        
        firebaseService.saveChatMessage(projectId, "X-Builder", currentCode, "ai");
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
