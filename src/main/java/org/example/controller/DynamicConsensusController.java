package org.example.controller;

import org.example.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Dynamic Adaptive Consensus Voting Controller
 * Exposes endpoints for consensus voting with adaptive strategies
 * 
 * Optimization #8: Replace "wait for 10 AIs" with dynamic voting
 */
@RestController
@RequestMapping("/api/v1/consensus")
public class DynamicConsensusController {
    private static final Logger logger = LoggerFactory.getLogger(DynamicConsensusController.class);
    
    @Autowired(required = false)
    private DynamicAdaptiveConsensusService consensusService;
    
    @Autowired(required = false)
    private SmartProviderWeightingService weightingService;
    
    @Autowired(required = false)
    private BuiltInAnalysisService builtInAnalysis;
    
    /**
     * Get consensus vote on a query
     * Automatically selects strategy based on available providers
     * 
     * GET /api/v1/consensus/vote?query=How to optimize database?&providers=groq,openai
     */
    @GetMapping("/vote")
    public Map<String, Object> getConsensusVote(
        @RequestParam String query,
        @RequestParam(required = false) String providers) {
        
        if (consensusService == null) {
            return Map.of("error", "Consensus service not initialized");
        }
        
        List<String> providerList = new ArrayList<>();
        if (providers != null && !providers.isEmpty()) {
            providerList = Arrays.asList(providers.split(","));
        }
        
        logger.info("🗳️ Consensus request: {} providers for query", providerList.size());
        
        DynamicAdaptiveConsensusService.ConsensusResult result = 
            consensusService.getConsensus(query, providerList);
        
        return Map.ofEntries(
            Map.entry("query", query),
            Map.entry("strategy", result.votingStrategy),
            Map.entry("consensus", result.consensus),
            Map.entry("confidenceScore", String.format("%.2f%%", result.confidenceScore * 100)),
            Map.entry("voterCount", result.voterCount),
            Map.entry("processingTimeMs", result.processingTimeMs),
            Map.entry("votes", result.votes.entrySet().stream()
                .map(e -> Map.ofEntries(
                    Map.entry("voter", e.getValue().voterId),
                    Map.entry("vote", e.getValue().vote.substring(0, Math.min(50, e.getValue().vote.length()))),
                    Map.entry("confidence", String.format("%.2f%%", e.getValue().confidence * 100)),
                    Map.entry("responseTimeMs", e.getValue().responseTimeMs),
                    Map.entry("timeouted", e.getValue().timeouted)
                ))
                .toList())
        );
    }
    
    /**
     * Get voting strategy info
     */
    @GetMapping("/strategy-info")
    public Map<String, Object> getStrategyInfo() {
        if (consensusService == null) {
            return Map.of("error", "Consensus service not initialized");
        }
        return consensusService.getStrategyInfo();
    }
    
    /**
     * Test solo mode (0 providers)
     */
    @PostMapping("/test/solo")
    public Map<String, Object> testSoloMode(@RequestParam String query) {
        if (consensusService == null) {
            return Map.of("error", "Consensus service not initialized");
        }
        
        DynamicAdaptiveConsensusService.ConsensusResult result = 
            consensusService.getConsensus(query, new ArrayList<>());
        
        return Map.ofEntries(
            Map.entry("strategy", "SOLO (0 providers)"),
            Map.entry("systemAnalysis", result.consensus),
            Map.entry("confidence", String.format("%.2f%%", result.confidenceScore * 100)),
            Map.entry("processingTimeMs", result.processingTimeMs)
        );
    }
    
    /**
     * Test direct mode (1 provider)
     */
    @PostMapping("/test/direct")
    public Map<String, Object> testDirectMode(
        @RequestParam String query,
        @RequestParam String provider) {
        
        if (consensusService == null) {
            return Map.of("error", "Consensus service not initialized");
        }
        
        DynamicAdaptiveConsensusService.ConsensusResult result = 
            consensusService.getConsensus(query, List.of(provider));
        
        return Map.ofEntries(
            Map.entry("strategy", "DIRECT (1 provider)"),
            Map.entry("provider", provider),
            Map.entry("response", result.consensus),
            Map.entry("confidence", String.format("%.2f%%", result.confidenceScore * 100)),
            Map.entry("processingTimeMs", result.processingTimeMs)
        );
    }
    
    /**
     * Test tiebreaker mode (2 providers + system)
     */
    @PostMapping("/test/tiebreaker")
    public Map<String, Object> testTiebreakerMode(
        @RequestParam String query,
        @RequestParam String provider1,
        @RequestParam String provider2) {
        
        if (consensusService == null) {
            return Map.of("error", "Consensus service not initialized");
        }
        
        DynamicAdaptiveConsensusService.ConsensusResult result = 
            consensusService.getConsensus(query, List.of(provider1, provider2));
        
        return Map.ofEntries(
            Map.entry("strategy", "TIEBREAKER (2 providers + system)"),
            Map.entry("voters", List.of(provider1, provider2, "supremeai-system")),
            Map.entry("consensus", result.consensus),
            Map.entry("voterCount", result.voterCount),
            Map.entry("confidenceScore", String.format("%.2f%%", result.confidenceScore * 100)),
            Map.entry("processingTimeMs", result.processingTimeMs),
            Map.entry("voteSummary", result.votes.entrySet().stream()
                .map(e -> e.getKey() + ": " + String.format("%.0f%%", e.getValue().confidence * 100))
                .toList())
        );
    }
    
