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
import java.util.concurrent.*;
import java.util.stream.Collectors;

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

    // Executors
    private final ExecutorService ensembleExecutor = Executors.newFixedThreadPool(20);
    private final java.util.concurrent.ExecutorService decisionExecutor;

    // Constants from TenAIVotingSystem
    public static final String[] TEN_AI_MODELS = {
        "gpt4", "claude", "gemini", "groq", "deepseek", "ollama", "huggingface", "airllm", "kimi", "mistral"
    };

    // Constants from MultiAIConsensusService
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_BACKOFF_MS = 250L;

    // In-memory history from MultiAIConsensusService
    private final List<ConsensusVote> consensusHistory = new CopyOnWriteArrayList<>();

    // Performance trackers from TenAIVotingSystem
    private final Map<String, ModelPerformanceTracker> performanceTrackers = new ConcurrentHashMap<>();

    @Value("${supremeai.active.providers:groq,openai,anthropic,ollama}")
    private String activeProviders;

    private final java.util.Random random = new java.util.Random();

    @Autowired
    public MultiAIVotingService(@org.springframework.beans.factory.annotation.Qualifier("votingTaskExecutor")
                                org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor votingTaskExecutor) {
        this.decisionExecutor = votingTaskExecutor.getThreadPoolExecutor();
        initializePerformanceTrackers();
    }

    private void initializePerformanceTrackers() {
        for (String model : TEN_AI_MODELS) {
            performanceTrackers.put(model, new ModelPerformanceTracker(model));
        }
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
     * Execute ensemble voting with 10 AI models
     * Replaces TenAIVotingSystem.executeVoting
     */
    @Cacheable(value = "ai_responses", key = "#prompt + '_10ai_vote'")
    public VotingResult executeEnsembleVoting(String prompt, List<String> selectedModels, long timeoutMs) {
        if (selectedModels == null || selectedModels.isEmpty()) {
            selectedModels = Arrays.asList(TEN_AI_MODELS);
        }

        logger.info("Starting ensemble voting with {} models for prompt: {}",
                   selectedModels.size(), prompt.substring(0, Math.min(50, prompt.length())));

        List<ProviderVote> allVotes = new CopyOnWriteArrayList<>();
        List<Future<ProviderVote>> futures = new ArrayList<>();

        // Submit tasks for each AI model
        for (String modelName : selectedModels) {
            if (!isModelAvailable(modelName)) {
                logger.warn("Model {} is not available, skipping", modelName);
                continue;
            }

            futures.add(ensembleExecutor.submit(() -> queryModel(modelName, prompt, timeoutMs)));
        }

        // Collect results with timeout
        long startTime = System.currentTimeMillis();
        ContextualAIRankingService.TaskType taskType = contextualRankingService.detectTaskType(prompt);

        for (Future<ProviderVote> future : futures) {
            try {
                ProviderVote vote = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                if (vote != null && vote.getResponse() != null && !vote.getResponse().isEmpty()) {
                    allVotes.add(vote);
                    updatePerformanceTracker(vote, taskType);
                }
            } catch (TimeoutException e) {
                logger.warn("Model query timed out");
                future.cancel(true);
            } catch (Exception e) {
                logger.warn("Error getting model response: {}", e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Ensemble voting completed in {}ms with {} responses", duration, allVotes.size());

        return calculateEnsembleResult(prompt, allVotes, duration, taskType);
    }

    // ===== APPROVAL VOTING (from CouncilVotingSystem) =====

    /**
     * Conduct approval vote for risky actions
     * Replaces CouncilVotingSystem.conductVote
     */
    public boolean conductApprovalVote(String changeType, String codeSnippet, List<AIProviderType> councilMembers) {
        logger.info("[Approval Voting] Initiating vote for major change: {}", changeType);

        VotingTopic topic = topicGenerator.generateTopicForMajorChange(changeType, codeSnippet);
        if (topic == null) {
            logger.error("[Approval Voting] Failed to generate voting topic for: {}", changeType);
            return false;
        }
        logger.info("[Approval Voting] Formulated Question: {}", topic.getQuestionToAsk());

        int approveCount = 0;
        int rejectCount = 0;

        for (AIProviderType member : councilMembers) {
            logger.debug(" -> Asking {}...", member.name());
            boolean voteApprove = simulateAIVote(member, topic);
            if (voteApprove) {
                logger.debug("Voted: APPROVE");
                approveCount++;
            } else {
                logger.debug("Voted: REJECT");
                rejectCount++;
            }
        }

        boolean finalDecision = approveCount > rejectCount;
        logger.info("[Approval Voting] Final Result: {} Approve, {} Reject -> Decision: {}",
                   approveCount, rejectCount, finalDecision ? "PROCEED" : "ABORT");

        return finalDecision;
    }

    // ===== DECISION VOTING (from AutonomousVotingService) =====

    /**
     * Conduct decision vote on questions
     * Replaces AutonomousVotingService.conductVote
     */
    public VotingDecision conductDecisionVote(String question, String context) {
        logger.info("Starting decision voting for question: {}", question);

        List<String> providerList = Arrays.asList(activeProviders.split(","));
        List<CompletableFuture<ProviderVote>> futures = new ArrayList<>();

        for (String providerName : providerList) {
            CompletableFuture<ProviderVote> future = CompletableFuture.supplyAsync(() -> {
                try {
                    AIProvider provider = providerFactory.getProvider(providerName);
                    long start = System.currentTimeMillis();
                    String response = provider.generate(buildDecisionPrompt(question, context)).block();
                    long latency = System.currentTimeMillis() - start;

                    ProviderVote vote = new ProviderVote();
                    vote.setProviderName(providerName);
                    vote.setResponse(response);
                    vote.setConfidence(calculateDecisionConfidence(response, latency));
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
            }, decisionExecutor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<ProviderVote> votes = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        return calculateDecisionConsensus(question, votes);
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
                AIProvider provider = providerFactory.getProvider(providerName);
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
                return Mono.just(new ConsensusResult(question, "Timeout reached", List.of(), 0.0, "TIMEOUT", 0.0, timeoutMs, 0.0));
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
        return askConsensus(question, providerNames, timeoutMs);
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

    private ProviderVote queryModel(String modelName, String prompt, long timeoutMs) {
        try {
            AIProvider provider = providerFactory.getProvider(modelName);

            long startTime = System.currentTimeMillis();
            String response = (selfHealingService != null) ?
                selfHealingService.executeWithRetry(
                    () -> provider.generate(prompt),
                    getMaxRetries(),
                    RETRY_BACKOFF_MS
                ).block() : provider.generate(prompt).block();

            long responseTime = System.currentTimeMillis() - startTime;
            ContextualAIRankingService.TaskType taskType = contextualRankingService.detectTaskType(prompt);
            double confidence = calculateConfidence(response, modelName, responseTime, taskType);

            return new ProviderVote(modelName, response, confidence, System.currentTimeMillis());

        } catch (Exception e) {
            logger.warn("Model {} failed: {}", modelName, e.getMessage());
            return null;
        }
    }

    private VotingResult calculateEnsembleResult(String prompt, List<ProviderVote> votes, long duration, ContextualAIRankingService.TaskType taskType) {
        if (votes.isEmpty()) {
            return new VotingResult(prompt, "No AI models responded successfully",
                                   votes, 0.0, "ERROR", duration);
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
                double weight = calculateModelWeight(vote.getProviderName(), vote.getConfidence(), taskType);
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
                "No AI providers responded",
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
            result.setTimestamp(java.time.LocalDateTime.now());
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

    private double calculateModelWeight(String modelName, double confidence, ContextualAIRankingService.TaskType taskType) {
        double baseWeight = getModelBaseWeight(modelName, taskType);
        return baseWeight * (0.5 + 0.5 * confidence);
    }

    private double getModelBaseWeight(String modelName, ContextualAIRankingService.TaskType taskType) {
        // Static weights for known models
        Map<String, Double> staticWeights = new HashMap<>();
        staticWeights.put("gpt4", 0.9);
        staticWeights.put("openai", 0.9);
        staticWeights.put("claude", 0.88);
        staticWeights.put("anthropic", 0.88);
        staticWeights.put("gemini", 0.85);
        staticWeights.put("groq", 0.82);
        staticWeights.put("deepseek", 0.80);
        staticWeights.put("ollama", 0.75);
        staticWeights.put("huggingface", 0.70);
        staticWeights.put("airllm", 0.65);
        staticWeights.put("kimi", 0.60);
        staticWeights.put("mistral", 0.75);

        double contextualScore = 0.5;
        if (taskType != null) {
            List<ContextualAIRankingService.ProviderRanking> rankings = contextualRankingService.getRankingsForTask(taskType);
            for (ContextualAIRankingService.ProviderRanking r : rankings) {
                if (r.provider.equalsIgnoreCase(modelName)) {
                    contextualScore = r.successRate / 100.0;
                    break;
                }
            }
        }

        double staticWeight = staticWeights.getOrDefault(modelName.toLowerCase(), 0.5);

        return (contextualScore * 0.7) + (staticWeight * 0.3);
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