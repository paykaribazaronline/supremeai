package com.supremeai.admin;

import java.util.UUID;

public class ImprovementProposal {
    private String proposalId;
    private String title;
    private String description;
    private String category; // e.g., "KNOWLEDGE_BASE", "IMMUNITY_SYSTEM", "AI_PROFILER"
    private String payload; // The actual code or data to be updated
    private long timestamp;
    private boolean isApproved;

    public ImprovementProposal(String title, String description, String category, String payload) {
        this.proposalId = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.category = category;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
        this.isApproved = false;
    }

    public void approve() {
        this.isApproved = true;
    }

    // Getters
    public String getProposalId() { return proposalId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getPayload() { return payload; }
    public boolean isApproved() { return isApproved; }
    public long getTimestamp() { return timestamp; }
}