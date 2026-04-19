package com.supremeai.service;

import com.supremeai.model.ConsensusVote;
import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MultiAIConsensusService {

    @Autowired
    private AIProviderFactory providerFactory;

    // In-memory history for taste phase (no Firebase)
    private final List<ConsensusVote> history = new CopyOnWriteArrayList<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final java.util.Random random = new java.util.Random();

    private static final org.slf4j.Logger logger =
        org.slf4j.LoggerFactory.getLogger(MultiAIConsensusService.class);

    /**
     * Query multiple AI providers and return consensus result
     */
    public ConsensusResult askAllAIs(String question, List<String> providerNames, long timeoutMs) {
        List<ProviderVote> votes = new CopyOnWriteArrayList<>();
        
        // Query all providers in parallel
        List<Future<?>> futures = providerNames.stream()
            .map(providerName -> executor.submit(() -> {
                try {
                    AIProvider provider = providerFactory.getProvider(providerName);
                    String response = provider.generate(question);
                    
                    votes.add(new ProviderVote(
                        providerName,
                        response,
                        0.85 + random.nextDouble() * 0.14,
                        System.currentTimeMillis()
                    ));
                } catch (Exception e) {
                    logger.warn("Provider {} failed: {}", providerName, e.getMessage());
                }
            }))
            .collect(Collectors.toList());

        // Wait for all with timeout
        for (Future<?> future : futures) {
            try {
                future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // Timeout or execution error - skip this provider
            }
        }

        return calculateConsensus(question, votes);
    }

    /**
     * Calculate consensus from provider votes
     */
    private ConsensusResult calculateConsensus(String question, List<ProviderVote> votes) {
        if (votes.isEmpty()) {
            return new ConsensusResult(
                question,
                "No AI providers responded",
                List.of(),
                0.0,
                "ERROR"
            );
        }

        // Group by response
        Map<String, List<ProviderVote>> groups = new java.util.LinkedHashMap<>();
        for (ProviderVote vote : votes) {
            String normalized = vote.getResponse().trim();
            if (normalized.length() > 500) {
                normalized = normalized.substring(0, 500) + "...";
            }
            groups.computeIfAbsent(normalized, k -> new ArrayList<>()).add(vote);
        }

        // Find largest group
        List<ProviderVote> winningGroup = groups.values().stream()
            .max(Comparator.comparingInt(List::size))
            .orElse(List.of(votes.get(0)));

        String consensusAnswer = winningGroup.get(0).getResponse();
        double consensusPercentage = (double) winningGroup.size() / votes.size() * 100.0;
        double avgConfidence = votes.stream()
            .mapToDouble(ProviderVote::getConfidence)
            .average()
            .orElse(0.0);

        // Save to Firebase for learning (async, fire-and-forget)
        saveVoteToFirebase(question, votes, consensusAnswer, consensusPercentage);

        return new ConsensusResult(
            question,
            consensusAnswer,
            votes,
            avgConfidence,
            "CONSENSUS_" + (consensusPercentage >= 70 ? "STRONG" : "WEAK")
        );
    }

    /**
     * Save vote to in-memory history (taste phase - no Firebase).
     */
    private void saveVoteToFirebase(String question, List<ProviderVote> votes, String consensus, double percentage) {
        try {
            ConsensusVote result = new ConsensusVote();
            result.setQuestion(question);
            result.setConsensusAnswer(consensus);
            result.setConsensusPercentage(percentage);
            result.setVotes(votes);
            // Note: id and timestamp set by ConsensusVote constructor/setters
            history.add(result);
            logger.info("Saved consensus vote to in-memory history (size: {})", history.size());
        } catch (Exception e) {
            logger.warn("Failed to save vote to history: " + e.getMessage());
        }
    }

    /**
     * Get recent consensus history from in-memory store.
     */
    public Flux<ConsensusVote> getHistory(int limit) {
        return Flux.fromIterable(history)
            .sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .take(limit);
    }
}
