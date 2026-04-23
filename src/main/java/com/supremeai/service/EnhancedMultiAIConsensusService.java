package com.supremeai.service;

import com.supremeai.model.ConsensusVote;
import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.selfhealing.SelfHealingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Enhanced Multi-AI Consensus Service with iterative discussion and weighted voting.
 * Supports 10+ AI providers with intelligent discussion rounds.
 */
@Service
public class EnhancedMultiAIConsensusService {

    private static final Logger log = LoggerFactory.getLogger(EnhancedMultiAIConsensusService.class);

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private SelfHealingService selfHealingService;

    @Autowired
    private AIRankingService aiRankingService;

    private final ExecutorService executor;
    private final List<ConsensusVote> history = new CopyOnWriteArrayList<>();

    // Configuration
    private static final int MAX_DISCUSSION_ROUNDS = 3;
    private static final long TIMEOUT_PER_ROUND_MS = 30000; // 30 seconds per round
    private static final int MIN_PROVIDERS_FOR_CONSENSUS = 3;
    private static final double CONSENSUS_THRESHOLD_STRONG = 0.75;
    private static final double CONSENSUS_THRESHOLD_WEAK = 0.50;

    public EnhancedMultiAIConsensusService(@Qualifier("consensusTaskExecutor") ExecutorService consensusTaskExecutor) {
        this.executor = consensusTaskExecutor;
    }

    /**
     * Run iterative discussion among multiple AIs and reach consensus.
     */
    public EnhancedConsensusResult discussAndVote(String question, List<String> providerNames, int maxRounds) {
        maxRounds = Math.min(maxRounds, MAX_DISCUSSION_ROUNDS);
        List<ProviderVote> currentVotes = new ArrayList<>();

        // Round 1: Initial responses
        log.info("Starting consensus discussion - Round 1 with {} providers", providerNames.size());
        currentVotes = queryProviders(question, providerNames, null, 1);

        if (currentVotes.size() < MIN_PROVIDERS_FOR_CONSENSUS) {
            return createFailureResult(question, currentVotes, "Not enough providers responded");
        }

        // Check initial consensus
        ConsensusResult initialConsensus = calculateWeightedConsensus(question, currentVotes);

        // If strong consensus, return early
        if (initialConsensus.consensusStrength.equals("CONSENSUS_STRONG")) {
            return new EnhancedConsensusResult(
                question,
                initialConsensus.consensusAnswer,
                initialConsensus.votes,
                initialConsensus.confidence,
                initialConsensus.consensusStrength,
                1, // rounds completed
                Map.of("initial_consensus", true)
            );
        }

        // Iterative discussion rounds
        Map<String, Object> metadata = new HashMap<>();
        int completedRounds = 1;

        for (int round = 2; round <= maxRounds; round++) {
            log.info("Discussion Round {}", round);

            // Prepare context from previous round
            String discussionContext = buildDiscussionContext(question, currentVotes, round);

            // Query providers again with discussion context
            List<ProviderVote> newVotes = queryProvidersWithContext(
                question, providerNames, discussionContext, round
            );

            // Merge votes (keep best response from each provider)
            currentVotes = mergeVotes(currentVotes, newVotes);

            // Re-check consensus
            ConsensusResult consensus = calculateWeightedConsensus(question, currentVotes);

            completedRounds = round;

            if (consensus.consensusStrength.equals("CONSENSUS_STRONG")) {
                metadata.put("achieved_strong_consensus", true);
                metadata.put("rounds_to_consensus", round);
                break;
            }

            // Check if answers are converging
            if (isConverging(currentVotes, newVotes)) {
                metadata.put("converging", true);
            }
        }

        // Final consensus calculation
        ConsensusResult finalConsensus = calculateWeightedConsensus(question, currentVotes);

        // Save to history
        saveVoteToHistory(question, currentVotes, finalConsensus.consensusAnswer,
            finalConsensus.confidence);

        return new EnhancedConsensusResult(
            question,
            finalConsensus.consensusAnswer,
            finalConsensus.votes,
            finalConsensus.confidence,
            finalConsensus.consensusStrength,
            completedRounds,
            metadata
        );
    }

