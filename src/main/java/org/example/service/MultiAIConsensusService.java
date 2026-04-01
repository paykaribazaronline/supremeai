package org.example.service;

import org.example.model.ConsensusVote;
import org.example.model.ConsensusFeedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Multi-AI Consensus Service
 * SupremeAI consults 10 AI providers, votes on best solution, learns from all
 */
@Service
public class MultiAIConsensusService {
    private static final Logger logger = LoggerFactory.getLogger(MultiAIConsensusService.class);
    
    @Autowired
    private SystemLearningService learningService;
    
    @Autowired
    private AIAPIService aiService; // Existing service to call AI providers
    
    @Autowired
    private QuotaService quotaService; // NEW: Track API quota usage
    
    private static final int CONSENSUS_THRESHOLD = 70; // Require 70% agreement
    private static final int AI_REQUEST_TIMEOUT_SEC = 5;
    private Map<String, ConsensusFeedback> feedbackHistory = new ConcurrentHashMap<>();
    
    // Simulate different AI provider perspectives
    private static final List<String> AI_PROVIDERS = Arrays.asList(
        "openai-gpt4",
        "anthropic-claude",
        "google-gemini",
        "meta-llama",
        "mistral",
        "cohere",
        "huggingface",
        "xai-grok",
        "deepseek",
        "perplexity"
    );
    
    private Map<String, ConsensusVote> voteHistory = new ConcurrentHashMap<>();
    
