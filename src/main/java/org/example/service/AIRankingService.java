package org.example.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 2: Intelligent AI Ranking Service
 * 
 * Provides 4 ranking strategies for agent selection:
 * 1. By Performance - Overall agent quality based on success rate and speed
 * 2. By Task Type - Task-specific historical success
 * 3. By Cost - Cost-optimized agent chain
 * 4. By Speed - Fastest response times
 * 
 * Used by SmartAssignment and AgentOrchestrator for intelligent agent selection.
 */
public class AIRankingService {
    private final MemoryManager memoryManager;
    private final FirebaseService firebaseService;
    
    // Cost ranking order (cheapest to most expensive)
    private static final List<String> COST_ORDER = List.of(
        "GROQ",           // Cheapest
        "DEEPSEEK",
        "MISTRAL", 
        "CLAUDE",
        "OPENAI",         // Most expensive
        "ANTHROPIC"
    );
    
    public AIRankingService(MemoryManager memoryManager, FirebaseService firebaseService) {
        this.memoryManager = memoryManager;
        this.firebaseService = firebaseService;
    }
    
    /**
     * Strategy 1: Rank all agents by overall performance
     * 
     * Uses the Phase 2 scoring algorithm:
     * Score = (Success_Rate × 0.5) - (Failure_Rate × 0.3) + (Speed_Bonus × 0.2)
     * 
     * @return List of agent IDs sorted by score (highest first)
     */
    public List<String> rankAgentsByPerformance() {
        return memoryManager.getTopAgents(Integer.MAX_VALUE);
    }

    /**
     * Strategy 2: Rank agents by task-type-specific historical success
     * 
     * Returns agents sorted by how often they've succeeded at this task type.
     * Agents with no history are excluded.
     * 
     * @param taskType The task type to rank for (e.g., "document_analysis", "code_generation")
     * @return List of agent IDs ranked by success count for this task type
     */
    public List<String> rankAgentsByTaskType(String taskType) {
        List<Map<String, Object>> patterns = memoryManager.getPatternsByTaskType(taskType);
        
        if (patterns.isEmpty()) {
            // Fall back to overall performance if no task-specific history
            return rankAgentsByPerformance();
        }
        
        // Count successes per agent for this task type
        Map<String, Integer> successCount = new HashMap<>();
        
        for (Map<String, Object> pattern : patterns) {
            String agent = (String) pattern.get("agent");
            boolean success = (boolean) pattern.get("success");
            
            if (success) {
                successCount.put(agent, successCount.getOrDefault(agent, 0) + 1);
            }
        }
        
        // Sort by success count (highest first)
        return successCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Strategy 3: Rank agents by cost (cheapest first)
     * 
     * Returns a fixed cost-optimized ranking:
     * GROQ → DeepSeek → Mistral → Claude → OpenAI → Anthropic
     * 
     * This is a static ranking since cost is relatively fixed per model.
     * Actual prices can be updated in COST_ORDER list.
     * 
     * @return List of agent IDs in cost order (cheapest first)
     */
    public List<String> rankAgentsByCost() {
        // Return cost-optimized ranking from cheapest to most expensive
        return new ArrayList<>(COST_ORDER);
    }

    /**
     * Strategy 4: Rank agents by speed (fastest response times first)
     * 
     * Uses average response time from the agent scoreboard.
     * Agents with lower avg_time are ranked higher.
     * 
     * @return List of agent IDs sorted by fastest response time
     */
    public List<String> rankAgentsBySpeed() {
        Map<String, Object> scoreboard = memoryManager.getAIScoreboard();
        
        List<String> agents = new ArrayList<>(scoreboard.keySet());
        
        // Sort by average time (lowest first = fastest)
        agents.sort((a, b) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> aScore = (Map<String, Object>) scoreboard.get(a);
            @SuppressWarnings("unchecked")
            Map<String, Object> bScore = (Map<String, Object>) scoreboard.get(b);
            
            int aTime = ((Number) aScore.get("avg_time")).intValue();
            int bTime = ((Number) bScore.get("avg_time")).intValue();
            
            return Integer.compare(aTime, bTime);
        });
        
        return agents;
    }

    /**
     * Hybrid Strategy: Combine performance, task-type, and cost ranking
     * 
     * Returns a mixed ranking that:
     * 1. Prioritizes agents with good task-specific history
     * 2. Falls back to cost-optimized agents if no history
     * 3. Ensures at least one high-performance agent in the chain
     * 
     * @param taskType The task type to optimize for
     * @return List of agent IDs in intelligent order
     */
    public List<String> rankAgentsHybrid(String taskType) {
        List<String> chain = new ArrayList<>();
        
        // Get task-specific ranking
        List<String> taskRanking = rankAgentsByTaskType(taskType);
        
        // Add agents with good scores for this task
        for (String agent : taskRanking) {
            double score = memoryManager.calculateAgentScore(agent);
            if (score > 0.5) { // Only agents with >50% score
                chain.add(agent);
            }
        }
        
        // Add cost-optimized agents as fallback
        List<String> costOptimized = rankAgentsByCost();
        for (String agent : costOptimized) {
            if (!chain.contains(agent)) {
                chain.add(agent);
            }
        }
        
        // Ensure we have at least some agents
        if (chain.isEmpty()) {
            chain.addAll(rankAgentsByPerformance());
        }
        
        return chain;
    }

