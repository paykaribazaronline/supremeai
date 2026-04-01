package org.example.model;

import java.util.*;

/**
 * Consensus Feedback
 * Tracks outcome of executed consensus votes
 */
public class ConsensusFeedback {
    private String id = UUID.randomUUID().toString();
    private String voteId;
    private String executedSolution;
    private String outcome; // SUCCESS, FAILED, PARTIAL
    private String errorDetails;
    private int executionTimeMs;
    private Double actualSuccessRate; // How well it worked (0-1)
    private String improvements;
    private long timestamp = System.currentTimeMillis();
    
    // Getters & Setters
    public String getId() { return id; }
    
    public String getVoteId() { return voteId; }
    public void setVoteId(String voteId) { this.voteId = voteId; }
    
    public String getExecutedSolution() { return executedSolution; }
    public void setExecutedSolution(String solution) { this.executedSolution = solution; }
    
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    
    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String details) { this.errorDetails = details; }
    
    public int getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(int time) { this.executionTimeMs = time; }
    
    public Double getActualSuccessRate() { return actualSuccessRate; }
    public void setActualSuccessRate(Double rate) { this.actualSuccessRate = rate; }
    
    public String getImprovements() { return improvements; }
    public void setImprovements(String improvements) { this.improvements = improvements; }
    
    public long getTimestamp() { return timestamp; }
    
    /**
     * Was the consensus recommendation actually good?
     */
    public boolean wasVoteAccurate() {
        return "SUCCESS".equals(outcome) && actualSuccessRate >= 0.8;
    }
}