    /**
     * Ask all AI providers and get consensus
     */
    public ConsensusVote askAllAI(String question) {
        try {
            ConsensusVote vote = new ConsensusVote();
            vote.setQuestion(question);
            
            // NEW: Check which AIs have available quota
            List<String> availableProviders = quotaService.getAvailableProviders();
            boolean needsFallback = quotaService.shouldUseFallback();
            
            if (availableProviders.isEmpty()) {
                logger.error("❌ ALL AI PROVIDERS OUT OF QUOTA! Falling back...");
                return handleQuotaFallback(question);
            }
            
            if (needsFallback) {
                logger.warn("⚠️ Less than 5 AIs available - using limited consensus");
            }
            
            logger.info("🤖 Asking {} available AI providers: {}", availableProviders.size(), question);
            logger.info("📊 Available: {}", availableProviders);
            
            // Get responses from available providers asynchronously
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(10, availableProviders.size()));
            List<Future<Map.Entry<String, String>>> futures = new ArrayList<>();
            
            for (String provider : availableProviders) {
                futures.add(executor.submit(() -> {
                    try {
                        // Query the AI
                        String response = queryAI(provider, question);
                        
                        // Record usage (assume ~200 tokens per request)
                        quotaService.recordUsage(provider, 200);
                        
                        logger.debug("✅ Response from {}: {}", provider, response.substring(0, Math.min(50, response.length())));
                        return new AbstractMap.SimpleEntry<>(provider, response);
                    } catch (Exception e) {
                        logger.warn("❌ {} failed: {}", provider, e.getMessage());
                        return new AbstractMap.SimpleEntry<>(provider, "error");
                    }
                }));
            }
            
            // Collect responses
            int successCount = 0;
            for (Future<Map.Entry<String, String>> future : futures) {
                try {
                    Map.Entry<String, String> entry = future.get(5, TimeUnit.SECONDS);
                    vote.addResponse(entry.getKey(), entry.getValue());
                    if (!entry.getValue().equals("error")) {
                        successCount++;
                    }
                } catch (TimeoutException e) {
                    logger.warn("⏱️ Timeout waiting for response");
                }
            }
            
            executor.shutdown();
            
            logger.info("✅ Received {} responses from {} available providers", successCount, availableProviders.size());
            
            // Vote on best response
            voteBestResponse(vote);
            
            // Learn from all perspectives
            learnFromMultipleAI(vote, question);
            
            // Store vote history
            voteHistory.put(vote.getId(), vote);
            
            return vote;
            
        } catch (Exception e) {
            logger.error("❌ Multi-AI consensus failed: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Fallback when quotas are exhausted
     */
    private ConsensusVote handleQuotaFallback(String question) {
        logger.warn("⚠️ QUOTA FALLBACK triggered - using cached consensus or local model");
        
        ConsensusVote vote = new ConsensusVote();
        vote.setQuestion(question);
        
        // Try to use cached consensus from last successful vote
        ConsensusVote lastVote = voteHistory.values().stream()
            .max(Comparator.comparingLong(ConsensusVote::getTimestamp))
            .orElse(null);
        
        if (lastVote != null && lastVote.getWinningResponse() != null) {
            logger.info("📚 Using cached consensus from previous vote");
            vote.setWinningResponse(lastVote.getWinningResponse());
            vote.setConfidenceScore(lastVote.getConfidenceScore());
            // Copy votes by re-voting
            for (Map.Entry<String, Integer> voteEntry : lastVote.getVotes().entrySet()) {
                for (int i = 0; i < voteEntry.getValue(); i++) {
                    vote.voteFor(voteEntry.getKey());
                }
            }
            return vote;
        }
        
        // Fallback: Local model response
        logger.warn("📍 All quotas exceeded and no cache - using local fallback response");
        vote.setWinningResponse("[LOCAL_FALLBACK] Please wait for quota reset or reduce requests");
        vote.setConfidenceScore(0.3);
        
        return vote;
    }
    
    /**
     * Vote on best response - majority wins
     */
    private void voteBestResponse(ConsensusVote vote) {
        // Group similar responses and count votes
        Map<String, Integer> responseCounts = new HashMap<>();
        
        for (String response : vote.getProviderResponses().values()) {
            if (!response.equals("error")) {
                // Normalize response for grouping
                String normalized = response.substring(0, Math.min(50, response.length()));
                responseCounts.put(normalized, responseCounts.getOrDefault(normalized, 0) + 1);
                vote.voteFor(normalized);
            }
        }
        
        // Find winning response
        String winner = vote.getVotes().entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(null);
        
        vote.setWinningResponse(winner);
        
        // Calculate confidence (consensus percentage)
        Double confidence = (double) vote.getConsensusPercentage() / 100.0;
        vote.setConfidenceScore(confidence);
        
        logger.info("📊 Consensus winner ({}% agreement): {}", 
            vote.getConsensusPercentage(), winner);
    }
    
    /**
     * Learn from all 10 AI perspectives
     */
    private void learnFromMultipleAI(ConsensusVote vote, String question) {
        try {
            Set<String> uniqueApproaches = new HashSet<>();
            
            // Extract unique approaches from each provider
            for (Map.Entry<String, String> entry : vote.getProviderResponses().entrySet()) {
                String provider = entry.getKey();
                String response = entry.getValue();
                
                if (!response.equals("error")) {
                    String learning = extractLearning(provider, response);
                    uniqueApproaches.add(learning);
                    vote.addLearning(learning);
                    
                    logger.info("📚 Learning from {}: {}", provider, learning);
                }
            }
            
            // Record all learnings in SystemLearning
            String combinedLearning = String.format(
                "Question: %s | Consensus: %s | Approaches: %d | Confidence: %.2f",
                question, vote.getWinningResponse(), uniqueApproaches.size(), 
                vote.getConfidenceScore()
            );
            
            learningService.recordPattern(
                "MULTI_AI_CONSENSUS",
                combinedLearning,
                "Learned from " + vote.getTotalResponses() + " AI providers"
            );
            
            logger.info("✅ Recorded learning from all {} AI perspectives", 
                vote.getTotalResponses());
            
        } catch (Exception e) {
            logger.error("❌ Learning from AI failed: {}", e.getMessage());
        }
    }
    
    /**
     * Extract unique learning from each AI provider
     */
    private String extractLearning(String provider, String response) {
        // Simple extraction - in production would use NLP
        String cleaned = response.replace("\\n", " ").substring(0, Math.min(100, response.length()));
        return String.format("[%s] %s", provider, cleaned);
    }
    
    /**
     * Query single AI provider
     */
    private String queryAI(String provider, String question) throws Exception {
        // In production, call actual AI APIs
        // For now, return mock response
        return String.format("Response from %s: Analysis of %s", provider, question);
    }
    
    /**
     * Get consensus vote history
     */
    public List<ConsensusVote> getVoteHistory() {
        return new ArrayList<>(voteHistory.values());
    }
    
    /**
     * Get stats on learnings
     */
    public Map<String, Object> getConsensusStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVotes", voteHistory.size());
        stats.put("totalProvidersConsulted", AI_PROVIDERS.size() * voteHistory.size());
        stats.put("learningsRecorded", voteHistory.size());
        stats.put("averageConsensus", voteHistory.values().stream()
            .mapToDouble(ConsensusVote::getConfidenceScore)
            .average()
            .orElse(0.0));
        
        return stats;
    }
    
    /**
     * Record feedback on consensus recommendation
     */
    public void recordFeedback(String voteId, String executedSolution, String outcome, 
                               Double successRate, String errorDetails) {
        try {
            ConsensusFeedback feedback = new ConsensusFeedback();
            feedback.setVoteId(voteId);
            feedback.setExecutedSolution(executedSolution);
            feedback.setOutcome(outcome);
            feedback.setActualSuccessRate(successRate);
            feedback.setErrorDetails(errorDetails);
            
            feedbackHistory.put(feedback.getId(), feedback);
            
            // If consensus was wrong, learn from it
            if (!feedback.wasVoteAccurate()) {
                logger.warn("⚠️ Consensus was INACCURATE: vote={}, success={}", voteId, successRate);
                learningService.recordError(
                    "CONSENSUS_FAILED",
                    "Consensus recommendation failed: " + outcome,
                    new Exception(errorDetails),
                    "Review AI provider " + executedSolution + " voting pattern"
                );
            } else {
                logger.info("✅ Consensus was ACCURATE: vote={}, success={}", voteId, successRate);
            }
            
        } catch (Exception e) {
            logger.error("❌ Failed to record feedback: {}", e.getMessage());
        }
    }
    
    /**
     * Get feedback history (how accurate are we?)
     */
    public Map<String, Object> getFeedbackStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long accurateVotes = feedbackHistory.values().stream()
            .filter(ConsensusFeedback::wasVoteAccurate)
            .count();
        
        stats.put("totalFeedback", feedbackHistory.size());
        stats.put("accurateVotes", accurateVotes);
        stats.put("accuracy", feedbackHistory.isEmpty() ? 0 : 
            (double) accurateVotes / feedbackHistory.size());
        stats.put("averageSuccessRate", feedbackHistory.values().stream()
            .mapToDouble(ConsensusFeedback::getActualSuccessRate)
            .average()
            .orElse(0.0));
        
        return stats;
    }
}
