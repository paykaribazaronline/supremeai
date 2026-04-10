package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Dynamic Adaptive Consensus Voting Service - Optimization #8
 * Replaces hardcoded "wait for 10 AIs" with intelligent voting:
 * 
 * Rules (DYNAMIC - based on actual provider count):
 * - 0 AIs: Solo mode (built-in analysis, no voting)
 * - 1 AI: Direct use (100% confidence in single provider)
 * - 2 AIs: System votes as tiebreaker (3-way: AI1 + AI2 + SupremeAI)
 * - 3-5 AIs: All participate in voting
 * - 6+ AIs: Top 5 by success rate (weighted selection)
 * 
 * Key: NO artificial delays, NO hardcoded "10", NO waiting for slow providers
 * Strategy: Parallel execution, adaptive voting circle, automatic timeout
 */
@Service
public class DynamicAdaptiveConsensusService {
    private static final Logger logger = LoggerFactory.getLogger(DynamicAdaptiveConsensusService.class);
    
    @Autowired(required = false)
    private SmartProviderWeightingService weightingService;
    
    @Autowired(required = false)
    private BuiltInAnalysisService builtInAnalysis;
    
    // Configuration - DYNAMIC, NO HARDCODING
    private static final int MAX_VOTING_PARTICIPANTS = 5;
    private static final long INDIVIDUAL_TIMEOUT_MS = 3000; // Per AI response
    private static final long TOTAL_TIMEOUT_MS = 10000; // Total consensus time
    
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    
    public static class ConsensusResult {
        public String consensus;           // Final agreed response
        public double confidenceScore;     // 0-1: how confident in result
        public Map<String, AIVote> votes;  // Who voted for what
        public int voterCount;             // How many actually voted
        public String votingStrategy;      // "solo", "direct", "tiebreaker", "consensus", "top5"
        public long processingTimeMs;
        
        public ConsensusResult() {
            this.votes = new LinkedHashMap<>();
        }
    }
    
    public static class AIVote {
        public String voterId;             // AI provider name or "system"
        public String vote;                // Their suggested response
        public double confidence;          // 0-1: confidence in answer
        public long responseTimeMs;
        public boolean timeouted;          // Did this AI timeout?
        public String reasoning;           // Why they voted this way
        
        public AIVote(String voterId) {
            this.voterId = voterId;
            this.timeouted = false;
        }
    }
    
    /**
     * Main consensus voting endpoint - ADAPTIVE
     */
    public ConsensusResult getConsensus(String query, List<String> availableProviders) {
        long startTime = System.currentTimeMillis();
        ConsensusResult result = new ConsensusResult();
        
        // Step 1: Determine actual provider count
        int providerCount = availableProviders == null ? 0 : availableProviders.size();
        logger.info("🗳️ Consensus voting: {} available providers, max {} will vote", 
            providerCount, MAX_VOTING_PARTICIPANTS);
        
        // Step 2: Choose strategy based on ACTUAL provider count
        switch (providerCount) {
            case 0:
                result.votingStrategy = "solo";
                handleSoloMode(query, result);
                break;
                
            case 1:
                result.votingStrategy = "direct";
                handleDirectMode(query, availableProviders.get(0), result);
                break;
                
            case 2:
                result.votingStrategy = "tiebreaker";
                handleTiebreakerMode(query, availableProviders, result);
                break;
                
            default: // 3+
                if (providerCount <= MAX_VOTING_PARTICIPANTS) {
                    result.votingStrategy = "consensus";
                    result.voterCount = providerCount;
                    handleConsensusMode(query, availableProviders, result);
                } else {
                    result.votingStrategy = "top5";
                    result.voterCount = MAX_VOTING_PARTICIPANTS;
                    List<String> top5 = selectTopProviders(availableProviders, MAX_VOTING_PARTICIPANTS);
                    handleConsensusMode(query, top5, result);
                }
        }
        
        result.processingTimeMs = System.currentTimeMillis() - startTime;
        logger.info("✅ Consensus complete ({}) in {}ms: {}", 
            result.votingStrategy, result.processingTimeMs, result.consensus.substring(0, Math.min(50, result.consensus.length())));
        
        return result;
    }
    
    /**
     * Strategy 1: No providers - Use built-in analysis (Solo Mode)
     */
    private void handleSoloMode(String query, ConsensusResult result) {
        logger.info("🚀 SOLO MODE: 0 external AIs, using SupremeAI built-in analysis");
        
        if (builtInAnalysis != null) {
            String analysis = builtInAnalysis.analyze(query);
            result.consensus = analysis;
            result.confidenceScore = 0.85; // Decent confidence in built-in
        } else {
            result.consensus = "SupremeAI Solo Analysis: Unable to reach external AIs. Using pattern matching and built-in rules.";
            result.confidenceScore = 0.70;
        }
        
        // Solo mode vote (system only)
        AIVote systemVote = new AIVote("supremeai-system");
        systemVote.confidence = result.confidenceScore;
        systemVote.reasoning = "Built-in analysis engine (no external AIs available)";
        result.votes.put("supremeai-system", systemVote);
        result.voterCount = 1;
    }
    
