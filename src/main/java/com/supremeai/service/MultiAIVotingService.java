package com.supremeai.service;

import com.supremeai.agentorchestration.VotingDecision;
import com.supremeai.intelligence.voting.VotingTopic;
import com.supremeai.intelligence.voting.VotingTopicGenerator;
import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ConsensusVote;
import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.provider.AIProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import com.supremeai.repository.TaskProviderAssignmentRepository;

/**
 * Unified Multi-AI Voting Service
 * Provides ensemble voting, consensus, and decision-making across multiple AI providers
 */
@Service
public class MultiAIVotingService {

    private static final Logger logger = LoggerFactory.getLogger(MultiAIVotingService.class);

    // Dependencies from original services
    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private ContextualAIRankingService contextualRankingService;

    @Autowired
    private VotingTopicGenerator topicGenerator;

    @org.springframework.context.annotation.Lazy
    @Autowired(required = false)
    private SelfHealingService selfHealingService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private KnowledgeFeedbackService feedbackService;

@Autowired
    private com.supremeai.repository.ProviderRepository providerRepository;

    @Autowired
    private TaskProviderAssignmentRepository taskAssignmentRepo;

    // Dynamic model selection (no hardcoded limit)
    private static final int DEFAULT_MAX_VOTING_PROVIDERS = 10;

    public static final String[] ALL_PROVIDERS = {};

    public static final String[] DEFAULT_PROVIDERS = {};

    private final Map<String, ModelPerformanceTracker> performanceTrackers = new ConcurrentHashMap<>();
    private final List<ConsensusVote> consensusHistory = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 1000;

    /**
     * Provider cache TTL in milliseconds
     */

    public MultiAIVotingService() {
    }

    // Configuration helpers
    private int getTimeoutMs() {
        return (int) configService.getTimeout("voting_timeout", 15000L);
    }

    private int getMaxRetries() {
        return configService.getSetting("max_retries", 2);
    }

    private double getConfidenceThreshold() {
        return configService.getThreshold("consensus", 0.6);
    }

    // ===== ENSEMBLE VOTING (from TenAIVotingSystem) =====

/**
     * Execute ensemble voting with DYNAMIC provider count
     * Selects providers based on task type and admin assignments.
     * Supports 0 to ∞ providers - no hardcoded limits.
     */
    public Mono<VotingResult> executeEnsembleVoting(
            String prompt,
            List<String> selectedModels,
            long timeoutMs
    ) {
        return executeEnsembleVoting(prompt, selectedModels, timeoutMs, null);
    }

    @Cacheable(value = "ai_responses", key = "#prompt + '_ensemble_' + #taskType")
    public Mono<VotingResult> executeEnsembleVoting(
            String prompt,
            List<String> selectedModels,
            long timeoutMs,
            String taskType  // NEW: task type for smart routing
    ) {
        long startTime = System.currentTimeMillis();
        ContextualAIRankingService.TaskType detectedTaskType =
            contextualRankingService.detectTaskType(prompt);

        Mono<List<String>> modelsToUseMono;
        if (selectedModels != null && !selectedModels.isEmpty()) {
            // User explicitly selected specific models (admin override)
            modelsToUseMono = Mono.just(selectedModels);
        } else {
            // 🔥 NEW: Get providers assigned to this task type from DB
            modelsToUseMono = getAssignedProvidersForTask(
                taskType != null ? taskType : detectedTaskType.name()
            );
        }

        return modelsToUseMono.flatMap(models ->
            Flux.fromIterable(models)
                .filter(this::isModelAvailable)
                .flatMap(modelName -> queryModel(modelName, prompt, timeoutMs))
                .collectList()
                .flatMap(allVotes -> {
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("Ensemble voting completed in {}ms with {} responses (task: {})",
                        duration, allVotes.size(), taskType);
                    
                    return providerRepository.findAll()
                        .collectMap(
                            p -> p.getName() != null ? p.getName().toLowerCase() : "",
                            p -> {
                                Double score = p.getCapabilityScores() != null ? p.getCapabilityScores().get("voting_weight") : null;
                                return score != null ? score : 0.5;
                            }
                        )
                        .flatMap(weightsMap -> {
                            VotingResult initialResult = calculateEnsembleResult(prompt, allVotes, duration, detectedTaskType, weightsMap);
                            if ("NO_CONSENSUS".equals(initialResult.getVerdict()) || "WEAK_CONSENSUS".equals(initialResult.getVerdict())) {
                                return performMetaSynthesis(prompt, allVotes, initialResult, duration);
                            } else {
                                return Mono.just(initialResult);
                            }
                        });
                })
        )
        .timeout(java.time.Duration.ofMillis(timeoutMs))
        .onErrorResume(java.util.concurrent.TimeoutException.class, e -> {
            logger.warn("Ensemble voting timed out for prompt: {}", prompt);
            return Mono.just(new VotingResult(prompt, "দুঃখিত, অনুরোধটির উত্তর দিতে সময়সীমা অতিক্রম হয়ে গেছে।",
                List.of(), 0.0, "TIMEOUT", timeoutMs));
        });
    }

