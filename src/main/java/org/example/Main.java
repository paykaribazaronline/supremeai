package org.example;

import org.example.service.*;
import org.example.model.SystemConfig;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║    🚀 SUPREME AI - AUTOMATED APP GENERATOR v3.5                 ║");
        System.out.println("║    Phase 3: Real Execution & File Orchestration                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        
        try {
            // ========== INITIALIZATION ==========
            System.out.println("\n⚙️  SYSTEM INITIALIZING...");
            
            // Firebase setup (Uses FIREBASE_SERVICE_ACCOUNT_JSON env var or /service-account.json resource)
            FirebaseService firebase = new FirebaseService();
            
            // ========== LOAD CONFIGURATION FROM FIREBASE ==========
            System.out.println("📥 Loading configuration from Cloud...");
            Map<String, Object> cloudConfig = firebase.getSystemConfig("main_config");
            
            SystemConfig config = new SystemConfig();
            if (cloudConfig != null && !cloudConfig.isEmpty()) {
                if (cloudConfig.containsKey("consensusThreshold")) {
                    Object threshold = cloudConfig.get("consensusThreshold");
                    if (threshold instanceof Number) {
                        config.setConsensusThreshold(((Number) threshold).doubleValue());
                    }
                }
                System.out.println("✅ Configuration loaded from Firebase Realtime Database.");
            } else {
                config.setConsensusThreshold(0.70); // Default: King's 70% rule
                System.out.println("⚠️  Cloud config not found, using defaults.");
                
                // Optional: Save defaults to cloud for next time
                Map<String, Object> defaultConfig = new HashMap<>();
                defaultConfig.put("consensusThreshold", 0.70);
                firebase.saveSystemConfig("main_config", defaultConfig);
            }

            // ========== LOAD API KEYS ==========
            Map<String, String> apiKeys = loadAPIKeys(firebase);

            AgentOrchestrator orchestrator = new AgentOrchestrator(apiKeys, firebase, config);
            System.out.println("✅ Orchestrator connected to Cloud Brain.");

            // ========== REAL TEST RUN: PROJECT GENERATION ==========
            System.out.println("\n" + "=".repeat(68));
            System.out.println("🚀 [EXECUTION] STARTING AUTOMATED GENERATION");
            System.out.println("=".repeat(68));

            String projectId = "real-task-manager-app";
            String task = "Build a complete Flutter task management app with: 1) User authentication (Firebase Auth), 2) Create, read, update, delete tasks with priority levels (High/Medium/Low) and due dates, 3) Task categories/folders, 4) Real-time sync with Firestore database, 5) Offline support with local SQLite caching, 6) Push notifications for due tasks using Firebase Cloud Messaging, 7) Clean Material Design UI with dark mode support.";
            
            System.out.println("📦 Project ID: " + projectId);
            System.out.println("🎯 Task: " + task);

            // Execute Workflow (Planning -> Voting -> Folder Setup -> Code Gen -> File Write)
            orchestrator.processProjectRequirement(projectId, task);

            // Output feedback
            System.out.println("\n⌛ Waiting for parallel processes to finalize files...");
            Thread.sleep(10000); // Wait 10s for the async loop to complete for test purposes

            System.out.println("\n" + "=".repeat(68));
            System.out.println("✅ [TEST COMPLETE] Check 'projects/" + projectId + "' directory.");
            System.out.println("=".repeat(68));

            orchestrator.shutdown();

        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load API keys dynamically from Firebase (admin-controlled)
     */
    private static Map<String, String> loadAPIKeys(FirebaseService firebase) {
        Map<String, String> apiKeys = new HashMap<>();
        
        // 1. Load from "api_keys" node (Admin Controlled via Dashboard)
        Map<String, Object> cloudKeys = firebase.getSystemConfig("api_keys");
        
        if (cloudKeys != null && !cloudKeys.isEmpty()) {
            System.out.println("🔍 Found " + cloudKeys.size() + " API Keys in Firebase:");
            for (Map.Entry<String, Object> entry : cloudKeys.entrySet()) {
                apiKeys.put(entry.getKey(), String.valueOf(entry.getValue()));
                System.out.println("  • " + entry.getKey() + ": [LOADED]");
            }
        }
        
        // 2. Fallback to Environment Variables
        String[] providers = {"DEEPSEEK", "GROQ", "CLAUDE", "GPT4", "GEMINI"};
        for (String p : providers) {
            if (!apiKeys.containsKey(p)) {
                String envKey = System.getenv(p + "_API_KEY");
                if (envKey != null && !envKey.isEmpty()) {
                    apiKeys.put(p, envKey);
                    System.out.println("  • " + p + ": [LOADED FROM ENV]");
                }
            }
        }
        
        if (apiKeys.isEmpty()) {
            System.err.println("\n⚠️  WARNING: No API keys found! Update them via Dashboard.");
        }
        
        return apiKeys;
    }
}
