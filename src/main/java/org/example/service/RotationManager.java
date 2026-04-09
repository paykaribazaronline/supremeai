package org.example.service;

import org.example.model.Agent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic AI Rotation Manager
 * 
 * MASTER RULE: No hardcoded AI provider names.
 * Fallback chains are built dynamically from whatever providers admin has configured.
 * When admin adds a new AI, it automatically enters the rotation.
 */
public class RotationManager {
    private final Map<Agent.Role, List<String>> fallbackChains = new HashMap<>();
    private final Map<Agent.Role, Integer> currentChainIndex = new HashMap<>();

    public RotationManager() {
        // Initialize with empty chains — populated dynamically via setFallbackChain()
        for (Agent.Role role : Agent.Role.values()) {
            fallbackChains.put(role, new ArrayList<>());
            currentChainIndex.put(role, 0);
        }
    }

    /**
     * Dynamically set the fallback chain for a role from configured providers.
     * Called by the orchestrator after loading provider registry.
     */
    public void setFallbackChain(Agent.Role role, List<String> providers) {
        fallbackChains.put(role, providers != null ? new ArrayList<>(providers) : new ArrayList<>());
        currentChainIndex.put(role, 0);
    }

    public String getActiveModel(Agent.Role role) {
        List<String> chain = fallbackChains.getOrDefault(role, List.of());
        if (chain.isEmpty()) return null; // No providers configured - solo mode
        return chain.get(currentChainIndex.getOrDefault(role, 0) % chain.size());
    }
    
    /**
     * Get the fallback chain for a given role
     */
    public List<String> getFallbackChain(Agent.Role role) {
        return fallbackChains.getOrDefault(role, List.of());
    }

    public void rotate(Agent.Role role, String reason) {
        List<String> chain = fallbackChains.getOrDefault(role, List.of());
        if (chain.isEmpty()) {
            System.out.println("🔧 [SOLO MODE] No providers configured for " + role + " - working solo");
            return;
        }
        int nextIndex = (currentChainIndex.getOrDefault(role, 0) + 1) % chain.size();
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
