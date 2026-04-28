package com.supremeai.service;

import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.service.SelfHealingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 10-AI Voting System (S4)
 * Enhanced ensemble algorithm that uses 10 AI models to vote on best responses
 * Uses weighted voting based on model confidence, historical performance, and response quality
 */
@Service
public class TenAIVotingSystem {

    private static final Logger logger = LoggerFactory.getLogger(TenAIVotingSystem.class);

    // The 10 AI Models for voting
    public static final String[] TEN_AI_MODELS = {
        "gpt4",      // OpenAI GPT-4
        "claude",     // Anthropic Claude
        "gemini",     // Google Gemini
        "groq",       // Groq (Mixtral/Llama)
        "deepseek",   // DeepSeek Coder
        "ollama",     // Local Ollama models
        "huggingface",// HuggingFace models
        "airllm",     // AirLLM hosted models
        "kimi",       // Kimi AI
        "mistral"     // Mistral AI
    };

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private AIRankingService aiRankingService;

    @Autowired
    private ContextualAIRankingService contextualRankingService;

    @Autowired(required = false)
    private SelfHealingService selfHealingService;

    private final ExecutorService executor;
    private final Map<String, ModelPerformanceTracker> performanceTrackers;

    private static final int DEFAULT_TIMEOUT_MS = 15000;
    private static final int MAX_RETRIES = 2;
    private static final double CONFIDENCE_THRESHOLD = 0.6;

    public TenAIVotingSystem() {
        this.executor = Executors.newFixedThreadPool(20);
        this.performanceTrackers = new ConcurrentHashMap<>();
        initializePerformanceTrackers();
    }

    /**
     * Initialize performance trackers for each model
     */
    private void initializePerformanceTrackers() {
        for (String model : TEN_AI_MODELS) {
            performanceTrackers.put(model, new ModelPerformanceTracker(model));
        }
    }

    /**
     * Execute 10-AI voting and return the best response
     */
    @Cacheable(value = "ai_responses", key = "#prompt + '_10ai_vote'")
    public VotingResult executeVoting(String prompt, List<String> selectedModels, long timeoutMs) {
        if (selectedModels == null || selectedModels.isEmpty()) {
            selectedModels = Arrays.asList(TEN_AI_MODELS);
        }

        logger.info("Starting 10-AI voting with {} models for prompt: {}", selectedModels.size(), 
                    prompt.substring(0, Math.min(50, prompt.length())));

        List<ProviderVote> allVotes = new CopyOnWriteArrayList<>();
        List<Future<ProviderVote>> futures = new ArrayList<>();

        // Submit tasks for each AI model
        for (String modelName : selectedModels) {
            if (!isModelAvailable(modelName)) {
                logger.warn("Model {} is not available, skipping", modelName);
                continue;
            }

            futures.add(executor.submit(() -> queryModel(modelName, prompt, timeoutMs)));
        }

        // Collect results with timeout
        long startTime = System.currentTimeMillis();
        
        // Detect task type for better weighting
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
        logger.info("Voting completed in {}ms with {} responses", duration, allVotes.size());

        // Calculate ensemble result
        return calculateEnsembleResult(prompt, allVotes, duration, taskType);
    }

