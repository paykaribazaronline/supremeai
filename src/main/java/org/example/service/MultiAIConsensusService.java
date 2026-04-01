package org.example.service;

import org.example.model.ConsensusVote;
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
            
            logger.info("🤖 Asking 10 AI providers: {}", question);
            
            // Get responses from all providers asynchronously
            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Future<Map.Entry<String, String>>> futures = new ArrayList<>();
            
            for (String provider : AI_PROVIDERS) {
                futures.add(executor.submit(() -> {
                    try {
                        String response = queryAI(provider, question);
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
            
            logger.info("✅ Received {} responses from {} providers", successCount, AI_PROVIDERS.size());
            
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
}
