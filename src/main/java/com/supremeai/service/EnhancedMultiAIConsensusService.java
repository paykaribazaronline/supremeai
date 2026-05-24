package com.supremeai.service;

import com.supremeai.learning.service.EnhancedContentSanitizerService;
import com.supremeai.model.ConsensusVote;
import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
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

    @Autowired
    private AIReasoningService aiReasoningService;

    @Autowired
    private EnhancedContentSanitizerService contentSanitizer;

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
     * Refactored to be fully reactive to avoid blocking the Netty event loop.
     */
    public Mono<EnhancedConsensusResult> discussAndVote(String question, List<String> providerNames, int maxRounds) {
        int finalMaxRounds = Math.min(maxRounds, MAX_DISCUSSION_ROUNDS);
        
        log.info("Starting consensus discussion - Round 1 with {} providers", providerNames.size());
        
        return queryProvidersReactive(question, providerNames, null, 1)
            .flatMap(initialVotes -> {
                if (initialVotes.size() < MIN_PROVIDERS_FOR_CONSENSUS) {
                    return Mono.just(new EnhancedConsensusResult(
                        question,
                        "Error: Not enough providers responded for consensus (need at least " + MIN_PROVIDERS_FOR_CONSENSUS + ")",
                        initialVotes,
                        0.0,
                        "ERROR",
                        0,
                        Map.of("error", "Not enough providers responded")
                    ));
                }

                ConsensusResult initialConsensus = calculateWeightedConsensus(question, initialVotes);

                if ("CONSENSUS_STRONG".equals(initialConsensus.getStrength())) {
                    return Mono.just(new EnhancedConsensusResult(
                            question,
                            initialConsensus.getConsensusAnswer(),
                            initialConsensus.getVotes(),
                            initialConsensus.getAverageConfidence(),
                            initialConsensus.getStrength(),
                            1,
                            Map.of("initial_consensus", true)
                    ));
                }

                return runDiscussionRounds(question, providerNames, initialVotes, 2, finalMaxRounds);
            });
    }

    private Mono<EnhancedConsensusResult> runDiscussionRounds(String question, List<String> providerNames, 
                                                             List<ProviderVote> currentVotes, int currentRound, int maxRounds) {
        if (currentRound > maxRounds) {
            ConsensusResult finalConsensus = calculateWeightedConsensus(question, currentVotes);
            
            // If still split after rounds, trigger Multi-Agent Debate (MAD)
            if ("CONSENSUS_SPLIT".equals(finalConsensus.getStrength())) {
                return triggerDebate(question, currentVotes, providerNames)
                    .map(debatedResult -> finalizeConsensusResult(question, debatedResult.votes, currentRound - 1, debatedResult.metadata));
            }
            
            return Mono.just(finalizeConsensusResult(question, currentVotes, currentRound - 1, new HashMap<>()));
        }

        log.info("Discussion Round {}", currentRound);
        String discussionContext = buildDiscussionContext(question, currentVotes, currentRound);

        // Optimize cost: only re-query providers that disagree with the current leading consensus
        ConsensusResult currentConsensus = calculateWeightedConsensus(question, currentVotes);
        String currentBestAnswer = currentConsensus.getConsensusAnswer();
        
        List<String> disagreeingProviders = currentVotes.stream()
            .filter(v -> !normalizeResponse(v.getResponse()).equals(normalizeResponse(currentBestAnswer)))
            .map(ProviderVote::getProviderName)
            .collect(Collectors.toList());
            
        // If everyone agrees or no one disagreed, we shouldn't be here (handled by strength check)
        List<String> providersToQuery = disagreeingProviders.isEmpty() ? providerNames : disagreeingProviders;
        log.info("Cost optimization: querying only {} disagreeing providers instead of all {}", providersToQuery.size(), providerNames.size());

        return queryProvidersReactive(question, providersToQuery, discussionContext, currentRound)
            .flatMap(newVotes -> {
                List<ProviderVote> mergedVotes = mergeVotes(currentVotes, newVotes);
                ConsensusResult consensus = calculateWeightedConsensus(question, mergedVotes);

                if ("CONSENSUS_STRONG".equals(consensus.getStrength())) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("achieved_strong_consensus", true);
                    metadata.put("rounds_to_consensus", currentRound);
                    return Mono.just(new EnhancedConsensusResult(
                        question,
                        consensus.getConsensusAnswer(),
                        consensus.getVotes(),
                        consensus.getConfidence(),
                        consensus.getStrength(),
                        currentRound,
                        metadata
                    ));
                }

                return runDiscussionRounds(question, providerNames, mergedVotes, currentRound + 1, maxRounds);
            });
    }

    private EnhancedConsensusResult finalizeConsensusResult(String question, List<ProviderVote> currentVotes, 
                                                            int completedRounds, Map<String, Object> metadata) {
        ConsensusResult finalConsensus = calculateWeightedConsensus(question, currentVotes);

        aiReasoningService.logReasoning(
            "CONSENSUS_" + System.currentTimeMillis(),
            "Final Consensus Reached",
            String.format("Strength: %s, Confidence: %.2f%%, Rounds: %d.", 
                finalConsensus.getStrength(), finalConsensus.getConfidence(), completedRounds),
            "EnhancedMultiAIConsensusService"
        );

        saveVoteToHistory(question, currentVotes, finalConsensus.getConsensusAnswer(),
            finalConsensus.getConfidence());

        return new EnhancedConsensusResult(
            question,
            finalConsensus.getConsensusAnswer(),
            finalConsensus.getVotes(),
            finalConsensus.getConfidence(),
            finalConsensus.getStrength(),
            completedRounds,
            metadata
        );
    }

    /**
     * Query all providers in parallel reactively.
     */
    private Mono<List<ProviderVote>> queryProvidersReactive(String question, List<String> providerNames,
                                                           String context, int round) {
        return Flux.fromIterable(providerNames)
            .filter(providerName -> !selfHealingService.isProviderQuarantined(providerName))
            .flatMap(providerName -> {
                return Mono.fromCallable(() -> providerFactory.getProvider(providerName))
                    .flatMap(provider -> {
                        String prompt;
                        if (context != null && round > 1) {
                            // Fix: Sanitize context to prevent prompt injection
                            String sanitizedContext = contentSanitizer.maskPII(context);
                            prompt = String.format(
                                "Original question: %s\n\nDiscussion context (Round %d):\n%s\n\nProvide your refined answer based on above context:",
                                question, round - 1, sanitizedContext
                            );
                        } else {
                            prompt = question;
                        }

                        long startTime = System.currentTimeMillis();
                        return selfHealingService.executeWithRetry(() -> provider.generate(prompt), 2, 250L)
                            .timeout(Duration.ofMillis(TIMEOUT_PER_ROUND_MS))
                            .map(response -> new ProviderVote(
                                providerName,
                                response,
                                calculateProviderWeight(providerName),
                                System.currentTimeMillis()
                            ))
                            .onErrorResume(e -> {
                                log.warn("Provider {} failed in round {}: {}", providerName, round, e.getMessage());
                                return Mono.empty();
                            });
                    })
                    .subscribeOn(Schedulers.boundedElastic());
            })
            .collectList();
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

        // Group similar responses — null-safe: null/blank responses are grouped under ""
        Map<String, List<ProviderVote>> grouped = votes.stream()
            .collect(Collectors.groupingBy(v -> {
                String r = v.getResponse();
                return (r == null || r.isBlank()) ? "" : normalizeResponse(r);
            }));

        int groupNum = 1;
        for (Map.Entry<String, List<ProviderVote>> entry : grouped.entrySet()) {
            context.append("\nGroup ").append(groupNum++).append(" (")
                   .append(entry.getValue().size()).append(" providers):\n");

            // Null-safe: use first non-null response, or placeholder if all are null
            String representative = entry.getValue().stream()
                    .map(ProviderVote::getResponse)
                    .filter(r -> r != null && !r.isBlank())
                    .findFirst()
                    .orElse("[no content]");
            int truncateLen = Math.min(200, representative.length());
            context.append(representative, 0, truncateLen)
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
     * Trigger Multi-Agent Debate (MAD) to resolve a split consensus.
     * Selects a "Judge" AI to evaluate conflicting responses.
     */
    private Mono<EnhancedConsensusResult> triggerDebate(String question, List<ProviderVote> votes, List<String> allProviders) {
        log.info("🚀 Triggering Multi-Agent Debate (MAD) to resolve split consensus...");

        // 1. Identify top 2 conflicting groups
        Map<String, WeightedVoteGroup> groups = new LinkedHashMap<>();
        for (ProviderVote vote : votes) {
            groups.computeIfAbsent(normalizeResponse(vote.getResponse()), k -> new WeightedVoteGroup())
                .addVote(vote, getProviderWeight(vote.getProviderName()));
        }

        List<WeightedVoteGroup> sortedGroups = groups.values().stream()
            .sorted((g1, g2) -> Double.compare(g2.totalWeight, g1.totalWeight))
            .limit(2)
            .collect(Collectors.toList());

        if (sortedGroups.size() < 2) {
            return Mono.just(new EnhancedConsensusResult(question, votes.get(0).getResponse(), votes, 0.0, "SPLIT", 0, Map.of()));
        }

        // 2. Select a Judge using ranking-based selection — pick the highest-ranked
        // provider from allProviders that is NOT already in the top 2 conflicting groups.
        // Falls back to the first available provider if ranking data is unavailable.
        Set<String> conflictingProviders = sortedGroups.stream()
                .flatMap(g -> g.votes.stream().map(ProviderVote::getProviderName))
                .collect(java.util.stream.Collectors.toSet());

        String judgeName = allProviders.stream()
                .filter(p -> !conflictingProviders.contains(p))
                .max(Comparator.comparingDouble(p -> {
                    AIRankingService.ProviderRanking ranking = aiRankingService.getRankingForProvider(p);
                    return ranking.getSuccessRate();
                }))
                .orElse(allProviders.get(0)); // Fallback: first provider if no non-conflicting option
        
        String optionA = sortedGroups.get(0).getRepresentativeResponse();
        String optionB = sortedGroups.get(1).getRepresentativeResponse();

        String debatePrompt = String.format(
            "Original Question: %s\n\n" +
            "Two different solutions have been proposed by other AI agents:\n\n" +
            "OPTION A:\n%s\n\n" +
            "OPTION B:\n%s\n\n" +
            "As the Supreme Judge, evaluate both options for accuracy, performance, and security. " +
            "Choose the best one or provide an even better integrated solution. " +
            "Start your response with 'JUDGMENT:' followed by your reasoning and final code.",
            question, optionA, optionB
        );

        return Mono.fromCallable(() -> providerFactory.getProvider(judgeName))
            .flatMap(judge -> judge.generate(debatePrompt))
            .map(judgment -> {
                log.info("⚖️ Judge {} has delivered a verdict.", judgeName);
                
                // Add the judge's verdict as a high-confidence vote
                ProviderVote judgeVote = new ProviderVote(
                    judgeName + " (JUDGE)",
                    judgment,
                    1.0, // High confidence for the verdict
                    System.currentTimeMillis()
                );
                
                List<ProviderVote> newVotes = new ArrayList<>(votes);
                newVotes.add(judgeVote);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("mad_resolved", true);
                metadata.put("judge", judgeName);
                
                return new EnhancedConsensusResult(question, judgment, newVotes, 0.9, "DEBATED", 0, metadata);
            })
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Get recent discussion history.
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
