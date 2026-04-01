package org.example.model;

import java.util.*;

/**
 * Multi-AI Consensus Vote
 * Multiple AI providers vote on best solution
 */
public class ConsensusVote {
    private String id = UUID.randomUUID().toString();
    private String question;
    private Map<String, String> providerResponses = new HashMap<>(); // provider -> response
    private Map<String, Integer> votes = new HashMap<>(); // response -> vote count
    private String winningResponse;
    private Double confidenceScore = 0.0;
    private List<String> learnings = new ArrayList<>();
    private long timestamp = System.currentTimeMillis();
    
    // Getters & Setters
    public String getId() { return id; }
    
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    
    public Map<String, String> getProviderResponses() { return providerResponses; }
    public void addResponse(String provider, String response) {
        this.providerResponses.put(provider, response);
    }
    
    public Map<String, Integer> getVotes() { return votes; }
    public void voteFor(String response) {
        votes.put(response, votes.getOrDefault(response, 0) + 1);
    }
    
    public String getWinningResponse() { return winningResponse; }
    public void setWinningResponse(String response) { this.winningResponse = response; }
    
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double score) { this.confidenceScore = score; }
    
    public List<String> getLearnings() { return learnings; }
    public void addLearning(String learning) { this.learnings.add(learning); }
    
    public long getTimestamp() { return timestamp; }
    
    public int getTotalResponses() { return providerResponses.size(); }
    public int getConsensusPercentage() {
        if (winningResponse == null || votes.isEmpty()) return 0;
        Integer winningVotes = votes.get(winningResponse);
        if (winningVotes == null) return 0;
        return (winningVotes * 100) / providerResponses.size();
    }
}
