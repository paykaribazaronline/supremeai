package org.example.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a decision made by voting
 */
public class Decision {
    private String id;
    private String proposalName;
    private String description;
    private String proposedBy;
    private LocalDateTime createdAt;
    private LocalDateTime deadline;
    private String status; // "pending", "approved", "rejected", "abstained"
    private List<VoteRecord> votes;
    private double approvalRate;
    private String category;

    public Decision() {
        this.id = UUID.randomUUID().toString();
        this.votes = new ArrayList<>();
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
    }

    public Decision(String proposalName, String proposedBy) {
        this();
        this.proposalName = proposalName;
        this.proposedBy = proposedBy;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProposalName() { return proposalName; }
    public void setProposalName(String proposalName) { this.proposalName = proposalName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProposedBy() { return proposedBy; }
    public void setProposedBy(String proposedBy) { this.proposedBy = proposedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<VoteRecord> getVotes() { return votes; }
    public void setVotes(List<VoteRecord> votes) { this.votes = votes; }

    public double getApprovalRate() { return approvalRate; }
    public void setApprovalRate(double approvalRate) { this.approvalRate = approvalRate; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    /**
     * Nested class for individual vote records
     */
    public static class VoteRecord {
        private String agentName;
        private String vote; // "approve", "reject", "abstain"
        private double confidence;
        private String reasoning;
        private LocalDateTime votedAt;

        public VoteRecord() {
            this.votedAt = LocalDateTime.now();
        }

        // Getters and Setters
        public String getAgentName() { return agentName; }
        public void setAgentName(String agentName) { this.agentName = agentName; }

        public String getVote() { return vote; }
        public void setVote(String vote) { this.vote = vote; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }

        public LocalDateTime getVotedAt() { return votedAt; }
        public void setVotedAt(LocalDateTime votedAt) { this.votedAt = votedAt; }
    }
}
