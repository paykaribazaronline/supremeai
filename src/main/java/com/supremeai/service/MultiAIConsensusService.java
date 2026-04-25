package com.supremeai.service;

import com.supremeai.model.ConsensusVote;
import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MultiAIConsensusService {

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private SelfHealingService selfHealingService;

    @Autowired
    private KnowledgeFeedbackService feedbackService;

    @Autowired
    private ContextualAIRankingService contextualRankingService;

    // In-memory history for taste phase (no Firebase)
    private final List<ConsensusVote> history = new CopyOnWriteArrayList<>();

    // Use Virtual Thread Executor if available, otherwise fallback
    private final ExecutorService executor = com.supremeai.config.VirtualThreadConfig.getVirtualThreadExecutor();
    
    private final java.util.Random random = new java.util.Random();

    private static final org.slf4j.Logger logger =
        org.slf4j.LoggerFactory.getLogger(MultiAIConsensusService.class);

    private static final int MAX_RETRIES = 2;
    private static final long RETRY_BACKOFF_MS = 250L;

    public MultiAIConsensusService() {
        // Virtual Thread executor is initialized directly above
    }

    /**
     * Query multiple AI providers and return consensus result
     */
    @Cacheable(value = "ai_responses", key = "#question + '_' + T(java.util.Collections).unmodifiableSortedSet(new java.util.TreeSet(#providerNames)).hashCode()")
    public ConsensusResult askAllAIs(String question, List<String> providerNames, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        List<ProviderVote> votes = new CopyOnWriteArrayList<>();
        int totalExpected = providerNames.size();

        // Query all providers in parallel with retry via SelfHealingService
        List<Future<?>> futures = providerNames.stream()
            .map(providerName -> executor.submit(() -> {
                try {
                    AIProvider provider = providerFactory.getProvider(providerName);
                    long pStart = System.currentTimeMillis();
                    String response = selfHealingService.executeWithRetry(
                        () -> provider.generate(question),
                        MAX_RETRIES,
                        RETRY_BACKOFF_MS
                    );
                    long pDuration = System.currentTimeMillis() - pStart;
                    
                    double confidence = 0.85 + random.nextDouble() * 0.14;
                    votes.add(new ProviderVote(
                        providerName,
                        response,
                        confidence,
                        System.currentTimeMillis()
                    ));

                    // Record outcome
                    recordProviderOutcome(providerName, question, response, pDuration, confidence);
                } catch (Exception e) {
                    logger.warn("Provider {} failed after retries: {}", providerName, e.getMessage());
                }
            }))
            .collect(Collectors.toList());

        // Wait with early exit if strong consensus reached
        long checkInterval = 100L;
        long elapsed = 0;
        while (elapsed < timeoutMs) {
            // Check if we have enough votes for strong consensus
            if (votes.size() >= Math.max(3, totalExpected / 2 + 1)) {
                ConsensusResult current = calculateConsensus(question, votes, System.currentTimeMillis() - startTime);
                if (current.isStrongConsensus() && votes.size() >= totalExpected * 0.7) {
                    logger.info("Early exit reached with {}/{} votes (Strong Consensus)", votes.size(), totalExpected);
                    // Cancel remaining futures
                    futures.forEach(f -> f.cancel(true));
                    return current;
                }
            }
            
            // Check if all futures are done
            if (futures.stream().allMatch(Future::isDone)) break;
            
            try {
                Thread.sleep(checkInterval);
                elapsed += checkInterval;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        return calculateConsensus(question, votes, duration);
    }

    /**
     * Enhanced contextual AI query that automatically selects the best providers
     */
    public ConsensusResult askContextualAIs(String question, int count, long timeoutMs) {
        ContextualAIRankingService.ProviderSelection selection = contextualRankingService.selectBestProvider(question, null);
        List<String> providerNames = new ArrayList<>();
        
        // Always include the best one
        if (selection.providerName != null) {
            providerNames.add(selection.providerName);
        }
        
        // Fill up with other top performers for consensus
        List<ContextualAIRankingService.ProviderRanking> rankings = contextualRankingService.getRankingsForTask(selection.taskType);
        for (ContextualAIRankingService.ProviderRanking ranking : rankings) {
            if (providerNames.size() >= count) break;
            if (!providerNames.contains(ranking.provider)) {
                providerNames.add(ranking.provider);
            }
        }
        
        // Fallback to defaults if not enough
        if (providerNames.size() < count) {
            String[] allDefaults = providerFactory.getAllProviderNames();
            for (String p : allDefaults) {
                if (providerNames.size() >= count) break;
                if (!providerNames.contains(p)) {
                    providerNames.add(p);
                }
            }
        }
        
        logger.info("Contextual selection for '{}' (Task: {}): {}", question, selection.taskType, providerNames);
        return askAllAIs(question, providerNames, timeoutMs);
    }

    private void recordProviderOutcome(String providerName, String question, String response, long latency, double confidence) {
        try {
            ContextualAIRankingService.TaskType taskType = contextualRankingService.detectTaskType(question);
            
            // Simplified quality score based on length and confidence
            double quality = Math.min(5.0, (response.length() > 100 ? 3.0 : 1.0) + confidence * 2.0);
            
            contextualRankingService.recordTaskOutcome(providerName, taskType, true, latency, quality);
        } catch (Exception e) {
            // Silently fail as this is just telemetry
        }
    }

    /**
     * Calculate consensus from provider votes
     */
    private ConsensusResult calculateConsensus(String question, List<ProviderVote> votes, long totalTimeMs) {
        if (votes.isEmpty()) {
            return new ConsensusResult(
                question,
                "No AI providers responded",
                List.of(),
                0.0,
                "ERROR",
                0.0,
                totalTimeMs,
                0.0
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

        // Quality score: weighted combination of consensus strength and average confidence
        double qualityScore = (consensusPercentage / 100.0) * 0.7 + (avgConfidence) * 0.3;

        // Save to local history for learning and diagnostics.
        saveVoteToHistory(question, votes, consensusAnswer, consensusPercentage);

        return new ConsensusResult(
            question,
            consensusAnswer,
            votes,
            avgConfidence,
            "CONSENSUS_" + (consensusPercentage >= 70 ? "STRONG" : (consensusPercentage >= 40 ? "WEAK" : "SPLIT")),
            consensusPercentage,
            totalTimeMs,
            qualityScore
        );
    }

    /**
     * Save vote to in-memory history (taste phase - no Firebase).
     */
    private void saveVoteToHistory(String question, List<ProviderVote> votes, String consensus, double percentage) {
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
