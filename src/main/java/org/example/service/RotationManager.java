package org.example.service;

import org.example.model.Agent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RotationManager {
    private final Map<Agent.Role, List<String>> fallbackChains = new HashMap<>();
    private final Map<Agent.Role, Integer> currentChainIndex = new HashMap<>();

    public RotationManager() {
        // Based on Document v3.0 Table: AI Agent Configuration
        fallbackChains.put(Agent.Role.BUILDER, List.of("DeepSeek", "Groq", "Together AI"));
        fallbackChains.put(Agent.Role.REVIEWER, List.of("Claude", "GPT-4", "DeepSeek"));
        fallbackChains.put(Agent.Role.ARCHITECT, List.of("GPT-4", "Claude", "Groq"));

        for (Agent.Role role : Agent.Role.values()) {
            currentChainIndex.put(role, 0);
        }
    }

    public String getActiveModel(Agent.Role role) {
        List<String> chain = fallbackChains.get(role);
        return chain.get(currentChainIndex.get(role));
    }
    
    /**
     * Get the fallback chain for a given role
     */
    public List<String> getFallbackChain(Agent.Role role) {
        return fallbackChains.getOrDefault(role, List.of());
    }

    public void rotate(Agent.Role role, String reason) {
        int nextIndex = (currentChainIndex.get(role) + 1) % fallbackChains.get(role).size();
        currentChainIndex.put(role, nextIndex);
        System.out.println("🔄 [ROTATION] Triggered for " + role + ". Reason: " + reason);
        System.out.println("📍 New Model: " + getActiveModel(role));
        
        if (reason.contains("429") || reason.contains("403")) {
            triggerVPNSwitch();
        }
    }

    private void triggerVPNSwitch() {
        System.out.println("🌐 [VPN] Switching IP via Proton/Windscribe (Simulated)...");
    }
}
