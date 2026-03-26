package org.example;

import org.example.model.Agent;
import org.example.model.Requirement;
import org.example.model.SystemConfig;
import org.example.service.*;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║    🚀 AI MULTI-AGENT APP GENERATOR SYSTEM v3.1                  ║");
        System.out.println("║    Phase 2: Intelligence & Self-Optimization                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        
        try {
            // ========== INITIALIZATION ==========
            System.out.println("\n⚙️  INITIALIZING SYSTEM...");
            
            // Initialize Firebase
            FirebaseService firebase = new FirebaseService(null);
            System.out.println("✅ Firebase initialized");
            
            // Initialize system configuration
            SystemConfig config = new SystemConfig();
            config.setConsensusThreshold(0.60);
            System.out.println("✅ System config loaded");
            
            // Mock API keys
            Map<String, String> apiKeys = new HashMap<>();
            apiKeys.put("DEEPSEEK", "sk-deepseek-demo-key");
            apiKeys.put("GROQ", "gsk-groq-demo-key");
            apiKeys.put("CLAUDE", "sk-ant-claude-demo-key");
            apiKeys.put("GPT4", "sk-gpt4-demo-key");
            
            // Initialize orchestrator
            AgentOrchestrator orchestrator = new AgentOrchestrator(apiKeys, firebase, config);
            System.out.println("✅ Agent orchestrator ready");

            // ========== SELF-OPTIMIZATION SCAN ==========
            SelfOptimizer optimizer = new SelfOptimizer(new MemoryManager(), firebase);
            optimizer.scanAndOptimizeDependencies();
            
            // ========== WORKFLOW: ORDER STAGE ==========
            System.out.println("\n" + "=".repeat(68));
            System.out.println("STAGE 1: ORDER");
            System.out.println("=".repeat(68));
            
            String projectId = firebase.createProject(
                "E-Commerce Platform",
                "Full-featured e-commerce app with payments",
                "E-commerce app with Stripe payment integration"
            );
            System.out.println("📝 Project created: " + projectId);
            
            String userRequest = "Build an E-commerce app with Stripe payment processing and user authentication";
            System.out.println("\n💬 Admin: \"" + userRequest + "\"");
            
            // ========== WORKFLOW: PLANNING & BUILD ==========
            orchestrator.processProjectRequirement(projectId, userRequest);
            firebase.updateProjectProgress(projectId, 60);
            
            // ========== AI PERFORMANCE METRICS ==========
            System.out.println("\n" + "=".repeat(68));
            System.out.println("PERFORMANCE METRICS & UPGRADE LOG");
            System.out.println("=".repeat(68));
            
            orchestrator.printAIScoreboard();
            
            // Phase 2 Update
            System.out.println("\n" + "=".repeat(68));
            System.out.println("✅ PHASE 2 IN PROGRESS: Intelligence (Memory + Optimizer)");
            System.out.println("=".repeat(68));
            System.out.println("Check document.md for the latest Upgrade Log.");
            
            // Cleanup
            orchestrator.shutdown();
            System.out.println("\n🛑 System shutdown gracefully");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
