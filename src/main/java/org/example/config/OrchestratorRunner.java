package org.example.config;

import org.example.service.AgentOrchestrator;
import org.example.service.FirebaseService;
import org.example.model.SystemConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * 🚀 Cloud Orchestrator Runner
 * Starts the AI Orchestrator in the background when Spring Boot starts on Render.
 */
@Component
public class OrchestratorRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n🤖 Starting SupremeAI Cloud Orchestrator...");
        
        try {
            // Firebase setup
            FirebaseService firebase = new FirebaseService("/service-account.json");
            
            // Load configuration
            Map<String, Object> cloudConfig = firebase.getSystemConfig("main_config");
            SystemConfig config = new SystemConfig();
            config.setConsensusThreshold(0.70);
            
            // Load API Keys from Env or Firebase
            Map<String, String> apiKeys = loadAllAPIKeys(firebase);
            
            // Start Orchestrator
            AgentOrchestrator orchestrator = new AgentOrchestrator(apiKeys, firebase, config);
            System.out.println("✅ Orchestrator is now LIVE and listening for commands.");
            
            // Keep the system running or handle specific cloud startup logic here
            
        } catch (Exception e) {
            System.err.println("❌ Orchestrator Startup Failed: " + e.getMessage());
        }
    }

    private Map<String, String> loadAllAPIKeys(FirebaseService firebase) {
        Map<String, String> apiKeys = new HashMap<>();
        
        // Load from Environment Variables (Render)
        System.getenv().forEach((k, v) -> {
            if (k.contains("API_KEY") || k.contains("MASTER_KEY")) {
                apiKeys.put(k.replace("_API_KEY", ""), v);
            }
        });
        
        // Fallback to Firebase
        Map<String, Object> cloudKeys = firebase.getSystemConfig("api_keys");
        if (cloudKeys != null) {
            cloudKeys.forEach((k, v) -> apiKeys.putIfAbsent(k, String.valueOf(v)));
        }
        
        return apiKeys;
    }
}