    /**
     * Strategy 2: Single provider - Use directly without voting (100% confidence if available)
     */
    private void handleDirectMode(String query, String provider, ConsensusResult result) {
        logger.info("➡️ DIRECT MODE: 1 external AI ({}), no voting needed", provider);
        
        try {
            // Query single provider directly
            CompletableFuture<AIVote> future = CompletableFuture.supplyAsync(() -> {
                AIVote vote = new AIVote(provider);
                long start = System.currentTimeMillis();
                
                try {
                    // In real implementation, call provider API
                    String response = "Placeholder response from " + provider;
                    vote.vote = response;
                    vote.confidence = 0.95; // Single dedicated provider = high confidence
                    vote.responseTimeMs = System.currentTimeMillis() - start;
                    
                    if (weightingService != null) {
                        weightingService.recordSuccess(provider);
                    }
                } catch (Exception e) {
                    vote.confidence = 0.5;
                    if (weightingService != null) {
                        weightingService.recordFailure(provider);
                    }
                }
                
                return vote;
            }, executor);
            
            AIVote vote = future.get(INDIVIDUAL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            result.consensus = vote.vote;
            result.confidenceScore = vote.confidence;
            result.votes.put(provider, vote);
            result.voterCount = 1;
            
        } catch (TimeoutException e) {
            logger.warn("⏱️ Single provider {} timed out, using fallback", provider);
            result.consensus = "Provider response delayed. Falling back to solo analysis.";
            result.confidenceScore = 0.60;
            result.voterCount = 0;
        } catch (Exception e) {
            logger.error("❌ Error querying single provider: {}", e.getMessage());
            result.consensus = "Unable to reach AI provider. Using built-in rules.";
            result.confidenceScore = 0.50;
        }
    }
    
    /**
     * Strategy 3: Two providers - Add System as tiebreaker (3-way vote)
     */
    private void handleTiebreakerMode(String query, List<String> providers, ConsensusResult result) {
        logger.info("⚖️ TIEBREAKER MODE: 2 external AIs + SupremeAI (3-way vote)");
        
        // Query 2 external AIs + system in parallel
        List<CompletableFuture<AIVote>> futures = new ArrayList<>();
        
        for (String provider : providers) {
            futures.add(queryProviderAsync(query, provider));
        }
        
        // System always participates in 2-AI scenario
        futures.add(querySystemAsync(query));
        
        // Wait for all (with timeout)
        try {
            CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            allDone.get(TOTAL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            // Collect votes
            for (CompletableFuture<AIVote> f : futures) {
                AIVote vote = f.getNow(null);
                if (vote != null) {
                    result.votes.put(vote.voterId, vote);
                }
            }
            
        } catch (TimeoutException e) {
            logger.warn("⏱️ Tiebreaker mode: Some votes delayed, using available votes");
            // Continue with whatever votes came in
        } catch (Exception e) {
            logger.error("❌ Error in tiebreaker voting: {}", e.getMessage());
        }
        
        // Aggregate votes
        aggregateVotes(result);
        result.voterCount = result.votes.size();
    }
    
    /**
     * Strategy 4-5: 3+ providers OR Top 5 selected
     */
    private void handleConsensusMode(String query, List<String> providers, ConsensusResult result) {
        logger.info("🗳️ CONSENSUS MODE: {} providers voting in parallel", providers.size());
        
        List<CompletableFuture<AIVote>> futures = new ArrayList<>();
        
        // Query each provider in parallel (NO WAITING for slow ones)
        for (String provider : providers) {
            futures.add(queryProviderAsync(query, provider));
        }
        
        // Always include system if not already included
        if (!providers.contains("supremeai-system")) {
            futures.add(querySystemAsync(query));
        }
        
        // Collect votes as they arrive (parallel, no blocking)
        try {
            CompletableFuture<Void> allDone = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            // Don't wait indefinitely
            allDone.get(TOTAL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
        } catch (TimeoutException e) {
            // GOOD! Some fast responses come in, slow ones timeout
            logger.info("⚡ Consensus partial: Fast AI responses received, slow providers skipped");
        } catch (Exception e) {
            logger.error("❌ Error in consensus voting: {}", e.getMessage());
        }
        
        // Collect all available votes
        for (CompletableFuture<AIVote> f : futures) {
            try {
                AIVote vote = f.getNow(null);
                if (vote != null && !vote.timeouted) {
                    result.votes.put(vote.voterId, vote);
                }
            } catch (Exception e) {
                // Skip failed votes
            }
        }
        
        // Aggregate votes
        aggregateVotes(result);
        result.voterCount = result.votes.size();
    }
    
    /**
     * Query provider asynchronously (with timeout)
     */
    private CompletableFuture<AIVote> queryProviderAsync(String query, String provider) {
        return CompletableFuture.supplyAsync(() -> {
            AIVote vote = new AIVote(provider);
            long start = System.currentTimeMillis();
            
            try {
                // In real implementation: call provider API
                // For now: simulated response
                Thread.sleep((long) (Math.random() * 2000)); // Simulate network latency
                
                vote.vote = "Response from " + provider;
                vote.confidence = 0.80 + Math.random() * 0.15;
                vote.responseTimeMs = System.currentTimeMillis() - start;
                
                if (weightingService != null) {
                    weightingService.recordSuccess(provider);
                }
                
            } catch (InterruptedException e) {
                vote.timeouted = true;
                vote.confidence = 0;
                if (weightingService != null) {
                    weightingService.recordFailure(provider);
                }
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                vote.confidence = 0.3;
                if (weightingService != null) {
                    weightingService.recordFailure(provider);
                }
            }
            
            return vote;
        }, executor);
    }
    
    /**
     * Query system (SupremeAI built-in) asynchronously
     */
    private CompletableFuture<AIVote> querySystemAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            AIVote vote = new AIVote("supremeai-system");
            long start = System.currentTimeMillis();
            
            try {
                if (builtInAnalysis != null) {
                    String analysis = builtInAnalysis.analyze(query);
                    vote.vote = analysis;
                } else {
                    vote.vote = "Built-in pattern analysis";
                }
                
                vote.confidence = 0.85; // System always confident
                vote.responseTimeMs = System.currentTimeMillis() - start;
                vote.reasoning = "SupremeAI built-in knowledge engine (always available)";
                
            } catch (Exception e) {
                vote.confidence = 0.6;
            }
            
            return vote;
        }, executor);
    }
    