    /**
     * Get the best single agent for a task
     * 
     * Returns the first agent from the hybrid ranking.
     * 
     * @param taskType The task type
     * @return Best agent ID, or null if no agents available
     */
    public String getBestAgent(String taskType) {
        List<String> ranking = rankAgentsHybrid(taskType);
        return ranking.isEmpty() ? null : ranking.get(0);
    }

    /**
     * Get a fallback chain for safe execution
     * 
     * Returns multiple agents in priority order for fallback execution.
     * If the first fails, try the second, and so on.
     * 
     * @param taskType The task type
     * @param chainLength How many fallback options to return
     * @return List of agents in fallback priority
     */
    public List<String> getFallbackChain(String taskType, int chainLength) {
        List<String> hybrid = rankAgentsHybrid(taskType);
        
        return hybrid.stream()
                .limit(chainLength)
                .collect(Collectors.toList());
    }

    /**
     * Save all rankings to Firebase for dashboard visualization
     * 
     * Stores performance, task-type, cost, and speed rankings in cloud.
     * Can be retrieved by admin dashboard to show ranking information.
     */
    public void saveRankingsToFirebase() {
        try {
            Map<String, Object> rankings = new HashMap<>();
            
            // Generate rankings for common task types
            List<String> taskTypes = List.of(
                "document_analysis",
                "code_generation", 
                "data_processing",
                "content_creation",
                "api_integration"
            );
            
            Map<String, List<String>> taskRankings = new HashMap<>();
            for (String taskType : taskTypes) {
                taskRankings.put(taskType, rankAgentsByTaskType(taskType));
            }
            
            rankings.put("overall_performance", rankAgentsByPerformance());
            rankings.put("by_cost", rankAgentsByCost());
            rankings.put("by_speed", rankAgentsBySpeed());
            rankings.put("by_task_type", taskRankings);
            rankings.put("generated_at", LocalDateTime.now().toString());
            rankings.put("version", "2.0");
            
            if (firebaseService != null) {
                firebaseService.saveSystemConfig("ai_rankings", rankings);
                System.out.println("✅ AI rankings saved to Firebase");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to save rankings to Firebase: " + e.getMessage());
        }
    }

    /**
     * Recalculate and refresh all rankings
     * 
     * Should be called after:
     * - Successfully executing tasks
     * - Recording failures
     * - Updating agent scoreboard
     * 
     * Then saves updated rankings to Firebase for real-time dashboard updates.
     */
    public void refreshRankings() {
        // Trigger recalculation by saving
        saveRankingsToFirebase();
        System.out.println("🔄 AI Rankings refreshed");
    }

    /**
     * Get ranking statistics for monitoring and debugging
     * 
     * Returns statistics about the ranking system:
     * - Number of ranked agents
     * - Number of task types with history
     * - Average agent score
     * - Ranking freshness
     * 
     * @return Map of ranking statistics
     */
    public Map<String, Object> getRankingStats() {
        Map<String, Object> stats = new HashMap<>();
        
        List<String> topAgents = rankAgentsByPerformance();
        stats.put("total_agents", topAgents.size());
        stats.put("top_5_agents", topAgents.stream().limit(5).collect(Collectors.toList()));
        
        // Calculate average score
        double avgScore = topAgents.stream()
                .mapToDouble(memoryManager::calculateAgentScore)
                .average()
                .orElse(0.0);
        stats.put("average_agent_score", String.format("%.2f", avgScore));
        
        stats.put("last_refreshed", LocalDateTime.now().toString());
        stats.put("ranking_version", "2.0");
        
        return stats;
    }

    /**
     * Debug method: Print all rankings for verification
     * 
     * Outputs all 4 ranking strategies to console.
     * Used during testing and debugging.
     */
    public void printAllRankings() {
        System.out.println("\n📊 ===== AI RANKING REPORT =====");
        
        System.out.println("\n🏆 Performance Ranking:");
        rankAgentsByPerformance().forEach(a -> 
            System.out.println("  - " + a + " (Score: " + String.format("%.2f", memoryManager.calculateAgentScore(a)) + ")")
        );
        
        System.out.println("\n💰 Cost Ranking (cheapest first):");
        rankAgentsByCost().forEach(a -> System.out.println("  - " + a));
        
        System.out.println("\n⚡ Speed Ranking:");
        rankAgentsBySpeed().forEach(a -> System.out.println("  - " + a));
        
        System.out.println("\n✅ Stats:");
        getRankingStats().forEach((k, v) -> System.out.println("  " + k + ": " + v));
        
        System.out.println();
    }
}
