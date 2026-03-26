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
            FirebaseService firebase = new FirebaseService("/service-account.json");
            
            // ========== LOAD CONFIGURATION FROM FIREBASE ==========
            System.out.println("📥 Loading configuration from Cloud...");
            Map<String, Object> cloudConfig = firebase.getSystemConfig("main_config");
            
            SystemConfig config = new SystemConfig();
            if (cloudConfig != null && !cloudConfig.isEmpty()) {
                if (cloudConfig.containsKey("consensusThreshold")) {
                    config.setConsensusThreshold((Double) cloudConfig.get("consensusThreshold"));
                }
                System.out.println("✅ Configuration loaded from Firestore.");
            } else {
                config.setConsensusThreshold(0.70); // Default: King's 70% rule
                System.out.println("⚠️  Cloud config not found, using defaults.");
                
                // Optional: Save defaults to cloud for next time
                Map<String, Object> defaultConfig = new HashMap<>();
                defaultConfig.put("consensusThreshold", 0.70);
                firebase.saveSystemConfig("main_config", defaultConfig);
            }

            // ========== LOAD API KEYS ==========
            // Priority: 1. Firebase (Securely stored), 2. Environment Variables
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
     * Load API keys dynamically from Firestore (admin-controlled)
     * 
     * Priority:
     * 1. Firestore (Admin-managed list - SINGLE SOURCE OF TRUTH)
     * 2. Environment Variables (Fallback for each provider found in Firestore)
     * 
     * Admin can add NEW providers anytime via dashboard without code changes.
     */
    private static Map<String, String> loadAPIKeys(FirebaseService firebase) {
        Map<String, String> apiKeys = new HashMap<>();
        
        // 1. Load provider list from Firestore (ADMIN CONTROLS THIS)
        Map<String, Object> firebaseProviders = firebase.getSystemConfig("api_providers");
        
        if (firebaseProviders != null && !firebaseProviders.isEmpty()) {
            System.out.println("🔍 Found " + firebaseProviders.size() + " configured AI providers:");
            
            for (Map.Entry<String, Object> entry : firebaseProviders.entrySet()) {
                String providerName = entry.getKey();
                Object providerConfig = entry.getValue();
                
                System.out.println("  • " + providerName);
                
                if (providerConfig instanceof Map) {
                    Map<String, Object> config = (Map<String, Object>) providerConfig;
                    String key = (String) config.get("key");
                    if (key != null && !key.isEmpty()) {
                        apiKeys.put(providerName, key);
                        System.out.println("    ✅ Key loaded from Firestore");
                    }
                } else if (providerConfig instanceof String) {
                    apiKeys.put(providerName, (String) providerConfig);
                    System.out.println("    ✅ Key loaded from Firestore");
                }
            }
        }
        
        // 2. Fallback to environment variables (for any provider)
        for (String provider : apiKeys.keySet()) {
            if (apiKeys.get(provider) == null || apiKeys.get(provider).isEmpty()) {
                String envKey = System.getenv(provider.toUpperCase() + "_API_KEY");
                if (envKey != null && !envKey.isEmpty()) {
                    apiKeys.put(provider, envKey);
                    System.out.println("    ✅ Key loaded from Environment: " + provider.toUpperCase() + "_API_KEY");
                }
            }
        }
        
        if (apiKeys.isEmpty()) {
            System.err.println("\n⚠️  WARNING: No AI providers configured in Firestore!");
            System.err.println("   → Admin must use dashboard to add providers:");
            System.err.println("   → http://localhost:8001 → AI Providers → Add New");
        } else {
            System.out.println("\n✅ Total providers ready: " + apiKeys.size());
        }
        
        return apiKeys;
    }
}
