package com.supremeai.model;

import java.util.List;

public class ConsensusResult {
    private String question;
    private String consensusAnswer;
    private List<ProviderVote> votes;
    private double averageConfidence;
    private String strength; // STRONG, WEAK, ERROR
    private double consensusPercentage;
    private long responseTimeMs;
    private double qualityScore;

    public ConsensusResult() {}

    public ConsensusResult(String question, String consensusAnswer, List<ProviderVote> votes, 
                          double averageConfidence, String strength) {
        this(question, consensusAnswer, votes, averageConfidence, strength, 0.0, 0, 0.0);
    }

    public ConsensusResult(String question, String consensusAnswer, List<ProviderVote> votes, 
                          double averageConfidence, String strength, double consensusPercentage,
                          long responseTimeMs, double qualityScore) {
        this.question = question;
        this.consensusAnswer = consensusAnswer;
        this.votes = votes;
        this.averageConfidence = averageConfidence;
        this.strength = strength;
        this.consensusPercentage = consensusPercentage;
        this.responseTimeMs = responseTimeMs;
        this.qualityScore = qualityScore;
    }

    // Results mapping for client display
    public List<ProviderVote> getProviderVotes() { return votes; }
    public String getConsensus() { return consensusAnswer; }
    public String getDecision() { return consensusAnswer; }
    public double getConfidence() { return averageConfidence; }
    public String getStatus() { return strength; }
    public boolean isStrongConsensus() { return "STRONG".equals(strength) || "CONSENSUS_STRONG".equals(strength); }
    public double getConsensusPercentage() { return consensusPercentage; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public double getQualityScore() { return qualityScore; }

    // Getters and setters
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    
    public String getConsensusAnswer() { return consensusAnswer; }
    public void setConsensusAnswer(String consensusAnswer) { this.consensusAnswer = consensusAnswer; }
    
    public List<ProviderVote> getVotes() { return votes; }
    public void setVotes(List<ProviderVote> votes) { this.votes = votes; }
    
    public double getAverageConfidence() { return averageConfidence; }
    public void setAverageConfidence(double averageConfidence) { this.averageConfidence = averageConfidence; }
    
    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public void setConsensusPercentage(double consensusPercentage) { this.consensusPercentage = consensusPercentage; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }
}