    /**
     * Query all providers in parallel.
     */
    private List<ProviderVote> queryProviders(String question, List<String> providerNames,
                                            String context, int round) {
        List<Future<ProviderVote>> futures = providerNames.stream()
            .map(providerName -> executor.submit(() -> {
                try {
                    AIProvider provider = providerFactory.getProvider(providerName);

                    String prompt;
                    if (context != null && round > 1) {
                        prompt = String.format(
                            "Original question: %s\n\nDiscussion context (Round %d):\n%s\n\nProvide your refined answer:",
                            question, round - 1, context
                        );
                    } else {
                        prompt = question;
                    }

                    String response = selfHealingService.executeWithRetry(
                        () -> provider.generate(prompt),
                        2, 250L
                    );

                    // Get weighted confidence based on provider's past performance
                    double confidence = calculateProviderWeight(providerName);
                    long latency = System.currentTimeMillis(); // Simplified

                    return new ProviderVote(
                        providerName,
                        response,
                        confidence,
                        System.currentTimeMillis()
                    );
                } catch (Exception e) {
                    log.warn("Provider {} failed in round {}: {}", providerName, round, e.getMessage());
                    return null;
                }
            }))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<ProviderVote> votes = new ArrayList<>();
        for (Future<ProviderVote> future : futures) {
            try {
                ProviderVote vote = future.get(TIMEOUT_PER_ROUND_MS, TimeUnit.MILLISECONDS);
                if (vote != null) {
                    votes.add(vote);
                }
            } catch (Exception e) {
                log.debug("Provider future timeout or error", e);
            }
        }

        return votes;
    }

    /**
     * Query providers with discussion context.
     */
    private List<ProviderVote> queryProvidersWithContext(String question, List<String> providerNames,
                                                        String context, int round) {
        return queryProviders(question, providerNames, context, round);
    }

    /**
     * Calculate weighted consensus using provider rankings.
     */
    private ConsensusResult calculateWeightedConsensus(String question, List<ProviderVote> votes) {
        if (votes.isEmpty()) {
            return new ConsensusResult(question, "No responses", List.of(), 0.0, "ERROR");
        }

        // Group responses and calculate weighted votes
        Map<String, WeightedVoteGroup> groups = new LinkedHashMap<>();

        for (ProviderVote vote : votes) {
            String normalized = normalizeResponse(vote.getResponse());

            groups.computeIfAbsent(normalized, k -> new WeightedVoteGroup())
                .addVote(vote, getProviderWeight(vote.getProviderName()));
        }

        // Find group with highest weighted score
        WeightedVoteGroup winningGroup = groups.values().stream()
            .max(Comparator.comparingDouble(g -> g.totalWeight))
            .orElse(new WeightedVoteGroup());

        // Calculate consensus percentage based on weighted votes
        double totalWeight = groups.values().stream().mapToDouble(g -> g.totalWeight).sum();
        double consensusPercentage = totalWeight > 0 ?
            (winningGroup.totalWeight / totalWeight) * 100.0 : 0.0;

        // Check if we have the actual response from the winning group
        String consensusAnswer = winningGroup.getRepresentativeResponse();

        // Determine strength
        String strength;
        if (consensusPercentage >= CONSENSUS_THRESHOLD_STRONG * 100) {
            strength = "CONSENSUS_STRONG";
        } else if (consensusPercentage >= CONSENSUS_THRESHOLD_WEAK * 100) {
            strength = "CONSENSUS_WEAK";
        } else {
            strength = "CONSENSUS_SPLIT";
        }

        // Average confidence
        double avgConfidence = votes.stream()
            .mapToDouble(ProviderVote::getConfidence)
            .average()
            .orElse(0.0);

        return new ConsensusResult(question, consensusAnswer, votes, avgConfidence, strength);
    }

    /**
     * Calculate provider weight based on ranking.
     */
    private double calculateProviderWeight(String providerName) {
        AIRankingService.ProviderRanking ranking = aiRankingService.getRankingForProvider(providerName);
        // Weight = success rate (0.0 to 1.0)
        return ranking.getSuccessRate() / 100.0;
    }

    /**
     * Get provider weight (cached).
     */
    private double getProviderWeight(String providerName) {
        return calculateProviderWeight(providerName);
    }

