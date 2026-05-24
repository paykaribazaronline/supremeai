package com.supremeai.agentorchestration;

import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AutonomousVotingService {

    private static final Logger logger = LoggerFactory.getLogger(AutonomousVotingService.class);
    private final java.util.concurrent.Executor executor;

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private com.supremeai.repository.ProviderRepository providerRepository;

    @Autowired
    public AutonomousVotingService(@Qualifier("votingTaskExecutor") java.util.concurrent.Executor votingTaskExecutor) {
        this.executor = votingTaskExecutor;
    }

    /**
     * Resolve active providers at call time from the database — no hardcoded defaults.
     * Falls back to Firestore/DB query if no config property is set.
     */
    private List<String> resolveActiveProviders() {
        try {
            List<com.supremeai.model.APIProvider> active = providerRepository.findByStatus("active")
                    .collectList()
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .block(java.time.Duration.ofSeconds(3));
            if (active != null && !active.isEmpty()) {
                return active.stream()
                        .map(p -> p.getName() != null && !p.getName().isBlank() ? p.getName() : p.getId())
                        .filter(java.util.Objects::nonNull)
                        .toList();
            }
        } catch (Exception e) {
            logger.warn("[DynamicProviders] DB query for active providers failed: {}", e.getMessage());
        }
        logger.warn("[DynamicProviders] No active providers found in DB — solo mode active, returning empty list");
        return List.of();
    }

    public VotingDecision conductVote(String question, String context) {
        logger.info("Starting autonomous voting for question: {}", question);

        List<String> providerList = resolveActiveProviders();
        List<CompletableFuture<ProviderVote>> futures = new ArrayList<>();
        
        for (String providerName : providerList) {
            CompletableFuture<ProviderVote> future = CompletableFuture.supplyAsync(() -> {
                try {
                    AIProvider provider = providerFactory.getProvider(providerName);
                    long start = System.currentTimeMillis();
                    String response = provider.generate(buildPrompt(question, context)).block();
                    long latency = System.currentTimeMillis() - start;
                    
                    ProviderVote vote = new ProviderVote();
                    vote.setProviderName(providerName);
                    vote.setResponse(response);
                    vote.setConfidence(calculateConfidence(response, latency));
                    vote.setLatencyMs(latency);
                    vote.setSuccess(true);
                    
                    logger.debug("Provider {} voted successfully in {}ms", providerName, latency);
                    return vote;
                    
                } catch (Exception e) {
                    logger.warn("Provider {} failed to vote: {}", providerName, e.getMessage());
                    ProviderVote vote = new ProviderVote();
                    vote.setProviderName(providerName);
                    vote.setSuccess(false);
                    vote.setErrorMessage(e.getMessage());
                    return vote;
                }
            }, executor);
            
            futures.add(future);
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        List<ProviderVote> votes = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        
        return calculateConsensus(question, votes);
    }
    
    private String buildPrompt(String question, String context) {
        return String.format("""
            Context: %s
            
            Question: %s
            
            Provide your expert opinion on this question. Be concise and specific.
            """, context, question);
    }

    private double calculateConfidence(String response, long latencyMs) {
        double confidence = 0.3; // Base confidence

        // Latency score: lower latency = higher score
        if (latencyMs < 100) {
            confidence += 0.3;
        } else if (latencyMs < 500) {
            confidence += 0.2;
        } else {
            confidence += 0.1;
        }

        // Response length score: reasonable length = higher score
        int length = response != null ? response.length() : 0;
        if (length >= 50 && length <= 500) {
            confidence += 0.4;
        } else if (length > 0) {
            confidence += 0.2;
        }

        // Cap confidence between 0 and 1
        return Math.max(0.0, Math.min(1.0, confidence));
    }
    
    private VotingDecision calculateConsensus(String question, List<ProviderVote> votes) {
        VotingDecision decision = new VotingDecision();
        decision.setDecisionKey(question);
        decision.setProviderVotes(votes);
        
        long successCount = votes.stream().filter(ProviderVote::isSuccess).count();
        
        if (successCount == 0) {
            decision.setStrength("ERROR");
            decision.setConfidence(0.0);
            decision.setAiConsensus("All providers failed to respond");
            return decision;
        }
        
        Map<String, Long> responseFrequency = votes.stream()
                .filter(ProviderVote::isSuccess)
                .collect(Collectors.groupingBy(ProviderVote::getResponse, Collectors.counting()));
        
        Map.Entry<String, Long> mostCommon = responseFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();
        
        double consensusPercentage = (double) mostCommon.getValue() / successCount;
        
        decision.setAiConsensus(mostCommon.getKey());
        decision.setConfidence(consensusPercentage);
        
        if (consensusPercentage >= 0.75) {
            decision.setStrength("STRONG");
        } else if (consensusPercentage >= 0.5) {
            decision.setStrength("WEAK");
        } else {
            decision.setStrength("SPLIT");
        }
        
        logger.info("Voting complete. Consensus: {} with {} confidence", 
                decision.getStrength(), decision.getConfidence());
        
        return decision;
    }
}
