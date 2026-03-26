package org.example.model;

public class Vote {
    private String agentId;
    private boolean approved;
    private String comments;

    public Vote(String agentId, boolean approved, String comments) {
        this.agentId = agentId;
        this.approved = approved;
        this.comments = comments;
    }

    public String getAgentId() { return agentId; }
    public boolean isApproved() { return approved; }
    public String getComments() { return comments; }
}
