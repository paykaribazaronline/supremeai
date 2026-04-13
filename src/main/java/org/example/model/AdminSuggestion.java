package org.example.model;

/**
 * AdminSuggestion — represents a change suggestion submitted by admin from any tab.
 * When applyNow=true the system processes it immediately via AI consensus.
 */
public class AdminSuggestion {

    public enum Status {
        PENDING,    // Saved but not yet applied
        PROCESSING, // Being applied right now
        APPLIED,    // Successfully applied by AI
        FAILED      // AI processing failed
    }

    private String id;
    private String tabKey;       // Which admin panel tab this suggestion is for
    private String tabLabel;     // Human-readable tab name
    private String suggestion;   // The admin's suggestion text
    private boolean applyNow;    // Whether to apply immediately
    private Status status;
    private String aiResponse;   // AI response / change summary after applying
    private long createdAt;
    private Long appliedAt;
    private String createdBy;

    public AdminSuggestion() {
        this.status = Status.PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTabKey() { return tabKey; }
    public void setTabKey(String tabKey) { this.tabKey = tabKey; }

    public String getTabLabel() { return tabLabel; }
    public void setTabLabel(String tabLabel) { this.tabLabel = tabLabel; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public boolean isApplyNow() { return applyNow; }
    public void setApplyNow(boolean applyNow) { this.applyNow = applyNow; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Long getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Long appliedAt) { this.appliedAt = appliedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