    /**
     * Build discussion context from previous round.
     */
    private String buildDiscussionContext(String question, List<ProviderVote> votes, int round) {
        StringBuilder context = new StringBuilder();

        // Add summary of previous round
        context.append("Previous round (").append(round - 1).append(") responses:\n");

        // Group similar responses
        Map<String, List<ProviderVote>> grouped = votes.stream()
            .collect(Collectors.groupingBy(v -> normalizeResponse(v.getResponse())));

        int groupNum = 1;
        for (Map.Entry<String, List<ProviderVote>> entry : grouped.entrySet()) {
            context.append("\nGroup ").append(groupNum++).append(" (")
                   .append(entry.getValue().size()).append(" providers):\n");
            context.append(entry.getValue().get(0).getResponse().substring(0,
                Math.min(200, entry.getValue().get(0).getResponse().length())))
                   .append("...\n");

            // Add provider names
            String providerNames = entry.getValue().stream()
                .map(ProviderVote::getProviderName)
                .collect(Collectors.joining(", "));
            context.append("Providers: ").append(providerNames).append("\n");
        }

        return context.toString();
    }

    /**
     * Normalize response for comparison.
     */
    private String normalizeResponse(String response) {
        if (response == null) return "";
        String normalized = response.trim().toLowerCase(Locale.ROOT);
        // Remove extra whitespace
        normalized = normalized.replaceAll("\\s+", " ");
        // Truncate for comparison
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    /**
     * Merge votes from multiple rounds (keep best from each provider).
     */
    private List<ProviderVote> mergeVotes(List<ProviderVote> existing, List<ProviderVote> newVotes) {
        Map<String, ProviderVote> bestVotes = new LinkedHashMap<>();

        // Add existing votes
        for (ProviderVote vote : existing) {
            bestVotes.put(vote.getProviderName(), vote);
        }

        // Update with new votes if better
        for (ProviderVote newVote : newVotes) {
            ProviderVote existingVote = bestVotes.get(newVote.getProviderName());
            if (existingVote == null ||
                newVote.getConfidence() > existingVote.getConfidence()) {
                bestVotes.put(newVote.getProviderName(), newVote);
            }
        }

        return new ArrayList<>(bestVotes.values());
    }

    /**
     * Check if responses are converging.
     */
    private boolean isConverging(List<ProviderVote> oldVotes, List<ProviderVote> newVotes) {
        // Simple heuristic: check if more responses are similar
        long similarCount = newVotes.stream()
            .filter(nv -> oldVotes.stream()
                .anyMatch(ov ->
                    normalizeResponse(ov.getResponse())
                        .equals(normalizeResponse(nv.getResponse()))))
            .count();

        return similarCount >= newVotes.size() * 0.6;
    }

    /**
     * Save vote to history.
     */
    private void saveVoteToHistory(String question, List<ProviderVote> votes,
                                   String consensus, double confidence) {
        try {
            ConsensusVote result = new ConsensusVote();
            result.setQuestion(question);
            result.setConsensusAnswer(consensus);
            result.setConsensusPercentage(confidence * 100);
            result.setVotes(votes);
            history.add(result);
            log.info("Saved enhanced consensus vote to history (size: {})", history.size());
        } catch (Exception e) {
            log.warn("Failed to save vote to history: {}", e.getMessage());
        }
    }

    /**
     * Get discussion history.
     */
    public List<ConsensusVote> getHistory(int limit) {
        return history.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    // ── Inner Classes ──────────────────────────────────────────────────────

    public static class EnhancedConsensusResult {
        public final String question;
        public final String consensusAnswer;
        public final List<ProviderVote> votes;
        public final double confidence;
        public final String consensusStrength;
        public final int roundsCompleted;
        public final Map<String, Object> metadata;

        public EnhancedConsensusResult(String question, String consensusAnswer,
                                       List<ProviderVote> votes, double confidence,
                                       String consensusStrength, int roundsCompleted,
                                       Map<String, Object> metadata) {
            this.question = question;
            this.consensusAnswer = consensusAnswer;
            this.votes = votes;
            this.confidence = confidence;
            this.consensusStrength = consensusStrength;
            this.roundsCompleted = roundsCompleted;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
    }

    private static class WeightedVoteGroup {
        double totalWeight = 0.0;
        List<ProviderVote> votes = new ArrayList<>();
        String representativeResponse = "";

        void addVote(ProviderVote vote, double weight) {
            totalWeight += weight;
            votes.add(vote);
            if (representativeResponse.isEmpty()) {
                representativeResponse = vote.getResponse();
            }
        }

        String getRepresentativeResponse() {
            return representativeResponse;
        }
    }
}
