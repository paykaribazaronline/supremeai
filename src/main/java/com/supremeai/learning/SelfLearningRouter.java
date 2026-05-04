package com.supremeai.learning;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Self-Learning Router - Plan 24 Week7-8
 * Q-Learning based router (SONA-style from Ruflo)
 */
@Component
public class SelfLearningRouter {
    private final Map<String, Double> qTable = new ConcurrentHashMap<>();
    private final Map<String, List<String>> stateAgentMap = new ConcurrentHashMap<>();
    
    private double learningRate = 0.1;
    private double discountFactor = 0.9;
    
    /**
     * Route task to best agent based on learned Q-values
     */
    public String routeTask(String taskType, List<String> availableAgents) {
        String state = extractState(taskType);
        
        // Initialize Q-values if not present
        for (String agent : availableAgents) {
            String key = state + ":" + agent;
            qTable.putIfAbsent(key, 0.0);
        }
        
        // Find best agent
        return availableAgents.stream()
            .max(Comparator.comparingDouble(agent -> 
                qTable.getOrDefault(state + ":" + agent, 0.0)))
            .orElse(availableAgents.get(0));
    }
    
    /**
     * Update Q-values based on task outcome (reward)
     */
    public void updateReward(String taskType, String agentUsed, double reward) {
        String state = extractState(taskType);
        String key = state + ":" + agentUsed;
        
        double oldValue = qTable.getOrDefault(key, 0.0);
        double newValue = oldValue + learningRate * (reward - oldValue);
        qTable.put(key, newValue);
    }
    
    private String extractState(String taskType) {
        // Simple state extraction (can be enhanced with embeddings)
        return taskType.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
    
    /**
     * Get top N agents for a task (for HNSW candidate retrieval)
     */
    public List<String> getTopAgents(String taskType, int n) {
        String state = extractState(taskType);
        return qTable.entrySet().stream()
            .filter(e -> e.getKey().startsWith(state + ":"))
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(n)
            .map(e -> e.getKey().split(":")[1])
            .collect(Collectors.toList());
    }
}
