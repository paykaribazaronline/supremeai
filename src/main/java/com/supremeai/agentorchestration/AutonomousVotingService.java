package com.supremeai.agentorchestration;

import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AutonomousVotingService {

    private static final Logger logger = LoggerFactory.getLogger(AutonomousVotingService.class);
    @Value("${supremeai.active.providers:groq,openai,anthropic,ollama}")
    private String activeProviders;
    
    private final java.util.concurrent.ExecutorService executor;
    
    @Autowired
    private AIProviderFactory providerFactory;

    public AutonomousVotingService(@Qualifier("votingTaskExecutor") ThreadPoolTaskExecutor votingTaskExecutor) {
        this.executor = votingTaskExecutor.getThreadPoolExecutor();
    }

    public VotingDecision conductVote(String question, String context) {
        logger.info("Starting autonomous voting for question: {}", question);
        
        List<String> providerList = Arrays.asList(activeProviders.split(","));
        List<CompletableFuture<ProviderVote>> futures = new ArrayList<>();
        
        for (String providerName : providerList) {
            CompletableFuture<ProviderVote> future = CompletableFuture.supplyAsync(() -> {
                try {
                    AIProvider provider = providerFactory.getProvider(providerName);
                    long start = System.currentTimeMillis();
                    String response = provider.generate(buildPrompt(question, context));
                    long latency = System.currentTimeMillis() - start;
                    
                    ProviderVote vote = new ProviderVote();
                    vote.setProviderName(providerName);
                    vote.setResponse(response);
                    vote.setConfidence(0.8); // Will be calculated based on response quality
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
    
    private VotingDecision calculateConsensus(String question, List<ProviderVote> votes) {
        VotingDecision decision = new VotingDecision();
        decision.setDecisionKey(question);
        decision.setProviderVotes(votes);
        
        long successCount = votes.stream().filter(ProviderVote::isSuccess).count();
        long totalCount = votes.size();
        
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