    /**
     * Query a single AI model
     */
    private ProviderVote queryModel(String modelName, String prompt, long timeoutMs) {
        try {
            AIProvider provider = providerFactory.getProvider(modelName);
            
            long startTime = System.currentTimeMillis();
            String response;
            
            if (selfHealingService != null) {
                response = selfHealingService.executeWithRetry(
                    () -> provider.generate(prompt),
                    MAX_RETRIES,
                    250L
                ).block();
            } else {
                response = provider.generate(prompt).block();
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Detect task type for confidence calculation
            ContextualAIRankingService.TaskType taskType = contextualRankingService.detectTaskType(prompt);
            
            double confidence = calculateConfidence(response, modelName, responseTime, taskType);
            
            return new ProviderVote(modelName, response, confidence, System.currentTimeMillis());
            
        } catch (Exception e) {
            logger.warn("Model {} failed: {}", modelName, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate confidence score for a model response
     */
    private double calculateConfidence(String response, String modelName, long responseTime, ContextualAIRankingService.TaskType taskType) {
        double confidence = 0.5; // Base confidence

        // Factor 1: Response length (longer responses often better, up to a point)
        int length = response.length();
        if (length > 100 && length < 5000) {
            confidence += 0.2;
        } else if (length >= 5000) {
            confidence += 0.1;
        }

        // Factor 2: Response time (faster responses get slight boost)
        if (responseTime < 2000) {
            confidence += 0.1;
        } else if (responseTime > 10000) {
            confidence -= 0.1;
        }

        // Factor 3: Historical performance (Context-aware)
        List<ContextualAIRankingService.ProviderRanking> rankings = contextualRankingService.getRankingsForTask(taskType);
        for (ContextualAIRankingService.ProviderRanking r : rankings) {
            if (r.provider.equalsIgnoreCase(modelName)) {
                confidence += (r.successRate / 100.0) * 0.3;
                break;
            }
        }

        // Factor 4: Code quality indicators (if applicable)
        if (containsCodeBlocks(response)) {
            confidence += 0.1;
        }

        // Factor 5: Response completeness (has conclusion/summary)
        if (hasConclusion(response)) {
            confidence += 0.1;
        }

        return Math.min(Math.max(confidence, 0.0), 1.0);
    }

    /**
     * Calculate ensemble result using weighted voting
     */
    private VotingResult calculateEnsembleResult(String prompt, List<ProviderVote> votes, long duration, ContextualAIRankingService.TaskType taskType) {
        if (votes.isEmpty()) {
            return new VotingResult(prompt, "No AI models responded successfully", 
                                   votes, 0.0, "ERROR", duration);
        }

        // Group responses by semantic similarity
        Map<String, List<ProviderVote>> similarityGroups = groupBySimilarity(votes);

        // Calculate weighted scores for each group
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

        // Find winning group
        String winningGroupKey = groupScores.entrySet().stream()
            .max(Comparator.comparingDouble(e -> groupWeights.get(e.getKey()) * e.getValue()))
            .map(Map.Entry::getKey)
            .orElse(votes.get(0).getResponse().substring(0, Math.min(100, votes.get(0).getResponse().length())));

        List<ProviderVote> winningVotes = similarityGroups.get(winningGroupKey);
        String bestResponse = winningVotes.get(0).getResponse();

        // Calculate consensus percentage
        double consensusPercentage = (double) winningVotes.size() / votes.size() * 100.0;

        // Calculate average confidence of winning group
        double avgConfidence = winningVotes.stream()
            .mapToDouble(ProviderVote::getConfidence)
            .average()
            .orElse(0.0);

        // Update performance trackers for winning models
        for (ProviderVote vote : winningVotes) {
            updatePerformanceTracker(vote, taskType);
        }

        String verdict = determineVerdict(consensusPercentage, avgConfidence);
        
        return new VotingResult(prompt, bestResponse, votes, avgConfidence, verdict, duration);
    }

    /**
     * Group responses by semantic similarity (simplified version using text hashing)
     */
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

    /**
     * Simple similarity check based on common words and structure (Optimized)
     */
    private String findSimilarGroup(Set<String> existingKeys, String newResponse) {
        if (newResponse.length() < 10) return null;
        
        String normalizedNew = newResponse.toLowerCase().replaceAll("[^a-z0-9]", " ");
        String[] newWords = normalizedNew.split("\\s+");
        if (newWords.length == 0) return null;
        
        Set<String> newWordSet = new HashSet<>();
        for (String w : newWords) if (w.length() > 2) newWordSet.add(w);

        for (String key : existingKeys) {
            // Quick length check
            if (Math.abs(key.length() - newResponse.length()) > Math.max(key.length(), newResponse.length()) * 0.5) {
                continue;
            }

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

            // S4 Enhancement: Structural similarity (punctuation and line breaks)
            double structuralSimilarity = calculateStructuralSimilarity(key, newResponse);
            
            double finalSimilarity = (similarity * 0.6) + (structuralSimilarity * 0.4);

            if (finalSimilarity > 0.65) {
                return key;
            }
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

    /**
     * Calculate weight for a model based on name and confidence
     */
    private double calculateModelWeight(String modelName, double confidence, ContextualAIRankingService.TaskType taskType) {
        double baseWeight = getModelBaseWeight(modelName, taskType);
        return baseWeight * (0.5 + 0.5 * confidence); // Weight ranges from 50% to 100% of base
    }

    /**
     * Get base weight for each model (based on reputation/reliability and task performance)
     */
    private double getModelBaseWeight(String modelName, ContextualAIRankingService.TaskType taskType) {
        // 1. Get contextual ranking score (0.0 to 1.0)
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

        // 2. Get overall success rate (0.0 to 1.0)
        AIRankingService.ProviderRanking overallRanking = aiRankingService.getRankingForProvider(modelName);
        double overallScore = overallRanking.getSuccessRate() / 100.0;

        // 3. Fallback to static weights if no data
        double staticWeight;
        switch (modelName.toLowerCase()) {
            case "gpt4": staticWeight = 1.0; break;
            case "claude": staticWeight = 0.95; break;
            case "gemini": staticWeight = 0.9; break;
            case "deepseek": staticWeight = 0.85; break;
            case "groq": staticWeight = 0.8; break;
            case "mistral": staticWeight = 0.75; break;
            case "kimi": staticWeight = 0.7; break;
            case "huggingface": staticWeight = 0.65; break;
            case "airllm": staticWeight = 0.6; break;
            case "ollama": staticWeight = 0.5; break;
            default: staticWeight = 0.5;
        }

        // Combine scores: 40% task-specific, 30% overall performance, 30% reputation (static)
        if (overallRanking.getSuccessCount() == 0) {
            return staticWeight;
        }

        return (contextualScore * 0.4) + (overallScore * 0.3) + (staticWeight * 0.3);
    }

    /**
     * Check if model is available
     */
    private boolean isModelAvailable(String modelName) {
        try {
            providerFactory.getProvider(modelName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Update performance tracker for a model
     */
    private void updatePerformanceTracker(ProviderVote vote, ContextualAIRankingService.TaskType taskType) {
        // 1. Update internal tracker
        ModelPerformanceTracker tracker = performanceTrackers.get(vote.getProviderName());
        if (tracker != null) {
            tracker.recordAttempt(vote.getConfidence());
        }

        // 2. Sync with AIRankingService
        aiRankingService.recordSuccess(vote.getProviderName());

        // 3. Sync with ContextualAIRankingService
        // Estimate response time and quality for context recording
        contextualRankingService.recordTaskOutcome(
            vote.getProviderName(), 
            taskType, 
            true, 
            1000, // Approximate response time
            vote.getConfidence() * 5.0 // Convert to 0-5 scale
        );
    }

    /**
     * Normalize response for comparison
     */
    private String normalizeResponse(String response) {
        if (response == null) return "";
        return response.trim()
            .replaceAll("\\s+", " ")
            .toLowerCase();
    }

    /**
     * Check if response contains code blocks
     */
    private boolean containsCodeBlocks(String response) {
        return response.contains("```") || 
               response.contains("    ") || 
               response.contains("\t") ||
               response.matches(".*(function|class|def |import |public |private ).*");
    }

    /**
     * Check if response has conclusion
     */
    private boolean hasConclusion(String response) {
        String lower = response.toLowerCase();
        return lower.contains("conclusion") || 
               lower.contains("summary") || 
               lower.contains("in summary") ||
               lower.contains("therefore") ||
               lower.contains("thus");
    }

    /**
     * Determine verdict based on consensus and confidence
     */
    private String determineVerdict(double consensusPercentage, double confidence) {
        if (consensusPercentage >= 80 && confidence >= 0.8) {
            return "STRONG_CONSENSUS";
        } else if (consensusPercentage >= 60 && confidence >= 0.6) {
            return "MODERATE_CONSENSUS";
        } else if (consensusPercentage >= 40) {
            return "WEAK_CONSENSUS";
        } else {
            return "NO_CONSENSUS";
        }
    }

    /**
     * Voting result container
     */
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

        public String getPrompt() { return prompt; }
        public String getBestResponse() { return bestResponse; }
        public List<ProviderVote> getAllVotes() { return allVotes; }
        public double getAverageConfidence() { return averageConfidence; }
        public String getVerdict() { return verdict; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public int getTotalModelsUsed() { return allVotes.size(); }
    }

    /**
     * Track model performance over time
     */
    private static class ModelPerformanceTracker {
        private final String modelName;
        private double historicalScore = 0.5;
        private int totalAttempts = 0;
        private int successfulAttempts = 0;

        public ModelPerformanceTracker(String modelName) {
            this.modelName = modelName;
        }

        public void recordAttempt(double confidence) {
            totalAttempts++;
            successfulAttempts++;
            // Exponential moving average
            historicalScore = 0.9 * historicalScore + 0.1 * confidence;
        }

        public void recordSuccess(double confidence) {
            recordAttempt(confidence);
        }

        public double getHistoricalScore() {
            if (totalAttempts == 0) return 0.5;
            return historicalScore;
        }
    }
}