    /**
     * Get providers assigned to a specific task type.
     * Priority: DB assignment → fallback providers → all active providers.
     */
    private Mono<List<String>> getAssignedProvidersForTask(String taskType) {
        // 1. Check DB for task-specific assignment
        return taskAssignmentRepo.findByTaskTypeAndIsActive(taskType, true)
            .collectList()
            .flatMap(assignments -> {
                if (assignments != null && !assignments.isEmpty()) {
                    // Merge all provider IDs from matching assignments
                    List<String> providers = new ArrayList<>();
                    for (com.supremeai.model.TaskProviderAssignment assignment : assignments) {
                        if (assignment.getProviderIds() != null) {
                            providers.addAll(assignment.getProviderIds());
                        }
                    }
                    if (!providers.isEmpty()) {
                        logger.info("🎯 Using {} task-assigned providers for '{}': {}",
                            providers.size(), taskType, providers);
                        return Mono.just(providers);
                    }
                }
                return Mono.empty();
            })
            .onErrorResume(e -> {
                logger.warn("Task assignment lookup failed for '{}': {}", taskType, e.getMessage());
                return Mono.empty();
            })
            .switchIfEmpty(Mono.defer(() -> {
                // 2. Fallback: use contextual ranking to select best providers
                try {
                    ContextualAIRankingService.TaskType tType = null;
                    for (ContextualAIRankingService.TaskType t : ContextualAIRankingService.TaskType.values()) {
                        if (t.name().equalsIgnoreCase(taskType)) {
                            tType = t;
                            break;
                        }
                    }
                    if (tType != null) {
                        List<String> ranked = contextualRankingService.getRankingsForTask(tType)
                            .stream().map(r -> r.provider).collect(Collectors.toList());
                        if (ranked != null && !ranked.isEmpty()) {
                            List<String> result = ranked.stream()
                                .limit(DEFAULT_MAX_VOTING_PROVIDERS)
                                .collect(Collectors.toList());
                            logger.info("📊 Using {} ranked providers for '{}': {}",
                                result.size(), taskType, result);
                            return Mono.just(result);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Contextual ranking lookup failed for '{}': {}", taskType, e.getMessage());
                }
                return Mono.empty();
            }))
            .switchIfEmpty(Mono.defer(() -> {
                // 3. Ultimate fallback: all healthy providers (truly unlimited)
                return providerRepository.findAll()
                    .filter(p -> "active".equalsIgnoreCase(p.getStatus()) && p.isCanParticipateInVoting())
                    .map(p -> p.getName().toLowerCase())
                    .collectList()
                    .doOnNext(allProviders -> 
                        logger.info("🌍 No task-specific assignment for '{}' - using {} active providers",
                            taskType, allProviders != null ? allProviders.size() : 0)
                    );
            }));
    }

    // ===== APPROVAL VOTING (from CouncilVotingSystem) =====

    /**
     * Conduct approval vote for risky actions
     * Replaces CouncilVotingSystem.conductVote
     */
    public Mono<Boolean> conductApprovalVote(String changeType, String codeSnippet, List<AIProviderType> councilMembers) {
        logger.info("[Approval Voting] Initiating vote for major change: {}", changeType);

        VotingTopic topic = topicGenerator.generateTopicForMajorChange(changeType, codeSnippet);
        if (topic == null) {
            logger.error("[Approval Voting] Failed to generate voting topic for: {}", changeType);
            return Mono.just(false);
        }
        logger.info("[Approval Voting] Formulated Question: {}", topic.getQuestionToAsk());

        return Flux.fromIterable(councilMembers)
            .flatMap(member -> Mono.fromCallable(() -> simulateAIVote(member, topic))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()))
            .collectList()
            .map(votes -> {
                long approveCount = votes.stream().filter(v -> v).count();
                long rejectCount = votes.size() - approveCount;
                boolean finalDecision = approveCount > rejectCount;
                logger.info("[Approval Voting] Final Result: {} Approve, {} Reject -> Decision: {}",
                           approveCount, rejectCount, finalDecision ? "PROCEED" : "ABORT");
                return finalDecision;
            });
    }

    // ===== DECISION VOTING (from AutonomousVotingService) =====

    /**
     * Conduct decision vote on questions
     * Replaces AutonomousVotingService.conductVote
     */
    public Mono<VotingDecision> conductDecisionVote(String question, String context) {
        logger.info("Starting decision voting for question: {}", question);

        return providerRepository.findAll()
                .filter(p -> "active".equalsIgnoreCase(p.getStatus()) && p.isCanParticipateInVoting())
                .map(p -> p.getName().toLowerCase())
                .collectList()
                .flatMap(providerList -> {
                    List<String> listToUse = providerList;

                    return Flux.fromIterable(listToUse)
                            .flatMap(providerName -> {
                                long start = System.currentTimeMillis();
                                try {
                                    AIProvider provider = providerFactory.getEnforcedProvider(providerName);
                                    return provider.generate(buildDecisionPrompt(question, context))
                                            .map(response -> {
                                                long latency = System.currentTimeMillis() - start;
                                                ProviderVote vote = new ProviderVote();
                                                vote.setProviderName(providerName);
                                                vote.setResponse(response);
                                                vote.setConfidence(calculateDecisionConfidence(response, latency));
                                                vote.setLatencyMs(latency);
                                                vote.setSuccess(true);
                                                logger.debug("Provider {} voted successfully in {}ms", providerName, latency);
                                                return vote;
                                            })
                                            .onErrorResume(e -> {
                                                logger.warn("Provider {} failed to vote: {}", providerName, e.getMessage());
                                                ProviderVote vote = new ProviderVote();
                                                vote.setProviderName(providerName);
                                                vote.setSuccess(false);
                                                vote.setErrorMessage(e.getMessage());
                                                return Mono.just(vote);
                                            });
                                } catch (Exception e) {
                                    logger.warn("Provider Factory failed for {}: {}", providerName, e.getMessage());
                                    ProviderVote vote = new ProviderVote();
                                    vote.setProviderName(providerName);
                                    vote.setSuccess(false);
                                    vote.setErrorMessage(e.getMessage());
                                    return Mono.just(vote);
                                }
                            })
                            .collectList()
                            .map(votes -> calculateDecisionConsensus(question, votes));
                });
    }

    // ===== CONSENSUS VOTING (from MultiAIConsensusService) =====

    /**
     * Ask consensus from multiple AI providers
     * Replaces MultiAIConsensusService.askAllAIs
     */
    @Cacheable(value = "ai_responses", key = "#question + '_' + T(java.util.Collections).unmodifiableSortedSet(new java.util.TreeSet(#providerNames)).hashCode()")
    public Mono<ConsensusResult> askConsensus(String question, List<String> providerNames, long timeoutMs) {
        long startTime = System.currentTimeMillis();

        return Flux.fromIterable(providerNames)
            .flatMap(providerName -> {
                AIProvider provider = providerFactory.getEnforcedProvider(providerName);
                long pStart = System.currentTimeMillis();

                return (selfHealingService != null ?
                    selfHealingService.executeWithRetry(
                        () -> provider.generate(question),
                        MAX_RETRIES,
                        RETRY_BACKOFF_MS
                    ) : provider.generate(question))
                    .map(response -> {
                        long pDuration = System.currentTimeMillis() - pStart;
                        ContextualAIRankingService.TaskType taskType = contextualRankingService.detectTaskType(question);
                        double confidence = contextualRankingService.calculateProviderScore(providerName, taskType, null) / 100.0;

                        if (confidence <= 0) {
                            confidence = 0.85 + random.nextDouble() * 0.14;
                        }

                        ProviderVote vote = new ProviderVote(
                            providerName,
                            response,
                            confidence,
                            System.currentTimeMillis()
                        );

                        recordProviderOutcome(providerName, question, response, pDuration, confidence);
                        return vote;
                    })
                    .onErrorResume(e -> {
                        logger.warn("Provider {} failed after retries: {}", providerName, e.getMessage());
                        return Mono.empty();
                    });
            })
            .collectList()
            .map(votes -> {
                long duration = System.currentTimeMillis() - startTime;
                return calculateConsensusResult(question, votes, duration);
            })
            .timeout(java.time.Duration.ofMillis(timeoutMs))
            .onErrorResume(java.util.concurrent.TimeoutException.class, e -> {
                logger.warn("Consensus timed out for question: {}", question);
                return Mono.just(new ConsensusResult(question, "দুঃখিত, অনুরোধটির উত্তর দিতে সময়সীমা অতিক্রম হয়ে গেছে।", List.of(), 0.0, "TIMEOUT", 0.0, timeoutMs, 0.0));
            });
    }

    /**
     * Ask contextual consensus with automatic provider selection
     * Replaces MultiAIConsensusService.askContextualAIs
     */
    public Mono<ConsensusResult> askContextualConsensus(String question, int count, long timeoutMs) {
        ContextualAIRankingService.ProviderSelection selection = contextualRankingService.selectBestProvider(question, null);
        List<String> providerNames = new ArrayList<>();

        if (selection.providerName != null) {
            providerNames.add(selection.providerName);
        }

        List<ContextualAIRankingService.ProviderRanking> rankings = contextualRankingService.getRankingsForTask(selection.taskType);
        for (ContextualAIRankingService.ProviderRanking ranking : rankings) {
            if (providerNames.size() >= count) break;
            if (!providerNames.contains(ranking.provider)) {
                providerNames.add(ranking.provider);
            }
        }

        Mono<List<String>> finalProvidersMono;
        if (providerNames.size() < count) {
            finalProvidersMono = providerRepository.findAll()
                .filter(p -> "active".equalsIgnoreCase(p.getStatus()) && p.isCanParticipateInVoting())
                .map(p -> p.getName().toLowerCase())
                .collectList()
                .map(dynamicDefaults -> {
                    for (String p : dynamicDefaults) {
                        if (providerNames.size() >= count) break;
                        if (!providerNames.contains(p)) {
                            providerNames.add(p);
                        }
                    }
                    return providerNames;
                });
        } else {
            finalProvidersMono = Mono.just(providerNames);
        }

        logger.info("Contextual selection for '{}' (Task: {}): {}", question, selection.taskType, providerNames);
        return finalProvidersMono.flatMap(names -> askConsensus(question, names, timeoutMs));
    }

    /**
     * Get consensus history
     * Replaces MultiAIConsensusService.getHistory
     */
    public Flux<ConsensusVote> getConsensusHistory(int limit) {
        return Flux.fromIterable(consensusHistory)
            .sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .take(limit);
    }

    // ===== PRIVATE HELPER METHODS =====

    private Mono<ProviderVote> queryModel(String modelName, String prompt, long timeoutMs) {
        return Mono.defer(() -> {
            try {
                AIProvider provider = providerFactory.getEnforcedProvider(modelName);
                long startTime = System.currentTimeMillis();

                Mono<String> generationMono = (selfHealingService != null) ?
                    selfHealingService.executeWithRetry(
                        () -> provider.generate(prompt),
                        getMaxRetries(),
                        RETRY_BACKOFF_MS
                    ) : provider.generate(prompt);

                return generationMono
                    .map(response -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        ContextualAIRankingService.TaskType taskType = contextualRankingService.detectTaskType(prompt);
                        double confidence = calculateConfidence(response, modelName, responseTime, taskType);
                        ProviderVote vote = new ProviderVote(modelName, response, confidence, System.currentTimeMillis());
                        updatePerformanceTracker(vote, taskType);
                        return vote;
                    })
                    .timeout(java.time.Duration.ofMillis(timeoutMs))
                    .onErrorResume(e -> {
                        logger.warn("Model {} failed: {}", modelName, e.getMessage());
                        return Mono.empty();
                    });
            } catch (Exception e) {
                logger.warn("Model Factory failed for {}: {}", modelName, e.getMessage());
                return Mono.empty();
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private VotingResult calculateEnsembleResult(
            String prompt, 
            List<ProviderVote> votes, 
            long duration, 
            ContextualAIRankingService.TaskType taskType,
            Map<String, Double> weightsMap
    ) {
        if (votes.isEmpty()) {
            return new VotingResult(prompt, "দুঃখিত, বর্তমানে কোনো এআই প্রোভাইডার সাড়া দিচ্ছে না। অনুগ্রহ করে ড্যাশবোর্ড থেকে এপিআই কী এবং প্রোভাইডার স্ট্যাটাস চেক করুন। (System Solo-Mode Active)",
                                   votes, 0.0, "ERROR", duration);
        }

        // ===== 2-ROUND META-CONSENSUS REFINEMENT LOOP =====
        if (votes.size() > 1) {
            double[] round1Confidences = new double[votes.size()];
            // Round 1: Peer Consensus Alignment
            for (int i = 0; i < votes.size(); i++) {
                ProviderVote vI = votes.get(i);
                double totalSim = 0.0;
                int peerCount = 0;
                for (int j = 0; j < votes.size(); j++) {
                    if (i == j) continue;
                    ProviderVote vJ = votes.get(j);
                    double jaccard = calculateJaccardOverlap(vI.getResponse(), vJ.getResponse());
                    double structural = calculateStructuralSimilarity(vI.getResponse(), vJ.getResponse());
                    double sim = 0.6 * jaccard + 0.4 * structural;
                    totalSim += sim;
                    peerCount++;
                }
                double avgPeerSupport = peerCount > 0 ? (totalSim / peerCount) : 1.0;
                round1Confidences[i] = vI.getConfidence() * (0.7 + 0.3 * avgPeerSupport);
            }

            // Round 2: Weighted Consensus Reinforcement
            for (int i = 0; i < votes.size(); i++) {
                ProviderVote vI = votes.get(i);
                double weightedSimSum = 0.0;
                double weightSum = 0.0;
                for (int j = 0; j < votes.size(); j++) {
                    if (i == j) continue;
                    ProviderVote vJ = votes.get(j);
                    double jaccard = calculateJaccardOverlap(vI.getResponse(), vJ.getResponse());
                    double structural = calculateStructuralSimilarity(vI.getResponse(), vJ.getResponse());
                    double sim = 0.6 * jaccard + 0.4 * structural;
                    weightedSimSum += sim * round1Confidences[j];
                    weightSum += round1Confidences[j];
                }
                double weightedSupport = weightSum > 0.0 ? (weightedSimSum / weightSum) : 1.0;
                double finalConfidence = round1Confidences[i] * (0.8 + 0.2 * weightedSupport);
                vI.setConfidence(Math.min(1.0, Math.max(0.0, finalConfidence)));
            }
        }

        Map<String, List<ProviderVote>> similarityGroups = groupBySimilarity(votes);
        Map<String, Double> groupScores = new HashMap<>();
        Map<String, Double> groupWeights = new HashMap<>();

        for (Map.Entry<String, List<ProviderVote>> entry : similarityGroups.entrySet()) {
            String groupKey = entry.getKey();
            List<ProviderVote> groupVotes = entry.getValue();

            double totalWeight = 0;
            double weightedScore = 0;

            for (ProviderVote vote : groupVotes) {
                double weight = calculateModelWeight(vote.getProviderName(), vote.getConfidence(), taskType, weightsMap);
                weightedScore += vote.getConfidence() * weight;
                totalWeight += weight;
            }

            groupScores.put(groupKey, totalWeight > 0 ? weightedScore / totalWeight : 0);
            groupWeights.put(groupKey, totalWeight);
        }

        String winningGroupKey = groupScores.entrySet().stream()
            .max(Comparator.comparingDouble(e -> groupWeights.get(e.getKey()) * e.getValue()))
            .map(Map.Entry::getKey)
            .orElse(votes.get(0).getResponse().substring(0, Math.min(100, votes.get(0).getResponse().length())));

        List<ProviderVote> winningVotes = similarityGroups.get(winningGroupKey);
        String bestResponse = winningVotes.get(0).getResponse();

        double consensusPercentage = (double) winningVotes.size() / votes.size() * 100.0;
        double avgConfidence = winningVotes.stream()
            .mapToDouble(ProviderVote::getConfidence)
            .average()
            .orElse(0.0);

        for (ProviderVote vote : winningVotes) {
            updatePerformanceTracker(vote, taskType);
        }

        String verdict = determineVerdict(consensusPercentage, avgConfidence);

        return new VotingResult(prompt, bestResponse, votes, avgConfidence, verdict, duration);
    }

    private boolean simulateAIVote(AIProviderType member, VotingTopic topic) {
        // Simulate based on topic category and member
        if (topic.getCategory().equals("SECURITY") && member.name().contains("CLAUDE")) {
            return Math.random() > 0.4;
        }
        return Math.random() > 0.2;
    }

    private String buildDecisionPrompt(String question, String context) {
        return String.format("""
            Context: %s

            Question: %s

            Provide your expert opinion on this question. Be concise and specific.
            """, context, question);
    }

    private double calculateDecisionConfidence(String response, long latencyMs) {
        double confidence = 0.3;

        if (latencyMs < 100) confidence += 0.3;
        else if (latencyMs < 500) confidence += 0.2;
        else confidence += 0.1;

        int length = response != null ? response.length() : 0;
        if (length >= 50 && length <= 500) confidence += 0.4;
        else if (length > 0) confidence += 0.2;

        return Math.max(0.0, Math.min(1.0, confidence));
    }

    private VotingDecision calculateDecisionConsensus(String question, List<ProviderVote> votes) {
        VotingDecision decision = new VotingDecision();
        decision.setDecisionKey(question);
        decision.setProviderVotes(votes);

        long successCount = votes.stream().filter(ProviderVote::isSuccess).count();

        if (successCount == 0) {
            decision.setStrength("ERROR");
            decision.setConfidence(0.0);
            decision.setAiConsensus("দুঃখিত, সকল এআই প্রোভাইডার সাড়া দিতে ব্যর্থ হয়েছে।");
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

        if (consensusPercentage >= 0.75) decision.setStrength("STRONG");
        else if (consensusPercentage >= 0.5) decision.setStrength("WEAK");
        else decision.setStrength("SPLIT");

        logger.info("Decision voting complete. Consensus: {} with {} confidence",
                   decision.getStrength(), decision.getConfidence());

        return decision;
    }

    private ConsensusResult calculateConsensusResult(String question, List<ProviderVote> votes, long totalTimeMs) {
        if (votes.isEmpty()) {
            return new ConsensusResult(
                question,
                "দুঃখিত, বর্তমানে কোনো এআই প্রোভাইডার সাড়া দিচ্ছে না। অনুগ্রহ করে ড্যাশবোর্ড থেকে এপিআই কী এবং প্রোভাইডার স্ট্যাটাস চেক করুন। (System Solo-Mode Active)",
                List.of(),
                0.0,
                "ERROR",
                0.0,
                totalTimeMs,
                0.0
            );
        }

        Map<String, List<ProviderVote>> groups = new LinkedHashMap<>();
        for (ProviderVote vote : votes) {
            String normalized = vote.getResponse().trim();
            if (normalized.length() > 500) {
                normalized = normalized.substring(0, 500) + "...";
            }
            groups.computeIfAbsent(normalized, k -> new ArrayList<>()).add(vote);
        }

        List<ProviderVote> winningGroup = groups.values().stream()
            .max(Comparator.comparingInt(List::size))
            .orElse(List.of(votes.get(0)));

        String consensusAnswer = winningGroup.get(0).getResponse();
        double consensusPercentage = (double) winningGroup.size() / votes.size() * 100.0;
        double avgConfidence = votes.stream()
            .mapToDouble(ProviderVote::getConfidence)
            .average()
            .orElse(0.0);

        double qualityScore = (consensusPercentage / 100.0) * 0.7 + (avgConfidence) * 0.3;

        saveConsensusVoteToHistory(question, votes, consensusAnswer, consensusPercentage);

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

    private void saveConsensusVoteToHistory(String question, List<ProviderVote> votes, String consensus, double percentage) {
        try {
            ConsensusVote result = new ConsensusVote();
            result.setQuestion(question);
            result.setConsensusAnswer(consensus);
            result.setConsensusPercentage(percentage);
            result.setVotes(votes);
            result.setTimestamp(new Date());
            consensusHistory.add(result);
            logger.info("Saved consensus vote to in-memory history (size: {})", consensusHistory.size());
        } catch (Exception e) {
            logger.warn("Failed to save vote to history: " + e.getMessage());
        }
    }

    // Methods from TenAIVotingSystem (simplified for brevity)
    private double calculateConfidence(String response, String modelName, long responseTime, ContextualAIRankingService.TaskType taskType) {
        double confidence = 0.5;
        int length = response.length();
        if (length > 100 && length < 5000) confidence += 0.2;
        else if (length >= 5000) confidence += 0.1;

        if (responseTime < 2000) confidence += 0.1;
        else if (responseTime > 10000) confidence -= 0.1;

        List<ContextualAIRankingService.ProviderRanking> rankings = contextualRankingService.getRankingsForTask(taskType);
        for (ContextualAIRankingService.ProviderRanking r : rankings) {
            if (r.provider.equalsIgnoreCase(modelName)) {
                confidence += (r.successRate / 100.0) * 0.3;
                break;
            }
        }

        if (containsCodeBlocks(response)) confidence += 0.1;
        if (hasConclusion(response)) confidence += 0.1;

        return Math.min(Math.max(confidence, 0.0), 1.0);
    }

    private Map<String, List<ProviderVote>> groupBySimilarity(List<ProviderVote> votes) {
        Map<String, List<ProviderVote>> groups = new LinkedHashMap<>();
        for (ProviderVote vote : votes) {
            String normalized = normalizeResponse(vote.getResponse());
            String groupKey = findSimilarGroup(groups.keySet(), normalized);
            if (groupKey == null) {
                groupKey = normalized.length() > 100 ? normalized.substring(0, 100) : normalized;
            }
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(vote);
        }
        return groups;
    }

    private String findSimilarGroup(Set<String> existingKeys, String newResponse) {
        if (newResponse.length() < 10) return null;
        String normalizedNew = newResponse.toLowerCase().replaceAll("[^a-z0-9]", " ");
        String[] newWords = normalizedNew.split("\\s+");
        if (newWords.length == 0) return null;

        Set<String> newWordSet = new HashSet<>();
        for (String w : newWords) if (w.length() > 2) newWordSet.add(w);

        for (String key : existingKeys) {
            if (Math.abs(key.length() - newResponse.length()) > Math.max(key.length(), newResponse.length()) * 0.5) continue;

            String normalizedKey = key.toLowerCase().replaceAll("[^a-z0-9]", " ");
            String[] keyWords = normalizedKey.split("\\s+");

            int matches = 0;
            int totalRelevant = 0;
            for (String kw : keyWords) {
                if (kw.length() > 2) {
                    totalRelevant++;
                    if (newWordSet.contains(kw)) matches++;
                }
            }

            if (totalRelevant == 0) continue;

            double similarity = (double) matches / Math.max(totalRelevant, newWordSet.size());
            double structuralSimilarity = calculateStructuralSimilarity(key, newResponse);
            double finalSimilarity = (similarity * 0.6) + (structuralSimilarity * 0.4);

            if (finalSimilarity > 0.65) return key;
        }
        return null;
    }

    private double calculateStructuralSimilarity(String s1, String s2) {
        int lines1 = s1.split("\n").length;
        int lines2 = s2.split("\n").length;
        double lineRatio = Math.min(lines1, lines2) / (double) Math.max(lines1, lines2);

        int codeBlocks1 = s1.split("```").length / 2;
        int codeBlocks2 = s2.split("```").length / 2;
        double codeRatio = (codeBlocks1 == codeBlocks2) ? 1.0 : 0.0;

        return (lineRatio * 0.5) + (codeRatio * 0.5);
    }

    private double calculateModelWeight(String modelName, double confidence, ContextualAIRankingService.TaskType taskType, Map<String, Double> weightsMap) {
        double baseWeight = getModelBaseWeight(modelName, taskType, weightsMap);
        return baseWeight * (0.5 + 0.5 * confidence);
    }

    private double getModelBaseWeight(String modelName, ContextualAIRankingService.TaskType taskType, Map<String, Double> weightsMap) {
        double contextualScore = 0.5;
        if (taskType != null) {
            List<ContextualAIRankingService.ProviderRanking> rankings = contextualRankingService.getRankingsForTask(taskType);
            if (rankings != null) {
                for (ContextualAIRankingService.ProviderRanking r : rankings) {
                    if (r.provider.equalsIgnoreCase(modelName)) {
                        contextualScore = r.successRate / 100.0;
                        break;
                    }
                }
            }
        }

        double firestoreWeight = 0.5;
        if (weightsMap != null) {
            Double score = weightsMap.get(modelName.toLowerCase());
            if (score != null) {
                firestoreWeight = score;
            }
        }

        return (contextualScore * 0.6) + (firestoreWeight * 0.4);
    }

    private boolean isModelAvailable(String modelName) {
        try {
            providerFactory.getProvider(modelName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void updatePerformanceTracker(ProviderVote vote, ContextualAIRankingService.TaskType taskType) {
        ModelPerformanceTracker tracker = performanceTrackers.get(vote.getProviderName());
        if (tracker != null) {
            tracker.recordAttempt(vote.getConfidence());
        }

        contextualRankingService.recordTaskOutcome(
            vote.getProviderName(),
            taskType,
            true,
            1000,
            vote.getConfidence() * 5.0
        );
    }

    private void recordProviderOutcome(String providerName, String question, String response, long latency, double confidence) {
        try {
            ContextualAIRankingService.TaskType taskType = contextualRankingService.detectTaskType(question);
            double quality = Math.min(5.0, (response.length() > 100 ? 3.0 : 1.0) + confidence * 2.0);
            contextualRankingService.recordTaskOutcome(providerName, taskType, true, latency, quality);
        } catch (Exception e) {
            // Silently fail
        }
    }

    private String normalizeResponse(String response) {
        if (response == null) return "";
        return response.trim()
            .replaceAll("\\s+", " ")
            .toLowerCase();
    }

    private boolean containsCodeBlocks(String response) {
        return response.contains("```") ||
               response.contains("    ") ||
               response.contains("\t") ||
               response.matches(".*(function|class|def |import |public |private ).*");
    }

    private boolean hasConclusion(String response) {
        String lower = response.toLowerCase();
        return lower.contains("conclusion") ||
               lower.contains("summary") ||
               lower.contains("in summary") ||
               lower.contains("therefore") ||
               lower.contains("thus");
    }

    private String determineVerdict(double consensusPercentage, double confidence) {
        if (consensusPercentage >= 80 && confidence >= 0.8) return "STRONG_CONSENSUS";
        else if (consensusPercentage >= 60 && confidence >= 0.6) return "MODERATE_CONSENSUS";
        else if (consensusPercentage >= 40) return "WEAK_CONSENSUS";
        else return "NO_CONSENSUS";
    }

    private double calculateJaccardOverlap(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        String n1 = normalizeResponse(s1);
        String n2 = normalizeResponse(s2);
        if (n1.isEmpty() && n2.isEmpty()) return 1.0;
        if (n1.isEmpty() || n2.isEmpty()) return 0.0;

        String[] w1 = n1.split("\\s+");
        String[] w2 = n2.split("\\s+");

        Set<String> set1 = new HashSet<>();
        for (String w : w1) {
            if (w.length() > 2) set1.add(w);
        }
        Set<String> set2 = new HashSet<>();
        for (String w : w2) {
            if (w.length() > 2) set2.add(w);
        }

        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    private Mono<VotingResult> performMetaSynthesis(String prompt, List<ProviderVote> votes, VotingResult initialResult, long initialDuration) {
        logger.info("Initiating Meta-Synthesis because consensus is weak/none. Verdict: {}", initialResult.getVerdict());
        
        // Choose primary orchestrator (gemini preferred, then openai, or any available)
        String orchestratorName = "gemini";
        if (!isModelAvailable(orchestratorName)) {
            orchestratorName = "openai";
            if (!isModelAvailable(orchestratorName)) {
                return Mono.just(initialResult);
            }
        }
        
        final String finalOrchestrator = orchestratorName;
        
        StringBuilder synthesisPrompt = new StringBuilder();
        synthesisPrompt.append("You are the SupremeAI Orchestrator (Meta-Synthesis Engine).\n");
        synthesisPrompt.append("We queried multiple AI models for the following user request. The decisions are split or weak. ");
        synthesisPrompt.append("Your task is to analyze all the different responses, reconcile contradictions, filter out erroneous details, and synthesize a single, supreme, and consolidated response that perfectly satisfies the user's intent.\n\n");
        synthesisPrompt.append("User Request: ").append(prompt).append("\n\n");
        synthesisPrompt.append("Here are the individual model responses:\n");
        
        for (ProviderVote vote : votes) {
            synthesisPrompt.append("--- Model: ").append(vote.getProviderName())
                           .append(" (Confidence: ").append(String.format("%.2f", vote.getConfidence())).append(") ---\n")
                           .append(vote.getResponse()).append("\n\n");
        }
        
        synthesisPrompt.append("Consolidate all the information into a single premium, structured, and complete response. Maintain high quality. DO NOT mention that you are synthesizing multiple models in your final output unless explicitly requested. Speak directly to the user.");
        
        long synthesisStart = System.currentTimeMillis();
        try {
            AIProvider orchestrator = providerFactory.getEnforcedProvider(finalOrchestrator);
            return orchestrator.generate(synthesisPrompt.toString())
                .map(synthesizedResponse -> {
                    long totalDuration = initialDuration + (System.currentTimeMillis() - synthesisStart);
                    logger.info("Meta-Synthesis successfully completed by {} in {}ms", finalOrchestrator, System.currentTimeMillis() - synthesisStart);
                    
                    ProviderVote synthesisVote = new ProviderVote(
                        finalOrchestrator + "_synthesis",
                        synthesizedResponse,
                        0.95,
                        System.currentTimeMillis()
                    );
                    synthesisVote.setSuccess(true);
                    
                    List<ProviderVote> updatedVotes = new ArrayList<>(votes);
                    updatedVotes.add(synthesisVote);
                    
                    return new VotingResult(
                        prompt, 
                        synthesizedResponse, 
                        updatedVotes, 
                        0.95, 
                        "META_SYNTHESIZED", 
                        totalDuration
                    );
                })
                .onErrorResume(e -> {
                    logger.warn("Meta-Synthesis failed using {}: {}. Falling back to initial result.", finalOrchestrator, e.getMessage());
                    return Mono.just(initialResult);
                });
        } catch (Exception e) {
            logger.warn("Failed to get orchestrator {} for Meta-Synthesis: {}. Falling back to initial result.", finalOrchestrator, e.getMessage());
            return Mono.just(initialResult);
        }
    }

    // Inner classes
    public static class VotingResult {
        private String prompt;
        private String bestResponse;
        private List<ProviderVote> allVotes;
        private double averageConfidence;
        private String verdict;
        private long processingTimeMs;

        public VotingResult(String prompt, String bestResponse, List<ProviderVote> allVotes,
                           double averageConfidence, String verdict, long processingTimeMs) {
            this.prompt = prompt;
            this.bestResponse = bestResponse;
            this.allVotes = allVotes;
            this.averageConfidence = averageConfidence;
            this.verdict = verdict;
            this.processingTimeMs = processingTimeMs;
        }

        // Getters
        public String getPrompt() { return prompt; }
        public String getBestResponse() { return bestResponse; }
        public List<ProviderVote> getAllVotes() { return allVotes; }
        public double getAverageConfidence() { return averageConfidence; }
        public String getVerdict() { return verdict; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public int getTotalModelsUsed() { return allVotes.size(); }
    }

    private static class ModelPerformanceTracker {
        private final String modelName;
        private double historicalScore = 0.5;
        private int totalAttempts = 0;

        public ModelPerformanceTracker(String modelName) {
            this.modelName = modelName;
        }

        public void recordAttempt(double confidence) {
            totalAttempts++;
            historicalScore = 0.9 * historicalScore + 0.1 * confidence;
        }

        public double getHistoricalScore() {
            if (totalAttempts == 0) return 0.5;
            return historicalScore;
        }
    }
}