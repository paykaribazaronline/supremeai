package com.supremeai.agentorchestration;

import com.supremeai.model.ProviderVote;
import java.util.List;

public class VotingDecision {
    private String decisionKey;
    private String proposedAnswer; // Admin's initial answer
    private String aiConsensus;   // AI-agreed answer
    private double confidence;
    private String strength; // STRONG, WEAK, ERROR
    private List<ProviderVote> providerVotes;

    public VotingDecision() {}

    public String getDecisionKey() { return decisionKey; }
    public void setDecisionKey(String decisionKey) { this.decisionKey = decisionKey; }
    public String getProposedAnswer() { return proposedAnswer; }
    public void setProposedAnswer(String proposedAnswer) { this.proposedAnswer = proposedAnswer; }
    public String getAiConsensus() { return aiConsensus; }
    public void setAiConsensus(String aiConsensus) { this.aiConsensus = aiConsensus; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }
    public List<ProviderVote> getProviderVotes() { return providerVotes; }
    public void setProviderVotes(List<ProviderVote> providerVotes) { this.providerVotes = providerVotes; }
}