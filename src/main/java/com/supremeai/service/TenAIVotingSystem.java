package com.supremeai.service;

import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
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

    @Autowired(required = false)
    private com.supremeai.selfhealing.SelfHealingService selfHealingService;

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
        for (Future<ProviderVote> future : futures) {
            try {
                ProviderVote vote = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                if (vote != null && vote.getResponse() != null && !vote.getResponse().isEmpty()) {
                    allVotes.add(vote);
                    updatePerformanceTracker(vote);
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
        return calculateEnsembleResult(prompt, allVotes, duration);
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
                );
            } else {
                response = provider.generate(prompt);
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            double confidence = calculateConfidence(response, modelName, responseTime);
            
            return new ProviderVote(modelName, response, confidence, System.currentTimeMillis());
            
        } catch (Exception e) {
            logger.warn("Model {} failed: {}", modelName, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate confidence score for a model response
     */
    private double calculateConfidence(String response, String modelName, long responseTime) {
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

        // Factor 3: Historical performance
        ModelPerformanceTracker tracker = performanceTrackers.get(modelName);
        if (tracker != null) {
            confidence += tracker.getHistoricalScore() * 0.3;
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
    private VotingResult calculateEnsembleResult(String prompt, List<ProviderVote> votes, long duration) {
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
                double weight = calculateModelWeight(vote.getProviderName(), vote.getConfidence());
                weightedScore += vote.getConfidence() * weight;
                totalWeight += weight;
            }

            groupScores.put(groupKey, totalWeight > 0 ? weightedScore / totalWeight : 0);
            groupWeights.put(groupKey, totalWeight);
        }

        // Find winning group
        String winningGroupKey = groupScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
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
            ModelPerformanceTracker tracker = performanceTrackers.get(vote.getProviderName());
            if (tracker != null) {
                tracker.recordSuccess(vote.getConfidence());
            }
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
     * Simple similarity check based on common words and structure
     */
    private String findSimilarGroup(Set<String> existingKeys, String newResponse) {
        String normalizedNew = newResponse.toLowerCase().replaceAll("[^a-z0-9]", " ");
        String[] newWords = normalizedNew.split("\\s+");
        Set<String> newWordSet = new HashSet<>(Arrays.asList(newWords));
        newWordSet.remove(""); // Remove empty strings

        for (String key : existingKeys) {
            String normalizedKey = key.toLowerCase().replaceAll("[^a-z0-9]", " ");
            String[] keyWords = normalizedKey.split("\\s+");
            Set<String> keyWordSet = new HashSet<>(Arrays.asList(keyWords));
            keyWordSet.remove("");

            // Calculate Jaccard similarity
            Set<String> intersection = new HashSet<>(newWordSet);
            intersection.retainAll(keyWordSet);

            Set<String> union = new HashSet<>(newWordSet);
            union.addAll(keyWordSet);

            double similarity = union.isEmpty() ? 0 : (double) intersection.size() / union.size();

            // S4 Enhancement: Length similarity boost
            double lengthRatio = Math.min(newResponse.length(), key.length()) / (double) Math.max(newResponse.length(), key.length());
            double finalSimilarity = (similarity * 0.7) + (lengthRatio * 0.3);

            if (finalSimilarity > 0.65) { // Adjusted threshold
                return key;
            }
        }

        return null;
    }

    /**
     * Calculate weight for a model based on name and confidence
     */
    private double calculateModelWeight(String modelName, double confidence) {
        double baseWeight = getModelBaseWeight(modelName);
        return baseWeight * (0.5 + 0.5 * confidence); // Weight ranges from 50% to 100% of base
    }

    /**
     * Get base weight for each model (based on reputation/reliability)
     */
    private double getModelBaseWeight(String modelName) {
        switch (modelName.toLowerCase()) {
            case "gpt4": return 1.0;      // Highest weight
            case "claude": return 0.95;
            case "gemini": return 0.9;
            case "deepseek": return 0.85;
            case "groq": return 0.8;
            case "mistral": return 0.75;
            case "kimi": return 0.7;
            case "huggingface": return 0.65;
            case "airllm": return 0.6;
            case "ollama": return 0.5;     // Local models get lower weight
            default: return 0.5;
        }
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
    private void updatePerformanceTracker(ProviderVote vote) {
        ModelPerformanceTracker tracker = performanceTrackers.get(vote.getProviderName());
        if (tracker != null) {
            tracker.recordAttempt(vote.getConfidence());
        }
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