    /**
     * Test consensus mode (3+ providers)
     */
    @PostMapping("/test/consensus")
    public Map<String, Object> testConsensusMode(
        @RequestParam String query,
        @RequestParam List<String> providers) {
        
        if (consensusService == null) {
            return Map.of("error", "Consensus service not initialized");
        }
        
        DynamicAdaptiveConsensusService.ConsensusResult result = 
            consensusService.getConsensus(query, providers);
        
        String strategy = providers.size() >= 6 ? "TOP5 selected" : "CONSENSUS";
        
        return Map.ofEntries(
            Map.entry("strategy", strategy + " (" + providers.size() + " providers)"),
            Map.entry("availableProviders", providers.size()),
            Map.entry("actualVoters", result.voterCount),
            Map.entry("consensus", result.consensus),
            Map.entry("confidenceScore", String.format("%.2f%%", result.confidenceScore * 100)),
            Map.entry("processingTimeMs", result.processingTimeMs),
            Map.entry("voterBreakdown", result.votes.entrySet().stream()
                .map(e -> Map.ofEntries(
                    Map.entry("voter", e.getKey()),
                    Map.entry("confidence", String.format("%.0f%%", e.getValue().confidence * 100))
                ))
                .toList())
        );
    }
    
    /**
     * Get built-in analysis (system always votes with this)
     */
    @GetMapping("/system-analysis")
    public Map<String, Object> getSystemAnalysis(@RequestParam String query) {
        if (builtInAnalysis == null) {
            return Map.of("error", "Built-in analysis not initialized");
        }
        
        String analysis = builtInAnalysis.analyze(query);
        
        return Map.ofEntries(
            Map.entry("queryType", "system analysis"),
            Map.entry("analysis", analysis),
            Map.entry("confidence", "0.85 (built-in always participates)"),
            Map.entry("note", "This is what SupremeAI votes when external AIs are present")
        );
    }
    
    /**
     * Compare strategies (0 vs 1 vs 2 vs 3+ providers)
     */
    @PostMapping("/compare-strategies")
    public Map<String, Object> compareStrategies(@RequestParam String query) {
        if (consensusService == null) {
            return Map.of("error", "Consensus service not initialized");
        }
        
        long start = System.currentTimeMillis();
        
        Map<String, Object> results = new LinkedHashMap<>();
        
        // Test all strategies
        DynamicAdaptiveConsensusService.ConsensusResult solo = 
            consensusService.getConsensus(query, new ArrayList<>());
        results.put("soloMode_0providers", Map.ofEntries(
            Map.entry("strategy", "SOLO"),
            Map.entry("confidence", String.format("%.0f%%", solo.confidenceScore * 100)),
            Map.entry("timeMs", solo.processingTimeMs)
        ));
        
        DynamicAdaptiveConsensusService.ConsensusResult direct = 
            consensusService.getConsensus(query, List.of("test-ai"));
        results.put("directMode_1provider", Map.ofEntries(
            Map.entry("strategy", "DIRECT"),
            Map.entry("confidence", String.format("%.0f%%", direct.confidenceScore * 100)),
            Map.entry("timeMs", direct.processingTimeMs)
        ));
        
        DynamicAdaptiveConsensusService.ConsensusResult tiebreaker = 
            consensusService.getConsensus(query, List.of("ai-1", "ai-2"));
        results.put("tiebreakerMode_2providers", Map.ofEntries(
            Map.entry("strategy", "TIEBREAKER"),
            Map.entry("voters", 3),
            Map.entry("confidence", String.format("%.0f%%", tiebreaker.confidenceScore * 100)),
            Map.entry("timeMs", tiebreaker.processingTimeMs)
        ));
        
        DynamicAdaptiveConsensusService.ConsensusResult consensus = 
            consensusService.getConsensus(query, List.of("ai-1", "ai-2", "ai-3"));
        results.put("consensusMode_3providers", Map.ofEntries(
            Map.entry("strategy", "CONSENSUS"),
            Map.entry("voters", consensus.voterCount),
            Map.entry("confidence", String.format("%.0f%%", consensus.confidenceScore * 100)),
            Map.entry("timeMs", consensus.processingTimeMs)
        ));
        
        long totalTime = System.currentTimeMillis() - start;
        results.put("comparisonTotalTime", totalTime + "ms");
        
        return results;
    }
}