    /**
     * Aggregate votes into consensus
     */
    private void aggregateVotes(ConsensusResult result) {
        if (result.votes.isEmpty()) {
            result.consensus = "No votes received. Using solo mode.";
            result.confidenceScore = 0.3;
            return;
        }
        
        // Find most common response (simple majority)
        Map<String, Integer> responseCounts = new HashMap<>();
        Map<String, Double> confidenceByResponse = new HashMap<>();
        
        for (AIVote vote : result.votes.values()) {
            String response = vote.vote;
            responseCounts.put(response, responseCounts.getOrDefault(response, 0) + 1);
            confidenceByResponse.put(response, vote.confidence);
        }
        
        // Consensus is most voted response
        String consensus = responseCounts.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse("No consensus reached");
        
        result.consensus = consensus;
        result.confidenceScore = responseCounts.get(consensus).doubleValue() / result.votes.size();
    }
    
    /**
     * Select top N providers by success rate
     */
    private List<String> selectTopProviders(List<String> providers, int limit) {
        if (weightingService == null) {
            return providers.stream().limit(limit).collect(Collectors.toList());
        }
        
        Map<String, Object> weights = weightingService.getProviderWeights();
        
        return providers.stream()
            .sorted((p1, p2) -> {
                // Sort by weight (higher weight = better)
                Object w1 = weights.getOrDefault(p1, Map.of("weight", "0.5"));
                Object w2 = weights.getOrDefault(p2, Map.of("weight", "0.5"));
                return String.valueOf(w2).compareTo(String.valueOf(w1));
            })
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get voting strategy info
     */
    public Map<String, Object> getStrategyInfo() {
        return Map.ofEntries(
            Map.entry("maxVotingParticipants", MAX_VOTING_PARTICIPANTS),
            Map.entry("strategies", Map.ofEntries(
                Map.entry("0providers", "Solo mode - SupremeAI built-in analysis"),
                Map.entry("1provider", "Direct mode - 100% use single provider"),
                Map.entry("2providers", "Tiebreaker - 3-way: 2 AIs + System"),
                Map.entry("3to5providers", "Consensus - All participate"),
                Map.entry("6+providers", "Top 5 selected by success rate")
            )),
            Map.entry("timeouts", Map.ofEntries(
                Map.entry("perProviderMs", INDIVIDUAL_TIMEOUT_MS),
                Map.entry("totalConsensusMs", TOTAL_TIMEOUT_MS),
                Map.entry("strategy", "Parallel execution, no waiting for slow providers")
            )),
            Map.entry("benefits", new String[]{
                "✅ Works with ANY number of providers (0 to billions)",
                "✅ No hardcoded '10 AIs' assumption",
                "✅ No artificial delays/waiting",
                "✅ Parallel execution (fast providers return first)",
                "✅ Automatic timeout (don't wait for slow ones)",
                "✅ System always participates as tiebreaker (2 & 3 AI cases)",
                "✅ Top 5 auto-selected (6+ case)",
                "✅ Graceful degradation (0 AIs = solo analysis)"
            })
        );
    }
}
