package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.Date;

@Document(collectionName = "learning_sources")
public class LearningSource {
    @DocumentId
    private String id;
    private String url;
    private String domain;
    private String detectedFocus;      // Auto-detected topic: marketing, security, ai_research, etc.
    private String manualFocus;        // Admin override
    private boolean enabled;
    private Integer priority;          // 1-10, higher = more frequent
    private Integer successCount;
    private Integer failureCount;
    private Date lastScrapedAt;
    private Date createdAt;
    private Date updatedAt;
    private String notes;

    public LearningSource() {
        this.enabled = true;
        this.priority = 5;
        this.successCount = 0;
        this.failureCount = 0;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getDetectedFocus() { return detectedFocus; }
    public void setDetectedFocus(String detectedFocus) { this.detectedFocus = detectedFocus; }

    public String getManualFocus() { return manualFocus; }
    public void setManualFocus(String manualFocus) { this.manualFocus = manualFocus; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }

    public Integer getFailureCount() { return failureCount; }
    public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }

    public Date getLastScrapedAt() { return lastScrapedAt; }
    public void setLastScrapedAt(Date lastScrapedAt) { this.lastScrapedAt = lastScrapedAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    /**
     * Returns the effective focus (manual override if set, else auto-detected).
     */
    public String getEffectiveFocus() {
        return manualFocus != null && !manualFocus.isEmpty() ? manualFocus : detectedFocus;
    }
}
