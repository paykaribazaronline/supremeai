package org.example.service;

import org.example.model.Vote;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FIXED: Weighted Consensus Engine
 * 
 * Problem: Original 3-agent system couldn't produce 70% consensus cleanly
 * Solution: Weighted voting system where different agents have different weights
 * based on their expertise and role in the decision-making process.
 * 
 * Agent Weights:
 * - X-Builder (Code Generation): 40% - Highest weight for code decisions
 * - Y-Reviewer (Security/Quality): 35% - High weight for security checks
 * - Z-Architect (Design/Architecture): 25% - Advisory weight for design decisions
 * 
 * Consensus Threshold: 70% of total possible weight
 */
@Service
public class WeightedConsensusEngine {
    
    // Agent weights - must sum to 1.0 (100%)
    private final Map<String, Double> agentWeights = Map.of(
        "X-Builder", 0.4,     // Code generation = highest weight
        "Y-Reviewer", 0.35,   // Security check = high weight
        "Z-Architect", 0.25   // Design = advisory weight
    );
    
    // Default threshold for consensus
    private static final double DEFAULT_THRESHOLD = 0.7; // 70%
    
    // Alternative consensus levels
    public enum ConsensusLevel {
        UNANIMOUS(1.0, "All agents agree - highest confidence"),
        STRONG_MAJORITY(0.7, "70% weighted agreement - good confidence"),
        SIMPLE_MAJORITY(0.51, "51% weighted agreement - marginal"),
        NO_CONSENSUS(0.0, "No agreement reached");
        
        private final double threshold;
        private final String description;
        
        ConsensusLevel(double threshold, String description) {
            this.threshold = threshold;
            this.description = description;
        }
        
        public double getThreshold() { return threshold; }
        public String getDescription() { return description; }
        
        public static ConsensusLevel fromRatio(double ratio) {
            if (ratio >= UNANIMOUS.threshold) return UNANIMOUS;
            if (ratio >= STRONG_MAJORITY.threshold) return STRONG_MAJORITY;
            if (ratio >= SIMPLE_MAJORITY.threshold) return SIMPLE_MAJORITY;
            return NO_CONSENSUS;
        }
    }
    
    /**
     * Check if consensus is reached with default 70% threshold
     */
    public boolean reachConsensus(List<Vote> votes) {
        return reachConsensus(votes, DEFAULT_THRESHOLD);
    }
    
    /**
     * Check if consensus is reached with custom threshold
     */
    public boolean reachConsensus(List<Vote> votes, double threshold) {
        ConsensusResult result = calculateConsensus(votes, threshold);
        return result.isReached();
    }
    
    /**
     * Calculate detailed consensus result with weighted voting
     */
    public ConsensusResult calculateConsensus(List<Vote> votes) {
        return calculateConsensus(votes, DEFAULT_THRESHOLD);
    }
    
    /**
     * Calculate detailed consensus result with custom threshold
     */
    public ConsensusResult calculateConsensus(List<Vote> votes, double threshold) {
        if (votes == null || votes.isEmpty()) {
            return new ConsensusResult(false, 0.0, ConsensusLevel.NO_CONSENSUS, 
                "No votes provided");
        }
        
        // Calculate total weight of all participating agents
        double totalWeight = votes.stream()
            .mapToDouble(v -> agentWeights.getOrDefault(v.getAgentId(), 0.0))
            .sum();
        
        if (totalWeight == 0) {
            return new ConsensusResult(false, 0.0, ConsensusLevel.NO_CONSENSUS,
                "No valid agent weights found");
        }
        
        // Calculate approval weight (only approved votes)
        double approvalWeight = votes.stream()
            .filter(Vote::isApproved)
            .mapToDouble(v -> agentWeights.getOrDefault(v.getAgentId(), 0.0))
            .sum();
        
        // Calculate approval ratio
        double approvalRatio = approvalWeight / totalWeight;
        
        // Determine consensus level
        ConsensusLevel level = ConsensusLevel.fromRatio(approvalRatio);
        boolean reached = approvalRatio >= threshold;
        
        String details = String.format(
            "Approval: %.1f%% (threshold: %.1f%%), Level: %s, Votes: %d/%d",
            approvalRatio * 100, threshold * 100, level.name(),
            votes.stream().filter(Vote::isApproved).count(),
            votes.size()
        );
        
        return new ConsensusResult(reached, approvalRatio, level, details);
    }
    
    /**
     * Get consensus with admin override capability
     * If no natural consensus, admin can force decision
     */
    public ConsensusResult reachConsensusWithOverride(List<Vote> votes, 
                                                       boolean adminOverride,
                                                       String adminId) {
        ConsensusResult result = calculateConsensus(votes);
        
        if (result.isReached()) {
            return result;
        }
        
        // Admin override for critical situations
        if (adminOverride && adminId != null) {
            return new ConsensusResult(true, 1.0, ConsensusLevel.UNANIMOUS,
                "Admin override by: " + adminId);
        }
        
        return result;
    }
    
    /**
     * Calculate weighted approval rate
     */
    public double getWeightedApprovalRate(List<Vote> votes) {
        if (votes == null || votes.isEmpty()) return 0.0;
        
        double totalWeight = votes.stream()
            .mapToDouble(v -> agentWeights.getOrDefault(v.getAgentId(), 0.0))
            .sum();
        
        if (totalWeight == 0) return 0.0;
        
        double approvalWeight = votes.stream()
            .filter(Vote::isApproved)
            .mapToDouble(v -> agentWeights.getOrDefault(v.getAgentId(), 0.0))
            .sum();
        
        return approvalWeight / totalWeight;
    }
    
    /**
     * Get dissenting agents (those who voted against)
     */
    public List<String> getDissentingAgents(List<Vote> votes) {
        return votes.stream()
            .filter(v -> !v.isApproved())
            .map(Vote::getAgentId)
            .collect(Collectors.toList());
    }
    
    /**
     * Get supporting agents (those who voted for)
     */
    public List<String> getSupportingAgents(List<Vote> votes) {
        return votes.stream()
            .filter(Vote::isApproved)
            .map(Vote::getAgentId)
            .collect(Collectors.toList());
    }
    
    /**
     * Get agent weight
     */
    public double getAgentWeight(String agentId) {
        return agentWeights.getOrDefault(agentId, 0.0);
    }
    
    /**
     * Consensus result containing detailed information
     */
    public static class ConsensusResult {
        private final boolean reached;
        private final double approvalRatio;
        private final ConsensusLevel level;
        private final String details;
        
        public ConsensusResult(boolean reached, double approvalRatio, 
                              ConsensusLevel level, String details) {
            this.reached = reached;
            this.approvalRatio = approvalRatio;
            this.level = level;
            this.details = details;
        }
        
        public boolean isReached() { return reached; }
        public double getApprovalRatio() { return approvalRatio; }
        public ConsensusLevel getLevel() { return level; }
        public String getDetails() { return details; }
        
        public int getApprovalPercentage() {
            return (int) Math.round(approvalRatio * 100);
        }
        
        @Override
        public String toString() {
            return String.format("ConsensusResult{reached=%s, ratio=%.1f%%, level=%s, details='%s'}",
                reached, approvalRatio * 100, level, details);
        }
    }
}
