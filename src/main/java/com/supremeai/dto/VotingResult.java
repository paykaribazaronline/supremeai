package com.supremeai.dto;

import java.util.List;
import java.util.Map;

public class VotingResult {
    private AISolution winner;
    private double confidence;
    private List<String> dissentingOpinions;
    private Map<String, Double> fullBreakdown;

    // Getters
    public AISolution getWinner() { return winner; }
    public double getConfidence() { return confidence; }
    public List<String> getDissentingOpinions() { return dissentingOpinions; }
    public Map<String, Double> getFullBreakdown() { return fullBreakdown; }

    // Setters
    public void setWinner(AISolution winner) { this.winner = winner; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public void setDissentingOpinions(List<String> dissentingOpinions) { this.dissentingOpinions = dissentingOpinions; }
    public void setFullBreakdown(Map<String, Double> fullBreakdown) { this.fullBreakdown = fullBreakdown; }

    // Static builder method
    public static VotingResult builder() {
        return new VotingResult();
    }

    public VotingResult build() {
        return this;
    }

    public VotingResult winner(AISolution winner) {
        this.winner = winner;
        return this;
    }

    public VotingResult confidence(double confidence) {
        this.confidence = confidence;
        return this;
    }

    public VotingResult dissentingOpinions(List<String> dissentingOpinions) {
        this.dissentingOpinions = dissentingOpinions;
        return this;
    }

    public VotingResult fullBreakdown(Map<String, Double> fullBreakdown) {
        this.fullBreakdown = fullBreakdown;
        return this;
    }
}
