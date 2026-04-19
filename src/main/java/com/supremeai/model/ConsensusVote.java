package com.supremeai.model;

import java.time.LocalDateTime;
import java.util.List;

// Plain POJO for taste phase - will be persisted to Firebase via custom logic
public class ConsensusVote {
    private String id;
    private String question;
    private String consensusAnswer;
    private Double consensusPercentage;
    private String consensusStrength;
    private List<ProviderVote> votes;
    private LocalDateTime timestamp;

    public ConsensusVote() {}

    public ConsensusVote(String question, String consensusAnswer, Double consensusPercentage, 
                        String consensusStrength, List<ProviderVote> votes) {
        this.question = question;
        this.consensusAnswer = consensusAnswer;
        this.consensusPercentage = consensusPercentage;
        this.consensusStrength = consensusStrength;
        this.votes = votes;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getConsensusAnswer() { return consensusAnswer; }
    public void setConsensusAnswer(String consensusAnswer) { this.consensusAnswer = consensusAnswer; }
    public Double getConsensusPercentage() { return consensusPercentage; }
    public void setConsensusPercentage(Double consensusPercentage) { this.consensusPercentage = consensusPercentage; }
    public String getConsensusStrength() { return consensusStrength; }
    public void setConsensusStrength(String consensusStrength) { this.consensusStrength = consensusStrength; }
    public List<ProviderVote> getVotes() { return votes; }
    public void setVotes(List<ProviderVote> votes) { this.votes = votes; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
